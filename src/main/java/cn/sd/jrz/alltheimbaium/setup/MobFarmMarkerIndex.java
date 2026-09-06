package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
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
 * 生物农场动态"标记物/产物"知识表（服务端惰性构建 + 缓存，仅服务端）。
 * <p>
 * 内容（全部来自生物农场专用白名单 {@link MobFarmWhitelist} + 击杀掉落补充，不再依赖旧农场配方/DataConfig）：
 * - 标记表 {@code Item → EntityType}：先以"特征物白名单（含 tag 展开）"占位（共享物品 first-wins），
 *   再对每个可收容实体廉价采样击杀战利品表，把未被占用的掉落物绑定到第一个检出的生物。
 * - 产物行：为每个可收容实体记录 {@code 白名单产物 ∪ 击杀采样掉落 ∪ 刷怪蛋}，供 GUI "生物 → 掉落物" 展示。
 * 构建结果排序后转成 int[] 编码，随开屏数据发给客户端。
 */
public final class MobFarmMarkerIndex {
    private static final Logger log = LoggerFactory.getLogger(MobFarmMarkerIndex.class);

    /** Item → 收容生物 的标记表 */
    private static volatile Map<Item, EntityType<?>> TABLE;
    /** 标记对 {itemId, typeId, …}（按 生物注册名 → 物品注册名 排序） */
    private static volatile int[] MARKER_PAIRS;
    /** 产物行 {typeId, itemId…} 数组（行序按生物注册名，行内物品按注册名） */
    private static volatile int[][] PRODUCT_ROWS;

    private MobFarmMarkerIndex() {
    }

    /** 惰性构建一次（服务端线程）。线程安全双重检查；构建失败留空下次再试。 */
    public static void ensureBuilt(@Nonnull ServerLevel level) {
        if (TABLE != null) {
            return;
        }
        synchronized (MobFarmMarkerIndex.class) {
            if (TABLE != null) {
                return;
            }
            try {
                build(level);
            } catch (Throwable e) {
                log.error("MobFarmMarkerIndex.ensureBuilt error", e);
            }
        }
    }

    /** 标记对（先确保已构建） */
    public static int[] pairs(@Nonnull ServerLevel level) {
        ensureBuilt(level);
        int[] pairs = MARKER_PAIRS;
        return pairs == null ? new int[0] : pairs;
    }

    /** 产物行（先确保已构建） */
    public static int[][] productRows(@Nonnull ServerLevel level) {
        ensureBuilt(level);
        int[][] rows = PRODUCT_ROWS;
        return rows == null ? new int[0][] : rows;
    }

    /** 把标记对 + 产物行编码进开屏 extraData（先 pairs，再 rows） */
    public static void writeToBuf(@Nonnull FriendlyByteBuf buf, @Nonnull ServerLevel level) {
        int[] pairs = pairs(level);
        buf.writeVarInt(pairs.length);
        for (int v : pairs) {
            buf.writeVarInt(v);
        }
        int[][] rows = productRows(level);
        buf.writeVarInt(rows.length);
        for (int[] row : rows) {
            buf.writeVarInt(row[0]);            // 生物 typeId
            buf.writeVarInt(row.length - 1);    // 物品个数
            for (int i = 1; i < row.length; i++) {
                buf.writeVarInt(row[i]);        // 物品 itemId
            }
        }
    }

    /** 按物品查它可收容的生物（白名单标记物 + 动态击杀掉落）。表未构建返回 null。 */
    @Nullable
    public static EntityType<?> lookup(@Nonnull Item item) {
        Map<Item, EntityType<?>> table = TABLE;
        return table == null ? null : table.get(item);
    }

    private static void build(@Nonnull ServerLevel level) {
        // 1) 标记表先以 特征物白名单（含 tag 展开）占位
        Map<Item, EntityType<?>> markers = new LinkedHashMap<>(MobFarmWhitelist.signatureMarkers());
        // 2) 产物行：先给每个白名单生物预置"白名单产物"
        Map<EntityType<?>, Set<Item>> bioItems = new LinkedHashMap<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (MobFarmWhitelist.known(type)) {
                bioItems.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(MobFarmWhitelist.productItemsFor(type));
            }
        }
        // 3) 按注册序扫描所有"可收容"实体（白名单 或 有刷怪蛋），击杀采样补标记与产物
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            try {
                if (!MobFarmWhitelist.known(type) && MobFarmCatalog.spawnEggOf(type) == null) {
                    continue; // 无法收容/非生物实体不参与
                }
                Set<Item> set = bioItems.computeIfAbsent(type, k -> new LinkedHashSet<>());
                for (Item item : KillLootEstimator.possibleItems(level, type)) {
                    markers.putIfAbsent(item, type); // 每个掉落物只标记第一个检出的生物
                    set.add(item);
                }
            } catch (Throwable e) {
                log.warn("MobFarmMarkerIndex.build skip type {} ", type, e);
            }
        }
        // 4) 每个可收容生物补它的刷怪蛋（作为产物之一）
        for (EntityType<?> type : new ArrayList<>(bioItems.keySet())) {
            Item egg = MobFarmCatalog.spawnEggOf(type);
            if (egg != null) {
                bioItems.get(type).add(egg);
            }
        }
        // 5) 标记对 与 产物行：均按 生物注册名 → 物品注册名 排序，保证双端一致
        MARKER_PAIRS = encodeMarkerPairs(markers);
        PRODUCT_ROWS = encodeProductRows(bioItems);
        TABLE = markers;
    }

    private static int[] encodeMarkerPairs(Map<Item, EntityType<?>> markers) {
        List<Map.Entry<Item, EntityType<?>>> list = new ArrayList<>(markers.entrySet());
        list.sort((a, b) -> {
            int cmp = BuiltInRegistries.ENTITY_TYPE.getKey(a.getValue()).toString()
                    .compareTo(BuiltInRegistries.ENTITY_TYPE.getKey(b.getValue()).toString());
            if (cmp != 0) {
                return cmp;
            }
            return BuiltInRegistries.ITEM.getKey(a.getKey()).toString()
                    .compareTo(BuiltInRegistries.ITEM.getKey(b.getKey()).toString());
        });
        int[] pairs = new int[list.size() * 2];
        for (int i = 0; i < list.size(); i++) {
            Map.Entry<Item, EntityType<?>> e = list.get(i);
            //noinspection deprecation
            pairs[i * 2] = BuiltInRegistries.ITEM.getId(e.getKey());
            //noinspection deprecation
            pairs[i * 2 + 1] = BuiltInRegistries.ENTITY_TYPE.getId(e.getValue());
        }
        return pairs;
    }

    private static int[][] encodeProductRows(Map<EntityType<?>, Set<Item>> bioItems) {
        List<Map.Entry<EntityType<?>, Set<Item>>> list = new ArrayList<>(bioItems.entrySet());
        list.sort((a, b) -> BuiltInRegistries.ENTITY_TYPE.getKey(a.getKey()).toString()
                .compareTo(BuiltInRegistries.ENTITY_TYPE.getKey(b.getKey()).toString()));
        List<int[]> rows = new ArrayList<>();
        for (Map.Entry<EntityType<?>, Set<Item>> entry : list) {
            List<Item> items = new ArrayList<>(entry.getValue());
            items.sort((a, b) -> BuiltInRegistries.ITEM.getKey(a).toString()
                    .compareTo(BuiltInRegistries.ITEM.getKey(b).toString()));
            int[] row = new int[items.size() + 1];
            //noinspection deprecation
            row[0] = BuiltInRegistries.ENTITY_TYPE.getId(entry.getKey());
            for (int i = 0; i < items.size(); i++) {
                //noinspection deprecation
                row[i + 1] = BuiltInRegistries.ITEM.getId(items.get(i));
            }
            rows.add(row);
        }
        return rows.toArray(new int[0][]);
    }
}
