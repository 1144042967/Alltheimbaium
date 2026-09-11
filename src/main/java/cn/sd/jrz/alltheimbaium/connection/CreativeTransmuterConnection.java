package cn.sd.jrz.alltheimbaium.connection;

import cn.sd.jrz.alltheimbaium.entity.CreativeTransmuterEntity;
import cn.sd.jrz.alltheimbaium.setup.TransmuteCatalog;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 创造物品质变器对外 IItemHandler（方向无关）。
 * <p>
 * 槽位与机器内部一一对应：槽 0~8 是输入栏，槽 9 是输出栏。两个方向不对称——
 * <ul>
 *     <li><b>输入栏</b>：可插入、不可抽取（材料进得去拿不回，避免管道把刚放进去的材料又抽走），
 *         且只接受配方内的材料，无关物品塞不进来；</li>
 *     <li><b>输出栏</b>：可抽取、不可插入（产物只能由机器自己写入）。</li>
 * </ul>
 * 每格上限都是 1，管道一次推入多组时会自动摊到九格里。
 */
public class CreativeTransmuterConnection implements IItemHandler {
    private static final Logger log = LoggerFactory.getLogger(CreativeTransmuterConnection.class);
    private final CreativeTransmuterEntity owner;

    public CreativeTransmuterConnection(CreativeTransmuterEntity owner) {
        this.owner = owner;
    }

    @Override
    public int getSlots() {
        return CreativeTransmuterEntity.SLOT_COUNT;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        try {
            if (slot < 0 || slot >= CreativeTransmuterEntity.SLOT_COUNT) {
                return ItemStack.EMPTY;
            }
            return owner.getInventory().getStackInSlot(slot);
        } catch (Throwable e) {
            log.error("CreativeTransmuterConnection.getStackInSlot error", e);
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        try {
            // 输出栏不收外部放入
            if (slot < 0 || slot >= CreativeTransmuterEntity.INPUT_SLOTS) {
                return stack;
            }
            return owner.getInventory().insertItem(slot, stack, simulate);
        } catch (Throwable e) {
            log.error("CreativeTransmuterConnection.insertItem error", e);
        }
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        try {
            // 输入栏不可抽取
            if (slot != CreativeTransmuterEntity.OUTPUT_SLOT || amount <= 0) {
                return ItemStack.EMPTY;
            }
            return owner.getInventory().extractItem(slot, amount, simulate);
        } catch (Throwable e) {
            log.error("CreativeTransmuterConnection.extractItem error", e);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return slot >= 0 && slot < CreativeTransmuterEntity.INPUT_SLOTS && TransmuteCatalog.isValidInput(stack);
    }
}
