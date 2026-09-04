package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.KillLootEstimator;
import cn.sd.jrz.alltheimbaium.setup.MobFarmCatalog;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 生物农场方块（玻璃罐风格）。空罐右键捕捉附近有效生物；否则右键打开 GUI。
 */
public class MobFarmBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(MobFarmBlock.class);

    // 由 Config.onConfigLoad() 调用 loadConfig() 填写的本地缓存
    static long carry;
    static long levelUpIntervalSeconds;
    static long maxLevel;
    static long initialLevel;
    static int captureRadius;
    static int maxProducts;
    static int sampleKills;
    static int useIntervalTicks;
    static int shearRegrowSeconds;

    public static void loadConfig() {
        carry = Config.MOB_FARM_CARRY.get();
        levelUpIntervalSeconds = Config.MOB_FARM_LEVEL_UP_INTERVAL_SECONDS.get();
        maxLevel = Config.MOB_FARM_MAX_LEVEL.get();
        initialLevel = Config.MOB_FARM_INITIAL_LEVEL.get();
        captureRadius = Config.MOB_FARM_CAPTURE_RADIUS.get();
        maxProducts = Config.MOB_FARM_MAX_PRODUCTS.get();
        sampleKills = Config.MOB_FARM_SAMPLE_KILLS.get();
        useIntervalTicks = Config.MOB_FARM_USE_INTERVAL_TICKS.get();
        shearRegrowSeconds = Config.MOB_FARM_SHEAR_REGROW_SECONDS.get();
    }

    public static long getCarry() {
        return carry;
    }

    public static long getLevelUpIntervalSeconds() {
        return levelUpIntervalSeconds;
    }

    public static long getMaxLevel() {
        return maxLevel;
    }

    public static long getInitialLevel() {
        return initialLevel;
    }

    public static int getCaptureRadius() {
        return captureRadius;
    }

    public static int getMaxProducts() {
        return Math.max(1, Math.min(27, maxProducts));
    }

    public static int getSampleKills() {
        return sampleKills;
    }

    public static int getUseIntervalTicks() {
        return Math.max(1, useIntervalTicks);
    }

    public static int getShearRegrowSeconds() {
        return shearRegrowSeconds;
    }

    public MobFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new MobFarmEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                if (tile instanceof MobFarmEntity machine) {
                    machine.tickServer();
                }
            } catch (Throwable e) {
                log.error("MobFarmBlock.getTicker error", e);
            }
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(pos) instanceof MobFarmEntity machine)) {
                return InteractionResult.FAIL;
            }
            // 空罐：尝试捕捉附近的生物
            if (!machine.hasContained() && player instanceof ServerPlayer) {
                if (tryCaptureNearby((ServerLevel) level, machine, player)) {
                    return InteractionResult.SUCCESS;
                }
            }
            // 否则打开 GUI
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, machine, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("MobFarmBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }

    /**
     * 空罐捕捉：范围内最近的有效生物（白名单，或采样有击杀掉落）。
     *
     * @return 是否捕捉成功
     */
    private boolean tryCaptureNearby(ServerLevel level, MobFarmEntity machine, Player player) {
        try {
            BlockPos pos = machine.getBlockPos();
            int radius = Math.max(1, getCaptureRadius());
            AABB box = new AABB(pos).inflate(radius);
            List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive()
                            && !(e instanceof Player)
                            && !e.isRemoved()
                            && (MobFarmCatalog.isWhitelisted(e.getType())
                            || KillLootEstimator.hasAnyDrop(level, e.getType())));
            if (list.isEmpty()) {
                return false;
            }
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            LivingEntity nearest = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : list) {
                double d = e.distanceToSqr(cx, cy, cz);
                if (d < best) {
                    best = d;
                    nearest = e;
                }
            }
            if (nearest == null) {
                return false;
            }
            String name = nearest.getName().getString();
            if (machine.captureEntity(nearest)) {
                nearest.discard();
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.alltheimbaium.mob_farm.capture", name));
                }
                return true;
            }
            return false;
        } catch (Throwable e) {
            log.error("MobFarmBlock.tryCaptureNearby error", e);
        }
        return false;
    }
}
