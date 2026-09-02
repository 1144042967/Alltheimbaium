package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 存储方块制造机对外 IItemHandler（只读，仅供抽取）。
 * <p>
 * 关联访问方向后，被动抽取（管道/漏斗等从该面抽取）与主动输出遵循同一套方向配置：
 * 指定槽 N 的面只能抽出该槽物品；随机面可抽出全部有存量物品；禁用面不提供任何物品。
 * side 为 null 表示不限定方向（按随机全量处理，兼容部分无方向查询的调用方）。
 */
public class StorageFountainConnection implements IItemHandler {
    private static final Logger log = LoggerFactory.getLogger(StorageFountainConnection.class);
    private final StorageFountainEntity owner;
    /** 访问本能力时所在的方向，null 表示不限定方向 */
    @Nullable
    private final Direction side;

    public StorageFountainConnection(StorageFountainEntity owner, @Nullable Direction side) {
        this.owner = owner;
        this.side = side;
    }

    /**
     * 该方向当前的输出状态（不限定方向时按随机处理）
     */
    private int resolveState() {
        if (side == null) {
            return StorageFountainEntity.STATE_RANDOM;
        }
        return owner.getDirectionState(side);
    }

    /**
     * 能力槽位号 → 已标记物品索引；该面不可访问时返回 -1
     */
    private int slotToIndex(int slot) {
        int state = resolveState();
        if (state == StorageFountainEntity.STATE_RANDOM) {
            // 随机：能力槽位号与已标记物品索引一一对应
            return slot >= 0 && slot < owner.itemList.size() ? slot : -1;
        }
        if (state >= StorageFountainEntity.STATE_SLOT_BASE) {
            // 指定槽 N：能力上仅暴露 1 个槽位，映射到已标记列表中的索引 N
            int idx = state - StorageFountainEntity.STATE_SLOT_BASE;
            return slot == 0 && idx < owner.itemList.size() ? idx : -1;
        }
        // 禁用：无任何可抽取槽位
        return -1;
    }

    @Override
    public int getSlots() {
        try {
            int state = resolveState();
            if (state == StorageFountainEntity.STATE_RANDOM) {
                return owner.itemList.size();
            }
            if (state >= StorageFountainEntity.STATE_SLOT_BASE) {
                int idx = state - StorageFountainEntity.STATE_SLOT_BASE;
                return idx < owner.itemList.size() ? 1 : 0;
            }
            // 禁用
            return 0;
        } catch (Throwable e) {
            log.error("StorageFountainConnection.getSlots error", e);
        }
        return 0;
    }

    @Override
    public @Nonnull ItemStack getStackInSlot(int slot) {
        try {
            int index = slotToIndex(slot);
            if (index < 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = owner.itemList.get(index);
            Long count = owner.blockList.get(index);
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            stack = stack.copy();
            stack.setCount(Tool.suitInt(count / StorageFountainBlock.getCarry()));
            return stack;
        } catch (Throwable e) {
            log.error("StorageFountainConnection.getStackInSlot error", e);
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
            int index = slotToIndex(slot);
            if (index < 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = owner.itemList.get(index);
            Long block = owner.blockList.get(index);
            int maxAmount = Tool.suitInt(block / StorageFountainBlock.getCarry());
            if (maxAmount <= 0) {
                return ItemStack.EMPTY;
            }
            int ret = Math.min(maxAmount, amount);
            if (!simulate) {
                owner.blockList.set(index, block - ret * StorageFountainBlock.getCarry());
                owner.setChanged();
            }
            stack = stack.copy();
            stack.setCount(ret);
            return stack;
        } catch (Throwable e) {
            log.error("StorageFountainConnection.extractItem error", e);
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
