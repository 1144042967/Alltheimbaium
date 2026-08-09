package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化：注册永恒之剑配置界面到菜单类型。
 */
@Mod.EventBusSubscriber(modid = "alltheimbaium", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(Registration.ETERNAL_SWORD_MENU.get(), EternalSwordScreen::new);
            MenuScreens.register(Registration.ETERNAL_TOTEM_MENU.get(), EternalTotemScreen::new);
        });
    }
}
