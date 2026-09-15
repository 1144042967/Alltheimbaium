package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ATI 存储方块制造机物品。
 * <p>
 * tooltip 说明三件非显然的事：标记槽的双向语义（放一次标记、再放一次取消并清空存量）、
 * 六面按钮的状态同时约束管道被动抽取、以及"支持哪些物品"由配置的标签/命名空间决定。
 */
public class StorageFountainItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(StorageFountainItem.class);

    /**
     * 已标记物品列表在方块实体数据里的键（与 StorageFountainEntity 的 KEY_STICK 一致，
     * 沿用旧名以免存档里的标记丢失）
     */
    private static final String STOCK_ROWS_KEY = "save_stick";

    /** 可复制范围说明里最多列出的标签 / MOD 数量 */
    private static final int MAX_ACCEPT_LISTED = 4;


    /**
     * 26.x：注册 id 与 block. 语言键前缀由 Registration 的 blockItemProps(key) 灌进属性里，这里只加品级
     */
    public StorageFountainItem(Block block, Item.Properties properties) {
        super(block, properties.rarity(Rarity.EPIC).fireResistant());
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
            long output = 5;
            List<ItemStack> stackList = new ArrayList<>();
            List<Long> blockList = new ArrayList<>();
            // 26.x：已标记物品列表不再手写 NBT，改由方块实体的 ValueOutput + Tool.StockRow 记录类型托管，
            // 这里用 TagValueInput 把物品上的 BlockEntityTag 当成 ValueInput 读回来（与实体侧对称）
            HolderLookup.Provider registries = registriesOf(context);
            CompoundTag tag = Tool.getBlockEntityTag(stack);
                if (tag != null) {
                if (tag != null) {
                    // 26.x：CompoundTag 的取值方法一律返回 Optional，"按类型判断"的 contains 重载已删除，
                    // 带默认值的读法统一走 getXOr
                    if (tag.contains("output")) {
                        output = Tool.suit(tag.getLongOr("output", output));
                    }
                    for (Tool.StockRow row : Tool.readRows(
                            TagValueInput.create(ProblemReporter.DISCARDING, registries, tag),
                            STOCK_ROWS_KEY, Tool.StockRow.CODEC)) {
                        if (row == null || row.item() == null || row.item().isEmpty()) {
                            continue;
                        }
                        stackList.add(row.item());
                        blockList.add(Tool.suit(row.count()));
                    }
                }
            }
            // 内部计数单位是 carry，换算成"件/tick"才便于玩家理解
            BigDecimal carry = new BigDecimal(StorageFountainBlock.getCarry());
            Tip tip = Tip.of(tooltip)
                    .head(stack, "tip.alltheimbaium.type.resource")
                    .summary("item.alltheimbaium.storage_fountain.summary", StorageFountainBlock.getMaxItemTypes())
                    .state("item.alltheimbaium.storage_fountain.state.output",
                            plain(new BigDecimal(output).divide(carry, 3, RoundingMode.HALF_UP)),
                            plain(new BigDecimal(StorageFountainBlock.getStep()).divide(carry, 3, RoundingMode.HALF_UP)),
                            StorageFountainBlock.getGrowthIntervalSeconds());
            // 已标记物品逐行列出（上限 9 种），带存量
            for (int i = 0; i < Math.min(stackList.size(), blockList.size()); i++) {
                tip.state("item.alltheimbaium.storage_fountain.state.marked",
                        stackList.get(i).getHoverName().getString(),
                        plain(new BigDecimal(blockList.get(i)).divide(carry, 3, RoundingMode.HALF_UP)));
            }
            tip.usage("item.alltheimbaium.storage_fountain.usage.1",
                            "item.alltheimbaium.storage_fountain.usage.2",
                            "item.alltheimbaium.storage_fountain.usage.3",
                            "item.alltheimbaium.storage_fountain.usage.4")
                    .warn("item.alltheimbaium.storage_fountain.warn.1");
            // 只在未标记任何物品时列出可复制范围，避免每次 hover 都拖着一条长表
            if (stackList.isEmpty()) {
                tip.params("item.alltheimbaium.storage_fountain.param.1")
                        .bullet("item.alltheimbaium.storage_fountain.param.items",
                                Tip.join(asLiteralList(StorageFountainBlock.getAcceptedItems()), MAX_ACCEPT_LISTED, "tip.alltheimbaium.more"))
                        .bullet("item.alltheimbaium.storage_fountain.param.tags",
                                Tip.join(asLiteralList(StorageFountainBlock.getAcceptedTags()), MAX_ACCEPT_LISTED, "tip.alltheimbaium.more"))
                        .bullet("item.alltheimbaium.storage_fountain.param.mods",
                                Tip.join(asLiteralList(StorageFountainBlock.getAcceptedMods()), MAX_ACCEPT_LISTED, "tip.alltheimbaium.more"));
            }
        } catch (Throwable e) {
            log.error("StorageFountainItem.appendHoverText error", e);
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
     * 去掉多余的小数尾零，避免 tooltip 里出现 "0.100" 这类噪声
     */
    @Nonnull
    private static String plain(@Nonnull BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /**
     * 配置里的字符串列表转成带高亮的条目文本
     */
    @Nonnull
    private static List<String> asLiteralList(@Nullable List<? extends String> source) {
        List<String> list = new ArrayList<>();
        if (source != null) {
            for (String s : source) {
                list.add("§e" + s);
            }
        }
        return list;
    }
}
