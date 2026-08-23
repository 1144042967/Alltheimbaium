package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化：注册各菜单类型的 GUI 与方块实体渲染器。
 */
@Mod.EventBusSubscriber(modid = "alltheimbaium", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(Registration.ETERNAL_SWORD_MENU.get(), EternalSwordScreen::new);
            MenuScreens.register(Registration.ETERNAL_TOTEM_MENU.get(), EternalTotemScreen::new);
            MenuScreens.register(Registration.LIQUID_FOUNTAIN_MENU.get(), LiquidFountainScreen::new);
            // 液体机镂空玻璃罐体：注册 cutout 渲染层（模型 JSON 的 render_type 兜底）
            ItemBlockRenderTypes.setRenderLayer(Registration.LIQUID_FOUNTAIN_BLOCK.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.LIQUID_FOUNTAIN_ENTITY.get(), LiquidFountainRenderer::new);
    }
}
