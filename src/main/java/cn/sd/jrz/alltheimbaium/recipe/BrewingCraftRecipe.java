package cn.sd.jrz.alltheimbaium.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 将酿造台配方转移到工作台的动态合成配方。
 * <p>
 * 原酿造台：3 个水瓶/药水 + 1 个酿造材料 → 3 个酿造后药水。
 * 现工作台：1 个水瓶/药水 + 1 个酿造材料 → 1 个酿造后药水。
 * <p>
 * 支持普通药水、喷溅药水和滞留药水作为输入，输出类型与输入类型保持一致。
 * <p>
 * 此配方为无序合成，需要在合成台中恰好放入 2 个物品（任意位置）。
 * 配方在合成时动态查询 {@link PotionBrewing}（经由 {@link Level#potionBrewing()}）获取所有酿造配方。
 */
public class BrewingCraftRecipe extends CustomRecipe {

    /**
     * 26.x：序列化器改为 {@code record RecipeSerializer<T>(MapCodec<T>, StreamCodec<...>)}，
     * 无自定义 JSON 字段的配方用一个"无字段"的编解码器即可。
     * MAP_CODEC 与 STREAM_CODEC 都按「每次解码新建实例」处理：cachedResult 是 matches/assemble
     * 之间的有状态缓存，各实例之间必须相互隔离（与 1.21.1 的 SimpleCraftingRecipeSerializer 一致）。
     */
    public static final MapCodec<BrewingCraftRecipe> MAP_CODEC = MapCodec.unit(BrewingCraftRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, BrewingCraftRecipe> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public @Nonnull BrewingCraftRecipe decode(@Nonnull RegistryFriendlyByteBuf buf) {
                    // 本配方没有任何需要传输的字段：解码时新建实例即可。
                    // 不能用 StreamCodec.unit —— 它会校验 value.equals(instance)，
                    // 而本配方未重写 equals，会让整个配方同步包编码失败。
                    return new BrewingCraftRecipe();
                }

                @Override
                public void encode(@Nonnull RegistryFriendlyByteBuf buf, @Nonnull BrewingCraftRecipe recipe) {
                    // 无字段可写
                }
            };

    @SuppressWarnings("deprecation")
    public static final RecipeSerializer<BrewingCraftRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /**
     * 由 {@link #matches} 计算并缓存，供 {@link #assemble} 消费。
     */
    private ItemStack cachedResult = ItemStack.EMPTY;

    // ==================== 配方方法覆写 ====================

    @Override
    public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) {
        // 每次匹配都先清空缓存，以本次网格为准，避免旧结果残留被 assemble 读到
        this.cachedResult = ItemStack.EMPTY;
        // 统计非空格子数量，必须恰好为 2
        ItemStack itemA = ItemStack.EMPTY;
        ItemStack itemB = ItemStack.EMPTY;
        int count = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                count++;
                if (count > 2) {
                    return false;
                }
                if (itemA.isEmpty()) {
                    itemA = stack;
                } else {
                    itemB = stack;
                }
            }
        }

        if (count != 2) {
            return false;
        }

        // 尝试两种朝向：(药水, 材料) 或 (材料, 药水)
        ItemStack result = tryBrew(itemA, itemB, level);
        if (result == null) {
            result = tryBrew(itemB, itemA, level);
        }

        if (result != null) {
            this.cachedResult = result;
            return true;
        }
        return false;
    }

    /**
     * 尝试匹配并执行酿造配方。
     * <p>
     * <b>注意：</b>{@link PotionBrewing#hasMix} 的参数顺序为 {@code (药水, 材料)}，
     * 而 {@link PotionBrewing#mix} 的参数顺序为 {@code (材料, 药水)}——两者参数顺序相反。
     * <p>
     * 1.21.1 起 {@link PotionBrewing} 不再有静态方法，实例由 {@link Level#potionBrewing()} 提供。
     *
     * @param potionCandidate     应为药水/水瓶的物品
     * @param ingredientCandidate 应为酿造材料的物品
     * @return 酿造结果的物品堆，如果无匹配配方则返回 null
     */
    @Nullable
    private static ItemStack tryBrew(ItemStack potionCandidate, ItemStack ingredientCandidate, Level level) {
        PotionBrewing brewing = level.potionBrewing();
        if (isPotionItem(potionCandidate) && brewing.isIngredient(ingredientCandidate)) {
            // hasMix(药水, 材料)：先药水后材料
            if (brewing.hasMix(potionCandidate, ingredientCandidate)) {
                // mix(材料, 药水)：先材料后药水（参数顺序与 hasMix 相反！）
                return brewing.mix(ingredientCandidate, potionCandidate);
            }
        }
        return null;
    }

    /**
     * 判断物品是否为药水类物品（普通药水、喷溅药水或滞留药水）。
     */
    private static boolean isPotionItem(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput input) {
        // 只读取 matches() 缓存的结果，不再清空。
        // 原因：Polymorph / FastWorkbench 等 mod 会在一次合成流程中对本配方多次调用
        // assemble()，若这里清空缓存，后续读取会拿到空物品，导致手工放置时结果槽被错误覆盖为空。
        // 网格变化时 matches() 会重新计算并覆盖缓存。
        ItemStack result = this.cachedResult.copy();
        return result;
    }

    @Override
    public boolean isSpecial() {
        // 动态配方，排除在配方书之外
        return true;
    }

    @Nonnull
    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
