package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
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
 * ATI 零刻压印器方块：读取 AE2 压印机配方，压板/组装双模式即时生成（消耗 FE）。
 * 右键打开 GUI；tick 仅在服务端把输出行产物转给启用"推送"的相邻面。
 */
public class InstantInscriberBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(InstantInscriberBlock.class);

    public InstantInscriberBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new InstantInscriberEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                if (tile instanceof InstantInscriberEntity inscriber) {
                    inscriber.serverTick();
                }
            } catch (Throwable e) {
                log.error("InstantInscriberBlock.getTicker error", e);
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
            if (!(level.getBlockEntity(pos) instanceof InstantInscriberEntity inscriber)) {
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, inscriber, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("InstantInscriberBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
