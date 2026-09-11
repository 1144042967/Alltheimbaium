package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.CreativeTransmuterEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * 创造物品质变器容器（布局参考工作台）。
 * <p>
 * 槽位：0~8 = 3×3 输入栏，9 = 输出栏，10~45 = 玩家背包。
 * 输入输出都是机器的真实物品栏，因此变更会随容器自动同步，不需要数据槽；
 * 转化在实体侧按 tick 结算，界面只负责展示。
 */
public class CreativeTransmuterMenu extends AbstractContainerMenu {
    /** 输出栏在槽位列表中的下标 */
    public static final int OUTPUT_INDEX = CreativeTransmuterEntity.OUTPUT_SLOT;
    /** 玩家背包起始下标 */
    private static final int PLAYER_START = CreativeTransmuterEntity.SLOT_COUNT;
    /** 玩家背包结束下标（不含） */
    private static final int PLAYER_END = PLAYER_START + 36;

    private final CreativeTransmuterEntity entity;

    public CreativeTransmuterMenu(int id, @Nonnull Inventory playerInventory, @Nonnull BlockPos pos) {
        super(Registration.CREATIVE_TRANSMUTER_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = blockEntity instanceof CreativeTransmuterEntity transmuter ? transmuter : null;
        // 方块实体缺失时给一份临时物品栏，避免界面构造直接崩溃
        IItemHandler handler = this.entity != null
                ? this.entity.getInventory()
                : new ItemStackHandler(CreativeTransmuterEntity.SLOT_COUNT);

        // 3×3 输入栏（每格 1 个）
        for (int i = 0; i < CreativeTransmuterEntity.INPUT_SLOTS; i++) {
            addSlot(new OneItemSlot(handler, i, 30 + (i % 3) * 18, 17 + (i / 3) * 18));
        }
        // 输出栏
        addSlot(new OneItemSlot(handler, CreativeTransmuterEntity.OUTPUT_SLOT, 124, 35));
        // 玩家背包（3 行 + 快捷栏）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /**
     * 每格只存 1 个的槽位：机器侧 {@code getSlotLimit} 已经是 1，
     * 这里再覆写一次，保证原版容器逻辑（拖拽、双击、Shift 转移）也按 1 个来算。
     */
    private static class OneItemSlot extends SlotItemHandler {
        OneItemSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
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
     * 快速转移：输出栏与输入栏都往玩家背包搬，玩家背包往输入栏搬（只接受配方材料，放不进去的原地不动）。
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == OUTPUT_INDEX) {
            if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index < CreativeTransmuterEntity.SLOT_COUNT) {
            // 输入栏 → 玩家背包
            if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 玩家背包 → 输入栏
            if (!this.moveItemStackTo(stack, 0, CreativeTransmuterEntity.INPUT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
