package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 通用资源农场方块物品。
 * <p>
 * 放置后空手右键打开界面；在界面标记槽放入一个标记物即永久确定该资源并开始产出。
 * tooltip 显示使用说明；若方块被拆下带回 BlockEntityTag，则额外显示当前标记物。
 */
public class ResourceFarmItem extends BlockItem {
    public ResourceFarmItem(Block block) {
        super(block, new Item.Properties());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        try {
            String marker = null;
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTagElement("BlockEntityTag");
                if (tag != null && tag.contains("marker", net.minecraft.nbt.Tag.TAG_STRING)) {
                    Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(tag.getString("marker")));
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        marker = new ItemStack(item).getHoverName().getString();
                    }
                }
            }
            if (marker != null) {
                tooltip.add(Component.translatable("item.alltheimbaium.resource_farm.tooltip.marked", marker));
            }
            tooltip.add(Component.translatable("item.alltheimbaium.resource_farm.tooltip.1"));
            tooltip.add(Component.translatable("item.alltheimbaium.resource_farm.tooltip.2"));
            tooltip.add(Component.translatable("item.alltheimbaium.resource_farm.tooltip.3"));
            tooltip.add(Component.translatable("item.alltheimbaium.resource_farm.tooltip.4"));
            tooltip.add(Component.translatable("item.alltheimbaium.resource_farm.tooltip.5"));
        } catch (Throwable e) {
            // tooltip 失败忽略
        }
    }
}
