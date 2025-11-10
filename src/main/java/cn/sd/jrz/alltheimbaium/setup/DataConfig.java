package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DataConfig {
    public static final DataConfig FARM_BAMBOO = new DataConfig(
            product(Items.BAMBOO, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_BAMBOO_ENTITY.get();
        }
    };
    public static final DataConfig FARM_BEE = new DataConfig(
            product(Items.HONEYCOMB, 500)
            , product(Items.HONEY_BLOCK, 400)
            , product(Items.BEE_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_BEE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_BLAZE = new DataConfig(
            product(Items.BLAZE_ROD, 500)
            , product(Items.BLAZE_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_BLAZE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_BONE_MEAL = new DataConfig(
            product(Items.BONE_MEAL, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_BONE_MEAL_ENTITY.get();
        }
    };
    public static final DataConfig FARM_CHICKEN = new DataConfig(
            product(Items.CHICKEN, 50)
            , product(Items.FEATHER, 500)
            , product(Items.CHICKEN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_CHICKEN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_COBBLESTONE = new DataConfig(
            product(Items.COBBLESTONE, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_COBBLESTONE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_COW = new DataConfig(
            product(Items.BEEF, 50)
            , product(Items.LEATHER, 500)
            , product(Items.COW_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_COW_ENTITY.get();
        }
    };
    public static final DataConfig FARM_CREEPER = new DataConfig(
            product(Items.GUNPOWDER, 500)
            , product(Items.CREEPER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_CREEPER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_DROWNED = new DataConfig(
            product(Items.ROTTEN_FLESH, 500)
            , product(Items.COPPER_INGOT, 400)
            , product(Items.TRIDENT, 50)
            , product(Items.NAUTILUS_SHELL, 50)
            , product(Items.DROWNED_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_DROWNED_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ENDERMAN = new DataConfig(
            product(Items.ENDER_PEARL, 500)
            , product(Items.ENDERMAN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ENDERMAN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ENDER_DRAGON = new DataConfig(
            product(Items.DRAGON_EGG, 500)
            , product(Items.DRAGON_BREATH, 400)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ENDER_DRAGON_ENTITY.get();
        }
    };
    public static final DataConfig FARM_EVOKER = new DataConfig(
            product(Items.TOTEM_OF_UNDYING, 250)
            , product(Items.EMERALD, 500)
            , product(Items.EVOKER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_EVOKER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_FROG = new DataConfig(
            product(Items.OCHRE_FROGLIGHT, 500)
            , product(Items.PEARLESCENT_FROGLIGHT, 500)
            , product(Items.VERDANT_FROGLIGHT, 500)
            , product(Items.FROG_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_FROG_ENTITY.get();
        }
    };
    public static final DataConfig FARM_GHAST = new DataConfig(
            product(Items.GHAST_TEAR, 250)
            , product(Items.GUNPOWDER, 500)
            , product(Items.GHAST_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_GHAST_ENTITY.get();
        }
    };
    public static final DataConfig FARM_GUARDIAN = new DataConfig(
            product(Items.PRISMARINE_SHARD, 500)
            , product(Items.PRISMARINE_CRYSTALS, 250)
            , product(Items.COD, 50)
            , product(Items.SALMON, 50)
            , product(Items.PUFFERFISH, 50)
            , product(Items.TROPICAL_FISH, 50)
            , product(Items.SPONGE, 50)
            , product(Items.GUARDIAN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_GUARDIAN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_HOGLIN = new DataConfig(
            product(Items.COOKED_PORKCHOP, 500)
            , product(Items.LEATHER, 100)
            , product(Items.HOGLIN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_HOGLIN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ICE = new DataConfig(
            product(Items.ICE, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ICE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_IRON_GOLEM = new DataConfig(
            product(Items.IRON_INGOT, 500)
            , product(Items.POPPY, 50)
            , product(Items.IRON_GOLEM_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_IRON_GOLEM_ENTITY.get();
        }
    };
    public static final DataConfig FARM_MAGMA_CUBE = new DataConfig(
            product(Items.MAGMA_CREAM, 500)
            , product(Items.MAGMA_CUBE_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_MAGMA_CUBE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_PHANTOM = new DataConfig(
            product(Items.PHANTOM_MEMBRANE, 500)
            , product(Items.PHANTOM_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_PHANTOM_ENTITY.get();
        }
    };
    public static final DataConfig FARM_PIG = new DataConfig(
            product(Items.PORKCHOP, 500)
            , product(Items.PIG_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_PIG_ENTITY.get();
        }
    };
    public static final DataConfig FARM_PILLAGER = new DataConfig(
            product(Items.ARROW, 500)
            , product(Items.EXPERIENCE_BOTTLE, 10)
            , product(Items.PILLAGER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_PILLAGER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_RABBIT = new DataConfig(
            product(Items.RABBIT_HIDE, 100)
            , product(Items.RABBIT, 300)
            , product(Items.RABBIT_FOOT, 100)
            , product(Items.RABBIT_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_RABBIT_ENTITY.get();
        }
    };
    public static final DataConfig FARM_RAVAGER = new DataConfig(
            product(Items.SADDLE, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_RAVAGER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SHEEP = new DataConfig(
            product(Items.MUTTON, 50)
            , product(Items.WHITE_WOOL, 500)
            , product(Items.LIGHT_GRAY_WOOL, 50)
            , product(Items.GRAY_WOOL, 50)
            , product(Items.BLACK_WOOL, 50)
            , product(Items.BROWN_WOOL, 50)
            , product(Items.RED_WOOL, 50)
            , product(Items.ORANGE_WOOL, 50)
            , product(Items.YELLOW_WOOL, 50)
            , product(Items.LIME_WOOL, 50)
            , product(Items.GREEN_WOOL, 50)
            , product(Items.CYAN_WOOL, 50)
            , product(Items.LIGHT_BLUE_WOOL, 50)
            , product(Items.BLUE_WOOL, 50)
            , product(Items.PURPLE_WOOL, 50)
            , product(Items.MAGENTA_WOOL, 50)
            , product(Items.PINK_WOOL, 50)
            , product(Items.SHEEP_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SHEEP_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SHULKER = new DataConfig(
            product(Items.SHULKER_SHELL, 500)
            , product(Items.SHULKER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SHULKER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SKELETON = new DataConfig(
            product(Items.BONE, 500)
            , product(Items.ARROW, 100)
            , product(Items.SKELETON_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SKELETON_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SLIME = new DataConfig(
            product(Items.SLIME_BALL, 500)
            , product(Items.SLIME_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SLIME_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SPIDER = new DataConfig(
            product(Items.STRING, 500)
            , product(Items.SPIDER_EYE, 50)
            , product(Items.SPIDER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SPIDER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SQUID = new DataConfig(
            product(Items.INK_SAC, 500)
            , product(Items.GLOW_INK_SAC, 200)
            , product(Items.SQUID_SPAWN_EGG, 1)
            , product(Items.GLOW_SQUID_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SQUID_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SUGAR_CANES = new DataConfig(
            product(Items.SUGAR_CANE, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SUGAR_CANES_ENTITY.get();
        }
    };
    public static final DataConfig FARM_VILLAGER = new DataConfig(
            product(Items.VILLAGER_SPAWN_EGG, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_VILLAGER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WARDEN = new DataConfig(
            product(Items.SCULK_CATALYST, 100),
            product(Items.SCULK_SHRIEKER, 100),
            product(Items.SCULK_SENSOR, 100),
            product(Items.SCULK_VEIN, 100),
            product(Items.ECHO_SHARD, 100)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WARDEN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WITCH = new DataConfig(
            product(Items.REDSTONE, 500)
            , product(Items.GLASS_BOTTLE, 50)
            , product(Items.GLOWSTONE_DUST, 50)
            , product(Items.GUNPOWDER, 50)
            , product(Items.SPIDER_EYE, 50)
            , product(Items.SUGAR, 50)
            , product(Items.STICK, 50)
            , product(Items.WITCH_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WITCH_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WITHER = new DataConfig(
            product(Items.NETHER_STAR, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WITHER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WITHER_SKELETON = new DataConfig(
            product(Items.COAL, 500)
            , product(Items.BONE, 100)
            , product(Items.WITHER_SKELETON_SKULL, 50)
            , product(Items.WITHER_SKELETON_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WITHER_SKELETON_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WOOD = new DataConfig(
            product(Items.OAK_LOG, 500)
            , product(Items.BIRCH_LOG, 50)
            , product(Items.SPRUCE_LOG, 50)
            , product(Items.JUNGLE_LOG, 50)
            , product(Items.ACACIA_LOG, 50)
            , product(Items.DARK_OAK_LOG, 50)
            , product(Items.MANGROVE_LOG, 50)
            , product(Items.CHERRY_LOG, 50)
            , product(Items.APPLE, 10)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WOOD_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ZOMBIE = new DataConfig(
            product(Items.ROTTEN_FLESH, 500)
            , product(Items.IRON_INGOT, 250)
            , product(Items.CARROT, 10)
            , product(Items.POTATO, 10)
            , product(Items.ZOMBIE_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ZOMBIE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ZOMBIFIED_PIGLIN = new DataConfig(
            product(Items.ROTTEN_FLESH, 500)
            , product(Items.GOLD_NUGGET, 250)
            , product(Items.GOLD_INGOT, 250)
            , product(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ZOMBIFIED_PIGLIN_ENTITY.get();
        }
    };

    public abstract BlockEntityType<?> getType();

    private final List<ItemProduct> productList;

    public DataConfig(ItemProduct... array) {
        this.productList = new ArrayList<>();
        this.productList.addAll(Arrays.asList(array));
    }

    public List<ItemProduct> getProductList() {
        return productList;
    }

    public static ItemProduct product(Item item, long count) {
        return new ItemProduct(item, count);
    }

    public static class ItemProduct {
        public Item item;
        public long count;

        public ItemProduct(Item item, long count) {
            this.item = item;
            this.count = count;
        }
    }
}
