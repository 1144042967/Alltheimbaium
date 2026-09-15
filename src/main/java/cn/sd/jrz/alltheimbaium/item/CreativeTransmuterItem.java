package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.TransmuteCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * 创造物品质变器物品。
 * <p>
 * tooltip 说明三件非显然的事：输入栏每格只存 1 个、九格同种材料才触发转化，
 * 输入输出都会留在机器里（与工作台不同），以及配方是写死的、只能在这里看到。
 */
public class CreativeTransmuterItem extends BlockItem {

    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public CreativeTransmuterItem(Block block, Item.Properties properties) {
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
        List<String> recipes = TransmuteCatalog.tooltipLines();
        Tip tip = Tip.of(tooltip)
                .head(stack, "tip.alltheimbaium.type.processing")
                .summary("item.alltheimbaium.creative_transmuter.summary")
                .usage("item.alltheimbaium.creative_transmuter.usage.1",
                        "item.alltheimbaium.creative_transmuter.usage.2",
                        "item.alltheimbaium.creative_transmuter.usage.3",
                        "item.alltheimbaium.creative_transmuter.usage.4")
                .params("item.alltheimbaium.creative_transmuter.param.1");
        if (recipes.isEmpty()) {
            // 配方用注册名解析，联动模组未安装时这里会是空的
            tip.bullet("item.alltheimbaium.creative_transmuter.recipe_none");
        } else {
            tip.raw(Tip.inline(recipes, "tip.alltheimbaium.more"));
        }
        tip.warn("item.alltheimbaium.creative_transmuter.warn.1");
    }
}
