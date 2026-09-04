package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生物农场白名单与特征掉落物知识库（纯查表，不写盘）。
 * <p>
 * 白名单 = 现有 33 个"生物农场"对应的原版生物；"特征掉落物" = 该生物现有 farm 合成配方
 * main/farm_*.json 的中心材料（标签类中心物用具体物品替代）。均用于 GUI 标记槽把生物收容进机器。
 */
public final class MobFarmCatalog {
    private static final Logger log = LoggerFactory.getLogger(MobFarmCatalog.class);

    /** 白名单：EntityType → 该生物现有农场的数据配置（其产物列表即"白名单额外掉落"来源） */
    private static final Map<EntityType<?>, DataConfig> WHITELIST = new LinkedHashMap<>();
    /** 特征掉落物：EntityType → Item */
    private static final Map<EntityType<?>, Item> SIGNATURE = new HashMap<>();

    // 刷怪蛋缓存：懒扫描一次（服务端收容/建产物表时才调用）
    private static Map<EntityType<?>, Item> eggCache = null;

    private MobFarmCatalog() {
    }

    // ==================== 白名单 / 特征物构建 ====================

    private static void add(String entityId, DataConfig config, String signatureItemId) {
        try {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(entityId));
            if (type == null) {
                log.warn("MobFarmCatalog: 找不到实体类型 {}", entityId);
                return;
            }
            WHITELIST.put(type, config);
            Item signature = BuiltInRegistries.ITEM.get(new ResourceLocation(signatureItemId));
            if (signature != null) {
                SIGNATURE.put(type, signature);
            }
        } catch (Throwable e) {
            log.warn("MobFarmCatalog.add error entity={}", entityId, e);
        }
    }

    static {
        // 与 DataConfig.java / Config.java 中 33 个生物农场一一对应
        add("minecraft:bee", DataConfig.FARM_BEE, "minecraft:honeycomb");
        add("minecraft:blaze", DataConfig.FARM_BLAZE, "minecraft:blaze_rod");
        add("minecraft:chicken", DataConfig.FARM_CHICKEN, "minecraft:feather");
        add("minecraft:cow", DataConfig.FARM_COW, "minecraft:beef");
        add("minecraft:creeper", DataConfig.FARM_CREEPER, "minecraft:gunpowder");
        add("minecraft:drowned", DataConfig.FARM_DROWNED, "minecraft:copper_ingot");
        add("minecraft:enderman", DataConfig.FARM_ENDERMAN, "minecraft:ender_pearl");
        add("minecraft:ender_dragon", DataConfig.FARM_ENDER_DRAGON, "minecraft:dragon_egg");
        add("minecraft:evoker", DataConfig.FARM_EVOKER, "minecraft:emerald");
        add("minecraft:frog", DataConfig.FARM_FROG, "minecraft:ochre_froglight");
        add("minecraft:ghast", DataConfig.FARM_GHAST, "minecraft:ghast_tear");
        add("minecraft:guardian", DataConfig.FARM_GUARDIAN, "minecraft:prismarine_shard");
        add("minecraft:hoglin", DataConfig.FARM_HOGLIN, "minecraft:cooked_porkchop");
        add("minecraft:iron_golem", DataConfig.FARM_IRON_GOLEM, "minecraft:iron_ingot");
        add("minecraft:magma_cube", DataConfig.FARM_MAGMA_CUBE, "minecraft:magma_cream");
        add("minecraft:phantom", DataConfig.FARM_PHANTOM, "minecraft:phantom_membrane");
        add("minecraft:pig", DataConfig.FARM_PIG, "minecraft:porkchop");
        add("minecraft:pillager", DataConfig.FARM_PILLAGER, "minecraft:arrow");
        add("minecraft:rabbit", DataConfig.FARM_RABBIT, "minecraft:rabbit_foot");
        add("minecraft:ravager", DataConfig.FARM_RAVAGER, "minecraft:saddle");
        add("minecraft:sheep", DataConfig.FARM_SHEEP, "minecraft:mutton");
        add("minecraft:shulker", DataConfig.FARM_SHULKER, "minecraft:shulker_shell");
        add("minecraft:skeleton", DataConfig.FARM_SKELETON, "minecraft:bone");
        add("minecraft:slime", DataConfig.FARM_SLIME, "minecraft:slime_ball");
        add("minecraft:spider", DataConfig.FARM_SPIDER, "minecraft:string");
        add("minecraft:squid", DataConfig.FARM_SQUID, "minecraft:ink_sac");
        add("minecraft:villager", DataConfig.FARM_VILLAGER, "minecraft:emerald");
        add("minecraft:warden", DataConfig.FARM_WARDEN, "minecraft:echo_shard");
        add("minecraft:witch", DataConfig.FARM_WITCH, "minecraft:redstone");
        add("minecraft:wither", DataConfig.FARM_WITHER, "minecraft:nether_star");
        add("minecraft:wither_skeleton", DataConfig.FARM_WITHER_SKELETON, "minecraft:coal");
        add("minecraft:zombie", DataConfig.FARM_ZOMBIE, "minecraft:rotten_flesh");
        add("minecraft:zombified_piglin", DataConfig.FARM_ZOMBIFIED_PIGLIN, "minecraft:gold_nugget");
    }

    // ==================== 查询 ====================

    public static boolean isWhitelisted(@Nonnull EntityType<?> type) {
        return WHITELIST.containsKey(type);
    }

    @Nullable
    public static DataConfig configFor(@Nonnull EntityType<?> type) {
        return WHITELIST.get(type);
    }

    /** 生物的特征掉落物（标记槽候选），白名单外返回 null */
    @Nullable
    public static Item signatureOf(@Nonnull EntityType<?> type) {
        return SIGNATURE.get(type);
    }

    /** 该物品是否为某白名单生物的特征掉落物；是则返回对应实体类型 */
    @Nullable
    public static EntityType<?> typeOfSignature(@Nonnull Item item) {
        for (Map.Entry<EntityType<?>, Item> entry : SIGNATURE.entrySet()) {
            if (entry.getValue() == item) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** 生成该生物对应的刷怪蛋物品（没有则返回 null）。懒扫描一次物品注册表。 */
    @Nullable
    public static Item spawnEggOf(@Nonnull EntityType<?> type) {
        try {
            if (eggCache == null) {
                Map<EntityType<?>, Item> map = new HashMap<>();
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item instanceof SpawnEggItem egg) {
                        //noinspection deprecation
                        EntityType<?> eggType = egg.getType(null);
                        if (eggType != null) {
                            map.put(eggType, item);
                        }
                    }
                }
                eggCache = map;
            }
            return eggCache.get(type);
        } catch (Throwable e) {
            log.error("MobFarmCatalog.spawnEggOf error", e);
        }
        return null;
    }

    /** 物品是否为某实体类型的刷怪蛋 */
    public static boolean isSpawnEggFor(@Nonnull ItemStack stack, @Nonnull EntityType<?> type) {
        if (!(stack.getItem() instanceof SpawnEggItem egg)) {
            return false;
        }
        //noinspection deprecation
        return egg.getType(null) == type;
    }
}
