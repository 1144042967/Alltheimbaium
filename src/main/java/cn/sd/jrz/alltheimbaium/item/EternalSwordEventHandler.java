package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * 永恒之剑事件处理：禁止铁砧锻造 / 改名 / 附魔书合成。
 */
@EventBusSubscriber(modid = "alltheimbaium")
public class EternalSwordEventHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.is(Registration.ETERNAL_SWORD.get())
                || right.is(Registration.ETERNAL_SWORD.get())) {
            event.setCanceled(true);
        }
    }
}
