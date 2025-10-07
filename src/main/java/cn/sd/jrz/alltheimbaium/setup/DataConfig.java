package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DataConfig {
    public static final DataConfig FARM_BAMBOO = new DataConfig(
            product(Items.BAMBOO, 10000)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_BAMBOO_ENTITY.get();
        }
    };
    public static final DataConfig FARM_BEE = new DataConfig(
            product(Items.HONEYCOMB, 6000)
            , product(Items.HONEY_BLOCK, 500)
            , product(Items.BEE_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_BEE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_BLAZE = new DataConfig(
            product(Items.BLAZE_ROD, 1000)
            , product(Items.BLAZE_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_BLAZE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_BONE_MEAL = new DataConfig(
            product(Items.BONE_MEAL, 10000)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_BONE_MEAL_ENTITY.get();
        }
    };
    public static final DataConfig FARM_CHICKEN = new DataConfig(
            product(Items.CHICKEN, 2000)
            , product(Items.FEATHER, 2000)
            , product(Items.CHICKEN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_CHICKEN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_COW = new DataConfig(
            product(Items.BEEF, 4000)
            , product(Items.FEATHER, 2000)
            , product(Items.COW_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_COW_ENTITY.get();
        }
    };
    public static final DataConfig FARM_CREEPER = new DataConfig(
            product(Items.GUNPOWDER, 2000)
            , product(Items.CREEPER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_CREEPER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_DROWNED = new DataConfig(
            product(Items.ROTTEN_FLESH, 2000)
            , product(Items.COPPER_INGOT, 220)
            , product(Items.TRIDENT, 6)
            , product(Items.NAUTILUS_SHELL, 2)
            , product(Items.DROWNED_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_DROWNED_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ENDERMAN = new DataConfig(
            product(Items.ENDER_PEARL, 1000)
            , product(Items.ENDERMAN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ENDERMAN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ENDER_DRAGON = new DataConfig(
            product(Items.DRAGON_EGG, 2000)
            , product(Items.DRAGON_BREATH, 16000)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ENDER_DRAGON_ENTITY.get();
        }
    };
    public static final DataConfig FARM_EVOKER = new DataConfig(
            product(Items.TOTEM_OF_UNDYING, 2000)
            , product(Items.EMERALD, 1000)
            , product(Items.EVOKER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_EVOKER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_FROG = new DataConfig(
            product(Items.OCHRE_FROGLIGHT, 2000)
            , product(Items.PEARLESCENT_FROGLIGHT, 2000)
            , product(Items.VERDANT_FROGLIGHT, 2000)
            , product(Items.FROG_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_FROG_ENTITY.get();
        }
    };
    public static final DataConfig FARM_GHAST = new DataConfig(
            product(Items.GHAST_TEAR, 1000)
            , product(Items.GUNPOWDER, 2000)
            , product(Items.GHAST_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_GHAST_ENTITY.get();
        }
    };
    public static final DataConfig FARM_GUARDIAN = new DataConfig(
            product(Items.PRISMARINE_SHARD, 2000)
            , product(Items.PRISMARINE_CRYSTALS, 800)
            , product(Items.COD, 800)
            , product(Items.SALMON, 2)
            , product(Items.PUFFERFISH, 1)
            , product(Items.TROPICAL_FISH, 1)
            , product(Items.SPONGE, 1)
            , product(Items.GUARDIAN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_GUARDIAN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_HOGLIN = new DataConfig(
            product(Items.PORKCHOP, 6000)
            , product(Items.LEATHER, 1000)
            , product(Items.HOGLIN_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_HOGLIN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ICE = new DataConfig(
            product(Items.ICE, 2000)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ICE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_IRON_GOLEM = new DataConfig(
            product(Items.IRON_INGOT, 8000)
            , product(Items.POPPY, 2000)
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
            product(Items.PHANTOM_MEMBRANE, 1000)
            , product(Items.PHANTOM_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_PHANTOM_ENTITY.get();
        }
    };
    public static final DataConfig FARM_PIG = new DataConfig(
            product(Items.PORKCHOP, 4000)
            , product(Items.PIG_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_PIG_ENTITY.get();
        }
    };
    public static final DataConfig FARM_PILLAGER = new DataConfig(
            product(Items.ARROW, 2000)
            , product(Items.CROSSBOW, 50)
            , product(Items.PILLAGER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_PILLAGER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_RABBIT = new DataConfig(
            product(Items.RABBIT_HIDE, 1000)
            , product(Items.RABBIT, 2000)
            , product(Items.RABBIT_FOOT, 200)
            , product(Items.RABBIT_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_RABBIT_ENTITY.get();
        }
    };
    public static final DataConfig FARM_RAVAGER = new DataConfig(
            product(Items.SADDLE, 2000)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_RAVAGER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SHEEP = new DataConfig(
            product(Items.MUTTON, 3000)
            , product(Items.WHITE_WOOL, 125)
            , product(Items.LIGHT_GRAY_WOOL, 125)
            , product(Items.GRAY_WOOL, 125)
            , product(Items.BLACK_WOOL, 125)
            , product(Items.BROWN_WOOL, 125)
            , product(Items.RED_WOOL, 125)
            , product(Items.ORANGE_WOOL, 125)
            , product(Items.YELLOW_WOOL, 125)
            , product(Items.LIME_WOOL, 125)
            , product(Items.GREEN_WOOL, 125)
            , product(Items.CYAN_WOOL, 125)
            , product(Items.LIGHT_BLUE_WOOL, 125)
            , product(Items.BLUE_WOOL, 125)
            , product(Items.PURPLE_WOOL, 125)
            , product(Items.MAGENTA_WOOL, 125)
            , product(Items.PINK_WOOL, 125)
            , product(Items.SHEEP_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SHEEP_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SHULKER = new DataConfig(
            product(Items.SHULKER_SHELL, 1000)
            , product(Items.SHULKER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SHULKER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SKELETON = new DataConfig(
            product(Items.BONE, 2000)
            , product(Items.ARROW, 2000)
            , product(Items.SKELETON_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SKELETON_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SLIME = new DataConfig(
            product(Items.SLIME_BALL, 3000)
            , product(Items.SLIME_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SLIME_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SPIDER = new DataConfig(
            product(Items.STRING, 2000)
            , product(Items.SPIDER_EYE, 660)
            , product(Items.SPIDER_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SPIDER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SQUID = new DataConfig(
            product(Items.INK_SAC, 4000)
            , product(Items.GLOW_INK_SAC, 1000)
            , product(Items.SQUID_SPAWN_EGG, 1)
            , product(Items.GLOW_SQUID_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SQUID_ENTITY.get();
        }
    };
    public static final DataConfig FARM_SUGAR_CANES = new DataConfig(
            product(Items.SUGAR_CANE, 10000)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_SUGAR_CANES_ENTITY.get();
        }
    };
    public static final DataConfig FARM_VILLAGER = new DataConfig(
            product(Items.VILLAGER_SPAWN_EGG, 50)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_VILLAGER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WARDEN = new DataConfig(
            product(Items.SCULK_CATALYST, 2000),
            product(Items.SCULK_SHRIEKER, 500),
            product(Items.SCULK_SENSOR, 500),
            product(Items.SCULK_VEIN, 500),
            product(Items.ECHO_SHARD, 500)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WARDEN_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WITCH = new DataConfig(
            product(Items.REDSTONE, 12000)
            , product(Items.GLASS_BOTTLE, 580)
            , product(Items.GLOWSTONE_DUST, 580)
            , product(Items.GUNPOWDER, 580)
            , product(Items.SPIDER_EYE, 580)
            , product(Items.SUGAR, 580)
            , product(Items.STICK, 1140)
            , product(Items.WITCH_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WITCH_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WITHER = new DataConfig(
            product(Items.NETHER_STAR, 2000)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WITHER_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WITHER_SKELETON = new DataConfig(
            product(Items.COAL, 660)
            , product(Items.BONE, 2000)
            , product(Items.WITHER_SKELETON_SKULL, 60)
            , product(Items.WITHER_SKELETON_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WITHER_SKELETON_ENTITY.get();
        }
    };
    public static final DataConfig FARM_WOOD = new DataConfig(
            product(Items.OAK_WOOD, 1250)
            , product(Items.BIRCH_WOOD, 1250)
            , product(Items.SPRUCE_WOOD, 1250)
            , product(Items.JUNGLE_WOOD, 1250)
            , product(Items.ACACIA_WOOD, 1250)
            , product(Items.DARK_OAK_WOOD, 1250)
            , product(Items.MANGROVE_WOOD, 1250)
            , product(Items.CHERRY_WOOD, 1250)
            , product(Items.APPLE, 100)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_WOOD_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ZOMBIE = new DataConfig(
            product(Items.ROTTEN_FLESH, 2000)
            , product(Items.IRON_INGOT, 20)
            , product(Items.CARROT, 20)
            , product(Items.POTATO, 20)
            , product(Items.ZOMBIE_SPAWN_EGG, 1)
    ) {
        @Override
        public BlockEntityType<?> getType() {
            return Registration.FARM_ZOMBIE_ENTITY.get();
        }
    };
    public static final DataConfig FARM_ZOMBIFIED_PIGLIN = new DataConfig(
            product(Items.ROTTEN_FLESH, 1000)
            , product(Items.GOLD_NUGGET, 1000)
            , product(Items.GOLD_INGOT, 60)
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
