package cn.sd.jrz.alltheimbaium.setup;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;

/**
 * 配置文件。使用 ForgeConfigSpec，参考 auto-resource 项目的配置模式。
 * 配置类型为 SERVER（每世界可不同），在 Alltheimbaium 构造器中注册。
 */
public class Config {

    // ==================== ATI 耕地 ====================
    public static ForgeConfigSpec.IntValue FARMLAND_TICK_INTERVAL;
    public static ForgeConfigSpec.IntValue FARMLAND_GROWTH_AMOUNT;
    public static ForgeConfigSpec.BooleanValue FARMLAND_BONEMEAL_ENABLED;
    public static ForgeConfigSpec.IntValue FARMLAND_BONEMEAL_INTERVAL;

    // ==================== 时钟方块 ====================
    public static ForgeConfigSpec.BooleanValue CLOCK_DEFAULT_ACTIVE;

    // ==================== 农场 ====================
    public static ForgeConfigSpec.IntValue FARM_LEVEL_UP_INTERVAL_SECONDS;
    public static ForgeConfigSpec.LongValue FARM_CARRY;
    public static ForgeConfigSpec.BooleanValue FARM_USE_LOOT_TABLE;
    public static ForgeConfigSpec.DoubleValue FARM_GROWTH_RATE;
    // 每个农场的实体类型（用于战利品表查询）和配置产物
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_BAMBOO_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_BEE_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_BLAZE_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_BONE_MEAL_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_CHICKEN_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_COBBLESTONE_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_COW_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_CREEPER_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_DROWNED_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_ENDERMAN_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_ENDER_DRAGON_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_EVOKER_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_FROG_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_GHAST_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_GUARDIAN_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_HOGLIN_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_ICE_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_IRON_GOLEM_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_MAGMA_CUBE_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_PHANTOM_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_PIG_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_PILLAGER_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_RABBIT_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_RAVAGER_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_SHEEP_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_SHULKER_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_SKELETON_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_SLIME_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_SPIDER_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_SQUID_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_SUGAR_CANES_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_VILLAGER_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_WARDEN_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_WITCH_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_WITHER_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_WITHER_SKELETON_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_WOOD_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_ZOMBIE_PRODUCTS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FARM_ZOMBIFIED_PIGLIN_PRODUCTS;

    // ==================== 液体无限制造机 ====================
    public static ForgeConfigSpec.LongValue LIQUID_FOUNTAIN_INFINITE_THRESHOLD;

    // ==================== 存储方块制造机 ====================
    public static ForgeConfigSpec.IntValue STORAGE_FOUNTAIN_MAX_ITEM_TYPES;
    public static ForgeConfigSpec.LongValue STORAGE_FOUNTAIN_CARRY;
    public static ForgeConfigSpec.IntValue STORAGE_FOUNTAIN_GROWTH_INTERVAL_SECONDS;
    public static ForgeConfigSpec.LongValue STORAGE_FOUNTAIN_GROWTH_STEP;
    public static ForgeConfigSpec.LongValue STORAGE_FOUNTAIN_INITIAL_OUTPUT;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> STORAGE_FOUNTAIN_ACCEPTED_MODS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> STORAGE_FOUNTAIN_ACCEPTED_TAGS;

    // ==================== 配置规范 ====================
    public static ForgeConfigSpec SERVER_CONFIG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // ---- ATI 耕地 ----
        builder.comment("ATI 耕地设置").push("farmland");
        FARMLAND_TICK_INTERVAL = builder
                .comment("每多少 tick 检查一次作物生长。默认为 1（每 tick 都检查）")
                .defineInRange("tick_interval", 1, 1, Integer.MAX_VALUE);
        FARMLAND_GROWTH_AMOUNT = builder
                .comment("每次增加的生长阶段数。-1 时直接设为最大生长阶段（立即成熟）")
                .defineInRange("growth_amount", -1, -1, Integer.MAX_VALUE);
        FARMLAND_BONEMEAL_ENABLED = builder
                .comment("是否额外对 BonemealableBlock 施放骨粉效果")
                .define("bonemeal_enabled", true);
        FARMLAND_BONEMEAL_INTERVAL = builder
                .comment("每多少 tick 额外施放一次骨粉")
                .defineInRange("bonemeal_interval", 1, 1, Integer.MAX_VALUE);
        builder.pop();

        // ---- 时钟方块 ----
        builder.comment("时钟方块设置").push("clock");
        CLOCK_DEFAULT_ACTIVE = builder
                .comment("进入游戏后时钟的初始开关状态。true=启用，false=禁用")
                .define("default_active", false);
        builder.pop();

        // ---- 农场 ----
        builder.comment("农场设置").push("farm");
        FARM_LEVEL_UP_INTERVAL_SECONDS = builder
                .comment("农场升级间隔（秒）")
                .defineInRange("level_up_interval_seconds", 20, 1, Integer.MAX_VALUE);
        FARM_CARRY = builder
                .comment("进位阈值。产出累加至此值后进位为完整物品")
                .defineInRange("carry", 10000L, 1L, Long.MAX_VALUE);
        FARM_USE_LOOT_TABLE = builder
                .comment("是否从对应实体的战利品表中获取产物。true 时与配置产物取交集")
                .define("use_loot_table", true);
        FARM_GROWTH_RATE = builder
                .comment("全局增长率，产出 = 原产出 × 增长率")
                .defineInRange("growth_rate", 1.0, 0.0, Double.MAX_VALUE);

        builder.comment("各农场产物配置，格式 item:count").push("products");
        FARM_BAMBOO_PRODUCTS = builder.defineList("bamboo", () -> List.of("minecraft:bamboo:500"), o -> o instanceof String);
        FARM_BEE_PRODUCTS = builder.defineList("bee", () -> List.of("minecraft:honeycomb:500", "minecraft:honey_block:400", "minecraft:bee_spawn_egg:1"), o -> o instanceof String);
        FARM_BLAZE_PRODUCTS = builder.defineList("blaze", () -> List.of("minecraft:blaze_rod:500", "minecraft:blaze_spawn_egg:1"), o -> o instanceof String);
        FARM_BONE_MEAL_PRODUCTS = builder.defineList("bone_meal", () -> List.of("minecraft:bone_meal:500"), o -> o instanceof String);
        FARM_CHICKEN_PRODUCTS = builder.defineList("chicken", () -> List.of("minecraft:chicken:50", "minecraft:feather:500", "minecraft:egg:500", "minecraft:chicken_spawn_egg:1"), o -> o instanceof String);
        FARM_COBBLESTONE_PRODUCTS = builder.defineList("cobblestone", () -> List.of("minecraft:cobblestone:500"), o -> o instanceof String);
        FARM_COW_PRODUCTS = builder.defineList("cow", () -> List.of("minecraft:beef:50", "minecraft:leather:500", "minecraft:cow_spawn_egg:1"), o -> o instanceof String);
        FARM_CREEPER_PRODUCTS = builder.defineList("creeper", () -> List.of("minecraft:gunpowder:500", "minecraft:creeper_spawn_egg:1"), o -> o instanceof String);
        FARM_DROWNED_PRODUCTS = builder.defineList("drowned", () -> List.of("minecraft:rotten_flesh:500", "minecraft:copper_ingot:400", "minecraft:trident:50", "minecraft:nautilus_shell:50", "minecraft:drowned_spawn_egg:1"), o -> o instanceof String);
        FARM_ENDERMAN_PRODUCTS = builder.defineList("enderman", () -> List.of("minecraft:ender_pearl:500", "minecraft:enderman_spawn_egg:1"), o -> o instanceof String);
        FARM_ENDER_DRAGON_PRODUCTS = builder.defineList("ender_dragon", () -> List.of("minecraft:dragon_egg:500", "minecraft:dragon_breath:400", "minecraft:ender_dragon_spawn_egg:1"), o -> o instanceof String);
        FARM_EVOKER_PRODUCTS = builder.defineList("evoker", () -> List.of("minecraft:totem_of_undying:250", "minecraft:emerald:500", "minecraft:evoker_spawn_egg:1"), o -> o instanceof String);
        FARM_FROG_PRODUCTS = builder.defineList("frog", () -> List.of("minecraft:ochre_froglight:500", "minecraft:pearlescent_froglight:500", "minecraft:verdant_froglight:500", "minecraft:frog_spawn_egg:1"), o -> o instanceof String);
        FARM_GHAST_PRODUCTS = builder.defineList("ghast", () -> List.of("minecraft:ghast_tear:250", "minecraft:gunpowder:500", "minecraft:ghast_spawn_egg:1"), o -> o instanceof String);
        FARM_GUARDIAN_PRODUCTS = builder.defineList("guardian", () -> List.of("minecraft:prismarine_shard:500", "minecraft:prismarine_crystals:250", "minecraft:cod:50", "minecraft:salmon:50", "minecraft:pufferfish:50", "minecraft:tropical_fish:50", "minecraft:sponge:50", "minecraft:guardian_spawn_egg:1"), o -> o instanceof String);
        FARM_HOGLIN_PRODUCTS = builder.defineList("hoglin", () -> List.of("minecraft:cooked_porkchop:500", "minecraft:leather:100", "minecraft:hoglin_spawn_egg:1"), o -> o instanceof String);
        FARM_ICE_PRODUCTS = builder.defineList("ice", () -> List.of("minecraft:ice:500"), o -> o instanceof String);
        FARM_IRON_GOLEM_PRODUCTS = builder.defineList("iron_golem", () -> List.of("minecraft:iron_ingot:500", "minecraft:poppy:50", "minecraft:iron_golem_spawn_egg:1"), o -> o instanceof String);
        FARM_MAGMA_CUBE_PRODUCTS = builder.defineList("magma_cube", () -> List.of("minecraft:magma_cream:500", "minecraft:magma_cube_spawn_egg:1"), o -> o instanceof String);
        FARM_PHANTOM_PRODUCTS = builder.defineList("phantom", () -> List.of("minecraft:phantom_membrane:500", "minecraft:phantom_spawn_egg:1"), o -> o instanceof String);
        FARM_PIG_PRODUCTS = builder.defineList("pig", () -> List.of("minecraft:porkchop:500", "minecraft:pig_spawn_egg:1"), o -> o instanceof String);
        FARM_PILLAGER_PRODUCTS = builder.defineList("pillager", () -> List.of("minecraft:arrow:500", "minecraft:experience_bottle:10", "minecraft:pillager_spawn_egg:1"), o -> o instanceof String);
        FARM_RABBIT_PRODUCTS = builder.defineList("rabbit", () -> List.of("minecraft:rabbit_hide:100", "minecraft:rabbit:300", "minecraft:rabbit_foot:100", "minecraft:rabbit_spawn_egg:1"), o -> o instanceof String);
        FARM_RAVAGER_PRODUCTS = builder.defineList("ravager", () -> List.of("minecraft:saddle:500", "minecraft:ravager_spawn_egg:1"), o -> o instanceof String);
        FARM_SHEEP_PRODUCTS = builder.defineList("sheep", () -> List.of("minecraft:mutton:50", "minecraft:white_wool:500", "minecraft:light_gray_wool:50", "minecraft:gray_wool:50", "minecraft:black_wool:50", "minecraft:brown_wool:50", "minecraft:red_wool:50", "minecraft:orange_wool:50", "minecraft:yellow_wool:50", "minecraft:lime_wool:50", "minecraft:green_wool:50", "minecraft:cyan_wool:50", "minecraft:light_blue_wool:50", "minecraft:blue_wool:50", "minecraft:purple_wool:50", "minecraft:magenta_wool:50", "minecraft:pink_wool:50", "minecraft:sheep_spawn_egg:1"), o -> o instanceof String);
        FARM_SHULKER_PRODUCTS = builder.defineList("shulker", () -> List.of("minecraft:shulker_shell:500", "minecraft:shulker_spawn_egg:1"), o -> o instanceof String);
        FARM_SKELETON_PRODUCTS = builder.defineList("skeleton", () -> List.of("minecraft:bone:500", "minecraft:arrow:100", "minecraft:skeleton_spawn_egg:1"), o -> o instanceof String);
        FARM_SLIME_PRODUCTS = builder.defineList("slime", () -> List.of("minecraft:slime_ball:500", "minecraft:slime_spawn_egg:1"), o -> o instanceof String);
        FARM_SPIDER_PRODUCTS = builder.defineList("spider", () -> List.of("minecraft:string:500", "minecraft:spider_eye:50", "minecraft:spider_spawn_egg:1"), o -> o instanceof String);
        FARM_SQUID_PRODUCTS = builder.defineList("squid", () -> List.of("minecraft:ink_sac:500", "minecraft:glow_ink_sac:200", "minecraft:squid_spawn_egg:1", "minecraft:glow_squid_spawn_egg:1"), o -> o instanceof String);
        FARM_SUGAR_CANES_PRODUCTS = builder.defineList("sugar_canes", () -> List.of("minecraft:sugar_cane:500"), o -> o instanceof String);
        FARM_VILLAGER_PRODUCTS = builder.defineList("villager", () -> List.of("minecraft:villager_spawn_egg:500"), o -> o instanceof String);
        FARM_WARDEN_PRODUCTS = builder.defineList("warden", () -> List.of("minecraft:sculk_catalyst:100", "minecraft:sculk_shrieker:100", "minecraft:sculk_sensor:100", "minecraft:sculk_vein:100", "minecraft:echo_shard:100", "minecraft:warden_spawn_egg:1"), o -> o instanceof String);
        FARM_WITCH_PRODUCTS = builder.defineList("witch", () -> List.of("minecraft:redstone:500", "minecraft:glass_bottle:50", "minecraft:glowstone_dust:50", "minecraft:gunpowder:50", "minecraft:spider_eye:50", "minecraft:sugar:50", "minecraft:stick:50", "minecraft:witch_spawn_egg:1"), o -> o instanceof String);
        FARM_WITHER_PRODUCTS = builder.defineList("wither", () -> List.of("minecraft:nether_star:500", "minecraft:wither_spawn_egg:500"), o -> o instanceof String);
        FARM_WITHER_SKELETON_PRODUCTS = builder.defineList("wither_skeleton", () -> List.of("minecraft:coal:500", "minecraft:bone:100", "minecraft:wither_skeleton_skull:50", "minecraft:wither_skeleton_spawn_egg:1"), o -> o instanceof String);
        FARM_WOOD_PRODUCTS = builder.defineList("wood", () -> List.of("minecraft:oak_log:500", "minecraft:birch_log:50", "minecraft:spruce_log:50", "minecraft:jungle_log:50", "minecraft:acacia_log:50", "minecraft:dark_oak_log:50", "minecraft:mangrove_log:50", "minecraft:cherry_log:50", "minecraft:apple:10"), o -> o instanceof String);
        FARM_ZOMBIE_PRODUCTS = builder.defineList("zombie", () -> List.of("minecraft:rotten_flesh:500", "minecraft:iron_ingot:250", "minecraft:carrot:10", "minecraft:potato:10", "minecraft:zombie_spawn_egg:1"), o -> o instanceof String);
        FARM_ZOMBIFIED_PIGLIN_PRODUCTS = builder.defineList("zombified_piglin", () -> List.of("minecraft:rotten_flesh:500", "minecraft:gold_nugget:250", "minecraft:gold_ingot:250", "minecraft:zombified_piglin_spawn_egg:1"), o -> o instanceof String);
        builder.pop();
        builder.pop();

        // ---- 液体无限制造机 ----
        builder.comment("液体无限制造机设置").push("liquid_fountain");
        LIQUID_FOUNTAIN_INFINITE_THRESHOLD = builder
                .comment("液体变为无限的数量阈值（mB）。达到此值后机器变为无限")
                .defineInRange("infinite_threshold", 10_000_000L, 1L, Long.MAX_VALUE);
        builder.pop();

        // ---- 存储方块制造机 ----
        builder.comment("存储方块制造机设置").push("storage_fountain");
        STORAGE_FOUNTAIN_MAX_ITEM_TYPES = builder
                .comment("最多可存储的物品类型数量")
                .defineInRange("max_item_types", 9, 1, Integer.MAX_VALUE);
        STORAGE_FOUNTAIN_CARRY = builder
                .comment("进位阈值。内部计数单位 = 此值")
                .defineInRange("carry", 1000L, 1L, Long.MAX_VALUE);
        STORAGE_FOUNTAIN_GROWTH_INTERVAL_SECONDS = builder
                .comment("每次增长间隔（秒）")
                .defineInRange("growth_interval_seconds", 20, 1, Integer.MAX_VALUE);
        STORAGE_FOUNTAIN_GROWTH_STEP = builder
                .comment("每次增长时 output 的增加量")
                .defineInRange("growth_step", 5L, 0L, Long.MAX_VALUE);
        STORAGE_FOUNTAIN_INITIAL_OUTPUT = builder
                .comment("初始产出速率")
                .defineInRange("initial_output", 5L, 0L, Long.MAX_VALUE);
        STORAGE_FOUNTAIN_ACCEPTED_MODS = builder
                .comment("额外接受的 MOD 命名空间列表")
                .defineList("accepted_mods",
                        () -> List.of("modern_industrialization", "extended_industrialization"),
                        o -> o instanceof String);
        STORAGE_FOUNTAIN_ACCEPTED_TAGS = builder
                .comment("接受的物品标签路径片段列表")
                .defineList("accepted_tags",
                        () -> List.of("storage_blocks", "ores", "ingots", "dusts", "gems", "alloys",
                                "plates", "enriched", "circuits", "pellets", "matter", "klein_star"),
                        o -> o instanceof String);
        builder.pop();

        SERVER_CONFIG = builder.build();
    }

    /**
     * 注册配置文件。必须在 Registration.init() 之前调用。
     */
    public static void init(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }
}
