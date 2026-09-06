package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 生物农场"使用槽"产物模拟：把"物品右击收容物能产掉落物"的原版组合翻译成确定性的产物 ItemStack。
 * <p>
 * 所有产物都不向世界刷实体，而是作为 ItemStack 交给机器并入存量；耐久型物品按使用次数损耗。
 * 产物判定只看"物品 × 收容生物"组合，不修改收容物 NBT 的剪毛状态（产物由机器持续自动生成）。
 */
public final class MobFarmInteraction {
    private static final Logger log = LoggerFactory.getLogger(MobFarmInteraction.class);

    private MobFarmInteraction() {
    }

    /**
     * 该工具对当前收容物是否能产出物品（非破坏性判定，供 GUI 状态/生成速度判断）。
     */
    public static boolean isApplicableItem(@Nonnull EntityType<?> type, @Nonnull ItemStack useItem) {
        return produceItem(type, null, useItem) != null;
    }

    /**
     * 该工具对当前收容物会产出的物品种（非破坏性）；组合不适用返回 null。
     */
    @Nullable
    public static Item produceItem(@Nonnull EntityType<?> type, @Nullable CompoundTag entityTag, @Nonnull ItemStack useItem) {
        try {
            Item item = useItem.getItem();
            CompoundTag tag = entityTag == null ? new CompoundTag() : entityTag;
            if (item == Items.SHEARS) {
                if (type == EntityType.SHEEP) {
                    return woolForSheep(tag);
                }
                if (type == EntityType.MOOSHROOM) {
                    return mushroomForCow(tag);
                }
                return null;
            }
            if (item == Items.BUCKET && (type == EntityType.COW || type == EntityType.MOOSHROOM)) {
                return Items.MILK_BUCKET;
            }
            if (item == Items.BOWL && type == EntityType.MOOSHROOM) {
                return Items.MUSHROOM_STEW;
            }
            if (item == Items.GLASS_BOTTLE && type == EntityType.ENDER_DRAGON) {
                return Items.DRAGON_BREATH;
            }
            return null;
        } catch (Throwable e) {
            log.error("MobFarmInteraction.produceItem error", e);
        }
        return null;
    }

    /** 羊当前羊毛颜色对应的羊毛物品（按羊 NBT 的 Color，默认白色） */
    @Nonnull
    private static Item woolForSheep(CompoundTag entityTag) {
        int id = 0;
        if (entityTag.contains("Color", Tag.TAG_BYTE)) {
            id = entityTag.getByte("Color") & 0xFF;
        } else if (entityTag.contains("Color", Tag.TAG_INT)) {
            id = entityTag.getInt("Color");
        }
        id = ((id % 16) + 16) % 16;
        String[] dyeNames = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
                "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        try {
            Item wool = BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", dyeNames[id] + "_wool"));
            if (wool != null && wool != Items.AIR) {
                return wool;
            }
        } catch (Throwable ignored) {
        }
        return Blocks.WHITE_WOOL.asItem();
    }

    /** 哞菇变体对应的蘑菇物品（默认红色） */
    @Nonnull
    private static Item mushroomForCow(CompoundTag entityTag) {
        boolean brown = false;
        if (entityTag.contains("Type", Tag.TAG_STRING)) {
            brown = "brown".equals(entityTag.getString("Type"));
        } else if (entityTag.contains("Type", Tag.TAG_BYTE)) {
            brown = entityTag.getByte("Type") == 1;
        }
        return brown ? Blocks.BROWN_MUSHROOM.asItem() : Blocks.RED_MUSHROOM.asItem();
    }
}
