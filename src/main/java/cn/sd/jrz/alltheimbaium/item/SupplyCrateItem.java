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
 * ATI 补给箱物品。
 */
public class SupplyCrateItem extends BlockItem {

    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public SupplyCrateItem(Block block, Item.Properties properties) {
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
                .head(stack, "tip.alltheimbaium.type.supply")
                .summary("item.alltheimbaium.supply_crate.summary")
                .usage("item.alltheimbaium.supply_crate.usage.1",
                        "item.alltheimbaium.supply_crate.usage.2",
                        "item.alltheimbaium.supply_crate.usage.3")
                .warn("item.alltheimbaium.supply_crate.warn.1");
    }
}
