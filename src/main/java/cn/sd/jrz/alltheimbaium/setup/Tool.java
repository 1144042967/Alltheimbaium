package cn.sd.jrz.alltheimbaium.setup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static JsonArray fromArray(ItemStack[] array) {
        JsonArray a = new JsonArray();
        for (ItemStack stack : array) {
            a.add(fromItem(stack));
        }
        return a;
    }

    public static JsonObject fromItem(ItemStack stack) {
        JsonObject o = new JsonObject();
        if (stack == null || stack.isEmpty()) {
            return o;
        }
        //noinspection deprecation
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(stack.getItem());
        o.addProperty("id", resourcelocation.toString());
        o.addProperty("c", stack.getCount());
        return o;
    }

    public static ItemStack[] toArray(JsonArray array) {
        ItemStack[] a = new ItemStack[array.size()];
        for (int i = 0; i < array.size(); i++) {
            a[i] = toItem(array.get(i).getAsJsonObject());
        }
        return a;
    }

    public static ItemStack toItem(JsonObject object) {
        if (!object.has("id")) {
            return null;
        }
        try {
            String id = object.get("id").getAsString();
            int count = object.get("c").getAsInt();
            //noinspection deprecation
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id)), count);
        } catch (Exception e) {
            log.error("Failed to parse item", e);
            return null;
        }
    }
}
