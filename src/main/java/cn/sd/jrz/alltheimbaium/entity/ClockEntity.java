package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.block.AlltheimbaiumFarmlandBlock;
import cn.sd.jrz.alltheimbaium.block.ClockBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class ClockEntity extends BlockEntity {
    private static boolean active = false;
    private static final int SPEED_MULTIPLIER = 256;

    public ClockEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<?>> supplier) {
        super(supplier.get(), pos, state);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        ClockEntity.active = active;
    }

    public void tick(Level level) {
        if (!active || level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos pos = getBlockPos().relative(direction);
            BlockState blockState = level.getBlockState(pos);
            Block block = blockState.getBlock();
            if (level instanceof ServerLevel && block.isRandomlyTicking(blockState)) {
                blockState.randomTick((ServerLevel) level, pos, level.getRandom());
            }
            if (block instanceof ClockBlock || block instanceof AlltheimbaiumFarmlandBlock) {
                continue;
            }
            if (!(block instanceof EntityBlock entityBlock)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                //noinspection unchecked
                BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) entityBlock.getTicker(level, blockState, blockEntity.getType());
                if (blockEntity.isRemoved() || ticker == null) {
                    continue;
                }
                for (int i = 1; i < SPEED_MULTIPLIER; i++) {
                    if (blockEntity.isRemoved()) {
                        break;
                    }
                    ticker.tick(level, pos, blockState, blockEntity);
                }
            }
            BlockEntity aboveEntity = level.getBlockEntity(pos);
            if (aboveEntity != null) {
                aboveEntity.setChanged();
            }
        }
    }
}