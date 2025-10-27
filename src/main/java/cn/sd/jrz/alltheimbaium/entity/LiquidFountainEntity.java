package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.LiquidFountainConnection;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LiquidFountainEntity extends BlockEntity implements ICapabilityProvider {
    private final LazyOptional<LiquidFountainConnection> fecOptional = LazyOptional.of(() -> new LiquidFountainConnection(this));
    public FluidStack stack = FluidStack.EMPTY;

    public LiquidFountainEntity(BlockPos pos, BlockState state) {
        super(Registration.LIQUID_FOUNTAIN_ENTITY.get(), pos, state);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        return capability == ForgeCapabilities.ITEM_HANDLER ? fecOptional.cast() : super.getCapability(capability, direction);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (stack != FluidStack.EMPTY) {
            //noinspection deprecation
            nbt.putString("fluid_id", BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
            nbt.putInt("fluid_amount", stack.getAmount());
        } else {
            //noinspection deprecation
            nbt.putString("fluid_id", BuiltInRegistries.FLUID.getKey(Fluids.EMPTY).toString());
            nbt.putInt("fluid_amount", stack.getAmount());
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("fluid_id", Tag.TAG_STRING)) {
            Fluid fluid = null;
            try {
                //noinspection deprecation
                fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(nbt.getString("fluid_id")));
            } catch (Exception ignored) {
            }
            if (fluid != null && fluid != Fluids.EMPTY) {
                this.stack = new FluidStack(fluid, 0);
            }
        }
        if (nbt.contains("fluid_amount", Tag.TAG_INT)) {
            if (stack != FluidStack.EMPTY) {
                stack.setAmount(nbt.getInt("fluid_amount"));
            }
        }
    }
}
