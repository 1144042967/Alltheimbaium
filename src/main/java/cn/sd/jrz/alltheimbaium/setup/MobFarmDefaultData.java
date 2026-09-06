package cn.sd.jrz.alltheimbaium.setup;

import java.util.List;

/**
 * 生物农场专用 serverconfig 白名单的默认值（纯字面量，运行期不再读取旧专属农场的配方/DataConfig/Config）。
 * <p>
 * 特征物白名单：每一行 "实体id=标记物token;标记物token…"，token 可为 "命名空间:物品" 或 "tag:命名空间:标签"；
 * 产物白名单：每一行 "实体id=物品id:权重;物品id:权重…"。
 * 内容为改造前生物农场实际使用的清单（产物照搬原 33 个生物农场的产物与权重）。
 */
public final class MobFarmDefaultData {
    private MobFarmDefaultData() {
    }

    /** 特征物白名单默认值 */
    public static List<String> signatures() {
        return List.of(
                "minecraft:bee=minecraft:honeycomb;minecraft:honey_block",
                "minecraft:blaze=minecraft:blaze_rod",
                "minecraft:chicken=minecraft:egg;minecraft:feather",
                "minecraft:cow=minecraft:beef;minecraft:leather",
                "minecraft:creeper=minecraft:gunpowder",
                "minecraft:drowned=minecraft:copper_ingot",
                "minecraft:enderman=minecraft:ender_pearl",
                "minecraft:ender_dragon=minecraft:dragon_egg",
                "minecraft:evoker=minecraft:emerald",
                "minecraft:frog=minecraft:ochre_froglight;minecraft:pearlescent_froglight;minecraft:verdant_froglight",
                "minecraft:ghast=minecraft:ghast_tear",
                "minecraft:guardian=minecraft:prismarine_shard;minecraft:prismarine_crystals",
                "minecraft:hoglin=minecraft:cooked_porkchop",
                "minecraft:iron_golem=minecraft:iron_ingot",
                "minecraft:magma_cube=minecraft:magma_cream",
                "minecraft:phantom=minecraft:phantom_membrane",
                "minecraft:pig=minecraft:porkchop",
                "minecraft:pillager=minecraft:arrow;minecraft:crossbow",
                "minecraft:rabbit=minecraft:rabbit_hide;minecraft:rabbit;minecraft:rabbit_foot",
                "minecraft:ravager=minecraft:saddle",
                "minecraft:sheep=minecraft:mutton;tag:minecraft:wool",
                "minecraft:shulker=minecraft:shulker_shell",
                "minecraft:skeleton=minecraft:bone",
                "minecraft:slime=minecraft:slime_ball",
                "minecraft:spider=minecraft:string;minecraft:spider_eye",
                "minecraft:squid=minecraft:ink_sac;minecraft:glow_ink_sac",
                "minecraft:villager=tag:minecraft:beds;minecraft:emerald",
                "minecraft:warden=minecraft:sculk_catalyst;minecraft:sculk_shrieker;minecraft:sculk_sensor;minecraft:sculk_vein;minecraft:echo_shard",
                "minecraft:witch=minecraft:redstone",
                "minecraft:wither=minecraft:nether_star",
                "minecraft:wither_skeleton=minecraft:coal",
                "minecraft:zombie=minecraft:rotten_flesh",
                "minecraft:zombified_piglin=minecraft:gold_nugget");
    }

    /** 产物白名单默认值 */
    public static List<String> products() {
        return List.of(
                "minecraft:bee=minecraft:honeycomb:500;minecraft:honey_block:400;minecraft:bee_spawn_egg:1",
                "minecraft:blaze=minecraft:blaze_rod:500;minecraft:blaze_spawn_egg:1",
                "minecraft:chicken=minecraft:chicken:50;minecraft:feather:500;minecraft:egg:500;minecraft:chicken_spawn_egg:1",
                "minecraft:cow=minecraft:beef:50;minecraft:leather:500;minecraft:cow_spawn_egg:1",
                "minecraft:creeper=minecraft:gunpowder:500;minecraft:creeper_spawn_egg:1",
                "minecraft:drowned=minecraft:rotten_flesh:500;minecraft:copper_ingot:400;minecraft:trident:50;minecraft:nautilus_shell:50;minecraft:drowned_spawn_egg:1",
                "minecraft:enderman=minecraft:ender_pearl:500;minecraft:enderman_spawn_egg:1",
                "minecraft:ender_dragon=minecraft:dragon_egg:500;minecraft:dragon_breath:400;minecraft:ender_dragon_spawn_egg:1",
                "minecraft:evoker=minecraft:totem_of_undying:250;minecraft:emerald:500;minecraft:evoker_spawn_egg:1",
                "minecraft:frog=minecraft:ochre_froglight:500;minecraft:pearlescent_froglight:500;minecraft:verdant_froglight:500;minecraft:frog_spawn_egg:1",
                "minecraft:ghast=minecraft:ghast_tear:250;minecraft:gunpowder:500;minecraft:ghast_spawn_egg:1",
                "minecraft:guardian=minecraft:prismarine_shard:500;minecraft:prismarine_crystals:250;minecraft:cod:50;minecraft:salmon:50;minecraft:pufferfish:50;minecraft:tropical_fish:50;minecraft:sponge:50;minecraft:guardian_spawn_egg:1",
                "minecraft:hoglin=minecraft:cooked_porkchop:500;minecraft:leather:100;minecraft:hoglin_spawn_egg:1",
                "minecraft:iron_golem=minecraft:iron_ingot:500;minecraft:poppy:50;minecraft:iron_golem_spawn_egg:1",
                "minecraft:magma_cube=minecraft:magma_cream:500;minecraft:magma_cube_spawn_egg:1",
                "minecraft:phantom=minecraft:phantom_membrane:500;minecraft:phantom_spawn_egg:1",
                "minecraft:pig=minecraft:porkchop:500;minecraft:pig_spawn_egg:1",
                "minecraft:pillager=minecraft:arrow:500;minecraft:experience_bottle:10;minecraft:pillager_spawn_egg:1",
                "minecraft:rabbit=minecraft:rabbit_hide:100;minecraft:rabbit:300;minecraft:rabbit_foot:100;minecraft:rabbit_spawn_egg:1",
                "minecraft:ravager=minecraft:saddle:500;minecraft:ravager_spawn_egg:1",
                "minecraft:sheep=minecraft:mutton:50;minecraft:white_wool:500;minecraft:light_gray_wool:50;minecraft:gray_wool:50;minecraft:black_wool:50;minecraft:brown_wool:50;minecraft:red_wool:50;minecraft:orange_wool:50;minecraft:yellow_wool:50;minecraft:lime_wool:50;minecraft:green_wool:50;minecraft:cyan_wool:50;minecraft:light_blue_wool:50;minecraft:blue_wool:50;minecraft:purple_wool:50;minecraft:magenta_wool:50;minecraft:pink_wool:50;minecraft:sheep_spawn_egg:1",
                "minecraft:shulker=minecraft:shulker_shell:500;minecraft:shulker_spawn_egg:1",
                "minecraft:skeleton=minecraft:bone:500;minecraft:arrow:100;minecraft:skeleton_spawn_egg:1",
                "minecraft:slime=minecraft:slime_ball:500;minecraft:slime_spawn_egg:1",
                "minecraft:spider=minecraft:string:500;minecraft:spider_eye:50;minecraft:spider_spawn_egg:1",
                "minecraft:squid=minecraft:ink_sac:500;minecraft:glow_ink_sac:200;minecraft:squid_spawn_egg:1;minecraft:glow_squid_spawn_egg:1",
                "minecraft:villager=minecraft:villager_spawn_egg:500",
                "minecraft:warden=minecraft:sculk_catalyst:100;minecraft:sculk_shrieker:100;minecraft:sculk_sensor:100;minecraft:sculk_vein:100;minecraft:echo_shard:100;minecraft:warden_spawn_egg:1",
                "minecraft:witch=minecraft:redstone:500;minecraft:glass_bottle:50;minecraft:glowstone_dust:50;minecraft:gunpowder:50;minecraft:spider_eye:50;minecraft:sugar:50;minecraft:stick:50;minecraft:witch_spawn_egg:1",
                "minecraft:wither=minecraft:nether_star:500;minecraft:wither_spawn_egg:500",
                "minecraft:wither_skeleton=minecraft:coal:500;minecraft:bone:100;minecraft:wither_skeleton_skull:50;minecraft:wither_skeleton_spawn_egg:1",
                "minecraft:zombie=minecraft:rotten_flesh:500;minecraft:iron_ingot:250;minecraft:carrot:10;minecraft:potato:10;minecraft:zombie_spawn_egg:1",
                "minecraft:zombified_piglin=minecraft:rotten_flesh:500;minecraft:gold_nugget:250;minecraft:gold_ingot:250;minecraft:zombified_piglin_spawn_egg:1");
    }
}
