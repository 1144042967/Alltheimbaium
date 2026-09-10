package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import cn.sd.jrz.alltheimbaium.entity.ExtractionInterfaceEntity;
import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * ATI 取出接口对外能力（只读聚合，方向无关）。
 * <p>
 * 聚合范围由 {@link ExtractionInterfaceEntity#getSources()} 给出——那是沿本模组方块连通搜索
 * 得到的产出机器位置，因此隔着多台机器也能取到，不再限于相邻六面。
 * 物品以 {@code direction = null}（全量只读）解析；液体来自液体无限制造机。
 * <p>
 * 零刻熔炉与零刻压印器不是产出源（见 {@code ExtractionInterfaceEntity.isSource}），
 * 它们只让网络穿过，其中的物品不会被抽走。
 * <p>
 * 槽位列表按游戏刻缓存：管道一次取物会连续调用 {@code getSlots} / {@code getStackInSlot} /
 * {@code extractItem}，逐次重扫连通范围会带来数量级的多余开销。
 */
public class ExtractionInterfaceConnection implements IItemHandler, IFluidHandler {
    private static final Logger log = LoggerFactory.getLogger(ExtractionInterfaceConnection.class);
    private final ExtractionInterfaceEntity owner;

    /** 一个被聚合的物品槽：真实来源 handler + 其局部槽号 */
    private static final class ItemSlot {
        final IItemHandler handler;
        final int slot;

        ItemSlot(IItemHandler handler, int slot) {
            this.handler = handler;
            this.slot = slot;
        }
    }

    /** 一个被聚合的液体槽：真实来源 handler + 其局部 tank 号 */
    private static final class TankSlot {
        final IFluidHandler handler;
        final int tank;

        TankSlot(IFluidHandler handler, int tank) {
            this.handler = handler;
            this.tank = tank;
        }
    }

    private List<ItemSlot> cachedItemSlots;
    private long cachedItemSlotsTick = Long.MIN_VALUE;
    private List<TankSlot> cachedTankSlots;
    private long cachedTankSlotsTick = Long.MIN_VALUE;

    public ExtractionInterfaceConnection(ExtractionInterfaceEntity owner) {
        this.owner = owner;
    }

    private boolean usable() {
        Level level = owner.getLevel();
        return level != null && !level.isClientSide;
    }

    /** 产出物品的机器 */
    private static boolean isItemSource(BlockEntity be) {
        return be instanceof StorageFountainEntity
                || be instanceof MobFarmEntity
                || be instanceof ResourceFarmEntity
                || be instanceof AutoFarmlandEntity;
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
                LazyOptional<IItemHandler> opt = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                IItemHandler handler = opt.resolve().orElse(null);
                if (handler == null) {
                    continue;
                }
                int count = handler.getSlots();
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
                if (!(be instanceof LiquidFountainEntity)) {
                    continue;
                }
                LazyOptional<IFluidHandler> opt = be.getCapability(ForgeCapabilities.FLUID_HANDLER, null);
                IFluidHandler handler = opt.resolve().orElse(null);
                if (handler == null) {
                    continue;
                }
                for (int i = 0; i < handler.getTanks(); i++) {
                    tanks.add(new TankSlot(handler, i));
                }
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.buildTankSlots error", e);
            }
        }
        return tanks;
    }

    // ==================== IItemHandler（只读，仅可抽取） ====================

    @Override
    public int getSlots() {
        return itemSlots().size();
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        List<ItemSlot> slots = itemSlots();
        if (slot < 0 || slot >= slots.size()) {
            return ItemStack.EMPTY;
        }
        try {
            ItemSlot entry = slots.get(slot);
            return entry.handler.getStackInSlot(entry.slot);
        } catch (Throwable e) {
            log.error("ExtractionInterfaceConnection.getStackInSlot error", e);
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack; // 不可插入
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        List<ItemSlot> slots = itemSlots();
        if (slot < 0 || slot >= slots.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        try {
            ItemSlot entry = slots.get(slot);
            return entry.handler.extractItem(entry.slot, amount, simulate);
        } catch (Throwable e) {
            log.error("ExtractionInterfaceConnection.extractItem error", e);
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

    // ==================== IFluidHandler（只读，仅可 drain） ====================

    @Override
    public int getTanks() {
        return tankSlots().size();
    }

    @Override
    @Nonnull
    public FluidStack getFluidInTank(int tank) {
        List<TankSlot> tanks = tankSlots();
        if (tank < 0 || tank >= tanks.size()) {
            return FluidStack.EMPTY;
        }
        try {
            TankSlot entry = tanks.get(tank);
            return entry.handler.getFluidInTank(entry.tank);
        } catch (Throwable e) {
            log.error("ExtractionInterfaceConnection.getFluidInTank error", e);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        List<TankSlot> tanks = tankSlots();
        if (tank < 0 || tank >= tanks.size()) {
            return 0;
        }
        try {
            TankSlot entry = tanks.get(tank);
            return entry.handler.getTankCapacity(entry.tank);
        } catch (Throwable e) {
            log.error("ExtractionInterfaceConnection.getTankCapacity error", e);
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0; // 不可灌入
    }

    @Override
    @Nonnull
    public FluidStack drain(FluidStack resource, FluidAction action) {
        for (TankSlot entry : tankSlots()) {
            try {
                FluidStack drained = entry.handler.drain(resource, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.drain(FluidStack) error", e);
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    @Nonnull
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (TankSlot entry : tankSlots()) {
            try {
                FluidStack drained = entry.handler.drain(maxDrain, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            } catch (Throwable e) {
                log.error("ExtractionInterfaceConnection.drain(int) error", e);
            }
        }
        return FluidStack.EMPTY;
    }
}
