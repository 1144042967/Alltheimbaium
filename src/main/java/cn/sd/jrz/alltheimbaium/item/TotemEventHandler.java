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
            if (hasEternalTotem(player)) {
                event.setCanceled(true);
                applyTotemEffects(player);
            }
        }
    }

    private static boolean hasEternalTotem(Player player) {
        // Check main hand and off hand
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.is(Registration.ETERNAL_TOTEM.get())) {
            return true;
        }
        if (offHand.is(Registration.ETERNAL_TOTEM.get())) {
            return true;
        }

        // Check armor slots
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.is(Registration.ETERNAL_TOTEM.get())) {
                return true;
            }
        }

        // Check inventory
        for (ItemStack item : player.getInventory().items) {
            if (item.is(Registration.ETERNAL_TOTEM.get())) {
                return true;
            }
        }

        return false;
    }

    private static void applyTotemEffects(Player player) {
        // Clear all effects first (like vanilla totem)
        player.removeAllEffects();

        // Restore full health
        player.setHealth(player.getMaxHealth());

        // Add absorption effect (level 4 = 8 extra hearts for 1200 ticks = 60 seconds)
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 3));

        // Add regeneration effect (level 3 for 1200 ticks = 60 seconds)
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2));

        // Add fire resistance (60 seconds)
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));

        // Add resistance (60 seconds) - reduce damage by 20% per level
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1));

        // Add speed (60 seconds)
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1));

        // Add strength (60 seconds)
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));

        // Send totem animation packet
        player.level().broadcastEntityEvent(player, (byte) 35);

        // Note: Unlike normal totem, we do NOT consume the item
    }
}
