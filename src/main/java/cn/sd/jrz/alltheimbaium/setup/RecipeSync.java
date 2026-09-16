package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端声明"哪些配方类型要同步给客户端"。
 * <p>
 * 26.x 起原版不再把完整配方表推给客户端，NeoForge 改用白名单式同步：只有在这里登记过的
 * {@link RecipeType}，其配方才会随登录与 {@code /reload} 发到客户端（见 {@link RecipeSource} 的说明）。
 * <p>
 * 本模组要让客户端看到配方的只有两处，都是本模组机器直接引用的外部配方：
 * <ul>
 *     <li>零刻熔炉：原版烧炼三件套（熔炉 / 高炉 / 烟熏）；</li>
 *     <li>零刻压印器：AE2 的压印机配方（未装 AE2 时该类型查不到，静默跳过）。</li>
 * </ul>
 * 标记物类的配方（资源农场 / 生物农场 / 存储方块制造机）来自配置而非配方表，不在此列。
 */
@EventBusSubscriber(modid = "alltheimbaium")
public class RecipeSync {
    private static final Logger log = LoggerFactory.getLogger(RecipeSync.class);

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        try {
            event.sendRecipes(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING);
            RecipeType<?> inscriber = RecipeSource.findAe2InscriberType(null);
            if (inscriber != null) {
                event.sendRecipes(inscriber);
            }
        } catch (Throwable e) {
            // 同步声明失败只影响客户端侧的 JEI / 帮助卡展示，机器本身（服务端）读的是本机配方表
            log.error("RecipeSync.onDatapackSync error", e);
        }
    }
}
