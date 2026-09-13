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
        // 先写数据、最后写"哨兵"字段：ensure() 只看 markersCache，最后写它才能保证
        // 别的线程看到非 null 时另两个字段已经就绪
        productsCache = products;
        knownCache = known;
        markersCache = markers;
    }

    /** 丢弃已构建的白名单（配置重载 / 换存档时调用），下次访问按新配置重新解析 */
    public static void invalidate() {
        synchronized (MobFarmWhitelist.class) {
            markersCache = null;
            productsCache = null;
            knownCache = null;
        }
    }

    private static void parseSignature(String line, Map<Item, EntityType<?>> markers, Set<EntityType<?>> known) {
        // 单行失败只丢这一行。此前实体 id 的构造在 try 之外，一行写错就会中断整个 build：
        // 后面的行全都不再解析（产物表整表为空），而半成品的缓存已写入、之后永不重建。
        try {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                return;
            }
            String typeId = line.substring(0, eq).trim();
            EntityType<?> type = lookupEntity(typeId);
            if (type == null) {
                log.warn("MobFarmWhitelist 特征物白名单: 找不到实体 {}", typeId);
                return;
            }
            known.add(type);
            for (String token : line.substring(eq + 1).split(";")) {
                try {
                    if (token == null || token.isEmpty()) {
                        continue;
                    }
                    if (token.startsWith("tag:")) {
                        ResourceLocation tagId = ResourceLocation.tryParse(token.substring(4).trim());
                        if (tagId == null) {
                            log.warn("MobFarmWhitelist.parseSignature: 非法标签 id {}", token);
                            continue;
                        }
                        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                        for (Item item : BuiltInRegistries.ITEM) {
                            if (item != Items.AIR && BuiltInRegistries.ITEM.wrapAsHolder(item).is(tag)) {
                                markers.putIfAbsent(item, type);
                            }
                        }
                    } else {
                        Item item = lookupItem(token);
                        if (item != null) {
                            markers.putIfAbsent(item, type);
                        }
                    }
                } catch (Throwable e) {
                    log.warn("MobFarmWhitelist.parseSignature token {} error", token, e);
                }
            }
        } catch (Throwable e) {
            log.warn("MobFarmWhitelist.parseSignature line {} error", line, e);
        }
    }

    private static void parseProduct(String line, Map<EntityType<?>, List<Product>> products, Set<EntityType<?>> known) {
        try {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                return;
            }
            String typeId = line.substring(0, eq).trim();
            EntityType<?> type = lookupEntity(typeId);
            if (type == null) {
                log.warn("MobFarmWhitelist 产物白名单: 找不到实体 {}", typeId);
                return;
            }
            known.add(type);
            List<Product> list = products.computeIfAbsent(type, k -> new ArrayList<>());
            for (String token : line.substring(eq + 1).split(";")) {
                try {
                    if (token == null || token.isEmpty()) {
                        continue;
                    }
                    Product product = parseProductToken(token);
                    if (product != null) {
                        list.add(product);
                    }
                } catch (Throwable e) {
                    log.warn("MobFarmWhitelist.parseProduct token {} error", token, e);
                }
            }
        } catch (Throwable e) {
            log.warn("MobFarmWhitelist.parseProduct line {} error", line, e);
        }
    }

    // ==================== 解析工具 ====================
    // 注意：ITEM / ENTITY_TYPE 都被 Forge 包成了"带默认值"的注册表——get() 查不到时返回默认值
    // （AIR / PIG）而不是 null，所以 `!= null` 恒真、永远拦不住拼错的 id（拼错会静默变成猪/空气）。
    // 一律用 containsKey 判定"是否注册"。

    /** 实体 id 解析：非法 id 或未注册返回 null */
    @Nullable
    private static EntityType<?> lookupEntity(@Nonnull String id) {
        ResourceLocation key = ResourceLocation.tryParse(id.trim());
        if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.get(key);
    }

    /** 物品 id 解析：非法 id、未注册或 air 一律返回 null（生物没有对应刷怪蛋时按此跳过） */
    @Nullable
    private static Item lookupItem(@Nonnull String id) {
        ResourceLocation key = ResourceLocation.tryParse(id.trim());
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        return item == null || item == Items.AIR ? null : item;
    }

    /** 解析一条 {@code 物品id:权重} 产物片段；非法或未注册返回 null（省略权重按 1 计） */
    @Nullable
    private static Product parseProductToken(@Nonnull String token) {
        String text = token.trim();
        int last = text.lastIndexOf(':');
        String idPart = text;
        long weight = 1;
        if (last > 0) {
            // 只有尾段是纯数字才当权重，否则整串都是物品 id（"minecraft:honeycomb" 这种省略权重的写法）
            Long parsed = parseLong(text.substring(last + 1).trim());
            if (parsed != null) {
                idPart = text.substring(0, last);
                weight = parsed;
            }
        }
        Item item = lookupItem(idPart);
        return item == null ? null : new Product(item, weight);
    }

    /** 纯数字串转 long，非数字返回 null */
    @Nullable
    private static Long parseLong(@Nonnull String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
