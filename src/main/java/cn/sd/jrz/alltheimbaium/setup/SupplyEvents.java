package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ATI 补给点相关事件：
 * - 玩家游玩时间累计每 30 分钟，最大补给点 +1（跨会话累计）；
 * - 每获得一个成就，最大补给点 +5（recipe 类隐藏成就不计）。
 */
@EventBusSubscriber(modid = "alltheimbaium")
public class SupplyEvents {
    private static final Logger log = LoggerFactory.getLogger(SupplyEvents.class);

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        try {
            // 1.21：TickEvent 拆成 Pre/Post，订阅 Post 即原来的 Phase.END；
            // 且 PlayerEvent 的玩家字段已私有化，改走 getEntity()
            if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
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
            // 1.21：getAdvancement() 返回 AdvancementHolder（不再是 Advancement），注册名走 id()
            net.minecraft.advancements.AdvancementHolder advancement = event.getAdvancement();
            // recipe 类成就不计入（玩家解锁大量配方会造成刷点）
            String path = advancement.id().getPath();
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
