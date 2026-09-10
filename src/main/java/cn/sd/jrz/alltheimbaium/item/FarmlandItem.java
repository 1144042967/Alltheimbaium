package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
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
 * ATI 耕地物品。
 */
public class FarmlandItem extends BlockItem {

    public FarmlandItem(Block block, Properties properties) {
        super(block, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        Tip.of(tooltip)
                .head(stack, "tip.alltheimbaium.type.farmland")
                .summary("item.alltheimbaium.farmland.summary")
                .usage("item.alltheimbaium.farmland.usage.1",
                        "item.alltheimbaium.farmland.usage.2",
                        "item.alltheimbaium.farmland.usage.3")
                .warn("item.alltheimbaium.farmland.warn.1");
    }
}
