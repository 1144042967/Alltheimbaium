package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用资源农场方块物品。
 * <p>
 * 放置后在界面标记槽放入一个标记物即永久确定该资源并开始产出。tooltip 说明标记规则
 * 与"只能标记一次"这条硬限制；若方块被拆下带回 BlockEntityTag，则额外显示当前标记物与产物。
 */
public class ResourceFarmItem extends BlockItem {

    public ResourceFarmItem(Block block) {
        super(block, new Properties().rarity(Rarity.RARE).fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        try {
            long level = MobFarmBlock.getInitialLevel();
            String marker = null;
            ListTag rows = null;
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTagElement("BlockEntityTag");
                if (tag != null) {
                    if (tag.contains("level", Tag.TAG_LONG)) {
                        level = Tool.suit(tag.getLong("level"));
                    }
                    if (tag.contains("marker", Tag.TAG_STRING)) {
                        ResourceLocation id = ResourceLocation.tryParse(tag.getString("marker"));
                        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
                        if (item != Items.AIR) {
                            marker = new ItemStack(item).getHoverName().getString();
                        }
                    }
                    if (tag.contains("rows", Tag.TAG_LIST)) {
                        rows = (ListTag) tag.get("rows");
                    }
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
                            "item.alltheimbaium.resource_farm.usage.4")
                    .params("item.alltheimbaium.resource_farm.param.1")
                    .warn("item.alltheimbaium.resource_farm.warn.1");
        } catch (Throwable e) {
            // tooltip 失败忽略
        }
    }

    /**
     * 把产物行压成"§e速率/秒§7 名称"列表；速率 = 权重 × 等级 ÷ 500 件/秒
     */
    @Nonnull
    private static List<String> collectProducts(@Nullable ListTag rows, long level) {
        List<String> products = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return products;
        }
        for (int i = 0; i < rows.size(); i++) {
            try {
                CompoundTag c = rows.getCompound(i);
                ItemStack rowStack = ItemStack.of(c);
                if (rowStack.isEmpty()) {
                    continue;
                }
                long weight = c.contains("Weight", Tag.TAG_LONG) ? Tool.suit(c.getLong("Weight")) : 0;
                if (weight <= 0) {
                    continue;
                }
                BigDecimal speed = new BigDecimal(weight).multiply(new BigDecimal(level))
                        .divide(new BigDecimal(500), 3, RoundingMode.HALF_UP);
                products.add("§e" + speed.stripTrailingZeros().toPlainString() + "/秒§7 " + rowStack.getHoverName().getString());
            } catch (Throwable ignored) {
            }
        }
        return products;
    }
}
