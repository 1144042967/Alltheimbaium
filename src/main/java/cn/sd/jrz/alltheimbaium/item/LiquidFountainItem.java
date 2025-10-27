package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class LiquidFountainItem extends BlockItem {

    public LiquidFountainItem(Block block) {
        super(block, new Properties().fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        FluidStack fluidStack = FluidStack.EMPTY;
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("fluid_id", Tag.TAG_STRING)) {
                    Fluid fluid = null;
                    try {
                        //noinspection deprecation
                        fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("fluid_id")));
                    } catch (Exception ignored) {
                    }
                    if (fluid != null && fluid != Fluids.EMPTY) {
                        fluidStack = new FluidStack(fluid, 0);
                    }
                }
                if (tag.contains("fluid_amount", Tag.TAG_INT)) {
                    if (fluidStack != FluidStack.EMPTY) {
                        fluidStack.setAmount(tag.getInt("fluid_amount"));
                    }
                }
            }
        }
        if (fluidStack == FluidStack.EMPTY) {
            tooltip.add(Component.translatable("screen.alltheimbaium.liquid.fountain.empty"));
            return;
        }
        if (fluidStack.getAmount() < LiquidFountainBlock.MAX) {
            tooltip.add(Component.translatable("screen.alltheimbaium.liquid.fountain.current", fluidStack.getDisplayName(), fluidStack.getAmount(), LiquidFountainBlock.MAX));
            return;
        }
        tooltip.add(Component.translatable("screen.alltheimbaium.liquid.fountain.max", fluidStack.getDisplayName()));
    }
}
