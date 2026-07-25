package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 农场数据配置。产物从配置文件读取。
 */
public class DataConfig {
    private final ModConfigSpec.ConfigValue<List<? extends String>> productsConfig;
    private final Supplier<BlockEntityType<?>> entityTypeSupplier;
    private List<ItemProduct> cachedProducts;

    public DataConfig(
            ModConfigSpec.ConfigValue<List<? extends String>> productsConfig,
            Supplier<BlockEntityType<?>> entityTypeSupplier
    ) {
        this.productsConfig = productsConfig;
        this.entityTypeSupplier = entityTypeSupplier;
    }

    public BlockEntityType<?> getType() {
        return entityTypeSupplier.get();
    }

    /**
     * 获取解析后的产物列表，结果会被缓存
     */
    public List<ItemProduct> getProductList() {
        if (cachedProducts == null) {
            cachedProducts = parseProducts(productsConfig.get());
        }
        return cachedProducts;
    }

    private static List<ItemProduct> parseProducts(List<? extends String> configList) {
        List<ItemProduct> result = new ArrayList<>();
        for (String entry : configList) {
            String[] parts = entry.split(":");
            if (parts.length >= 3) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                Item item = BuiltInRegistries.ITEM.get(id);
                try {
                    long count = Long.parseLong(parts[2]);
                    result.add(new ItemProduct(item, count));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    private static DataConfig create(ModConfigSpec.ConfigValue<List<? extends String>> products,
                                     Supplier<BlockEntityType<?>> entityTypeSupplier) {
        return new DataConfig(products, entityTypeSupplier);
    }

    public static final DataConfig FARM_BAMBOO = create(Config.FARM_BAMBOO_PRODUCTS, Registration.FARM_BAMBOO_ENTITY::get);
    public static final DataConfig FARM_BEE = create(Config.FARM_BEE_PRODUCTS, Registration.FARM_BEE_ENTITY::get);
    public static final DataConfig FARM_BLAZE = create(Config.FARM_BLAZE_PRODUCTS, Registration.FARM_BLAZE_ENTITY::get);
    public static final DataConfig FARM_BONE_MEAL = create(Config.FARM_BONE_MEAL_PRODUCTS, Registration.FARM_BONE_MEAL_ENTITY::get);
    public static final DataConfig FARM_CHICKEN = create(Config.FARM_CHICKEN_PRODUCTS, Registration.FARM_CHICKEN_ENTITY::get);
    public static final DataConfig FARM_COBBLESTONE = create(Config.FARM_COBBLESTONE_PRODUCTS, Registration.FARM_COBBLESTONE_ENTITY::get);
    public static final DataConfig FARM_COW = create(Config.FARM_COW_PRODUCTS, Registration.FARM_COW_ENTITY::get);
    public static final DataConfig FARM_CREEPER = create(Config.FARM_CREEPER_PRODUCTS, Registration.FARM_CREEPER_ENTITY::get);
    public static final DataConfig FARM_DROWNED = create(Config.FARM_DROWNED_PRODUCTS, Registration.FARM_DROWNED_ENTITY::get);
    public static final DataConfig FARM_ENDERMAN = create(Config.FARM_ENDERMAN_PRODUCTS, Registration.FARM_ENDERMAN_ENTITY::get);
    public static final DataConfig FARM_ENDER_DRAGON = create(Config.FARM_ENDER_DRAGON_PRODUCTS, Registration.FARM_ENDER_DRAGON_ENTITY::get);
    public static final DataConfig FARM_EVOKER = create(Config.FARM_EVOKER_PRODUCTS, Registration.FARM_EVOKER_ENTITY::get);
    public static final DataConfig FARM_FROG = create(Config.FARM_FROG_PRODUCTS, Registration.FARM_FROG_ENTITY::get);
    public static final DataConfig FARM_GHAST = create(Config.FARM_GHAST_PRODUCTS, Registration.FARM_GHAST_ENTITY::get);
    public static final DataConfig FARM_GUARDIAN = create(Config.FARM_GUARDIAN_PRODUCTS, Registration.FARM_GUARDIAN_ENTITY::get);
    public static final DataConfig FARM_HOGLIN = create(Config.FARM_HOGLIN_PRODUCTS, Registration.FARM_HOGLIN_ENTITY::get);
    public static final DataConfig FARM_ICE = create(Config.FARM_ICE_PRODUCTS, Registration.FARM_ICE_ENTITY::get);
    public static final DataConfig FARM_IRON_GOLEM = create(Config.FARM_IRON_GOLEM_PRODUCTS, Registration.FARM_IRON_GOLEM_ENTITY::get);
    public static final DataConfig FARM_MAGMA_CUBE = create(Config.FARM_MAGMA_CUBE_PRODUCTS, Registration.FARM_MAGMA_CUBE_ENTITY::get);
    public static final DataConfig FARM_PHANTOM = create(Config.FARM_PHANTOM_PRODUCTS, Registration.FARM_PHANTOM_ENTITY::get);
    public static final DataConfig FARM_PIG = create(Config.FARM_PIG_PRODUCTS, Registration.FARM_PIG_ENTITY::get);
    public static final DataConfig FARM_PILLAGER = create(Config.FARM_PILLAGER_PRODUCTS, Registration.FARM_PILLAGER_ENTITY::get);
    public static final DataConfig FARM_RABBIT = create(Config.FARM_RABBIT_PRODUCTS, Registration.FARM_RABBIT_ENTITY::get);
    public static final DataConfig FARM_RAVAGER = create(Config.FARM_RAVAGER_PRODUCTS, Registration.FARM_RAVAGER_ENTITY::get);
    public static final DataConfig FARM_SHEEP = create(Config.FARM_SHEEP_PRODUCTS, Registration.FARM_SHEEP_ENTITY::get);
    public static final DataConfig FARM_SHULKER = create(Config.FARM_SHULKER_PRODUCTS, Registration.FARM_SHULKER_ENTITY::get);
    public static final DataConfig FARM_SKELETON = create(Config.FARM_SKELETON_PRODUCTS, Registration.FARM_SKELETON_ENTITY::get);
    public static final DataConfig FARM_SLIME = create(Config.FARM_SLIME_PRODUCTS, Registration.FARM_SLIME_ENTITY::get);
    public static final DataConfig FARM_SPIDER = create(Config.FARM_SPIDER_PRODUCTS, Registration.FARM_SPIDER_ENTITY::get);
    public static final DataConfig FARM_SQUID = create(Config.FARM_SQUID_PRODUCTS, Registration.FARM_SQUID_ENTITY::get);
    public static final DataConfig FARM_SUGAR_CANES = create(Config.FARM_SUGAR_CANES_PRODUCTS, Registration.FARM_SUGAR_CANES_ENTITY::get);
    public static final DataConfig FARM_VILLAGER = create(Config.FARM_VILLAGER_PRODUCTS, Registration.FARM_VILLAGER_ENTITY::get);
    public static final DataConfig FARM_WARDEN = create(Config.FARM_WARDEN_PRODUCTS, Registration.FARM_WARDEN_ENTITY::get);
    public static final DataConfig FARM_WITCH = create(Config.FARM_WITCH_PRODUCTS, Registration.FARM_WITCH_ENTITY::get);
    public static final DataConfig FARM_WITHER = create(Config.FARM_WITHER_PRODUCTS, Registration.FARM_WITHER_ENTITY::get);
    public static final DataConfig FARM_WITHER_SKELETON = create(Config.FARM_WITHER_SKELETON_PRODUCTS, Registration.FARM_WITHER_SKELETON_ENTITY::get);
    public static final DataConfig FARM_WOOD = create(Config.FARM_WOOD_PRODUCTS, Registration.FARM_WOOD_ENTITY::get);
    public static final DataConfig FARM_ZOMBIE = create(Config.FARM_ZOMBIE_PRODUCTS, Registration.FARM_ZOMBIE_ENTITY::get);
    public static final DataConfig FARM_ZOMBIFIED_PIGLIN = create(Config.FARM_ZOMBIFIED_PIGLIN_PRODUCTS, Registration.FARM_ZOMBIFIED_PIGLIN_ENTITY::get);

    public static class ItemProduct {
        public final Item item;
        public final long count;

        public ItemProduct(Item item, long count) {
            this.item = item;
            this.count = count;
        }
    }
}
