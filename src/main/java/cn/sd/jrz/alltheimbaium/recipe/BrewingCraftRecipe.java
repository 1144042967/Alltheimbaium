package cn.sd.jrz.alltheimbaium.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * 将酿造台配方转移到工作台的动态合成配方。1个药水 + 1个酿造材料 → 1个酿造后药水。
 */
public class BrewingCraftRecipe extends CustomRecipe {
    public static final RecipeSerializer<BrewingCraftRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(BrewingCraftRecipe::new);
    private ItemStack cachedResult = ItemStack.EMPTY;

    public BrewingCraftRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(@Nonnull CraftingInput input, @Nonnull Level level) {
        if (input.ingredientCount() != 2) return false;
        ItemStack itemA = ItemStack.EMPTY, itemB = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (itemA.isEmpty()) itemA = stack;
                else {
                    itemB = stack;
                    break;
                }
            }
        }
        if (itemA.isEmpty() || itemB.isEmpty()) return false;
        PotionBrewing brewing = level.potionBrewing();
        ItemStack result = tryBrew(itemA, itemB, brewing);
        if (result == null) result = tryBrew(itemB, itemA, brewing);
        if (result != null) {
            this.cachedResult = result;
            return true;
        }
        return false;
    }

    @Nullable
    private static ItemStack tryBrew(ItemStack potionCandidate, ItemStack ingredientCandidate, PotionBrewing brewing) {
        if (isPotionItem(potionCandidate) && brewing.isIngredient(ingredientCandidate)) {
            if (brewing.hasMix(potionCandidate, ingredientCandidate)) {
                return brewing.mix(ingredientCandidate, potionCandidate);
            }
        }
        return null;
    }

    private static boolean isPotionItem(ItemStack s) {
        return s.is(Items.POTION) || s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput input, @Nonnull HolderLookup.Provider registries) {
        ItemStack r = this.cachedResult.copy();
        this.cachedResult = ItemStack.EMPTY;
        return r;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem(@Nonnull HolderLookup.Provider registries) {
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
