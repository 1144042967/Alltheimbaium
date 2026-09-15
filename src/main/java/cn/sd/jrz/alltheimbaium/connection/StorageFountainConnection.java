package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
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
 * 存储方块制造机对外物品能力（只读，仅供抽取）。
 * <p>
 * 关联访问方向后，被动抽取（管道/漏斗等从该面抽取）与主动输出遵循同一套方向配置：
 * 指定槽 N 的面只能抽出该槽物品；随机面可抽出全部有存量物品；禁用面不提供任何物品。
 * side 为 null 表示不限定方向（按随机全量处理，兼容部分无方向查询的调用方）。
 * <p>
 * 26.x：改为新传输 API 的 {@link ResourceHandler}&lt;{@link ItemResource}&gt;。存量以内部计数
 * （{@code carry} 为一个整件的单位）保存，对外一律换算成整件数。
 */
public class StorageFountainConnection implements ResourceHandler<ItemResource> {
    private static final Logger log = LoggerFactory.getLogger(StorageFountainConnection.class);
    private final StorageFountainEntity owner;
    /** 访问本能力时所在的方向，null 表示不限定方向 */
    @Nullable
    private final Direction side;
    private final BlockJournal journal = new BlockJournal();

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
    public int size() {
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
            log.error("StorageFountainConnection.size error", e);
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
            ItemStack stack = owner.itemList.get(slot);
            if (stack.isEmpty()) {
                return ItemResource.EMPTY;
            }
            return ItemResource.of(stack);
        } catch (Throwable e) {
            log.error("StorageFountainConnection.getResource error", e);
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
            long count = owner.blockList.get(slot);
            // 内部计数换算成整件数
            return Math.max(0, count / StorageFountainBlock.getCarry());
        } catch (Throwable e) {
            log.error("StorageFountainConnection.getAmountAsLong error", e);
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
            ItemStack stack = owner.itemList.get(slot);
            if (stack.isEmpty() || !resource.matches(stack)) {
                return 0;
            }
            int maxAmount = Tool.suitInt(owner.blockList.get(slot) / StorageFountainBlock.getCarry());
            if (maxAmount <= 0) {
                return 0;
            }
            int got = Math.min(maxAmount, amount);
            if (got <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            owner.extractItems(slot, got);
            return got;
        } catch (Throwable e) {
            log.error("StorageFountainConnection.extract error", e);
        }
        return 0;
    }

    /** 事务快照：各槽位的内部计数（提取会扣减单位数，回滚必须整表还原） */
    private static final class BlocksState {
        /** 被快照的列表引用（实体在读档时会整个换掉 blockList，回滚要写回原来那一个） */
        final List<Long> list;
        final List<Long> values;

        BlocksState(List<Long> list, List<Long> values) {
            this.list = list;
            this.values = values;
        }
    }

    private final class BlockJournal extends SnapshotJournal<BlocksState> {
        @Override
        protected BlocksState createSnapshot() {
            List<Long> current = owner.blockList;
            return new BlocksState(current, new ArrayList<>(current));
        }

        @Override
        protected void revertToSnapshot(BlocksState snapshot) {
            snapshot.list.clear();
            snapshot.list.addAll(snapshot.values);
        }

        @Override
        protected void onRootCommit(BlocksState originalState) {
            owner.setChanged();
        }
    }
}
