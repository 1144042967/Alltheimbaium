package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import cn.sd.jrz.alltheimbaium.entity.ExtractionInterfaceEntity;
import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity;
import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 每次查询实时扫描六面相邻方块，把本 MOD 产物机器的"全量只读物品视图"
 * （以 {@code direction = null} 解析，StorageFountain/MobFarm/ResourceFarm/AutoFarmland 为随机全量、
 * InstantFurnace/InstantInscriber 输出区）与液体机（LiquidFountain）的液体聚合为本能力：
 * 物品可任意抽取、液体可 drain；物品不可插入、液体不可 fill，无其它功能。
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

    public ExtractionInterfaceConnection(ExtractionInterfaceEntity owner) {
        this.owner = owner;
    }

    /** 是否为本 MOD 的产物/流体机器（取出接口自身不计入，避免递归） */
    private static boolean isSupportedMachine(BlockEntity be) {
        return be instanceof StorageFountainEntity
                || be instanceof MobFarmEntity
                || be instanceof ResourceFarmEntity
                || be instanceof AutoFarmlandEntity
                || be instanceof InstantFurnaceEntity
                || be instanceof InstantInscriberEntity
                || be instanceof LiquidFountainEntity;
    }

    /** 相邻六面的本 MOD 产物机器（物品类） */
    private static boolean isItemMachine(BlockEntity be) {
        return be instanceof StorageFountainEntity
                || be instanceof MobFarmEntity
                || be instanceof ResourceFarmEntity
                || be instanceof AutoFarmlandEntity
                || be instanceof InstantFurnaceEntity
                || be instanceof InstantInscriberEntity;
    }

    private boolean usable() {
        Level level = owner.getLevel();
        return level != null && !level.isClientSide;
    }

    // ==================== 聚合槽位构建 ====================

    /** 收集相邻产物机器以 direction=null（全量只读）暴露的每个槽 */
    private List<ItemSlot> itemSlots() {
        List<ItemSlot> slots = new ArrayList<>();
        Level level = owner.getLevel();
        if (!usable()) {
            return slots;
        }
        BlockPos pos = owner.getBlockPos();
        for (Direction dir : Direction.values()) {
            try {
                BlockEntity be = level.getBlockEntity(pos.relative(dir));
                if (be == null || !isItemMachine(be)) {
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
                log.error("ExtractionInterfaceConnection.itemSlots error", e);
            }
        }
        return slots;
    }

    /** 收集相邻液体机的液体槽（每个 1 tank） */
    private List<TankSlot> tankSlots() {
        List<TankSlot> tanks = new ArrayList<>();
        Level level = owner.getLevel();
        if (!usable()) {
            return tanks;
        }
        BlockPos pos = owner.getBlockPos();
        for (Direction dir : Direction.values()) {
            try {
                BlockEntity be = level.getBlockEntity(pos.relative(dir));
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
                log.error("ExtractionInterfaceConnection.tankSlots error", e);
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
        List<TankSlot> tanks = tankSlots();
        for (TankSlot entry : tanks) {
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
        List<TankSlot> tanks = tankSlots();
        for (TankSlot entry : tanks) {
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
