package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
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
 * 生物农场容器。
 * <p>
 * 槽位：0 = 标记槽（刷怪蛋/特征掉落物收容生物），1 = 使用槽（放物品自动模拟右击收容物），
 * 2~28 = 27 个产物行虚拟槽（单击提取），29~64 = 玩家背包。
 * 通过数据槽同步等级/进度、27 行物品 id+存量+权重、收容生物、六面状态与总开关。
 */
public class MobFarmMenu extends AbstractContainerMenu {
    public static final int MAX_PRODUCTS = 27;

    // 按钮 ID
    public static final int BUTTON_DIR_BASE = 0;                 // 0~5：循环切换六面输出状态
    public static final int BUTTON_EXTRACT_ONE_BASE = 6;         // 6~32：单击提取 1 个
    public static final int BUTTON_EXTRACT_STACK_BASE = 33;      // 33~59：shift 提取 1 组
    public static final int BUTTON_EXTRACT_ALL_BASE = 60;        // 60~86：空格 提取到背包满
    public static final int BUTTON_OUTPUT = 87;                  // 输出总开关
    public static final int BUTTON_CLEAR = 88;                   // 清空收容物

    // 槽位号
    public static final int SLOT_MARKER = 0;
    public static final int SLOT_USE = 1;
    public static final int SLOT_PRODUCT_BASE = 2;               // 2~28
    public static final int SLOT_PLAYER_BASE = 29;               // 29~64

    public final MobFarmEntity entity;

    // 客户端镜像（由数据槽同步）
    private final int[] clientItemIds = new int[MAX_PRODUCTS];
    private final long[] clientStocks = new long[MAX_PRODUCTS];
    private final long[] clientWeights = new long[MAX_PRODUCTS];
    private long clientLevel;
    private int clientTickCount;
    private int clientContainedId;
    private final int[] clientDirectionState = new int[6];
    private boolean clientOutputEnabled;

    public MobFarmMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.MOB_FARM_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (MobFarmEntity) blockEntity;

        // 0 标记槽（放入即处理并清空，不允许取出）
        addSlot(new SlotItemHandler(entity.markerSlot, 0, 150, 6) {
            @Override
            public boolean mayPickup(@Nonnull Player player) {
                return false;
            }
        });
        // 1 使用槽
        addSlot(new SlotItemHandler(entity.useSlot, 0, 150, 24));
        // 2~28 产物行虚拟槽（3 行 × 9 列，单击/Shift/空格由 Screen 拦截发按钮）
        for (int i = 0; i < MAX_PRODUCTS; i++) {
            addSlot(new ProductSlot(i, 8 + (i % 9) * 18, 44 + (i / 9) * 18));
        }
        // 29~64 玩家背包
        addPlayerInventory(playerInventory);

        // ===== 数据同步 =====
        addDataSlot(makeDataSlot(() -> hiWord(entity.level), v -> clientLevel = mergeLong(v, loWord(clientLevel))));
        addDataSlot(makeDataSlot(() -> loWord(entity.level), v -> clientLevel = mergeLong(hiWord(clientLevel), v)));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.tickCount), v -> clientTickCount = v));
        addDataSlot(makeDataSlot(entity::getContainedEntityId, v -> clientContainedId = v));
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

    // ==================== 展示 getter（服务端读实体 / 客户端读镜像） ====================

    private boolean serverSide() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide;
    }

    public long getLevel() {
        return serverSide() ? entity.level : clientLevel;
    }

    public int getTickCount() {
        return serverSide() ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
    }

    public int getContainedEntityId() {
        return serverSide() ? entity.getContainedEntityId() : clientContainedId;
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

    // ==================== 按钮处理 ====================

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
        } else if (id == BUTTON_CLEAR) {
            entity.clearContained();
        } else {
            return false;
        }
        entity.setChanged();
        return true;
    }

    /**
     * 从产物行提取最多 maxCount 件放入玩家背包；放不下时退回存量
     */
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
                // 放不下的退回
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

    /**
     * 快速转移：产物虚拟槽不可取出；标记槽不可取出；使用槽可移向背包；背包可 Shift 放入标记槽/使用槽
     */
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
            if (index == SLOT_USE) {
                // 使用槽 → 背包
                if (!this.moveItemStackTo(stack, SLOT_PLAYER_BASE, SLOT_PLAYER_BASE + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= SLOT_PLAYER_BASE) {
                // 背包 → 先标记槽（仅标记物会被接受），再使用槽
                if (!this.moveItemStackTo(stack, SLOT_MARKER, SLOT_USE + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 标记槽/产物行不可快速取出
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
        int baseY = 176;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, baseY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, baseY + 3 * 18 + 2));
        }
    }

    /**
     * 产物行虚拟槽：getItem 从实体/同步数据计算，不可放入/取出（取出由 Screen 拦截按钮处理）
     */
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
