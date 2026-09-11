package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.CuriosHelper;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒图腾事件处理：
 * - 死亡时触发复活（基础效果 + 药水槽效果）
 */
@Mod.EventBusSubscriber(modid = "alltheimbaium")
public class TotemEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            if (!findTotem(player).isEmpty() && EternalTotemItem.enabled) {
                event.setCanceled(true);
                // 基础复活效果
                applyTotemEffects(player);
                // 基础效果之后，再逐个应用药水槽位中药水的效果
                applyPotionEffects(player);
            }
        }
    }

    /**
     * 在玩家物品栏与 Curios 饰品槽中查找永恒图腾（主手 → 副手 → 盔甲 → 背包 → Curios）
     */
    private static ItemStack findTotem(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Registration.ETERNAL_TOTEM.get())) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(Registration.ETERNAL_TOTEM.get())) {
            return offHand;
        }
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.is(Registration.ETERNAL_TOTEM.get())) {
                return armor;
            }
        }
        for (ItemStack item : player.getInventory().items) {
            if (item.is(Registration.ETERNAL_TOTEM.get())) {
                return item;
            }
        }
        // Curios 饰品槽（软依赖，未装时返回空）
        return CuriosHelper.findCurioItem(player, Registration.ETERNAL_TOTEM.get());
    }

    /**
     * 基础复活效果：清除效果、血量变为 1、获得 40 秒抗火 / 45 秒生命恢复 II / 5 秒伤害吸收 II
     */
    private static void applyTotemEffects(Player player) {
        player.removeAllEffects();
        player.setHealth(1F);

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));   // 40秒 抗火
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));      // 45秒 生命恢复 II
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));        // 5秒 伤害吸收 II

        player.level().broadcastEntityEvent(player, (byte) 35);
    }

    /**
     * 基础效果之后：先逐个应用图腾槽位中药水的效果，再挨个"食用"槽位里的食物。
     * 两者都只读取、不消耗，物品原样留在槽位里。
     */
    private static void applyPotionEffects(Player player) {
        ItemStack totem = findTotem(player);
        if (!totem.isEmpty()) {
            EternalTotemItem.applyPotionEffects(player, totem);
            EternalTotemItem.eatStoredFood(player, totem);
        }
    }
}
