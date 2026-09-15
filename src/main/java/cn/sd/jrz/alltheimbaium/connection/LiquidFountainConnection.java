package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 液体无限制造机对外流体能力（方向无关，单罐）。
 * <p>
 * 26.x：旧的 {@code IFluidHandler} 已标 {@code forRemoval} 且没有"旧→新"适配器，
 * 能力类型换成新传输 API 的 {@link ResourceHandler}&lt;{@link FluidResource}&gt;：
 * 罐内流体由 {@link #getResource} 描述、容量与存量分别由
 * {@link #getCapacityAsLong} / {@link #getAmountAsLong} 给出，灌入/抽出都走事务。
 * <p>
 * 语义与旧实现一致：未达无限阈值时可灌入（单一种流体，上限为配置阈值）；
 * 达到阈值后变为无限——此时不可再灌入，抽出多少都不扣内部存量。
 */
public class LiquidFountainConnection implements ResourceHandler<FluidResource> {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainConnection.class);
    private final LiquidFountainEntity owner;
    private final FluidJournal journal = new FluidJournal();

    public LiquidFountainConnection(LiquidFountainEntity owner) {
        this.owner = owner;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    @Nonnull
    public FluidResource getResource(int index) {
        try {
            return owner.stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(owner.stack);
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.getResource error", e);
        }
        return FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        try {
            return Math.max(0, owner.stack.getAmount());
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.getAmountAsLong error", e);
        }
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, @Nonnull FluidResource resource) {
        // 未无限时容量为配置阈值，无限后返回最大值
        return isInfinity() ? Integer.MAX_VALUE : Tool.suitInt(LiquidFountainBlock.getMax());
    }

    @Override
    public boolean isValid(int index, @Nonnull FluidResource resource) {
        try {
            return !isInfinity() && (owner.stack.isEmpty() || resource.matches(owner.stack));
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.isValid error", e);
        }
        return false;
    }

    @Override
    public int insert(int index, @Nonnull FluidResource resource, int amount, @Nonnull TransactionContext transaction) {
        try {
            if (isInfinity() || amount <= 0) {
                return 0;
            }
            if (!owner.stack.isEmpty() && !resource.matches(owner.stack)) {
                return 0;
            }
            int maxInput = (int) Math.min(amount, Tool.suitInt(LiquidFountainBlock.getMax() - owner.stack.getAmount()));
            if (maxInput <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            if (owner.stack.isEmpty()) {
                owner.stack = resource.toStack(maxInput);
            } else {
                owner.stack.grow(maxInput);
            }
            return maxInput;
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.insert error", e);
        }
        return 0;
    }

    @Override
    public int extract(int index, @Nonnull FluidResource resource, int amount, @Nonnull TransactionContext transaction) {
        try {
            if (amount <= 0 || !resource.matches(owner.stack)) {
                return 0;
            }
            // 无限：内部存量不变，来多少给多少
            if (isInfinity()) {
                return amount;
            }
            if (owner.stack.isEmpty()) {
                return 0;
            }
            int maxOutput = Math.min(owner.stack.getAmount(), amount);
            if (maxOutput <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            owner.stack.grow(-maxOutput);
            return maxOutput;
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.extract error", e);
        }
        return 0;
    }

    /** 是否已达无限阈值（判定同旧实现：存量达到配置阈值即为无限） */
    private boolean isInfinity() {
        try {
            return !owner.stack.isEmpty() && owner.stack.getAmount() >= LiquidFountainBlock.getMax();
        } catch (Throwable e) {
            log.error("LiquidFountainConnection.isInfinity error", e);
        }
        return false;
    }

    /** 事务快照：罐内流体（回滚时整体还原） */
    private final class FluidJournal extends SnapshotJournal<FluidStack> {
        @Override
        protected FluidStack createSnapshot() {
            return owner.stack.copy();
        }

        @Override
        protected void revertToSnapshot(FluidStack snapshot) {
            owner.stack = snapshot;
        }

        @Override
        protected void onRootCommit(FluidStack originalState) {
            owner.setChanged();
        }
    }
}
