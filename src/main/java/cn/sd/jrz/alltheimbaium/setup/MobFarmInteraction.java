package cn.sd.jrz.alltheimbaium.setup;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
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
import java.util.List;

/**
 * 生物农场"使用槽"产物模拟：把"物品右击收容物能产掉落物"的原版组合翻译成确定性的产物 ItemStack。
 * <p>
 * 所有产物都不向世界刷实体，而是作为 ItemStack 交给机器并入存量；耐久型物品按使用次数损耗；
 * 剪毛后的羊/哞菇按 {@link MobFarmBlock#getShearRegrowSeconds()} 计时长回毛（状态与计时器存进收容物 NBT）。
 */
public final class MobFarmInteraction {
    private static final Logger log = LoggerFactory.getLogger(MobFarmInteraction.class);
    /** 剪刀在羊/哞菇 NBT 里使用的内部标记键（避免与 vanilla 键冲突） */
    public static final String REGROW_TICKS = "ATIRegrowTicks";
    /** 哞菇"已被剪"标记（哞菇 vanilla 没有 Sheared 字段，用内部键模拟） */
    public static final String MOOSHROOM_SHEARED = "ATISheared";

    private static final List<ItemStack> NOTHING = List.of();

    private MobFarmInteraction() {
    }

    /** 一次模拟使用的结果 */
    public record UseResult(@Nonnull List<ItemStack> produced, boolean consumeInput, int durabilityUsed) {
        public boolean isEmpty() {
            return produced.isEmpty();
        }
    }

    private static final UseResult EMPTY_RESULT = new UseResult(NOTHING, false, 0);

    /**
     * 模拟"用 useItem 右击收容物"一次（服务端）。
     *
     * @param entityTag 收容物实体 NBT（会被修改，如设置 Sheared / 内部计时）
     */
    @Nonnull
    public static UseResult simulateUse(EntityType<?> type, CompoundTag entityTag, ItemStack useItem) {
        try {
            Item item = useItem.getItem();
            // ---- 剪刀：剪羊 / 剪哞菇 ----
            if (item == Items.SHEARS) {
                if (type == EntityType.SHEEP && !isSheepSheared(entityTag)) {
                    ItemStack wool = new ItemStack(woolForSheep(entityTag), 2);
                    entityTag.putBoolean("Sheared", true);
                    entityTag.putInt(REGROW_TICKS, regrowTicks());
                    return new UseResult(List.of(wool), false, 1);
                }
                if (type == EntityType.MOOSHROOM && entityTag.getInt(MOOSHROOM_SHEARED) == 0) {
                    ItemStack mushroom = new ItemStack(mushroomForCow(entityTag), 5);
                    entityTag.putInt(MOOSHROOM_SHEARED, 1);
                    entityTag.putInt(REGROW_TICKS, regrowTicks());
                    return new UseResult(List.of(mushroom), false, 1);
                }
                return EMPTY_RESULT;
            }
            // ---- 空桶挤奶：牛 / 哞菇 ----
            if (item == Items.BUCKET && (type == EntityType.COW || type == EntityType.MOOSHROOM)) {
                return new UseResult(List.of(new ItemStack(Items.MILK_BUCKET, 1)), true, 0);
            }
            // ---- 碗对哞菇：蘑菇煲 ----
            if (item == Items.BOWL && type == EntityType.MOOSHROOM) {
                return new UseResult(List.of(new ItemStack(Items.MUSHROOM_STEW, 1)), true, 0);
            }
            // ---- 玻璃瓶对末影龙：龙息 ----
            if (item == Items.GLASS_BOTTLE && type == EntityType.ENDER_DRAGON) {
                return new UseResult(List.of(new ItemStack(Items.DRAGON_BREATH, 1)), true, 0);
            }
            return EMPTY_RESULT;
        } catch (Throwable e) {
            log.error("MobFarmInteraction.simulateUse error", e);
        }
        return EMPTY_RESULT;
    }

    /**
     * 剪毛恢复计时（服务端每 tick 调用）。
     *
     * @return true 表示本帧刚计时结束、毛已长回（需要通知客户端刷新）
     */
    public static boolean tickRegrow(EntityType<?> type, CompoundTag entityTag) {
        try {
            if (type != EntityType.SHEEP && type != EntityType.MOOSHROOM) {
                return false;
            }
            if (!entityTag.contains(REGROW_TICKS, Tag.TAG_INT)) {
                return false;
            }
            int left = entityTag.getInt(REGROW_TICKS) - 1;
            if (left > 0) {
                entityTag.putInt(REGROW_TICKS, left);
                return false;
            }
            entityTag.remove(REGROW_TICKS);
            if (type == EntityType.SHEEP) {
                entityTag.putBoolean("Sheared", false);
            } else {
                entityTag.putInt(MOOSHROOM_SHEARED, 0);
            }
            return true;
        } catch (Throwable e) {
            log.error("MobFarmInteraction.tickRegrow error", e);
        }
        return false;
    }

    /** 羊是否已剪毛 */
    private static boolean isSheepSheared(CompoundTag entityTag) {
        if (entityTag.contains("Sheared", Tag.TAG_BYTE)) {
            return entityTag.getBoolean("Sheared");
        }
        return false;
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

    private static int regrowTicks() {
        return Math.max(1, MobFarmBlock.getShearRegrowSeconds()) * 20;
    }
}
