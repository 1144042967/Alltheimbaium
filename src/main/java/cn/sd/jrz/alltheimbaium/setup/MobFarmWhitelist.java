package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生物农场专用 serverconfig 白名单（特征物 / 产物），与旧专属农场的配方、DataConfig、Config(FARM_*_PRODUCTS) 解耦。
 * <p>
 * - 特征物白名单 {@link Config#MOB_FARM_SIGNATURES}：每行 "实体id=标记物token;…"，token 为物品id或 tag:标签id；
 *   展开成 物品→收容生物（共享物品按配置行序 first-wins），供标记槽收容。
 * - 产物白名单 {@link Config#MOB_FARM_PRODUCTS}：每行 "实体id=物品id:权重;…"，供收容后机器稳定产出的白名单产物。
 * 懒解析 + 缓存；解析失败仅 warn 跳过。
 */
public final class MobFarmWhitelist {
    private static final Logger log = LoggerFactory.getLogger(MobFarmWhitelist.class);

    /** 一条白名单产物 */
    public record Product(@Nonnull Item item, long weight) {
    }

    // 缓存（惰性构建一次，服务端线程）
    private static volatile Map<Item, EntityType<?>> markersCache;
    private static volatile Map<EntityType<?>, List<Product>> productsCache;
    private static volatile Set<EntityType<?>> knownCache;

    private MobFarmWhitelist() {
    }

    // ==================== 查询 ====================

    /** 该生物是否出现在任一白名单行（代替旧 33 白名单判定） */
    public static boolean known(@Nonnull EntityType<?> type) {
        ensure();
        return knownCache.contains(type);
    }

    /** 物品是否为某白名单特征物；是则返回对应收容生物（含 tag 展开） */
    @Nullable
    public static EntityType<?> markerTypeOf(@Nonnull Item item) {
        ensure();
        return markersCache.get(item);
    }

    /** 完整特征物表 物品→生物（含 tag 展开），勿修改返回值 */
    @Nonnull
    public static Map<Item, EntityType<?>> signatureMarkers() {
        ensure();
        return markersCache;
    }

    /** 某生物的白名单产物（含权重），没有返回空列表 */
    @Nonnull
    public static List<Product> productsFor(@Nonnull EntityType<?> type) {
        ensure();
        List<Product> list = productsCache.get(type);
        return list == null ? List.of() : list;
    }

    /** 某生物的白名单产物物品种（去重按序），没有返回空列表 */
    @Nonnull
    public static List<Item> productItemsFor(@Nonnull EntityType<?> type) {
        List<Item> items = new ArrayList<>();
        for (Product product : productsFor(type)) {
            if (!items.contains(product.item())) {
                items.add(product.item());
            }
        }
        return items;
    }

    // ==================== 惰性解析 ====================

    private static void ensure() {
        if (markersCache != null) {
            return;
        }
        synchronized (MobFarmWhitelist.class) {
            if (markersCache != null) {
                return;
            }
            build();
        }
    }

    private static void build() {
        Map<Item, EntityType<?>> markers = new LinkedHashMap<>();
        Map<EntityType<?>, List<Product>> products = new LinkedHashMap<>();
        Set<EntityType<?>> known = new LinkedHashSet<>();
        try {
            for (String line : Config.MOB_FARM_SIGNATURES.get()) {
                parseSignature(line, markers, known);
            }
            for (String line : Config.MOB_FARM_PRODUCTS.get()) {
                parseProduct(line, products, known);
            }
        } catch (Throwable e) {
            log.error("MobFarmWhitelist.build error", e);
        }
        markersCache = markers;
        productsCache = products;
        knownCache = known;
    }

    private static void parseSignature(String line, Map<Item, EntityType<?>> markers, Set<EntityType<?>> known) {
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(line.substring(0, eq)));
        if (type == null) {
            log.warn("MobFarmWhitelist 特征物白名单: 找不到实体 {}", line.substring(0, eq));
            return;
        }
        known.add(type);
        for (String token : line.substring(eq + 1).split(";")) {
            try {
                if (token == null || token.isEmpty()) {
                    continue;
                }
                if (token.startsWith("tag:")) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(token.substring(4)));
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (item != Items.AIR && BuiltInRegistries.ITEM.wrapAsHolder(item).is(tag)) {
                            markers.putIfAbsent(item, type);
                        }
                    }
                } else {
                    Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(token));
                    if (item != null) {
                        markers.putIfAbsent(item, type);
                    }
                }
            } catch (Throwable e) {
                log.warn("MobFarmWhitelist.parseSignature token {} error", token, e);
            }
        }
    }

    private static void parseProduct(String line, Map<EntityType<?>, List<Product>> products, Set<EntityType<?>> known) {
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(line.substring(0, eq)));
        if (type == null) {
            log.warn("MobFarmWhitelist 产物白名单: 找不到实体 {}", line.substring(0, eq));
            return;
        }
        known.add(type);
        List<Product> list = products.computeIfAbsent(type, k -> new ArrayList<>());
        for (String token : line.substring(eq + 1).split(";")) {
            try {
                if (token == null || token.isEmpty()) {
                    continue;
                }
                int lastColon = token.lastIndexOf(':');
                if (lastColon <= 0) {
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(token.substring(0, lastColon)));
                if (item == null) {
                    continue;
                }
                long weight;
                try {
                    weight = Long.parseLong(token.substring(lastColon + 1));
                } catch (NumberFormatException e) {
                    weight = 1;
                }
                list.add(new Product(item, weight));
            } catch (Throwable e) {
                log.warn("MobFarmWhitelist.parseProduct token {} error", token, e);
            }
        }
    }
}
