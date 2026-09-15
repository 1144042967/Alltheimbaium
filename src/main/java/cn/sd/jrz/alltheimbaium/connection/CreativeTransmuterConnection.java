package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.CreativeTransmuterEntity;
import cn.sd.jrz.alltheimbaium.setup.TransmuteCatalog;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 创造物品质变器对外物品能力（方向无关）。
 * <p>
 * 槽位与机器内部一一对应：槽 0~8 是输入栏，槽 9 是输出栏。两个方向不对称——
 * <ul>
 *     <li><b>输入栏</b>：可插入、不可抽取（材料进得去拿不回，避免管道把刚放进去的材料又抽走），
 *         且只接受配方内的材料，无关物品塞不进来；</li>
 *     <li><b>输出栏</b>：可抽取、不可插入（产物只能由机器自己写入）。</li>
 * </ul>
 * 每格上限都是 1，管道一次推入多组时会自动摊到九格里。
 * <p>
 * 26.x：旧的 {@code IItemHandler} 已标 {@code forRemoval}，改为新传输 API 的
 * {@link ResourceHandler}&lt;{@link ItemResource}&gt;。内部物品栏仍是实体的 {@code ItemStackHandler}，
 * 这里做一层只读/单向的视图；写入走事务快照，回滚时整栏还原。
 */
public class CreativeTransmuterConnection implements ResourceHandler<ItemResource> {
    private static final Logger log = LoggerFactory.getLogger(CreativeTransmuterConnection.class);
    private final CreativeTransmuterEntity owner;
    private final InventoryJournal journal = new InventoryJournal();

    public CreativeTransmuterConnection(CreativeTransmuterEntity owner) {
        this.owner = owner;
    }

    private static boolean isInputSlot(int slot) {
        return slot >= 0 && slot < CreativeTransmuterEntity.INPUT_SLOTS;
    }

    @Nonnull
    private ItemStack stackAt(int slot) {
        if (slot < 0 || slot >= CreativeTransmuterEntity.SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return owner.getInventory().getStackInSlot(slot);
    }

    @Override
    public int size() {
        return CreativeTransmuterEntity.SLOT_COUNT;
    }

    @Override
    @Nonnull
    public ItemResource getResource(int index) {
        try {
            ItemStack current = stackAt(index);
            return current.isEmpty() ? ItemResource.EMPTY : ItemResource.of(current);
        } catch (Throwable e) {
            log.error("CreativeTransmuterConnection.getResource error", e);
        }
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        try {
            return stackAt(index).getCount();
        } catch (Throwable e) {
            log.error("CreativeTransmuterConnection.getAmountAsLong error", e);
        }
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, @Nonnull ItemResource resource) {
        // 每格上限 1（输入栏与输出栏一致）
        return 1;
    }

    @Override
    public boolean isValid(int index, @Nonnull ItemResource resource) {
        try {
            return isInputSlot(index) && TransmuteCatalog.isValidInput(resource.toStack(1));
        } catch (Throwable e) {
            log.error("CreativeTransmuterConnection.isValid error", e);
        }
        return false;
    }

    @Override
    public int insert(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        try {
            // 输出栏不收外部放入
            if (!isInputSlot(index) || amount <= 0) {
                return 0;
            }
            if (!TransmuteCatalog.isValidInput(resource.toStack(1))) {
                return 0;
            }
            ItemStack current = owner.getInventory().getStackInSlot(index);
            if (!current.isEmpty() && !resource.matches(current)) {
                return 0;
            }
            int inserted = Math.min(amount, 1 - current.getCount());
            if (inserted <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            owner.getInventory().insertItem(index, resource.toStack(inserted), false);
            return inserted;
        } catch (Throwable e) {
            log.error("CreativeTransmuterConnection.insert error", e);
        }
        return 0;
    }

    @Override
    public int extract(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        try {
            // 输入栏不可抽取
            if (index != CreativeTransmuterEntity.OUTPUT_SLOT || amount <= 0) {
                return 0;
            }
            ItemStack current = owner.getInventory().getStackInSlot(index);
            if (current.isEmpty() || !resource.matches(current)) {
                return 0;
            }
            int got = Math.min(amount, current.getCount());
            if (got <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            owner.getInventory().extractItem(index, got, false);
            return got;
        } catch (Throwable e) {
            log.error("CreativeTransmuterConnection.extract error", e);
        }
        return 0;
    }

    /** 事务快照：整栏物品（槽位少、每格只 1 个，直接整表拷贝） */
    private final class InventoryJournal extends SnapshotJournal<ItemStack[]> {
        @Override
        protected ItemStack[] createSnapshot() {
            ItemStack[] snapshot = new ItemStack[CreativeTransmuterEntity.SLOT_COUNT];
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i] = owner.getInventory().getStackInSlot(i).copy();
            }
            return snapshot;
        }

        @Override
        protected void revertToSnapshot(ItemStack[] snapshot) {
            for (int i = 0; i < snapshot.length; i++) {
                owner.getInventory().setStackInSlot(i, snapshot[i]);
            }
        }

        @Override
        protected void onRootCommit(ItemStack[] originalState) {
            owner.setChanged();
        }
    }
}
