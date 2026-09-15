package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
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
 * 生物农场对外物品能力（只读，仅供抽取）。
 * <p>
 * 关联访问方向后，被动抽取与主动输出遵循同一套方向配置：
 * 随机面可抽全部有存量行；槽 N 面只能抽该行；禁用面不提供任何物品；side 为 null 按随机处理。
 * <p>
 * 26.x：改为新传输 API 的 {@link ResourceHandler}&lt;{@link ItemResource}&gt;，只允许抽取，
 * 扣减存量走事务快照（回滚时连被清空的行一起还原）。
 */
public class MobFarmConnection implements ResourceHandler<ItemResource> {
    private static final Logger log = LoggerFactory.getLogger(MobFarmConnection.class);
    private final MobFarmEntity owner;
    @Nullable
    private final Direction side;
    private final RowJournal journal = new RowJournal();

    public MobFarmConnection(MobFarmEntity owner, @Nullable Direction side) {
        this.owner = owner;
        this.side = side;
    }

    private int resolveState() {
        if (side == null) {
            return MobFarmEntity.STATE_RANDOM;
        }
        return owner.getDirectionState(side);
    }

    /** 能力槽位号 → 产物行索引；不可访问返回 -1 */
    private int slotToIndex(int slot) {
        int state = resolveState();
        if (state == MobFarmEntity.STATE_RANDOM) {
            return slot >= 0 && slot < owner.getProductCount() ? slot : -1;
        }
        if (state >= MobFarmEntity.STATE_SLOT_BASE) {
            int idx = state - MobFarmEntity.STATE_SLOT_BASE;
            return slot == 0 && idx < owner.getProductCount() ? idx : -1;
        }
        return -1;
    }

    @Override
    public int size() {
        try {
            int state = resolveState();
            if (state == MobFarmEntity.STATE_RANDOM) {
                return owner.getProductCount();
            }
            if (state >= MobFarmEntity.STATE_SLOT_BASE) {
                int idx = state - MobFarmEntity.STATE_SLOT_BASE;
                return idx < owner.getProductCount() ? 1 : 0;
            }
            return 0;
        } catch (Throwable e) {
            log.error("MobFarmConnection.size error", e);
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
            log.error("MobFarmConnection.getResource error", e);
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
            log.error("MobFarmConnection.getAmountAsLong error", e);
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
            log.error("MobFarmConnection.extract error", e);
        }
        return 0;
    }

    /** 事务快照：产物行表 + 每行存量（提取会因行清空而删行，回滚必须连行带值一起还原） */
    private static final class RowsState {
        final List<MobFarmEntity.Row> rows;
        final long[] stocks;

        RowsState(List<MobFarmEntity.Row> rows, long[] stocks) {
            this.rows = rows;
            this.stocks = stocks;
        }
    }

    private final class RowJournal extends SnapshotJournal<RowsState> {
        @Override
        protected RowsState createSnapshot() {
            List<MobFarmEntity.Row> current = owner.rows;
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
