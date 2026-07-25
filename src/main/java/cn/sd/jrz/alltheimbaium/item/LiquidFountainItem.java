package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

public class LiquidFountainItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainItem.class);

    public LiquidFountainItem(Block block) {
        super(block, new Properties().fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        try {
            FluidStack fluidStack = FluidStack.EMPTY;
            String blockData = stack.getOrDefault(Registration.BLOCK_DATA.get(), "");
            if (!blockData.isEmpty()) {
                String[] dataArray = blockData.split(",");
                if (dataArray.length >= 2) {
                    fluidStack = new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(dataArray[0])), (int) Tool.suit(dataArray[1]));
                }
            }
            if (fluidStack.isEmpty()) {
                tooltip.add(Component.translatable("screen.alltheimbaium.liquid.fountain.empty", String.format("%,d", LiquidFountainBlock.MAX)));
                return;
            }
            if (fluidStack.getAmount() < LiquidFountainBlock.MAX) {
                tooltip.add(Component.translatable("screen.alltheimbaium.liquid.fountain.current", fluidStack.getHoverName(), fluidStack.getAmount(), LiquidFountainBlock.MAX));
                return;
            }
            tooltip.add(Component.translatable("screen.alltheimbaium.liquid.fountain.max", fluidStack.getHoverName()));
        } catch (Throwable e) {
            log.error("LiquidFountainItem.appendHoverText error", e);
        }
    }
}
