# Alltheimbaium 配方总表

> **本文件由 `tools/gen_recipe_docs.py` 自动生成，请勿手工修改。**
> 配方有增删改后重新执行 `python tools/gen_recipe_docs.py` 即可同步。

- 共 **213** 个配方文件、**213** 条配方
- 物品一律写**注册名**；数量省略表示 1
- 物品流向一律写作：**产物 ← 材料**
- 带「前置」的条目被 `forge:conditional` 包裹，**未安装对应 MOD 时该配方根本不存在**

## 总览

| 分类 | 目录 | 配方数 | 涉及 MOD |
|------|------|-------:|----------|
| 本模组机器 | `recipe/main/` | 17 | — |
| 原版简化 | `recipe/crafting/` | 12 | — |
| 拆解回收 | `recipe/salvaging/` | 34 | — |
| AE2 / 高级 AE | `recipe/ae2/` | 24 | — |
| 通用机械 Mekanism | `recipe/mek/` | 18 | — |
| 热力系列 Thermal | `recipe/thermal/` | 28 | — |
| 神秘农业 Mystical Agriculture | `recipe/mystical/` | 14 | — |
| 血魔法 Blood Magic | `recipe/blood/` | 5 | — |
| 机械动力 Create | `recipe/create/` | 1 | — |
| 龙之研究 Draconic Evolution | `recipe/draconicevolution/` | 1 | — |
| advanced_ae | `recipe/advanced_ae/` | 3 | — |
| evilcraft | `recipe/evilcraft/` | 1 | — |
| extendedae | `recipe/extendedae/` | 15 | — |
| forbidden_arcanus | `recipe/forbidden_arcanus/` | 6 | — |
| jdt | `recipe/jdt/` | 21 | — |
| smelting | `recipe/smelting/` | 10 | — |
| 本模组自定义配方 | `recipe/*.json` | 3 | — |

## 目录

1. [本模组机器（17 条）](#1-本模组机器)
2. [原版简化（12 条）](#2-原版简化)
3. [拆解回收（34 条）](#3-拆解回收)
4. [AE2 / 高级 AE（24 条）](#4-ae2--高级-ae)
5. [通用机械 Mekanism（18 条）](#5-通用机械-mekanism)
6. [热力系列 Thermal（28 条）](#6-热力系列-thermal)
7. [神秘农业 Mystical Agriculture（14 条）](#7-神秘农业-mystical-agriculture)
8. [血魔法 Blood Magic（5 条）](#8-血魔法-blood-magic)
9. [机械动力 Create（1 条）](#9-机械动力-create)
10. [龙之研究 Draconic Evolution（1 条）](#10-龙之研究-draconic-evolution)
11. [advanced_ae（3 条）](#11-advanced_ae)
12. [evilcraft（1 条）](#12-evilcraft)
13. [extendedae（15 条）](#13-extendedae)
14. [forbidden_arcanus（6 条）](#14-forbidden_arcanus)
15. [jdt（21 条）](#15-jdt)
16. [smelting（10 条）](#16-smelting)
17. [本模组自定义配方（3 条）](#17-本模组自定义配方)

## 1. 本模组机器

目录：`src/main/resources/data/alltheimbaium/recipe/main/`

### `auto_farmland.json`

- **`alltheimbaium:auto_farmland`**  ·  图案 `FFF` / `XRX` / `FFF`
  - 键位：`X`=`alltheimbaium:package_material`×2、`F`=`alltheimbaium:farmland`×6、`R`=`minecraft:redstone`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `clock.json`

- **`alltheimbaium:clock`**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`、`B`=`minecraft:iron_block`×7、`C`=`minecraft:clock`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `creative_transmuter.json`

- **`alltheimbaium:creative_transmuter`**  ·  图案 `NNN` / `NPN` / `NNN`
  - 键位：`N`=`minecraft:netherite_block`×8、`P`=`alltheimbaium:package_material`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `eternal_sword.json`

- **`alltheimbaium:eternal_sword`**  ·  图案 `ABA` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`A`=`minecraft:netherite_block`×2、`B`=`minecraft:golden_apple`×4、`C`=`minecraft:netherite_sword`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `eternal_totem.json`

- **`alltheimbaium:eternal_totem`**  ·  图案 `ABA` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`A`=`minecraft:netherite_block`×2、`B`=`minecraft:golden_apple`×4、`C`=`minecraft:totem_of_undying`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `extraction_interface.json`

- **`alltheimbaium:extraction_interface`**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:iron_ingot`×6、`C`=`minecraft:redstone_block`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `farmland.json`

- **`alltheimbaium:farmland`**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`、`B`=`minecraft:iron_ingot`×7、`C`=`minecraft:dirt`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `instant_furnace.json`

- **`alltheimbaium:instant_furnace`**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:iron_block`×6、`C`=`minecraft:furnace`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `instant_inscriber.json`

- **`alltheimbaium:instant_inscriber`**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:iron_block`×6、`C`=`minecraft:smithing_table`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `liquid_fountain.json`

- **`alltheimbaium:liquid_fountain`**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:bucket`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `mob_farm.json`

- **`alltheimbaium:mob_farm`**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:glass_bottle`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `obsidian_from_buckets.json`

- **`minecraft:obsidian`**  ←  `minecraft:water_bucket` + `minecraft:lava_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `package_material_1.json`

- **`alltheimbaium:package_material`**  ·  图案 `AAA` / `ABA` / `AAA`
  - 键位：`A`=`#minecraft:planks`×8、`B`=`#c:cobblestones`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `package_material_2.json`

- **`alltheimbaium:package_material`**  ·  图案 `AAA` / `ABA` / `AAA`
  - 键位：`A`=`#c:cobblestones`×8、`B`=`#minecraft:planks`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `platform.json`

- **`alltheimbaium:platform`**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`、`B`=`minecraft:iron_ingot`×7、`C`=`minecraft:smooth_stone`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `resource_farm.json`

- **`alltheimbaium:resource_farm`**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:dirt`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `storage_fountain.json`

- **`alltheimbaium:storage_fountain`**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:iron_block`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 2. 原版简化

目录：`src/main/resources/data/alltheimbaium/recipe/crafting/`

### `allthemodium_upgrade_smithing_template.json`

- **`allthemodium:allthemodium_upgrade_smithing_template`**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:allthemodium_ingot`、`B`=`minecraft:sculk`、`#`=`minecraft:netherite_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `budding_amethyst.json`

- **`minecraft:budding_amethyst`**  ·  图案 `##` / `##`
  - 键位：`#`=`minecraft:amethyst_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `certus_quartz_dust.json`

- **`ae2:certus_quartz_dust`**  ←  `misc` + `dust` + `enderio:powdered_quartz` + `minecraft:water_bucket` + `enderio` + `ae2`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `moss_block.json`

- **`minecraft:moss_block`**  ·  图案 `##` / `##`
  - 键位：`#`=`minecraft:wheat_seeds`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `netherite_upgrade_smithing_template.json`

- **`minecraft:netherite_upgrade_smithing_template`**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`minecraft:netherite_ingot`、`B`=`minecraft:netherrack`、`#`=`minecraft:diamond`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `obsidian_from_buckets.json`

- **`minecraft:obsidian`**  ←  `misc` + `minecraft:water_bucket` + `minecraft:lava_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `powdered_quartz.json`

- **`enderio:powdered_quartz`**  ←  `misc` + `dust` + `ae2:certus_quartz_dust` + `minecraft:lava_bucket` + `ae2` + `enderio`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `spawner.json`

- **`minecraft:spawner`**  ·  图案 `###` / `#A#` / `###`
  - 键位：`A`=`minecraft:nether_star`、`#`=`minecraft:iron_chain`×8
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `supply_crate.json`

- **`alltheimbaium:supply_crate`**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:chest`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `unobtainium_upgrade_smithing_template.json`

- **`allthemodium:unobtainium_upgrade_smithing_template`**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:unobtainium_ingot`、`B`=`allthemodium:piglich_heart`、`#`=`allthemodium:vibranium_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `vibranium_upgrade_smithing_template.json`

- **`allthemodium:vibranium_upgrade_smithing_template`**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:vibranium_ingot`、`B`=`minecraft:gilded_blackstone`、`#`=`allthemodium:allthemodium_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `wheat_seeds.json`

- **`minecraft:wheat_seeds`**  ←  `misc` + `seed` + `minecraft:wheat`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

## 3. 拆解回收

目录：`src/main/resources/data/alltheimbaium/recipe/salvaging/`

### `allthemodium_axe.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_axe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_boots.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_boots` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_chestplate.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_chestplate` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_helmet.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_helmet` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_hoe.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_hoe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_leggings.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_leggings` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_mace.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `silentgear` + `allthemodium` + `allthemodium:allthemodium_mace`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_pickaxe.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_pickaxe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_shovel.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_shovel` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_sword.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_sword` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `enchanted_book.json`

- **`minecraft:book` + `minecraft:experience_bottle`**  ←  `minecraft:enchanted_book` + `silentgear`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `fishing_rod.json`

- **`minecraft:string`×2 + `minecraft:stick`×3**  ←  `minecraft:fishing_rod` + `silentgear`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `potion.json`

- **`minecraft:glass_bottle` + `mysticalagriculture:water_essence`**  ←  `minecraft:potion` + `silentgear` + `mysticalagriculture`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `saddle.json`

- **`minecraft:leather`×2**  ←  `minecraft:saddle` + `silentgear`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_axe.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_axe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_boots.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_boots` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_chestplate.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_chestplate` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_helmet.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_helmet` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_hoe.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_hoe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_leggings.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_leggings` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_mace.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `silentgear` + `allthemodium` + `allthemodium:unobtainium_mace`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_pickaxe.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_pickaxe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_shovel.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_shovel` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_sword.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_sword` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_axe.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_axe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_boots.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_boots` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_chestplate.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_chestplate` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_helmet.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_helmet` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_hoe.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_hoe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_leggings.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_leggings` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_mace.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `silentgear` + `allthemodium` + `allthemodium:vibranium_mace`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_pickaxe.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_pickaxe` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_shovel.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_shovel` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_sword.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_sword` + `silentgear` + `allthemodium`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

## 4. AE2 / 高级 AE

目录：`src/main/resources/data/alltheimbaium/recipe/ae2/`

### `accumulation_processor_chamber.json`

- **`ae2:i`×64 + `megacells:accumulation_processor`×64**  ←  `minecraft:lava`×1600 + `megacells:printed_accumulation_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64 + `advanced_ae` + `megacells` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `calculation_processor_chamber.json`

- **`ae2:i`×64 + `ae2:calculation_processor`×64**  ←  `minecraft:lava`×1600 + `ae2:printed_calculation_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64 + `advanced_ae` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `certus_quartz_dust04.json`

- **`ae2:certus_quartz_dust`×4**  ←  `ae2` + `mekanism` + `ae2:quartz_block`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `certus_quartz_dust16.json`

- **`ae2:certus_quartz_dust`×16**  ←  `ae2` + `mekanism` + `ae2:damaged_budding_quartz`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `certus_quartz_dust64.json`

- **`ae2:certus_quartz_dust`×64**  ←  `ae2` + `mekanism` + `ae2:chipped_budding_quartz`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `chipped_budding_quartz.json`

- **`ae2:chipped_budding_quartz`**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:damaged_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `damaged_budding_quartz.json`

- **`ae2:damaged_budding_quartz`**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:quartz_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `ender_dust09.json`

- **`ae2:ender_dust`×9**  ←  `mekanism` + `allthetweaks` + `ae2` + `allthetweaks:ender_pearl_block`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `ender_dust64.json`

- **`ae2:ender_dust`×64**  ←  `mekanism` + `allthecompressed` + `ae2` + `allthecompressed:ender_pearl_block_1x`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `energy_processor_chamber.json`

- **`ae2:i`×64 + `appflux:energy_processor`×64**  ←  `minecraft:lava`×1600 + `appflux:printed_energy_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64 + `advanced_ae` + `appflux` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `engineering_processor_chamber.json`

- **`ae2:i`×64 + `ae2:engineering_processor`×64**  ←  `minecraft:lava`×1600 + `ae2:printed_engineering_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64 + `advanced_ae` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `flawed_budding_quartz.json`

- **`ae2:flawed_budding_quartz`**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:chipped_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `flawless_budding_quartz.json`

- **`ae2:flawless_budding_quartz`**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:flawed_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `logic_processor_chamber.json`

- **`ae2:i`×64 + `ae2:logic_processor`×64**  ←  `minecraft:lava`×1600 + `ae2:printed_logic_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64 + `advanced_ae` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `printed_calculation_processor.json`

- **`ae2:printed_calculation_processor`×64**  ←  `extendedae` + `ae2` + `ae2:chipped_budding_quartz`
  - 机器：extendedae:circuit_cutter · `extendedae:circuit_cutter`
  - 前置：无（始终生效）

### `printed_processor_calculation.json`

- **`ae2:printed_calculation_processor`×64**  ←  `minecraft:water`×800 + `ae2:chipped_budding_quartz` + `expatternprovider` + `ae2`
  - 机器：扩展 AE · 电路切割机 · `expatternprovider:circuit_cutter`
  - 前置：无（始终生效）

### `quantum_alloy.json`

- **`ae2:i`×16 + `advanced_ae:quantum_alloy`×16**  ←  `advanced_ae:quantum_infusion_source`×16000 + `minecraft:copper_block`×8 + `advanced_ae:shattered_singularity`×64 + `ae2:singularity`×64 + `advanced_ae` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_alloy_plate.json`

- **`ae2:i`×9 + `advanced_ae:quantum_alloy_plate`×9**  ←  `advanced_ae:quantum_infusion_source`×9000 + `advanced_ae:quantum_alloy_block`×8 + `minecraft:netherite_block`×2 + `minecraft:nether_star`×9 + `advanced_ae` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_infusion.json`

- **`ae2:f`×16000 + `advanced_ae:quantum_infusion_source`×16000**  ←  `minecraft:lava`×16000 + `advanced_ae:quantum_infused_dust`×16 + `advanced_ae`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_processor.json`

- **`ae2:i`×64 + `advanced_ae:quantum_processor`×64**  ←  `minecraft:lava`×1600 + `advanced_ae:printed_quantum_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64 + `advanced_ae` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `shatteredsingularity.json`

- **`ae2:i`×64 + `advanced_ae:shattered_singularity`×64**  ←  `minecraft:water`×3200 + `ae2:singularity`×32 + `#c:dusts/ender_pearl`×64 + `ae2:sky_dust`×64 + `advanced_ae` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `sky_dust.json`

- **`ae2:i`×64 + `ae2:sky_dust`×64**  ←  `minecraft:water`×1000 + `ae2:sky_stone_block`×64 + `advanced_ae` + `ae2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `sky_dust09.json`

- **`ae2:sky_dust`×9**  ←  `mekanism` + `allthecompressed` + `ae2` + `allthecompressed:sky_stone_block_1x`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `sky_dust64.json`

- **`ae2:sky_dust`×64**  ←  `mekanism` + `allthecompressed` + `ae2` + `allthecompressed:sky_stone_block_2x`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

## 5. 通用机械 Mekanism

目录：`src/main/resources/data/alltheimbaium/recipe/mek/`

### `alloy_atomic.json`

- **`mekanism:alloy_atomic`×9**  ←  `#c:storage_blocks/diamond` + `mekanism:refined_obsidian`（化学）×360 + `mekanism`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `alloy_infused1.json`

- **`mekanism:alloy_infused`×9**  ←  `#c:storage_blocks/iron` + `mekanism:redstone`（化学）×90 + `mekanism`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `alloy_infused2.json`

- **`mekanism:alloy_infused`×9**  ←  `#c:storage_blocks/copper` + `mekanism:redstone`（化学）×90 + `mekanism`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `alloy_reinforced.json`

- **`mekanism:alloy_reinforced`×9**  ←  `#c:storage_blocks/redstone` + `mekanism:diamond`（化学）×180 + `mekanism`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `basic_control_circuit.json`

- **`mekanism:basic_control_circuit`×9**  ←  `#c:storage_blocks/osmium` + `mekanism:redstone`（化学）×180 + `mekanism`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `certus_quartz_dust.json`

- **`ae2:certus_quartz_dust`×64**  ←  `ae2:chipped_budding_quartz` + `mekanism` + `ae2`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `creative_chemical_tank.json`

- **`mekanism:creative_chemical_tank`**  ·  图案 `PPP` / `PPP` / `PPP`
  - 键位：`P`=`mekanism:ultimate_chemical_tank`×9
  - 机器：工作台（有序） · `mekanism:mek_data`
  - 前置：无（始终生效）

### `crushed_blackstone.json`

- **`exdeorum:crushed_blackstone`**  ←  `minecraft:blackstone` + `mekanism` + `exdeorum`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `crushed_deepslate.json`

- **`exdeorum:crushed_deepslate`**  ←  `minecraft:cobbled_deepslate` + `mekanism` + `exdeorum`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `crushed_end_stone.json`

- **`exdeorum:crushed_end_stone`**  ←  `minecraft:end_stone` + `mekanism` + `exdeorum`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `crushed_netherrack.json`

- **`exdeorum:crushed_netherrack`**  ←  `minecraft:netherrack` + `mekanism` + `exdeorum`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `dust.json`

- **`exdeorum:dust`**  ←  `minecraft:sand` + `mekanism` + `exdeorum`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `red_sand.json`

- **`minecraft:red_sand`**  ←  `minecraft:granite` + `mekanism`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `rose_quartz.json`

- **`create:rose_quartz`×4**  ←  `#c:storage_blocks/quartz` + `mekanism:redstone`（化学）×320 + `mekanism` + `create`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `sky_dust.json`

- **`ae2:sky_dust`**  ←  `ae2:certus_quartz_dust` + `mekanism:carbon`（化学）×5 + `mekanism` + `ae2`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `sky_dust2.json`

- **`ae2:sky_dust`×64**  ←  `ae2:chipped_budding_quartz` + `mekanism:carbon`（化学）×80 + `mekanism` + `ae2`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `steel_ingot.json`

- **`alltheores:steel_ingot`**  ←  `alltheores:iron_dust` + `mekanism:carbon`（化学）×20 + `mekanism` + `alltheores`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `wheat_flour.json`

- **`create:wheat_flour`×2**  ←  `minecraft:wheat` + `mekanism` + `create`
  - 机器：通用机械 · 富集仓 · `mekanism:enriching`
  - 前置：无（始终生效）

## 6. 热力系列 Thermal

目录：`src/main/resources/data/alltheimbaium/recipe/thermal/`

### `activationcrystalweak.json`

- **`bloodmagic:activationcrystalweak`**  ←  `bloodmagic:lavacrystal` + `bloodmagic:life_essence_fluid`×10000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `airscribetool.json`

- **`bloodmagic:airscribetool`**  ←  `minecraft:ghast_tear` + `bloodmagic:life_essence_fluid`×1000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `alchemy_flask.json`

- **`bloodmagic:alchemy_flask`**  ←  `minecraft:glass_bottle` + `bloodmagic:life_essence_fluid`×4000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `apprenticebloodorb.json`

- **`bloodmagic:apprenticebloodorb`**  ←  `#c:storage_blocks/redstone` + `bloodmagic:life_essence_fluid`×5000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `archmagebloodorb.json`

- **`bloodmagic:archmagebloodorb`**  ←  `bloodmagic:dungeon_metal` + `bloodmagic:life_essence_fluid`×80000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `blankslate.json`

- **`bloodmagic:blankslate`**  ←  `#c:stones` + `bloodmagic:life_essence_fluid`×1000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `bleedingedge.json`

- **`bloodmagic:bleedingedge`**  ←  `bloodmagic:rawdemoniteblock` + `bloodmagic:life_essence_fluid`×10000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `blood.json`

- **`bloodmagic:life_essence_fluid`×1000**  ←  `evilcraft:blood`×1000 + `thermal` + `evilcraft` + `bloodmagic`
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：无（始终生效）

### `daggerofsacrifice.json`

- **`bloodmagic:daggerofsacrifice`**  ←  `minecraft:iron_sword` + `bloodmagic:life_essence_fluid`×3000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `defaultcrystal.json`

- **`bloodmagic:defaultcrystal`**  ←  `#c:gems/quartz` + `bloodmagic:life_essence_fluid`×1000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `demonslate.json`

- **`bloodmagic:demonslate`**  ←  `bloodmagic:infusedslate` + `bloodmagic:life_essence_fluid`×15000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `duskscribetool.json`

- **`bloodmagic:duskscribetool`**  ←  `#c:storage_blocks/coal` + `bloodmagic:life_essence_fluid`×2000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `earthscribetool.json`

- **`bloodmagic:earthscribetool`**  ←  `#c:obsidians` + `bloodmagic:life_essence_fluid`×1000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `enhancedteleposerfocus.json`

- **`bloodmagic:enhancedteleposerfocus`**  ←  `bloodmagic:teleposerfocus` + `bloodmagic:life_essence_fluid`×10000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `etherealslate.json`

- **`bloodmagic:etherealslate`**  ←  `bloodmagic:demonslate` + `bloodmagic:life_essence_fluid`×30000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `firescribetool.json`

- **`bloodmagic:firescribetool`**  ←  `minecraft:magma_cream` + `bloodmagic:life_essence_fluid`×1000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `infusedslate.json`

- **`bloodmagic:infusedslate`**  ←  `bloodmagic:reinforcedslate` + `bloodmagic:life_essence_fluid`×5000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `ingot_hellforged.json`

- **`bloodmagic:ingot_hellforged`**  ←  `#c:ingots/netherite` + `bloodmagic:life_essence_fluid`×8000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `life_essence_fluid.json`

- **`evilcraft:blood`×1000**  ←  `bloodmagic:life_essence_fluid`×1000 + `thermal` + `bloodmagic` + `evilcraft`
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：无（始终生效）

### `magicianbloodorb.json`

- **`bloodmagic:magicianbloodorb`**  ←  `#c:storage_blocks/gold` + `bloodmagic:life_essence_fluid`×25000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `masterbloodorb.json`

- **`bloodmagic:masterbloodorb`**  ←  `bloodmagic:weakbloodshard` + `bloodmagic:life_essence_fluid`×40000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `meat.json`

- **`mekanism:bio_fuel` + `bloodmagic:life_essence_fluid`×1000**  ←  `industrialforegoing:meat`×1000 + `thermal` + `industrialforegoing` + `mekanism` + `bloodmagic`
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：无（始终生效）

### `rawdemonite.json`

- **`bloodmagic:rawdemonite`**  ←  `#c:ores/netherite_scrap` + `bloodmagic:life_essence_fluid`×2000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `reinforcedslate.json`

- **`bloodmagic:reinforcedslate`**  ←  `bloodmagic:blankslate` + `bloodmagic:life_essence_fluid`×2000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `soulsnare.json`

- **`bloodmagic:soulsnare`**  ←  `#c:strings` + `bloodmagic:life_essence_fluid`×500 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `teleposerfocus.json`

- **`bloodmagic:teleposerfocus`**  ←  `#c:ender_pearls` + `bloodmagic:life_essence_fluid`×2000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `waterscribetool.json`

- **`bloodmagic:waterscribetool`**  ←  `#c:storage_blocks/lapis` + `bloodmagic:life_essence_fluid`×1000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `weakbloodorb.json`

- **`bloodmagic:weakbloodorb`**  ←  `#c:gems/diamond` + `bloodmagic:life_essence_fluid`×2000 + `thermal` + `bloodmagic`
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

## 7. 神秘农业 Mystical Agriculture

目录：`src/main/resources/data/alltheimbaium/recipe/mystical/`

### `gaia_spirit_crux.json`

- **`mysticalagradditions:gaia_spirit_crux`**  ·  图案 `EHE` / `WDW` / `EWE`
  - 键位：`E`=`mysticalagradditions:insanium_essence`×4、`H`=`botania:terrasteel_block`、`W`=`botania:gaia_ingot`×3、`D`=`#c:storage_blocks/diamond`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `gaia_spirit_seeds.json`

- **`mysticalagriculture:gaia_spirit_seeds`**  ←  `mysticalagradditions:gaia_spirit（seed）` + `mysticalagradditions:gaia_spirit（material）`×4 + `mysticalagradditions:gaia_spirit（essence）`×4 + `mysticalagriculture` + `mysticalagradditions`
  - 机器：神秘农业 · 注魔祭坛 · `mysticalagriculture:infusion`
  - 前置：无（始终生效）

### `imperium_farmland.json`

- **`mysticalagriculture:imperium_essence`**  ←  `mystical_agriculture_farmland` + `misc` + `mysticalagriculture:imperium_farmland` + `mysticalagriculture`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `imperium_growth_accelerator.json`

- **`mysticalagriculture:imperium_growth_accelerator`**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:tertium_essence`×6、`S`=`mysticalagriculture:tertium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `inferium_farmland.json`

- **`mysticalagriculture:inferium_essence`**  ←  `mystical_agriculture_farmland` + `misc` + `mysticalagriculture:inferium_farmland` + `mysticalagriculture`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `insanium_farmland.json`

- **`mysticalagradditions:insanium_essence`**  ←  `mystical_agriculture_farmland` + `misc` + `mysticalagradditions:insanium_farmland` + `mysticalagradditions`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `life_essence.json`

- **`botania:life_essence`**  ←  `botania_life_essence` + `misc` + `mysticalagriculture:gaia_spirit_essence`×3 + `mysticalagriculture` + `botania`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `magical_soil.json`

- **`kubejs:magical_soil`**  ·  图案 `#A#` / `ASA` / `#A#`
  - 键位：`S`=`mysticalagradditions:insanium_farmland`、`A`=`mysticalagradditions:dragon_scale`×4、`#`=`mysticalagradditions:insanium_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `prudentium_farmland.json`

- **`mysticalagriculture:prudentium_essence`**  ←  `mystical_agriculture_farmland` + `misc` + `mysticalagriculture:prudentium_farmland` + `mysticalagriculture`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `prudentium_growth_accelerator.json`

- **`mysticalagriculture:prudentium_growth_accelerator`**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:inferium_essence`×6、`S`=`mysticalagriculture:inferium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `supremium_farmland.json`

- **`mysticalagriculture:supremium_essence`**  ←  `mystical_agriculture_farmland` + `misc` + `mysticalagriculture:supremium_farmland` + `mysticalagriculture`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `supremium_growth_accelerator.json`

- **`mysticalagriculture:supremium_growth_accelerator`**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:imperium_essence`×6、`S`=`mysticalagriculture:imperium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `terium_farmland.json`

- **`mysticalagriculture:tertium_essence`**  ←  `mystical_agriculture_farmland` + `misc` + `mysticalagriculture:tertium_farmland` + `mysticalagriculture`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `tertium_growth_accelerator.json`

- **`mysticalagriculture:tertium_growth_accelerator`**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:prudentium_essence`×6、`S`=`mysticalagriculture:prudentium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 8. 血魔法 Blood Magic

目录：`src/main/resources/data/alltheimbaium/recipe/blood/`

### `hellforgedparts.json`

- **`bloodmagic:hellforgedparts`**  ·  图案 ` # ` / `# #` / ` # `
  - 键位：`#`=`bloodmagic:ingot_hellforged`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `mineentrancekey.json`

- **`bloodmagic:mineentrancekey`**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`bloodmagic:hellforgedparts`、`#`=`#c:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `minekey.json`

- **`bloodmagic:minekey`**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`bloodmagic:ingot_hellforged`、`#`=`#c:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `simplekey.json`

- **`bloodmagic:simplekey`**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`#c:storage_blocks/redstone`、`#`=`#c:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `strong_tau.json`

- **`bloodmagic:strong_tau`**  ·  图案 `##` / `##`
  - 键位：`#`=`bloodmagic:weak_tau`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 9. 机械动力 Create

目录：`src/main/resources/data/alltheimbaium/recipe/create/`

### `life_essence_fluid.json`

- **`bloodmagic:life_essence_fluid`×500**  ←  `industrialforegoing:meat`×250 + `minecraft:water`×250 + `create` + `industrialforegoing` + `bloodmagic`
  - 机器：机械动力 · 混合搅拌 · `create:mixing`
  - 前置：无（始终生效）

## 10. 龙之研究 Draconic Evolution

目录：`src/main/resources/data/alltheimbaium/recipe/draconicevolution/`

### `small_chaos_frag.json`

- **`draconicevolution:small_chaos_frag`**  ·  图案 `ADA` / `DLD` / `ADA`
  - 键位：`A`=`alltheimbaium:package_material`×4、`D`=`minecraft:dragon_head`×4、`L`=`minecraft:dragon_egg`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 11. advanced_ae

目录：`src/main/resources/data/alltheimbaium/recipe/advanced_ae/`

### `quantum_alloy.json`

- **`ae2:i`×16 + `advanced_ae:quantum_alloy`×16**  ←  `ae2` + `advanced_ae` + `advanced_ae:quantum_infusion_source`×16000 + `minecraft:copper_block`×8 + `advanced_ae:shattered_singularity`×64 + `ae2:singularity`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_alloy_plate.json`

- **`ae2:i`×16 + `advanced_ae:quantum_alloy_plate`×16**  ←  `advanced_ae` + `advanced_ae:quantum_infusion_source`×1000 + `advanced_ae:quantum_alloy_block`×15 + `minecraft:netherite_ingot`×32 + `minecraft:nether_star`×16
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_infusion_source.json`

- **`ae2:f`×16000 + `advanced_ae:quantum_infusion_source`×16000**  ←  `advanced_ae` + `minecraft:lava`×4000 + `advanced_ae:quantum_infused_dust`×16
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

## 12. evilcraft

目录：`src/main/resources/data/alltheimbaium/recipe/evilcraft/`

### `blood.json`

- **`ae2:f`×16000 + `evilcraft:blood`×16000**  ←  `advanced_ae` + `industrialforegoing` + `evilcraft` + `industrialforegoing:meat`×16000 + `minecraft:charcoal`×16
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

## 13. extendedae

目录：`src/main/resources/data/alltheimbaium/recipe/extendedae/`

### `accumulation_processor.json`

- **`ae2:i`×64 + `megacells:accumulation_processor`×64**  ←  `advanced_ae` + `ae2` + `megacells` + `minecraft:water`×16000 + `megacells:printed_accumulation_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/fluix`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `calculation_processor.json`

- **`ae2:i`×64 + `ae2:calculation_processor`×64**  ←  `advanced_ae` + `ae2` + `minecraft:water`×16000 + `ae2:printed_calculation_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `concurrent_processor.json`

- **`ae2:i`×64 + `extendedae:concurrent_processor`×64**  ←  `extendedae` + `advanced_ae` + `ae2` + `minecraft:water`×16000 + `extendedae:concurrent_processor_print`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `concurrent_processor_print.json`

- **`extendedae:concurrent_processor_print`×64**  ←  `extendedae` + `extendedae:entro_budding_half`
  - 机器：extendedae:circuit_cutter · `extendedae:circuit_cutter`
  - 前置：无（始终生效）

### `engineering_processor.json`

- **`ae2:i`×64 + `ae2:engineering_processor`×64**  ←  `advanced_ae` + `ae2` + `minecraft:water`×16000 + `ae2:printed_engineering_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `entro_budding_fully.json`

- **`extendedae:entro_budding_fully`**  ·  图案 `##` / `##`
  - 键位：`#`=`extendedae:entro_budding_mostly`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `entro_budding_half.json`

- **`extendedae:entro_budding_half`**  ·  图案 `##` / `##`
  - 键位：`#`=`extendedae:entro_budding_hardly`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `entro_budding_hardly.json`

- **`extendedae:entro_budding_hardly`**  ·  图案 `##` / `##`
  - 键位：`#`=`extendedae:entro_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `entro_budding_mostly.json`

- **`extendedae:entro_budding_mostly`**  ·  图案 `##` / `##`
  - 键位：`#`=`extendedae:entro_budding_half`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `entro_dust04.json`

- **`extendedae:entro_dust`×4**  ←  `extendedae` + `mekanism` + `extendedae:entro_block`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `entro_dust16.json`

- **`extendedae:entro_dust`×16**  ←  `extendedae` + `mekanism` + `extendedae:entro_budding_hardly`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `entro_dust64.json`

- **`extendedae:entro_dust`×64**  ←  `extendedae` + `mekanism` + `extendedae:entro_budding_half`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `logic_processor.json`

- **`ae2:i`×64 + `ae2:logic_processor`×64**  ←  `advanced_ae` + `ae2` + `minecraft:water`×16000 + `ae2:printed_logic_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_processor.json`

- **`ae2:i`×64 + `advanced_ae:quantum_processor`×64**  ←  `advanced_ae` + `ae2` + `minecraft:water`×16000 + `advanced_ae:printed_quantum_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `shattered_singularity.json`

- **`advanced_ae:shattered_singularity`×64**  ←  `extendedae` + `ae2` + `minecraft:lava`×100 + `ae2:singularity`×32 + `ae2:ender_dust`×64 + `ae2:sky_dust`×64
  - 机器：extendedae:crystal_assembler · `extendedae:crystal_assembler`
  - 前置：无（始终生效）

## 14. forbidden_arcanus

目录：`src/main/resources/data/alltheimbaium/recipe/forbidden_arcanus/`

### `artisan_relic.json`

- **`forbidden_arcanus:artisan_relic`**  ←  `forbidden_arcanus`×2 + `misc` + `forbidden_arcanus:maledictus_pact`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `crescent_moon.json`

- **`forbidden_arcanus:crescent_moon`**  ←  `forbidden_arcanus`×2 + `misc` + `forbidden_arcanus:artisan_relic`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `crimson_stone.json`

- **`forbidden_arcanus:crimson_stone`**  ←  `forbidden_arcanus`×2 + `misc` + `forbidden_arcanus:crescent_moon`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `divine_pact.json`

- **`forbidden_arcanus:divine_pact`**  ←  `forbidden_arcanus`×2 + `misc` + `forbidden_arcanus:elementarium`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `elementarium.json`

- **`forbidden_arcanus:elementarium`**  ←  `forbidden_arcanus`×2 + `misc` + `forbidden_arcanus:crimson_stone`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `maledictus_pact.json`

- **`forbidden_arcanus:maledictus_pact`**  ←  `forbidden_arcanus`×2 + `misc` + `forbidden_arcanus:divine_pact`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

## 15. jdt

目录：`src/main/resources/data/alltheimbaium/recipe/jdt/`

### `celestigem.json`

- **`justdirethings:celestigem`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier3` + `minecraft:diamond_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `coal_t1.json`

- **`justdirethings:coal_t1`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier1` + `minecraft:coal_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `coal_t2.json`

- **`justdirethings:coal_t2`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier2` + `justdirethings:coalblock_t1`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `coal_t3.json`

- **`justdirethings:coal_t3`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier3` + `justdirethings:coalblock_t2`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `coal_t4.json`

- **`justdirethings:coal_t4`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier4` + `justdirethings:coalblock_t3`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `polymorphic_fluid_source.json`

- **`ae2:f`×1000 + `justdirethings:polymorphic_fluid_source`×1000**  ←  `advanced_ae` + `justdirethings` + `minecraft:water`×1000 + `justdirethings:polymorphic_catalyst`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `portal_fluid_source.json`

- **`ae2:f`×16000 + `justdirethings:portal_fluid_source`×16000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:unstable_portal_fluid_source`×16000 + `justdirethings:gooblock_tier3`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `raw_blazegold.json`

- **`justdirethings:raw_blazegold`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier2` + `minecraft:gold_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `raw_eclipsealloy.json`

- **`justdirethings:raw_eclipsealloy`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier4` + `minecraft:netherite_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `raw_ferricore.json`

- **`justdirethings:raw_ferricore`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier1` + `minecraft:iron_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `refined_t2_fluid_source.json`

- **`ae2:f`×16000 + `justdirethings:refined_t2_fluid_source`×16000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:unrefined_t2_fluid_source`×16000 + `justdirethings:gooblock_tier2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `refined_t3_fluid_source.json`

- **`ae2:f`×16000 + `justdirethings:refined_t3_fluid_source`×16000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:unrefined_t3_fluid_source`×16000 + `justdirethings:gooblock_tier3`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `refined_t4_fluid_source.json`

- **`ae2:f`×16000 + `justdirethings:refined_t4_fluid_source`×16000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:unrefined_t4_fluid_source`×16000 + `justdirethings:gooblock_tier4`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `time_crystal.json`

- **`justdirethings:time_crystal`×9**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier4` + `minecraft:diamond_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `time_crystal_block.json`

- **`justdirethings:time_crystal_block`**  ←  `justdirethings` + `misc` + `justdirethings:gooblock_tier4` + `minecraft:budding_amethyst`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `time_crystal_budding_block.json`

- **`justdirethings:time_crystal_budding_block`**  ·  图案 `##` / `##`
  - 键位：`#`=`justdirethings:time_crystal_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `time_fluid_source.json`

- **`ae2:f`×1000 + `justdirethings:time_fluid_source`×1000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:polymorphic_fluid_source`×1000 + `justdirethings:time_crystal`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `unrefined_t2_fluid_source.json`

- **`ae2:f`×1000 + `justdirethings:unrefined_t2_fluid_source`×1000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:polymorphic_fluid_source`×1000 + `justdirethings:coal_t2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `unrefined_t3_fluid_source.json`

- **`ae2:f`×1000 + `justdirethings:unrefined_t3_fluid_source`×1000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:refined_t2_fluid_source`×1000 + `justdirethings:coal_t3`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `unrefined_t4_fluid_source.json`

- **`ae2:f`×1000 + `justdirethings:unrefined_t4_fluid_source`×1000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:refined_t3_fluid_source`×1000 + `justdirethings:coal_t4`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `unstable_portal_fluid_source.json`

- **`ae2:f`×1000 + `justdirethings:unstable_portal_fluid_source`×1000**  ←  `advanced_ae` + `justdirethings` + `justdirethings:polymorphic_fluid_source`×1000 + `justdirethings:portal_fluid_catalyst`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

## 16. smelting

目录：`src/main/resources/data/alltheimbaium/recipe/smelting/`

### `glass_1x.json`

- **`allthecompressed:glass_1x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_1x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_2x.json`

- **`allthecompressed:glass_2x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_2x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_3x.json`

- **`allthecompressed:glass_3x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_3x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_4x.json`

- **`allthecompressed:glass_4x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_4x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_5x.json`

- **`allthecompressed:glass_5x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_5x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_6x.json`

- **`allthecompressed:glass_6x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_6x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_7x.json`

- **`allthecompressed:glass_7x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_7x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_8x.json`

- **`allthecompressed:glass_8x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_8x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_9x.json`

- **`allthecompressed:glass_9x`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_9x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `silicon.json`

- **`ae2:silicon`**  ←  `allthecompressed` + `blocks` + `allthecompressed:sand_9x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

## 17. 本模组自定义配方

这几种配方**不是数据包配方**，而是由 Java 代码动态判定——JSON 只是一份类型标记文件，
配方书与 JEI 里都不会出现，改行为要改代码：

### `brewing_craft.json`

- 配方类型：`alltheimbaium:brewing_craft`
- 说明：工作台中恰好 2 个物品（1 药水 + 1 酿造材料）无序合成，输出原版酿造结果。行为见 recipe/BrewingCraftRecipe.java

### `potion_combine.json`

- 配方类型：`alltheimbaium:potion_combine`
- 说明：任意两瓶药水合成混合药水；混合药水 + 火药/龙息/奶桶 转换类型。等级取高者，同等级时长按配置系数叠加。行为见 recipe/PotionCombineRecipe.java

### `smelting_craft.json`

- 配方类型：`alltheimbaium:smelting_craft`
- 说明：3×3 外围 8 格放同一种可烧炼物品、中心放煤炭/木炭，一次产出 8 个烧炼结果。动态查询熔炉→高炉→烟熏炉三级配方，行为见 recipe/SmeltingCraftRecipe.java
