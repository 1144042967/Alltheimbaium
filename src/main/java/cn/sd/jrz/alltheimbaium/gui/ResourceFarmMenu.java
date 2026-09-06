package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 通用资源农场容器。
 * <p>
 * 槽位：0=标记槽；1~27=产物行虚拟槽；28~63=玩家背包。
 * 数据槽同步 等级/进度、标记物、27 行 物品id+存量+权重、六面状态与总开关。
 * 开屏 extraData 另带"标记→产物"帮助行（供 GUI "?" 使用）。
 */
public class ResourceFarmMenu extends AbstractContainerMenu {
    public static final int MAX_PRODUCTS = 27;

    public static final int BUTTON_DIR_BASE = 0;
    public static final int BUTTON_EXTRACT_ONE_BASE = 6;
    public static final int BUTTON_EXTRACT_STACK_BASE = 33;
    public static final int BUTTON_EXTRACT_ALL_BASE = 60;
    public static final int BUTTON_OUTPUT = 87;

    public static final int SLOT_MARKER = 0;
    public static final int SLOT_PRODUCT_BASE = 1;
    public static final int SLOT_PLAYER_BASE = 28;

    public final ResourceFarmEntity entity;

    /** "标记→产物" 帮助行：每行 {markerId, itemId…} */
    private final int[][] helpRows;

    // 客户端镜像（由数据槽同步）
    private final int[] clientItemIds = new int[MAX_PRODUCTS];
    private final long[] clientStocks = new long[MAX_PRODUCTS];
    private final long[] clientWeights = new long[MAX_PRODUCTS];
    private long clientLevel;
    private int clientTickCount;
    private int clientMarkerId;
    private final int[] clientDirectionState = new int[6];
    private boolean clientOutputEnabled;

    public ResourceFarmMenu(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, pos, new int[0][]);
    }

    public ResourceFarmMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, data.readBlockPos(), readHelpRows(data));
    }

    private ResourceFarmMenu(int id, Inventory playerInventory, BlockPos pos, int[][] helpRows) {
        super(Registration.RESOURCE_FARM_MENU.get(), id);
        this.helpRows = helpRows;
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (ResourceFarmEntity) blockEntity;

        addSlot(new SlotItemHandler(entity.markerSlot, 0, 152, 23) {
            // 标记物不可取出（标记后常驻展示；取出由服务端权威）
            @Override
            public boolean mayPickup(@Nonnull Player player) {
                return false;
            }
        });
        for (int i = 0; i < MAX_PRODUCTS; i++) {
            addSlot(new ProductSlot(i, 8 + (i % 9) * 18, 44 + (i / 9) * 18));
        }
        addPlayerInventory(playerInventory);

        addDataSlot(makeDataSlot(() -> hiWord(entity.level), v -> clientLevel = mergeLong(v, loWord(clientLevel))));
        addDataSlot(makeDataSlot(() -> loWord(entity.level), v -> clientLevel = mergeLong(hiWord(clientLevel), v)));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.tickCount), v -> clientTickCount = v));
        addDataSlot(makeDataSlot(entity::getMarkerItemId, v -> clientMarkerId = v));
        for (int i = 0; i < MAX_PRODUCTS; i++) {
            final int idx = i;
            addDataSlot(makeDataSlot(() -> entity.getProductItemId(idx), v -> clientItemIds[idx] = v));
            addDataSlot(makeDataSlot(() -> hiWord(entity.getProductStock(idx)), v -> clientStocks[idx] = mergeLong(v, loWord(clientStocks[idx]))));
            addDataSlot(makeDataSlot(() -> loWord(entity.getProductStock(idx)), v -> clientStocks[idx] = mergeLong(hiWord(clientStocks[idx]), v)));
            addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.getProductWeight(idx)), v -> clientWeights[idx] = v));
        }
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.getDirectionState(direction), v -> clientDirectionState[idx] = v));
        }
        addDataSlot(makeDataSlot(() -> entity.outputEnabled ? 1 : 0, v -> clientOutputEnabled = v != 0));
    }

    // ==================== 展示 getter ====================

    private boolean serverSide() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide;
    }

    public long getLevel() {
        return serverSide() ? entity.level : clientLevel;
    }

    public int getTickCount() {
        return serverSide() ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
    }

    public int getMarkerItemId() {
        return serverSide() ? entity.getMarkerItemId() : clientMarkerId;
    }

    public int getDirectionState(Direction direction) {
        return serverSide() ? entity.getDirectionState(direction) : clientDirectionState[direction.ordinal()];
    }

    public boolean isOutputEnabled() {
        return serverSide() ? entity.outputEnabled : clientOutputEnabled;
    }

    @Nonnull
    public ItemStack getProductStack(int index) {
        if (serverSide()) {
            return entity.getProductStack(index);
        }
        if (index < 0 || index >= MAX_PRODUCTS || clientItemIds[index] <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        return new ItemStack(BuiltInRegistries.ITEM.byId(clientItemIds[index]), 1);
    }

    public long getProductStock(int index) {
        if (serverSide()) {
            return entity.getProductStock(index);
        }
        return index >= 0 && index < MAX_PRODUCTS ? clientStocks[index] : 0;
    }

    public long getProductWeight(int index) {
        if (serverSide()) {
            return entity.getProductWeight(index);
        }
        return index >= 0 && index < MAX_PRODUCTS ? clientWeights[index] : 0;
    }

    // ==================== 帮助行（GUI "?"） ====================

    public int helpRowCount() {
        return helpRows.length;
    }

    @Nullable
    public Item helpRowMarker(int row) {
        if (row < 0 || row >= helpRows.length) {
            return null;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.byId(helpRows[row][0]);
    }

    public int helpRowItemCount(int row) {
        if (row < 0 || row >= helpRows.length) {
            return 0;
        }
        return helpRows[row].length - 1;
    }

    @Nullable
    public Item helpRowItem(int row, int k) {
        if (row < 0 || row >= helpRows.length || k < 0 || k + 1 >= helpRows[row].length) {
            return null;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.byId(helpRows[row][k + 1]);
    }

    private static int[][] readHelpRows(FriendlyByteBuf data) {
        try {
            int rows = Math.max(0, Math.min(data.readVarInt(), 512));
            int[][] out = new int[rows][];
            for (int r = 0; r < rows; r++) {
                int markerId = data.readVarInt();
                int count = Math.max(0, Math.min(data.readVarInt(), 256));
                int[] row = new int[count + 1];
                row[0] = markerId;
                for (int i = 0; i < count; i++) {
                    row[i + 1] = data.readVarInt();
                }
                out[r] = row;
            }
            return out;
        } catch (Throwable e) {
            return new int[0][];
        }
    }

    // ==================== 按钮 / 快速转移 ====================

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        if (id >= BUTTON_DIR_BASE && id < BUTTON_DIR_BASE + 6) {
            entity.cycleDirectionState(Direction.values()[id - BUTTON_DIR_BASE]);
        } else if (id >= BUTTON_EXTRACT_ONE_BASE && id < BUTTON_EXTRACT_ONE_BASE + MAX_PRODUCTS) {
            extract(player, id - BUTTON_EXTRACT_ONE_BASE, 1);
        } else if (id >= BUTTON_EXTRACT_STACK_BASE && id < BUTTON_EXTRACT_STACK_BASE + MAX_PRODUCTS) {
            int slot = id - BUTTON_EXTRACT_STACK_BASE;
            ItemStack template = entity.getProductStack(slot);
            long maxStack = template.isEmpty() ? 1 : template.getMaxStackSize();
            extract(player, slot, maxStack);
        } else if (id >= BUTTON_EXTRACT_ALL_BASE && id < BUTTON_EXTRACT_ALL_BASE + MAX_PRODUCTS) {
            extract(player, id - BUTTON_EXTRACT_ALL_BASE, Long.MAX_VALUE);
        } else if (id == BUTTON_OUTPUT) {
            entity.outputEnabled = !entity.outputEnabled;
        } else {
            return false;
        }
        entity.setChanged();
        return true;
    }

    private void extract(Player player, int slot, long maxCount) {
        if (maxCount <= 0) {
            return;
        }
        long remaining = maxCount;
        while (remaining > 0) {
            long available = entity.getProductStock(slot);
            if (available <= 0) {
                break;
            }
            int amount = (int) Math.min(available, Math.min(remaining, 64));
            long got = entity.extractItems(slot, amount);
            if (got <= 0) {
                break;
            }
            ItemStack stack = entity.getProductStack(slot).copy();
            stack.setCount((int) got);
            player.addItem(stack);
            int placed = (int) got - stack.getCount();
            if (placed < got) {
                // 放不回背包的部分退回产物行
                entity.addProduct(stack.copy());
            }
            remaining -= placed;
            if (placed <= 0) {
                break;
            }
        }
        entity.setChanged();
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        try {
            if (index < SLOT_MARKER || index > SLOT_PLAYER_BASE + 35) {
                return ItemStack.EMPTY;
            }
            Slot slot = this.slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (index == SLOT_MARKER) {
                if (!this.moveItemStackTo(stack, SLOT_PLAYER_BASE, SLOT_PLAYER_BASE + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= SLOT_PLAYER_BASE) {
                if (!this.moveItemStackTo(stack, SLOT_MARKER, SLOT_MARKER + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } catch (Throwable e) {
            // 忽略快速转移异常
        }
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int mainY = 160;
        int hotbarY = 218;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, mainY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, hotbarY));
        }
    }

    private class ProductSlot extends Slot {
        private final int index;

        ProductSlot(int index, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.index = index;
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        @Nonnull
        public ItemStack getItem() {
            return getProductStack(index);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@Nonnull Player player) {
            return false;
        }

        @Override
        public void set(@Nonnull ItemStack stack) {
        }

        @Override
        @Nonnull
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static DataSlot makeDataSlot(IntSupplier getter, IntConsumer setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt();
            }

            @Override
            public void set(int value) {
                setter.accept(value);
            }
        };
    }

    private static int hiWord(long value) {
        return (int) (value >> 32);
    }

    private static int loWord(long value) {
        return (int) (value & 0xFFFFFFFFL);
    }

    private static long mergeLong(int hi, int lo) {
        return ((long) hi << 32) | (lo & 0xFFFFFFFFL);
    }
}
