package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 加速时钟物品。
 */
public class ClockItem extends BlockItem {

    public ClockItem(Block block) {
        super(block, new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        Tip.of(tooltip)
                .head(stack, "tip.alltheimbaium.type.accelerator")
                .summary("item.alltheimbaium.clock.summary")
                .usage("item.alltheimbaium.clock.usage.1",
                        "item.alltheimbaium.clock.usage.2",
                        "item.alltheimbaium.clock.usage.3")
                .warn("item.alltheimbaium.clock.warn.1");
    }
}
