package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
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
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 存储方块制造机容器。
 * <p>
 * 槽位：0 = 标记槽（放入物品标记/取消标记），1~9 = 已标记物品槽（单击提取），10~45 = 玩家背包。
 * 通过数据槽同步产量、增长进度、九个物品槽的物品 id 与存量、六面输出状态与总开关，
 * 通过按钮（clickMenuButton）修改六面状态、提取物品与输出总开关。
 */
public class StorageFountainMenu extends AbstractContainerMenu {
    // 按钮 ID
    public static final int BUTTON_DIR_BASE = 0;                 // 0~5：循环切换六面输出状态（与 Direction.values() 顺序一致）
    public static final int BUTTON_EXTRACT_ONE_BASE = 6;         // 6~14：单击提取 1 个
    public static final int BUTTON_EXTRACT_STACK_BASE = 15;      // 15~23：shift 提取 1 组
    public static final int BUTTON_EXTRACT_ALL_BASE = 24;        // 24~32：空格 提取到背包满
    public static final int BUTTON_OUTPUT = 33;                  // 输出总开关

    public final StorageFountainEntity entity;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private long clientOutput;
    private int clientTickCount;
    private final int[] clientItemIds = new int[9];
    private final long[] clientCounts = new long[9];
    private final int[] clientDirectionState = new int[6];
    private boolean clientOutputEnabled;

    public StorageFountainMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.STORAGE_FOUNTAIN_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (StorageFountainEntity) blockEntity;

        // 标记槽（0）：放入物品触发标记/取消标记，处理完毕后立即清空；不允许取出
        addSlot(new SlotItemHandler(entity.markerSlot, 0, 150, 49) {
            @Override
            public boolean mayPickup(@Nonnull Player player) {
                return false;
            }
        });
        // 已标记物品槽：1~9（虚拟槽，单击提取由 Screen 拦截发按钮）
        for (int i = 0; i < 9; i++) {
            addSlot(new MarkedSlot(i, 8 + i * 18, 71));
        }
        // 玩家背包：10~45
        addPlayerInventory(playerInventory);

        // 数据同步（long 拆成高低 32 位两个数据槽）
        addDataSlot(makeDataSlot(() -> hiWord(entity.output), v -> clientOutput = mergeLong(v, loWord(clientOutput))));
        addDataSlot(makeDataSlot(() -> loWord(entity.output), v -> clientOutput = mergeLong(hiWord(clientOutput), v)));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.tickCount), v -> clientTickCount = v));
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            addDataSlot(makeDataSlot(() -> entity.getMarkedItemId(idx), v -> clientItemIds[idx] = v));
            addDataSlot(makeDataSlot(() -> hiWord(entity.getMarkedCount(idx)), v -> clientCounts[idx] = mergeLong(v, loWord(clientCounts[idx]))));
            addDataSlot(makeDataSlot(() -> loWord(entity.getMarkedCount(idx)), v -> clientCounts[idx] = mergeLong(hiWord(clientCounts[idx]), v)));
        }
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.getDirectionState(direction), v -> clientDirectionState[idx] = v));
        }
        addDataSlot(makeDataSlot(() -> entity.outputEnabled ? 1 : 0, v -> clientOutputEnabled = v != 0));
    }

    // ==================== 客户端/服务端都能访问的展示值 ====================

    public long getOutput() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.output : clientOutput;
    }

    public int getTickCount() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
    }

    public long getGrowthIntervalSeconds() {
        return StorageFountainBlock.getGrowthIntervalSeconds();
    }

    public long getStep() {
        return StorageFountainBlock.getStep();
    }

    public int getDirectionState(Direction direction) {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.getDirectionState(direction);
        }
        return clientDirectionState[direction.ordinal()];
    }

    public boolean isOutputEnabled() {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.outputEnabled;
        }
        return clientOutputEnabled;
    }

    /**
     * 指定槽位的物品模板（数量 1），供 GUI 图标展示
     */
    @Nonnull
    public ItemStack getMarkedStack(int index) {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.getMarkedStack(index);
        }
        if (index < 0 || index >= 9 || clientItemIds[index] <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        return new ItemStack(BuiltInRegistries.ITEM.byId(clientItemIds[index]), 1);
    }

    /**
     * 指定槽位的存量（内部单位）
     */
    public long getMarkedCount(int index) {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.getMarkedCount(index);
        }
        return index >= 0 && index < 9 ? clientCounts[index] : 0;
    }

    // ==================== 按钮处理 ====================

    /**
     * 处理 GUI 按钮点击（六面状态、提取、标记/取消标记、输出总开关）
     */
    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        if (id >= BUTTON_DIR_BASE && id < BUTTON_DIR_BASE + 6) {
            entity.cycleDirectionState(Direction.values()[id - BUTTON_DIR_BASE]);
        } else if (id >= BUTTON_EXTRACT_ONE_BASE && id < BUTTON_EXTRACT_ONE_BASE + 9) {
            extract(player, id - BUTTON_EXTRACT_ONE_BASE, 1);
        } else if (id >= BUTTON_EXTRACT_STACK_BASE && id < BUTTON_EXTRACT_STACK_BASE + 9) {
            int slot = id - BUTTON_EXTRACT_STACK_BASE;
            ItemStack template = entity.getMarkedStack(slot);
            long maxStack = template.isEmpty() ? 1 : template.getMaxStackSize();
            extract(player, slot, maxStack);
        } else if (id >= BUTTON_EXTRACT_ALL_BASE && id < BUTTON_EXTRACT_ALL_BASE + 9) {
            extract(player, id - BUTTON_EXTRACT_ALL_BASE, Long.MAX_VALUE);
        } else if (id == BUTTON_OUTPUT) {
            entity.outputEnabled = !entity.outputEnabled;
        } else {
            return false;
        }
        entity.setChanged();
        return true;
    }

    /**
     * 从指定槽位提取最多 maxCount 个物品放入玩家背包；放不下时退回存量
     */
    private void extract(Player player, int slot, long maxCount) {
        if (maxCount <= 0) {
            return;
        }
        long remaining = maxCount;
        while (remaining > 0) {
            long available = entity.getMarkedCount(slot) / StorageFountainBlock.getCarry();
            if (available <= 0) {
                break;
            }
            int amount = (int) Math.min(available, Math.min(remaining, 64));
            long got = entity.extractItems(slot, amount);
            if (got <= 0) {
                break;
            }
            ItemStack stack = entity.getMarkedStack(slot).copy();
            stack.setCount((int) got);
            // addItem 会把无法放入背包的剩余部分留在 stack 中
            player.addItem(stack);
            int placed = (int) got - stack.getCount();
            if (placed < got) {
                // 背包放不下的部分退回存量
                entity.blockList.set(slot, entity.getMarkedCount(slot) + (got - placed) * StorageFountainBlock.getCarry());
            }
            remaining -= placed;
            if (placed <= 0) {
                break; // 背包已满
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

    /**
     * 快速转移物品：标记槽与已标记物品槽不可取出；玩家背包可移向标记槽（Shift 点击标记）
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        if (index < 10) {
            // 标记槽与已标记物品槽不可取出
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack itemStack = stack.copy();
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
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

    /**
     * 已标记物品槽：虚拟槽，getItem 从实体/同步数据计算，不可放入/取出（取出由 Screen 拦截按钮处理）
     */
    private class MarkedSlot extends Slot {
        private final int index;

        MarkedSlot(int index, int x, int y) {
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
            return getMarkedStack(index);
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
