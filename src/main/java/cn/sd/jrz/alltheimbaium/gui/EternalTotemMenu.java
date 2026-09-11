package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.item.EternalTotemItem;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

/**
 * 永恒图腾配置界面菜单。
 * <p>
 * - 27 格药水 / 食物槽位（药水或任意可食用物品）
 * - 槽位内容随打开时写入图腾 NBT，所以界面只是图腾数据的编辑器
 * - 死亡后先应用基础复活效果，再逐个应用药水槽中药水的效果
 */
public class EternalTotemMenu extends AbstractContainerMenu {

    /**
     * 图腾容器大小：27 格药水 / 食物槽
     */
    private static final int CONTAINER_SIZE = EternalTotemItem.STORAGE_INVENTORY_SIZE;

    private final SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
    /**
     * 服务端持有图腾引用；客户端为 null
     */
    private final ItemStack totem;
    /**
     * 服务端玩家引用；客户端为 null
     */
    private final Player player;

    /**
     * 客户端构造
     */
    public EternalTotemMenu(int id, Inventory inv) {
        this(id, inv, null, null);
    }

    /**
     * 服务端构造
     */
    public EternalTotemMenu(int id, Inventory inv, Player player, ItemStack totem) {
        super(Registration.ETERNAL_TOTEM_MENU.get(), id);
        this.totem = totem;
        this.player = player;
        // 服务端从图腾 NBT 加载 27 格药水槽位
        if (totem != null && !totem.isEmpty()) {
            EternalTotemItem.loadPotionItems(totem, container);
        }
        // 槽位变化：保存药水 / 食物槽到图腾 NBT
        container.addListener(new ContainerListener() {
            @Override
            public void containerChanged(Container c) {
                savePotionItems();
            }
        });
        // 27 个药水 / 食物槽（顶部 3 行 9 列）
        for (int i = 0; i < EternalTotemItem.STORAGE_INVENTORY_SIZE; i++) {
            addSlot(new PotionFoodSlot(container, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }
        // 玩家背包（3 行）与快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 143));
        }
    }

    private void savePotionItems() {
        if (totem == null || totem.isEmpty()) return;
        EternalTotemItem.savePotionItems(totem, container);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        savePotionItems();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < CONTAINER_SIZE) {
                // 图腾容器 → 玩家背包
                if (!this.moveItemStackTo(stack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 → 图腾容器（mayPlace 决定只有药水与可食用物品放得进去）
                if (!this.moveItemStackTo(stack, 0, CONTAINER_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }

    /**
     * 拦截所有鼠标/键盘点击：任何涉及打开的图腾的操作（拿起、放下、shift、数字键交换）都会被拒绝
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (totem != null && !totem.isEmpty() && this.player != null
                && wouldMoveTotem(slotId, button, clickType, player)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /**
     * 本次点击是否会移动打开的图腾：拖拽中的图腾，或点击的槽位/数字键目标槽是图腾
     */
    private boolean wouldMoveTotem(int slotId, int button, ClickType clickType, Player player) {
        if (!this.getCarried().isEmpty() && isTotem(this.getCarried())) return true;
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (slot != null && isTotem(slot.getItem())) return true;
        }
        // 数字键交换：button 是目标快捷栏（0-8），对应菜单槽 药水27 + 背包27 = 54 起
        if (clickType == ClickType.SWAP) {
            int hotbarStart = CONTAINER_SIZE + 27;
            int hotbarSlot = hotbarStart + button;
            if (hotbarSlot >= 0 && hotbarSlot < this.slots.size()) {
                Slot hb = this.slots.get(hotbarSlot);
                if (hb != null && isTotem(hb.getItem())) return true;
            }
        }
        return false;
    }

    /**
     * 判断物品是否为「打开的这只图腾」：引用一致或内容完全一致
     */
    private boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || totem == null || totem.isEmpty()) return false;
        if (!stack.is(Registration.ETERNAL_TOTEM.get())) return false;
        return stack == totem || ItemStack.isSameItemSameTags(stack, totem);
    }

    @Override
    public boolean stillValid(Player player) {
        // 图腾不在主手/副手（被移动、掉落或移除）时关闭配置界面
        if (totem == null || totem.isEmpty()) return true;
        return isTotem(player.getMainHandItem()) || isTotem(player.getOffhandItem());
    }

    /**
     * 药水 / 食物槽：药水类物品与任何可食用物品都可以放
     */
    private static class PotionFoodSlot extends Slot {
        PotionFoodSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof PotionItem || stack.isEdible();
        }
    }
}
