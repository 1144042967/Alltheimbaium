package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.block.AlltheimbaiumFarmlandBlock;
import cn.sd.jrz.alltheimbaium.block.ClockBlock;
import cn.sd.jrz.alltheimbaium.setup.Config;
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
    // 运行时状态，初始值由 loadConfig() 从配置文件读取后设置
    private static boolean active = false;
    private final int speedMultiplier;

    /** 由 Config.onConfigLoad() 在配置文件加载完成后调用，设置时钟初始开关状态 */
    public static void loadConfig() {
        active = Config.CLOCK_DEFAULT_ACTIVE.get();
    }

    public ClockEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<?>> supplier, int speedMultiplier) {
        super(supplier.get(), pos, state);
        this.speedMultiplier = speedMultiplier;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        ClockEntity.active = active;
    }

    public int getSpeedMultiplier() {
        return speedMultiplier;
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
                for (int i = 1; i < speedMultiplier; i++) {
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