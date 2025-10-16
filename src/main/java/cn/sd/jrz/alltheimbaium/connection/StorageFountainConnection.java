package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

public class StorageFountainConnection implements IItemHandler {
    private final StorageFountainEntity owner;

    public StorageFountainConnection(StorageFountainEntity owner) {
        this.owner = owner;
    }

    @Override
    public int getSlots() {
        return owner.saveItems.length;
    }

    @Override
    public @Nonnull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= owner.saveItems.length) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = owner.saveItems[slot];
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copy();
    }

    @Override
    public @Nonnull ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public @Nonnull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= owner.saveItems.length) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = owner.saveItems[slot];
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int ret = Math.min(stack.getCount(), amount);
        if (!simulate) {
            stack.setCount(Tool.suitInt(stack.getCount() - ret));
            owner.setChanged();
        }
        ItemStack copy = stack.copy();
        copy.setCount(ret);
        return copy;
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
