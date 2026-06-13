package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.List;

public class ClockItem extends BlockItem {
    private final String descriptionKey;

    public ClockItem(Block block, String descriptionKey) {
        super(block, new Item.Properties());
        this.descriptionKey = descriptionKey;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        tooltip.add(Component.translatable(descriptionKey));
        super.appendHoverText(stack, context, tooltip, flagIn);
    }
}
