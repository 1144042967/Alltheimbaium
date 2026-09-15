package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用资源农场方块物品。
 * <p>
 * 放置后在界面标记槽放入一个标记物即永久确定该资源并开始产出。tooltip 说明标记规则
 * 与"只能标记一次"这条硬限制；若方块被拆下带回 BlockEntityTag，则额外显示当前标记物与产物。
 */
public class ResourceFarmItem extends BlockItem {

    /** 产物行在方块实体数据里的键（与 ResourceFarmEntity 的 KEY_ROWS 一致） */
    private static final String ROWS_KEY = "rows";

    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public ResourceFarmItem(Block block, Item.Properties properties) {
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
            // 26.x：产物行不再手写 NBT，统一由方块实体的 ValueOutput + Tool.WeightedRow 记录类型托管，
            // 这里用 TagValueInput 把物品上的 BlockEntityTag 当成 ValueInput 读回来（与实体侧对称）
            HolderLookup.Provider registries = registriesOf(context);
            long level = MobFarmBlock.getInitialLevel();
            String marker = null;
            List<Tool.WeightedRow> rows = new ArrayList<>();
            CompoundTag tag = Tool.getBlockEntityTag(stack);
                if (tag != null) {
                if (tag != null) {
                    // 26.x：CompoundTag 的取值方法一律返回 Optional，"按类型判断"的 contains 重载已删除，
                    // 带默认值的读法统一走 getXOr
                    if (tag.contains("level")) {
                        level = Tool.suit(tag.getLongOr("level", level));
                    }
                    String markerRaw = tag.getStringOr("marker", "");
                    if (!markerRaw.isEmpty()) {
                        Identifier id = Identifier.tryParse(markerRaw);
                        // 26.x：Registry#get 返回 Optional<Holder.Reference>，取实际对象改走 getValue
                        // （ITEM 是"带默认值"的注册表，查不到时返回 AIR，不会为 null）
                        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
                        if (item != Items.AIR) {
                            marker = new ItemStack(item).getHoverName().getString();
                        }
                    }
                    rows = Tool.readRows(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag),
                            ROWS_KEY, Tool.WeightedRow.CODEC);
                }
            }
            Tip tip = Tip.of(tooltip)
                    .head(stack, "tip.alltheimbaium.type.agriculture")
                    .summary("item.alltheimbaium.resource_farm.summary");
            if (marker != null) {
                tip.state("item.alltheimbaium.resource_farm.state.marked", marker);
            }
            tip.state("item.alltheimbaium.resource_farm.state.level", level);
            // 产物表最多 27 行，压成一行展示，避免把 tooltip 撑爆
            List<String> products = collectProducts(rows, level);
            if (!products.isEmpty()) {
                tip.raw(Tip.inline(products, "tip.alltheimbaium.more"));
            }
            tip.usage("item.alltheimbaium.resource_farm.usage.1",
                            "item.alltheimbaium.resource_farm.usage.2",
                            "item.alltheimbaium.resource_farm.usage.3",
                            "item.alltheimbaium.resource_farm.usage.4",
                            "item.alltheimbaium.resource_farm.usage.5")
                    .params("item.alltheimbaium.resource_farm.param.1")
                    .warn("item.alltheimbaium.resource_farm.warn.1");
        } catch (Throwable e) {
            // tooltip 失败忽略
        }
    }

    /**
     * tooltip 上下文带注册表访问器（物品组件里的注册名靠它解析）；客户端拿不到时回退到 Tool 的兜底
     */
    @Nonnull
    private static HolderLookup.Provider registriesOf(@Nonnull Item.TooltipContext context) {
        HolderLookup.Provider registries = context.registries();
        return registries != null ? registries : Tool.registries();
    }

    /**
     * 把产物行压成"§e速率/秒§7 名称"列表；速率 = 权重 × 等级 ÷ 500 件/秒
     */
    @Nonnull
    private static List<String> collectProducts(@Nonnull List<Tool.WeightedRow> rows, long level) {
        List<String> products = new ArrayList<>();
        for (Tool.WeightedRow row : rows) {
            try {
                if (row == null || row.item() == null || row.item().isEmpty() || row.weight() <= 0) {
                    continue;
                }
                BigDecimal speed = new BigDecimal(row.weight()).multiply(new BigDecimal(level))
                        .divide(new BigDecimal(500), 3, RoundingMode.HALF_UP);
                products.add("§e" + speed.stripTrailingZeros().toPlainString()
                        + Component.translatable("item.alltheimbaium.tooltip.per_second").getString()
                        + "§7 " + row.item().getHoverName().getString());
            } catch (Throwable ignored) {
            }
        }
        return products;
    }
}
