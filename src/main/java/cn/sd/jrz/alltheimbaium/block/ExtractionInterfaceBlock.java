package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.ExtractionInterfaceEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ATI 取出接口：沿本 MOD 方块连通搜索，聚合范围内全部产物/流体机器，供管道被动抽取。
 * 无 GUI（不覆写 use）、无主动输出；仅提供只读能力，见 {@link ExtractionInterfaceEntity}。
 */
public class ExtractionInterfaceBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(ExtractionInterfaceBlock.class);

    public ExtractionInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ExtractionInterfaceEntity(pos, state);
    }

    /** 服务端 tick：周期性重算连通范围；客户端不参与 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                tick(l, tile);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceBlock.getTicker error", e);
            }
        };
    }

    private <T extends BlockEntity> void tick(@Nonnull Level level, @Nonnull T tile) {
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof ExtractionInterfaceEntity entity)) {
            return;
        }
        entity.serverTick();
    }
}
