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
 * ATI 取出接口：聚合相邻本 MOD 产物/流体机器，供管道被动抽取。
 * 无 GUI（不覆写 use）、无主动 tick；仅提供只读能力，见 {@link ExtractionInterfaceEntity}。
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

    /** 无需 tick：抽取聚合在每次能力查询时实时计算 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return null;
    }
}
