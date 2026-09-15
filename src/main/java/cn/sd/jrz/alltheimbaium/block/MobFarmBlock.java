package cn.sd.jrz.alltheimbaium.block;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootParams;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.MobFarmMarkerIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

    public static void loadConfig() {
        carry = Config.MOB_FARM_CARRY.get();
        levelUpIntervalSeconds = Config.MOB_FARM_LEVEL_UP_INTERVAL_SECONDS.get();
        maxLevel = Config.MOB_FARM_MAX_LEVEL.get();
        initialLevel = Config.MOB_FARM_INITIAL_LEVEL.get();
        captureRadius = Config.MOB_FARM_CAPTURE_RADIUS.get();
        maxProducts = Config.MOB_FARM_MAX_PRODUCTS.get();
        sampleKills = Config.MOB_FARM_SAMPLE_KILLS.get();
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
    @Nonnull
    private InteractionResult doUse(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(pos) instanceof MobFarmEntity machine)) {
                return InteractionResult.FAIL;
            }
            // 收容在手持物品阶段完成；放置后的方块右键一律打开 GUI
            if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                // 首次打开时惰性构建白名单/动态标记表，并随开屏 extraData 发给客户端供 "?" 帮助展示
                serverPlayer.openMenu(machine, buf -> {
                    buf.writeBlockPos(pos);
                    MobFarmMarkerIndex.writeToBuf(buf, serverLevel);
                });
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("MobFarmBlock.use error", e);
        }
        return InteractionResult.PASS;
    }

    /**
     * 1.21：原版的 Block#use 拆成了空手的 useWithoutItem 与持物的 useItemOn，
     * 这里两个都覆写并统一转到 doUse，行为与 1.20.1 保持一致。
     */
    @Override
    protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        return doUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    protected @Nonnull InteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        InteractionResult result = doUse(state, level, pos, player, handIn, hit);
        // 26.x：InteractionResult 是 sealed 接口，判定改用 instanceof；
        // 旧的 ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION 对应 TRY_WITH_EMPTY_HAND（表示"再试一次空手交互"）
        if (result instanceof InteractionResult.Success) {
            return InteractionResult.SUCCESS;
        }
        if (result instanceof InteractionResult.Fail) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * 1.21：掉落时把方块实体数据写进物品的 block_entity_data 组件（替代 1.20.1 战利品表的 copy_nbt）。
     */
    @Override
    @Nonnull
    public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder params) {
        return Tool.withBlockEntityData(super.getDrops(state, params), params);
    }
}