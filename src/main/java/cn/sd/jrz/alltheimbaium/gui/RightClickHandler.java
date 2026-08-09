package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.network.Network;
import cn.sd.jrz.alltheimbaium.network.OpenEternalSwordGuiPacket;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * ALT+右击永恒之剑时，取消攻击并请求服务端打开配置界面。
 */
@Mod.EventBusSubscriber(modid = "alltheimbaium", value = Dist.CLIENT)
public class RightClickHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient()
                && event.getItemStack().is(Registration.ETERNAL_SWORD.get())
                && isAltDown()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            Network.CHANNEL.sendToServer(new OpenEternalSwordGuiPacket());
        }
    }

    /** 是否按住 ALT 键（左右任一） */
    private static boolean isAltDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }
}
