package cn.sd.jrz.alltheimbaium.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Collection;
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
 * This recipe dynamically queries the server's {@link net.minecraft.world.item.crafting.RecipeManager}
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

    /**
     * The serializer instance. Uses {@link SimpleCraftingRecipeSerializer} because
     * this recipe needs no custom JSON data beyond the type discriminator.
     */
    public static final RecipeSerializer<SmeltingCraftRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(SmeltingCraftRecipe::new);

    /**
     * Cached smelting result set by {@link #matches} and consumed by {@link #assemble}.
     * Reset to EMPTY after consumption. This pattern is necessary because
     * {@code assemble()} receives {@link RegistryAccess} but not {@link Level},
     * so it cannot access the world-specific {@code RecipeManager}.
     */
    private ItemStack cachedResult = ItemStack.EMPTY;

    public SmeltingCraftRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    // ==================== Recipe overrides ====================

    @Override
    public boolean matches(@Nonnull CraftingContainer container, @Nonnull Level level) {
        // 每次匹配都先清空缓存，以本次网格为准，避免旧结果残留被 assemble/getResultItem 读到
        this.cachedResult = ItemStack.EMPTY;

        // (1) Require 3×3 crafting grid (not the player's 2×2 grid)
        if (container.getWidth() != 3 || container.getHeight() != 3) {
            return false;
        }

        // (2) Center slot must be coal or charcoal
        ItemStack center = container.getItem(4);
        if (center.isEmpty() || !center.is(ItemTags.COALS)) {
            return false;
        }

        // (3) First outer slot must be non-empty (defines the reference item)
        ItemStack reference = container.getItem(0);
        if (reference.isEmpty()) {
            return false;
        }

        // (4) All 8 outer slots must contain the exact same item (including NBT)
        for (int slot : OUTER_SLOTS) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameTags(reference, stack)) {
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

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer container, @Nonnull RegistryAccess registryAccess) {
        // 只读取 matches() 缓存的结果，不再清空。
        // 原因：Polymorph / FastWorkbench 等 mod 会在一次合成流程中对本配方多次调用
        // getResultItem()/assemble()（遍历配方列表、刷新客户端预览等），若这里清空缓存，
        // 后续 getResultItem() 会读到空物品，导致手工放置时结果槽被错误覆盖为空。
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

    @Nonnull
    @Override
    public ItemStack getResultItem(@Nonnull RegistryAccess registryAccess) {
        // Dynamic recipe — no constant output to preview
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        // Exclude from recipe book (cannot auto-fill a dynamic pattern)
        return true;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Nonnull
    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    // ==================== Furnace lookup ====================

    /**
     * Searches smelting, blasting, and smoking recipe lists (in that order)
     * for a recipe whose ingredient accepts {@code input}.
     *
     * @return the matching furnace recipe's result item (copy), or {@code null} if none found
     */
    @javax.annotation.Nullable
    private static ItemStack findFurnaceResult(@Nonnull ItemStack input, @Nonnull Level level) {
        // Client does not have the authoritative recipe manager — skip
        if (level.isClientSide) {
            return null;
        }

        RecipeManager recipeManager = level.getRecipeManager();
        ItemStack singleItem = input.copyWithCount(1);

        for (RecipeType<? extends AbstractCookingRecipe> type : FURNACE_TYPES) {
            Collection<? extends AbstractCookingRecipe> recipes = recipeManager.getAllRecipesFor(type);
            for (AbstractCookingRecipe recipe : recipes) {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.test(singleItem)) {
                        return recipe.getResultItem(level.registryAccess()).copy();
                    }
                }
            }
        }

        return null;
    }
}
