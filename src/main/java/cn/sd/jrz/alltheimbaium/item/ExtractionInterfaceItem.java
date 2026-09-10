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
 * 取出接口方块物品。
 * <p>
 * 方块本身没有界面、没有主动输出，只是沿本模组方块连通搜索，把找到的机器产物与流体
 * 聚合成一个只读视图供管道抽取。tooltip 只说明非显然的部分：搜索沿什么传导、谁能被抽、
 * 谁只传导，以及平台地面会断开连通。
 */
public class ExtractionInterfaceItem extends BlockItem {

    public ExtractionInterfaceItem(Block block) {
        super(block, new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        Tip.of(tooltip)
                .head(stack, "tip.alltheimbaium.type.logistics")
                .summary("item.alltheimbaium.extraction_interface.summary")
                .usage("item.alltheimbaium.extraction_interface.usage.1",
                        "item.alltheimbaium.extraction_interface.usage.2",
                        "item.alltheimbaium.extraction_interface.usage.3",
                        "item.alltheimbaium.extraction_interface.usage.4")
                .params("item.alltheimbaium.extraction_interface.param.1")
                .warn("item.alltheimbaium.extraction_interface.warn.1",
                        "item.alltheimbaium.extraction_interface.warn.2");
    }
}
