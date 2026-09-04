package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * 生物农场方块物品。手持时显示等级、收容生物与各产物存量/生成速度。
 */
public class MobFarmItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(MobFarmItem.class);

    public MobFarmItem(Block block) {
        super(block, new Properties().fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        try {
            long level = MobFarmBlock.getInitialLevel();
            String containedName = null;
            ListTag rows = null;
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTagElement("BlockEntityTag");
                if (tag != null) {
                    if (tag.contains("level", Tag.TAG_LONG)) {
                        level = Tool.suit(tag.getLong("level"));
                    }
                    if (tag.contains("entityTag", Tag.TAG_COMPOUND)) {
                        String id = tag.getCompound("entityTag").getString("id");
                        Optional<EntityType<?>> type = EntityType.byString(id);
                        if (type.isPresent()) {
                            containedName = type.get().getDescription().getString();
                        }
                    }
                    if (tag.contains("rows", Tag.TAG_LIST)) {
                        rows = (ListTag) tag.get("rows");
                    }
                }
            }
            if (containedName != null) {
                tooltip.add(Component.translatable("item.alltheimbaium.mob_farm.tooltip.contained", containedName, level));
            } else {
                tooltip.add(Component.translatable("item.alltheimbaium.mob_farm.tooltip.level", level));
            }
            if (rows != null && !rows.isEmpty()) {
                for (int i = 0; i < rows.size(); i++) {
                    try {
                        CompoundTag c = rows.getCompound(i);
                        ItemStack rowStack = ItemStack.of(c);
                        if (rowStack.isEmpty()) {
                            continue;
                        }
                        long stock = c.contains("Stock", Tag.TAG_LONG) ? Tool.suit(c.getLong("Stock")) : 0;
                        long weight = c.contains("Weight", Tag.TAG_LONG) ? Tool.suit(c.getLong("Weight")) : 0;
                        String name = rowStack.getHoverName().getString();
                        if (stock > 0) {
                            tooltip.add(Component.translatable("item.alltheimbaium.mob_farm.tooltip.product", name, stock));
                        }
                        if (weight > 0) {
                            // 生成速度：重量×等级 / 500 (件/秒)
                            BigDecimal speed = new BigDecimal(weight).multiply(new BigDecimal(level))
                                    .divide(new BigDecimal(500), 3, RoundingMode.HALF_UP);
                            tooltip.add(Component.translatable("item.alltheimbaium.mob_farm.tooltip.rate", name, speed));
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } else {
                tooltip.add(Component.translatable("item.alltheimbaium.mob_farm.tooltip.empty.1"));
                tooltip.add(Component.translatable("item.alltheimbaium.mob_farm.tooltip.empty.2"));
            }
        } catch (Throwable e) {
            log.error("MobFarmItem.appendHoverText error", e);
        }
    }
}
