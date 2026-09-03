package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ATI 补给点相关事件：
 * - 玩家游玩时间累计每 30 分钟，最大补给点 +1（跨会话累计）；
 * - 每获得一个成就，最大补给点 +5（recipe 类隐藏成就不计）。
 */
@Mod.EventBusSubscriber(modid = "alltheimbaium")
public class SupplyEvents {
    private static final Logger log = LoggerFactory.getLogger(SupplyEvents.class);

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (!(event.player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            // 每秒推进一次，减少读写
            if (serverPlayer.tickCount % 20 != 0) {
                return;
            }
            SupplyData.tickSecond(serverPlayer);
        } catch (Throwable e) {
            log.error("SupplyEvents.onPlayerTick error", e);
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            Advancement advancement = event.getAdvancement();
            if (advancement == null) {
                return;
            }
            // recipe 类成就不计入（玩家解锁大量配方会造成刷点）
            String path = advancement.getId().getPath();
            if (path.startsWith("recipes/") || path.endsWith("/root")) {
                return;
            }
            SupplyData.addMax(serverPlayer, 5);
            serverPlayer.sendSystemMessage(Component.translatable("chat.alltheimbaium.supply.max_grant", 5));
        } catch (Throwable e) {
            log.error("SupplyEvents.onAdvancementEarned error", e);
        }
    }
}
