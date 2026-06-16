package cn.sd.jrz.alltheimbaium.item;

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

@Mod.EventBusSubscriber(modid = "alltheimbaium")
public class TotemEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            if (hasEternalTotem(player) && EternalTotemItem.enabled) {
                event.setCanceled(true);
                applyTotemEffects(player);
            }
        }
    }

    private static boolean hasEternalTotem(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.is(Registration.ETERNAL_TOTEM.get())) {
            return true;
        }
        if (offHand.is(Registration.ETERNAL_TOTEM.get())) {
            return true;
        }

        for (ItemStack armor : player.getInventory().armor) {
            if (armor.is(Registration.ETERNAL_TOTEM.get())) {
                return true;
            }
        }

        for (ItemStack item : player.getInventory().items) {
            if (item.is(Registration.ETERNAL_TOTEM.get())) {
                return true;
            }
        }

        return false;
    }

    private static void applyTotemEffects(Player player) {
        player.removeAllEffects();

        player.setHealth(player.getMaxHealth());

        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 3));

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2));

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1));

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1));

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));

        player.level().broadcastEntityEvent(player, (byte) 35);
    }
}
