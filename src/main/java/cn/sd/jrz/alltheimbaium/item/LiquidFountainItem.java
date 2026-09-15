package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * ATI 液体无限制造机物品。
 * <p>
 * tooltip 说明三件非显然的事：只接受单一流体、未达阈值时不会主动输出但仍可被管道抽走、
 * 以及手持容器右键可直接装取。
 */
public class LiquidFountainItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainItem.class);

    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public LiquidFountainItem(Block block, Item.Properties properties) {
        super(block, properties.rarity(Rarity.EPIC).fireResistant());
    }

    /**
     * 26.x：tooltip 出口由 List&lt;Component&gt; 换成 Consumer&lt;Component&gt;，@OnlyIn 已删除
     */
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context,
                                @Nonnull TooltipDisplay display, @Nonnull Consumer<Component> tooltip,
                                @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        try {
            FluidStack fluidStack = FluidStack.EMPTY;
            CompoundTag tag = Tool.getBlockEntityTag(stack);
                if (tag != null) {
                if (tag != null) {
                    // 26.x：CompoundTag 的取值方法一律返回 Optional，"按类型判断"的 contains 重载已删除，
                    // 带默认值的读法统一走 getXOr
                    String fluidId = tag.getStringOr("fluid_id", "");
                    if (!fluidId.isEmpty()) {
                        Fluid fluid = null;
                        try {
                            //noinspection deprecation
                            Identifier parsed = Identifier.tryParse(fluidId);
                            // 26.x：Registry#get 返回 Optional<Holder.Reference>，取实际对象改走 getValue
                            // （FLUID 是"带默认值"的注册表，查不到时返回 Fluids.EMPTY，不会为 null）
                            fluid = parsed == null ? null : BuiltInRegistries.FLUID.getValue(parsed);
                        } catch (Exception ignored) {
                        }
                        if (fluid != null && fluid != Fluids.EMPTY) {
                            fluidStack = new FluidStack(fluid, 0);
                        }
                    }
                    if (fluidStack != FluidStack.EMPTY) {
                        fluidStack.setAmount(tag.getIntOr("fluid_amount", 0));
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
                        fluidStack.getHoverName().getString(),
                        String.format("%,d", fluidStack.getAmount()),
                        String.format("%,d", max));
            } else {
                tip.state("item.alltheimbaium.liquid_fountain.state.infinite",
                        fluidStack.getHoverName().getString());
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
