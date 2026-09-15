package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 自动耕地容器（资源农场式）：27 行虚拟产物槽 + 玩家背包。
 * 数据槽同步等级/进度、27 行 itemId+存量(long)、六面状态与总开关。
 */
public class AutoFarmlandMenu extends AbstractContainerMenu {
    public static final int MAX_PRODUCTS = 27;
    public static final int BUTTON_DIR_BASE = 0;
    public static final int BUTTON_EXTRACT_ONE_BASE = 6;
    public static final int BUTTON_EXTRACT_STACK_BASE = 33;
    public static final int BUTTON_EXTRACT_ALL_BASE = 60;
    public static final int BUTTON_OUTPUT = 87;
    /** 88~93：右键反向循环切换六面输出状态 */
    public static final int BUTTON_DIR_REVERSE_BASE = 88;
    public static final int SLOT_PRODUCT_BASE = 0;
    public static final int SLOT_PLAYER_BASE = 27;

    public final AutoFarmlandEntity entity;
    private final int[] clientItemIds = new int[MAX_PRODUCTS];
    private final long[] clientStocks = new long[MAX_PRODUCTS];
    private long clientLevel;
    private int clientTickCount;
    private final int[] clientDirectionState = new int[6];
    private boolean clientOutputEnabled;

    public AutoFarmlandMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.AUTO_FARMLAND_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (AutoFarmlandEntity) blockEntity;

        for (int i = 0; i < MAX_PRODUCTS; i++) {
            addSlot(new ProductSlot(i, 8 + (i % 9) * 18, 44 + (i / 9) * 18));
        }
        addPlayerInventory(playerInventory);

        // 每个值都按 16 位一块拆开传：数据槽走 writeShort，>32767 的值会被客户端读成负数
        for (int k = 0; k < 4; k++) {
            final int part = k;
            addDataSlot(makeDataSlot(() -> longChunk(entity.level, part), v -> clientLevel = setChunk(clientLevel, part, v)));
        }
        addDataSlot(makeDataSlot(() -> intChunk(suitInt(entity.tickCount), 0), v -> clientTickCount = merge32(intChunk(clientTickCount, 1), v)));
        addDataSlot(makeDataSlot(() -> intChunk(suitInt(entity.tickCount), 1), v -> clientTickCount = merge32(v, intChunk(clientTickCount, 0))));
        for (int i = 0; i < MAX_PRODUCTS; i++) {
            final int idx = i;
            addDataSlot(makeDataSlot(() -> intChunk(entity.getProductItemId(idx), 0), v -> clientItemIds[idx] = merge32(intChunk(clientItemIds[idx], 1), v)));
            addDataSlot(makeDataSlot(() -> intChunk(entity.getProductItemId(idx), 1), v -> clientItemIds[idx] = merge32(v, intChunk(clientItemIds[idx], 0))));
            for (int k = 0; k < 4; k++) {
                final int part = k;
                addDataSlot(makeDataSlot(() -> longChunk(entity.getProductStock(idx), part), v -> clientStocks[idx] = setChunk(clientStocks[idx], part, v)));
            }
        }
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.getDirectionState(direction), v -> clientDirectionState[idx] = v));
        }
        addDataSlot(makeDataSlot(() -> entity.outputEnabled ? 1 : 0, v -> clientOutputEnabled = v != 0));
    }

    private boolean serverSide() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide();
    }

    public long getLevel() {
        return serverSide() ? entity.level : clientLevel;
    }

    public int getTickCount() {
        return serverSide() ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
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

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide()) {
            return false;
        }
        if (id >= BUTTON_DIR_BASE && id < BUTTON_DIR_BASE + 6) {
            entity.cycleDirectionState(Direction.values()[id - BUTTON_DIR_BASE]);
        } else if (id >= BUTTON_DIR_REVERSE_BASE && id < BUTTON_DIR_REVERSE_BASE + 6) {
            // 右键：反向循环
            entity.cycleDirectionState(Direction.values()[id - BUTTON_DIR_REVERSE_BASE], false);
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
        return ItemStack.EMPTY; // 产物虚拟槽不可放取；背包也无法放入
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 160 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 218));
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

    // ==================== 数据槽拆位工具 ====================
    // 数据槽在线路上走 ClientboundContainerSetDataPacket，值用 writeShort 写——**只有 16 位**。
    // 因此 32 位值要拆成 2 块、64 位值要拆成 4 块；每块按无符号 16 位传递
    // （0~65535 写出去会被读成负数，所以取块与并块都要 & 0xFFFF）。
    // 注意「拆成高低 32 位」是错的：那样每一半仍然会被截断到 16 位。

    /** 取 32 位值的第 {@code part} 个 16 位块（part 0 = 低 16 位） */
    private static int intChunk(int value, int part) {
        return (value >>> (part * 16)) & 0xFFFF;
    }

    /** 把两个 16 位块并回 32 位值 */
    private static int merge32(int high, int low) {
        return ((high & 0xFFFF) << 16) | (low & 0xFFFF);
    }

    /** 取 long 的第 {@code part} 个 16 位块（part 0 = 低 16 位，共 4 块） */
    private static int longChunk(long value, int part) {
        return (int) ((value >>> (part * 16)) & 0xFFFFL);
    }

    /** 把第 {@code part} 个 16 位块写回 long */
    private static long setChunk(long value, int part, int chunk) {
        int shift = part * 16;
        long mask = 0xFFFFL << shift;
        return (value & ~mask) | (((long) (chunk & 0xFFFF)) << shift);
    }

    /** 裁剪到 int 范围（负值取 0） */
    private static int suitInt(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }
}
