package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 农场数据配置。产物从配置文件读取，可选合并对应实体的战利品表。
 */
public class DataConfig {

    private final ForgeConfigSpec.ConfigValue<List<? extends String>> productsConfig;
    private final Supplier<BlockEntityType<?>> entityTypeSupplier;
    private final ResourceLocation entityLootTableId;
    private List<ItemProduct> cachedResolved;

    public DataConfig(ForgeConfigSpec.ConfigValue<List<? extends String>> productsConfig, Supplier<BlockEntityType<?>> entityTypeSupplier, ResourceLocation entityLootTableId) {
        this.productsConfig = productsConfig;
        this.entityTypeSupplier = entityTypeSupplier;
        this.entityLootTableId = entityLootTableId;
    }

    public BlockEntityType<?> getType() {
        return entityTypeSupplier.get();
    }

    /**
     * 获取解析后的产物列表。先读取配置产物，若开启了战利品表合并则追加战利品表中不重复的物品。
     */
    public List<ItemProduct> getProductList() {
        return parseProducts(productsConfig.get());
    }

    /**
     * 获取解析后的产物列表，可选合并实体战利品表（仅首次调用时合并，之后缓存）。
     */
    public List<ItemProduct> getResolvedProductList(Level level) {
        if (cachedResolved != null) return cachedResolved;

        List<ItemProduct> result = parseProducts(productsConfig.get());

        if (Config.FARM_USE_LOOT_TABLE.get() && entityLootTableId != null && level.getServer() != null) {
            Set<Item> existingItems = new HashSet<>();
            for (ItemProduct p : result) {
                existingItems.add(p.item);
            }

            // 查询实体战利品表
            LootTable lootTable = level.getServer().getLootData().getLootTable(entityLootTableId);
            LootParams lootParams = new LootParams.Builder(level.getServer().overworld())
                    .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                    .create(LootContextParamSets.EMPTY);
            List<ItemStack> lootDrops = lootTable.getRandomItems(lootParams);

            for (ItemStack drop : lootDrops) {
                if (!drop.isEmpty() && !existingItems.contains(drop.getItem())) {
                    result.add(new ItemProduct(drop.getItem(), getDefaultCount(drop.getItem())));
                    existingItems.add(drop.getItem());
                }
            }
        }

        cachedResolved = result;
        return result;
    }

    /**
     * 解析产物配置字符串列表，格式 "namespace:item:count"
     */
    private static List<ItemProduct> parseProducts(List<? extends String> configList) {
        List<ItemProduct> result = new ArrayList<>();
        for (String entry : configList) {
            String[] parts = entry.split(":");
            if (parts.length >= 3) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null) {
                    try {
                        long count = Long.parseLong(parts[2]);
                        result.add(new ItemProduct(item, count));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return result;
    }

    /**
     * 为战利品表物品分配默认产出速率
     */
    private static long getDefaultCount(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        if (path.contains("spawn_egg")) return 1;
        if (path.contains("star") || path.contains("head") || path.contains("skull")) return 100;
        if (path.contains("totem") || path.contains("trident")) return 50;
        return 500;
    }

    // ==================== 42 个农场的静态配置实例 ====================
    private static DataConfig create(ForgeConfigSpec.ConfigValue<List<? extends String>> products,
                                     Supplier<BlockEntityType<?>> entityTypeSupplier, String lootTable) {
        ResourceLocation lootId = lootTable != null ? ResourceLocation.fromNamespaceAndPath("minecraft", "entities/" + lootTable) : null;
        return new DataConfig(products, entityTypeSupplier, lootId);
    }

    public static final DataConfig FARM_BAMBOO = create(Config.FARM_BAMBOO_PRODUCTS, Registration.FARM_BAMBOO_ENTITY::get, null);
    public static final DataConfig FARM_BEE = create(Config.FARM_BEE_PRODUCTS, Registration.FARM_BEE_ENTITY::get, "bee");
    public static final DataConfig FARM_BLAZE = create(Config.FARM_BLAZE_PRODUCTS, Registration.FARM_BLAZE_ENTITY::get, "blaze");
    public static final DataConfig FARM_BONE_MEAL = create(Config.FARM_BONE_MEAL_PRODUCTS, Registration.FARM_BONE_MEAL_ENTITY::get, null);
    public static final DataConfig FARM_CHICKEN = create(Config.FARM_CHICKEN_PRODUCTS, Registration.FARM_CHICKEN_ENTITY::get, "chicken");
    public static final DataConfig FARM_COBBLESTONE = create(Config.FARM_COBBLESTONE_PRODUCTS, Registration.FARM_COBBLESTONE_ENTITY::get, null);
    public static final DataConfig FARM_COW = create(Config.FARM_COW_PRODUCTS, Registration.FARM_COW_ENTITY::get, "cow");
    public static final DataConfig FARM_CREEPER = create(Config.FARM_CREEPER_PRODUCTS, Registration.FARM_CREEPER_ENTITY::get, "creeper");
    public static final DataConfig FARM_DROWNED = create(Config.FARM_DROWNED_PRODUCTS, Registration.FARM_DROWNED_ENTITY::get, "drowned");
    public static final DataConfig FARM_ENDERMAN = create(Config.FARM_ENDERMAN_PRODUCTS, Registration.FARM_ENDERMAN_ENTITY::get, "enderman");
    public static final DataConfig FARM_ENDER_DRAGON = create(Config.FARM_ENDER_DRAGON_PRODUCTS, Registration.FARM_ENDER_DRAGON_ENTITY::get, "ender_dragon");
    public static final DataConfig FARM_EVOKER = create(Config.FARM_EVOKER_PRODUCTS, Registration.FARM_EVOKER_ENTITY::get, "evoker");
    public static final DataConfig FARM_FROG = create(Config.FARM_FROG_PRODUCTS, Registration.FARM_FROG_ENTITY::get, "frog");
    public static final DataConfig FARM_GHAST = create(Config.FARM_GHAST_PRODUCTS, Registration.FARM_GHAST_ENTITY::get, "ghast");
    public static final DataConfig FARM_GUARDIAN = create(Config.FARM_GUARDIAN_PRODUCTS, Registration.FARM_GUARDIAN_ENTITY::get, "guardian");
    public static final DataConfig FARM_HOGLIN = create(Config.FARM_HOGLIN_PRODUCTS, Registration.FARM_HOGLIN_ENTITY::get, "hoglin");
    public static final DataConfig FARM_ICE = create(Config.FARM_ICE_PRODUCTS, Registration.FARM_ICE_ENTITY::get, null);
    public static final DataConfig FARM_IRON_GOLEM = create(Config.FARM_IRON_GOLEM_PRODUCTS, Registration.FARM_IRON_GOLEM_ENTITY::get, "iron_golem");
    public static final DataConfig FARM_MAGMA_CUBE = create(Config.FARM_MAGMA_CUBE_PRODUCTS, Registration.FARM_MAGMA_CUBE_ENTITY::get, "magma_cube");
    public static final DataConfig FARM_PHANTOM = create(Config.FARM_PHANTOM_PRODUCTS, Registration.FARM_PHANTOM_ENTITY::get, "phantom");
    public static final DataConfig FARM_PIG = create(Config.FARM_PIG_PRODUCTS, Registration.FARM_PIG_ENTITY::get, "pig");
    public static final DataConfig FARM_PILLAGER = create(Config.FARM_PILLAGER_PRODUCTS, Registration.FARM_PILLAGER_ENTITY::get, "pillager");
    public static final DataConfig FARM_RABBIT = create(Config.FARM_RABBIT_PRODUCTS, Registration.FARM_RABBIT_ENTITY::get, "rabbit");
    public static final DataConfig FARM_RAVAGER = create(Config.FARM_RAVAGER_PRODUCTS, Registration.FARM_RAVAGER_ENTITY::get, "ravager");
    public static final DataConfig FARM_SHEEP = create(Config.FARM_SHEEP_PRODUCTS, Registration.FARM_SHEEP_ENTITY::get, "sheep");
    public static final DataConfig FARM_SHULKER = create(Config.FARM_SHULKER_PRODUCTS, Registration.FARM_SHULKER_ENTITY::get, "shulker");
    public static final DataConfig FARM_SKELETON = create(Config.FARM_SKELETON_PRODUCTS, Registration.FARM_SKELETON_ENTITY::get, "skeleton");
    public static final DataConfig FARM_SLIME = create(Config.FARM_SLIME_PRODUCTS, Registration.FARM_SLIME_ENTITY::get, "slime");
    public static final DataConfig FARM_SPIDER = create(Config.FARM_SPIDER_PRODUCTS, Registration.FARM_SPIDER_ENTITY::get, "spider");
    public static final DataConfig FARM_SQUID = create(Config.FARM_SQUID_PRODUCTS, Registration.FARM_SQUID_ENTITY::get, "squid");
    public static final DataConfig FARM_SUGAR_CANES = create(Config.FARM_SUGAR_CANES_PRODUCTS, Registration.FARM_SUGAR_CANES_ENTITY::get, null);
    public static final DataConfig FARM_VILLAGER = create(Config.FARM_VILLAGER_PRODUCTS, Registration.FARM_VILLAGER_ENTITY::get, "villager");
    public static final DataConfig FARM_WARDEN = create(Config.FARM_WARDEN_PRODUCTS, Registration.FARM_WARDEN_ENTITY::get, "warden");
    public static final DataConfig FARM_WITCH = create(Config.FARM_WITCH_PRODUCTS, Registration.FARM_WITCH_ENTITY::get, "witch");
    public static final DataConfig FARM_WITHER = create(Config.FARM_WITHER_PRODUCTS, Registration.FARM_WITHER_ENTITY::get, "wither");
    public static final DataConfig FARM_WITHER_SKELETON = create(Config.FARM_WITHER_SKELETON_PRODUCTS, Registration.FARM_WITHER_SKELETON_ENTITY::get, "wither_skeleton");
    public static final DataConfig FARM_WOOD = create(Config.FARM_WOOD_PRODUCTS, Registration.FARM_WOOD_ENTITY::get, null);
    public static final DataConfig FARM_ZOMBIE = create(Config.FARM_ZOMBIE_PRODUCTS, Registration.FARM_ZOMBIE_ENTITY::get, "zombie");
    public static final DataConfig FARM_ZOMBIFIED_PIGLIN = create(Config.FARM_ZOMBIFIED_PIGLIN_PRODUCTS, Registration.FARM_ZOMBIFIED_PIGLIN_ENTITY::get, "zombified_piglin");

    public static class ItemProduct {
        public final Item item;
        public final long count;

        public ItemProduct(Item item, long count) {
            this.item = item;
            this.count = count;
        }
    }
}
