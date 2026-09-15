package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import cn.sd.jrz.alltheimbaium.entity.ExtractionInterfaceEntity;
import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * ATI 取出接口对外能力（只读聚合，方向无关）。
 * <p>
 * 聚合范围由 {@link ExtractionInterfaceEntity#getSources()} 给出——那是沿本模组与联动模组方块
 * 连通搜索得到的产出机器位置，因此隔着多台机器也能取到，不再限于相邻六面。
 * 物品以 {@code direction = null}（全量只读）解析；液体来自液体无限制造机与 AutoResource 的流体生成器。
 * <p>
 * 零刻熔炉与零刻压印器不是产出源（见 {@code ExtractionInterfaceEntity.isSource}），
 * 它们只让网络穿过，其中的物品不会被抽走。AutoResource 的机器由
 * {@link ExtractionInterfaceEntity#isLinkedSource} 判定，水车马达同样只传导不产出。
 * <p>
 * 26.x：旧的 {@code IItemHandler} / {@code IFluidHandler} 已标 {@code forRemoval}，
 * 能力类型换成新传输 API 的 {@link ResourceHandler}。但 {@code ResourceHandler<ItemResource>}
 * 与 {@code ResourceHandler<FluidResource>} 擦除后是同一个接口，<b>同一个类无法同时实现两份</b>，
 * 因此这里把聚合逻辑拆成 {@link ItemView} 与 {@link FluidView} 两个视图，
 * 由 {@link #getItemHandler()} / {@link #getFluidHandler()} 分别对外暴露。
 * 视图自身不持有状态，只把调用转发给真实来源的 handler——事务也随之下传，
 * 回滚由各来源机器自己的 {@code SnapshotJournal} 负责。
 * <p>
 * 槽位列表按游戏刻缓存：管道一次取物会连续调用 {@code size} / {@code getResource} /
 * {@code extract}，逐次重扫连通范围会带来数量级的多余开销。
 */
public class ExtractionInterfaceConnection {
    private static final Logger log = LoggerFactory.getLogger(ExtractionInterfaceConnection.class);
    private final ExtractionInterfaceEntity owner;

    /** 一个被聚合的物品槽：真实来源 handler + 其局部槽号 */
    private static final class ItemSlot {
        final ResourceHandler<ItemResource> handler;
        final int slot;

        ItemSlot(ResourceHandler<ItemResource> handler, int slot) {
            this.handler = handler;
            this.slot = slot;
        }
    }

    /** 一个被聚合的液体槽：真实来源 handler + 其局部 tank 号 */
    private static final class TankSlot {
        final ResourceHandler<FluidResource> handler;
        final int tank;

        TankSlot(ResourceHandler<FluidResource> handler, int tank) {
            this.handler = handler;
            this.tank = tank;
        }
    }

    private List<ItemSlot> cachedItemSlots;
    private long cachedItemSlotsTick = Long.MIN_VALUE;
    private List<TankSlot> cachedTankSlots;
    private long cachedTankSlotsTick = Long.MIN_VALUE;

    private final ItemView itemView = new ItemView();
    private final FluidView fluidView = new FluidView();

    public ExtractionInterfaceConnection(ExtractionInterfaceEntity owner) {
        this.owner = owner;
    }

    /** 对外只读物品视图（方向无关） */
    @Nonnull
    public ResourceHandler<ItemResource> getItemHandler() {
        return itemView;
    }

    /** 对外只读流体视图（方向无关），拆视图的原因见类注释 */
    @Nonnull
    public ResourceHandler<FluidResource> getFluidHandler() {
        return fluidView;
    }

    private boolean usable() {
        Level level = owner.getLevel();
        return level != null && !level.isClientSide();
    }

    /** 产出物品的机器（联动模组的机器按其自身能力判定，抽不到物品的自然被下面的空 handler 过滤掉） */
    private static boolean isItemSource(BlockEntity be) {
        return be instanceof StorageFountainEntity
                || be instanceof MobFarmEntity
                || be instanceof ResourceFarmEntity
                || be instanceof AutoFarmlandEntity
                || ExtractionInterfaceEntity.isLinkedSource(be);
    }

    /** 产出流体的机器 */
    private static boolean isFluidSource(BlockEntity be) {
        return be instanceof LiquidFountainEntity
                || ExtractionInterfaceEntity.isLinkedSource(be);
    }

    // ==================== 聚合槽位构建（按刻缓存） ====================

    private List<ItemSlot> itemSlots() {
        if (!usable()) {
            return List.of();
        }
        Level level = owner.getLevel();
        long now = level.getGameTime();
        if (cachedItemSlots == null || cachedItemSlotsTick != now) {
            cachedItemSlots = buildItemSlots(level);
            cachedItemSlotsTick = now;
        }
        return cachedItemSlots;
    }

    private List<TankSlot> tankSlots() {
        if (!usable()) {
            return List.of();
        }
        Level level = owner.getLevel();
        long now = level.getGameTime();
        if (cachedTankSlots == null || cachedTankSlotsTick != now) {
            cachedTankSlots = buildTankSlots(level);
            cachedTankSlotsTick = now;
        }
        return cachedTankSlots;
    }

    /** 收集连通范围内物品机器以 direction=null（全量只读）暴露的每个槽 */
    private List<ItemSlot> buildItemSlots(@Nonnull Level level) {
        List<ItemSlot> slots = new ArrayList<>();
        for (BlockPos pos : owner.getSources()) {
            try {
                BlockEntity be = level.getBlockEntity(pos);
                if (be == null || !isItemSource(be)) {
                    continue;
                }
                // 能力查询走 level.getCapability（无 LazyOptional，直接返回可空实例）；
                // null 作为 context 表示"未知面"，与本模组机器 getItemHandler(null) 的语义一致
                ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
                if (handler == null) {
                    continue;
                }
                int count = handler.size();
                for (int i = 0; i < count; i++) {
                    slots.add(new ItemSlot(handler, i));
                }
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.buildItemSlots error", e);
            }
        }
        return slots;
    }

    /** 收集连通范围内液体机的液体槽 */
    private List<TankSlot> buildTankSlots(@Nonnull Level level) {
        List<TankSlot> tanks = new ArrayList<>();
        for (BlockPos pos : owner.getSources()) {
            try {
                BlockEntity be = level.getBlockEntity(pos);
                if (be == null || !isFluidSource(be)) {
                    continue;
                }
                // 同上：null 面查询，液体机对 null 面返回同一聚合实例
                ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
                if (handler == null) {
                    continue;
                }
                int count = handler.size();
                for (int i = 0; i < count; i++) {
                    tanks.add(new TankSlot(handler, i));
                }
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.buildTankSlots error", e);
            }
        }
        return tanks;
    }

    // ==================== 物品视图（只读，仅可抽取） ====================

    private final class ItemView implements ResourceHandler<ItemResource> {

        @Override
        public int size() {
            return itemSlots().size();
        }

        @Override
        @Nonnull
        public ItemResource getResource(int index) {
            List<ItemSlot> slots = itemSlots();
            if (index < 0 || index >= slots.size()) {
                return ItemResource.EMPTY;
            }
            try {
                ItemSlot entry = slots.get(index);
                return entry.handler.getResource(entry.slot);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.getResource error", e);
            }
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            List<ItemSlot> slots = itemSlots();
            if (index < 0 || index >= slots.size()) {
                return 0;
            }
            try {
                ItemSlot entry = slots.get(index);
                return entry.handler.getAmountAsLong(entry.slot);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.getAmountAsLong error", e);
            }
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, @Nonnull ItemResource resource) {
            List<ItemSlot> slots = itemSlots();
            if (index < 0 || index >= slots.size()) {
                return 0;
            }
            try {
                ItemSlot entry = slots.get(index);
                return entry.handler.getCapacityAsLong(entry.slot, resource);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.getCapacityAsLong error", e);
            }
            return 0;
        }

        @Override
        public boolean isValid(int index, @Nonnull ItemResource resource) {
            return false;
        }

        @Override
        public int insert(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
            // 只读聚合：不可插入
            return 0;
        }

        @Override
        public int extract(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
            List<ItemSlot> slots = itemSlots();
            if (index < 0 || index >= slots.size() || amount <= 0) {
                return 0;
            }
            try {
                // 事务原样下传：回滚由来源机器自己的 SnapshotJournal 负责
                ItemSlot entry = slots.get(index);
                return entry.handler.extract(entry.slot, resource, amount, transaction);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.extract error", e);
            }
            return 0;
        }
    }

    // ==================== 流体视图（只读，仅可抽取） ====================

    private final class FluidView implements ResourceHandler<FluidResource> {

        @Override
        public int size() {
            return tankSlots().size();
        }

        @Override
        @Nonnull
        public FluidResource getResource(int index) {
            List<TankSlot> tanks = tankSlots();
            if (index < 0 || index >= tanks.size()) {
                return FluidResource.EMPTY;
            }
            try {
                TankSlot entry = tanks.get(index);
                return entry.handler.getResource(entry.tank);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.getFluidResource error", e);
            }
            return FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            List<TankSlot> tanks = tankSlots();
            if (index < 0 || index >= tanks.size()) {
                return 0;
            }
            try {
                TankSlot entry = tanks.get(index);
                return entry.handler.getAmountAsLong(entry.tank);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.getFluidAmount error", e);
            }
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, @Nonnull FluidResource resource) {
            List<TankSlot> tanks = tankSlots();
            if (index < 0 || index >= tanks.size()) {
                return 0;
            }
            try {
                TankSlot entry = tanks.get(index);
                return entry.handler.getCapacityAsLong(entry.tank, resource);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.getTankCapacity error", e);
            }
            return 0;
        }

        @Override
        public boolean isValid(int index, @Nonnull FluidResource resource) {
            return false;
        }

        @Override
        public int insert(int index, @Nonnull FluidResource resource, int amount, @Nonnull TransactionContext transaction) {
            // 只读聚合：不可灌入
            return 0;
        }

        @Override
        public int extract(int index, @Nonnull FluidResource resource, int amount, @Nonnull TransactionContext transaction) {
            List<TankSlot> tanks = tankSlots();
            if (index < 0 || index >= tanks.size() || amount <= 0) {
                return 0;
            }
            try {
                // 事务原样下传，回滚同物品侧
                TankSlot entry = tanks.get(index);
                return entry.handler.extract(entry.tank, resource, amount, transaction);
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.extractFluid error", e);
            }
            return 0;
        }
    }
}
