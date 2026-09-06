package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 农场数据配置。产物从配置文件读取。
 */
public class DataConfig {

    private final ForgeConfigSpec.ConfigValue<List<? extends String>> productsConfig;
    private final Supplier<BlockEntityType<?>> entityTypeSupplier;
    private List<ItemProduct> cachedProducts;

    public DataConfig(ForgeConfigSpec.ConfigValue<List<? extends String>> productsConfig,
                      Supplier<BlockEntityType<?>> entityTypeSupplier) {
        this.productsConfig = productsConfig;
        this.entityTypeSupplier = entityTypeSupplier;
    }

    public BlockEntityType<?> getType() {
        return entityTypeSupplier.get();
    }

    /**
     * 获取产物列表（带缓存，首次调用时从配置解析，后续直接返回缓存）。
     */
    public List<ItemProduct> getProductList() {
        if (cachedProducts == null) {
            cachedProducts = parseProducts(productsConfig.get());
        }
        return cachedProducts;
    }

    /**
     * 解析产物配置字符串列表，格式 "namespace:item:count"
     */
    private static List<ItemProduct> parseProducts(List<? extends String> configList) {
        List<ItemProduct> result = new ArrayList<>();
        for (String entry : configList) {
            String[] parts = entry.split(":");
            if (parts.length >= 3) {
                ResourceLocation id = new ResourceLocation(parts[0], parts[1]);
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

    // ==================== 42 个农场的静态配置实例 ====================
    private static DataConfig create(ForgeConfigSpec.ConfigValue<List<? extends String>> products,
                                     Supplier<BlockEntityType<?>> entityTypeSupplier) {
        return new DataConfig(products, entityTypeSupplier);
    }

    public static final DataConfig FARM_BAMBOO = create(Config.FARM_BAMBOO_PRODUCTS, () -> Registration.FARM_BAMBOO_ENTITY.get());
    public static final DataConfig FARM_BONE_MEAL = create(Config.FARM_BONE_MEAL_PRODUCTS, () -> Registration.FARM_BONE_MEAL_ENTITY.get());
    public static final DataConfig FARM_COBBLESTONE = create(Config.FARM_COBBLESTONE_PRODUCTS, () -> Registration.FARM_COBBLESTONE_ENTITY.get());
    public static final DataConfig FARM_ICE = create(Config.FARM_ICE_PRODUCTS, () -> Registration.FARM_ICE_ENTITY.get());
    public static final DataConfig FARM_SUGAR_CANES = create(Config.FARM_SUGAR_CANES_PRODUCTS, () -> Registration.FARM_SUGAR_CANES_ENTITY.get());
    public static final DataConfig FARM_WOOD = create(Config.FARM_WOOD_PRODUCTS, () -> Registration.FARM_WOOD_ENTITY.get());

    public static class ItemProduct {
        public final Item item;
        public final long count;

        public ItemProduct(Item item, long count) {
            this.item = item;
            this.count = count;
        }
    }
}
