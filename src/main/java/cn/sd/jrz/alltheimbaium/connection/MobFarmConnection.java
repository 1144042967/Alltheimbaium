package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 生物农场对外 IItemHandler（只读，仅供抽取）。
 * <p>
 * 关联访问方向后，被动抽取与主动输出遵循同一套方向配置：
 * 随机面可抽全部有存量行；槽 N 面只能抽该行；禁用面不提供任何物品；side 为 null 按随机处理。
 */
public class MobFarmConnection implements IItemHandler {
    private static final Logger log = LoggerFactory.getLogger(MobFarmConnection.class);
    private final MobFarmEntity owner;
    @Nullable
    private final Direction side;

    public MobFarmConnection(MobFarmEntity owner, @Nullable Direction side) {
        this.owner = owner;
        this.side = side;
    }

    private int resolveState() {
        if (side == null) {
            return MobFarmEntity.STATE_RANDOM;
        }
        return owner.getDirectionState(side);
    }

    /** 能力槽位号 → 产物行索引；不可访问返回 -1 */
    private int slotToIndex(int slot) {
        int state = resolveState();
        if (state == MobFarmEntity.STATE_RANDOM) {
            return slot >= 0 && slot < owner.getProductCount() ? slot : -1;
        }
        if (state >= MobFarmEntity.STATE_SLOT_BASE) {
            int idx = state - MobFarmEntity.STATE_SLOT_BASE;
            return slot == 0 && idx < owner.getProductCount() ? idx : -1;
        }
        return -1;
    }

    @Override
    public int getSlots() {
        try {
            int state = resolveState();
            if (state == MobFarmEntity.STATE_RANDOM) {
                return owner.getProductCount();
            }
            if (state >= MobFarmEntity.STATE_SLOT_BASE) {
                int idx = state - MobFarmEntity.STATE_SLOT_BASE;
                return idx < owner.getProductCount() ? 1 : 0;
            }
            return 0;
        } catch (Throwable e) {
            log.error("MobFarmConnection.getSlots error", e);
        }
        return 0;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        try {
            int index = slotToIndex(slot);
            if (index < 0) {
                return ItemStack.EMPTY;
            }
            Item item = owner.getProductItem(index);
            long stock = owner.getProductStock(index);
            if (item == null || stock <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(item);
            int maxStack = Math.max(1, new ItemStack(item).getMaxStackSize());
            stack.setCount((int) Math.min(stock, (long) maxStack));
            return stack;
        } catch (Throwable e) {
            log.error("MobFarmConnection.getStackInSlot error", e);
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        try {
            int index = slotToIndex(slot);
            if (index < 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            Item item = owner.getProductItem(index);
            long stock = owner.getProductStock(index);
            if (item == null || stock <= 0) {
                return ItemStack.EMPTY;
            }
            long got = Math.min(stock, amount);
            if (!simulate) {
                owner.extractItems(index, got);
            }
            ItemStack stack = new ItemStack(item);
            stack.setCount((int) got);
            return stack;
        } catch (Throwable e) {
            log.error("MobFarmConnection.extractItem error", e);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return false;
    }
}
