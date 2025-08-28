package cn.sd.jrz.alltheimbaium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class CommonEntity extends BlockEntity {
    public CommonEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<?>> supplier) {
        super(supplier.get(), pos, state);
    }
}
