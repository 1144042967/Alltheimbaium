package cn.sd.jrz.alltheimbaium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public class ClockEntity extends BlockEntity {
    private boolean active = true;
    private static final int SPEED_MULTIPLIER = 256;

    public ClockEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<?>> supplier) {
        super(supplier.get(), pos, state);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void tick(Level level) {
        if (!active) {
            return;
        }
        BlockPos abovePos = getBlockPos().above();
        BlockEntity aboveEntity = level.getBlockEntity(abovePos);
        if (aboveEntity == null) {
            return;
        }

        for (int i = 0; i < SPEED_MULTIPLIER; i++) {
            try {
                tickEntity(level, aboveEntity);
            } catch (Throwable e) {
                break;
            }
        }
        aboveEntity.setChanged();
    }

    private void tickEntity(Level level, BlockEntity entity) {
        BlockEntityType<?> type = entity.getType();
        BlockState state = entity.getBlockState();
        BlockPos pos = entity.getBlockPos();

        try {
            Method tickerMethod = level.getClass().getMethod("getBlockEntityTicker", BlockEntityType.class, BlockState.class, BlockEntityType.class);
            Object ticker = tickerMethod.invoke(level, type, state, type);

            if (ticker != null) {
                Method tickMethod = ticker.getClass().getMethod("tick", Level.class, BlockPos.class, BlockState.class, BlockEntity.class);
                tickMethod.invoke(ticker, level, pos, state, entity);
            }
        } catch (Exception e) {
            try {
                Method getTickerMethod = state.getBlock().getClass().getMethod("getTicker", Level.class, BlockState.class, BlockEntityType.class);
                Object ticker = getTickerMethod.invoke(state.getBlock(), level, state, type);

                if (ticker != null) {
                    Method tickMethod = ticker.getClass().getMethod("tick", Level.class, BlockPos.class, BlockState.class, BlockEntity.class);
                    tickMethod.invoke(ticker, level, pos, state, entity);
                }
            } catch (Exception ex) {
            }
        }
    }
}
