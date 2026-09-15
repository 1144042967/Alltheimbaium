package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * 零刻熔炉方块物品：tooltip 显示已存电量与投料 / 取物的非显然操作。
 * 方块被挖掉时 input/output/energy 由方块类的 getDrops 经 BlockEntityTag 存入掉落物，重放即可恢复。
 */
public class InstantFurnaceItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(InstantFurnaceItem.class);

    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public InstantFurnaceItem(Block block, Item.Properties properties) {
        super(block, properties.rarity(Rarity.RARE).fireResistant());
    }

    /**
     * 26.x：tooltip 出口由 List&lt;Component&gt; 换成 Consumer&lt;Component&gt;，@OnlyIn 已删除
     */
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context,
                                @Nonnull TooltipDisplay display, @Nonnull Consumer<Component> tooltip,
                                @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        try {
            int stored = 0;
            CompoundTag tag = Tool.getBlockEntityTag(stack);
                if (tag != null) {
                // 26.x：CompoundTag 的取值方法一律返回 Optional，"按类型判断"的 contains 重载已删除，
                // 带默认值的读法统一走 getXOr
                if (tag.contains("energy")) {
                    stored = Math.max(0, tag.getIntOr("energy", 0));
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
