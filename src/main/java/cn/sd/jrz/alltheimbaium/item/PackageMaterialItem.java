package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * 打包材料（合成中间物）。
 * <p>
 * 材料级物品没有可配置项，按规范只保留品级行与一句话概述——合成配方由 JEI 呈现，
 * 再写一遍用法属于冗余说明。
 */
public class PackageMaterialItem extends Item {

    /**
     * 26.x：注册 id 由 Registration 的 itemProps(key) 灌进属性里，构造器只负责品级等自身属性
     */
    public PackageMaterialItem(Item.Properties properties) {
        super(properties);
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
                .head(stack, "tip.alltheimbaium.type.material")
                .summary("item.alltheimbaium.package_material.summary");
    }
}
