package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.Holder;
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
            Map<EntityType<?>, Item> map = eggCache;
            if (map == null) {
                map = buildEggCache();
                eggCache = map;
            }
            Item item = map.get(type);
            if (item != null) {
                return item;
            }
            // 兜底：档位表里没建到就直接问原版反查表（26.x 返回 Optional<Holder<Item>>）
            return SpawnEggItem.byId(type).map(Holder::value).orElse(null);
        } catch (Throwable e) {
            log.error("MobFarmCatalog.spawnEggOf error", e);
        }
        return null;
    }

    /**
     * 扫描物品注册表建立"实体类型 → 刷怪蛋"表。
     * <p>
     * 26.x 的 {@code SpawnEggItem#getType(ItemStack)} 改成了静态方法，实体类型从
     * {@code ENTITY_DATA} 组件读（刷怪蛋由 {@code Item.Properties#spawnEgg} 在注册时写入），
     * 所以这里仍要读该物品的默认实例——默认实例带着原型物品的组件；且**每件必须单独 try**——
     * 否则一个蛋抛异常就让整张表永远建不出来，表现为所有刷怪蛋都识别不了。
     */
    @Nonnull
    private static Map<EntityType<?>, Item> buildEggCache() {
        Map<EntityType<?>, Item> map = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof SpawnEggItem)) {
                continue;
            }
            try {
                EntityType<?> eggType = SpawnEggItem.getType(item.getDefaultInstance());
                if (eggType != null) {
                    map.putIfAbsent(eggType, item);
                }
            } catch (Throwable e) {
                log.warn("MobFarmCatalog: 读取刷怪蛋 {} 的实体类型失败，跳过",
                        BuiltInRegistries.ITEM.getKey(item), e);
            }
        }
        return map;
    }

    /** 物品是否为某实体类型的刷怪蛋 */
    public static boolean isSpawnEggFor(@Nonnull ItemStack stack, @Nonnull EntityType<?> type) {
        if (!(stack.getItem() instanceof SpawnEggItem)) {
            return false;
        }
        try {
            // 26.x：原版提供了等价的静态判定 spawnsEntity(stack, type)
            return SpawnEggItem.spawnsEntity(stack, type);
        } catch (Throwable e) {
            log.warn("MobFarmCatalog.isSpawnEggFor error", e);
        }
        return false;
    }
}
