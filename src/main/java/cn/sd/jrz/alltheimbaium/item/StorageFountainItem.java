package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * ATI 存储方块制造机物品。
 * <p>
 * tooltip 说明三件非显然的事：标记槽的双向语义（放一次标记、再放一次取消并清空存量）、
 * 六面按钮的状态同时约束管道被动抽取、以及"支持哪些物品"由配置的标签/命名空间决定。
 */
public class StorageFountainItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(StorageFountainItem.class);

    /** 可复制范围说明里最多列出的标签 / MOD 数量 */
    private static final int MAX_ACCEPT_LISTED = 4;


    public StorageFountainItem(Block block) {
        super(block, new Properties().rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        try {
            long output = 5;
            List<ItemStack> stackList = new ArrayList<>();
            List<Long> blockList = new ArrayList<>();
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTagElement("BlockEntityTag");
                if (tag != null) {
                    if (tag.contains("output", Tag.TAG_LONG)) {
                        output = Tool.suit(tag.getLong("output"));
                    }
                    if (tag.contains("save_stick")) {
                        ListTag array = (ListTag) tag.get("save_stick");
                        if (array != null) {
                            stackList = Tool.toItemList(array);
                            blockList = Tool.toBlockList(array);
                        }
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
