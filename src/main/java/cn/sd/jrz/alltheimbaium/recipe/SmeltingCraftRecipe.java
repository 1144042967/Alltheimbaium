package cn.sd.jrz.alltheimbaium.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 动态合成配方：3×3工作台中用煤炭批量烧炼物品，无需熔炉。
 * 外围8格放同一种可烧炼物品，中心放煤炭/木炭。
 */
public class SmeltingCraftRecipe extends CustomRecipe {
    public static final RecipeSerializer<SmeltingCraftRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(SmeltingCraftRecipe::new);
    private ItemStack cachedResult = ItemStack.EMPTY;

    public SmeltingCraftRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        ItemStack center = input.getItem(1, 1);
        if (center.isEmpty() || !center.is(ItemTags.COALS)) return false;
        ItemStack ref = input.getItem(0, 0);
        if (ref.isEmpty()) return false;
        int idx = 0;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++, idx++)
                if (idx != 4) {
                    ItemStack s = input.getItem(col, row);
                    if (s.isEmpty() || !ItemStack.isSameItemSameComponents(ref, s)) return false;
                }
        ItemStack smelted = findFurnaceResult(ref, level);
        if (smelted == null) return false;
        this.cachedResult = smelted;
        return true;
    }

    @Nullable
    private static ItemStack findFurnaceResult(ItemStack input, Level level) {
        if (level.isClientSide) return null;
        RecipeManager rm = level.getRecipeManager();
        ItemStack single = input.copyWithCount(1);
        for (RecipeType<? extends AbstractCookingRecipe> type : List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING))
            for (var h : rm.getAllRecipesFor(type)) {
                AbstractCookingRecipe r = h.value();
                for (Ingredient ing : r.getIngredients())
                    if (ing.test(single)) return r.getResultItem(level.registryAccess()).copy();
            }
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w >= 3 && h >= 3;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput input, @Nonnull HolderLookup.Provider registries) {
        ItemStack r = this.cachedResult.copy();
        this.cachedResult = ItemStack.EMPTY;
        int count = Math.min(8, r.getMaxStackSize());
        r.setCount(count);
        return r;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem(@Nonnull HolderLookup.Provider p) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
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
}
