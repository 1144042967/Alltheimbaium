package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 自动耕地对外 IItemHandler（只读），方向语义同 ResourceFarm。
 */
public class AutoFarmlandConnection implements IItemHandler {
    private static final Logger log = LoggerFactory.getLogger(AutoFarmlandConnection.class);
    private final AutoFarmlandEntity owner;
    @Nullable
    private final Direction side;

    public AutoFarmlandConnection(AutoFarmlandEntity owner, @Nullable Direction side) {
        this.owner = owner;
        this.side = side;
    }

    private int resolveState() {
        if (side == null) {
            return AutoFarmlandEntity.STATE_RANDOM;
        }
        return owner.getDirectionState(side);
    }

    private int slotToIndex(int slot) {
        int state = resolveState();
        if (state == AutoFarmlandEntity.STATE_RANDOM) {
            return slot >= 0 && slot < owner.getProductCount() ? slot : -1;
        }
        if (state >= AutoFarmlandEntity.STATE_SLOT_BASE) {
            int idx = state - AutoFarmlandEntity.STATE_SLOT_BASE;
            return slot == 0 && idx < owner.getProductCount() ? idx : -1;
        }
        return -1;
    }

    @Override
    public int getSlots() {
        try {
            int state = resolveState();
            if (state == AutoFarmlandEntity.STATE_RANDOM) {
                return owner.getProductCount();
            }
            if (state >= AutoFarmlandEntity.STATE_SLOT_BASE) {
                int idx = state - AutoFarmlandEntity.STATE_SLOT_BASE;
                return idx < owner.getProductCount() ? 1 : 0;
            }
            return 0;
        } catch (Throwable e) {
            log.error("AutoFarmlandConnection.getSlots error", e);
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
            int maxStack = Math.max(1, new ItemStack(item).getMaxStackSize());
            return new ItemStack(item, (int) Math.min(stock, (long) maxStack));
        } catch (Throwable e) {
            log.error("AutoFarmlandConnection.getStackInSlot error", e);
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
            return new ItemStack(item, (int) got);
        } catch (Throwable e) {
            log.error("AutoFarmlandConnection.extractItem error", e);
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
