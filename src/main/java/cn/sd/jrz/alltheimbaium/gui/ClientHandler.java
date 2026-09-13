package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化：注册各菜单类型的 GUI 与方块实体渲染器。
 */
@EventBusSubscriber(modid = "alltheimbaium", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
    /**
     * 菜单 → 界面绑定。1.21.1 里 {@code MenuScreens.register} 已改为私有，
     * 必须走 NeoForge 的 {@link RegisterMenuScreensEvent}（mod 总线）。
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.ETERNAL_SWORD_MENU.get(), EternalSwordScreen::new);
        event.register(Registration.ETERNAL_TOTEM_MENU.get(), EternalTotemScreen::new);
        event.register(Registration.LIQUID_FOUNTAIN_MENU.get(), LiquidFountainScreen::new);
        event.register(Registration.AUTO_FARMLAND_MENU.get(), AutoFarmlandScreen::new);
        event.register(Registration.STORAGE_FOUNTAIN_MENU.get(), StorageFountainScreen::new);
        event.register(Registration.INSTANT_FURNACE_MENU.get(), InstantFurnaceScreen::new);
        event.register(Registration.INSTANT_INSCRIBER_MENU.get(), InstantInscriberScreen::new);
        event.register(Registration.CLOCK_MENU.get(), ClockScreen::new);
        event.register(Registration.CREATIVE_TRANSMUTER_MENU.get(), CreativeTransmuterScreen::new);
        event.register(Registration.PLATFORM_MENU.get(), PlatformScreen::new);
        event.register(Registration.SUPPLY_CRATE_MENU.get(), SupplyCrateScreen::new);
        event.register(Registration.MOB_FARM_MENU.get(), MobFarmScreen::new);
        event.register(Registration.RESOURCE_FARM_MENU.get(), ResourceFarmScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 液体机镂空玻璃罐体：注册 cutout 渲染层（模型 JSON 的 render_type 兜底）
            ItemBlockRenderTypes.setRenderLayer(Registration.LIQUID_FOUNTAIN_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(Registration.MOB_FARM_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(Registration.RESOURCE_FARM_BLOCK.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.LIQUID_FOUNTAIN_ENTITY.get(), LiquidFountainRenderer::new);
        event.registerBlockEntityRenderer(Registration.STORAGE_FOUNTAIN_ENTITY.get(), StorageFountainRenderer::new);
        event.registerBlockEntityRenderer(Registration.CLOCK_ENTITY.get(), ClockRenderer::new);
        event.registerBlockEntityRenderer(Registration.MOB_FARM_ENTITY.get(), MobFarmRenderer::new);
        event.registerBlockEntityRenderer(Registration.RESOURCE_FARM_ENTITY.get(), ResourceFarmRenderer::new);
    }
}
