package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用资源农场专用 serverconfig 白名单（标记物→资源，资源→产物）。
 * <p>
 * 每行格式 {@code 资源id=标记物token;…|物品id:权重;…}；标记物 token 可为物品id或 tag:标签id（tag 展开到其下所有物品）。
 * 惰性解析 + 缓存。产物权重尺度沿用 500≈1件/s@Lv1。
 */
public final class ResourceData {
    private static final Logger log = LoggerFactory.getLogger(ResourceData.class);

    /** 一条白名单产物 */
    public record Product(@Nonnull Item item, long weight) {
    }

    /** 一个资源：代表标记物 + 产物表 */
    public record Entry(@Nonnull Item marker, @Nonnull List<Product> products) {
    }

    // 缓存（惰性构建一次）
    private static volatile Map<Item, String> markerToResource;
    private static volatile List<Entry> entries;
    private static volatile int[][] helpRows;

    private ResourceData() {
    }

    // ==================== 查询 ====================

    /** 该物品是否为某资源的标记物 */
    public static boolean isMarker(@Nonnull Item item) {
        ensure();
        return markerToResource.containsKey(item);
    }

    /** 标记物所属的资源 id（非标记物返回 null） */
    @Nullable
    public static String resourceOf(@Nonnull Item item) {
        ensure();
        return markerToResource.get(item);
    }

    /** 某标记物对应资源的白名单产物（带权重）；非标记物返回空列表 */
    @Nonnull
    public static List<Product> productsForMarker(@Nonnull Item marker) {
        ensure();
        String id = markerToResource.get(marker);
        return productsForId(id);
    }

    /** 某资源 id 的白名单产物（带权重） */
    @Nonnull
    public static List<Product> productsForId(@Nullable String resourceId) {
        ensure();
        if (resourceId == null) {
            return List.of();
        }
        // id 不单独缓存，遍历 entries 找 markerToResource 逆推不可行，故单独建 id→products
        List<Product> list = productsById.get(resourceId);
        return list == null ? List.of() : list;
    }

    // 缓存 map（id→产物）伴随构建
    private static Map<String, List<Product>> productsById = new LinkedHashMap<>();

    // ==================== 帮助数据（GUI "?"） ====================

    /** 资源条目数（一个标记物代表 + 其产物） */
    public static int entryCount() {
        ensure();
        return entries.size();
    }

    /** 第 i 个资源：代表标记物（首个具体标记物） */
    @Nullable
    public static Item entryMarker(int i) {
        ensure();
        return (i < 0 || i >= entries.size()) ? null : entries.get(i).marker();
    }

    /** 第 i 个资源的产物表 */
    @Nonnull
    public static List<Product> entryProducts(int i) {
        ensure();
        return (i < 0 || i >= entries.size()) ? List.of() : entries.get(i).products();
    }

    /** 帮助行编码 {markerId, itemId…}（随开屏数据发给客户端） */
    public static int[][] helpRows() {
        ensure();
        return helpRows;
    }

    // ==================== 惰性解析 ====================

    private static void ensure() {
        if (markerToResource != null) {
            return;
        }
        synchronized (ResourceData.class) {
            if (markerToResource != null) {
                return;
            }
            build();
        }
    }

    private static void build() {
        Map<Item, String> markers = new LinkedHashMap<>();
        List<Entry> list = new ArrayList<>();
        Map<String, List<Product>> prodById = new LinkedHashMap<>();
        Map<String, Item> repByResource = new LinkedHashMap<>();
        try {
            for (String line : Config.RESOURCE_WHITELIST.get()) {
                int pipe = line.indexOf('|');
                int eq = line.indexOf('=');
                if (eq <= 0 || pipe < eq) {
                    continue;
                }
                String id = line.substring(0, eq).trim();
                String markerPart = line.substring(eq + 1, pipe);
                String productPart = line.substring(pipe + 1);
                List<Product> products = new ArrayList<>();
                for (String token : productPart.split(";")) {
                    try {
                        if (token.isEmpty()) {
                            continue;
                        }
                        int last = token.lastIndexOf(':');
                        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(token.substring(0, last)));
                        if (item == null) {
                            continue;
                        }
                        long weight;
                        try {
                            weight = Long.parseLong(token.substring(last + 1));
                        } catch (NumberFormatException e) {
                            weight = 1;
                        }
                        products.add(new Product(item, weight));
                    } catch (Throwable e) {
                        log.warn("ResourceData.parse product {} error", token, e);
                    }
                }
                prodById.put(id, products);
                // 记录该资源的代表标记物（首个具体物）
                for (String token : markerPart.split(";")) {
                    try {
                        if (token.isEmpty()) {
                            continue;
                        }
                        if (token.startsWith("tag:")) {
                            TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(token.substring(4)));
                            for (Item item : BuiltInRegistries.ITEM) {
                                if (item == Items.AIR || BuiltInRegistries.ITEM.wrapAsHolder(item).is(tag)) {
                                    if (markers.putIfAbsent(item, id) == null) {
                                        repByResource.putIfAbsent(id, item);
                                    }
                                }
                            }
                        } else {
                            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(token));
                            if (item != null && markers.putIfAbsent(item, id) == null) {
                                repByResource.putIfAbsent(id, item);
                            }
                        }
                    } catch (Throwable e) {
                        log.warn("ResourceData.parse marker {} error", token, e);
                    }
                }
            }
            for (Map.Entry<String, List<Product>> e : prodById.entrySet()) {
                Item rep = repByResource.get(e.getKey());
                if (rep == null && !e.getValue().isEmpty()) {
                    rep = e.getValue().get(0).item();
                }
                if (rep != null) {
                    list.add(new Entry(rep, e.getValue()));
                }
            }
        } catch (Throwable e) {
            log.error("ResourceData.build error", e);
        }
        // helpRows 编码
        int[][] rows = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            Entry entry = list.get(i);
            int[] row = new int[entry.products().size() + 1];
            //noinspection deprecation
            row[0] = BuiltInRegistries.ITEM.getId(entry.marker());
            for (int k = 0; k < entry.products().size(); k++) {
                //noinspection deprecation
                row[k + 1] = BuiltInRegistries.ITEM.getId(entry.products().get(k).item());
            }
            rows[i] = row;
        }
        markerToResource = markers;
        entries = list;
        productsById = prodById;
        helpRows = rows;
    }
}
