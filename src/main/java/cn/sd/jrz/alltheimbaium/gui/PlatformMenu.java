package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.PlatformBlock;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 生成平台配置容器（纯配置 GUI，无物品槽）。
 * <p>
 * 数据槽同步全局伪装开关状态；
 * 按钮：0 = 伪装总开关，1~9 = 九宫格（以所点平台所在区块为中心的 3×3 区块），点击生成/重建对应区块平台。
 */
public class PlatformMenu extends AbstractContainerMenu {
    // 按钮 ID
    public static final int BUTTON_DISGUISE = 0;
    public static final int BUTTON_CELL_BASE = 1;   // 1~9：九宫格

    /** 打开 GUI 时右击的那个生成平台方块的位置（区块生成以此为锚点） */
    public final BlockPos anchorPos;

    // 客户端展示的伪装开关状态（服务端通过数据槽同步而来）
    private boolean clientDisguise;

    public PlatformMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.PLATFORM_MENU.get(), id);
        this.anchorPos = pos;
        addDataSlot(makeDataSlot(() -> PlatformBlock.isDisguiseActive() ? 1 : 0, v -> clientDisguise = v != 0));
    }

    /**
     * 当前全局伪装开关（客户端读数据槽同步值）
     */
    public boolean isDisguiseActive() {
        return clientDisguise;
    }

    // ==================== 按钮处理 ====================

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (player.level().isClientSide) {
            return false;
        }
        if (id == BUTTON_DISGUISE) {
            if (player.level().getServer() != null) {
                PlatformBlock.setDisguiseActive(player.level().getServer(), !PlatformBlock.isDisguiseActive());
            }
            return true;
        }
        if (id >= BUTTON_CELL_BASE && id < BUTTON_CELL_BASE + 9) {
            int cellIndex = id - BUTTON_CELL_BASE;
            int dx = cellIndex % 3 - 1;   // 列偏移：左=西(-1) 中=0 右=东(+1)
            int dz = cellIndex / 3 - 1;   // 行偏移：上=北(-1) 中=0 下=南(+1)
            PlatformBlock.generateCellAt(player.level(), anchorPos, dx, dz);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        BlockState state = player.level().getBlockState(anchorPos);
        return state.getBlock() instanceof PlatformBlock
                && player.distanceToSqr(anchorPos.getX() + 0.5, anchorPos.getY() + 0.5, anchorPos.getZ() + 0.5) <= 64.0;
    }

    /**
     * 纯配置 GUI：无物品栏，禁止任何槽位移动
     */
    @Override
    @Nonnull
    public net.minecraft.world.item.ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
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
