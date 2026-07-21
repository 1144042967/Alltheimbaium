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
