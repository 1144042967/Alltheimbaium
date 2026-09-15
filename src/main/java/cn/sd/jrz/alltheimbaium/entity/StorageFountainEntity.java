package cn.sd.jrz.alltheimbaium.entity;

import static cn.sd.jrz.alltheimbaium.setup.Registration.STORAGE_FOUNTAIN_ENTITY;
import static cn.sd.jrz.alltheimbaium.setup.Registration.STORAGE_FOUNTAIN_ITEM;
import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.connection.StorageFountainConnection;
import cn.sd.jrz.alltheimbaium.gui.StorageFountainMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

public class StorageFountainEntity extends BlockEntity implements MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(StorageFountainEntity.class);

    /**
     * 六面 + 无方向(任意) 的对外能力缓存：索引 0~5 与 Direction.values() 顺序一致，索引 6 表示不限定方向。
     * NeoForge 不再实现 ICapabilityProvider，由 {@code registerCapabilities} 在 RegisterCapabilitiesEvent 里拉取。
     */
    private final StorageFountainConnection[] itemHandlers = new StorageFountainConnection[7];

    /** 26.x：{@link Capabilities.Item#BLOCK} 要求的是 {@code ResourceHandler<ItemResource>}，实现类见 StorageFountainConnection */
    @Nullable
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        int idx = side == null ? 6 : side.ordinal();
        if (itemHandlers[idx] == null) {
            itemHandlers[idx] = new StorageFountainConnection(this, side);
        }
        return itemHandlers[idx];
    }
    public int findIndex = 0;

    /**
     * 为每个方向缓存一个能力实例（被动抽取会按该方向配置过滤物品），另缓存一个不限定方向的实例。
     */
    @SuppressWarnings("unchecked")

    // ==================== 方向输出状态 ====================
    /** 随机：输出任意有存量的物品 */
    public static final int STATE_RANDOM = 0;
    /** 禁用：该面不输出 */
    public static final int STATE_DISABLED = 1;
    /** 槽1~槽9 起始值：state = STATE_SLOT_BASE + 槽索引(0~8)，即 2~10 */
    public static final int STATE_SLOT_BASE = 2;
    /** 每个方向的输出状态总数（随机、禁用、槽1~槽9） */
    public static final int STATE_COUNT = 11;
    /** 六面输出状态，索引与 Direction.values() 顺序一致 */
    public final int[] directionState = new int[6];
    /** 主动输出总开关（GUI 输出按钮控制），关闭后不执行任何主动输出 */
    public boolean outputEnabled = true;

    // 从配置文件加载的本地缓存值，由 Config.onConfigLoad() 在配置加载后调用 loadConfig() 填入
    static long initialOutput;

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用
     */
    public static void loadConfig() {
        initialOutput = Config.STORAGE_FOUNTAIN_INITIAL_OUTPUT.get();
    }

    public long output;
    /** 已标记物品模板（每种 1 份），数量与 blockList 一一对应 */
    public List<ItemStack> itemList = new ArrayList<>();
    /** 每种已标记物品的存量（内部单位，CARRY = 1 个物品） */
    public List<Long> blockList = new ArrayList<>();
    public long tickCount = 0;

    public StorageFountainEntity(BlockPos pos, BlockState state) {
        super(STORAGE_FOUNTAIN_ENTITY.get(), pos, state);
        this.output = initialOutput;
    }

    // ==================== 标记槽 ====================

    /**
     * 标记槽：放入已标记物品 → 取消标记并清空数量；放入支持的未标记物品 → 添加到标记列表。
     * 一次最多放入 1 个物品（getStackLimit），处理完毕后立即清空。
     */
    public final ItemStackHandler markerSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            // 已标记 → 允许放入以取消标记；未标记 → 仅支持且列表未满
            return isMarked(stack) || (StorageFountainBlock.isAcceptedItem(stack) && itemList.size() < StorageFountainBlock.getMaxItemTypes());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            Level level = getLevel();
            if (level == null) {
                return;
            }
            if (level.isClientSide()) {
                // 客户端：立即清空标记槽，避免残留物品影响后续点击（标记逻辑由服务端处理）
                if (!markerSlot.getStackInSlot(0).isEmpty()) {
                    markerSlot.setStackInSlot(0, ItemStack.EMPTY);
                }
                return;
            }
            processMarkerSlot();
        }
    };

    /**
     * 判断物品是否已经标记（比较物品 + NBT，忽略数量）
     */
    public boolean isMarked(ItemStack stack) {
        return findMarkedIndex(stack) >= 0;
    }

    /**
     * 查找物品在已标记列表中的索引，未标记返回 -1
     */
    public int findMarkedIndex(ItemStack stack) {
        ItemStack single = stack.copy();
        single.setCount(1);
        for (int i = 0; i < itemList.size(); i++) {
            // 1.21：ItemStack.matches(other, true) 已移除，改用具名静态方法（物品 + 数据组件全等）
            if (ItemStack.isSameItemSameComponents(itemList.get(i), single)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 处理标记槽中的物品（服务端）：已标记 → 取消标记；未标记且支持 → 添加标记。处理完毕后清空标记槽。
     */
    private void processMarkerSlot() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        ItemStack stack = markerSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        try {
            ItemStack single = stack.copy();
            single.setCount(1);
            int idx = findMarkedIndex(single);
            boolean marked = idx >= 0;
            if (marked) {
                // 已标记 → 取消标记并清空数量
                itemList.remove(idx);
                blockList.remove(idx);
            } else if (StorageFountainBlock.isAcceptedItem(single) && itemList.size() < StorageFountainBlock.getMaxItemTypes()) {
                itemList.add(single);
                blockList.add(0L);
                Tool.sort(itemList, blockList);
            } else {
                // 不应发生（isItemValid 已拦截），保留物品
                return;
            }
            markerSlot.setStackInSlot(0, ItemStack.EMPTY);
            setChanged();
            sendUpdatePacket();
            // 26.x：Item#getName(ItemStack) 只读 ITEM_NAME 组件，取显示名一律用 getHoverName()
            String name = new ItemStack(single.getItem()).getHoverName().getString();
            sendMessageToNearbyPlayer(marked
                    ? "chat.alltheimbaium.storage_fountain.unmark"
                    : "chat.alltheimbaium.storage_fountain.mark", name);
        } catch (Throwable e) {
            log.error("StorageFountainEntity.processMarkerSlot error", e);
        }
    }

    /**
     * 给机器附近最近的玩家发送系统消息（标记/取消标记反馈）
     */
    private void sendMessageToNearbyPlayer(String key, Object... args) {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Player player = serverLevel.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 8.0, false);
        if (player != null) {
            player.sendSystemMessage(Component.translatable(key, args));
        }
    }

    /**
     * 从指定槽位提取最多 maxItems 个物品（扣减对应存量），返回实际提取数量
     */
    public long extractItems(int index, long maxItems) {
        if (index < 0 || index >= itemList.size()) {
            return 0;
        }
        long units = blockList.get(index);
        long available = units / StorageFountainBlock.getCarry();
        if (available <= 0) {
            return 0;
        }
        long toExtract = Math.min(available, maxItems);
        blockList.set(index, units - toExtract * StorageFountainBlock.getCarry());
        setChanged();
        return toExtract;
    }

    // ==================== 方向状态 ====================

    /**
     * 指定面的输出状态
     */
    public int getDirectionState(Direction direction) {
        return directionState[direction.ordinal()];
    }

    /**
     * 指定面的状态是否为槽 N（返回槽索引 0~8，否则 -1）
     */
    public int getDirectionSlot(Direction direction) {
        int state = getDirectionState(direction);
        return state >= STATE_SLOT_BASE ? state - STATE_SLOT_BASE : -1;
    }

    /**
     * 循环切换指定面的输出状态
     */
    public void cycleDirectionState(Direction direction) {
        cycleDirectionState(direction, true);
    }

    /**
     * 循环切换某面的输出状态
     *
     * @param forward true 正向（左键），false 反向（右键）
     */
    public void cycleDirectionState(Direction direction, boolean forward) {
        int idx = direction.ordinal();
        directionState[idx] = forward
                ? (directionState[idx] + 1) % STATE_COUNT
                : (directionState[idx] + STATE_COUNT - 1) % STATE_COUNT;
        setChanged();
    }

    // ==================== 已标记物品读取（菜单/数据同步用） ====================

    /**
     * 指定槽位的物品模板（数量 1），不存在返回空
     */
    @Nonnull
    public ItemStack getMarkedStack(int index) {
        if (index < 0 || index >= itemList.size()) {
            return ItemStack.EMPTY;
        }
        return itemList.get(index).copy();
    }

    /**
     * 指定槽位的存量（内部单位）
     */
    public long getMarkedCount(int index) {
        if (index < 0 || index >= blockList.size()) {
            return 0;
        }
        return blockList.get(index);
    }

    /**
     * 指定槽位物品的注册 id（用于客户端数据同步，空槽返回 0）
     */
    public int getMarkedItemId(int index) {
        if (index < 0 || index >= itemList.size()) {
            return 0;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.getId(itemList.get(index).getItem());
    }

    // ==================== 菜单提供 ====================

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.storage_fountain").withStyle(Tip.rarityColor(STORAGE_FOUNTAIN_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new StorageFountainMenu(id, inv, worldPosition);
    }


    private static final String KEY_OUTPUT = "output";
    private static final String KEY_STICK = "save_stick";
    private static final String KEY_DIR = "directionState";
    private static final String KEY_OUTPUT_ENABLED = "outputEnabled";
    private static final String KEY_MARKER_SLOT = "markerSlot";

    @Override
    protected void saveAdditional(@Nonnull ValueOutput output) {
        super.saveAdditional(output);
        try {
            output.putLong(KEY_OUTPUT, this.output);
            // 26.x：已标记物品列表改由 ValueOutput 列表托管（记录类型见 Tool.StockRow），不再手写物品 NBT
            Tool.writeRows(output, KEY_STICK, toStockRows(), Tool.StockRow.CODEC);
            output.putIntArray(KEY_DIR, directionState);
            output.putBoolean(KEY_OUTPUT_ENABLED, outputEnabled);
            output.putChild(KEY_MARKER_SLOT, markerSlot);
        } catch (Throwable e) {
            log.error("StorageFountainEntity.saveAdditional error", e);
        }
    }

    @Override
    protected void loadAdditional(@Nonnull ValueInput input) {
        super.loadAdditional(input);
        try {
            this.output = Tool.suit(input.getLongOr(KEY_OUTPUT, 0L));
            applyStockRows(Tool.readRows(input, KEY_STICK, Tool.StockRow.CODEC));
            int[] arr = input.getIntArray(KEY_DIR).orElse(null);
            if (arr != null) {
                for (int i = 0; i < Math.min(6, arr.length); i++) {
                    directionState[i] = Math.max(0, Math.min(STATE_COUNT - 1, arr[i]));
                }
            }
            outputEnabled = input.getBooleanOr(KEY_OUTPUT_ENABLED, outputEnabled);
            input.readChild(KEY_MARKER_SLOT, markerSlot);
        } catch (Throwable e) {
            log.error("StorageFountainEntity.loadAdditional error", e);
        }
    }

    /** 已标记物品列表 → 持久化记录（数量沿用内部单位，即 CARRY 个内部单位折合 1 件） */
    @Nonnull
    private List<Tool.StockRow> toStockRows() {
        List<Tool.StockRow> list = new ArrayList<>(itemList.size());
        for (int i = 0; i < itemList.size(); i++) {
            list.add(new Tool.StockRow(Tool.oneOf(itemList.get(i)), i < blockList.size() ? blockList.get(i) : 0L));
        }
        return list;
    }

    private void applyStockRows(@Nonnull List<Tool.StockRow> list) {
        List<ItemStack> newItems = new ArrayList<>(list.size());
        List<Long> newCounts = new ArrayList<>(list.size());
        for (Tool.StockRow record : list) {
            try {
                ItemStack stack = record.item();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                newItems.add(Tool.oneOf(stack));
                newCounts.add(Tool.suit(record.count()));
            } catch (Throwable e) {
                log.warn("StorageFountainEntity.applyStockRows entry error", e);
            }
        }
        this.itemList = newItems;
        this.blockList = newCounts;
        Tool.sort(itemList, blockList);
    }

    // ==================== 客户端同步（供 BER 渲染已标记物品贴图） ====================
    // 26.x：handleUpdateTag / onDataPacket 的覆写已删除，改由原版 loadWithComponents(ValueInput) 默认接管。

    /**
     * 区块加载/方块放置时同步全部数据（含已标记物品列表）到客户端
     */
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

    /**
     * 已标记物品列表变化时向附近客户端发送更新包（刷新方块表面的物品贴图渲染）
     */
    private void sendUpdatePacket() {
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
