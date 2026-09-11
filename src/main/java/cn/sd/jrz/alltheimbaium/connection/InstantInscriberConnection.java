package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity.Row;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 零刻压印器对外 IItemHandler（行为与方向无关）。
 * <p>
 * 暴露 27 个逻辑槽位：0~17 输入行（插入并入大数）、18~26 输出行（抽取）。
 */
public class InstantInscriberConnection implements IItemHandler {
    private static final Logger log = LoggerFactory.getLogger(InstantInscriberConnection.class);
    private final InstantInscriberEntity owner;

    public InstantInscriberConnection(InstantInscriberEntity owner) {
        this.owner = owner;
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= InstantInscriberEntity.INPUT_MAX_TYPES
                && slot < InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;
    }

    private static boolean isInputSlot(int slot) {
        return slot >= 0 && slot < InstantInscriberEntity.INPUT_MAX_TYPES;
    }

    @Override
    public int getSlots() {
        return InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        try {
            if (isOutputSlot(slot)) {
                int index = slot - InstantInscriberEntity.INPUT_MAX_TYPES;
                return withFullStock(owner.getOutputStack(index), owner.getOutputStock(index));
            }
            if (isInputSlot(slot)) {
                return withFullStock(owner.getInputStack(slot), owner.getInputStock(slot));
            }
        } catch (Throwable e) {
            log.error("InstantInscriberConnection.getStackInSlot error", e);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 把 {@code count = 1} 的模板换成真实存量。
     * <p>
     * 实体的 {@code getInputStack}/{@code getOutputStack} 是给 GUI 用的（GUI 自己画缩写存量，
     * 槽位里只能放 1 个），管道查询则要拿到全部数量，否则只能取走 1 个。
     * 超过 int 的部分夹到 {@link Integer#MAX_VALUE}。
     */
    @Nonnull
    private static ItemStack withFullStock(@Nonnull ItemStack template, long stock) {
        if (template.isEmpty() || stock <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = template.copy();
        stack.setCount(Tool.suitInt(stock));
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        try {
            if (isInputSlot(slot)) {
                return owner.insertInput(stack, simulate);
            }
        } catch (Throwable e) {
            log.error("InstantInscriberConnection.insertItem error", e);
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
            int index = slot - InstantInscriberEntity.INPUT_MAX_TYPES;
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
            log.error("InstantInscriberConnection.extractItem error", e);
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
