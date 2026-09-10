package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 打包材料（合成中间物）。
 * <p>
 * 材料级物品没有可配置项，按规范只保留品级行与一句话概述——合成配方由 JEI 呈现，
 * 再写一遍用法属于冗余说明。
 */
public class PackageMaterialItem extends Item {

    public PackageMaterialItem() {
        super(new Item.Properties());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        Tip.of(tooltip)
                .head(stack, "tip.alltheimbaium.type.material")
                .summary("item.alltheimbaium.package_material_x1.summary");
    }
}
