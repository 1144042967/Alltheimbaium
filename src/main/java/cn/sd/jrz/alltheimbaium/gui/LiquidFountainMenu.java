package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 液体无限制造机容器。
 * <p>
 * 包含 + 槽（0，输入桶/容器）、- 槽（1，输出处理完毕的桶/容器）以及玩家背包。
 * 通过数据槽把流体类型、存量、阈值、六面开关同步到客户端用于 GUI 展示，
 * 并通过按钮（clickMenuButton）修改每台机器的六面主动输出开关。
 */
public class LiquidFountainMenu extends AbstractContainerMenu {
    // 按钮 ID
    public static final int BUTTON_TRANSFER_DOWN = 0;
    public static final int BUTTON_TRANSFER_UP = 1;
    public static final int BUTTON_TRANSFER_NORTH = 2;
    public static final int BUTTON_TRANSFER_SOUTH = 3;
    public static final int BUTTON_TRANSFER_WEST = 4;
    public static final int BUTTON_TRANSFER_EAST = 5;

    public final LiquidFountainEntity entity;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private int clientFluidId;
    private long clientLiquid;
    private long clientMax;
    // 六面主动输出开关（客户端侧同步值），索引与 Direction.values() 顺序一致
    private final boolean[] clientTransfer = new boolean[6];

    public LiquidFountainMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.LIQUID_FOUNTAIN_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (LiquidFountainEntity) blockEntity;

        // 机器槽位：0 = + 槽（输入），1 = - 槽（输出）——左上与左下，对应新 GUI 图
        addSlot(new SlotItemHandler(entity.inputSlot, 0, 8, 18));
        addSlot(new SlotItemHandler(entity.outputSlot, 0, 8, 75));
        // 玩家背包：2-37
        addPlayerInventory(playerInventory);

        // 数据同步（long 拆成高低 32 位两个数据槽）
        addDataSlot(makeDataSlot(() -> getFluidId(entity), v -> clientFluidId = v));
        addDataSlot(makeDataSlot(() -> hiWord(entity.getFluidAmount()), v -> clientLiquid = mergeLong(v, loWord(clientLiquid))));
        addDataSlot(makeDataSlot(() -> loWord(entity.getFluidAmount()), v -> clientLiquid = mergeLong(hiWord(clientLiquid), v)));
        addDataSlot(makeDataSlot(() -> hiWord(entity.getMax()), v -> clientMax = mergeLong(v, loWord(clientMax))));
        addDataSlot(makeDataSlot(() -> loWord(entity.getMax()), v -> clientMax = mergeLong(hiWord(clientMax), v)));
        // 六面主动输出开关数据槽（Direction.values() 顺序与 BUTTON_TRANSFER_* 一致）
        for (Direction direction : Direction.values()) {
            int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.isTransferEnabled(direction) ? 1 : 0, v -> clientTransfer[idx] = v != 0));
        }
    }

    /**
     * 服务端读实体流体注册 id，客户端读同步值
     */
    private static int getFluidId(LiquidFountainEntity entity) {
        if (entity.getStack().isEmpty()) {
            return BuiltInRegistries.FLUID.getId(Fluids.EMPTY);
        }
        //noinspection deprecation
        return BuiltInRegistries.FLUID.getId(entity.getStack().getFluid());
    }

    /**
     * 客户端/服务端都能访问的展示值（服务端读实体，客户端读同步值）
     */
    public Fluid getFluid() {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.getStack().isEmpty() ? Fluids.EMPTY : entity.getStack().getFluid();
        }
        return BuiltInRegistries.FLUID.byId(clientFluidId);
    }

    public long getAmount() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.getFluidAmount() : clientLiquid;
    }

    public long getMax() {
        return entity != null ? entity.getMax() : clientMax;
    }

    public boolean isInfinity() {
        return getAmount() >= getMax() && getFluid() != Fluids.EMPTY;
    }

    public boolean isFaceEnabled(Direction direction) {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.isTransferEnabled(direction);
        }
        return clientTransfer[direction.ordinal()];
    }

    /**
     * 处理 GUI 按钮点击（六面主动输出开关）
     */
    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        switch (id) {
            case BUTTON_TRANSFER_DOWN -> entity.transferDown = !entity.transferDown;
            case BUTTON_TRANSFER_UP -> entity.transferUp = !entity.transferUp;
            case BUTTON_TRANSFER_NORTH -> entity.transferNorth = !entity.transferNorth;
            case BUTTON_TRANSFER_SOUTH -> entity.transferSouth = !entity.transferSouth;
            case BUTTON_TRANSFER_WEST -> entity.transferWest = !entity.transferWest;
            case BUTTON_TRANSFER_EAST -> entity.transferEast = !entity.transferEast;
            default -> {
                return false;
            }
        }
        entity.setChanged();
        return true;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    /**
     * 快速转移物品
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemStack = stack.copy();
            if (index < 2) {
                // 机器槽 -> 玩家背包
                if (!this.moveItemStackTo(stack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 -> 优先 + 槽，其次 - 槽
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    if (!this.moveItemStackTo(stack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
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
        return itemStack;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 110 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 168));
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
