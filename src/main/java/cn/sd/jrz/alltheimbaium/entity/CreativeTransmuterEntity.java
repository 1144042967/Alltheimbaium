package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.CreativeTransmuterConnection;
import cn.sd.jrz.alltheimbaium.gui.CreativeTransmuterMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.TransmuteCatalog;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 创造物品质变器实体。
 * <p>
 * 内部就是一个 10 格 {@link ItemStackHandler}：槽 0~8 是输入栏（3×3，每格 1 个），槽 9 是输出栏。
 * 每 tick 检查九格输入——填满且为同一种配方材料、输出栏又空着时，消耗九格并把 1 个产物写进输出栏。
 * 输入栏与输出栏都存在机器里，因此关掉 GUI、拆下重放都不会丢东西。
 * <p>
 * 对外能力见 {@link CreativeTransmuterConnection}：方向无关，输入栏可插不可抽、输出栏可抽不可插。
 */
public class CreativeTransmuterEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(CreativeTransmuterEntity.class);

    /** 输入栏格数（槽 0~8） */
    public static final int INPUT_SLOTS = TransmuteCatalog.INPUT_SLOTS;
    /** 输出栏槽号 */
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    /** 槽位总数 */
    public static final int SLOT_COUNT = INPUT_SLOTS + TransmuteCatalog.OUTPUT_SLOTS;
    /** NBT 键：物品栏 */
    public static final String TAG_INVENTORY = "inventory";

    /**
     * 机器物品栏。每格只存 1 个；输入栏只收配方材料，输出栏不接受任何放入（只能由机器写入）。
     */
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot < INPUT_SLOTS && TransmuteCatalog.isValidInput(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** 对外物品能力（方向无关） */
    private final LazyOptional<IItemHandler> itemCapability =
            LazyOptional.of(() -> new CreativeTransmuterConnection(this));

    public CreativeTransmuterEntity(BlockPos pos, BlockState state) {
        super(Registration.CREATIVE_TRANSMUTER_ENTITY.get(), pos, state);
    }

    @Nonnull
    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ==================== 转化逻辑 ====================

    /**
     * 服务端每 tick 调用：九格输入凑齐一份配方就立即转化，输出栏被占用时停产
     */
    public void serverTick() {
        try {
            if (inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
                ItemStack result = TransmuteCatalog.match(inputs());
                if (!result.isEmpty()) {
                    craft(result);
                }
            }
        } catch (Throwable e) {
            log.error("CreativeTransmuterEntity.serverTick error", e);
        }
    }

    /** 输入栏九格的只读快照 */
    @Nonnull
    private List<ItemStack> inputs() {
        List<ItemStack> list = new ArrayList<>(INPUT_SLOTS);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            list.add(inventory.getStackInSlot(i));
        }
        return list;
    }

    /**
     * 消耗九格输入并写入产物。九格必定同时清空，不会出现只扣一半的中间态。
     */
    private void craft(@Nonnull ItemStack result) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        inventory.setStackInSlot(OUTPUT_SLOT, result);
        setChanged();
    }

    // ==================== 能力 ====================

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                return this.itemCapability.cast();
            }
            return super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("CreativeTransmuterEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.itemCapability.invalidate();
    }

    // ==================== 菜单提供 ====================

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.creative_transmuter")
                .withStyle(Tip.rarityColor(Registration.CREATIVE_TRANSMUTER_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new CreativeTransmuterMenu(id, inv, worldPosition);
    }

    // ==================== NBT 持久化 ====================

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            nbt.put(TAG_INVENTORY, inventory.serializeNBT());
        } catch (Throwable e) {
            log.error("CreativeTransmuterEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
            if (nbt.contains(TAG_INVENTORY)) {
                inventory.deserializeNBT(nbt.getCompound(TAG_INVENTORY));
            }
        } catch (Throwable e) {
            log.error("CreativeTransmuterEntity.load error", e);
        }
    }

    // ==================== 客户端同步 ====================

    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundTag tag) {
        this.load(tag);
    }

    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.load(tag);
        }
    }
}
