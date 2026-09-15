package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
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
 * ATI 自动耕地物品。
 * <p>
 * 产物表随所种作物变化且行数不定，界面里看更合适；tooltip 只说明非显然的收割范围
 * 与等级机制，以及顶面不打开界面这一点。
 */
public class AutoFarmlandItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(AutoFarmlandItem.class);

    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public AutoFarmlandItem(Block block, Item.Properties properties) {
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
            long level = MobFarmBlock.getInitialLevel();
            CompoundTag tag = Tool.getBlockEntityTag(stack);
                if (tag != null) {
                // 26.x：CompoundTag 的取值方法一律返回 Optional，"按类型判断"的 contains 重载已删除，
                // 带默认值的读法统一走 getXOr
                if (tag.contains("level")) {
                    level = Tool.suit(tag.getLongOr("level", level));
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
