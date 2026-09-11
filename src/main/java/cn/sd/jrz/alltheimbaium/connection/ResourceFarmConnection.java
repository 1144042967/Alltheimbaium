package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 通用资源农场对外 IItemHandler（只读，仅供抽取），语义同 MobFarmConnection。
 */
public class ResourceFarmConnection implements IItemHandler {
    private static final Logger log = LoggerFactory.getLogger(ResourceFarmConnection.class);
    private final ResourceFarmEntity owner;
    @Nullable
    private final Direction side;

    public ResourceFarmConnection(ResourceFarmEntity owner, @Nullable Direction side) {
        this.owner = owner;
        this.side = side;
    }

    private int resolveState() {
        if (side == null) {
            return ResourceFarmEntity.STATE_RANDOM;
        }
        return owner.getDirectionState(side);
    }

    /** 能力槽位号 → 产物行索引；不可访问返回 -1 */
    private int slotToIndex(int slot) {
        int state = resolveState();
        if (state == ResourceFarmEntity.STATE_RANDOM) {
            return slot >= 0 && slot < owner.getProductCount() ? slot : -1;
        }
        if (state >= ResourceFarmEntity.STATE_SLOT_BASE) {
            int idx = state - ResourceFarmEntity.STATE_SLOT_BASE;
            return slot == 0 && idx < owner.getProductCount() ? idx : -1;
        }
        return -1;
    }

    @Override
    public int getSlots() {
        try {
            int state = resolveState();
            if (state == ResourceFarmEntity.STATE_RANDOM) {
                return owner.getProductCount();
            }
            if (state >= ResourceFarmEntity.STATE_SLOT_BASE) {
                int idx = state - ResourceFarmEntity.STATE_SLOT_BASE;
                return idx < owner.getProductCount() ? 1 : 0;
            }
            return 0;
        } catch (Throwable e) {
            log.error("ResourceFarmConnection.getSlots error", e);
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
            // 管道查询要返回真实存量：不能按物品自身的堆叠上限（通常 64）截断，
            // 否则管道只能看到 64，取不走本模组的大数存量。超过 int 的部分夹到 Integer.MAX_VALUE。
            ItemStack stack = new ItemStack(item);
            stack.setCount(Tool.suitInt(stock));
            return stack;
        } catch (Throwable e) {
            log.error("ResourceFarmConnection.getStackInSlot error", e);
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
            log.error("ResourceFarmConnection.extractItem error", e);
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
