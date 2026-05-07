package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.FarmEntity;
import cn.sd.jrz.alltheimbaium.setup.DataConfig;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class FarmConnection implements IItemHandler {
    private static final Logger log = LoggerFactory.getLogger(FarmConnection.class);
    private final FarmEntity owner;
    public final DataConfig config;

    public FarmConnection(FarmEntity owner) {
        this.owner = owner;
        this.config = owner.config;
    }

    @Override
    public int getSlots() {
        try {
            return config.getProductList().size();
        } catch (Throwable e) {
            log.error("FarmConnection.getSlots error", e);
        }
        return 0;
    }

    @Override
    public @Nonnull ItemStack getStackInSlot(int slot) {
        try {
            if (slot < 0 || slot >= config.getProductList().size()) {
                return ItemStack.EMPTY;
            }
            Item item = config.getProductList().get(slot).item;
            int maxOutput = Tool.suitInt(owner.saveArray[slot]);
            return new ItemStack(item, maxOutput);
        } catch (Throwable e) {
            log.error("FarmConnection.getStackInSlot error", e);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @Nonnull ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public @Nonnull ItemStack extractItem(int slot, int amount, boolean simulate) {
        try {
            if (slot < 0 || slot >= config.getProductList().size()) {
                return ItemStack.EMPTY;
            }
            int maxOutput = Tool.suitInt(owner.saveArray[slot]);
            if (maxOutput <= 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int ret = Math.min(maxOutput, amount);
            if (!simulate) {
                owner.saveArray[slot] = Tool.suit(owner.saveArray[slot] - ret);
                owner.setChanged();
            }
            Item item = config.getProductList().get(slot).item;
            return new ItemStack(item, ret);
        } catch (Throwable e) {
            log.error("FarmConnection.extractItem error", e);
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
