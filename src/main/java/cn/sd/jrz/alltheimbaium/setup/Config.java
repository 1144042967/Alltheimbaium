package cn.sd.jrz.alltheimbaium.setup;

import cn.sd.jrz.alltheimbaium.block.FarmlandBlock;
import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.block.PlatformBlock;
import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.ClockEntity;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.item.EternalTotemItem;
import cn.sd.jrz.alltheimbaium.item.StorageFountainItem;
import cn.sd.jrz.alltheimbaium.item.TotemEventHandler;
import cn.sd.jrz.alltheimbaium.recipe.PotionCombineRecipe;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

/**
 * 配置文件。使用 ForgeConfigSpec，参考 auto-resource 项目的配置模式。
 * 配置类型为 SERVER（每世界可不同），在 Alltheimbaium 构造器中注册。
 * <p>
 * 监听 {@link ModConfigEvent.Loading} 事件，在 Forge 完成配置文件加载后，
 * 将配置值统一分发到各模块的静态字段中，确保运行时无需直接调用 Config.get()。
 */
@Mod.EventBusSubscriber(modid = "alltheimbaium", bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    // ==================== ATI 耕地 ====================
    public static ForgeConfigSpec.IntValue FARMLAND_TICK_INTERVAL;
    public static ForgeConfigSpec.IntValue FARMLAND_GROWTH_AMOUNT;
    public static ForgeConfigSpec.BooleanValue FARMLAND_BONEMEAL_ENABLED;
    public static ForgeConfigSpec.IntValue FARMLAND_BONEMEAL_INTERVAL;

    // ==================== 时钟方块 ====================
    public static ForgeConfigSpec.BooleanValue CLOCK_DEFAULT_ACTIVE;

    // ==================== 永恒图腾 ====================
    public static ForgeConfigSpec.BooleanValue ETERNAL_TOTEM_DEFAULT_ENABLED;
    public static ForgeConfigSpec.BooleanValue ETERNAL_TOTEM_TANK_CONVERSION;

    // ==================== 液体无限制造机 ====================
    public static ForgeConfigSpec.LongValue LIQUID_FOUNTAIN_INFINITE_THRESHOLD;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> LIQUID_FOUNTAIN_AUTO_INFINITE_MODS;

    // ==================== 混合药水合成 ====================
    public static ForgeConfigSpec.DoubleValue POTION_COMBINE_DURATION_FACTOR;

    // ==================== 存储方块制造机 ====================
    public static ForgeConfigSpec.IntValue STORAGE_FOUNTAIN_MAX_ITEM_TYPES;
    public static ForgeConfigSpec.LongValue STORAGE_FOUNTAIN_CARRY;
    public static ForgeConfigSpec.IntValue STORAGE_FOUNTAIN_GROWTH_INTERVAL_SECONDS;
    public static ForgeConfigSpec.LongValue STORAGE_FOUNTAIN_GROWTH_STEP;
    public static ForgeConfigSpec.LongValue STORAGE_FOUNTAIN_INITIAL_OUTPUT;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> STORAGE_FOUNTAIN_ACCEPTED_MODS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> STORAGE_FOUNTAIN_ACCEPTED_TAGS;
    /** 物品白名单：完整注册 ID，命中则无视标签/命名空间规则直接接受 */
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> STORAGE_FOUNTAIN_ACCEPTED_ITEMS;

    // ==================== 生物农场 ====================
    public static ForgeConfigSpec.IntValue MOB_FARM_LEVEL_UP_INTERVAL_SECONDS;
    public static ForgeConfigSpec.LongValue MOB_FARM_CARRY;
    public static ForgeConfigSpec.LongValue MOB_FARM_INITIAL_LEVEL;
    public static ForgeConfigSpec.LongValue MOB_FARM_MAX_LEVEL;
    public static ForgeConfigSpec.IntValue MOB_FARM_CAPTURE_RADIUS;
    public static ForgeConfigSpec.IntValue MOB_FARM_MAX_PRODUCTS;
    public static ForgeConfigSpec.IntValue MOB_FARM_SAMPLE_KILLS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_FARM_SIGNATURES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_FARM_PRODUCTS;

    // ==================== 生成平台 ====================
    /** 平台伪装全局开关（所有世界共用，由平台 GUI 切换并保存） */
    public static ForgeConfigSpec.BooleanValue PLATFORM_DISGUISE_ENABLED;
    /** 已加载的 SERVER 配置实例，运行时切换伪装后用于回写保存配置文件 */
    public static ModConfig SERVER_MOD_CONFIG;

    // ==================== ATI 补给箱 ====================
    /** 补给箱随机物品黑名单（物品注册 ID），默认含基岩、末地传送门框架 */
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> SUPPLY_CRATE_BLACKLIST;

    // ==================== 通用资源农场 ====================
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> RESOURCE_WHITELIST;

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
                .comment("进入游戏后所有时钟的全局开关初始状态。true=启用，false=禁用")
                .define("default_active", true);
        builder.pop();

        // ---- 永恒图腾 ----
        builder.comment("永恒图腾设置").push("eternal_totem");
        ETERNAL_TOTEM_DEFAULT_ENABLED = builder
                .comment("永恒图腾的初始开关状态。true=启用，false=禁用")
                .define("default_enabled", true);
        ETERNAL_TOTEM_TANK_CONVERSION = builder
                .comment("是否允许永恒图腾右键 Mekanism 终极化学品储罐升级为创造化学品储罐")
                .define("tank_conversion", true);
        builder.pop();


        // ---- 液体无限制造机 ----
        builder.comment("液体无限制造机设置").push("liquid_fountain");
        LIQUID_FOUNTAIN_INFINITE_THRESHOLD = builder
                .comment("液体变为无限的数量阈值（mB）。达到此值后机器变为无限")
                .defineInRange("infinite_threshold", 10_000_000L, 1L, Long.MAX_VALUE);
        LIQUID_FOUNTAIN_AUTO_INFINITE_MODS = builder
                .comment("这些 MOD 命名空间的流体在输入后直接变为无限（支持部分匹配）")
                .defineList("auto_infinite_mods",
                        () -> List.of("modern_industrialization", "extended_industrialization"),
                        o -> o instanceof String);
        builder.pop();

        // ---- 混合药水合成 ----
        builder.comment("混合药水合成设置").push("potion_combine");
        POTION_COMBINE_DURATION_FACTOR = builder
                .comment("两瓶药水同等级效果合并时，持续时间系数。公式: (时间A + 时间B) × 此系数")
                .defineInRange("duration_factor", 0.75, 0.5, 1.0);
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
        STORAGE_FOUNTAIN_ACCEPTED_ITEMS = builder
                .comment("物品白名单：完整物品注册 ID（如 minecraft:diamond）。列出的物品无视上面的标签与命名空间规则，始终可以被标记复制")
                .defineList("accepted_items",
                        List::of,
                        o -> o instanceof String);
        builder.pop();

        // ---- 生物农场 ----
        builder.comment("生物农场设置").push("mob_farm");
        MOB_FARM_LEVEL_UP_INTERVAL_SECONDS = builder
                .comment("等级增长间隔（秒）")
                .defineInRange("level_up_interval_seconds", 20, 1, Integer.MAX_VALUE);
        MOB_FARM_CARRY = builder
                .comment("进位阈值。权重累计至此进位为一件完整物品")
                .defineInRange("carry", 10000L, 1L, Long.MAX_VALUE);
        MOB_FARM_INITIAL_LEVEL = builder
                .comment("初始等级")
                .defineInRange("initial_level", 1L, 1L, Long.MAX_VALUE);
        MOB_FARM_MAX_LEVEL = builder
                .comment("最大等级（达到后不再升级）")
                .defineInRange("max_level", Long.MAX_VALUE, 1L, Long.MAX_VALUE);
        MOB_FARM_CAPTURE_RADIUS = builder
                .comment("空罐右键捕捉生物的半径（格）")
                .defineInRange("capture_radius", 5, 1, 64);
        MOB_FARM_MAX_PRODUCTS = builder
                .comment("最多产物行数 / 输出槽数（≤27）")
                .defineInRange("max_products", 27, 1, 27);
        MOB_FARM_SAMPLE_KILLS = builder
                .comment("击杀掉落采样次数（收容时估算权重用，越大越稳定）")
                .defineInRange("sample_kills", 2000, 10, 100000);
        MOB_FARM_SIGNATURES = builder
                .comment("生物农场特征物白名单：每行 \"实体id=标记物token;标记物token…\"（token 为 物品id 或 tag:标签id）。放入标记槽可收容对应生物。旧专属农场删除后仍由本清单生效")
                .defineList("signature_whitelist", MobFarmDefaultData::signatures, o -> o instanceof String);
        MOB_FARM_PRODUCTS = builder
                .comment("生物农场产物白名单：每行 \"实体id=物品id:权重;物品id:权重…\"，为收容该生物后机器额外稳定产出的白名单产物（击杀掉落/刷怪蛋仍会补充）")
                .defineList("product_whitelist", MobFarmDefaultData::products, o -> o instanceof String);
        builder.pop();

        // ---- 生成平台 ----
        builder.comment("生成平台设置").push("platform");
        PLATFORM_DISGUISE_ENABLED = builder
                .comment("平台伪装全局开关（所有世界共用）。开启后所有生成平台的上表面贴图显示为石砖；由平台 GUI 切换并保存，重进仍生效")
                .define("disguise_enabled", false);
        builder.pop();

        // ---- ATI 补给箱 ----
        builder.comment("ATI 补给箱设置").push("supply_crate");
        SUPPLY_CRATE_BLACKLIST = builder
                .comment("补给箱随机物品黑名单（物品注册 ID，如 minecraft:bedrock）。列出的物品不会被随机列出")
                .defineList("blacklist",
                        () -> List.of("minecraft:bedrock", "minecraft:end_portal_frame"),
                        o -> o instanceof String);
        builder.pop();

        // ---- 通用资源农场 ----
        builder.comment("通用资源农场设置").push("resource_farm");
        RESOURCE_WHITELIST = builder
                .comment("资源白名单：每行 \"资源id=标记物token;…|物品id:权重;…\"。| 前为放入标记槽可标记该资源的标记物（物品id 或 tag:标签id），后为该资源的白名单产物（权重 500≈1件/s@Lv1）。旧专属资源农场删除后仍由本清单生效")
                .defineList("whitelist", ResourceDefaultData::whitelist, o -> o instanceof String);
        builder.pop();

        SERVER_CONFIG = builder.build();
    }

    /**
     * 注册配置文件。必须在 Registration.init() 之前调用。
     */
    public static void init(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }

    /**
     * 配置文件加载完成后，将配置值一次性分发到各模块的静态字段中。
     * 此后运行时逻辑读取各模块自身的本地字段，不再调用 Config.xxx.get()。
     */
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SERVER_CONFIG) {
            // 保存配置实例引用，供运行时回写（如平台伪装开关切换）
            SERVER_MOD_CONFIG = event.getConfig();
            FarmlandBlock.loadConfig();
            ClockEntity.loadConfig();
            EternalTotemItem.loadConfig();
            TotemEventHandler.loadConfig();
            LiquidFountainBlock.loadConfig();
            PlatformBlock.loadConfig();
            SupplyRoll.loadConfig();
            StorageFountainBlock.loadConfig();
            StorageFountainBlock.loadConfig();
            StorageFountainEntity.loadConfig();
            MobFarmBlock.loadConfig();
            PotionCombineRecipe.loadConfig();
        }
    }
}
