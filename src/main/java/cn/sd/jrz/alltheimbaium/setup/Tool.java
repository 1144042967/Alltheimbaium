package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Tool {
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
}
