# Alltheimbaium 配方总表

> **本文件由 `tools/gen_recipe_docs.py` 自动生成，请勿手工修改。**
> 配方有增删改后重新执行 `python tools/gen_recipe_docs.py` 即可同步。

- 共 **145** 个配方文件、**145** 条配方
- 物品一律写**注册名**；数量省略表示 1
- 物品流向一律写作：**产物 ← 材料**
- 带「前置」的条目被 `forge:conditional` 包裹，**未安装对应 MOD 时该配方根本不存在**

## 总览

| 分类 | 目录 | 配方数 | 涉及 MOD |
|------|------|-------:|----------|
| 本模组机器 | `recipes/main/` | 17 | — |
| 原版简化 | `recipes/crafting/` | 11 | `ae2`、`allthemodium`、`enderio` |
| 拆解回收 | `recipes/salvaging/` | 31 | `allthemodium`、`mysticalagriculture`、`silentgear` |
| AE2 / 高级 AE | `recipes/ae2/` | 16 | `advanced_ae`、`ae2`、`appflux`、`expatternprovider`、`megacells` |
| 通用机械 Mekanism | `recipes/mek/` | 18 | `ae2`、`alltheores`、`create`、`exdeorum`、`mekanism` |
| 热力系列 Thermal | `recipes/thermal/` | 28 | `bloodmagic`、`evilcraft`、`industrialforegoing`、`mekanism`、`thermal` |
| 神秘农业 Mystical Agriculture | `recipes/mystical/` | 14 | `botania`、`kubejs`、`mysticalagradditions`、`mysticalagriculture` |
| 血魔法 Blood Magic | `recipes/blood/` | 5 | `bloodmagic` |
| 机械动力 Create | `recipes/create/` | 1 | `bloodmagic`、`create`、`industrialforegoing` |
| 龙之研究 Draconic Evolution | `recipes/draconicevolution/` | 1 | `draconicevolution` |
| 本模组自定义配方 | `recipes/*.json` | 3 | — |

## 目录

1. [本模组机器（17 条）](#1-本模组机器)
2. [原版简化（11 条）](#2-原版简化)
3. [拆解回收（31 条）](#3-拆解回收)
4. [AE2 / 高级 AE（16 条）](#4-ae2--高级-ae)
5. [通用机械 Mekanism（18 条）](#5-通用机械-mekanism)
6. [热力系列 Thermal（28 条）](#6-热力系列-thermal)
7. [神秘农业 Mystical Agriculture（14 条）](#7-神秘农业-mystical-agriculture)
8. [血魔法 Blood Magic（5 条）](#8-血魔法-blood-magic)
9. [机械动力 Create（1 条）](#9-机械动力-create)
10. [龙之研究 Draconic Evolution（1 条）](#10-龙之研究-draconic-evolution)
11. [本模组自定义配方（3 条）](#11-本模组自定义配方)

## 1. 本模组机器

目录：`src/main/resources/data/alltheimbaium/recipes/main/`

### `auto_farmland.json`

- **`alltheimbaium:auto_farmland`**  ·  图案 `XFF` / `FRF` / `FFX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`F`=`alltheimbaium:farmland`×6、`R`=`minecraft:redstone`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `clock.json`

- **`alltheimbaium:clock`**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material_x1`、`B`=`minecraft:iron_block`×7、`C`=`minecraft:clock`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `creative_transmuter.json`

- **`alltheimbaium:creative_transmuter`**  ·  图案 `NNN` / `NPN` / `NNN`
  - 键位：`N`=`minecraft:netherite_block`×8、`P`=`alltheimbaium:package_material_x1`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `eternal_sword.json`

- **`alltheimbaium:eternal_sword`**  ·  图案 `XBA` / `BCB` / `ABX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`A`=`minecraft:netherite_block`×2、`B`=`minecraft:golden_apple`×4、`C`=`minecraft:netherite_sword`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `eternal_totem.json`

- **`alltheimbaium:eternal_totem`**  ·  图案 `XBA` / `BCB` / `ABX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`A`=`minecraft:netherite_block`×2、`B`=`minecraft:golden_apple`×4、`C`=`minecraft:totem_of_undying`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `extraction_interface.json`

- **`alltheimbaium:extraction_interface`**  ·  图案 `XBB` / `BCB` / `BBX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`B`=`minecraft:iron_ingot`×6、`C`=`minecraft:redstone_block`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `farmland.json`

- **`alltheimbaium:farmland`**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material_x1`、`B`=`minecraft:iron_ingot`×7、`C`=`minecraft:dirt`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `instant_furnace.json`

- **`alltheimbaium:instant_furnace`**  ·  图案 `XBB` / `BCB` / `BBX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`B`=`minecraft:iron_block`×6、`C`=`minecraft:furnace`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `instant_inscriber.json`

- **`alltheimbaium:instant_inscriber`**  ·  图案 `XBB` / `BCB` / `BBX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`B`=`minecraft:iron_block`×6、`C`=`minecraft:smithing_table`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `liquid_fountain.json`

- **`alltheimbaium:liquid_fountain`**  ·  图案 `XBB` / `BCB` / `BBX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:bucket`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `mob_farm.json`

- **`alltheimbaium:mob_farm`**  ·  图案 `XBB` / `BCB` / `BBX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:glass_bottle`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `obsidian_from_buckets.json`

- **`minecraft:obsidian`**  ←  `minecraft:water_bucket` + `minecraft:lava_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `package_material_x1_1.json`

- **`alltheimbaium:package_material_x1`×8**  ·  图案 `AAA` / `ABA` / `AAA`
  - 键位：`A`=`#minecraft:planks`×8、`B`=`#forge:cobblestone`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `package_material_x1_2.json`

- **`alltheimbaium:package_material_x1`×8**  ·  图案 `AAA` / `ABA` / `AAA`
  - 键位：`A`=`#forge:cobblestone`×8、`B`=`#minecraft:planks`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `platform.json`

- **`alltheimbaium:platform`**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material_x1`、`B`=`minecraft:iron_ingot`×7、`C`=`minecraft:smooth_stone`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `resource_farm.json`

- **`alltheimbaium:resource_farm`**  ·  图案 `XBB` / `BCB` / `BBX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:dirt`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `storage_fountain.json`

- **`alltheimbaium:storage_fountain`**  ·  图案 `XBB` / `BCB` / `BBX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:iron_block`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 2. 原版简化

目录：`src/main/resources/data/alltheimbaium/recipes/crafting/`

### `allthemodium_upgrade_smithing_template.json`

- **`allthemodium:allthemodium_upgrade_smithing_template`**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:allthemodium_ingot`、`B`=`minecraft:sculk`、`#`=`minecraft:netherite_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `allthemodium`

### `budding_amethyst.json`

- **`minecraft:budding_amethyst`**  ·  图案 `##` / `##`
  - 键位：`#`=`minecraft:amethyst_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `certus_quartz_dust.json`

- **`ae2:certus_quartz_dust`**  ←  `enderio:powdered_quartz` + `minecraft:water_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `enderio`、`ae2`

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

### `powdered_quartz.json`

- **`enderio:powdered_quartz`**  ←  `ae2:certus_quartz_dust` + `minecraft:lava_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `ae2`、`enderio`

### `spawner.json`

- **`minecraft:spawner`**  ·  图案 `###` / `#A#` / `###`
  - 键位：`A`=`minecraft:nether_star`、`#`=`minecraft:chain`×8
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `supply_crate.json`

- **`alltheimbaium:supply_crate`**  ·  图案 `XBB` / `BCB` / `BBX`
  - 键位：`X`=`alltheimbaium:package_material_x1`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:chest`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `unobtainium_upgrade_smithing_template.json`

- **`allthemodium:unobtainium_upgrade_smithing_template`**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:unobtainium_ingot`、`B`=`allthemodium:piglich_heart`、`#`=`allthemodium:vibranium_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `allthemodium`

### `vibranium_upgrade_smithing_template.json`

- **`allthemodium:vibranium_upgrade_smithing_template`**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:vibranium_ingot`、`B`=`minecraft:gilded_blackstone`、`#`=`allthemodium:allthemodium_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `allthemodium`

### `wheat_seeds.json`

- **`minecraft:wheat_seeds`**  ←  `minecraft:wheat`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

## 3. 拆解回收

目录：`src/main/resources/data/alltheimbaium/recipes/salvaging/`

### `allthemodium_axe.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_axe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `allthemodium_boots.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_boots`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `allthemodium_chestplate.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_chestplate`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `allthemodium_helmet.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_helmet`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `allthemodium_hoe.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_hoe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `allthemodium_leggings.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_leggings`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `allthemodium_pickaxe.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_pickaxe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `allthemodium_shovel.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_shovel`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `allthemodium_sword.json`

- **`allthemodium:allthemodium_ingot`×2**  ←  `allthemodium:allthemodium_sword`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `enchanted_book.json`

- **`minecraft:book` + `minecraft:experience_bottle`**  ←  `minecraft:enchanted_book`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`

### `fishing_rod.json`

- **`minecraft:string`×2 + `minecraft:stick`×3**  ←  `minecraft:fishing_rod`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`

### `potion.json`

- **`minecraft:glass_bottle` + `mysticalagriculture:water_essence`**  ←  `minecraft:potion`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`mysticalagriculture`

### `saddle.json`

- **`minecraft:leather`×2**  ←  `minecraft:saddle`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`

### `unobtainium_axe.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_axe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `unobtainium_boots.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_boots`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `unobtainium_chestplate.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_chestplate`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `unobtainium_helmet.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_helmet`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `unobtainium_hoe.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_hoe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `unobtainium_leggings.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_leggings`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `unobtainium_pickaxe.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_pickaxe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `unobtainium_shovel.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_shovel`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `unobtainium_sword.json`

- **`allthemodium:unobtainium_ingot`×2**  ←  `allthemodium:unobtainium_sword`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_axe.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_axe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_boots.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_boots`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_chestplate.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_chestplate`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_helmet.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_helmet`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_hoe.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_hoe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_leggings.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_leggings`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_pickaxe.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_pickaxe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_shovel.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_shovel`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

### `vibranium_sword.json`

- **`allthemodium:vibranium_ingot`×2**  ←  `allthemodium:vibranium_sword`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：需要安装 `silentgear`、`allthemodium`

## 4. AE2 / 高级 AE

目录：`src/main/resources/data/alltheimbaium/recipes/ae2/`

### `accumulation_processor_chamber.json`

- **`megacells:accumulation_processor`×64**  ←  `minecraft:lava` + `megacells:printed_accumulation_processor`×64 + `ae2:printed_silicon`×64 + `#forge:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`megacells`、`ae2`

### `calculation_processor_chamber.json`

- **`ae2:calculation_processor`×64**  ←  `minecraft:lava` + `ae2:printed_calculation_processor`×64 + `ae2:printed_silicon`×64 + `#forge:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`ae2`

### `chipped_budding_quartz.json`

- **`ae2:chipped_budding_quartz`**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:damaged_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `ae2`

### `damaged_budding_quartz.json`

- **`ae2:damaged_budding_quartz`**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:quartz_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `ae2`

### `energy_processor_chamber.json`

- **`appflux:energy_processor`×64**  ←  `minecraft:lava` + `appflux:printed_energy_processor`×64 + `ae2:printed_silicon`×64 + `#forge:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`appflux`、`ae2`

### `engineering_processor_chamber.json`

- **`ae2:engineering_processor`×64**  ←  `minecraft:lava` + `ae2:printed_engineering_processor`×64 + `ae2:printed_silicon`×64 + `#forge:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`ae2`

### `flawed_budding_quartz.json`

- **`ae2:flawed_budding_quartz`**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:chipped_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `ae2`

### `flawless_budding_quartz.json`

- **`ae2:flawless_budding_quartz`**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:flawed_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `ae2`

### `logic_processor_chamber.json`

- **`ae2:logic_processor`×64**  ←  `minecraft:lava` + `ae2:printed_logic_processor`×64 + `ae2:printed_silicon`×64 + `#forge:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`ae2`

### `printed_processor_calculation.json`

- **`ae2:printed_calculation_processor`×64**  ←  `minecraft:water`×800 + `ae2:chipped_budding_quartz`
  - 机器：扩展 AE · 电路切割机 · `expatternprovider:circuit_cutter`
  - 前置：需要安装 `expatternprovider`、`ae2`

### `quantum_alloy.json`

- **`advanced_ae:quantum_alloy`×16**  ←  `advanced_ae:quantum_infusion_source` + `minecraft:copper_block`×8 + `advanced_ae:shattered_singularity`×64 + `ae2:singularity`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`ae2`

### `quantum_alloy_plate.json`

- **`advanced_ae:quantum_alloy_plate`×9**  ←  `advanced_ae:quantum_infusion_source` + `advanced_ae:quantum_block`×8 + `minecraft:netherite_block`×2 + `minecraft:nether_star`×9
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`ae2`

### `quantum_infusion.json`

- **`advanced_ae:quantum_infusion_source`×16000**  ←  `minecraft:lava` + `advanced_ae:quantum_infused_dust`×16
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`

### `quantum_processor.json`

- **`advanced_ae:quantum_processor`×64**  ←  `minecraft:lava` + `advanced_ae:printed_quantum_processor`×64 + `ae2:printed_silicon`×64 + `#forge:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`ae2`

### `shatteredsingularity.json`

- **`advanced_ae:shattered_singularity`×64**  ←  `minecraft:water` + `ae2:singularity`×32 + `#forge:dusts/ender_pearl`×64 + `ae2:sky_dust`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`ae2`

### `sky_dust.json`

- **`ae2:sky_dust`×64**  ←  `minecraft:water` + `ae2:sky_stone_block`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：需要安装 `advanced_ae`、`ae2`

## 5. 通用机械 Mekanism

目录：`src/main/resources/data/alltheimbaium/recipes/mek/`

### `alloy_atomic.json`

- **`mekanism:alloy_atomic`×9**  ←  `mekanism:refined_obsidian`（化学）×360 + `#forge:storage_blocks/diamond`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`

### `alloy_infused1.json`

- **`mekanism:alloy_infused`×9**  ←  `mekanism:redstone`（化学）×90 + `#forge:storage_blocks/iron`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`

### `alloy_infused2.json`

- **`mekanism:alloy_infused`×9**  ←  `mekanism:redstone`（化学）×90 + `#forge:storage_blocks/copper`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`

### `alloy_reinforced.json`

- **`mekanism:alloy_reinforced`×9**  ←  `mekanism:diamond`（化学）×180 + `#forge:storage_blocks/redstone`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`

### `basic_control_circuit.json`

- **`mekanism:basic_control_circuit`×9**  ←  `mekanism:redstone`（化学）×180 + `#forge:storage_blocks/osmium`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`

### `certus_quartz_dust.json`

- **`ae2:certus_quartz_dust`×64**  ←  `ae2:chipped_budding_quartz`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：需要安装 `mekanism`、`ae2`

### `creative_chemical_tank.json`

- **`mekanism:creative_chemical_tank`**  ·  图案 `PPP` / `PPP` / `PPP`
  - 键位：`P`=`mekanism:ultimate_chemical_tank`×9
  - 机器：工作台（有序） · `mekanism:mek_data`
  - 前置：需要安装 `mekanism`

### `crushed_blackstone.json`

- **`exdeorum:crushed_blackstone`**  ←  `minecraft:blackstone`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：需要安装 `mekanism`、`exdeorum`

### `crushed_deepslate.json`

- **`exdeorum:crushed_deepslate`**  ←  `minecraft:cobbled_deepslate`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：需要安装 `mekanism`、`exdeorum`

### `crushed_end_stone.json`

- **`exdeorum:crushed_end_stone`**  ←  `minecraft:end_stone`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：需要安装 `mekanism`、`exdeorum`

### `crushed_netherrack.json`

- **`exdeorum:crushed_netherrack`**  ←  `minecraft:netherrack`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：需要安装 `mekanism`、`exdeorum`

### `dust.json`

- **`exdeorum:dust`**  ←  `minecraft:sand`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：需要安装 `mekanism`、`exdeorum`

### `red_sand.json`

- **`minecraft:red_sand`**  ←  `minecraft:granite`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：需要安装 `mekanism`

### `rose_quartz.json`

- **`create:rose_quartz`×4**  ←  `mekanism:redstone`（化学）×320 + `#forge:storage_blocks/quartz`
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`、`create`

### `sky_dust.json`

- **`ae2:sky_dust`**  ←  `ae2:certus_quartz_dust` + `mekanism:carbon`（化学）×5
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`、`ae2`

### `sky_dust2.json`

- **`ae2:sky_dust`×64**  ←  `ae2:chipped_budding_quartz` + `mekanism:carbon`（化学）×80
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`、`ae2`

### `steel_ingot.json`

- **`alltheores:steel_ingot`**  ←  `alltheores:iron_dust` + `mekanism:carbon`（化学）×20
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：需要安装 `mekanism`、`alltheores`

### `wheat_flour.json`

- **`create:wheat_flour`×2**  ←  `minecraft:wheat`
  - 机器：通用机械 · 富集仓 · `mekanism:enriching`
  - 前置：需要安装 `mekanism`、`create`

## 6. 热力系列 Thermal

目录：`src/main/resources/data/alltheimbaium/recipes/thermal/`

### `activationcrystalweak.json`

- **`bloodmagic:activationcrystalweak`**  ←  `bloodmagic:lavacrystal` + `bloodmagic:life_essence_fluid`×10000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `airscribetool.json`

- **`bloodmagic:airscribetool`**  ←  `minecraft:ghast_tear` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `alchemy_flask.json`

- **`bloodmagic:alchemy_flask`**  ←  `minecraft:glass_bottle` + `bloodmagic:life_essence_fluid`×4000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `apprenticebloodorb.json`

- **`bloodmagic:apprenticebloodorb`**  ←  `#forge:storage_blocks/redstone` + `bloodmagic:life_essence_fluid`×5000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `archmagebloodorb.json`

- **`bloodmagic:archmagebloodorb`**  ←  `bloodmagic:dungeon_metal` + `bloodmagic:life_essence_fluid`×80000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `blankslate.json`

- **`bloodmagic:blankslate`**  ←  `#forge:stone` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `bleedingedge.json`

- **`bloodmagic:bleedingedge`**  ←  `bloodmagic:rawdemoniteblock` + `bloodmagic:life_essence_fluid`×10000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `blood.json`

- **`bloodmagic:life_essence_fluid`×1000**  ←  `evilcraft:blood`×1000
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：需要安装 `thermal`、`evilcraft`、`bloodmagic`

### `daggerofsacrifice.json`

- **`bloodmagic:daggerofsacrifice`**  ←  `minecraft:iron_sword` + `bloodmagic:life_essence_fluid`×3000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `defaultcrystal.json`

- **`bloodmagic:defaultcrystal`**  ←  `#forge:gems/quartz` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `demonslate.json`

- **`bloodmagic:demonslate`**  ←  `bloodmagic:infusedslate` + `bloodmagic:life_essence_fluid`×15000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `duskscribetool.json`

- **`bloodmagic:duskscribetool`**  ←  `#forge:storage_blocks/coal` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `earthscribetool.json`

- **`bloodmagic:earthscribetool`**  ←  `#forge:obsidian` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `enhancedteleposerfocus.json`

- **`bloodmagic:enhancedteleposerfocus`**  ←  `bloodmagic:teleposerfocus` + `bloodmagic:life_essence_fluid`×10000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `etherealslate.json`

- **`bloodmagic:etherealslate`**  ←  `bloodmagic:demonslate` + `bloodmagic:life_essence_fluid`×30000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `firescribetool.json`

- **`bloodmagic:firescribetool`**  ←  `minecraft:magma_cream` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `infusedslate.json`

- **`bloodmagic:infusedslate`**  ←  `bloodmagic:reinforcedslate` + `bloodmagic:life_essence_fluid`×5000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `ingot_hellforged.json`

- **`bloodmagic:ingot_hellforged`**  ←  `#forge:ingots/netherite` + `bloodmagic:life_essence_fluid`×8000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `life_essence_fluid.json`

- **`evilcraft:blood`×1000**  ←  `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：需要安装 `thermal`、`bloodmagic`、`evilcraft`

### `magicianbloodorb.json`

- **`bloodmagic:magicianbloodorb`**  ←  `#forge:storage_blocks/gold` + `bloodmagic:life_essence_fluid`×25000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `masterbloodorb.json`

- **`bloodmagic:masterbloodorb`**  ←  `bloodmagic:weakbloodshard` + `bloodmagic:life_essence_fluid`×40000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `meat.json`

- **`mekanism:bio_fuel` + `bloodmagic:life_essence_fluid`×1000**  ←  `industrialforegoing:meat`×1000
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：需要安装 `thermal`、`industrialforegoing`、`mekanism`、`bloodmagic`

### `rawdemonite.json`

- **`bloodmagic:rawdemonite`**  ←  `#forge:ores/netherite_scrap` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `reinforcedslate.json`

- **`bloodmagic:reinforcedslate`**  ←  `bloodmagic:blankslate` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `soulsnare.json`

- **`bloodmagic:soulsnare`**  ←  `#forge:string` + `bloodmagic:life_essence_fluid`×500
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `teleposerfocus.json`

- **`bloodmagic:teleposerfocus`**  ←  `#forge:ender_pearls` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `waterscribetool.json`

- **`bloodmagic:waterscribetool`**  ←  `#forge:storage_blocks/lapis` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

### `weakbloodorb.json`

- **`bloodmagic:weakbloodorb`**  ←  `#forge:gems/diamond` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：需要安装 `thermal`、`bloodmagic`

## 7. 神秘农业 Mystical Agriculture

目录：`src/main/resources/data/alltheimbaium/recipes/mystical/`

### `gaia_spirit_crux.json`

- **`mysticalagradditions:gaia_spirit_crux`**  ·  图案 `EHE` / `WDW` / `EWE`
  - 键位：`E`=`mysticalagradditions:insanium_essence`×4、`H`=`botania:terrasteel_block`、`W`=`botania:gaia_ingot`×3、`D`=`#forge:storage_blocks/diamond`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `mysticalagradditions`、`botania`

### `gaia_spirit_seeds.json`

- **`mysticalagriculture:gaia_spirit_seeds`**  ←  `mysticalagradditions:gaia_spirit（seed）` + `mysticalagradditions:gaia_spirit（material）`×4 + `mysticalagradditions:gaia_spirit（essence）`×4
  - 机器：神秘农业 · 注魔祭坛 · `mysticalagriculture:infusion`
  - 前置：需要安装 `mysticalagriculture`、`mysticalagradditions`

### `imperium_farmland.json`

- **`mysticalagriculture:imperium_essence`**  ←  `mysticalagriculture:imperium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `mysticalagriculture`

### `imperium_growth_accelerator.json`

- **`mysticalagriculture:imperium_growth_accelerator`**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:tertium_essence`×6、`S`=`mysticalagriculture:tertium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `mysticalagriculture`

### `inferium_farmland.json`

- **`mysticalagriculture:inferium_essence`**  ←  `mysticalagriculture:inferium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `mysticalagriculture`

### `insanium_farmland.json`

- **`mysticalagradditions:insanium_essence`**  ←  `mysticalagradditions:insanium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `mysticalagradditions`

### `life_essence.json`

- **`botania:life_essence`**  ←  `mysticalagriculture:gaia_spirit_essence`×3
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `mysticalagriculture`、`botania`

### `magical_soil.json`

- **`kubejs:magical_soil`**  ·  图案 `#A#` / `ASA` / `#A#`
  - 键位：`S`=`mysticalagradditions:insanium_farmland`、`A`=`mysticalagradditions:dragon_scale`×4、`#`=`mysticalagradditions:insanium_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `mysticalagradditions`、`kubejs`

### `prudentium_farmland.json`

- **`mysticalagriculture:prudentium_essence`**  ←  `mysticalagriculture:prudentium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `mysticalagriculture`

### `prudentium_growth_accelerator.json`

- **`mysticalagriculture:prudentium_growth_accelerator`**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:inferium_essence`×6、`S`=`mysticalagriculture:inferium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `mysticalagriculture`

### `supremium_farmland.json`

- **`mysticalagriculture:supremium_essence`**  ←  `mysticalagriculture:supremium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `mysticalagriculture`

### `supremium_growth_accelerator.json`

- **`mysticalagriculture:supremium_growth_accelerator`**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:imperium_essence`×6、`S`=`mysticalagriculture:imperium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `mysticalagriculture`

### `terium_farmland.json`

- **`mysticalagriculture:tertium_essence`**  ←  `mysticalagriculture:tertium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：需要安装 `mysticalagriculture`

### `tertium_growth_accelerator.json`

- **`mysticalagriculture:tertium_growth_accelerator`**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:prudentium_essence`×6、`S`=`mysticalagriculture:prudentium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `mysticalagriculture`

## 8. 血魔法 Blood Magic

目录：`src/main/resources/data/alltheimbaium/recipes/blood/`

### `hellforgedparts.json`

- **`bloodmagic:hellforgedparts`**  ·  图案 ` # ` / `# #` / ` # `
  - 键位：`#`=`bloodmagic:ingot_hellforged`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `bloodmagic`

### `mineentrancekey.json`

- **`bloodmagic:mineentrancekey`**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`bloodmagic:hellforgedparts`、`#`=`#forge:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `bloodmagic`

### `minekey.json`

- **`bloodmagic:minekey`**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`bloodmagic:ingot_hellforged`、`#`=`#forge:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `bloodmagic`

### `simplekey.json`

- **`bloodmagic:simplekey`**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`#forge:storage_blocks/redstone`、`#`=`#forge:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `bloodmagic`

### `strong_tau.json`

- **`bloodmagic:strong_tau`**  ·  图案 `##` / `##`
  - 键位：`#`=`bloodmagic:weak_tau`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `bloodmagic`

## 9. 机械动力 Create

目录：`src/main/resources/data/alltheimbaium/recipes/create/`

### `life_essence_fluid.json`

- **`bloodmagic:life_essence_fluid`×500**  ←  `industrialforegoing:meat`×250 + `minecraft:water`×250
  - 机器：机械动力 · 混合搅拌 · `create:mixing`
  - 前置：需要安装 `create`、`industrialforegoing`、`bloodmagic`

## 10. 龙之研究 Draconic Evolution

目录：`src/main/resources/data/alltheimbaium/recipes/draconicevolution/`

### `small_chaos_frag.json`

- **`draconicevolution:small_chaos_frag`**  ·  图案 `ADA` / `DLD` / `ADA`
  - 键位：`A`=`alltheimbaium:package_material_x1`×4、`D`=`minecraft:dragon_head`×4、`L`=`minecraft:dragon_egg`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：需要安装 `draconicevolution`

## 11. 本模组自定义配方

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
