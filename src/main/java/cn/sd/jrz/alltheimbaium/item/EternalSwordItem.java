package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

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
     * 自定义工具等级：攻击力加成 0、耐久 0、附魔等级 0，保证剑本身攻击伤害为 0。
     * <p>
     * 1.21 删除了 {@code ForgeTier}，直接实现 {@link Tier}；原 {@code getLevel()} 改名为
     * {@link Tier#getIncorrectBlocksForDrops()}，返回的是"本等级挖了也不掉落的方块"标签。
     * 剑的 Tool 组件由 {@link SwordItem} 自己写入，因此这里的取值不影响任何行为。
     */
    private static final Tier SWORD_TIER = new Tier() {
        @Override
        public int getUses() {
            return 0;
        }

        @Override
        public float getSpeed() {
            return 0.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0.0F;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.AIR);
        }
    };

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

    /**
     * 主手攻击速度：与原版剑一致
     */
    private static final float ATTACK_SPEED = -2.4F;

    public EternalSwordItem() {
        // 1.21 的 SwordItem 只有 (Tier, Properties)：伤害/攻速不再走构造器参数，
        // 而是由下面的 getDefaultAttributeModifiers 动态给出（伤害随槽位内容变化，写死进属性组件反而会被覆盖）
        super(SWORD_TIER, new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    // ==================== 无耐久条 ====================
    /**
     * 1.21 删除了 {@code canBeDepleted()}，是否耗耐久完全由 MAX_DAMAGE / DAMAGE 数据组件决定，
     * 而 {@code TieredItem} 一定会用 {@code getUses()}（0）写进 MAX_DAMAGE。这里从两个口子掐掉耐久：
     * 一是不再产生任何耐久消耗，二是让"伤害值 ≥ 上限即损毁"的判定永远不成立。
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        // 伤害值恒为 0，若上限也为 0 则 "0 >= 0" 会直接把剑销毁，这里给一个正值规避
        return 1;
    }

    // ==================== 属性：面板直接显示实际伤害 ====================
    /**
     * 1.21 删除了 {@code IItemStackExtension#getAttributeModifiers(EquipmentSlot, ItemStack)}，
     * 改为覆写 NeoForge 的 {@link Item#getDefaultAttributeModifiers(ItemStack)}（仅在物品没有
     * ATTRIBUTE_MODIFIERS 组件时生效，所以构造器里不能写 {@code .attributes(...)}）。
     */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                // 玩家基础攻击 1 + modifier = 面板伤害 = 实际伤害
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                                getSwordDamage(stack) - 1F,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                                ATTACK_SPEED,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
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
     * 三段都在同一 tick 内结算，因此每次命中前都要清掉目标的受击无敌帧（见
     * {@link #clearInvulnerable}），否则第 2 段起会被原版受击冷却吞掉。
     * 混沌守卫走反射免伤突破。
     */
    private void doAttack(Level level, Player player, ItemStack stack) {
        // 附魔伤害加成 / 命中效果都要求服务端世界（EnchantmentHelper 的 1.21 入口签名里带 ServerLevel）
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int range = getRange(stack);
        boolean killAll = getKillAll(stack) == 1;
        float baseDamage = getSwordDamage(stack);
        DamageSource source = serverLevel.damageSources().playerAttack(player);
        AABB aabb = player.getBoundingBox().inflate(range);
        List<ItemStack> slotWeapons = collectSlotWeapons(stack, serverLevel.registryAccess());
        // 只取可能的目标：活着的生物 或 混沌守卫（本体/部位）
        List<Entity> targets = serverLevel.getEntities(player, aabb,
                e -> e.isAlive() && (e instanceof LivingEntity || Tool.isGuardian(e)));
        for (Entity target : targets) {
            if (target instanceof Player) continue;
            if (target instanceof LivingEntity living) {
                if (living instanceof ArmorStand) continue;
                // 敌对模式：只攻击敌对生物（Enemy 接口：僵尸、骷髅、苦力怕、末影龙、守卫者等）
                if (!killAll && !(living instanceof Enemy)) continue;
                // 第一段：剑自身伤害（modifyDamage 就是原版给主手武器算锋利/亡灵杀手/节肢杀手加成的那一步）
                float damage = EnchantmentHelper.modifyDamage(serverLevel, stack, living, source, baseDamage);
                // 混沌守卫本体：反射突破免伤，命中则跳过普通伤害
                if (Tool.bypassGuardianDamage(living, source, damage)) continue;
                clearInvulnerable(living);
                living.hurt(source, damage);
                // 第二段：剑上附魔的命中效果（火焰附加 / 击退 / 节肢杀手的迟缓）
                applyWeaponEffects(serverLevel, stack, player, living, source);
                // 第三段：槽位里每把武器各打一次
                for (ItemStack weapon : slotWeapons) {
                    meleeHit(serverLevel, weapon, player, living, source);
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
    private static List<ItemStack> collectSlotWeapons(ItemStack sword, @Nonnull HolderLookup.Provider registries) {
        List<ItemStack> weapons = new ArrayList<>();
        CompoundTag tag = Tool.getCustomTag(sword);
        if (tag == null || !tag.contains(TAG_ITEMS)) {
            return weapons;
        }
        Set<String> seen = new HashSet<>();
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (Tag t : list) {
            if (!(t instanceof CompoundTag ct)) continue;
            ItemStack s = ItemStack.parseOptional(registries, ct);
            if (s.isEmpty() || getDamageContribution(s) <= 0F) {
                continue;
            }
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(s.getItem());
            if (key != null && seen.add(key.toString())) {
                weapons.add(s);
            }
        }
        return weapons;
    }

    /**
     * 用指定武器对目标做一次近战命中：伤害 = 该武器攻击力 + 它自己的附魔伤害加成，再触发其命中附魔效果。
     */
    private static void meleeHit(ServerLevel level, ItemStack weapon, Player player, LivingEntity target, DamageSource source) {
        float damage = EnchantmentHelper.modifyDamage(level, weapon, target, source, getDamageContribution(weapon));
        if (damage <= 0F) {
            return;
        }
        if (Tool.bypassGuardianDamage(target, source, damage)) {
            return;
        }
        clearInvulnerable(target);
        target.hurt(source, damage);
        applyWeaponEffects(level, weapon, player, target, source);
    }

    /**
     * 清除目标的受击无敌帧，让同一 tick 内的多段伤害真正叠加。
     * <p>
     * 原版 {@code LivingEntity.hurt} 在 {@code invulnerableTime > 10} 时，会直接丢弃
     * 不高于 {@code lastHurt} 的伤害（只补上超出的差额），而三段结算全部发生在同一 tick、
     * 第一段就已经把无敌帧顶到 20 刻，于是第 2 段起基本被整段吃掉，最终只有最大的一段生效。
     * 每段命中前把无敌帧归零，下一段就会走完整伤害分支。
     * <p>
     * 目标已死亡时后续段本就不会结算（{@code hurt} 开头即返回），因此不会打出超额伤害。
     */
    private static void clearInvulnerable(@Nonnull LivingEntity target) {
        target.invulnerableTime = 0;
    }

    /**
     * 触发武器上附魔的命中效果。
     * <p>
     * 1.21 把"命中时附魔生效"整体改成了数据驱动的 {@code minecraft:post_attack} 效果组件
     * （{@link Enchantment#doPostAttack}），原版 {@code Player.attack} 也是调它。逐条附魔调用就能
     * 指定任意一把武器，不必临时替换玩家的主手物品。
     * <p>
     * 参数里的 {@link EnchantmentTarget} 过滤的是原版 JSON 里的 <b>enchanted</b> 字段（"附魔挂在谁身上"）。
     * 武器命中类的附魔全部写的是 {@code enchanted: attacker}——火焰附加的点燃、节肢杀手的迟缓、
     * 引雷与风爆都是；只有 {@code enchanted: victim} 的荆棘是"被打时反伤"，挂在剑上会让剑反伤自己，
     * 因此这里只按 {@link EnchantmentTarget#ATTACKER} 调用一次。
     * <p>
     * <b>击退必须单独补</b>：它走的是 {@code minecraft:knockback} 效果组件，由
     * {@link EnchantmentHelper#modifyKnockback} 取值（原版写在 {@code Player.attack} 里，
     * 取到的值再乘 0.5 沿攻击者朝向推开），不在 post_attack 里，所以这里显式复刻原版行为。
     */
    private static void applyWeaponEffects(ServerLevel level, ItemStack weapon, Player player, LivingEntity target, DamageSource source) {
        // 附魔书的附魔在 STORED_ENCHANTMENTS 组件里，统一走 getEnchantmentsForCrafting
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(weapon);
        if (enchantments.isEmpty()) {
            return;
        }
        // 击退：基准传 0 即"只算附魔带来的那一份"，强度与附魔等级成正比（与原版一致）
        float knockback = EnchantmentHelper.modifyKnockback(level, weapon, target, source, 0.0F);
        if (knockback > 0.0F) {
            double angle = Math.toRadians(player.getYRot());
            target.knockback(knockback * 0.5D, Math.sin(angle), -Math.cos(angle));
        }
        EnchantedItemInUse inUse = new EnchantedItemInUse(weapon, EquipmentSlot.MAINHAND, player);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            int enchantmentLevel = enchantments.getLevel(holder);
            if (enchantmentLevel <= 0) {
                continue;
            }
            try {
                holder.value().doPostAttack(level, enchantmentLevel, inUse, EnchantmentTarget.ATTACKER, target, source);
            } catch (Throwable ignored) {
                // 单个附魔出错不影响其余
            }
        }
    }

    // ==================== tooltip ====================
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
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

    // ==================== 自定义数据读写 ====================
    // 1.20.1 的 getTag()/getOrCreateTag() 已删除，改走 CUSTOM_DATA 数据组件（见 Tool）。
    // 组件里的 tag 是副本，改完必须 Tool.setCustomTag 写回。

    /**
     * 击杀模式：0=敌对生物，1=所有生物
     */
    public static int getKillAll(ItemStack stack) {
        CompoundTag tag = Tool.getCustomTag(stack);
        return tag != null && tag.getInt(TAG_KILL_ALL) == 1 ? 1 : 0;
    }

    /**
     * 攻击距离：默认 8
     */
    public static int getRange(ItemStack stack) {
        CompoundTag tag = Tool.getCustomTag(stack);
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
        CompoundTag tag = Tool.getCustomTag(stack);
        float damage = tag == null ? 0F : tag.getFloat(TAG_DAMAGE);
        return Math.max(1F, damage);
    }

    /**
     * 从剑的自定义数据加载 27 格槽位
     *
     * @param registries 解析物品栈需要的注册表（1.21 的物品序列化带数据组件，取世界注册表）
     */
    public static void loadInventory(ItemStack sword, SimpleContainer inv, @Nonnull HolderLookup.Provider registries) {
        CompoundTag tag = Tool.getCustomTag(sword);
        if (tag == null || !tag.contains(TAG_ITEMS)) return;
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (Tag t : list) {
            if (!(t instanceof CompoundTag ct)) continue;
            int slot = ct.getByte("Slot");
            if (slot >= 0 && slot < INVENTORY_SIZE) {
                inv.setItem(slot, ItemStack.parseOptional(registries, ct));
            }
        }
    }

    /**
     * 保存 27 格槽位到剑的自定义数据，并重新计算伤害与附魔
     *
     * @param registries 序列化物品栈需要的注册表
     */
    public static void saveInventory(ItemStack sword, SimpleContainer inv, @Nonnull HolderLookup.Provider registries) {
        if (sword == null || sword.isEmpty()) return;
        CompoundTag tag = Tool.getCustomTagOrEmpty(sword);
        ListTag list = new ListTag();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag ct = new CompoundTag();
                ct.putByte("Slot", (byte) i);
                // 1.21 的物品 NBT 必须由带注册表的 save 产生（附魔书等组件要查数据包注册表）
                ct.merge((CompoundTag) s.save(registries));
                list.add(ct);
                stacks.add(s);
            }
        }
        tag.put(TAG_ITEMS, list);
        tag.putFloat(TAG_DAMAGE, calcDamage(stacks));
        Tool.setCustomTag(sword, tag);
        // 附魔写进数据组件：SHARPNESS 之类现在是数据包注册表里的条目，键类型是 Holder<Enchantment>
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        calcEnchantments(stacks).forEach(mutable::set);
        EnchantmentHelper.setEnchantments(sword, mutable.toImmutable());
    }

    /**
     * 单个物品对剑伤害的贡献（主手 ATTACK_DAMAGE modifier 之和）
     */
    public static float getDamageContribution(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0F;
        float damage = 0F;
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (entry.slot() == EquipmentSlotGroup.MAINHAND && entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                damage += entry.modifier().amount();
            }
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
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(s.getItem());
            if (key == null) continue;
            if (!seen.add(key.toString())) continue;
            damage += getDamageContribution(s);
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
    public static Map<Holder<Enchantment>, Integer> calcEnchantments(List<ItemStack> stacks) {
        Map<Holder<Enchantment>, Long> points = new HashMap<>();
        for (ItemStack s : stacks) {
            if (s == null || s.isEmpty() || !s.is(Items.ENCHANTED_BOOK)) continue;
            // 附魔书的附魔在 STORED_ENCHANTMENTS 组件里，必须走 getEnchantmentsForCrafting
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(s);
            for (Holder<Enchantment> holder : enchantments.keySet()) {
                int level = Math.max(1, Math.min(MAX_ENCHANT_LEVEL, enchantments.getLevel(holder)));
                points.merge(holder, 1L << (level - 1), Long::sum);
            }
        }
        Map<Holder<Enchantment>, Integer> result = new HashMap<>();
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
