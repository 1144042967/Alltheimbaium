package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
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
        return owner.itemList.size();
    }

    @Override
    public @Nonnull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= owner.itemList.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = owner.itemList.get(slot);
        Long count = owner.blockList.get(slot);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        stack = stack.copy();
        stack.setCount(Tool.suitInt(count / StorageFountainBlock.CARRY));
        return stack;
    }

    @Override
    public @Nonnull ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public @Nonnull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= owner.itemList.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = owner.itemList.get(slot);
        Long block = owner.blockList.get(slot);
        int maxAmount = Tool.suitInt(block / StorageFountainBlock.CARRY);
        if (maxAmount <= 0) {
            return ItemStack.EMPTY;
        }
        int ret = Math.min(maxAmount, amount);
        if (!simulate) {
            owner.blockList.set(slot, block - ret * StorageFountainBlock.CARRY);
            owner.setChanged();
        }
        stack = stack.copy();
        stack.setCount(ret);
        return stack;
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
