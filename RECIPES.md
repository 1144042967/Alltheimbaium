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

- **无产物**  ·  图案 `FFF` / `XRX` / `FFF`
  - 键位：`X`=`alltheimbaium:package_material`×2、`F`=`alltheimbaium:farmland`×6、`R`=`minecraft:redstone`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `clock.json`

- **无产物**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`、`B`=`minecraft:iron_block`×7、`C`=`minecraft:clock`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `creative_transmuter.json`

- **无产物**  ·  图案 `NNN` / `NPN` / `NNN`
  - 键位：`N`=`minecraft:netherite_block`×8、`P`=`alltheimbaium:package_material`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `eternal_sword.json`

- **无产物**  ·  图案 `ABA` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`A`=`minecraft:netherite_block`×2、`B`=`minecraft:golden_apple`×4、`C`=`minecraft:netherite_sword`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `eternal_totem.json`

- **无产物**  ·  图案 `ABA` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`A`=`minecraft:netherite_block`×2、`B`=`minecraft:golden_apple`×4、`C`=`minecraft:totem_of_undying`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `extraction_interface.json`

- **无产物**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:iron_ingot`×6、`C`=`minecraft:redstone_block`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `farmland.json`

- **无产物**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`、`B`=`minecraft:iron_ingot`×7、`C`=`minecraft:dirt`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `instant_furnace.json`

- **无产物**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:iron_block`×6、`C`=`minecraft:furnace`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `instant_inscriber.json`

- **无产物**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:iron_block`×6、`C`=`minecraft:smithing_table`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `liquid_fountain.json`

- **无产物**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:bucket`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `mob_farm.json`

- **无产物**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:glass_bottle`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `obsidian_from_buckets.json`

- **无产物**  ←  `minecraft:water_bucket` + `minecraft:lava_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `package_material_1.json`

- **无产物**  ·  图案 `AAA` / `ABA` / `AAA`
  - 键位：`A`=`#minecraft:planks`×8、`B`=`#c:cobblestone`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `package_material_2.json`

- **无产物**  ·  图案 `AAA` / `ABA` / `AAA`
  - 键位：`A`=`#c:cobblestone`×8、`B`=`#minecraft:planks`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `platform.json`

- **无产物**  ·  图案 `BXB` / `BCB` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`、`B`=`minecraft:iron_ingot`×7、`C`=`minecraft:smooth_stone`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `resource_farm.json`

- **无产物**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:dirt`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `storage_fountain.json`

- **无产物**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:iron_block`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 2. 原版简化

目录：`src/main/resources/data/alltheimbaium/recipe/crafting/`

### `allthemodium_upgrade_smithing_template.json`

- **无产物**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:allthemodium_ingot`、`B`=`minecraft:sculk`、`#`=`minecraft:netherite_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `budding_amethyst.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`minecraft:amethyst_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `certus_quartz_dust.json`

- **无产物**  ←  `enderio:powdered_quartz` + `minecraft:water_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `moss_block.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`minecraft:wheat_seeds`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `netherite_upgrade_smithing_template.json`

- **无产物**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`minecraft:netherite_ingot`、`B`=`minecraft:netherrack`、`#`=`minecraft:diamond`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `obsidian_from_buckets.json`

- **无产物**  ←  `minecraft:water_bucket` + `minecraft:lava_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `powdered_quartz.json`

- **无产物**  ←  `ae2:certus_quartz_dust` + `minecraft:lava_bucket`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `spawner.json`

- **无产物**  ·  图案 `###` / `#A#` / `###`
  - 键位：`A`=`minecraft:nether_star`、`#`=`minecraft:chain`×8
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `supply_crate.json`

- **无产物**  ·  图案 `BBB` / `XCX` / `BBB`
  - 键位：`X`=`alltheimbaium:package_material`×2、`B`=`minecraft:diamond_block`×6、`C`=`minecraft:chest`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `unobtainium_upgrade_smithing_template.json`

- **无产物**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:unobtainium_ingot`、`B`=`allthemodium:piglich_heart`、`#`=`allthemodium:vibranium_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `vibranium_upgrade_smithing_template.json`

- **无产物**  ·  图案 `#A#` / `#B#` / `###`
  - 键位：`A`=`allthemodium:vibranium_ingot`、`B`=`minecraft:gilded_blackstone`、`#`=`allthemodium:allthemodium_ingot`×7
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `wheat_seeds.json`

- **无产物**  ←  `minecraft:wheat`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

## 3. 拆解回收

目录：`src/main/resources/data/alltheimbaium/recipe/salvaging/`

### `allthemodium_axe.json`

- **无产物**  ←  `allthemodium:allthemodium_axe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_boots.json`

- **无产物**  ←  `allthemodium:allthemodium_boots`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_chestplate.json`

- **无产物**  ←  `allthemodium:allthemodium_chestplate`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_helmet.json`

- **无产物**  ←  `allthemodium:allthemodium_helmet`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_hoe.json`

- **无产物**  ←  `allthemodium:allthemodium_hoe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_leggings.json`

- **无产物**  ←  `allthemodium:allthemodium_leggings`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_mace.json`

- **无产物**  ←  `allthemodium:allthemodium_mace`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_pickaxe.json`

- **无产物**  ←  `allthemodium:allthemodium_pickaxe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_shovel.json`

- **无产物**  ←  `allthemodium:allthemodium_shovel`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `allthemodium_sword.json`

- **无产物**  ←  `allthemodium:allthemodium_sword`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `enchanted_book.json`

- **无产物**  ←  `minecraft:enchanted_book`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `fishing_rod.json`

- **无产物**  ←  `minecraft:fishing_rod`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `potion.json`

- **无产物**  ←  `minecraft:potion`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `saddle.json`

- **无产物**  ←  `minecraft:saddle`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_axe.json`

- **无产物**  ←  `allthemodium:unobtainium_axe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_boots.json`

- **无产物**  ←  `allthemodium:unobtainium_boots`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_chestplate.json`

- **无产物**  ←  `allthemodium:unobtainium_chestplate`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_helmet.json`

- **无产物**  ←  `allthemodium:unobtainium_helmet`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_hoe.json`

- **无产物**  ←  `allthemodium:unobtainium_hoe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_leggings.json`

- **无产物**  ←  `allthemodium:unobtainium_leggings`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_mace.json`

- **无产物**  ←  `allthemodium:unobtainium_mace`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_pickaxe.json`

- **无产物**  ←  `allthemodium:unobtainium_pickaxe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_shovel.json`

- **无产物**  ←  `allthemodium:unobtainium_shovel`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `unobtainium_sword.json`

- **无产物**  ←  `allthemodium:unobtainium_sword`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_axe.json`

- **无产物**  ←  `allthemodium:vibranium_axe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_boots.json`

- **无产物**  ←  `allthemodium:vibranium_boots`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_chestplate.json`

- **无产物**  ←  `allthemodium:vibranium_chestplate`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_helmet.json`

- **无产物**  ←  `allthemodium:vibranium_helmet`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_hoe.json`

- **无产物**  ←  `allthemodium:vibranium_hoe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_leggings.json`

- **无产物**  ←  `allthemodium:vibranium_leggings`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_mace.json`

- **无产物**  ←  `allthemodium:vibranium_mace`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_pickaxe.json`

- **无产物**  ←  `allthemodium:vibranium_pickaxe`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_shovel.json`

- **无产物**  ←  `allthemodium:vibranium_shovel`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

### `vibranium_sword.json`

- **无产物**  ←  `allthemodium:vibranium_sword`
  - 机器：寂静装备 · 拆解台 · `silentgear:salvaging`
  - 前置：无（始终生效）

## 4. AE2 / 高级 AE

目录：`src/main/resources/data/alltheimbaium/recipe/ae2/`

### `accumulation_processor_chamber.json`

- **`megacells:accumulation_processor`×64**  ←  `minecraft:lava`×1600 + `megacells:printed_accumulation_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `calculation_processor_chamber.json`

- **`ae2:calculation_processor`×64**  ←  `minecraft:lava`×1600 + `ae2:printed_calculation_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `certus_quartz_dust04.json`

- **无产物**  ←  `ae2:quartz_block`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `certus_quartz_dust16.json`

- **无产物**  ←  `ae2:damaged_budding_quartz`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `certus_quartz_dust64.json`

- **无产物**  ←  `ae2:chipped_budding_quartz`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `chipped_budding_quartz.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:damaged_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `damaged_budding_quartz.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:quartz_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `ender_dust09.json`

- **无产物**  ←  `allthetweaks:ender_pearl_block`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `ender_dust64.json`

- **无产物**  ←  `allthecompressed:ender_pearl_block_1x`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `energy_processor_chamber.json`

- **`appflux:energy_processor`×64**  ←  `minecraft:lava`×1600 + `appflux:printed_energy_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `engineering_processor_chamber.json`

- **`ae2:engineering_processor`×64**  ←  `minecraft:lava`×1600 + `ae2:printed_engineering_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `flawed_budding_quartz.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:chipped_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `flawless_budding_quartz.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`ae2:flawed_budding_quartz`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `logic_processor_chamber.json`

- **`ae2:logic_processor`×64**  ←  `minecraft:lava`×1600 + `ae2:printed_logic_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `printed_calculation_processor.json`

- **无产物**  ←  `ae2:chipped_budding_quartz`
  - 机器：extendedae:circuit_cutter · `extendedae:circuit_cutter`
  - 前置：无（始终生效）

### `printed_processor_calculation.json`

- **无产物**  ←  `minecraft:water`×800 + `ae2:chipped_budding_quartz`
  - 机器：扩展 AE · 电路切割机 · `expatternprovider:circuit_cutter`
  - 前置：无（始终生效）

### `quantum_alloy.json`

- **`advanced_ae:quantum_alloy`×16**  ←  `advanced_ae:quantum_infusion_source`×16000 + `minecraft:copper_block`×8 + `advanced_ae:shattered_singularity`×64 + `ae2:singularity`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_alloy_plate.json`

- **`advanced_ae:quantum_alloy_plate`×9**  ←  `advanced_ae:quantum_infusion_source`×9000 + `advanced_ae:quantum_alloy_block`×8 + `minecraft:netherite_block`×2 + `minecraft:nether_star`×9
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_infusion.json`

- **`advanced_ae:quantum_infusion_source`×16000**  ←  `minecraft:lava`×16000 + `advanced_ae:quantum_infused_dust`×16
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_processor.json`

- **`advanced_ae:quantum_processor`×64**  ←  `minecraft:lava`×1600 + `advanced_ae:printed_quantum_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `shatteredsingularity.json`

- **`advanced_ae:shattered_singularity`×64**  ←  `minecraft:water`×3200 + `ae2:singularity`×32 + `#c:dusts/ender_pearl`×64 + `ae2:sky_dust`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `sky_dust.json`

- **`ae2:sky_dust`×64**  ←  `minecraft:water`×1000 + `ae2:sky_stone_block`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `sky_dust09.json`

- **无产物**  ←  `allthecompressed:sky_stone_block_1x`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `sky_dust64.json`

- **无产物**  ←  `allthecompressed:sky_stone_block_2x`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

## 5. 通用机械 Mekanism

目录：`src/main/resources/data/alltheimbaium/recipe/mek/`

### `alloy_atomic.json`

- **无产物**  ←  `#c:storage_blocks/diamond` + `mekanism:refined_obsidian`（化学）×360
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `alloy_infused1.json`

- **无产物**  ←  `#c:storage_blocks/iron` + `mekanism:redstone`（化学）×90
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `alloy_infused2.json`

- **无产物**  ←  `#c:storage_blocks/copper` + `mekanism:redstone`（化学）×90
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `alloy_reinforced.json`

- **无产物**  ←  `#c:storage_blocks/redstone` + `mekanism:diamond`（化学）×180
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `basic_control_circuit.json`

- **无产物**  ←  `#c:storage_blocks/osmium` + `mekanism:redstone`（化学）×180
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `certus_quartz_dust.json`

- **无产物**  ←  `ae2:chipped_budding_quartz`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `creative_chemical_tank.json`

- **无产物**  ·  图案 `PPP` / `PPP` / `PPP`
  - 键位：`P`=`mekanism:ultimate_chemical_tank`×9
  - 机器：工作台（有序） · `mekanism:mek_data`
  - 前置：无（始终生效）

### `crushed_blackstone.json`

- **无产物**  ←  `minecraft:blackstone`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `crushed_deepslate.json`

- **无产物**  ←  `minecraft:cobbled_deepslate`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `crushed_end_stone.json`

- **无产物**  ←  `minecraft:end_stone`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `crushed_netherrack.json`

- **无产物**  ←  `minecraft:netherrack`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `dust.json`

- **无产物**  ←  `minecraft:sand`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `red_sand.json`

- **无产物**  ←  `minecraft:granite`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `rose_quartz.json`

- **无产物**  ←  `#c:storage_blocks/quartz` + `mekanism:redstone`（化学）×320
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `sky_dust.json`

- **无产物**  ←  `ae2:certus_quartz_dust` + `mekanism:carbon`（化学）×5
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `sky_dust2.json`

- **无产物**  ←  `ae2:chipped_budding_quartz` + `mekanism:carbon`（化学）×80
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `steel_ingot.json`

- **无产物**  ←  `alltheores:iron_dust` + `mekanism:carbon`（化学）×20
  - 机器：通用机械 · 冶金灌注机 · `mekanism:metallurgic_infusing`
  - 前置：无（始终生效）

### `wheat_flour.json`

- **无产物**  ←  `minecraft:wheat`
  - 机器：通用机械 · 富集仓 · `mekanism:enriching`
  - 前置：无（始终生效）

## 6. 热力系列 Thermal

目录：`src/main/resources/data/alltheimbaium/recipe/thermal/`

### `activationcrystalweak.json`

- **无产物**  ←  `bloodmagic:lavacrystal` + `bloodmagic:life_essence_fluid`×10000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `airscribetool.json`

- **无产物**  ←  `minecraft:ghast_tear` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `alchemy_flask.json`

- **无产物**  ←  `minecraft:glass_bottle` + `bloodmagic:life_essence_fluid`×4000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `apprenticebloodorb.json`

- **无产物**  ←  `#c:storage_blocks/redstone` + `bloodmagic:life_essence_fluid`×5000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `archmagebloodorb.json`

- **无产物**  ←  `bloodmagic:dungeon_metal` + `bloodmagic:life_essence_fluid`×80000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `blankslate.json`

- **无产物**  ←  `#c:stone` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `bleedingedge.json`

- **无产物**  ←  `bloodmagic:rawdemoniteblock` + `bloodmagic:life_essence_fluid`×10000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `blood.json`

- **`bloodmagic:life_essence_fluid`×1000**  ←  `evilcraft:blood`×1000
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：无（始终生效）

### `daggerofsacrifice.json`

- **无产物**  ←  `minecraft:iron_sword` + `bloodmagic:life_essence_fluid`×3000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `defaultcrystal.json`

- **无产物**  ←  `#c:gems/quartz` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `demonslate.json`

- **无产物**  ←  `bloodmagic:infusedslate` + `bloodmagic:life_essence_fluid`×15000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `duskscribetool.json`

- **无产物**  ←  `#c:storage_blocks/coal` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `earthscribetool.json`

- **无产物**  ←  `#c:obsidian` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `enhancedteleposerfocus.json`

- **无产物**  ←  `bloodmagic:teleposerfocus` + `bloodmagic:life_essence_fluid`×10000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `etherealslate.json`

- **无产物**  ←  `bloodmagic:demonslate` + `bloodmagic:life_essence_fluid`×30000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `firescribetool.json`

- **无产物**  ←  `minecraft:magma_cream` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `infusedslate.json`

- **无产物**  ←  `bloodmagic:reinforcedslate` + `bloodmagic:life_essence_fluid`×5000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `ingot_hellforged.json`

- **无产物**  ←  `#c:ingots/netherite` + `bloodmagic:life_essence_fluid`×8000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `life_essence_fluid.json`

- **`evilcraft:blood`×1000**  ←  `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：无（始终生效）

### `magicianbloodorb.json`

- **无产物**  ←  `#c:storage_blocks/gold` + `bloodmagic:life_essence_fluid`×25000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `masterbloodorb.json`

- **无产物**  ←  `bloodmagic:weakbloodshard` + `bloodmagic:life_essence_fluid`×40000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `meat.json`

- **`bloodmagic:life_essence_fluid`×1000**  ←  `industrialforegoing:meat`×1000
  - 机器：热力 · 精炼厂 · `thermal:refinery`
  - 前置：无（始终生效）

### `rawdemonite.json`

- **无产物**  ←  `#c:ores/netherite_scrap` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `reinforcedslate.json`

- **无产物**  ←  `bloodmagic:blankslate` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `soulsnare.json`

- **无产物**  ←  `#c:string` + `bloodmagic:life_essence_fluid`×500
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `teleposerfocus.json`

- **无产物**  ←  `#c:ender_pearls` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `waterscribetool.json`

- **无产物**  ←  `#c:storage_blocks/lapis` + `bloodmagic:life_essence_fluid`×1000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

### `weakbloodorb.json`

- **无产物**  ←  `#c:gems/diamond` + `bloodmagic:life_essence_fluid`×2000
  - 机器：热力 · 灌装机 · `thermal:bottler`
  - 前置：无（始终生效）

## 7. 神秘农业 Mystical Agriculture

目录：`src/main/resources/data/alltheimbaium/recipe/mystical/`

### `gaia_spirit_crux.json`

- **无产物**  ·  图案 `EHE` / `WDW` / `EWE`
  - 键位：`E`=`mysticalagradditions:insanium_essence`×4、`H`=`botania:terrasteel_block`、`W`=`botania:gaia_ingot`×3、`D`=`#c:storage_blocks/diamond`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `gaia_spirit_seeds.json`

- **无产物**  ←  `mysticalagradditions:gaia_spirit（seed）` + `mysticalagradditions:gaia_spirit（material）`×4 + `mysticalagradditions:gaia_spirit（essence）`×4
  - 机器：神秘农业 · 注魔祭坛 · `mysticalagriculture:infusion`
  - 前置：无（始终生效）

### `imperium_farmland.json`

- **无产物**  ←  `mysticalagriculture:imperium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `imperium_growth_accelerator.json`

- **无产物**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:tertium_essence`×6、`S`=`mysticalagriculture:tertium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `inferium_farmland.json`

- **无产物**  ←  `mysticalagriculture:inferium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `insanium_farmland.json`

- **无产物**  ←  `mysticalagradditions:insanium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `life_essence.json`

- **无产物**  ←  `mysticalagriculture:gaia_spirit_essence`×3
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `magical_soil.json`

- **无产物**  ·  图案 `#A#` / `ASA` / `#A#`
  - 键位：`S`=`mysticalagradditions:insanium_farmland`、`A`=`mysticalagradditions:dragon_scale`×4、`#`=`mysticalagradditions:insanium_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `prudentium_farmland.json`

- **无产物**  ←  `mysticalagriculture:prudentium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `prudentium_growth_accelerator.json`

- **无产物**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:inferium_essence`×6、`S`=`mysticalagriculture:inferium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `supremium_farmland.json`

- **无产物**  ←  `mysticalagriculture:supremium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `supremium_growth_accelerator.json`

- **无产物**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:imperium_essence`×6、`S`=`mysticalagriculture:imperium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `terium_farmland.json`

- **无产物**  ←  `mysticalagriculture:tertium_farmland`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `tertium_growth_accelerator.json`

- **无产物**  ·  图案 `# #` / `#S#` / `# #`
  - 键位：`#`=`mysticalagriculture:prudentium_essence`×6、`S`=`mysticalagriculture:prudentium_growth_accelerator`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 8. 血魔法 Blood Magic

目录：`src/main/resources/data/alltheimbaium/recipe/blood/`

### `hellforgedparts.json`

- **无产物**  ·  图案 ` # ` / `# #` / ` # `
  - 键位：`#`=`bloodmagic:ingot_hellforged`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `mineentrancekey.json`

- **无产物**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`bloodmagic:hellforgedparts`、`#`=`#c:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `minekey.json`

- **无产物**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`bloodmagic:ingot_hellforged`、`#`=`#c:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `simplekey.json`

- **无产物**  ·  图案 `#A` / `# ` / `B `
  - 键位：`A`=`#c:storage_blocks/redstone`、`#`=`#c:ingots/iron`×2、`B`=`bloodmagic:infusedslate`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `strong_tau.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`bloodmagic:weak_tau`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 9. 机械动力 Create

目录：`src/main/resources/data/alltheimbaium/recipe/create/`

### `life_essence_fluid.json`

- **`bloodmagic:life_essence_fluid`×500**  ←  `industrialforegoing:meat`×250 + `minecraft:water`×250
  - 机器：机械动力 · 混合搅拌 · `create:mixing`
  - 前置：无（始终生效）

## 10. 龙之研究 Draconic Evolution

目录：`src/main/resources/data/alltheimbaium/recipe/draconicevolution/`

### `small_chaos_frag.json`

- **无产物**  ·  图案 `ADA` / `DLD` / `ADA`
  - 键位：`A`=`alltheimbaium:package_material`×4、`D`=`minecraft:dragon_head`×4、`L`=`minecraft:dragon_egg`
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

## 11. advanced_ae

目录：`src/main/resources/data/alltheimbaium/recipe/advanced_ae/`

### `quantum_alloy.json`

- **`advanced_ae:quantum_alloy`×16**  ←  `advanced_ae:quantum_infusion_source`×16000 + `minecraft:copper_block`×8 + `advanced_ae:shattered_singularity`×64 + `ae2:singularity`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_alloy_plate.json`

- **`advanced_ae:quantum_alloy_plate`×16**  ←  `advanced_ae:quantum_infusion_source`×1000 + `advanced_ae:quantum_alloy_block`×15 + `minecraft:netherite_ingot`×32 + `minecraft:nether_star`×16
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_infusion_source.json`

- **`advanced_ae:quantum_infusion_source`×16000**  ←  `minecraft:lava`×4000 + `advanced_ae:quantum_infused_dust`×16
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

## 12. evilcraft

目录：`src/main/resources/data/alltheimbaium/recipe/evilcraft/`

### `blood.json`

- **`evilcraft:blood`×16000**  ←  `industrialforegoing:meat`×16000 + `minecraft:charcoal`×16
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

## 13. extendedae

目录：`src/main/resources/data/alltheimbaium/recipe/extendedae/`

### `accumulation_processor.json`

- **`megacells:accumulation_processor`×64**  ←  `minecraft:water`×16000 + `megacells:printed_accumulation_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/fluix`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `calculation_processor.json`

- **`ae2:calculation_processor`×64**  ←  `minecraft:water`×16000 + `ae2:printed_calculation_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `concurrent_processor.json`

- **`extendedae:concurrent_processor`×64**  ←  `minecraft:water`×16000 + `extendedae:concurrent_processor_print`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `concurrent_processor_print.json`

- **无产物**  ←  `extendedae:entro_budding_half`
  - 机器：extendedae:circuit_cutter · `extendedae:circuit_cutter`
  - 前置：无（始终生效）

### `engineering_processor.json`

- **`ae2:engineering_processor`×64**  ←  `minecraft:water`×16000 + `ae2:printed_engineering_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `entro_budding_fully.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`extendedae:entro_budding_mostly`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `entro_budding_half.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`extendedae:entro_budding_hardly`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `entro_budding_hardly.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`extendedae:entro_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `entro_budding_mostly.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`extendedae:entro_budding_half`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `entro_dust04.json`

- **无产物**  ←  `extendedae:entro_block`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `entro_dust16.json`

- **无产物**  ←  `extendedae:entro_budding_hardly`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `entro_dust64.json`

- **无产物**  ←  `extendedae:entro_budding_half`
  - 机器：通用机械 · 粉碎机 · `mekanism:crushing`
  - 前置：无（始终生效）

### `logic_processor.json`

- **`ae2:logic_processor`×64**  ←  `minecraft:water`×16000 + `ae2:printed_logic_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `quantum_processor.json`

- **`advanced_ae:quantum_processor`×64**  ←  `minecraft:water`×16000 + `advanced_ae:printed_quantum_processor`×64 + `ae2:printed_silicon`×64 + `#c:dusts/redstone`×64
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `shattered_singularity.json`

- **无产物**  ←  `minecraft:lava`×100 + `ae2:singularity`×32 + `ae2:ender_dust`×64 + `ae2:sky_dust`×64
  - 机器：extendedae:crystal_assembler · `extendedae:crystal_assembler`
  - 前置：无（始终生效）

## 14. forbidden_arcanus

目录：`src/main/resources/data/alltheimbaium/recipe/forbidden_arcanus/`

### `artisan_relic.json`

- **无产物**  ←  `forbidden_arcanus:maledictus_pact`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `crescent_moon.json`

- **无产物**  ←  `forbidden_arcanus:artisan_relic`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `crimson_stone.json`

- **无产物**  ←  `forbidden_arcanus:crescent_moon`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `divine_pact.json`

- **无产物**  ←  `forbidden_arcanus:elementarium`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `elementarium.json`

- **无产物**  ←  `forbidden_arcanus:crimson_stone`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `maledictus_pact.json`

- **无产物**  ←  `forbidden_arcanus:divine_pact`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

## 15. jdt

目录：`src/main/resources/data/alltheimbaium/recipe/jdt/`

### `celestigem.json`

- **无产物**  ←  `justdirethings:gooblock_tier3` + `minecraft:diamond_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `coal_t1.json`

- **无产物**  ←  `justdirethings:gooblock_tier1` + `minecraft:coal_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `coal_t2.json`

- **无产物**  ←  `justdirethings:gooblock_tier2` + `justdirethings:coalblock_t1`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `coal_t3.json`

- **无产物**  ←  `justdirethings:gooblock_tier3` + `justdirethings:coalblock_t2`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `coal_t4.json`

- **无产物**  ←  `justdirethings:gooblock_tier4` + `justdirethings:coalblock_t3`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `polymorphic_fluid_source.json`

- **`justdirethings:polymorphic_fluid_source`×1000**  ←  `minecraft:water`×1000 + `justdirethings:polymorphic_catalyst`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `portal_fluid_source.json`

- **`justdirethings:portal_fluid_source`×16000**  ←  `justdirethings:unstable_portal_fluid_source`×16000 + `justdirethings:gooblock_tier3`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `raw_blazegold.json`

- **无产物**  ←  `justdirethings:gooblock_tier2` + `minecraft:gold_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `raw_eclipsealloy.json`

- **无产物**  ←  `justdirethings:gooblock_tier4` + `minecraft:netherite_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `raw_ferricore.json`

- **无产物**  ←  `justdirethings:gooblock_tier1` + `minecraft:iron_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `refined_t2_fluid_source.json`

- **`justdirethings:refined_t2_fluid_source`×16000**  ←  `justdirethings:unrefined_t2_fluid_source`×16000 + `justdirethings:gooblock_tier2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `refined_t3_fluid_source.json`

- **`justdirethings:refined_t3_fluid_source`×16000**  ←  `justdirethings:unrefined_t3_fluid_source`×16000 + `justdirethings:gooblock_tier3`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `refined_t4_fluid_source.json`

- **`justdirethings:refined_t4_fluid_source`×16000**  ←  `justdirethings:unrefined_t4_fluid_source`×16000 + `justdirethings:gooblock_tier4`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `time_crystal.json`

- **无产物**  ←  `justdirethings:gooblock_tier4` + `minecraft:diamond_block`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `time_crystal_block.json`

- **无产物**  ←  `justdirethings:gooblock_tier4` + `minecraft:budding_amethyst`
  - 机器：工作台（无序） · `minecraft:crafting_shapeless`
  - 前置：无（始终生效）

### `time_crystal_budding_block.json`

- **无产物**  ·  图案 `##` / `##`
  - 键位：`#`=`justdirethings:time_crystal_block`×4
  - 机器：工作台（有序） · `minecraft:crafting_shaped`
  - 前置：无（始终生效）

### `time_fluid_source.json`

- **`justdirethings:time_fluid_source`×1000**  ←  `justdirethings:polymorphic_fluid_source`×1000 + `justdirethings:time_crystal`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `unrefined_t2_fluid_source.json`

- **`justdirethings:unrefined_t2_fluid_source`×1000**  ←  `justdirethings:polymorphic_fluid_source`×1000 + `justdirethings:coal_t2`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `unrefined_t3_fluid_source.json`

- **`justdirethings:unrefined_t3_fluid_source`×1000**  ←  `justdirethings:refined_t2_fluid_source`×1000 + `justdirethings:coal_t3`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `unrefined_t4_fluid_source.json`

- **`justdirethings:unrefined_t4_fluid_source`×1000**  ←  `justdirethings:refined_t3_fluid_source`×1000 + `justdirethings:coal_t4`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

### `unstable_portal_fluid_source.json`

- **`justdirethings:unstable_portal_fluid_source`×1000**  ←  `justdirethings:polymorphic_fluid_source`×1000 + `justdirethings:portal_fluid_catalyst`
  - 机器：高级 AE · 反应仓 · `advanced_ae:reaction`
  - 前置：无（始终生效）

## 16. smelting

目录：`src/main/resources/data/alltheimbaium/recipe/smelting/`

### `glass_1x.json`

- **无产物**  ←  `allthecompressed:sand_1x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_2x.json`

- **无产物**  ←  `allthecompressed:sand_2x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_3x.json`

- **无产物**  ←  `allthecompressed:sand_3x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_4x.json`

- **无产物**  ←  `allthecompressed:sand_4x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_5x.json`

- **无产物**  ←  `allthecompressed:sand_5x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_6x.json`

- **无产物**  ←  `allthecompressed:sand_6x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_7x.json`

- **无产物**  ←  `allthecompressed:sand_7x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_8x.json`

- **无产物**  ←  `allthecompressed:sand_8x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `glass_9x.json`

- **无产物**  ←  `allthecompressed:sand_9x`
  - 机器：minecraft:smelting · `minecraft:smelting`
  - 前置：无（始终生效）

### `silicon.json`

- **无产物**  ←  `allthecompressed:sand_9x`
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
