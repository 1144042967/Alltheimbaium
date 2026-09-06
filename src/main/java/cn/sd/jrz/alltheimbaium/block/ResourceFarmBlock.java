package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.ResourceData;
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
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 通用资源农场方块。放置后右键（服务端）打开 GUI；标记在 GUI 标记槽完成。
 */
public class ResourceFarmBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(ResourceFarmBlock.class);

    public ResourceFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ResourceFarmEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                if (tile instanceof ResourceFarmEntity machine) {
                    machine.tickServer();
                }
            } catch (Throwable e) {
                log.error("ResourceFarmBlock.getTicker error", e);
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
            if (!(level.getBlockEntity(pos) instanceof ResourceFarmEntity machine)) {
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel) {
                // 随开屏 extraData 下发"标记→产物"帮助行供 GUI 展示
                NetworkHooks.openScreen(serverPlayer, machine, buf -> {
                    buf.writeBlockPos(pos);
                    int[][] rows = ResourceData.helpRows();
                    buf.writeVarInt(rows.length);
                    for (int[] row : rows) {
                        buf.writeVarInt(row[0]);            // 标记物 itemId
                        buf.writeVarInt(row.length - 1);    // 产物个数
                        for (int i = 1; i < row.length; i++) {
                            buf.writeVarInt(row[i]);
                        }
                    }
                });
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("ResourceFarmBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
