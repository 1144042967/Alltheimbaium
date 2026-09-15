package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * 取出接口方块物品。
 * <p>
 * 方块本身没有界面、没有主动输出，只是沿本模组与联动模组（AutoResource）的方块连通搜索，
 * 把找到的机器产物与流体聚合成一个只读视图供管道抽取。tooltip 只说明非显然的部分：
 * 搜索沿什么传导、谁能被抽、谁只传导，以及平台地面会断开连通。
 */
public class ExtractionInterfaceItem extends BlockItem {

    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public ExtractionInterfaceItem(Block block, Item.Properties properties) {
        super(block, properties.rarity(Rarity.UNCOMMON));
    }

    /**
     * 26.x：tooltip 出口由 List&lt;Component&gt; 换成 Consumer&lt;Component&gt;，@OnlyIn 已删除
     */
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context,
                                @Nonnull TooltipDisplay display, @Nonnull Consumer<Component> tooltip,
                                @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
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
