package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.SaplingBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用资源农场专用 serverconfig 白名单（标记物→资源，资源→产物）。
 * <p>
 * 每行格式 {@code 资源id=标记物token;…|物品id:权重;…}；标记物 token 可为物品id或 tag:标签id（tag 展开到其下所有物品）。
 * 惰性解析 + 缓存。产物权重尺度沿用 500≈1件/s@Lv1。
 * <p>
 * 除配置外，还会**内置扫描注册表里的所有树苗**（见 {@link #scanTrees}）：每个树苗自成一个资源，
 * 产出「对应原木 + 该树苗 + 对应树叶 + 特有掉落」。树苗的认领先于配置，因此配置里基于
 * {@code tag:minecraft:saplings} 的旧资源（默认白名单的 {@code wood} 行）不会截走树苗，
 * 而是退化成"以首个产物为标记物"的普通资源。
 */
public final class ResourceData {
    private static final Logger log = LoggerFactory.getLogger(ResourceData.class);

    /** 一条白名单产物 */
    public record Product(@Nonnull Item item, long weight) {
    }

    /** 一个资源：代表标记物 + 产物表 */
    public record Entry(@Nonnull Item marker, @Nonnull List<Product> products) {
    }

    // ==================== 树苗扫描参数 ====================

    /** 树苗识别兜底：模组树苗多半会加入 minecraft:saplings 标签，但也有只继承 SaplingBlock 的 */
    private static final TagKey<Item> SAPLINGS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "saplings"));

    /** 树干 id 后缀，按优先级逐个尝试（X_sapling → X_log / X_wood / X_stem…） */
    private static final String[] STEM_SUFFIXES = {"_log", "_wood", "_stem", "_hyphae", "_trunk"};

    /** 名称推不出树干时的显式例外：树苗 id → 树干 id（杜鹃长成橡树；红树林的繁殖体不叫 sapling） */
    private static final Map<String, String> STEM_OVERRIDES = Map.of(
            "minecraft:azalea", "minecraft:oak_log",
            "minecraft:flowering_azalea", "minecraft:oak_log",
            "minecraft:mangrove_propagule", "minecraft:mangrove_log");

    /** 树的特有掉落（键为树干 id，值为 {@code 物品id:权重}） */
    private static final Map<String, List<String>> STEM_EXTRAS = Map.of(
            "minecraft:oak_log", List.of("minecraft:apple:10"));

    private static final long TREE_LOG_WEIGHT = 500;
    private static final long TREE_SAPLING_WEIGHT = 200;
    private static final long TREE_LEAVES_WEIGHT = 150;

    /** 内置扫描出的一棵树：资源 id + 标记物（树苗）+ 产物 */
    private record TreeResource(@Nonnull String id, @Nonnull Item marker, @Nonnull List<Product> products) {
    }

    /** 推导出的树干：物品 + 它是按哪个名字查到的（用于再推树叶） */
    private record TreeStem(@Nonnull Item item, @Nonnull String base) {
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
            // ① 解析配置：先建产物表，标记物挂起——树苗扫描要抢先认领树苗
            List<String[]> pendingMarkers = new ArrayList<>();
            for (String line : Config.RESOURCE_WHITELIST.get()) {
                int pipe = line.indexOf('|');
                int eq = line.indexOf('=');
                if (eq <= 0 || pipe < eq) {
                    continue;
                }
                String id = line.substring(0, eq).trim();
                List<Product> products = new ArrayList<>();
                for (String token : line.substring(pipe + 1).split(";")) {
                    try {
                        if (token.isEmpty()) {
                            continue;
                        }
                        Product product = parseProduct(token);
                        if (product != null) {
                            products.add(product);
                        }
                    } catch (Throwable e) {
                        log.warn("ResourceData.parse product {} error", token, e);
                    }
                }
                prodById.put(id, products);
                pendingMarkers.add(new String[]{id, line.substring(eq + 1, pipe)});
            }
            // ② 内置树扫描：每个树苗自成一个资源
            List<TreeResource> trees = scanTrees();
            for (TreeResource tree : trees) {
                if (markers.putIfAbsent(tree.marker(), tree.id()) == null) {
                    repByResource.putIfAbsent(tree.id(), tree.marker());
                }
            }
            // ③ 认领配置里的标记物：树苗已被上一步占用，配置行只会拿到非树苗标记物
            for (String[] pending : pendingMarkers) {
                claimMarkers(pending[0], pending[1], markers, repByResource);
            }
            // ④ 树资源产物追加在配置资源之后，帮助卡里配置资源仍排在前面
            for (TreeResource tree : trees) {
                prodById.putIfAbsent(tree.id(), tree.products());
            }
            // ⑤ 组装条目。标记物被完全抢空的资源（例如默认白名单里靠 tag:minecraft:saplings 认领的
            //    wood 行——树苗已全部被上面的树扫描领走）用首个产物顶替成标记物，并**真的登记为标记物**：
            //    只当作展示值的话，帮助卡会显示一个玩家实际放不进标记槽的"标记物"。
            for (Map.Entry<String, List<Product>> e : prodById.entrySet()) {
                Item rep = repByResource.get(e.getKey());
                if (rep == null && !e.getValue().isEmpty()) {
                    Item candidate = e.getValue().get(0).item();
                    if (markers.putIfAbsent(candidate, e.getKey()) == null) {
                        rep = candidate;
                        repByResource.put(e.getKey(), candidate);
                    }
                    // 首个产物已是别的资源的标记物时 rep 保持 null：这条资源既不可标记也不进帮助卡
                    // （否则卡片上会显示一条"标记物 → 产物"其实是假的对照）
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
        // 先写数据、最后写"哨兵"字段：ensure() 只看 markerToResource，最后写它才能保证
        // 别的线程看到非 null 时其余字段已经就绪
        entries = list;
        productsById = prodById;
        helpRows = rows;
        markerToResource = markers;
    }

    /** 丢弃已构建的白名单（配置重载 / 换存档时调用），下次访问按新配置与新生效的模组重新扫描 */
    public static void invalidate() {
        synchronized (ResourceData.class) {
            markerToResource = null;
            entries = null;
            helpRows = null;
            // productsById 也要清：它是"资源 id → 产物"的正表，留着旧表会让 productsForId 在新一轮
            // 构建完成之前返回上一个存档的结果（build() 里是整体重建，不清就等于跨存档泄漏）
            productsById = new LinkedHashMap<>();
        }
    }

    /**
     * 认领某资源的标记物（物品 id / tag:标签id，多个用 ; 分隔）。
     * <p>
     * 已被别的资源占用的物品不动（first-wins），因此调用顺序即优先级。
     */
    private static void claimMarkers(@Nonnull String id, @Nonnull String markerPart,
                                     @Nonnull Map<Item, String> markers, @Nonnull Map<String, Item> repByResource) {
        for (String token : markerPart.split(";")) {
            try {
                if (token.isEmpty()) {
                    continue;
                }
                if (token.startsWith("tag:")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(token.substring(4).trim());
                    if (tagId == null) {
                        log.warn("ResourceData.parse marker {} error: 非法标签 id", token);
                        continue;
                    }
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                    for (Item item : BuiltInRegistries.ITEM) {
                        // 必须排除 air：它是物品注册表第 0 项，漏判会让任何 tag 都把空气登记成标记物，
                        // 进而让该资源的代表标记物变成空气（帮助卡里整行被当作空行跳过）
                        //noinspection deprecation
                        if (item == Items.AIR || !BuiltInRegistries.ITEM.wrapAsHolder(item).is(tag)) {
                            continue;
                        }
                        if (markers.putIfAbsent(item, id) == null) {
                            repByResource.putIfAbsent(id, item);
                        }
                    }
                } else {
                    Item item = lookupItem(token);
                    if (item != null && markers.putIfAbsent(item, id) == null) {
                        repByResource.putIfAbsent(id, item);
                    }
                }
            } catch (Throwable e) {
                log.warn("ResourceData.parse marker {} error", token, e);
            }
        }
    }

    // ==================== 树苗扫描 ====================

    /**
     * 扫描所有树苗，每棵生成一个资源：{@code 原木 + 该树苗 + 树叶 + 特有掉落}。
     * <p>
     * 树干与树叶按注册名约定推导（{@code X_sapling → X_log / X_leaves}），推不出树干的树苗只产出自身并记一条日志
     * （这类树苗仍可用 {@code resource_farm.whitelist} 手工补齐）。任何一棵树出错都只跳过它自己。
     */
    @Nonnull
    private static List<TreeResource> scanTrees() {
        List<TreeResource> found = new ArrayList<>();
        for (Item item : saplingCandidates()) {
            try {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                String base = saplingBase(key.getPath());
                List<Product> products = new ArrayList<>();
                TreeStem stem = treeStem(key, base);
                if (stem == null) {
                    log.warn("ResourceData 树扫描：{} 推不出对应树干，只产出自身", key);
                } else {
                    products.add(new Product(stem.item(), TREE_LOG_WEIGHT));
                }
                products.add(new Product(item, TREE_SAPLING_WEIGHT));
                Item leaves = treeLeaves(key.getNamespace(), base, stem);
                if (leaves != null && (stem == null || leaves != stem.item())) {
                    products.add(new Product(leaves, TREE_LEAVES_WEIGHT));
                }
                if (stem != null) {
                    String stemId = BuiltInRegistries.ITEM.getKey(stem.item()).toString();
                    for (String extra : STEM_EXTRAS.getOrDefault(stemId, List.of())) {
                        Product product = parseProduct(extra);
                        if (product != null) {
                            products.add(product);
                        }
                    }
                }
                found.add(new TreeResource("tree:" + key, item, products));
            } catch (Throwable e) {
                log.warn("ResourceData 树扫描 item {} error", item, e);
            }
        }
        // 按资源 id 排序，保证帮助卡分页顺序稳定（注册表遍历顺序取决于模组加载顺序）
        found.sort(Comparator.comparing(TreeResource::id));
        return found;
    }

    /** 候选树苗：原版三种树苗方块 + {@code minecraft:saplings} 标签成员（有的模组树苗只加标签、不继承 SaplingBlock） */
    @Nonnull
    private static Collection<Item> saplingCandidates() {
        Set<Item> candidates = new LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR && isSaplingBlock(item)) {
                candidates.add(item);
            }
        }
        BuiltInRegistries.ITEM.getTag(SAPLINGS_TAG).ifPresent(named -> {
            for (Holder<Item> holder : named) {
                Item item = holder.value();
                if (item != Items.AIR) {
                    candidates.add(item);
                }
            }
        });
        return candidates;
    }

    /** 是否为树苗类方块（原版三种：普通树苗 / 杜鹃 / 红树林繁殖体） */
    private static boolean isSaplingBlock(@Nonnull Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        return block instanceof SaplingBlock || block instanceof AzaleaBlock || block instanceof MangrovePropaguleBlock;
    }

    /** 树苗名去掉 {@code _sapling} 后缀即树名（红树林繁殖体等不叫 sapling 的按原样取） */
    @Nonnull
    private static String saplingBase(@Nonnull String path) {
        return path.endsWith("_sapling") ? path.substring(0, path.length() - "_sapling".length()) : path;
    }

    /** 推导树苗对应的树干（注册名约定 + 显式例外），推不出返回 null */
    @Nullable
    private static TreeStem treeStem(@Nonnull ResourceLocation saplingKey, @Nonnull String base) {
        String override = STEM_OVERRIDES.get(saplingKey.toString());
        if (override != null) {
            Item item = lookupItem(override);
            return item == null ? null : new TreeStem(item, stemBase(override));
        }
        for (String suffix : STEM_SUFFIXES) {
            Item item = lookupItem(saplingKey.getNamespace() + ":" + base + suffix);
            if (item != null) {
                return new TreeStem(item, base);
            }
        }
        return null;
    }

    /** 树干名去掉后缀即树名（{@code mangrove_log → mangrove}），用于再推树叶 */
    @Nonnull
    private static String stemBase(@Nonnull String stemId) {
        int colon = stemId.indexOf(':');
        String path = colon < 0 ? stemId : stemId.substring(colon + 1);
        for (String suffix : STEM_SUFFIXES) {
            if (path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
        }
        return path;
    }

    /**
     * 推导树叶：先按树苗名（杜鹃 → {@code azalea_leaves}），推不出再按树干名
     * （红树林繁殖体 → {@code mangrove_propagule_leaves} 不存在，回落到 {@code mangrove_leaves}）。
     */
    @Nullable
    private static Item treeLeaves(@Nonnull String namespace, @Nonnull String base, @Nullable TreeStem stem) {
        Item leaves = lookupItem(namespace + ":" + base + "_leaves");
        if (leaves == null && stem != null) {
            leaves = lookupItem(namespace + ":" + stem.base() + "_leaves");
        }
        return leaves;
    }

    // ==================== 解析工具 ====================
    // 注意：BuiltInRegistries 的 ITEM / ENTITY_TYPE 都被 Forge 包成了"带默认值"的注册表——
    // get() 查不到时返回默认值（AIR / PIG）而不是 null，所以判空写 `!= null` 恒真、永远拦不住拼错的 id。
    // 一律改用 containsKey 判定"是否注册"，再取默认值兜底。

    /** 物品 id 解析：非法 id、未注册或 air 一律返回 null */
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
    private static Product parseProduct(@Nonnull String token) {
        String text = token.trim();
        int last = text.lastIndexOf(':');
        String idPart = text;
        long weight = 1;
        if (last > 0) {
            // 只有尾段是纯数字才当权重，否则整串都是物品 id（"minecraft:oak_log" 这种省略权重的写法）
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
