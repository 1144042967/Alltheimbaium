package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.network.OpenEternalSwordGuiPacket;
import cn.sd.jrz.alltheimbaium.network.OpenEternalTotemGuiPacket;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * ALT+右击永恒之剑 / 永恒图腾时，取消默认行为并请求服务端打开配置界面。
 * <p>
 * 26.x：客户端 → 服务端的发送方法从 {@code PacketDistributor#sendToServer} 移到了
 * {@link ClientPacketDistributor#sendToServer}（服务端侧的发送方法仍在 {@code PacketDistributor}）。
 */
@EventBusSubscriber(modid = "alltheimbaium", value = Dist.CLIENT)
public class RightClickHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient() && isAltDown()) {
            if (event.getItemStack().is(Registration.ETERNAL_SWORD.get())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                ClientPacketDistributor.sendToServer(new OpenEternalSwordGuiPacket());
            } else if (event.getItemStack().is(Registration.ETERNAL_TOTEM.get())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                ClientPacketDistributor.sendToServer(new OpenEternalTotemGuiPacket());
            }
        }
    }

    /**
     * 是否按住 ALT 键（左右任一）。
     * 26.x：{@code Window#getWindow()}（GLFW 句柄）已删除，改走 {@code InputConstants.isKeyDown(Window, int)}
     * （内部就是 {@code GLFW.glfwGetKey(window.handle(), key)}）。
     */
    private static boolean isAltDown() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }
}
