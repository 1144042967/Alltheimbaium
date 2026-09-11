package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.TransmuteCatalog;
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
 * 创造物品质变器物品。
 * <p>
 * tooltip 说明三件非显然的事：输入栏每格只存 1 个、九格同种材料才触发转化，
 * 输入输出都会留在机器里（与工作台不同），以及配方是写死的、只能在这里看到。
 */
public class CreativeTransmuterItem extends BlockItem {

    public CreativeTransmuterItem(Block block) {
        super(block, new Properties().rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
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
