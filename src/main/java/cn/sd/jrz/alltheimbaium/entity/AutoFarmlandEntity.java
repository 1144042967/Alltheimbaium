package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.gui.AutoFarmlandMenu;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ATI 自动耕地实体：27 槽收获存储容器。
 * <p>
 * 每 tick 由方块 ticker 触发收获逻辑；容器提供 IItemHandler capability 供管道抽取，
 * 并提供 MenuProvider 打开箱子界面 GUI。
 */
public class AutoFarmlandEntity extends BlockEntity implements MenuProvider, ICapabilityProvider {
    private static final Logger log = LoggerFactory.getLogger(AutoFarmlandEntity.class);
    /** 收获存储容器（27 槽，普通箱子大小） */
    public final ItemStackHandler storage = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemOptional = LazyOptional.of(() -> storage);
    /** 能量存储（上限 100 万 FE，供模拟收获消耗；可被能量管道充能） */
    public final EnergyStorage energy = new EnergyStorage(1_000_000) {
        // 1.20.1 EnergyStorage 无 onContentsChanged 钩子，能量实际变化时手动触发存档标记
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int ret = super.receiveEnergy(maxReceive, simulate);
            if (ret > 0 && !simulate) {
                setChanged();
            }
            return ret;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int ret = super.extractEnergy(maxExtract, simulate);
            if (ret > 0 && !simulate) {
                setChanged();
            }
            return ret;
        }
    };
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energy);
    /** 是否开启向下输出（默认开启；GUI 按钮切换，NBT 持久化），开启后每 tick 把容器物品自动推给下方方块 */
    public boolean outputToDown = true;

    public AutoFarmlandEntity(BlockPos pos, BlockState state) {
        super(Registration.AUTO_FARMLAND_ENTITY.get(), pos, state);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            if (capability == ForgeCapabilities.ENERGY) {
                return energyOptional.cast();
            }
            return capability == ForgeCapabilities.ITEM_HANDLER ? itemOptional.cast() : super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("AutoFarmlandEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.auto_farmland");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new AutoFarmlandMenu(id, inv, worldPosition);
    }

    /** 是否开启向下输出 */
    public boolean isOutputToDown() {
        return outputToDown;
    }

    /**
     * 向下输出：开启时把存储容器中的物品自动推入下方方块的物品容器。
     * <p>
     * 由方块 ticker 每 tick 调用；下方无容器或容器满时跳过，容器可插入的物品即时转移。
     */
    public void pushDownToContainer() {
        if (!outputToDown) {
            return;
        }
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        try {
            BlockEntity below = level.getBlockEntity(getBlockPos().below());
            if (below == null) {
                return;
            }
            IItemHandler handler = below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
            if (handler == null) {
                return;
            }
            for (int i = 0; i < storage.getSlots(); i++) {
                ItemStack stack = storage.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                // 尝试把整组插入下方容器，返回剩余；与插入前数量一致说明一个都没塞进去，跳过避免重复存档标记
                ItemStack remain = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                if (remain.getCount() != stack.getCount()) {
                    storage.setStackInSlot(i, remain);
                }
            }
        } catch (Throwable e) {
            log.error("AutoFarmlandEntity.pushDownToContainer error", e);
        }
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            nbt.put("storage", storage.serializeNBT());
            nbt.put("energy", energy.serializeNBT());
            nbt.putBoolean("outputToDown", outputToDown);
        } catch (Throwable e) {
            log.error("AutoFarmlandEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
            if (nbt.contains("storage")) {
                storage.deserializeNBT(nbt.getCompound("storage"));
            }
            if (nbt.contains("energy")) {
                energy.deserializeNBT(nbt.get("energy"));
            }
            if (nbt.contains("outputToDown")) {
                outputToDown = nbt.getBoolean("outputToDown");
            }
        } catch (Throwable e) {
            log.error("AutoFarmlandEntity.load error", e);
        }
    }

    /**
     * 客户端数据同步：区块加载/方块放置时把容器内容发给客户端，供 GUI 显示。
     */
    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundTag tag) {
        this.load(tag);
    }

    /**
     * 实时数据同步：容器内容变化时（onContentsChanged 触发 setChanged）向客户端发送更新包。
     */
    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }
}
