package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity;
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
 * ATI 零刻熔炉方块：18 输入槽 + 18 输出槽，放入原料即时熔炼（消耗 FE，无耗时）。
 * 右键打开配置 GUI；方块 tick 仅在服务端把输出槽成品自动转给启用"推送"的相邻面。
 */
public class InstantFurnaceBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(InstantFurnaceBlock.class);

    public InstantFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new InstantFurnaceEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                if (tile instanceof InstantFurnaceEntity furnace) {
                    furnace.serverTick();
                }
            } catch (Throwable e) {
                log.error("InstantFurnaceBlock.getTicker error", e);
            }
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(pos) instanceof InstantFurnaceEntity furnace)) {
                return InteractionResult.FAIL;
            }
            // 右键打开 GUI
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, furnace, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("InstantFurnaceBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
