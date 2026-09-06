package cn.sd.jrz.alltheimbaium.setup;

import java.util.List;

/**
 * 通用资源农场 serverconfig 白名单的默认值（纯字面量）。
 * <p>
 * 每行格式：{@code 资源id=标记物token;…|物品id:权重;…}
 * | 前为放入标记槽可标记该资源的标记物（物品id 或 tag:标签id，多个用 ; 分隔，首个即"代表标记物"用于展示），
 * | 后为该资源的白名单产物（物品id:权重，权重 500≈1件/s@Lv1）。
 */
public final class ResourceDefaultData {
    private ResourceDefaultData() {
    }

    /** 资源白名单默认值 */
    public static List<String> whitelist() {
        return List.of(
                "cobblestone=minecraft:cobblestone|minecraft:cobblestone:500",
                "wood=tag:minecraft:saplings|minecraft:oak_log:500;minecraft:birch_log:50;minecraft:spruce_log:50;minecraft:jungle_log:50;minecraft:acacia_log:50;minecraft:dark_oak_log:50;minecraft:mangrove_log:50;minecraft:cherry_log:50;minecraft:apple:10",
                "bamboo=minecraft:bamboo|minecraft:bamboo:500",
                "sugar_cane=minecraft:sugar_cane|minecraft:sugar_cane:500",
                "ice=minecraft:ice|minecraft:ice:400;minecraft:packed_ice:80;minecraft:blue_ice:30",
                "bone_meal=minecraft:bone_meal|minecraft:bone_meal:500",
                "flowers=minecraft:poppy;minecraft:dandelion;minecraft:blue_orchid;minecraft:allium;minecraft:azure_bluet;minecraft:red_tulip;minecraft:orange_tulip;minecraft:white_tulip;minecraft:pink_tulip;minecraft:oxeye_daisy;minecraft:cornflower;minecraft:lily_of_the_valley;minecraft:wither_rose|minecraft:poppy:50;minecraft:dandelion:50;minecraft:blue_orchid:50;minecraft:allium:50;minecraft:azure_bluet:50;minecraft:red_tulip:50;minecraft:orange_tulip:50;minecraft:white_tulip:50;minecraft:pink_tulip:50;minecraft:oxeye_daisy:50;minecraft:cornflower:50;minecraft:lily_of_the_valley:50;minecraft:wither_rose:20",
                "cactus=minecraft:cactus|minecraft:cactus:500",
                "mushroom=minecraft:red_mushroom;minecraft:brown_mushroom|minecraft:red_mushroom:250;minecraft:brown_mushroom:250",
                "chorus=minecraft:chorus_fruit|minecraft:chorus_fruit:500;minecraft:chorus_flower:60;minecraft:popped_chorus_fruit:60",
                "cocoa=minecraft:cocoa_beans|minecraft:cocoa_beans:500",
                "melon=minecraft:melon_seeds|minecraft:melon_slice:500;minecraft:melon:100;minecraft:melon_seeds:100",
                "pumpkin=minecraft:pumpkin_seeds|minecraft:pumpkin:300;minecraft:pumpkin_seeds:100",
                "sniffer=minecraft:sniffer_egg|minecraft:torchflower:100;minecraft:pitcher_plant:100;minecraft:torchflower_seeds:300;minecraft:pitcher_pod:300",
                "coral=minecraft:tube_coral_fan;minecraft:brain_coral_fan;minecraft:bubble_coral_fan;minecraft:fire_coral_fan;minecraft:horn_coral_fan|minecraft:tube_coral_fan:120;minecraft:brain_coral_fan:120;minecraft:bubble_coral_fan:120;minecraft:fire_coral_fan:120;minecraft:horn_coral_fan:120;minecraft:tube_coral:80;minecraft:brain_coral:80;minecraft:bubble_coral:80;minecraft:fire_coral:80;minecraft:horn_coral:80;minecraft:tube_coral_block:40;minecraft:brain_coral_block:40;minecraft:bubble_coral_block:40;minecraft:fire_coral_block:40;minecraft:horn_coral_block:40",
                "moss=minecraft:moss_block|minecraft:moss_block:250;minecraft:moss_carpet:80;minecraft:azalea:60;minecraft:flowering_azalea_leaves:40;minecraft:rooted_dirt:120",
                "pointed_dripstone=minecraft:pointed_dripstone|minecraft:pointed_dripstone:300;minecraft:dripstone_block:200",
                "amethyst=minecraft:amethyst_shard|minecraft:amethyst_shard:500;minecraft:budding_amethyst:20;minecraft:small_amethyst_bud:80;minecraft:medium_amethyst_bud:80;minecraft:large_amethyst_bud:80;minecraft:amethyst_cluster:20",
                "nether_wart=minecraft:nether_wart|minecraft:nether_wart:500",
                "sweet_berries=minecraft:sweet_berries|minecraft:sweet_berries:500",
                "glow_berries=minecraft:glow_berries|minecraft:glow_berries:500",
                "snowball=minecraft:snowball|minecraft:snowball:500;minecraft:snow_block:50",
                "music_disc=tag:minecraft:music_discs|minecraft:music_disc_13:20;minecraft:music_disc_cat:20;minecraft:music_disc_blocks:20;minecraft:music_disc_chirp:20;minecraft:music_disc_far:20;minecraft:music_disc_mall:20;minecraft:music_disc_mellohi:20;minecraft:music_disc_stal:20;minecraft:music_disc_strad:20;minecraft:music_disc_ward:20;minecraft:music_disc_11:20;minecraft:music_disc_wait:20;minecraft:music_disc_otherside:20;minecraft:music_disc_5:20;minecraft:music_disc_pigstep:20;minecraft:music_disc_relic:20");
    }
}
