package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

    public static long suit(String value) {
        try {
            return suit(Long.parseLong(value));
        } catch (Exception e) {
            return 0;
        }
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

    public static String toItemString(List<ItemStack> itemList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < itemList.size(); i++) {
            if (i != 0) {
                sb.append(",");
            }
            ItemStack stack = itemList.get(i);
            sb.append(BuiltInRegistries.ITEM.getKey(stack.getItem())).append("-").append(stack.getCount());
        }
        return sb.toString();
    }

    public static List<ItemStack> fromItemString(String itemString) {
        List<ItemStack> list = new ArrayList<>();
        String[] split = itemString.split(",");
        for (String s : split) {
            String[] sub = s.split("-");
            list.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(sub[0])), (int) Tool.suit(sub[1])));
        }
        return list;
    }

    public static String toBlockString(List<Long> blockList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < blockList.size(); i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(blockList.get(i));
        }
        return sb.toString();
    }

    public static List<Long> fromBlockString(String blockString) {
        List<Long> list = new ArrayList<>();
        String[] split = blockString.split(",");
        for (String s : split) {
            list.add(Tool.suit(s));
        }
        return list;
    }

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
