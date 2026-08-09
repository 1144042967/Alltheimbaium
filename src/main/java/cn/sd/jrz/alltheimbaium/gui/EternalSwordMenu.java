package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.item.EternalSwordItem;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 永恒之剑配置界面菜单。
 * <p>
 * - 27 格剑槽（存放带伤害物品与附魔书）
 * - 模式 / 范围按钮通过 clickMenuButton 处理
 * - 槽位变化或关闭时写回剑 NBT 并重新计算伤害与附魔
 */
public class EternalSwordMenu extends AbstractContainerMenu {

    /** 27 格剑槽 */
    private final SimpleContainer swordInv = new SimpleContainer(EternalSwordItem.INVENTORY_SIZE);
    /** 服务端持有剑引用；客户端为 null */
    private final ItemStack sword;
    /** 模式数据槽：0=敌对生物，1=所有生物 */
    private final DataSlot killAllSlot;
    /** 距离数据槽：8 / 16 / 24 / 32 */
    private final DataSlot rangeSlot;

    /** 客户端构造 */
    public EternalSwordMenu(int id, Inventory inv) {
        this(id, inv, null, null);
    }

    /** 服务端构造 */
    public EternalSwordMenu(int id, Inventory inv, Player player, ItemStack sword) {
        super(Registration.ETERNAL_SWORD_MENU.get(), id);
        this.sword = sword;
        // 服务端从剑 NBT 加载 27 格槽位
        if (sword != null && !sword.isEmpty()) {
            EternalSwordItem.loadInventory(sword, swordInv);
        }
        // 剑槽内容变化时实时保存到剑 NBT
        swordInv.addListener(new ContainerListener() {
            @Override
            public void containerChanged(Container container) {
                saveInventory();
            }
        });
        // 27 个剑槽（顶部 3 行 9 列）
        for (int i = 0; i < EternalSwordItem.INVENTORY_SIZE; i++) {
            addSlot(new Slot(swordInv, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }
        // 玩家背包（3 行）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 198));
        }
        // 配置数据槽（服务端初始值来自剑 NBT，广播同步给客户端）
        this.killAllSlot = DataSlot.standalone();
        this.rangeSlot = DataSlot.standalone();
        if (sword != null && !sword.isEmpty()) {
            this.killAllSlot.set(EternalSwordItem.getKillAll(sword));
            this.rangeSlot.set(EternalSwordItem.getRange(sword));
        }
        addDataSlot(killAllSlot);
        addDataSlot(rangeSlot);
    }

    public int getKillAll() {
        return killAllSlot.get();
    }

    public int getRange() {
        return rangeSlot.get();
    }

    /** 按钮点击：0=切换击杀模式，1..4=设置攻击距离 8/16/24/32 */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (sword == null || sword.isEmpty()) return false;
        CompoundTag tag = sword.getOrCreateTag();
        if (id == 0) {
            int mode = EternalSwordItem.getKillAll(sword);
            tag.putInt(EternalSwordItem.TAG_KILL_ALL, mode == 0 ? 1 : 0);
            killAllSlot.set(mode == 0 ? 1 : 0);
        } else if (id >= 1 && id <= EternalSwordItem.RANGES.length) {
            int range = EternalSwordItem.RANGES[id - 1];
            tag.putInt(EternalSwordItem.TAG_RANGE, range);
            rangeSlot.set(range);
        } else {
            return false;
        }
        saveInventory();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveInventory();
    }

    /** 保存 27 格槽位到剑 NBT 并重新计算伤害与附魔 */
    private void saveInventory() {
        if (sword == null || sword.isEmpty()) return;
        EternalSwordItem.saveInventory(sword, swordInv);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < EternalSwordItem.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(stack, EternalSwordItem.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, EternalSwordItem.INVENTORY_SIZE, false)) {
                return ItemStack.EMPTY;
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

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
