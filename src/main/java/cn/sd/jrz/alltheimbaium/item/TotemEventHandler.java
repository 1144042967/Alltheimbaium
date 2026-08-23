package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.CuriosHelper;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 永恒图腾事件处理：
 * - 死亡时触发复活（基础效果 + 药水槽效果）
 * - 右键 Mekanism 终极化学品储罐升级为创造化学品储罐（保留原功能）
 */
@Mod.EventBusSubscriber(modid = "alltheimbaium")
public class TotemEventHandler {

    // 从配置文件加载的本地缓存值，由 Config.onConfigLoad() 在配置加载后调用 loadConfig() 填入
    private static boolean tankConversion;

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用
     */
    public static void loadConfig() {
        tankConversion = Config.ETERNAL_TOTEM_TANK_CONVERSION.get();
    }

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

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!tankConversion) {
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
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null || !blockId.equals(EternalTotemItem.ULTIMATE_CHEMICAL_TANK)) {
            return;
        }
        if (level.isClientSide) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        Block creativeTank = ForgeRegistries.BLOCKS.getValue(EternalTotemItem.CREATIVE_CHEMICAL_TANK);
        if (creativeTank == null) {
            return;
        }
        level.setBlock(pos, creativeTank.defaultBlockState(), 3);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
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
     * 基础效果之后，逐个应用图腾 27 格药水槽位中药水的效果
     */
    private static void applyPotionEffects(Player player) {
        ItemStack totem = findTotem(player);
        if (!totem.isEmpty()) {
            EternalTotemItem.applyPotionEffects(player, totem);
        }
    }
}
