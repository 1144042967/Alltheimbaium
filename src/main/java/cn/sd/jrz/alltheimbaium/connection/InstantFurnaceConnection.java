package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity;
import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity.Row;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 零刻熔炉对外 IItemHandler（行为与访问方向无关）。
 * <p>
 * 对外暴露 36 个逻辑槽位：0~17 输入行、18~35 输出行。
 * 插入只在输入区生效（按物品种类并入对应输入行，大数无 64 上限）；抽取只在输出区生效（按输出行扣减）。
 */
public class InstantFurnaceConnection implements IItemHandler {
    private static final Logger log = LoggerFactory.getLogger(InstantFurnaceConnection.class);
    private final InstantFurnaceEntity owner;

    public InstantFurnaceConnection(InstantFurnaceEntity owner) {
        this.owner = owner;
    }

    /** 是否落在输出区（18~35） */
    private static boolean isOutputSlot(int slot) {
        return slot >= InstantFurnaceEntity.MAX_TYPES && slot < InstantFurnaceEntity.MAX_TYPES * 2;
    }

    /** 是否落在输入区（0~17） */
    private static boolean isInputSlot(int slot) {
        return slot >= 0 && slot < InstantFurnaceEntity.MAX_TYPES;
    }

    @Override
    public int getSlots() {
        return InstantFurnaceEntity.MAX_TYPES * 2;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        try {
            if (isOutputSlot(slot)) {
                return owner.getOutputStack(slot - InstantFurnaceEntity.MAX_TYPES);
            }
            if (isInputSlot(slot)) {
                return owner.getInputStack(slot);
            }
        } catch (Throwable e) {
            log.error("InstantFurnaceConnection.getStackInSlot error", e);
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        try {
            // 只允许插入输入区；大数存储整组并入对应输入行
            if (isInputSlot(slot)) {
                return owner.insertInput(stack, simulate);
            }
        } catch (Throwable e) {
            log.error("InstantFurnaceConnection.insertItem error", e);
        }
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        try {
            if (!isOutputSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            int index = slot - InstantFurnaceEntity.MAX_TYPES;
            if (index < 0 || index >= owner.getOutputCount()) {
                return ItemStack.EMPTY;
            }
            Row row = owner.outputRows.get(index);
            if (row == null || row.stock <= 0) {
                return ItemStack.EMPTY;
            }
            long got = Math.min(row.stock, amount);
            if (!simulate) {
                owner.extractOutputItems(index, got);
            }
            return new ItemStack(row.item, (int) got);
        } catch (Throwable e) {
            log.error("InstantFurnaceConnection.extractItem error", e);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return isInputSlot(slot);
    }
}
