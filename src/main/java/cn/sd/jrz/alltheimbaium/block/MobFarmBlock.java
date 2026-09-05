package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 生物农场方块（玻璃罐风格）。收容在手持物品阶段完成；放置后的方块右键打开 GUI。
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
            // 收容在手持物品阶段完成；放置后的方块右键一律打开 GUI
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, machine, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("MobFarmBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
