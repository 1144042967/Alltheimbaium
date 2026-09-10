package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * ATI 液体无限制造机物品。
 * <p>
 * tooltip 说明三件非显然的事：只接受单一流体、未达阈值时不会主动输出但仍可被管道抽走、
 * 以及手持容器右键可直接装取。
 */
public class LiquidFountainItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainItem.class);

    public LiquidFountainItem(Block block) {
        super(block, new Properties().rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        try {
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
                    if (tag.contains("fluid_amount", Tag.TAG_INT) && fluidStack != FluidStack.EMPTY) {
                        fluidStack.setAmount(tag.getInt("fluid_amount"));
                    }
                }
            }
            long max = LiquidFountainBlock.getMax();
            Tip tip = Tip.of(tooltip)
                    .head(stack, "tip.alltheimbaium.type.resource")
                    .summary("item.alltheimbaium.liquid_fountain.summary");
            if (fluidStack == FluidStack.EMPTY) {
                tip.state("item.alltheimbaium.liquid_fountain.state.empty");
            } else if (fluidStack.getAmount() < max) {
                // 内部存量按 mB 计数，直接展示原始值即可，无需换算
                tip.state("item.alltheimbaium.liquid_fountain.state.fluid",
                        fluidStack.getDisplayName().getString(),
                        String.format("%,d", fluidStack.getAmount()),
                        String.format("%,d", max));
            } else {
                tip.state("item.alltheimbaium.liquid_fountain.state.infinite",
                        fluidStack.getDisplayName().getString());
            }
            tip.usage("item.alltheimbaium.liquid_fountain.usage.1",
                            "item.alltheimbaium.liquid_fountain.usage.2",
                            "item.alltheimbaium.liquid_fountain.usage.3")
                    .params()
                    .bullet("item.alltheimbaium.liquid_fountain.param.1", String.format("%,d", max))
                    .warn("item.alltheimbaium.liquid_fountain.warn.1");
        } catch (Throwable e) {
            log.error("LiquidFountainItem.appendHoverText error", e);
        }
    }
}
