package cn.sd.jrz.alltheimbaium.entity;

import static cn.sd.jrz.alltheimbaium.setup.Registration.CREATIVE_TRANSMUTER_ENTITY;
import static cn.sd.jrz.alltheimbaium.setup.Registration.CREATIVE_TRANSMUTER_ITEM;
import cn.sd.jrz.alltheimbaium.connection.CreativeTransmuterConnection;
import cn.sd.jrz.alltheimbaium.gui.CreativeTransmuterMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.TransmuteCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
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
public class CreativeTransmuterEntity extends BlockEntity implements MenuProvider {
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

    /**
     * 对外物品能力（方向无关）。NeoForge 不再实现 ICapabilityProvider，
     * 由 {@code registerCapabilities} 在 RegisterCapabilitiesEvent 里拉取。
     */
    private final CreativeTransmuterConnection itemHandler = new CreativeTransmuterConnection(this);

    /** 26.x：{@link Capabilities.Item#BLOCK} 要求的是 {@code ResourceHandler<ItemResource>} */
    @Nonnull
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    public CreativeTransmuterEntity(BlockPos pos, BlockState state) {
        super(CREATIVE_TRANSMUTER_ENTITY.get(), pos, state);
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
    // 1.21：方块实体不再实现 ICapabilityProvider、也没有 LazyOptional，
    // 因此旧版 invalidateCaps() 里"让能力缓存失效"这件事整个消失——
    // 能力由 registerCapabilities 按方块实体类型静态注册，无需逐实例失效。

    // ==================== 菜单提供 ====================

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.creative_transmuter")
                .withStyle(Tip.rarityColor(CREATIVE_TRANSMUTER_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new CreativeTransmuterMenu(id, inv, worldPosition);
    }

    // ==================== NBT 持久化 ====================

    @Override
    protected void saveAdditional(@Nonnull ValueOutput output) {
        super.saveAdditional(output);
        try {
            // 26.x：ItemStackHandler 改实现 ValueIOSerializable，用 putChild/readChild 存取（serializeNBT 已删除）
            output.putChild(TAG_INVENTORY, inventory);
        } catch (Throwable e) {
            log.error("CreativeTransmuterEntity.saveAdditional error", e);
        }
    }

    @Override
    protected void loadAdditional(@Nonnull ValueInput input) {
        super.loadAdditional(input);
        try {
            input.readChild(TAG_INVENTORY, inventory);
        } catch (Throwable e) {
            log.error("CreativeTransmuterEntity.loadAdditional error", e);
        }
    }

    // ==================== 客户端同步 ====================
    // 26.x：handleUpdateTag / onDataPacket 的覆写已删除，改由原版 loadWithComponents(ValueInput) 默认接管。

    @Override
    @Nonnull
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
