package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 生物农场刷怪蛋工具类。
 * <p>
 * "哪些生物可收容/用什么特征物标记/产物"一律取自生物农场专用 serverconfig 白名单
 * {@link MobFarmWhitelist}，与旧专属农场的配方、DataConfig、Config(FARM_*_PRODUCTS) 解耦。
 */
public final class MobFarmCatalog {
    private static final Logger log = LoggerFactory.getLogger(MobFarmCatalog.class);

    // 刷怪蛋缓存：懒扫描一次（服务端收容/建产物表时才调用）
    private static Map<EntityType<?>, Item> eggCache = null;

    private MobFarmCatalog() {
    }

    /** 该生物是否在生物农场专用白名单中出现（可稳定收容并有白名单数据） */
    public static boolean isWhitelisted(@Nonnull EntityType<?> type) {
        return MobFarmWhitelist.known(type);
    }

    /** 生成该生物对应的刷怪蛋物品（没有则返回 null）。懒扫描一次物品注册表。 */
    @Nullable
    public static Item spawnEggOf(@Nonnull EntityType<?> type) {
        try {
            if (eggCache == null) {
                Map<EntityType<?>, Item> map = new HashMap<>();
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item instanceof SpawnEggItem egg) {
                        //noinspection deprecation
                        EntityType<?> eggType = egg.getType(null);
                        if (eggType != null) {
                            map.put(eggType, item);
                        }
                    }
                }
                eggCache = map;
            }
            return eggCache.get(type);
        } catch (Throwable e) {
            log.error("MobFarmCatalog.spawnEggOf error", e);
        }
        return null;
    }

    /** 物品是否为某实体类型的刷怪蛋 */
    public static boolean isSpawnEggFor(@Nonnull ItemStack stack, @Nonnull EntityType<?> type) {
        if (!(stack.getItem() instanceof SpawnEggItem egg)) {
            return false;
        }
        //noinspection deprecation
        return egg.getType(null) == type;
    }
}
