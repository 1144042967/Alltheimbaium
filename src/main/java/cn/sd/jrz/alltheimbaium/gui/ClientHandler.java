package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 客户端初始化：注册各菜单类型的 GUI 与方块实体渲染器。
 * <p>
 * 26.x 两处变化：
 * <ul>
 *   <li>{@code @EventBusSubscriber} 不再有 {@code bus} 属性，挂在哪条总线上由事件类型自动判断；</li>
 *   <li>{@code ItemBlockRenderTypes} 已删除（镂空罐体由模型贴图的透明通道自动推断 cutout），
 *       原来在 {@code FMLClientSetupEvent} 里注册渲染层的整段客户端初始化随之删除。</li>
 * </ul>
 */
@EventBusSubscriber(modid = "alltheimbaium", value = Dist.CLIENT)
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

    /**
     * 方块实体渲染器。26.x 的 {@code registerBlockEntityRenderer} 要求同时给出渲染状态泛型，
     * 直接传方法引用即可由编译器推断。
     */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.LIQUID_FOUNTAIN_ENTITY.get(), LiquidFountainRenderer::new);
        event.registerBlockEntityRenderer(Registration.STORAGE_FOUNTAIN_ENTITY.get(), StorageFountainRenderer::new);
        event.registerBlockEntityRenderer(Registration.CLOCK_ENTITY.get(), ClockRenderer::new);
        event.registerBlockEntityRenderer(Registration.MOB_FARM_ENTITY.get(), MobFarmRenderer::new);
        event.registerBlockEntityRenderer(Registration.RESOURCE_FARM_ENTITY.get(), ResourceFarmRenderer::new);
    }
}
