package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.FarmEntity;
import cn.sd.jrz.alltheimbaium.setup.DataConfig;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

public class FarmConnection implements IItemHandler {
    private final FarmEntity owner;
    public final DataConfig config;

    public FarmConnection(FarmEntity owner) {
        this.owner = owner;
        this.config = owner.config;
    }

    @Override
    public int getSlots() {
        return config.getProductList().size();
    }

    @Override
    public @Nonnull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= config.getProductList().size()) {
            return ItemStack.EMPTY;
        }
        Item item = config.getProductList().get(slot).item;
        int maxOutput = Tool.suitInt(owner.outputArray[slot] / 1000);
        return new ItemStack(item, maxOutput);
    }

    @Override
    public @Nonnull ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public @Nonnull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= config.getProductList().size()) {
            return ItemStack.EMPTY;
        }
        int maxOutput = Tool.suitInt(owner.outputArray[slot] / 1000);
        if (maxOutput <= 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int ret = Math.min(maxOutput, amount);
        if (!simulate) {
            owner.outputArray[slot] -= ret * 1000L;
            owner.setChanged();
        }
        Item item = config.getProductList().get(slot).item;
        return new ItemStack(item, ret);
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
