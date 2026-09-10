package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity;
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
 * 零刻熔炉容器（大数版）。
 * <p>
 * 槽位：0~17 输入行（虚拟展示，两行）、18~35 输出行（虚拟展示，两行）、36~71 玩家背包。
 * 通过数据槽同步能量、六面状态与 18+18 行的 物品 id + 存量(long)。
 * 交互：点击虚拟格取物（1/一组/空格取满）、点输入格投料（持物品时并入输入行）、
 * 背包 Shift+点击直接投料，另有 交换输入/输出 与 六面推送开关。
 */
public class InstantFurnaceMenu extends AbstractContainerMenu {
    // 按钮 ID
    public static final int BUTTON_FACE_BASE = 0;            // 0~5：六面推送开关
    public static final int BUTTON_SWAP = 6;                 // 交换输入/输出
    public static final int BUTTON_DEPOSIT_INPUT = 7;        // 把手中物品并入输入行
    // 输入行取物（0~17）
    public static final int BUTTON_INPUT_ONE_BASE = 10;
    public static final int BUTTON_INPUT_STACK_BASE = 40;
    public static final int BUTTON_INPUT_ALL_BASE = 70;
    // 输出行取物（0~17）
    public static final int BUTTON_OUTPUT_ONE_BASE = 100;
    public static final int BUTTON_OUTPUT_STACK_BASE = 130;
    public static final int BUTTON_OUTPUT_ALL_BASE = 160;

    public static final int SLOT_INPUT_BASE = 0;
    public static final int SLOT_OUTPUT_BASE = InstantFurnaceEntity.MAX_TYPES;
    public static final int SLOT_PLAYER_BASE = InstantFurnaceEntity.MAX_TYPES * 2;

    /** GUI 总高度（像素），对应 176×214 贴图 */
    public static final int IMAGE_HEIGHT = 214;

    public final InstantFurnaceEntity entity;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private int clientEnergy;
    private final int[] clientDirectionState = new int[6];
    private final int[] clientInputIds = new int[InstantFurnaceEntity.MAX_TYPES];
    private final long[] clientInputStocks = new long[InstantFurnaceEntity.MAX_TYPES];
    private final int[] clientOutputIds = new int[InstantFurnaceEntity.MAX_TYPES];
    private final long[] clientOutputStocks = new long[InstantFurnaceEntity.MAX_TYPES];

    public InstantFurnaceMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.INSTANT_FURNACE_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = blockEntity instanceof InstantFurnaceEntity furnace ? furnace : null;

        // 输入行 0~17：两行（y 26 / 44）
        for (int i = 0; i < InstantFurnaceEntity.MAX_TYPES; i++) {
            addSlot(new RowSlot(i, true, 8 + (i % 9) * 18, 26 + (i / 9) * 18));
        }
        // 输出行 18~35：两行（y 83 / 101）
        for (int i = 0; i < InstantFurnaceEntity.MAX_TYPES; i++) {
            addSlot(new RowSlot(i, false, 8 + (i % 9) * 18, 83 + (i / 9) * 18));
        }
        // 玩家背包 36~71
        addPlayerInventory(playerInventory);

        // 数据同步：能量 + 六面状态
        addDataSlot(makeDataSlot(() -> entity == null ? 0 : entity.getEnergyStored(), v -> clientEnergy = v));
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : entity.getDirectionState(direction), v -> clientDirectionState[idx] = v));
        }
        // 输入行 18：itemId + 存量(hi/lo)
        for (int i = 0; i < InstantFurnaceEntity.MAX_TYPES; i++) {
            final int idx = i;
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : entity.getInputItemId(idx), v -> clientInputIds[idx] = v));
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : hiWord(entity.getInputStock(idx)), v -> clientInputStocks[idx] = mergeLong(v, loWord(clientInputStocks[idx]))));
            addDataSlot(makeDataSlot(() -> entity == null ? 0 : loWord(entity.getInputStock(idx)), v -> clientInputStocks[idx] = mergeLong(hiWord(clientInputStocks[idx]), v)));
        }
        // 输出行 18：itemId + 存量(hi/lo)
        for (int i = 0; i < InstantFurnaceEntity.MAX_TYPES; i++) {
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
        return InstantFurnaceEntity.MAX_ENERGY;
    }

    public int getEnergyPerSmelt() {
        return InstantFurnaceEntity.ENERGY_PER_SMELT;
    }

    public int getDirectionState(Direction direction) {
        return serverSide() ? entity.getDirectionState(direction) : clientDirectionState[direction.ordinal()];
    }

    @Nonnull
    public ItemStack getInputStack(int index) {
        if (serverSide()) {
            return entity.getInputStack(index);
        }
        if (index < 0 || index >= InstantFurnaceEntity.MAX_TYPES || clientInputIds[index] <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        return new ItemStack(BuiltInRegistries.ITEM.byId(clientInputIds[index]), 1);
    }

    public long getInputStock(int index) {
        if (serverSide()) {
            return entity.getInputStock(index);
        }
        return index >= 0 && index < InstantFurnaceEntity.MAX_TYPES ? clientInputStocks[index] : 0;
    }

    @Nonnull
    public ItemStack getOutputStack(int index) {
        if (serverSide()) {
            return entity.getOutputStack(index);
        }
        if (index < 0 || index >= InstantFurnaceEntity.MAX_TYPES || clientOutputIds[index] <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        return new ItemStack(BuiltInRegistries.ITEM.byId(clientOutputIds[index]), 1);
    }

    public long getOutputStock(int index) {
        if (serverSide()) {
            return entity.getOutputStock(index);
        }
        return index >= 0 && index < InstantFurnaceEntity.MAX_TYPES ? clientOutputStocks[index] : 0;
    }

    // ==================== 按钮处理 ====================

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        if (id >= BUTTON_FACE_BASE && id < BUTTON_FACE_BASE + 6) {
            entity.cycleDirection(Direction.values()[id - BUTTON_FACE_BASE]);
        } else if (id == BUTTON_SWAP) {
            entity.swapSlots();
        } else if (id == BUTTON_DEPOSIT_INPUT) {
            depositCarried();
        } else if (id >= BUTTON_INPUT_ONE_BASE && id < BUTTON_INPUT_ONE_BASE + InstantFurnaceEntity.MAX_TYPES) {
            extract(player, true, id - BUTTON_INPUT_ONE_BASE, 1);
        } else if (id >= BUTTON_INPUT_STACK_BASE && id < BUTTON_INPUT_STACK_BASE + InstantFurnaceEntity.MAX_TYPES) {
            int i = id - BUTTON_INPUT_STACK_BASE;
            ItemStack template = entity.getInputStack(i);
            long max = template.isEmpty() ? 1 : template.getMaxStackSize();
            extract(player, true, i, max);
        } else if (id >= BUTTON_INPUT_ALL_BASE && id < BUTTON_INPUT_ALL_BASE + InstantFurnaceEntity.MAX_TYPES) {
            extract(player, true, id - BUTTON_INPUT_ALL_BASE, Long.MAX_VALUE);
        } else if (id >= BUTTON_OUTPUT_ONE_BASE && id < BUTTON_OUTPUT_ONE_BASE + InstantFurnaceEntity.MAX_TYPES) {
            extract(player, false, id - BUTTON_OUTPUT_ONE_BASE, 1);
        } else if (id >= BUTTON_OUTPUT_STACK_BASE && id < BUTTON_OUTPUT_STACK_BASE + InstantFurnaceEntity.MAX_TYPES) {
            int i = id - BUTTON_OUTPUT_STACK_BASE;
            ItemStack template = entity.getOutputStack(i);
            long max = template.isEmpty() ? 1 : template.getMaxStackSize();
            extract(player, false, i, max);
        } else if (id >= BUTTON_OUTPUT_ALL_BASE && id < BUTTON_OUTPUT_ALL_BASE + InstantFurnaceEntity.MAX_TYPES) {
            extract(player, false, id - BUTTON_OUTPUT_ALL_BASE, Long.MAX_VALUE);
        } else {
            return false;
        }
        entity.setChanged();
        return true;
    }

    /**
     * 把手中（carried）物品整组并入输入行；放不下（种类满）的部分留在手上
     */
    private void depositCarried() {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }
        ItemStack left = entity.insertInput(carried, false);
        setCarried(left);
    }

    /**
     * 从输入行(true)/输出行(false)取物放入玩家背包；背包放不下的部分退回行。
     */
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
                // 该行已被取空移除、后续行顶替到同 index，停止（避免误取下一行）
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
                // 背包放不下的部分退回行
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
            // 输入行种类已满且该物品不在其中：未放入任何物品
            return ItemStack.EMPTY;
        }
        slot.set(left);
        entity.setChanged();
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // 玩家背包对齐贴图：首行 = imageHeight-82，快捷栏再 +58
        int invTop = IMAGE_HEIGHT - 82;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, invTop + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, invTop + 58));
        }
    }

    /**
     * 输入/输出行的虚拟展示槽：getItem 从同步数据计算，不可放入/取出（交互由 Screen 拦截按钮处理）。
     */
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
