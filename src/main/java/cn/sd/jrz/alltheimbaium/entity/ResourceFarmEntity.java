package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.connection.ResourceFarmConnection;
import cn.sd.jrz.alltheimbaium.gui.ResourceFarmMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.ResourceData;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用资源农场方块实体。
 * <p>
 * 仿 MobFarmEntity：标记槽放入一个"标记物"（圆石/任意树苗/竹子/甘蔗/冰/骨粉/…等），
 * 即永久确定该资源的白名单产物表；每 tick 按 权重×等级 平滑累计、按 CARRY 进位成整数物品；
 * 产物行长期保存、可六面输出、GUI 内取物。每台机器只能标记一次。
 */
public class ResourceFarmEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(ResourceFarmEntity.class);

    // ==================== 方向输出状态 ====================
    public static final int STATE_RANDOM = 0;
    public static final int STATE_DISABLED = 1;
    public static final int STATE_SLOT_BASE = 2;

    public final int[] directionState = new int[6];
    public boolean outputEnabled = true;
    public int findIndex = 0;

    // ==================== 等级 / 状态 ====================
    public long level;
    public long tickCount = 0;

    /** 产物行 */
    public static final class Row {
        @Nonnull
        public final Item item;
        public long stock;
        public long weight;
        public long acc;

        public Row(@Nonnull Item item, long weight) {
            this.item = item;
            this.weight = weight;
        }
    }

    /** 产物行（≤ maxProducts） */
    public final List<Row> rows = new ArrayList<>();

    /** 当前标记物（决定产物表），null = 未标记 */
    @Nullable
    private Item markerItem;
    /** 载入含标记但无产物行时，首个服务端 tick 补建 */
    private boolean needRebuild = false;

    @SuppressWarnings("unchecked")
    private final LazyOptional<ResourceFarmConnection>[] fecOptionals = createDirectionalOptionals();

    private LazyOptional<ResourceFarmConnection>[] createDirectionalOptionals() {
        LazyOptional<ResourceFarmConnection>[] optionals = new LazyOptional[7];
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            final Direction direction = directions[i];
            optionals[i] = LazyOptional.of(() -> new ResourceFarmConnection(this, direction));
        }
        optionals[6] = LazyOptional.of(() -> new ResourceFarmConnection(this, null));
        return optionals;
    }

    public ResourceFarmEntity(BlockPos pos, BlockState state) {
        super(Registration.RESOURCE_FARM_ENTITY.get(), pos, state);
        this.level = Math.max(1, MobFarmBlock.getInitialLevel());
    }

    // ==================== 标记查询 ====================

    public boolean hasMarker() {
        return markerItem != null;
    }

    @Nullable
    public Item getMarkerItem() {
        return markerItem;
    }

    /** 标记物物品注册 id（客户端同步用，未标记返回 0） */
    public int getMarkerItemId() {
        //noinspection deprecation
        return markerItem == null ? 0 : BuiltInRegistries.ITEM.getId(markerItem);
    }

    // ==================== 标记槽 ====================

    /**
     * 标记槽：放入有效标记物即永久确定该资源（只一次）；容量固定 1，标记后槽内始终保留该标记物用于展示且不可再改。
     */
    public final ItemStackHandler markerSlot = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (hasMarker()) {
                return false; // 已标记，锁定
            }
            Level lvl = getLevel();
            if (lvl != null && lvl.isClientSide) {
                return true; // 客户端放行，由服务端权威判定
            }
            return ResourceData.isMarker(stack.getItem());
        }

        @Override
        protected void onContentsChanged(int slot) {
            Level level = getLevel();
            if (level == null) {
                return;
            }
            if (level.isClientSide) {
                if (!hasMarker() && !markerSlot.getStackInSlot(0).isEmpty()) {
                    markerSlot.setStackInSlot(0, ItemStack.EMPTY);
                }
                return;
            }
            if (!hasMarker()) {
                processMarkerSlot();
            }
        }
    };

    private void processMarkerSlot() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        try {
            ItemStack stack = markerSlot.getStackInSlot(0);
            if (stack.isEmpty() || hasMarker()) {
                return;
            }
            Item item = stack.getItem();
            if (!ResourceData.isMarker(item)) {
                markerSlot.setStackInSlot(0, ItemStack.EMPTY);
                return;
            }
            setMarker(item);
        } catch (Throwable e) {
            log.error("ResourceFarmEntity.processMarkerSlot error", e);
        }
    }

    /** 服务端：标记资源并重建产物表；标记槽内保留 1 个标记物用于展示 */
    private void setMarker(@Nonnull Item item) {
        markerItem = item;
        if (markerSlot.getStackInSlot(0).isEmpty()) {
            markerSlot.setStackInSlot(0, new ItemStack(item, 1));
        }
        rebuildProducts();
        setChanged();
        sendUpdatePacket();
        String name = new ItemStack(item).getHoverName().getString();
        sendMessageToNearbyPlayer("chat.alltheimbaium.resource_farm.mark", name);
    }

    // ==================== 产物表 ====================

    /** 按当前标记重建产物行（首次标记或需要时调用），存量按物品保留 */
    public void rebuildProducts() {
        try {
            Level level = getLevel();
            if (level == null || level.isClientSide) {
                return;
            }
            if (markerItem == null) {
                return;
            }
            Map<Item, Row> oldByItem = new HashMap<>();
            for (Row row : rows) {
                oldByItem.put(row.item, row);
            }
            List<Row> newRows = new ArrayList<>();
            int max = MobFarmBlock.getMaxProducts();
            for (ResourceData.Product p : ResourceData.productsForMarker(markerItem)) {
                Row old = oldByItem.remove(p.item());
                if (old != null) {
                    old.weight = p.weight();
                    newRows.add(old);
                } else if (newRows.size() < max) {
                    newRows.add(new Row(p.item(), p.weight()));
                }
            }
            // 旧的存量行（不在新表内）保留（权重清 0，仍可取出）
            for (Row old : oldByItem.values()) {
                if (newRows.size() < max) {
                    old.weight = 0;
                    old.acc = 0;
                    newRows.add(old);
                }
            }
            while (newRows.size() > max) {
                newRows.sort(Comparator.comparingLong((Row r) -> r.weight).thenComparingLong(r -> r.stock));
                Row drop = newRows.get(0);
                if (drop.stock > 0) {
                    break;
                }
                newRows.remove(0);
            }
            rows.clear();
            rows.addAll(newRows);
            for (int i = 0; i < directionState.length; i++) {
                if (directionState[i] >= STATE_SLOT_BASE + rows.size()) {
                    directionState[i] = STATE_RANDOM;
                }
            }
            setChanged();
        } catch (Throwable e) {
            log.error("ResourceFarmEntity.rebuildProducts error", e);
        }
    }

    @Nullable
    private Row findRow(Item item) {
        for (Row row : rows) {
            if (row.item == item) {
                return row;
            }
        }
        return null;
    }

    private void cleanEmptyRows() {
        rows.removeIf(row -> row.stock <= 0 && row.weight <= 0);
    }

    // ==================== 产物行读取 ====================

    public int getProductCount() {
        return rows.size();
    }

    @Nonnull
    public ItemStack getProductStack(int index) {
        if (index < 0 || index >= rows.size()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(rows.get(index).item, 1);
    }

    public int getProductItemId(int index) {
        if (index < 0 || index >= rows.size()) {
            return 0;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.getId(rows.get(index).item);
    }

    public long getProductStock(int index) {
        if (index < 0 || index >= rows.size()) {
            return 0;
        }
        return rows.get(index).stock;
    }

    public long getProductWeight(int index) {
        if (index < 0 || index >= rows.size()) {
            return 0;
        }
        return rows.get(index).weight;
    }

    @Nullable
    public Item getProductItem(int index) {
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index).item;
    }

    public long extractItems(int index, long maxItems) {
        try {
            if (index < 0 || index >= rows.size() || maxItems <= 0) {
                return 0;
            }
            Row row = rows.get(index);
            long available = row.stock;
            if (available <= 0) {
                return 0;
            }
            long toExtract = Math.min(available, maxItems);
            row.stock = available - toExtract;
            cleanEmptyRows();
            setChanged();
            return toExtract;
        } catch (Throwable e) {
            log.error("ResourceFarmEntity.extractItems error", e);
        }
        return 0;
    }

    /** 加入一笔存量（取物放不回背包时退回等），并入对应行 */
    public void addProduct(@Nonnull ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            int max = MobFarmBlock.getMaxProducts();
            Row row = findRow(stack.getItem());
            if (row == null) {
                if (rows.size() < max) {
                    rows.add(new Row(stack.getItem(), 0));
                    row = findRow(stack.getItem());
                } else {
                    Row victim = null;
                    for (Row r : rows) {
                        if (r.stock == 0 && r.weight == 0) {
                            victim = r;
                            break;
                        }
                    }
                    if (victim != null) {
                        rows.remove(victim);
                        rows.add(new Row(stack.getItem(), 0));
                        row = findRow(stack.getItem());
                    } else {
                        log.warn("ResourceFarmEntity.addProduct: 输出槽已满，丢弃 {}", stack);
                        return;
                    }
                }
            }
            if (row != null) {
                row.stock = Tool.suit(row.stock + stack.getCount());
            }
            for (int i = 0; i < directionState.length; i++) {
                if (directionState[i] >= STATE_SLOT_BASE + rows.size()) {
                    directionState[i] = STATE_RANDOM;
                }
            }
            setChanged();
        } catch (Throwable e) {
            log.error("ResourceFarmEntity.addProduct error", e);
        }
    }

    // ==================== 方向状态 ====================

    public static int getStateCount() {
        return STATE_SLOT_BASE + MobFarmBlock.getMaxProducts();
    }

    public int getDirectionState(Direction direction) {
        return directionState[direction.ordinal()];
    }

    public void cycleDirectionState(Direction direction) {
        int idx = direction.ordinal();
        int count = getStateCount();
        directionState[idx] = (directionState[idx] + 1) % count;
        setChanged();
    }

    // ==================== 服务端 tick ====================

    public void tickServer() {
        Level world = getLevel();
        if (world == null || world.isClientSide) {
            return;
        }
        try {
            if (this.level < MobFarmBlock.getMaxLevel()) {
                tickCount++;
                if (tickCount >= 20L * MobFarmBlock.getLevelUpIntervalSeconds()) {
                    this.level++;
                    tickCount = 0;
                }
            }
            boolean marked = hasMarker();
            long carry = MobFarmBlock.getCarry();
            if (needRebuild) {
                needRebuild = false;
                if (marked) {
                    rebuildProducts();
                }
            }
            if (marked) {
                for (Row row : rows) {
                    if (row.weight <= 0) {
                        continue;
                    }
                    row.acc = Tool.suit(row.acc + row.weight * this.level);
                    if (row.acc >= carry) {
                        row.stock = Tool.suit(row.stock + row.acc / carry);
                        row.acc = row.acc % carry;
                    }
                }
            }
            if (outputEnabled) {
                outputToNeighbors();
            }
            setChanged();
        } catch (Throwable e) {
            log.error("ResourceFarmEntity.tickServer error", e);
        }
    }

    private void outputToNeighbors() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        Direction[] directions = Direction.values();
        findIndex = (findIndex + 1) % directions.length;
        Direction direction = directions[findIndex];
        int state = getDirectionState(direction);
        if (state == STATE_DISABLED) {
            return;
        }
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
        if (neighbor == null) {
            return;
        }
        var optional = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
        var handler = optional.resolve().orElse(null);
        if (handler == null) {
            return;
        }
        List<Integer> targets = new ArrayList<>();
        if (state == STATE_RANDOM) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).stock > 0) {
                    targets.add(i);
                }
            }
        } else {
            int idx = state - STATE_SLOT_BASE;
            if (idx < rows.size() && rows.get(idx).stock > 0) {
                targets.add(idx);
            }
        }
        for (int idx : targets) {
            pushRow(handler, idx);
        }
    }

    private void pushRow(net.minecraftforge.items.IItemHandler handler, int index) {
        Row row = rows.get(index);
        int maxStack = new ItemStack(row.item).getMaxStackSize();
        if (maxStack <= 0) {
            maxStack = 1;
        }
        long remaining = row.stock;
        while (remaining > 0) {
            int amount = (int) Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(row.item, amount);
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            int inserted = amount - leftover.getCount();
            if (inserted <= 0) {
                break;
            }
            row.stock -= inserted;
            remaining -= inserted;
        }
        cleanEmptyRows();
    }

    // ==================== capability / 菜单 ====================

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                int idx = direction == null ? 6 : direction.ordinal();
                if (idx >= 0 && idx < fecOptionals.length) {
                    return fecOptionals[idx].cast();
                }
            }
            return super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("ResourceFarmEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.resource_farm").withStyle(Tip.rarityColor(Registration.RESOURCE_FARM_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new ResourceFarmMenu(id, inv, worldPosition);
    }

    // ==================== NBT ====================

    private static final String KEY_MARKER = "marker";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_TICK = "tickCount";
    private static final String KEY_ROWS = "rows";
    private static final String KEY_DIR = "directionState";
    private static final String KEY_OUTPUT = "outputEnabled";

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            if (markerItem != null) {
                nbt.putString(KEY_MARKER, BuiltInRegistries.ITEM.getKey(markerItem).toString());
            }
            nbt.putLong(KEY_LEVEL, level);
            nbt.putLong(KEY_TICK, tickCount);
            nbt.put(KEY_ROWS, saveRows());
            nbt.putIntArray(KEY_DIR, directionState);
            nbt.putBoolean(KEY_OUTPUT, outputEnabled);
        } catch (Throwable e) {
            log.error("ResourceFarmEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
            if (nbt.contains(KEY_MARKER, Tag.TAG_STRING)) {
                markerItem = BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(nbt.getString(KEY_MARKER)));
                if (markerItem == net.minecraft.world.item.Items.AIR) {
                    markerItem = null;
                }
            } else {
                markerItem = null;
            }
            if (nbt.contains(KEY_LEVEL, Tag.TAG_LONG)) {
                level = Tool.suit(nbt.getLong(KEY_LEVEL));
            }
            if (nbt.contains(KEY_TICK, Tag.TAG_LONG)) {
                tickCount = Tool.suit(nbt.getLong(KEY_TICK));
            }
            if (nbt.contains(KEY_ROWS, Tag.TAG_LIST)) {
                loadRows((ListTag) nbt.get(KEY_ROWS));
            }
            if (nbt.contains(KEY_DIR)) {
                int[] arr = nbt.getIntArray(KEY_DIR);
                for (int i = 0; i < Math.min(6, arr.length); i++) {
                    directionState[i] = Math.max(0, Math.min(getStateCount() - 1, arr[i]));
                }
            }
            if (nbt.contains(KEY_OUTPUT, Tag.TAG_BYTE)) {
                outputEnabled = nbt.getBoolean(KEY_OUTPUT);
            }
            boolean hasWeight = false;
            for (Row row : rows) {
                if (row.weight > 0) {
                    hasWeight = true;
                    break;
                }
            }
            needRebuild = hasMarker() && !hasWeight;
            // 已标记的机器：标记槽常驻展示标记物
            if (hasMarker() && markerItem != null && markerSlot.getStackInSlot(0).isEmpty()) {
                markerSlot.setStackInSlot(0, new ItemStack(markerItem, 1));
            }
        } catch (Throwable e) {
            log.error("ResourceFarmEntity.load error", e);
        }
    }

    private ListTag saveRows() {
        ListTag list = new ListTag();
        for (Row row : rows) {
            CompoundTag c = new CompoundTag();
            new ItemStack(row.item, 1).save(c);
            c.putLong("Stock", row.stock);
            c.putLong("Weight", row.weight);
            list.add(c);
        }
        return list;
    }

    private void loadRows(ListTag list) {
        rows.clear();
        for (int i = 0; i < list.size(); i++) {
            try {
                CompoundTag c = list.getCompound(i);
                ItemStack stack = ItemStack.of(c);
                if (stack.isEmpty()) {
                    continue;
                }
                Row row = new Row(stack.getItem(), 0);
                row.stock = c.contains("Stock", Tag.TAG_LONG) ? Tool.suit(c.getLong("Stock")) : 0;
                row.weight = c.contains("Weight", Tag.TAG_LONG) ? Tool.suit(c.getLong("Weight")) : 0;
                rows.add(row);
            } catch (Throwable e) {
                log.warn("ResourceFarmEntity.loadRows entry error", e);
            }
        }
        cleanEmptyRows();
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
        this.load(pkt.getTag());
    }

    public void sendUpdatePacket() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

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
}
