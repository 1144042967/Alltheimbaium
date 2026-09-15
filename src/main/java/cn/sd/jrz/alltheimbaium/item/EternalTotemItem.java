package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 永恒图腾。
 * <p>
 * - 默认配置启用状态，无右键开关
 * - 死亡后：清除效果、血量变为 1，获得 40 秒抗火 / 45 秒生命恢复 II / 5 秒伤害吸收 II
 * - 基础效果之后，再逐个应用 27 格药水槽位中药水的效果
 * - ALT+右击打开配置界面（27 格药水 / 食物槽）
 */
public class EternalTotemItem extends Item {

    // 运行时状态，初始值由 loadConfig() 从配置文件读取后设置
    public static boolean enabled = false;

    // ==================== 常量 ====================
    /**
     * 药水 / 食物槽位数量
     */
    public static final int STORAGE_INVENTORY_SIZE = 27;
    /**
     * 图腾 NBT 中槽位的键。
     * <p>
     * 名字保留 {@code potion_items}：槽位含义已扩为"药水 + 食物"，但改键会让旧存档里的药水丢失。
     */
    public static final String TAG_POTION_ITEMS = "potion_items";

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用，设置永恒图腾初始开关状态
     */
    public static void loadConfig() {
        enabled = Config.ETERNAL_TOTEM_DEFAULT_ENABLED.get();
    }

    /**
     * 26.x：注册 id 由 Registration 的 itemProps(key) 灌进属性里，构造器只负责堆叠上限/品级等自身属性
     */
    public EternalTotemItem(Item.Properties properties) {
        super(properties
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    /**
     * 26.x：{@code InteractionResultHolder} 已删除，改用 {@link InteractionResult}
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 已移除右键开关功能，仅保留右键动画
        if (level.isClientSide()) {
            player.swing(hand);
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
    }

    /**
     * 26.x：tooltip 出口由 List&lt;Component&gt; 换成 Consumer&lt;Component&gt;，@OnlyIn 已删除
     */
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context,
                                @Nonnull TooltipDisplay display, @Nonnull Consumer<Component> tooltip,
                                @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        Tip.of(tooltip)
                .head(stack, "tip.alltheimbaium.type.survival")
                .summary("item.alltheimbaium.eternal_totem.summary")
                .usage("item.alltheimbaium.eternal_totem.usage.1",
                        "item.alltheimbaium.eternal_totem.usage.2",
                        "item.alltheimbaium.eternal_totem.usage.3",
                        "item.alltheimbaium.eternal_totem.usage.4")
                .warn("item.alltheimbaium.eternal_totem.warn.1");
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }

    // ==================== 药水 / 食物槽位数据 ====================
    // 1.20.1 的 getTag()/getOrCreateTag() 已删除，改走 CUSTOM_DATA 数据组件（见 Tool）。
    // 组件里的 tag 是副本，改完必须 Tool.setCustomTag 写回。
    // 26.x：ItemStack.save / ItemStack.parseOptional 也已删除，物品栈一律走 ItemStack.CODEC +
    //       带注册表的序列化上下文（附魔、药水这类组件按注册名书写，与注册表实例无关）。

    /**
     * 物品栈 → NBT；编码失败只丢这一格
     */
    @Nonnull
    private static CompoundTag encodeStack(@Nonnull ItemStack stack, @Nonnull DynamicOps<Tag> ops) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        try {
            return (CompoundTag) ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result()
                    .orElseGet(CompoundTag::new);
        } catch (Throwable e) {
            return new CompoundTag();
        }
    }

    /**
     * NBT → 物品栈；解不出来只当空格处理
     */
    @Nonnull
    private static ItemStack decodeStack(@Nonnull CompoundTag tag, @Nonnull DynamicOps<Tag> ops) {
        try {
            return ItemStack.OPTIONAL_CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
        } catch (Throwable e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * 从图腾的自定义数据加载 27 格药水 / 食物槽位
     *
     * @param registries 解析物品栈需要的注册表（物品序列化带数据组件，取世界注册表）
     */
    public static void loadPotionItems(ItemStack totem, SimpleContainer inv, @Nonnull HolderLookup.Provider registries) {
        CompoundTag tag = Tool.getCustomTag(totem);
        if (tag == null || !tag.contains(TAG_POTION_ITEMS)) return;
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = tag.getListOrEmpty(TAG_POTION_ITEMS);
        for (Tag t : list) {
            if (!(t instanceof CompoundTag ct)) continue;
            int slot = ct.getByteOr("Slot", (byte) -1);
            if (slot >= 0 && slot < STORAGE_INVENTORY_SIZE) {
                inv.setItem(slot, decodeStack(ct, ops));
            }
        }
    }

    /**
     * 保存 27 格药水 / 食物槽位到图腾的自定义数据
     *
     * @param registries 序列化物品栈需要的注册表
     */
    public static void savePotionItems(ItemStack totem, SimpleContainer inv, @Nonnull HolderLookup.Provider registries) {
        if (totem == null || totem.isEmpty()) return;
        CompoundTag tag = Tool.getCustomTagOrEmpty(totem);
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        for (int i = 0; i < STORAGE_INVENTORY_SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag ct = new CompoundTag();
                ct.putByte("Slot", (byte) i);
                ct.merge(encodeStack(s, ops));
                list.add(ct);
            }
        }
        tag.put(TAG_POTION_ITEMS, list);
        Tool.setCustomTag(totem, tag);
    }

    /**
     * 读取图腾 27 格药水 / 食物槽位中的非空物品
     */
    public static List<ItemStack> getPotionItems(ItemStack totem, @Nonnull HolderLookup.Provider registries) {
        List<ItemStack> list = new ArrayList<>();
        CompoundTag tag = Tool.getCustomTag(totem);
        if (tag == null || !tag.contains(TAG_POTION_ITEMS)) return list;
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag nbt = tag.getListOrEmpty(TAG_POTION_ITEMS);
        for (Tag t : nbt) {
            if (!(t instanceof CompoundTag ct)) continue;
            ItemStack s = decodeStack(ct, ops);
            if (!s.isEmpty()) {
                list.add(s);
            }
        }
        return list;
    }

    /**
     * 应用图腾 27 格药水 / 食物槽位中药水的效果到玩家。
     * <p>
     * 1.21 删除了 {@code PotionUtils}：药水效果改由数据组件 {@link DataComponents#POTION_CONTENTS} 提供，
     * {@link PotionContents#getAllEffects()} 与旧的 {@code PotionUtils.getMobEffects} 等价。
     */
    public static void applyPotionEffects(Player player, ItemStack totem) {
        for (ItemStack s : getPotionItems(totem, player.level().registryAccess())) {
            if (s.isEmpty()) continue;
            PotionContents contents = s.get(DataComponents.POTION_CONTENTS);
            if (contents == null) continue;
            for (MobEffectInstance effect : contents.getAllEffects()) {
                player.addEffect(effect);
            }
        }
    }

    /**
     * 挨个"食用"图腾槽位里的食物：给予营养并触发食物自带效果（如金苹果的生命恢复 / 伤害吸收）。
     * <p>
     * 直接调用物品自己的 {@code finishUsingItem}，因此苹果、金苹果乃至任何自定义食物都按原版
     * 食用逻辑处理；**忽略返回值、不消耗物品**，每次复活都能再用。
     * <p>
     * 1.21 没有 {@code isEdible()}，改用"有没有 FOOD 数据组件"判断。
     */
    public static void eatStoredFood(Player player, ItemStack totem) {
        Level level = player.level();
        for (ItemStack s : getPotionItems(totem, level.registryAccess())) {
            if (s.isEmpty() || !s.has(DataComponents.FOOD)) {
                continue;
            }
            try {
                s.finishUsingItem(level, player);
            } catch (Throwable ignored) {
                // 单个食物失败不影响其余
            }
        }
    }
}
