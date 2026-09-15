package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity.Row;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * 零刻压印器对外物品能力（行为与方向无关）。
 * <p>
 * 暴露 27 个逻辑槽位：0~17 输入行（插入并入大数）、18~26 输出行（抽取）。
 * <p>
 * 26.x：改为新传输 API 的 {@link ResourceHandler}&lt;{@link ItemResource}&gt;。数量与方法都变了：
 * 旧 {@code getSlotLimit} 恒为 {@link Integer#MAX_VALUE} 的语义由 {@link #getCapacityAsLong} 承担；
 * 插入返回"已插入数量"而非剩余栈；扣减存量一律走事务快照——输入行可能新建、输出行可能被清空删除，
 * 回滚必须把整张行表连值一起还原。
 */
public class InstantInscriberConnection implements ResourceHandler<ItemResource> {
    private static final Logger log = LoggerFactory.getLogger(InstantInscriberConnection.class);
    private final InstantInscriberEntity owner;
    private final RowsJournal inputJournal;
    private final RowsJournal outputJournal;

    public InstantInscriberConnection(InstantInscriberEntity owner) {
        this.owner = owner;
        this.inputJournal = new RowsJournal(owner.inputRows);
        this.outputJournal = new RowsJournal(owner.outputRows);
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= InstantInscriberEntity.INPUT_MAX_TYPES
                && slot < InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;
    }

    private static boolean isInputSlot(int slot) {
        return slot >= 0 && slot < InstantInscriberEntity.INPUT_MAX_TYPES;
    }

    @Override
    public int size() {
        return InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;
    }

    @Override
    @Nonnull
    public ItemResource getResource(int index) {
        try {
            if (isOutputSlot(index)) {
                return resourceAt(owner.outputRows, index - InstantInscriberEntity.INPUT_MAX_TYPES);
            }
            if (isInputSlot(index)) {
                return resourceAt(owner.inputRows, index);
            }
        } catch (Throwable e) {
            log.error("InstantInscriberConnection.getResource error", e);
        }
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        try {
            if (isOutputSlot(index)) {
                return stockAt(owner.outputRows, index - InstantInscriberEntity.INPUT_MAX_TYPES);
            }
            if (isInputSlot(index)) {
                return stockAt(owner.inputRows, index);
            }
        } catch (Throwable e) {
            log.error("InstantInscriberConnection.getAmountAsLong error", e);
        }
        return 0;
    }

    @Nonnull
    private static ItemResource resourceAt(@Nonnull List<Row> rows, int index) {
        if (index < 0 || index >= rows.size()) {
            return ItemResource.EMPTY;
        }
        Row row = rows.get(index);
        return row.stock > 0 ? ItemResource.of(row.item) : ItemResource.EMPTY;
    }

    private static long stockAt(@Nonnull List<Row> rows, int index) {
        if (index < 0 || index >= rows.size()) {
            return 0;
        }
        long stock = rows.get(index).stock;
        return stock > 0 ? stock : 0;
    }

    @Override
    public long getCapacityAsLong(int index, @Nonnull ItemResource resource) {
        // 大数存储：不按物品的堆叠上限截断，否则管道只能看到 64
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int index, @Nonnull ItemResource resource) {
        return isInputSlot(index);
    }

    @Override
    public int insert(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        try {
            if (!isInputSlot(index) || amount <= 0) {
                return 0;
            }
            if (!canInputAccept(resource)) {
                return 0;
            }
            inputJournal.updateSnapshots(transaction);
            ItemStack left = owner.insertInput(resource.toStack(amount), false);
            return amount - left.getCount();
        } catch (Throwable e) {
            log.error("InstantInscriberConnection.insert error", e);
        }
        return 0;
    }

    /** 输入区能否再收下该物品：已有同种类行，或种类数未满 */
    private boolean canInputAccept(@Nonnull ItemResource resource) {
        for (Row row : owner.inputRows) {
            if (row.item == resource.getItem()) {
                return true;
            }
        }
        return owner.inputRows.size() < InstantInscriberEntity.INPUT_MAX_TYPES;
    }

    @Override
    public int extract(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        try {
            if (!isOutputSlot(index) || amount <= 0) {
                return 0;
            }
            int row = index - InstantInscriberEntity.INPUT_MAX_TYPES;
            if (row < 0 || row >= owner.getOutputCount()) {
                return 0;
            }
            Row target = owner.outputRows.get(row);
            if (target == null || target.stock <= 0 || !resource.is(target.item)) {
                return 0;
            }
            long got = Math.min(target.stock, amount);
            if (got <= 0) {
                return 0;
            }
            outputJournal.updateSnapshots(transaction);
            owner.extractOutputItems(row, got);
            return (int) got;
        } catch (Throwable e) {
            log.error("InstantInscriberConnection.extract error", e);
        }
        return 0;
    }

    /** 事务快照：一张行表 + 各行存量（新建行 / 清空删行都要能还原） */
    private static final class RowsState {
        final List<Row> rows;
        final long[] stocks;

        RowsState(List<Row> rows, long[] stocks) {
            this.rows = rows;
            this.stocks = stocks;
        }
    }

    private final class RowsJournal extends SnapshotJournal<RowsState> {
        /** 被监视的行表（实体内部的输入 / 输出行表引用） */
        private final List<Row> target;

        RowsJournal(List<Row> target) {
            this.target = target;
        }

        @Override
        protected RowsState createSnapshot() {
            long[] stocks = new long[target.size()];
            for (int i = 0; i < target.size(); i++) {
                stocks[i] = target.get(i).stock;
            }
            return new RowsState(new ArrayList<>(target), stocks);
        }

        @Override
        protected void revertToSnapshot(RowsState snapshot) {
            for (int i = 0; i < snapshot.stocks.length; i++) {
                snapshot.rows.get(i).stock = snapshot.stocks[i];
            }
            target.clear();
            target.addAll(snapshot.rows);
        }

        @Override
        protected void onRootCommit(RowsState originalState) {
            owner.setChanged();
        }
    }
}
