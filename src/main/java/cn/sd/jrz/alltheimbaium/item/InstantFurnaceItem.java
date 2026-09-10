package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
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
 * 零刻熔炉方块物品：tooltip 显示已存电量与投料 / 取物的非显然操作。
 * 方块被挖掉时 input/output/energy 通过 loot 表 copy_nbt 存入 BlockEntityTag，重放即可恢复。
 */
public class InstantFurnaceItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(InstantFurnaceItem.class);

    public InstantFurnaceItem(Block block) {
        super(block, new Properties().rarity(Rarity.RARE).fireResistant());
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
            Tip.of(tooltip)
                    .head(stack, "tip.alltheimbaium.type.processing")
                    .summary("item.alltheimbaium.instant_furnace.summary")
                    .state("item.alltheimbaium.instant_furnace.state.energy",
                            String.format("%,d", stored), String.format("%,d", InstantFurnaceEntity.MAX_ENERGY))
                    .usage("item.alltheimbaium.instant_furnace.usage.1",
                            "item.alltheimbaium.instant_furnace.usage.2",
                            "item.alltheimbaium.instant_furnace.usage.3")
                    .params("item.alltheimbaium.instant_furnace.param.1",
                            "item.alltheimbaium.instant_furnace.param.2")
                    .warn("item.alltheimbaium.instant_furnace.warn.1");
        } catch (Throwable e) {
            log.error("InstantFurnaceItem.appendHoverText error", e);
        }
    }
}
