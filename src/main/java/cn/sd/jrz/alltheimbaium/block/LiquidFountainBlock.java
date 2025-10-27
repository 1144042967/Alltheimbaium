package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LiquidFountainBlock extends Block implements EntityBlock {
    public static final Integer MAX = 10000 * 1000;

    public LiquidFountainBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LiquidFountainEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> tick(l, tile);
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof LiquidFountainEntity generator)) {
            return;
        }
        if (generator.stack == FluidStack.EMPTY || generator.stack.getAmount() < LiquidFountainBlock.MAX) {
            if (generator.stack.getAmount() <= 0) {
                generator.stack = FluidStack.EMPTY;
            }
            return;
        }
        generator.stack.setAmount(LiquidFountainBlock.MAX);
        BlockPos blockPos = generator.getBlockPos();
        //传输
        for (Direction direction : Direction.values()) {
            BlockEntity entity = level.getBlockEntity(blockPos.relative(direction));
            if (entity == null) {
                continue;
            }
            IFluidHandler handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).resolve().orElse(null);
            if (handler == null) {
                continue;
            }
            FluidStack stack = generator.stack;
            stack.setAmount(Integer.MAX_VALUE);
            handler.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        }
        generator.setChanged();
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        LiquidFountainEntity generator = (LiquidFountainEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        FluidStack stack = generator.stack;
        if (stack == FluidStack.EMPTY) {
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.empty"));
            return InteractionResult.SUCCESS;
        }
        if (stack.getAmount() < LiquidFountainBlock.MAX) {
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.current", stack.getDisplayName(), stack.getAmount(), LiquidFountainBlock.MAX));
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.max", stack.getDisplayName()));
        return InteractionResult.SUCCESS;
    }
}
