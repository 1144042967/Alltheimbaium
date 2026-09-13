package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.entity.BlockEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
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

    /**
     * 取当前可用于物品（反）序列化的注册表访问器。
     * <p>
     * 1.21 起 {@code ItemStack#save}/{@code parseOptional} 需要 {@link HolderLookup.Provider}：
     * 带附魔、药水等"按注册名引用注册表条目"的组件必须靠它才能正确编解码。
     * 优先取当前服务端的 {@code registryAccess()}；服务端尚未启动（如仅客户端主菜单、
     * 单元测试）时拿不到，回退到 {@link RegistryAccess#EMPTY}——此时只会丢失这类
     * 需要注册表查表的组件，纯原版物品仍能正常读写。
     */
    @Nonnull
    public static HolderLookup.Provider registries() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
    }

    public static ListTag toJsonArray(List<ItemStack> itemList, List<Long> blockList) {
        ListTag list = new ListTag();
        HolderLookup.Provider registries = registries();
        for (int i = 0; i < itemList.size(); i++) {
            ItemStack item = itemList.get(i);
            Long count = blockList.get(i);
            if (item == null || count == null) {
                continue;
            }
            // 1.21：save 需要注册表访问器，且一律以返回值为准（不要依赖传入 tag 被原地填充）
            CompoundTag tag = (CompoundTag) item.save(registries, new CompoundTag());
            tag.putLong("Long_Count", count);
            list.add(tag);
        }
        return list;
    }

    public static List<ItemStack> toItemList(ListTag array) {
        List<ItemStack> itemList = new ArrayList<>();
        HolderLookup.Provider registries = registries();
        for (Tag value : array) {
            CompoundTag tag = (CompoundTag) value;
            ItemStack stack = ItemStack.parseOptional(registries, tag);
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
        HolderLookup.Provider registries = registries();
        for (Tag value : array) {
            CompoundTag tag = (CompoundTag) value;
            ItemStack stack = ItemStack.parseOptional(registries, tag);
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

    /**
     * 初始化混沌守卫反射信息（只尝试一次）
     */
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

    /**
     * 判断实体是否为混沌守卫（本体或部位），用于范围攻击目标筛选
     */
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

    // ==================== ItemStack 数据组件（1.21 的 NBT 替代） ====================
    // 1.20.1 的 stack.getTag()/getOrCreateTag()/getTagElement("BlockEntityTag") 在 1.21 全部删除：
    // 自定义数据改走 CUSTOM_DATA 组件、方块实体数据走 BLOCK_ENTITY_DATA 组件，且都不再返回"可写回"的活对象。

    /** 读取物品上的方块实体数据（对应旧的 {@code getTagElement("BlockEntityTag")}），没有返回 null */
    @Nullable
    public static CompoundTag getBlockEntityTag(@Nonnull ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTag();
    }

    /** 读取物品的自定义数据（对应旧的 {@code getTag()}），没有返回 null */
    @Nullable
    public static CompoundTag getCustomTag(@Nonnull ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    /** 读取物品的自定义数据，没有则返回空标签（对应旧的 {@code getOrCreateTag()} 的读法） */
    @Nonnull
    public static CompoundTag getCustomTagOrEmpty(@Nonnull ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** 写回物品的自定义数据（改完 getCustomTagOrEmpty 的结果后必须调它，组件不像旧 NBT 那样是活引用） */
    public static void setCustomTag(@Nonnull ItemStack stack, @Nonnull CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 读取方块实体数据用于改写（对应旧的 {@code getOrCreateTagElement("BlockEntityTag")}），没有则返回空标签。
     * 注意返回值是副本，改完必须调 {@link #setBlockEntityTag} 写回。
     */
    @Nonnull
    public static CompoundTag getBlockEntityTagOrEmpty(@Nonnull ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** 写回物品上的方块实体数据（放置时会被 {@code BlockItem#updateCustomBlockEntityTag} 合并进方块实体） */
    public static void setBlockEntityTag(@Nonnull ItemStack stack, @Nonnull CompoundTag tag) {
        if (tag.isEmpty()) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        } else {
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
    }

    /**
     * 1.21：把方块实体数据写进掉落物的 block_entity_data 组件。
     * <p>
     * 1.20.1 的战利品表用 {@code minecraft:copy_nbt} 把 BE 数据搬进 {@code BlockEntityTag}，
     * 该函数在 1.21 已被删除（BE 数据改走组件），因此在方块类的 {@code getDrops} 里直接写。
     * 物品 tooltip 读的就是这个组件（见 {@link #getBlockEntityTag}），拆下重放不丢数据。
     */
    @Nonnull
    public static List<ItemStack> withBlockEntityData(@Nonnull List<ItemStack> drops, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be == null) {
            return drops;
        }
        try {
            HolderLookup.Provider registries = be.getLevel() == null ? RegistryAccess.EMPTY : be.getLevel().registryAccess();
            CompoundTag tag = be.saveWithoutMetadata(registries);
            if (tag.isEmpty()) {
                return drops;
            }
            ItemStack self = new ItemStack(be.getBlockState().getBlock().asItem());
            for (ItemStack stack : drops) {
                if (ItemStack.isSameItem(stack, self)) {
                    stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
                }
            }
        } catch (Throwable e) {
            log.error("Tool.withBlockEntityData error", e);
        }
        return drops;
    }
}