package cn.sd.jrz.alltheimbaium.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A dynamic crafting recipe that allows smelting items without a furnace.
 * <p>
 * Pattern (3x3 grid):
 * <pre>
 *   A  A  A
 *   A  B  A
 *   A  A  A
 * </pre>
 * A = any item that has a furnace (smelting/blasting/smoking) recipe,
 * all 8 must be the exact same item.
 * B = coal or charcoal ({@link ItemTags#COALS}).
 * <p>
 * Output: 8 × the furnace recipe's result (capped at max stack size).
 * <p>
 * This recipe dynamically queries the server's {@link RecipeManager}
 * at craft time, so it works with furnace recipes from any mod.
 */
public class SmeltingCraftRecipe extends CustomRecipe {

    /**
     * Indices of the 8 outer slots in a 3×3 crafting grid (all except center = 4).
     */
    private static final int[] OUTER_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};

    /**
     * Furnace recipe types to check, in priority order (smelting first).
     */
    private static final List<RecipeType<? extends AbstractCookingRecipe>> FURNACE_TYPES = List.of(
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING
    );

    /**     * {@code MAP_CODEC} 与 {@code STREAM_CODEC} 都按「每次解码新建实例」处理：{@code cachedResult}
     * 是 matches/assemble 之间的有状态缓存，各实例之间必须相互隔离（与 1.21.1 的
     * {@code SimpleCraftingRecipeSerializer} 行为一致）。

     */
    public static final MapCodec<SmeltingCraftRecipe> MAP_CODEC = MapCodec.unit(SmeltingCraftRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SmeltingCraftRecipe> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public @Nonnull SmeltingCraftRecipe decode(@Nonnull RegistryFriendlyByteBuf buf) {
                    // 本配方没有任何需要传输的字段：解码时新建实例即可。
                    // 不能用 StreamCodec.unit —— 它会校验 value.equals(instance)，
                    // 而本配方未重写 equals，会让整个配方同步包编码失败。
                    return new SmeltingCraftRecipe();
                }

                @Override
                public void encode(@Nonnull RegistryFriendlyByteBuf buf, @Nonnull SmeltingCraftRecipe recipe) {
                    // 无字段可写
                }
            };

    /**
     * The serializer instance. The stream codec component of {@link RecipeSerializer} is marked
     * deprecated by vanilla (it is only kept for network sync), hence the suppression.
     */
    @SuppressWarnings("deprecation")
    public static final RecipeSerializer<SmeltingCraftRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /**
     * Cached smelting result set by {@link #matches} and consumed by {@link #assemble}.
     * Reset to EMPTY after consumption. This pattern is necessary because
     * {@code assemble()} no longer receives the {@link Level},
     * so it cannot access the world-specific {@code RecipeManager}.
     */
    private ItemStack cachedResult = ItemStack.EMPTY;

    // ==================== Recipe overrides ====================

    @Override
    public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) {
        // 每次匹配都先清空缓存，以本次网格为准，避免旧结果残留被 assemble 读到
        this.cachedResult = ItemStack.EMPTY;

        // (1) Require 3×3 crafting grid (not the player's 2×2 grid)
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }

        // (2) Center slot must be coal or charcoal
        ItemStack center = input.getItem(4);
        if (center.isEmpty() || !center.is(ItemTags.COALS)) {
            return false;
        }

        // (3) First outer slot must be non-empty (defines the reference item)
        ItemStack reference = input.getItem(0);
        if (reference.isEmpty()) {
            return false;
        }

        // (4) All 8 outer slots must contain the exact same item (including components)
        for (int slot : OUTER_SLOTS) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(reference, stack)) {
                return false;
            }
        }

        // (5) Look up furnace recipe for the reference item (server-side only)
        ItemStack smelted = findFurnaceResult(reference, level);
        if (smelted == null) {
            return false;
        }
        this.cachedResult = smelted;
        return true;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput input) {
        // 只读取 matches() 缓存的结果，不再清空。
        // 原因：Polymorph / FastWorkbench 等 mod 会在一次合成流程中对本配方多次调用
        // assemble()（遍历配方列表、刷新客户端预览等），若这里清空缓存，
        // 后续读取会拿到空物品，导致手工放置时结果槽被错误覆盖为空。
        // 网格变化时 matches() 会重新计算并覆盖缓存，因此只读是安全的。
        ItemStack result = this.cachedResult.copy();

        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Output 8 × the furnace result count, capped at the item's max stack size
        int count = Math.min(8, result.getMaxStackSize());
        result.setCount(count);
        return result;
    }

    @Override
    public boolean isSpecial() {
        // Exclude from recipe book (cannot auto-fill a dynamic pattern)
        return true;
    }

    @Nonnull
    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }

    // ==================== Furnace lookup ====================

    /**
     * Searches smelting, blasting, and smoking recipe lists (in that order)
     * for a recipe whose ingredient accepts {@code input}.
     * <p>
     * 26.x：{@code Recipe#getIngredients()} / {@code getResultItem()} 已删除，改成按输入直接查表
     * （{@code RecipeManager#getRecipeFor}）+ {@code assemble(RecipeInput)} 取产物；
     * {@code Level#getRecipeManager()} 也改成了 {@code Level#recipeAccess()}，后者只在服务端返回
     * {@link RecipeManager}（客户端的 {@code ClientRecipeContainer} 没有配方表），查不到就当作没有配方。
     *
     * @return the matching furnace recipe's result item (copy), or {@code null} if none found
     */
    @javax.annotation.Nullable
    private static ItemStack findFurnaceResult(@Nonnull ItemStack input, @Nonnull Level level) {
        // Client does not have the authoritative recipe manager — skip
        if (level.isClientSide()) {
            return null;
        }

        if (!(level.recipeAccess() instanceof RecipeManager recipeManager)) {
            return null;
        }

        SingleRecipeInput singleInput = new SingleRecipeInput(input.copyWithCount(1));

        for (RecipeType<? extends AbstractCookingRecipe> type : FURNACE_TYPES) {
            ItemStack result = findMatchingResult(recipeManager, type, singleInput, level);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * 按输入查一条烧炼类配方并取出它的产物。
     * <p>
     * {@code FURNACE_TYPES} 里的元素是通配类型 {@code RecipeType<? extends AbstractCookingRecipe>}，
     * 直接调用会把泛型摊平成原始类型；这里用一个"上界是 {@code Recipe<SingleRecipeInput>}"的辅助方法
     * 让类型推断自己接上（{@link AbstractCookingRecipe} 正是这条路），避免 raw type 告警。
     *
     * @return 该输入命中的产物（copy），无配方或产物为空时返回 {@code null}
     */
    @javax.annotation.Nullable
    private static <T extends Recipe<SingleRecipeInput>> ItemStack findMatchingResult(
            @Nonnull RecipeManager manager, @Nonnull RecipeType<T> type,
            @Nonnull SingleRecipeInput input, @Nonnull Level level) {
        RecipeHolder<T> holder = manager.getRecipeFor(type, input, level).orElse(null);
        if (holder == null) {
            return null;
        }
        ItemStack result = holder.value().assemble(input).copy();
        // 空产物配方（数据包里写了 air、或产物物品被移除后配方仍能加载）不能算匹配成功——
        // 否则 matches() 判定成立、assemble() 却吐不出东西，玩家放进去的材料会被白白吞掉。
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }
}
