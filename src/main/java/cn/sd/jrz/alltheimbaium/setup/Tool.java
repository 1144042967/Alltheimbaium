package cn.sd.jrz.alltheimbaium.setup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

    public static JsonArray toJsonArray(List<Item> itemList, List<Long> blockList) {
        JsonArray a = new JsonArray();
        for (int i = 0; i < itemList.size(); i++) {
            Item item = itemList.get(i);
            Long count = blockList.get(i);
            if (item == null || count == null) {
                continue;
            }
            JsonObject stack = new JsonObject();
            //noinspection deprecation
            stack.addProperty("id", BuiltInRegistries.ITEM.getKey(item).toString());
            stack.addProperty("c", count);
            a.add(stack);
        }
        return a;
    }

    public static List<Item> toItemList(JsonArray array) {
        List<Item> itemList = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject stack = element.getAsJsonObject();
            String id = stack.get("id").getAsString();
            Item item = null;
            try {
                //noinspection deprecation
                item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
            } catch (Exception ignored) {
            }
            itemList.add(item);
        }
        return itemList;
    }

    public static List<Long> toBlockList(JsonArray array) {
        List<Long> blockList = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject stack = element.getAsJsonObject();
            long count = stack.get("c").getAsLong();
            blockList.add(count);
        }
        return blockList;
    }

    @SuppressWarnings("deprecation")
    public static void sort(List<Item> itemList, List<Long> blockList) {
        for (int i = 0; i < Math.min(itemList.size(), blockList.size()); i++) {
            for (int j = i + 1; j < Math.min(itemList.size(), blockList.size()); j++) {
                Item aItem = itemList.get(i);
                Item bItem = itemList.get(j);
                String a = BuiltInRegistries.ITEM.getKey(aItem).toString();
                String b = BuiltInRegistries.ITEM.getKey(bItem).toString();
                if (compareName(a, b) > 0) {
                    itemList.set(i, bItem);
                    itemList.set(j, aItem);
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
