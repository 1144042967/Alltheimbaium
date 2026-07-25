package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "alltheimbaium")
public class TotemEventHandler {
    private static final ResourceLocation ULTIMATE_CHEMICAL_TANK = ResourceLocation.fromNamespaceAndPath("mekanism", "ultimate_chemical_tank");
    private static final ResourceLocation CREATIVE_CHEMICAL_TANK = ResourceLocation.fromNamespaceAndPath("mekanism", "creative_chemical_tank");


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        EternalTotemItem.init();
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            if (hasEternalTotem(player) && EternalTotemItem.enabled) {
                event.setCanceled(true);
                applyTotemEffects(player);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.ETERNAL_TOTEM_TANK_CONVERSION.get()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!event.getItemStack().is(Registration.ETERNAL_TOTEM.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!blockId.equals(ULTIMATE_CHEMICAL_TANK)) {
            return;
        }
        if (level.isClientSide) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        Block creativeTank = BuiltInRegistries.BLOCK.get(CREATIVE_CHEMICAL_TANK);
        level.setBlock(pos, creativeTank.defaultBlockState(), 3);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean hasEternalTotem(Player player) {
        for (ItemStack stack : player.getAllSlots()) {
            if (stack.is(Registration.ETERNAL_TOTEM.get())) {
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
