package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 永恒之剑事件处理：禁止铁砧锻造 / 改名 / 附魔书合成。
 * <p>
 * 26.x：{@code @EventBusSubscriber} 不再有 {@code bus} 属性，挂在哪条总线上由事件类型自动判断；
 * 本类只监听游戏总线事件，因此与旧版行为一致，无需其它改动。
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
