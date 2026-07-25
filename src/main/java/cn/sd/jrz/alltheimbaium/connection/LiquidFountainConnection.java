package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class LiquidFountainConnection implements IFluidHandler {
    private final LiquidFountainEntity owner;

    public LiquidFountainConnection(LiquidFountainEntity owner) {
        this.owner = owner;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int i) {
        return owner.stack.copy();
    }

    @Override
    public int getTankCapacity(int i) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int i, @NotNull FluidStack fluidStack) {
        return !isInfinity() && (owner.stack.isEmpty() || FluidStack.isSameFluidSameComponents(fluidStack, owner.stack));
    }

    @Override
    public int fill(@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
        if (isInfinity()) {
            return 0;
        }
        if (fluidStack.getAmount() <= 0) {
            return 0;
        }
        if (!owner.stack.isEmpty() && !FluidStack.isSameFluidSameComponents(owner.stack, fluidStack)) {
            return 0;
        }
        int maxInput = Math.min(fluidStack.getAmount(), LiquidFountainBlock.getMax() - owner.stack.getAmount());
        if (fluidAction.execute()) {
            if (owner.stack.isEmpty()) {
                owner.stack = new FluidStack(fluidStack.getFluid(), maxInput);
            } else {
                owner.stack.grow(maxInput);
            }
            owner.setChanged();
        }
        return maxInput;
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
        if (!FluidStack.isSameFluidSameComponents(owner.stack, fluidStack)) {
            return FluidStack.EMPTY;
        }
        return drain(fluidStack.getAmount(), fluidAction);
    }

    @Override
    public @NotNull FluidStack drain(int amount, @NotNull FluidAction fluidAction) {
        if (isInfinity()) {
            FluidStack copy = owner.stack.copy();
            copy.setAmount(amount);
            return copy;
        }
        if (owner.stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int output = owner.stack.getAmount();
        if (output <= 0 || amount <= 0) {
            return FluidStack.EMPTY;
        }
        int maxOutput = Math.min(output, amount);
        if (fluidAction.execute()) {
            owner.stack.grow(-maxOutput);
            owner.setChanged();
        }
        FluidStack copy = owner.stack.copy();
        copy.setAmount(maxOutput);
        return copy;
    }

    private boolean isInfinity() {
        return !owner.stack.isEmpty() && owner.stack.getAmount() >= LiquidFountainBlock.getMax();
    }
}
