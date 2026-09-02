package cn.sd.jrz.alltheimbaium.setup;

import cn.sd.jrz.alltheimbaium.block.PlatformBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 生成平台相关世界事件。
 * <p>
 * - 平台区块加载时不能直接写世界（会与主线程的区块加载任务互相等待而死锁），
 *   因此只把待纠正区块登记入队，在后续服务端 tick 里再扫描纠正；
 * - 平台方块被放置/破坏时维护追踪集合；
 * - 服务端关闭时清空追踪与待处理队列。
 */
@Mod.EventBusSubscriber(modid = "alltheimbaium")
public class PlatformEvents {
    private static final Logger log = LoggerFactory.getLogger(PlatformEvents.class);
    /** 每个服务端 tick 最多处理的待纠正区块数（避免一次性积压太多造成卡顿） */
    private static final int MAX_PENDING_PER_TICK = 256;
    private record Pending(ServerLevel level, ChunkPos pos) {
    }

    private static final Deque<Pending> PENDING = new ArrayDeque<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        try {
            if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (!(event.getChunk() instanceof LevelChunk chunk)) {
                return;
            }
            // 仅登记，不在此处写世界（避免区块加载阶段写方块导致死锁）
            PENDING.add(new Pending(serverLevel, chunk.getPos()));
        } catch (Throwable e) {
            log.error("PlatformEvents.onChunkLoad error", e);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        try {
            int handled = 0;
            while (!PENDING.isEmpty() && handled < MAX_PENDING_PER_TICK) {
                Pending pending = PENDING.poll();
                if (pending == null) {
                    break;
                }
                LevelChunk chunk = pending.level().getChunkSource().getChunkNow(pending.pos().x, pending.pos().z);
                if (chunk != null) {
                    PlatformBlock.correctChunkDisguise(pending.level(), chunk);
                }
                handled++;
            }
        } catch (Throwable e) {
            log.error("PlatformEvents.onServerTick error", e);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        try {
            if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (event.getState().getBlock() instanceof PlatformBlock) {
                PlatformBlock.trackPlatform(serverLevel, event.getPos());
            }
        } catch (Throwable e) {
            log.error("PlatformEvents.onBlockPlace error", e);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        try {
            if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (event.getState().getBlock() instanceof PlatformBlock) {
                PlatformBlock.untrackPlatform(serverLevel, event.getPos());
            }
        } catch (Throwable e) {
            log.error("PlatformEvents.onBlockBreak error", e);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        try {
            PENDING.clear();
            PlatformBlock.clearDisguisePositions();
        } catch (Throwable e) {
            log.error("PlatformEvents.onServerStopping error", e);
        }
    }
}
