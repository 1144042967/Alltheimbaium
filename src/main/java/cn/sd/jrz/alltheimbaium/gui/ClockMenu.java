package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.ClockEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 加速时钟配置容器（纯配置 GUI，无物品槽）。
 * <p>
 * 通过数据槽同步全局开关、单独开关、当前倍速、六方向开关与六方向实际相邻方块的注册 id；
 * 通过按钮（clickMenuButton）切换全局/单独开关、循环切换六方向开关与倍速。
 */
public class ClockMenu extends AbstractContainerMenu {
    // 按钮 ID
    public static final int BUTTON_GLOBAL = 0;       // 全局开关（所有时钟同步）
    public static final int BUTTON_SELF = 1;         // 单独开关（当前时钟）
    public static final int BUTTON_DIR_BASE = 2;     // 2~7：六方向开关（与 Direction.values() 顺序一致）
    public static final int BUTTON_SPEED_BASE = 8;   // 8~17：十个倍速档位快速选择按钮

    private final ClockEntity entity;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private boolean clientGlobal;
    private boolean clientSelf;
    private int clientSpeed;
    private final boolean[] clientDirEnabled = new boolean[6];
    private final int[] clientNeighborBlockId = new int[6];

    public ClockMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.CLOCK_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (ClockEntity) blockEntity;

        // 纯配置 GUI：不显示物品栏
        // 数据同步
        addDataSlot(makeDataSlot(() -> ClockEntity.isGlobalActive() ? 1 : 0, v -> clientGlobal = v != 0));
        addDataSlot(makeDataSlot(() -> entity.enabled ? 1 : 0, v -> clientSelf = v != 0));
        addDataSlot(makeDataSlot(() -> entity.speed, v -> clientSpeed = v));
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.directionEnabled[idx] ? 1 : 0, v -> clientDirEnabled[idx] = v != 0));
            addDataSlot(makeDataSlot(() -> entity.getNeighborBlockId(direction), v -> clientNeighborBlockId[idx] = v));
        }
    }

    // ==================== 客户端/服务端都能访问的展示值 ====================

    public boolean isGlobalActive() {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return ClockEntity.isGlobalActive();
        }
        return clientGlobal;
    }

    public boolean isSelfEnabled() {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.enabled;
        }
        return clientSelf;
    }

    public int getSpeed() {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.speed;
        }
        return clientSpeed;
    }

    public boolean isDirectionEnabled(Direction direction) {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.directionEnabled[direction.ordinal()];
        }
        return clientDirEnabled[direction.ordinal()];
    }

    /**
     * 指定方向实际相邻方块的物品栈（数量 1），无方块或方块无物品时返回空，供 GUI 图标展示
     */
    @Nonnull
    public ItemStack getNeighborStack(Direction direction) {
        int id;
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            id = entity.getNeighborBlockId(direction);
        } else {
            id = clientNeighborBlockId[direction.ordinal()];
        }
        if (id <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        Item item = BuiltInRegistries.BLOCK.byId(id).asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * 指定方向实际相邻方块的显示名（无方块时返回空文本），供 GUI tooltip 使用
     */
    @Nonnull
    public Component getNeighborName(Direction direction) {
        int id;
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            id = entity.getNeighborBlockId(direction);
        } else {
            id = clientNeighborBlockId[direction.ordinal()];
        }
        if (id <= 0) {
            return Component.empty();
        }
        //noinspection deprecation
        return BuiltInRegistries.BLOCK.byId(id).getName();
    }

    // ==================== 按钮处理 ====================

    /**
     * 处理 GUI 按钮点击（全局/单独开关、六方向开关、倍速循环切换）
     */
    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        if (id == BUTTON_GLOBAL) {
            ClockEntity.setGlobalActive(!ClockEntity.isGlobalActive());
        } else if (id == BUTTON_SELF) {
            entity.enabled = !entity.enabled;
        } else if (id >= BUTTON_DIR_BASE && id < BUTTON_DIR_BASE + 6) {
            entity.toggleDirection(Direction.values()[id - BUTTON_DIR_BASE]);
        } else if (id >= BUTTON_SPEED_BASE && id < BUTTON_SPEED_BASE + ClockEntity.SPEEDS.length) {
            entity.setSpeed(ClockEntity.SPEEDS[id - BUTTON_SPEED_BASE]);
        } else {
            return false;
        }
        entity.setChanged();
        entity.sendUpdatePacket();
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
     * 纯配置 GUI：无物品栏，禁止任何槽位移动
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
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
