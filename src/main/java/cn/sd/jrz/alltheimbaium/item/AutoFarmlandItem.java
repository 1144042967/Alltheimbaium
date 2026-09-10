package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.setup.Tool;
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
 * ATI 自动耕地物品。
 * <p>
 * 产物表随所种作物变化且行数不定，界面里看更合适；tooltip 只说明非显然的收割范围
 * 与等级机制，以及顶面不打开界面这一点。
 */
public class AutoFarmlandItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(AutoFarmlandItem.class);

    public AutoFarmlandItem(Block block, Properties properties) {
        super(block, properties.rarity(Rarity.RARE).fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        try {
            long level = MobFarmBlock.getInitialLevel();
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTagElement("BlockEntityTag");
                if (tag != null && tag.contains("level", Tag.TAG_LONG)) {
                    level = Tool.suit(tag.getLong("level"));
                }
            }
            Tip.of(tooltip)
                    .head(stack, "tip.alltheimbaium.type.agriculture")
                    .summary("item.alltheimbaium.auto_farmland.summary")
                    .state("item.alltheimbaium.auto_farmland.state.level", level, Math.max(0L, level - 1))
                    .usage("item.alltheimbaium.auto_farmland.usage.1",
                            "item.alltheimbaium.auto_farmland.usage.2",
                            "item.alltheimbaium.auto_farmland.usage.3")
                    .warn("item.alltheimbaium.auto_farmland.warn.1");
        } catch (Throwable e) {
            log.error("AutoFarmlandItem.appendHoverText error", e);
        }
    }
}
