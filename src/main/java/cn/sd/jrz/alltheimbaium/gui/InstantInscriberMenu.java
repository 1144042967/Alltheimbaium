package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 零刻压印器容器。
 * <p>
 * 槽位：0~17 输入行（两行）、18~26 输出行（一行 9）、27~62 玩家背包。
 * 数据槽同步 能量/六面状态/模式/18 输入 + 9 输出的 物品id+存量(long)。
 * 交互：点击虚拟格取物、点输入格投料、背包 Shift 投料、模式切换、六面推送开关。
 */
public class InstantInscriberMenu extends AbstractContainerMenu {
    // 按钮 ID
    public static final int BUTTON_FACE_BASE = 0;       // 0~5
    public static final int BUTTON_MODE = 6;            // 切换 压板/组装
    public static final int BUTTON_DEPOSIT_INPUT = 7;   // 手中物品并入输入行
    public static final int BUTTON_INPUT_ONE_BASE = 10;
    public static final int BUTTON_INPUT_STACK_BASE = 40;
    public static final int BUTTON_INPUT_ALL_BASE = 70;
    public static final int BUTTON_OUTPUT_ONE_BASE = 100;
    public static final int BUTTON_OUTPUT_STACK_BASE = 130;
    public static final int BUTTON_OUTPUT_ALL_BASE = 160;

    public static final int SLOT_OUTPUT_BASE = InstantInscriberEntity.INPUT_MAX_TYPES;
    public static final int SLOT_PLAYER_BASE = InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;

    public final InstantInscriberEntity entity;

    // 客户端展示数据
    private int clientEnergy;
    private int clientMode;
    private final int[] clientDirectionState = new int[6];
    private final int[] clientInputIds = new int[InstantInscriberEntity.INPUT_MAX_TYPES];
    private final long[] clientInputStocks = new long[InstantInscriberEntity.INPUT_MAX_TYPES];
    private final int[] clientOutputIds = new int[InstantInscriberEntity.OUTPUT_MAX_TYPES];
    private final long[] clientOutputStocks = new long[InstantInscriberEntity.OUTPUT_MAX_TYPES];

    public InstantInscriberMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.INSTANT_INSCRIBER_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = blockEntity instanceof InstantInscriberEntity inscriber ? inscriber : null;

        // 输入行 0~17：两行（y 26 / 44）
        for (int i = 0; i < InstantInscriberEntity.INPUT_MAX_TYPES; i++) {
            addSlot(new RowSlot(i, true, 8 + (i % 9) * 18, 26 + (i / 9) * 18));
        }
        // 输出行 18~26：一行 9（y 100）
        for (int i = 0; i < InstantInscriberEntity.OUTPUT_MAX_TYPES; i++) {
            addSlot(new RowSlot(i, false, 8 + i * 18, 100));
        }
        // 玩家背包 27~62
        addPlayerInventory(playerInventory);

        // 数据同步
        addDataSlot(makeDataSlot(() -> entity == null ? 0 : entity.getEnergyStored(), v -> clientEnergy = v));
        addDataSlot(makeDataSlot(() -> entity == null ? 0 : entity.getMode(), v -> clientMode = v));
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : entity.getDirectionState(direction), v -> clientDirectionState[idx] = v));
        }
        for (int i = 0; i < InstantInscriberEntity.INPUT_MAX_TYPES; i++) {
            final int idx = i;
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : entity.getInputItemId(idx), v -> clientInputIds[idx] = v));
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : hiWord(entity.getInputStock(idx)), v -> clientInputStocks[idx] = mergeLong(v, loWord(clientInputStocks[idx]))));
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : loWord(entity.getInputStock(idx)), v -> clientInputStocks[idx] = mergeLong(hiWord(clientInputStocks[idx]), v)));
        }
        for (int i = 0; i < InstantInscriberEntity.OUTPUT_MAX_TYPES; i++) {
            final int idx = i;
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : entity.getOutputItemId(idx), v -> clientOutputIds[idx] = v));
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : hiWord(entity.getOutputStock(idx)), v -> clientOutputStocks[idx] = mergeLong(v, loWord(clientOutputStocks[idx]))));
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : loWord(entity.getOutputStock(idx)), v -> clientOutputStocks[idx] = mergeLong(hiWord(clientOutputStocks[idx]), v)));
        }
    }

    private boolean serverSide() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide;
    }

    public int getEnergy() {
        return serverSide() ? entity.getEnergyStored() : clientEnergy;
    }

    public int getMaxEnergy() {
        return InstantInscriberEntity.MAX_ENERGY;
    }

    public int getEnergyPerOp() {
        return InstantInscriberEntity.ENERGY_PER_OP;
    }

    public int getMode() {
        return serverSide() ? entity.getMode() : clientMode;
    }

    public int getDirectionState(Direction direction) {
        return serverSide() ? entity.getDirectionState(direction) : clientDirectionState[direction.ordinal()];
    }

    @Nonnull
    public ItemStack getInputStack(int index) {
        if (serverSide()) {
            return entity.getInputStack(index);
        }
        if (index < 0 || index >= InstantInscriberEntity.INPUT_MAX_TYPES || clientInputIds[index] <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        return new ItemStack(BuiltInRegistries.ITEM.byId(clientInputIds[index]), 1);
    }

    public long getInputStock(int index) {
        if (serverSide()) {
            return entity.getInputStock(index);
        }
        return index >= 0 && index < InstantInscriberEntity.INPUT_MAX_TYPES ? clientInputStocks[index] : 0;
    }

    @Nonnull
    public ItemStack getOutputStack(int index) {
        if (serverSide()) {
            return entity.getOutputStack(index);
        }
        if (index < 0 || index >= InstantInscriberEntity.OUTPUT_MAX_TYPES || clientOutputIds[index] <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        return new ItemStack(BuiltInRegistries.ITEM.byId(clientOutputIds[index]), 1);
    }

    public long getOutputStock(int index) {
        if (serverSide()) {
            return entity.getOutputStock(index);
        }
        return index >= 0 && index < InstantInscriberEntity.OUTPUT_MAX_TYPES ? clientOutputStocks[index] : 0;
    }

    // ==================== 按钮处理 ====================

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        if (id >= BUTTON_FACE_BASE && id < BUTTON_FACE_BASE + 6) {
            entity.cycleDirection(Direction.values()[id - BUTTON_FACE_BASE]);
        } else if (id == BUTTON_MODE) {
            entity.cycleMode();
        } else if (id == BUTTON_DEPOSIT_INPUT) {
            depositCarried();
        } else if (id >= BUTTON_INPUT_ONE_BASE && id < BUTTON_INPUT_ONE_BASE + InstantInscriberEntity.INPUT_MAX_TYPES) {
            extract(player, true, id - BUTTON_INPUT_ONE_BASE, 1);
        } else if (id >= BUTTON_INPUT_STACK_BASE && id < BUTTON_INPUT_STACK_BASE + InstantInscriberEntity.INPUT_MAX_TYPES) {
            int i = id - BUTTON_INPUT_STACK_BASE;
            ItemStack template = entity.getInputStack(i);
            long max = template.isEmpty() ? 1 : template.getMaxStackSize();
            extract(player, true, i, max);
        } else if (id >= BUTTON_INPUT_ALL_BASE && id < BUTTON_INPUT_ALL_BASE + InstantInscriberEntity.INPUT_MAX_TYPES) {
            extract(player, true, id - BUTTON_INPUT_ALL_BASE, Long.MAX_VALUE);
        } else if (id >= BUTTON_OUTPUT_ONE_BASE && id < BUTTON_OUTPUT_ONE_BASE + InstantInscriberEntity.OUTPUT_MAX_TYPES) {
            extract(player, false, id - BUTTON_OUTPUT_ONE_BASE, 1);
        } else if (id >= BUTTON_OUTPUT_STACK_BASE && id < BUTTON_OUTPUT_STACK_BASE + InstantInscriberEntity.OUTPUT_MAX_TYPES) {
            int i = id - BUTTON_OUTPUT_STACK_BASE;
            ItemStack template = entity.getOutputStack(i);
            long max = template.isEmpty() ? 1 : template.getMaxStackSize();
            extract(player, false, i, max);
        } else if (id >= BUTTON_OUTPUT_ALL_BASE && id < BUTTON_OUTPUT_ALL_BASE + InstantInscriberEntity.OUTPUT_MAX_TYPES) {
            extract(player, false, id - BUTTON_OUTPUT_ALL_BASE, Long.MAX_VALUE);
        } else {
            return false;
        }
        entity.setChanged();
        return true;
    }

    private void depositCarried() {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }
        ItemStack left = entity.insertInput(carried, false);
        setCarried(left);
    }

    private void extract(Player player, boolean fromInput, int index, long maxCount) {
        if (maxCount <= 0) {
            return;
        }
        long remaining = maxCount;
        Item target = null;
        while (remaining > 0) {
            ItemStack cur = fromInput ? entity.getInputStack(index) : entity.getOutputStack(index);
            if (cur.isEmpty()) {
                break;
            }
            if (target == null) {
                target = cur.getItem();
            } else if (cur.getItem() != target) {
                break;
            }
            long stock = fromInput ? entity.getInputStock(index) : entity.getOutputStock(index);
            if (stock <= 0) {
                break;
            }
            int amount = (int) Math.min(stock, Math.min(remaining, 64));
            long got = fromInput ? entity.extractInputItems(index, amount) : entity.extractOutputItems(index, amount);
            if (got <= 0) {
                break;
            }
            ItemStack stack = cur.copy();
            stack.setCount((int) got);
            player.addItem(stack);
            int placed = (int) got - stack.getCount();
            if (placed < got) {
                ItemStack back = cur.copy();
                back.setCount((int) got - placed);
                if (fromInput) {
                    entity.insertInput(back, false);
                } else {
                    entity.addOutputProduct(back.getItem(), back.getCount());
                }
            }
            remaining -= placed;
            if (placed <= 0) {
                break;
            }
        }
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    /**
     * 快速转移：机器虚拟槽不可取出；玩家背包 Shift+点击 = 整组投料并入输入行。
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        if (entity == null || index < SLOT_PLAYER_BASE || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack left = entity.insertInput(stack, false);
        if (left.getCount() >= stack.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.set(left);
        entity.setChanged();
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 151 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 209));
        }
    }

    private class RowSlot extends Slot {
        private final int index;
        private final boolean input;

        RowSlot(int index, boolean input, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.index = index;
            this.input = input;
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        @Nonnull
        public ItemStack getItem() {
            return input ? getInputStack(index) : getOutputStack(index);
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
