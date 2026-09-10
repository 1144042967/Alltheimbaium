package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Tool;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * 永恒之剑。
 * <p>
 * - 无耐久条、基础攻击伤害为 0
 * - 右击对范围内生物结算三段：剑自身伤害（槽位攻击力之和 + 附魔伤害加成）、剑附魔的命中效果、
 *   以及槽位里每把武器各打一次（各用自己那把武器的伤害与附魔）
 * - ALT+右击打开配置界面（击杀模式 / 攻击距离 / 27 格物品槽位）
 * - 剑附魔来自槽位中的附魔书：按点数叠加（等级 1~10 计 1/2/4/…/512 点），上限 10 级
 * - 禁止附魔台 / 铁砧附魔；对 Draconic-Evolution 混沌守卫可突破免伤限制
 */
public class EternalSwordItem extends SwordItem {

    /**
     * 自定义工具等级：攻击力加成 0、耐久 0、附魔等级 0，保证剑本身攻击伤害为 0
     */
    private static final Tier SWORD_TIER = new ForgeTier(
            0, 0, 0.0F, 0.0F, 0,
            BlockTags.NEEDS_STONE_TOOL,
            () -> Ingredient.of(Items.AIR));

    /**
     * 物品槽位数量
     */
    public static final int INVENTORY_SIZE = 27;
    /**
     * 附魔叠加后的等级上限：等级 1~10 对应点数 2^0 ~ 2^9
     */
    public static final int MAX_ENCHANT_LEVEL = 10;
    /**
     * 可选攻击距离
     */
    public static final int[] RANGES = {8, 16, 24, 32};

    // ==================== NBT 键 ====================
    public static final String TAG_KILL_ALL = "kill_all";
    public static final String TAG_RANGE = "range";
    public static final String TAG_ITEMS = "items";
    public static final String TAG_DAMAGE = "sword_damage";

    public EternalSwordItem() {
        super(SWORD_TIER, 0, -2.4F, new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    // ==================== 无耐久条 ====================
    @Override
    public boolean canBeDepleted() {
        return false;
    }

    // ==================== 属性：面板直接显示实际伤害 ====================
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> map = ArrayListMultimap.create();
        map.putAll(super.getAttributeModifiers(slot, stack));
        if (slot == EquipmentSlot.MAINHAND) {
            map.removeAll(Attributes.ATTACK_DAMAGE);
            // 玩家基础攻击 1 + modifier = 面板伤害 = 实际伤害
            map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                    getSwordDamage(stack) - 1F, AttributeModifier.Operation.ADDITION));
        }
        return map;
    }

    // ==================== 禁止附魔 ====================
    @Override
    public boolean isEnchantable(@Nonnull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(@Nonnull ItemStack stack, @Nonnull ItemStack book) {
        return false;
    }

    // ==================== 右击：范围攻击 ====================
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            player.swing(hand);
        } else {
            doAttack(level, player, stack);
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * 范围攻击：对攻击距离内的目标结算三段伤害，命中后逐个目标依次执行——
     * <ol>
     *     <li>剑自身伤害 = 槽位伤害总和 + 剑上附魔的伤害加成（锋利 / 亡灵杀手 / 节肢杀手）；</li>
     *     <li>剑上附魔的命中效果（火焰附加、击退、节肢杀手的迟缓等）；</li>
     *     <li>槽位里每把武器各对目标做一次近战命中，用<b>那把武器自己的伤害</b>与命中附魔。</li>
     * </ol>
     * 混沌守卫走反射免伤突破。
     */
    private void doAttack(Level level, Player player, ItemStack stack) {
        int range = getRange(stack);
        boolean killAll = getKillAll(stack) == 1;
        float baseDamage = getSwordDamage(stack);
        DamageSource source = level.damageSources().playerAttack(player);
        AABB aabb = player.getBoundingBox().inflate(range);
        List<ItemStack> slotWeapons = collectSlotWeapons(stack);
        // 只取可能的目标：活着的生物 或 混沌守卫（本体/部位）
        List<Entity> targets = level.getEntities(player, aabb,
                e -> e.isAlive() && (e instanceof LivingEntity || Tool.isGuardian(e)));
        for (Entity target : targets) {
            if (target instanceof Player) continue;
            if (target instanceof LivingEntity living) {
                if (living instanceof ArmorStand) continue;
                // 敌对模式：只攻击敌对生物（Enemy 接口：僵尸、骷髅、苦力怕、末影龙、守卫者等）
                if (!killAll && !(living instanceof Enemy)) continue;
                // 第一段：剑自身伤害
                float damage = baseDamage + EnchantmentHelper.getDamageBonus(stack, living.getMobType());
                // 混沌守卫本体：反射突破免伤，命中则跳过普通伤害
                if (Tool.bypassGuardianDamage(living, source, damage)) continue;
                living.hurt(source, damage);
                // 第二段：剑上附魔的命中效果（原版的 doPostHurt / doPostAttack 钩子，
                // 覆盖火焰附加、击退、节肢杀手的迟缓，无需再手写火焰附加）
                applyWeaponEffects(stack, player, living);
                // 第三段：槽位里每把武器各打一次
                for (ItemStack weapon : slotWeapons) {
                    meleeHit(weapon, player, living, source);
                }
            } else if (Tool.isGuardian(target)) {
                // 混沌守卫部位（非 LivingEntity）：反射突破免伤
                Tool.bypassGuardianDamage(target, source, baseDamage);
            }
        }
    }

    /**
     * 收集槽位里所有能造成伤害的武器，按物品 ID 去重（与剑伤害的"同 ID 只计一次"规则一致，
     * 否则在槽位里塞满同一把武器就能把近战命中次数刷上去）。
     */
    private static List<ItemStack> collectSlotWeapons(ItemStack sword) {
        List<ItemStack> weapons = new ArrayList<>();
        CompoundTag tag = sword.getTag();
        if (tag == null || !tag.contains(TAG_ITEMS)) {
            return weapons;
        }
        Set<String> seen = new HashSet<>();
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (Tag t : list) {
            ItemStack s = ItemStack.of((CompoundTag) t);
            if (s.isEmpty() || getDamageContribution(s) <= 0F) {
                continue;
            }
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (key != null && seen.add(key.toString())) {
                weapons.add(s);
            }
        }
        return weapons;
    }

    /**
     * 用指定武器对目标做一次近战命中：伤害 = 该武器攻击力 + 它自己的附魔伤害加成，再触发其命中附魔效果。
     */
    private static void meleeHit(ItemStack weapon, Player player, LivingEntity target, DamageSource source) {
        float damage = getDamageContribution(weapon) + EnchantmentHelper.getDamageBonus(weapon, target.getMobType());
        if (damage <= 0F) {
            return;
        }
        if (Tool.bypassGuardianDamage(target, source, damage)) {
            return;
        }
        target.hurt(source, damage);
        applyWeaponEffects(weapon, player, target);
    }

    /**
     * 触发武器上附魔的命中效果。
     * <p>
     * 原版 {@code EnchantmentHelper} 的两个入口都从"攻击者主手"读附魔，这里改为按附魔逐条调用
     * {@link Enchantment#doPostHurt} / {@link Enchantment#doPostAttack}，就能指定任意一把武器，
     * 也不必临时替换玩家的主手物品。
     */
    private static void applyWeaponEffects(ItemStack weapon, Player player, LivingEntity target) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(weapon);
        if (enchantments.isEmpty()) {
            return;
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            try {
                entry.getKey().doPostHurt(target, player, entry.getValue());
                entry.getKey().doPostAttack(player, target, entry.getValue());
            } catch (Throwable ignored) {
                // 单个附魔出错不影响其余
            }
        }
    }

    // ==================== tooltip ====================
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 伤害由原版"在主手时"属性行给出，这里不再重复
        Tip.of(tooltip)
                .head(stack, "tip.alltheimbaium.type.combat")
                .summary("item.alltheimbaium.eternal_sword.summary")
                .state(getKillAll(stack) == 1 ? "item.alltheimbaium.eternal_sword.state.mode_all"
                        : "item.alltheimbaium.eternal_sword.state.mode_hostile")
                .state("item.alltheimbaium.eternal_sword.state.range", getRange(stack))
                .usage("item.alltheimbaium.eternal_sword.usage.1",
                        "item.alltheimbaium.eternal_sword.usage.2",
                        "item.alltheimbaium.eternal_sword.usage.3",
                        "item.alltheimbaium.eternal_sword.usage.4",
                        "item.alltheimbaium.eternal_sword.usage.5")
                .warn("item.alltheimbaium.eternal_sword.warn.1",
                        "item.alltheimbaium.eternal_sword.warn.2");
    }

    // ==================== NBT 读写 ====================

    /**
     * 击杀模式：0=敌对生物，1=所有生物
     */
    public static int getKillAll(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getInt(TAG_KILL_ALL) == 1 ? 1 : 0;
    }

    /**
     * 攻击距离：默认 8
     */
    public static int getRange(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_RANGE)) return RANGES[0];
        int range = tag.getInt(TAG_RANGE);
        for (int r : RANGES) {
            if (r == range) return range;
        }
        return RANGES[0];
    }

    /**
     * 剑的实际伤害（由 GUI 槽位固化）：所有 ID 不同的带伤害物品伤害总和，最低 1
     */
    public static float getSwordDamage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        float damage = tag == null ? 0F : tag.getFloat(TAG_DAMAGE);
        return Math.max(1F, damage);
    }

    /**
     * 从剑 NBT 加载 27 格槽位
     */
    public static void loadInventory(ItemStack sword, SimpleContainer inv) {
        CompoundTag tag = sword.getTag();
        if (tag == null || !tag.contains(TAG_ITEMS)) return;
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag ct = (CompoundTag) t;
            int slot = ct.getByte("Slot");
            if (slot >= 0 && slot < INVENTORY_SIZE) {
                inv.setItem(slot, ItemStack.of(ct));
            }
        }
    }

    /**
     * 保存 27 格槽位到剑 NBT，并重新计算伤害与附魔
     */
    public static void saveInventory(ItemStack sword, SimpleContainer inv) {
        if (sword == null || sword.isEmpty()) return;
        CompoundTag tag = sword.getOrCreateTag();
        ListTag list = new ListTag();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag ct = new CompoundTag();
                ct.putByte("Slot", (byte) i);
                s.save(ct);
                list.add(ct);
                stacks.add(s);
            }
        }
        tag.put(TAG_ITEMS, list);
        tag.putFloat(TAG_DAMAGE, calcDamage(stacks));
        EnchantmentHelper.setEnchantments(calcEnchantments(stacks), sword);
    }

    /**
     * 单个物品对剑伤害的贡献（ATTACK_DAMAGE modifier 之和）
     */
    public static float getDamageContribution(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0F;
        float damage = 0F;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            damage += modifier.getAmount();
        }
        return damage;
    }

    /**
     * 剑伤害 = 所有 ID 不同的带伤害物品的 ATTACK_DAMAGE modifier 之和
     */
    public static float calcDamage(List<ItemStack> stacks) {
        float damage = 0F;
        Set<String> seen = new HashSet<>();
        for (ItemStack s : stacks) {
            if (s == null || s.isEmpty()) continue;
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (key == null) continue;
            if (!seen.add(key.toString())) continue;
            Collection<AttributeModifier> modifiers =
                    s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
            for (AttributeModifier modifier : modifiers) {
                damage += modifier.getAmount();
            }
        }
        return damage;
    }

    /**
     * 剑附魔 = 槽位里所有附魔书的附魔按 <b>点数</b> 累加后换算回等级。
     * <p>
     * 等级 1~10 分别计 1、2、4、8、16、32、64、128、256、512 点；每一类附魔各自累加点数，
     * 累加值够到哪一档就是哪一级，最高 {@link #MAX_ENCHANT_LEVEL} 级。
     * 例如：锋利 V + 锋利 V = 16 + 16 = 32 点 → 锋利 VI；锋利 V + 锋利 I = 17 点 → 仍是锋利 V。
     */
    public static Map<Enchantment, Integer> calcEnchantments(List<ItemStack> stacks) {
        Map<Enchantment, Long> points = new HashMap<>();
        for (ItemStack s : stacks) {
            if (s == null || s.isEmpty() || !s.is(Items.ENCHANTED_BOOK)) continue;
            for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.getEnchantments(s).entrySet()) {
                int level = Math.max(1, Math.min(MAX_ENCHANT_LEVEL, e.getValue()));
                points.merge(e.getKey(), 1L << (level - 1), Long::sum);
            }
        }
        Map<Enchantment, Integer> result = new HashMap<>();
        points.forEach((enchantment, point) -> result.put(enchantment, levelForPoints(point)));
        return result;
    }

    /**
     * 点数换算回等级：达到哪一档就是哪一级，最高 {@link #MAX_ENCHANT_LEVEL} 级。
     * 1 点 → 1 级，16 点 → 5 级，32 点 → 6 级，512 点及以上 → 10 级。
     */
    public static int levelForPoints(long points) {
        int level = 1;
        while (level < MAX_ENCHANT_LEVEL && points >= (1L << level)) {
            level++;
        }
        return level;
    }
}
