package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.connection.AutoFarmlandConnection;
import cn.sd.jrz.alltheimbaium.gui.AutoFarmlandMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Registration;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动耕地方块实体（资源农场式）。
 * <p>
 * 无能量、无标记槽：每 tick 等级增长；上方若有成熟 {@link CropBlock}，则模拟“收获一次”，
 * 把该次掉落每种物品数量 × 当前效率(每级 +1%) 累加到对应产物行（大数 long 存量）；
 * 支持六面输出与总开关。作物保持成熟持续可收。
 */
public class AutoFarmlandEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(AutoFarmlandEntity.class);

    public static final int STATE_RANDOM = 0;
    public static final int STATE_DISABLED = 1;
    public static final int STATE_SLOT_BASE = 2;

    public final int[] directionState = new int[6];
    public boolean outputEnabled = true;
    public int findIndex = 0;

    public long level;
    public long tickCount = 0;

    /** 产物行 */
    public static final class Row {
        @Nonnull
        public final Item item;
        public long stock;
        public double frac;

        public Row(@Nonnull Item item) {
            this.item = item;
        }
    }

    public final List<Row> rows = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private final LazyOptional<AutoFarmlandConnection>[] fecOptionals = createDirectionalOptionals();

    private LazyOptional<AutoFarmlandConnection>[] createDirectionalOptionals() {
        LazyOptional<AutoFarmlandConnection>[] optionals = new LazyOptional[7];
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            final Direction direction = directions[i];
            optionals[i] = LazyOptional.of(() -> new AutoFarmlandConnection(this, direction));
        }
        optionals[6] = LazyOptional.of(() -> new AutoFarmlandConnection(this, null));
        return optionals;
    }

    public AutoFarmlandEntity(BlockPos pos, BlockState state) {
        super(Registration.AUTO_FARMLAND_ENTITY.get(), pos, state);
        this.level = Math.max(1, MobFarmBlock.getInitialLevel());
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
            log.error("AutoFarmlandEntity.extractItems error", e);
        }
        return 0;
    }

    /** 整件物品并入对应行（取物放不回时退回等） */
    public void addProduct(@Nonnull ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Row row = findRow(stack.getItem());
        if (row == null) {
            if (rows.size() >= MobFarmBlock.getMaxProducts()) {
                return;
            }
            row = new Row(stack.getItem());
            rows.add(row);
        }
        row.stock = Tool.suit(row.stock + stack.getCount());
        clampDirectionStates();
        setChanged();
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
        rows.removeIf(row -> row.stock <= 0 && row.frac <= 0);
    }

    private void clampDirectionStates() {
        for (int i = 0; i < directionState.length; i++) {
            if (directionState[i] >= STATE_SLOT_BASE + rows.size()) {
                directionState[i] = STATE_RANDOM;
            }
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
        cycleDirectionState(direction, true);
    }

    /**
     * 循环切换某面的输出状态
     *
     * @param forward true 正向（左键），false 反向（右键）
     */
    public void cycleDirectionState(Direction direction, boolean forward) {
        int idx = direction.ordinal();
        int count = getStateCount();
        directionState[idx] = forward
                ? (directionState[idx] + 1) % count
                : (directionState[idx] + count - 1) % count;
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
            // 收获上方成熟作物：真实掉落 × 当前效率
            BlockPos cropPos = worldPosition.above();
            BlockState cropState = world.getBlockState(cropPos);
            if (cropState.getBlock() instanceof CropBlock crop) {
                if (crop.getAge(cropState) < crop.getMaxAge()) {
                    cropState = cropState.setValue(CropBlock.AGE, crop.getMaxAge());
                    world.setBlock(cropPos, cropState, 2);
                }
                if (crop.getAge(cropState) >= crop.getMaxAge() && world instanceof ServerLevel serverLevel) {
                    double eff = efficiency();
                    for (ItemStack drop : crop.getDrops(cropState, serverLevel, cropPos, null)) {
                        if (!drop.isEmpty()) {
                            harvestAdd(drop.getItem(), drop.getCount() * eff);
                        }
                    }
                }
            }
            if (outputEnabled) {
                outputToNeighbors();
            }
            setChanged();
        } catch (Throwable e) {
            log.error("AutoFarmlandEntity.tickServer error", e);
        }
    }

    /** 每级 +1%：效率 = 1 + (等级-1)×0.01 */
    private double efficiency() {
        return 1.0 + (Math.max(1, level) - 1) * 0.01;
    }

    private void harvestAdd(Item item, double amount) {
        if (item == null || amount <= 0) {
            return;
        }
        Row row = findRow(item);
        if (row == null) {
            if (rows.size() >= MobFarmBlock.getMaxProducts()) {
                return;
            }
            row = new Row(item);
            rows.add(row);
        }
        row.frac += amount;
        long add = (long) row.frac;
        if (add > 0) {
            row.frac -= add;
            row.stock = Tool.suit(row.stock + add);
        }
        clampDirectionStates();
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
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(row.item, amount), false);
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
            log.error("AutoFarmlandEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.auto_farmland").withStyle(Tip.rarityColor(Registration.AUTO_FARMLAND_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new AutoFarmlandMenu(id, inv, worldPosition);
    }

    // ==================== NBT ====================

    private static final String KEY_ROWS = "rows";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_TICK = "tickCount";
    private static final String KEY_DIR = "directionState";
    private static final String KEY_OUTPUT = "outputEnabled";

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            nbt.putLong(KEY_LEVEL, level);
            nbt.putLong(KEY_TICK, tickCount);
            nbt.put(KEY_ROWS, saveRows());
            nbt.putIntArray(KEY_DIR, directionState);
            nbt.putBoolean(KEY_OUTPUT, outputEnabled);
        } catch (Throwable e) {
            log.error("AutoFarmlandEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
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
        } catch (Throwable e) {
            log.error("AutoFarmlandEntity.load error", e);
        }
    }

    private ListTag saveRows() {
        ListTag list = new ListTag();
        for (Row row : rows) {
            CompoundTag c = new CompoundTag();
            new ItemStack(row.item, 1).save(c);
            c.putLong("Stock", row.stock);
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
                Row row = new Row(stack.getItem());
                row.stock = c.contains("Stock", Tag.TAG_LONG) ? Tool.suit(c.getLong("Stock")) : 0;
                rows.add(row);
            } catch (Throwable e) {
                log.warn("AutoFarmlandEntity.loadRows entry error", e);
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
}
