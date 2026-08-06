package cn.sd.jrz.alltheimbaium.recipe;

import cn.sd.jrz.alltheimbaium.setup.Config;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * 混合药水合成配方。
 * <p>
 * <b>核心规则：</b>
 * <ol>
 *   <li>任意两瓶药水（原版或混合）在工作台合成 → 混合药水</li>
 *   <li>混合药水 + 火药 → 喷溅混合药水</li>
 *   <li>混合药水 + 龙息 → 滞留混合药水</li>
 *   <li>混合药水 + 奶桶 → 普通混合药水</li>
 * </ol>
 * <p>
 * <b>效果合并规则：</b>
 * <ul>
 *   <li>等级：保留两瓶中等级更高的</li>
 *   <li>持续时间：
 *     <ul>
 *       <li>效果仅在其中一瓶中存在 → 取该瓶的时间</li>
 *       <li>效果在两瓶中均存在 → (时间A + 时间B) × 配置系数</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * <b>输出类型（融合时）：</b>从原料中选取，优先级：滞留 > 喷溅 > 普通。
 * <p>
 * <b>与酿造合成的分工：</b>
 * 原版药水 + 普通酿造材料 → 走 {@link BrewingCraftRecipe}（原版酿造配方）。
 * 混合药水（有自定义效果）+ 火药/龙息/奶桶 → 走本配方的类型转换。
 */
public class PotionCombineRecipe extends CustomRecipe {

    public static final RecipeSerializer<PotionCombineRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(PotionCombineRecipe::new);

    // 从配置文件加载的本地缓存值，由 Config.onConfigLoad() 在配置加载后调用 loadConfig() 填入
    private static double durationFactor;

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用
     */
    public static void loadConfig() {
        durationFactor = Config.POTION_COMBINE_DURATION_FACTOR.get();
    }

    /**
     * 由 {@link #matches} 计算并缓存，供 {@link #assemble} 消费。
     */
    private ItemStack cachedResult = ItemStack.EMPTY;

    public PotionCombineRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    // ==================== 配方方法覆写 ====================

    @Override
    public boolean matches(@Nonnull CraftingContainer container, @Nonnull Level level) {
        // 每次匹配都先清空缓存，以本次网格为准，避免旧结果残留被 assemble/getResultItem 读到
        this.cachedResult = ItemStack.EMPTY;
        // 统计非空格子数量，必须恰好为 2
        ItemStack itemA = ItemStack.EMPTY;
        ItemStack itemB = ItemStack.EMPTY;
        int count = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
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

        // 类型转换：混合药水 + 火药/龙息/奶桶（优先检查）
        ItemStack result = tryConvertType(itemA, itemB);
        if (result == null) {
            result = tryConvertType(itemB, itemA);
        }

        // 两瓶药水融合
        if (result == null) {
            result = tryCombine(itemA, itemB);
        }

        if (result != null) {
            this.cachedResult = result;
            return true;
        }
        return false;
    }

    // ==================== 效果读取 ====================

    /**
     * 获取药水的效果列表。混合药水取自定义效果，原版药水取基础类型效果。
     * 不做任何缩放，直接使用存储的原始值。
     */
    private static List<MobEffectInstance> getResolvedEffects(ItemStack potion) {
        List<MobEffectInstance> customEffects = PotionUtils.getCustomEffects(potion);
        if (!customEffects.isEmpty()) {
            return new ArrayList<>(customEffects);
        }
        return new ArrayList<>(PotionUtils.getPotion(potion).getEffects());
    }

    // ==================== 类型转换：混合药水 + 火药/龙息/奶桶 ====================

    /**
     * 混合药水类型转换。
     * 仅对混合药水（有自定义效果）生效；原版药水交由 {@link BrewingCraftRecipe} 处理。
     */
    @javax.annotation.Nullable
    private static ItemStack tryConvertType(ItemStack potionCandidate, ItemStack otherCandidate) {
        if (!isPotionItem(potionCandidate) || PotionUtils.getCustomEffects(potionCandidate).isEmpty()) {
            return null; // 不是混合药水，不处理
        }

        Item targetType = getConversionTarget(potionCandidate, otherCandidate);
        if (targetType == null) {
            return null;
        }

        List<MobEffectInstance> resolved = getResolvedEffects(potionCandidate);

        if (resolved.isEmpty()) {
            ItemStack result = new ItemStack(targetType);
            PotionUtils.setPotion(result, Potions.MUNDANE);
            result.setHoverName(Component.translatable("item.alltheimbaium.potion_combine.mundane"));
            return result;
        }

        ItemStack result = new ItemStack(targetType);
        PotionUtils.setCustomEffects(result, resolved);
        result.setHoverName(buildPotionName(resolved, targetType));
        return result;
    }

    /**
     * 判断转换物品并返回目标药水类型。
     * 火药 → 喷溅，龙息 → 滞留，奶桶 → 普通。非转换物品返回 null。
     */
    @javax.annotation.Nullable
    private static Item getConversionTarget(ItemStack potion, ItemStack other) {
        if (other.is(Items.GUNPOWDER)) {
            return Items.SPLASH_POTION;
        }
        if (other.is(Items.DRAGON_BREATH)) {
            return Items.LINGERING_POTION;
        }
        if (other.is(Items.MILK_BUCKET)) {
            return Items.POTION;
        }
        return null;
    }

    // ==================== 两瓶药水融合 ====================

    @javax.annotation.Nullable
    private static ItemStack tryCombine(ItemStack stackA, ItemStack stackB) {
        if (!isPotionItem(stackA) || !isPotionItem(stackB)) {
            return null;
        }

        List<MobEffectInstance> effectsA = getResolvedEffects(stackA);
        List<MobEffectInstance> effectsB = getResolvedEffects(stackB);

        // 合并效果
        Map<MobEffect, MobEffectInstance> combined = new LinkedHashMap<>();

        for (MobEffectInstance effect : effectsA) {
            combined.put(effect.getEffect(), new MobEffectInstance(effect));
        }

        for (MobEffectInstance effectB : effectsB) {
            MobEffect key = effectB.getEffect();
            MobEffectInstance existing = combined.get(key);
            if (existing == null) {
                combined.put(key, new MobEffectInstance(effectB));
            } else {
                combined.put(key, mergeEffect(existing, effectB));
            }
        }

        // 确定输出类型：滞留 > 喷溅 > 普通
        Item outputType = getOutputPotionType(stackA, stackB);
        ItemStack result = new ItemStack(outputType);

        if (combined.isEmpty()) {
            PotionUtils.setPotion(result, Potions.MUNDANE);
            result.setHoverName(Component.translatable("item.alltheimbaium.potion_combine.mundane"));
        } else {
            PotionUtils.setCustomEffects(result, combined.values());
            result.setHoverName(buildPotionName(combined.values(), outputType));
        }

        return result;
    }

    /**
     * 合并两个同类型效果。
     * <ul>
     *   <li>等级：取两者中更高者</li>
     *   <li>持续时间：效果仅在一瓶中存在则直接取该时间；两瓶都有则 (d1 + d2) × 配置系数</li>
     * </ul>
     */
    private static MobEffectInstance mergeEffect(MobEffectInstance a, MobEffectInstance b) {
        MobEffect effect = a.getEffect();
        int ampA = a.getAmplifier();
        int ampB = b.getAmplifier();
        int bestAmp = Math.max(ampA, ampB);

        int newDuration;
        if (ampA == ampB) {
            // 同等级，两瓶都有此效果 → (d1 + d2) × 配置系数
            newDuration = (int) ((a.getDuration() + b.getDuration()) * durationFactor);
        } else if (ampA > ampB) {
            // 等级来自 A → 取 A 的时间
            newDuration = a.getDuration();
        } else {
            // 等级来自 B → 取 B 的时间
            newDuration = b.getDuration();
        }

        return new MobEffectInstance(effect, newDuration, bestAmp);
    }

    /**
     * 构建混合药水的自定义名称。
     * 取持续时间最长的前两种效果名称 + 类型后缀（混合药水 / 混合喷溅药水 / 混合滞留药水）。
     */
    private static Component buildPotionName(Collection<MobEffectInstance> effects, Item outputType) {
        List<MobEffectInstance> sorted = new ArrayList<>(effects);
        sorted.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));

        String langKey;
        if (outputType == Items.SPLASH_POTION) {
            langKey = sorted.size() == 1
                    ? "item.alltheimbaium.potion_combine.single_splash"
                    : "item.alltheimbaium.potion_combine.double_splash";
        } else if (outputType == Items.LINGERING_POTION) {
            langKey = sorted.size() == 1
                    ? "item.alltheimbaium.potion_combine.single_lingering"
                    : "item.alltheimbaium.potion_combine.double_lingering";
        } else {
            langKey = sorted.size() == 1
                    ? "item.alltheimbaium.potion_combine.single"
                    : "item.alltheimbaium.potion_combine.double";
        }

        if (sorted.size() == 1) {
            return Component.translatable(langKey, sorted.get(0).getEffect().getDisplayName());
        } else {
            return Component.translatable(langKey,
                    sorted.get(0).getEffect().getDisplayName(),
                    sorted.get(1).getEffect().getDisplayName());
        }
    }

    // ==================== 工具方法 ====================

    private static boolean isPotionItem(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    /**
     * 确定融合输出的药水类型。优先级：滞留 > 喷溅 > 普通。
     */
    private static Item getOutputPotionType(ItemStack a, ItemStack b) {
        if (a.is(Items.LINGERING_POTION) || b.is(Items.LINGERING_POTION)) {
            return Items.LINGERING_POTION;
        }
        if (a.is(Items.SPLASH_POTION) || b.is(Items.SPLASH_POTION)) {
            return Items.SPLASH_POTION;
        }
        return Items.POTION;
    }

    // ==================== 标准配方方法 ====================

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer container, @Nonnull RegistryAccess registryAccess) {
        // 只读取 matches() 缓存的结果，不再清空。
        // 原因：Polymorph / FastWorkbench 等 mod 会在一次合成流程中对本配方多次调用
        // getResultItem()/assemble()，若这里清空缓存，后续 getResultItem() 会读到空物品，
        // 导致手工放置时结果槽被错误覆盖为空。网格变化时 matches() 会重新计算并覆盖缓存。
        ItemStack result = this.cachedResult.copy();
        return result;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem(@Nonnull RegistryAccess registryAccess) {
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
