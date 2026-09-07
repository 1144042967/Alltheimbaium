package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 零刻压印器方块物品：悬浮提示显示已存电量与使用说明。
 */
public class InstantInscriberItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(InstantInscriberItem.class);

    public InstantInscriberItem(Block block) {
        super(block, new Properties().fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        try {
            int stored = 0;
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTagElement("BlockEntityTag");
                if (tag != null && tag.contains("energy", Tag.TAG_INT)) {
                    stored = Math.max(0, tag.getInt("energy"));
                }
            }
            tooltip.add(Component.translatable("screen.alltheimbaium.instant_inscriber.energy_tooltip",
                    String.format("%,d", stored), String.format("%,d", InstantInscriberEntity.MAX_ENERGY)));
            tooltip.add(Component.translatable("item.alltheimbaium.instant_inscriber.tooltip.1"));
        } catch (Throwable e) {
            log.error("InstantInscriberItem.appendHoverText error", e);
        }
    }
}
