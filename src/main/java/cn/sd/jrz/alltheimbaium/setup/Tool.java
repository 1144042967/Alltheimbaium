package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Tool {
    private static final Logger log = LoggerFactory.getLogger(Tool.class);

    public static long suit(long value) {
        return value < 0 ? Long.MAX_VALUE : value;
    }

    public static int suitInt(long value) {
        return value < 0 || value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    public static void takeItem(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            ItemEntity entity = player.drop(stack, false);
            if (entity != null) {
                entity.setNoPickUpDelay();
                entity.setTarget(player.getUUID());
            }
        }
    }

    public static ListTag toJsonArray(List<ItemStack> itemList, List<Long> blockList) {
        ListTag list = new ListTag();
        for (int i = 0; i < itemList.size(); i++) {
            ItemStack item = itemList.get(i);
            Long count = blockList.get(i);
            if (item == null || count == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            item.save(tag);
            tag.putLong("Long_Count", count);
            list.add(tag);
        }
        return list;
    }

    public static List<ItemStack> toItemList(ListTag array) {
        List<ItemStack> itemList = new ArrayList<>();
        for (Tag value : array) {
            CompoundTag tag = (CompoundTag) value;
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) {
                continue;
            }
            stack = stack.copy();
            stack.setCount(1);
            itemList.add(stack);
        }
        return itemList;
    }

    public static List<Long> toBlockList(ListTag array) {
        List<Long> blockList = new ArrayList<>();
        for (Tag value : array) {
            CompoundTag tag = (CompoundTag) value;
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) {
                continue;
            }
            blockList.add(tag.getLong("Long_Count"));
        }
        return blockList;
    }

    @SuppressWarnings("deprecation")
    public static void sort(List<ItemStack> itemList, List<Long> blockList) {
        for (int i = 0; i < Math.min(itemList.size(), blockList.size()); i++) {
            for (int j = i + 1; j < Math.min(itemList.size(), blockList.size()); j++) {
                ItemStack aStack = itemList.get(i);
                ItemStack bStack = itemList.get(j);
                String a = BuiltInRegistries.ITEM.getKey(aStack.getItem()).toString();
                String b = BuiltInRegistries.ITEM.getKey(bStack.getItem()).toString();
                if (compareName(a, b) > 0) {
                    itemList.set(i, bStack);
                    itemList.set(j, aStack);
                    Long aBlock = blockList.get(i);
                    Long bBlock = blockList.get(j);
                    blockList.set(i, bBlock);
                    blockList.set(j, aBlock);
                }
            }
        }
    }

    private static int compareName(String a, String b) {
        if (a.startsWith("minecraft:") && b.startsWith("minecraft:")) {
            return a.compareTo(b);
        }
        if (!a.startsWith("minecraft:") && !b.startsWith("minecraft:")) {
            return a.compareTo(b);
        }
        return a.startsWith("minecraft:") ? -1 : 1;
    }
}
