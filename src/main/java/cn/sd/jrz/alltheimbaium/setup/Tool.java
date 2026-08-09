package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Tool {
    private static final Logger log = LoggerFactory.getLogger(Tool.class);

    public static long suit(long value) {
        return value < 0 ? Long.MAX_VALUE : value;
    }

    public static int suitInt(long value) {
        return value < 0 || value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    public static void takeItem(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            ItemEntity entity = player.drop(stack, false);
            if (entity != null) {
                entity.setNoPickUpDelay();
                entity.setTarget(player.getUUID());
            }
        }
    }

    public static ListTag toJsonArray(List<ItemStack> itemList, List<Long> blockList) {
        ListTag list = new ListTag();
        for (int i = 0; i < itemList.size(); i++) {
            ItemStack item = itemList.get(i);
            Long count = blockList.get(i);
            if (item == null || count == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            item.save(tag);
            tag.putLong("Long_Count", count);
            list.add(tag);
        }
        return list;
    }

    public static List<ItemStack> toItemList(ListTag array) {
        List<ItemStack> itemList = new ArrayList<>();
        for (Tag value : array) {
            CompoundTag tag = (CompoundTag) value;
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) {
                continue;
            }
            stack = stack.copy();
            stack.setCount(1);
            itemList.add(stack);
        }
        return itemList;
    }

    public static List<Long> toBlockList(ListTag array) {
        List<Long> blockList = new ArrayList<>();
        for (Tag value : array) {
            CompoundTag tag = (CompoundTag) value;
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) {
                continue;
            }
            blockList.add(tag.getLong("Long_Count"));
        }
        return blockList;
    }

    @SuppressWarnings("deprecation")
    public static void sort(List<ItemStack> itemList, List<Long> blockList) {
        for (int i = 0; i < Math.min(itemList.size(), blockList.size()); i++) {
            for (int j = i + 1; j < Math.min(itemList.size(), blockList.size()); j++) {
                ItemStack aStack = itemList.get(i);
                ItemStack bStack = itemList.get(j);
                String a = BuiltInRegistries.ITEM.getKey(aStack.getItem()).toString();
                String b = BuiltInRegistries.ITEM.getKey(bStack.getItem()).toString();
                if (compareName(a, b) > 0) {
                    itemList.set(i, bStack);
                    itemList.set(j, aStack);
                    Long aBlock = blockList.get(i);
                    Long bBlock = blockList.get(j);
                    blockList.set(i, bBlock);
                    blockList.set(j, aBlock);
                }
            }
        }
    }

    private static int compareName(String a, String b) {
        if (a.startsWith("minecraft:") && b.startsWith("minecraft:")) {
            return a.compareTo(b);
        }
        if (!a.startsWith("minecraft:") && !b.startsWith("minecraft:")) {
            return a.compareTo(b);
        }
        return a.startsWith("minecraft:") ? -1 : 1;
    }

    // ==================== Draconic-Evolution 混沌守卫免伤突破 ====================
    // 反射缓存：未安装 DE 或版本不符时保持 null，静默禁用
    private static Class<?> guardianClass;
    private static Class<?> guardianPartClass;
    private static Method guardianAttackMethod;
    private static Field guardianPartDragonField;
    private static boolean guardianInit;

    /** 初始化混沌守卫反射信息（只尝试一次） */
    private static void initGuardian() {
        if (guardianInit) return;
        guardianInit = true;
        try {
            guardianClass = Class.forName("com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity");
            guardianPartClass = Class.forName("com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianPartEntity");
            // protected boolean attackDragonFrom(DamageSource source, float amount) -> super.hurt
            guardianAttackMethod = guardianClass.getDeclaredMethod("attackDragonFrom", DamageSource.class, float.class);
            guardianAttackMethod.setAccessible(true);
            // public final DraconicGuardianEntity dragon
            guardianPartDragonField = guardianPartClass.getDeclaredField("dragon");
            guardianPartDragonField.setAccessible(true);
        } catch (Throwable t) {
            log.warn("未检测到 Draconic-Evolution 混沌守卫，永恒之剑免伤突破已禁用", t);
            guardianClass = null;
            guardianPartClass = null;
            guardianAttackMethod = null;
            guardianPartDragonField = null;
        }
    }

    /** 判断实体是否为混沌守卫（本体或部位），用于范围攻击目标筛选 */
    public static boolean isGuardian(Entity target) {
        initGuardian();
        if (guardianClass == null || target == null) return false;
        return guardianClass.isInstance(target)
                || (guardianPartClass != null && guardianPartClass.isInstance(target));
    }

    /**
     * 突破混沌守卫免伤：反射调用其 protected attackDragonFrom(source, damage)，
     * 绕过攻击冷却 / 水晶护盾 / 单发伤害上限。命中守卫返回 true，否则返回 false。
     * 未安装 DE 时始终返回 false，由调用方回退到普通伤害。
     */
    public static boolean bypassGuardianDamage(Entity target, DamageSource source, float damage) {
        initGuardian();
        if (guardianClass == null || target == null) return false;
        try {
            Object guardian = null;
            if (guardianClass.isInstance(target)) {
                guardian = target;
            } else if (guardianPartClass != null && guardianPartClass.isInstance(target)) {
                guardian = guardianPartDragonField.get(target);
            }
            if (guardian != null && guardianClass.isInstance(guardian)) {
                guardianAttackMethod.invoke(guardian, source, damage);
                return true;
            }
        } catch (Throwable t) {
            log.warn("混沌守卫免伤突破反射调用失败，回退到普通伤害", t);
        }
        return false;
    }
}
