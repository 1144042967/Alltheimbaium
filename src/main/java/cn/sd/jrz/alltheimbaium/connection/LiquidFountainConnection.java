package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class LiquidFountainConnection implements IFluidHandler {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainConnection.class);
    private final LiquidFountainEntity owner;

    public LiquidFountainConnection(LiquidFountainEntity owner) {
        this.owner = owner;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @Nonnull FluidStack getFluidInTank(int i) {
        try {
            return owner.stack.copy();
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.getFluidInTank error", e);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int i) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int i, @Nonnull FluidStack fluidStack) {
        try {
            return !isInfinity() && (owner.stack == FluidStack.EMPTY || fluidStack.isFluidEqual(owner.stack));
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.isFluidValid error", e);
        }
        return false;
    }

    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction) {
        try {
            if (isInfinity()) {
                return 0;
            }
            if (fluidStack.getAmount() <= 0) {
                return 0;
            }
            if (owner.stack != FluidStack.EMPTY && !owner.stack.isFluidEqual(fluidStack)) {
                return 0;
            }
            int maxInput = Math.min(fluidStack.getAmount(), Tool.suitInt(LiquidFountainBlock.getMax() - owner.stack.getAmount()));
            if (fluidAction.execute()) {
                if (owner.stack == FluidStack.EMPTY) {
                    owner.stack = new FluidStack(fluidStack.getFluid(), maxInput);
                } else {
                    owner.stack.grow(maxInput);
                }
                owner.setChanged();
            }
            return maxInput;
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.fill error", e);
        }
        return 0;
    }

    @Override
    public @Nonnull FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
        try {
            if (!owner.stack.isFluidEqual(fluidStack)) {
                return FluidStack.EMPTY;
            }
            return drain(fluidStack.getAmount(), fluidAction);
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.fill error", e);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public @Nonnull FluidStack drain(int amount, FluidAction fluidAction) {
        try {
            if (isInfinity()) {
                FluidStack copy = owner.stack.copy();
                copy.setAmount(amount);
                return copy;
            }
            if (owner.stack == FluidStack.EMPTY) {
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
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.drain error", e);
        }
        return FluidStack.EMPTY;
    }

    private boolean isInfinity() {
        return owner.stack != FluidStack.EMPTY && owner.stack.getAmount() >= LiquidFountainBlock.getMax();
    }
}
