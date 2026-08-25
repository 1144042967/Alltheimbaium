package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
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
 * ATI 自动耕地容器（原版普通箱子界面，27 槽）。
 * <p>
 * 包含 27 个收获存储槽（3×9）与玩家背包，供玩家查看/取出收获物。
 */
public class AutoFarmlandMenu extends AbstractContainerMenu {
    /** "下方输出"按钮 ID */
    public static final int BUTTON_OUTPUT_DOWN = 6;
    private final AutoFarmlandEntity entity;
    // 客户端同步的当前能量（服务端通过数据槽下发）
    private int clientEnergy;
    // 客户端同步的向下输出开关（服务端通过数据槽下发，默认开启）
    private boolean clientOutputToDown = true;

    public AutoFarmlandMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.AUTO_FARMLAND_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (AutoFarmlandEntity) blockEntity;

        // 收获存储容器：27 槽（3×9），只读不可放入（只能取出）
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new SlotItemHandler(entity.storage, j + i * 9, 8 + j * 18, 18 + i * 18) {
                    @Override
                    public boolean mayPlace(@Nonnull ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        // 玩家背包：3×9
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        // 热键栏
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        // 能量同步（客户端 GUI 显示能量条）
        addDataSlot(makeDataSlot(() -> entity.energy.getEnergyStored(), v -> clientEnergy = v));
        // 向下输出开关同步（客户端 GUI 按钮显示状态）
        addDataSlot(makeDataSlot(() -> entity.isOutputToDown() ? 1 : 0, v -> clientOutputToDown = v != 0));
    }

    /** 当前是否开启向下输出（客户端读同步值，服务端读实体） */
    public boolean isOutputToDown() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.isOutputToDown() : clientOutputToDown;
    }

    /**
     * 处理 GUI 按钮点击（"下方输出"开关）
     */
    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        switch (id) {
            case BUTTON_OUTPUT_DOWN -> entity.outputToDown = !entity.outputToDown;
            default -> {
                return false;
            }
        }
        entity.setChanged();
        return true;
    }

    /**
     * 快速转移物品：容器槽（0-26）与玩家背包（27-62）双向移动
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemStack = stack.copy();
            if (index < 27) {
                // 容器槽 -> 玩家背包
                if (!this.moveItemStackTo(stack, 27, 63, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 -> 容器：容器只读，禁止放入
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
        return itemStack;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    /** 当前能量（客户端读同步值，服务端读实体） */
    public int getEnergyStored() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.energy.getEnergyStored() : clientEnergy;
    }

    /** 能量上限 */
    public int getEnergyCapacity() {
        return entity != null ? entity.energy.getMaxEnergyStored() : 0;
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
}
