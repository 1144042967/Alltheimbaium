package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiquidFountainEntity extends BlockEntity {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainEntity.class);
    public FluidStack stack = FluidStack.EMPTY;

    public LiquidFountainEntity(BlockPos pos, BlockState state) {
        super(Registration.LIQUID_FOUNTAIN_ENTITY.get(), pos, state);
    }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentInput input) {
        super.applyImplicitComponents(input);
        try {
            String blockData = input.getOrDefault(Registration.BLOCK_DATA.get(), "");
            if (blockData.isEmpty()) {
                return;
            }
            String[] dataArray = blockData.split(",");
            if (dataArray.length < 2) {
                return;
            }
            this.stack = new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(dataArray[0])), (int) Tool.suit(dataArray[1]));
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.applyImplicitComponents error", e);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder builder) {
        super.collectImplicitComponents(builder);
        try {
            builder.set(Registration.BLOCK_DATA.get(), BuiltInRegistries.FLUID.getKey(stack.getFluid()) + "," + stack.getAmount());
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.collectImplicitComponents error", e);
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        try {
            nbt.putString("fluid", BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
            nbt.putInt("amount", stack.getAmount());
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.saveAdditional error", e);
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        try {
            Fluid fluid = Fluids.EMPTY;
            if (nbt.contains("fluid", Tag.TAG_STRING)) {
                fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(nbt.getString("fluid")));
            }
            int liquid = 0;
            if (nbt.contains("amount", Tag.TAG_INT)) {
                liquid = (int) Tool.suit(nbt.getInt("amount"));
            }
            this.stack = new FluidStack(fluid, liquid);
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.loadAdditional error", e);
        }
    }
}
