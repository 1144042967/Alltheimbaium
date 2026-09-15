package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动耕地对外物品能力（只读），方向语义同 ResourceFarm。
 * <p>
 * 26.x：旧的 {@code IItemHandler} 已标 {@code forRemoval} 且没有"旧→新"适配器，
 * 能力类型换成新传输 API 的 {@link ResourceHandler}&lt;{@link ItemResource}&gt;。
 * 数量与资源类型分开返回（{@link #getResource} + {@link #getAmountAsLong}），
 * 因此不再需要把存量塞进 {@code ItemStack} 的 count 里。
 * <p>
 * 只允许抽取（{@link #insert} 恒返回 0），扣减存量必须走事务：先
 * {@link SnapshotJournal#updateSnapshots} 记录快照，事务回滚时恢复整张产物行表。
 */
public class AutoFarmlandConnection implements ResourceHandler<ItemResource> {
    private static final Logger log = LoggerFactory.getLogger(AutoFarmlandConnection.class);
    private final AutoFarmlandEntity owner;
    @Nullable
    private final Direction side;
    private final RowJournal journal = new RowJournal();

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
    public int size() {
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
            log.error("AutoFarmlandConnection.size error", e);
        }
        return 0;
    }

    @Override
    @Nonnull
    public ItemResource getResource(int index) {
        try {
            int slot = slotToIndex(index);
            if (slot < 0) {
                return ItemResource.EMPTY;
            }
            Item item = owner.getProductItem(slot);
            long stock = owner.getProductStock(slot);
            if (item == null || stock <= 0) {
                return ItemResource.EMPTY;
            }
            return ItemResource.of(item);
        } catch (Throwable e) {
            log.error("AutoFarmlandConnection.getResource error", e);
        }
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        try {
            int slot = slotToIndex(index);
            if (slot < 0) {
                return 0;
            }
            long stock = owner.getProductStock(slot);
            return stock > 0 ? stock : 0;
        } catch (Throwable e) {
            log.error("AutoFarmlandConnection.getAmountAsLong error", e);
        }
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, @Nonnull ItemResource resource) {
        // 大数存储：不按物品的堆叠上限截断，否则管道只能看到 64
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int index, @Nonnull ItemResource resource) {
        return false;
    }

    @Override
    public int insert(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        // 只读：不接受任何插入
        return 0;
    }

    @Override
    public int extract(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        try {
            int slot = slotToIndex(index);
            if (slot < 0 || amount <= 0) {
                return 0;
            }
            Item item = owner.getProductItem(slot);
            long stock = owner.getProductStock(slot);
            if (item == null || stock <= 0 || !resource.is(item)) {
                return 0;
            }
            long got = Math.min(stock, amount);
            if (got <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            owner.extractItems(slot, got);
            return (int) got;
        } catch (Throwable e) {
            log.error("AutoFarmlandConnection.extract error", e);
        }
        return 0;
    }

    /** 事务快照：产物行表 + 每行存量（提取会因行清空而删行，回滚必须连行带值一起还原） */
    private static final class RowsState {
        final List<AutoFarmlandEntity.Row> rows;
        final long[] stocks;

        RowsState(List<AutoFarmlandEntity.Row> rows, long[] stocks) {
            this.rows = rows;
            this.stocks = stocks;
        }
    }

    private final class RowJournal extends SnapshotJournal<RowsState> {
        @Override
        protected RowsState createSnapshot() {
            List<AutoFarmlandEntity.Row> current = owner.rows;
            long[] stocks = new long[current.size()];
            for (int i = 0; i < current.size(); i++) {
                stocks[i] = current.get(i).stock;
            }
            return new RowsState(new ArrayList<>(current), stocks);
        }

        @Override
        protected void revertToSnapshot(RowsState snapshot) {
            for (int i = 0; i < snapshot.stocks.length; i++) {
                snapshot.rows.get(i).stock = snapshot.stocks[i];
            }
            owner.rows.clear();
            owner.rows.addAll(snapshot.rows);
        }

        @Override
        protected void onRootCommit(RowsState originalState) {
            owner.setChanged();
        }
    }
}
