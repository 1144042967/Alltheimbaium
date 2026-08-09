package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 永恒图腾。
 * <p>
 * - 默认配置启用状态，无右键开关
 * - 死亡后：清除效果、血量变为 1，获得 40 秒抗火 / 45 秒生命恢复 II / 5 秒伤害吸收 II
 * - 基础效果之后，再逐个应用 27 格药水槽位中药水的效果
 * - ALT+右击打开配置界面（药水槽 + 输入/输出化学品储罐）
 */
public class EternalTotemItem extends Item {

    // 运行时状态，初始值由 loadConfig() 从配置文件读取后设置
    public static boolean enabled = false;

    // ==================== 常量 ====================
    /** 药水槽位数量 */
    public static final int POTION_INVENTORY_SIZE = 27;
    /** 图腾 NBT 中药水槽位的键 */
    public static final String TAG_POTION_ITEMS = "potion_items";
    /** Mekanism 终极化学品储罐 */
    public static final ResourceLocation ULTIMATE_CHEMICAL_TANK =
            ResourceLocation.fromNamespaceAndPath("mekanism", "ultimate_chemical_tank");
    /** Mekanism 创造化学品储罐 */
    public static final ResourceLocation CREATIVE_CHEMICAL_TANK =
            ResourceLocation.fromNamespaceAndPath("mekanism", "creative_chemical_tank");

    /** 由 Config.onConfigLoad() 在配置文件加载完成后调用，设置永恒图腾初始开关状态 */
    public static void loadConfig() {
        enabled = Config.ETERNAL_TOTEM_DEFAULT_ENABLED.get();
    }

    public EternalTotemItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 已移除右键开关功能，仅保留右键动画
        if (level.isClientSide) {
            player.swing(hand);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.alltheimbaium.eternal_totem.description"));
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }

    // ==================== 药水槽位 NBT ====================

    /** 从图腾 NBT 加载 27 格药水槽位 */
    public static void loadPotionItems(ItemStack totem, SimpleContainer inv) {
        CompoundTag tag = totem.getTag();
        if (tag == null || !tag.contains(TAG_POTION_ITEMS)) return;
        ListTag list = tag.getList(TAG_POTION_ITEMS, Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag ct = (CompoundTag) t;
            int slot = ct.getByte("Slot");
            if (slot >= 0 && slot < POTION_INVENTORY_SIZE) {
                inv.setItem(slot, ItemStack.of(ct));
            }
        }
    }

    /** 保存 27 格药水槽位到图腾 NBT */
    public static void savePotionItems(ItemStack totem, SimpleContainer inv) {
        if (totem == null || totem.isEmpty()) return;
        CompoundTag tag = totem.getOrCreateTag();
        ListTag list = new ListTag();
        for (int i = 0; i < POTION_INVENTORY_SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag ct = new CompoundTag();
                ct.putByte("Slot", (byte) i);
                s.save(ct);
                list.add(ct);
            }
        }
        tag.put(TAG_POTION_ITEMS, list);
    }

    /** 读取图腾 27 格药水槽位中的非空物品 */
    public static List<ItemStack> getPotionItems(ItemStack totem) {
        List<ItemStack> list = new ArrayList<>();
        CompoundTag tag = totem.getTag();
        if (tag == null || !tag.contains(TAG_POTION_ITEMS)) return list;
        ListTag nbt = tag.getList(TAG_POTION_ITEMS, Tag.TAG_COMPOUND);
        for (Tag t : nbt) {
            ItemStack s = ItemStack.of((CompoundTag) t);
            if (!s.isEmpty()) {
                list.add(s);
            }
        }
        return list;
    }

    /** 应用图腾 27 格药水槽位中药水的效果到玩家 */
    public static void applyPotionEffects(Player player, ItemStack totem) {
        for (ItemStack s : getPotionItems(totem)) {
            if (s.isEmpty()) continue;
            for (MobEffectInstance effect : PotionUtils.getMobEffects(s)) {
                player.addEffect(effect);
            }
        }
    }
}
