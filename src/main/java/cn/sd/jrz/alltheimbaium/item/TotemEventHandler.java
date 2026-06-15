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
        player.setHealth(1.0f);

        // Clear all effects first (like vanilla totem)
        player.removeAllEffects();

        // Add absorption effect (2 hearts for 100 ticks)
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));

        // Add regeneration effect (II for 900 ticks = 45 seconds)
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));

        // Add fire resistance (40 seconds)
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

        // Send totem animation packet
        player.level().broadcastEntityEvent(player, (byte) 35);

        // Note: Unlike normal totem, we do NOT consume the item
    }
}
