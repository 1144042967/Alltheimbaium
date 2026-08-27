# Alltheimbaium (ATI) — Minecraft Forge 1.20.1 Mod

## 项目概述

一个添加"破坏平衡"机器的 Minecraft Forge 模组。可以独立使用，但设计目标是与 ATM (All The Mods) 整合包一起使用。添加了即时成熟耕地、无限资源制造机、多种生物农场等功能。

- **Mod ID**: `alltheimbaium`
- **Group**: `cn.sd.jrz`
- **Minecraft 版本**: `1.20.1`
- **Forge 版本**: `47.4.2`
- **Java 版本**: `17`
- **Mappings**: `official` (Mojang)
- **许可证**: `GNU LGPL v3`
- **版本**: `1.20.1.10`

## 构建和开发

```bash
# 运行客户端
./gradlew runClient

# 运行服务端
./gradlew runServer

# 构建 mod jar
./gradlew build

# 运行数据生成
./gradlew runData

# 发布到本地 maven 仓库
./gradlew publish
```

数据生成输出到 `src/generated/resources/`，产物包括 blockstate JSON、模型 JSON、战利品表和配方。

## 项目架构

```
src/main/java/cn/sd/jrz/alltheimbaium/
├── Alltheimbaium.java          # 主 mod 类 (@Mod)
├── block/                       # 方块类
│   ├── FarmlandBlock.java  # ATI 耕地
│   ├── ClockBlock.java                  # 时钟加速方块
│   ├── FarmBlock.java                   # 农场方块
│   ├── LiquidFountainBlock.java         # 液体无限制造机
│   ├── StorageFountainBlock.java        # 存储方块制造机
│   └── PlatformBlock.java               # 平台生成方块
├── entity/                      # BlockEntity 类
│   ├── CommonEntity.java                # 通用实体 (耕地用)
│   ├── ClockEntity.java                 # 时钟实体
│   ├── FarmEntity.java                  # 农场实体
│   ├── LiquidFountainEntity.java        # 液体制造机实体
│   └── StorageFountainEntity.java       # 存储制造机实体
├── item/                        # 物品类
│   ├── ClockItem.java                   # 时钟物品
│   ├── FarmItem.java                    # 农场物品
│   ├── LiquidFountainItem.java          # 液体制造机物品
│   ├── StorageFountainItem.java         # 存储制造机物品
│   ├── PlatformItem.java                # 平台物品
│   ├── EternalTotemItem.java            # 永恒护符
│   └── TotemEventHandler.java           # 永恒护符事件处理
├── connection/                  # Forge Capability 实现
│   ├── FarmConnection.java              # 农场 IItemHandler
│   ├── LiquidFountainConnection.java    # 液体制造机 IFluidHandler
│   └── StorageFountainConnection.java   # 存储制造机 IItemHandler
├── recipe/                       # 自定义配方
│   ├── SmeltingCraftRecipe.java         # 熔炼合成配方 (煤炭+可烧炼物品)
│   ├── BrewingCraftRecipe.java          # 酿造合成配方 (药水+酿造材料)
│   └── PotionCombineRecipe.java         # 药水融合配方 (两药水合成/牛奶净化)
└── setup/                       # 注册和配置
    ├── Registration.java                # 所有方块/物品/实体的注册
    ├── DataConfig.java                  # 农场产出数据配置
    └── Tool.java                        # 工具方法 (NBT, 数量裁剪等)
```

## 注册体系

`Registration.java` 是中心注册文件，使用 Forge 的 `DeferredRegister` 模式：

- 4 个 `DeferredRegister`: BLOCKS, ITEMS, ENTITIES, CREATIVE_MODE_TABS
- 在 `init(FMLJavaModLoadingContext)` 中注册所有内容
- 所有物品都在创造模式标签页中按顺序排列
- 公有的方块属性 (`BLOCK_PROPERTIES`): 蓝色、活塞推动时销毁、硬度/抗性 0.5

## 功能模块

### 1. ATI 耕地 (`FarmlandBlock`)

继承 `net.minecraft.world.level.block.FarmBlock`，实现 `EntityBlock`。

- 每 tick 检查上方方块：若为 `BonemealableBlock` 则催熟；若为 `CropBlock` 则设为最大生长阶段
- `canSurvive()` 始终返回 `true`，不会退化
- 只在 `PlantType.CROP` 和 `StemBlock` 类型的作物上方允许种植
- 禁止树苗等非作物种植
- 有湿润动画但无实际效果
- 掉落自身

### 2. 时钟方块 (`ClockBlock` / `ClockEntity`)

加速相邻方块 tick 的方块，类似加速火把。

- 4 个等级: x2, x4, x16, x256
- 右击切换开关状态，全局生效（所有时钟共享同一个 `active` 状态）
- `ClockEntity.tick()`: 遍历6个面，对邻接的 `EntityBlock` 额外调用 ticker `(speedMultiplier - 1)` 次
- 跳过 `ClockBlock` 和 `FarmlandBlock` （避免无限循环/过度加速）
- 对 `randomTick` 方块额外调用 `randomTick()`

### 3. 存储方块制造机 (`StorageFountainBlock` / `StorageFountainEntity`)

记录并无限复制物品（类似创造抽屉），右键打开 GUI（`StorageFountainMenu`/`StorageFountainScreen`）。

- **标记槽**（黑色信息面板右下角）：放入支持的物品 → 标记该物品类型并复制；放入已标记物品 → 取消标记并清空存量
- 物品必须带 `forge:ores`/`forge:storage_blocks`/`forge:ingots` 等标签，或是 `modern_industrialization`/`extended_industrialization` 命名空间物品
- 最多存储 9 种不同物品，对应 GUI 中 9 个已标记物品槽
- 每 20 秒 (`20 * 20 ticks`) 产出速度 +5；使用 `CARRY = 1000` 作为内部计数器进位阈值
- GUI 黑色信息面板四行：第一行增长进度条、第二行增长百分比、第三行下次增长数值、第四行产量
- 9 个已标记物品槽支持单击提取 1 个、Shift 提取 1 组、空格+单击提取到背包满；槽位左下角以 AE2 风格缩写显示存量（如 1.1K、2.1M）
- 六面输出按钮可逐面循环切换 11 个状态：随机 / 禁用 / 槽1~槽9（槽位状态显示对应物品图标）；"输出"按钮控制整体主动输出开关
- 自动向六个面传输物品，支持管道抽取
- 提供 `IItemHandler` capability（只读，不可插入）
- **方块表面渲染**（`StorageFountainRenderer` BER）：在方块四个侧面按九宫格（3×3，左上开始）绘制已标记物品贴图，参考 auto-resource 方块机渲染器；已标记物品变化时通过 `sendBlockUpdated` 同步客户端刷新

### 4. 液体无限制造机 (`LiquidFountainBlock` / `LiquidFountainEntity`)

将有限流体变为无限。内部液体由 `LiquidFountainRenderer`（BER）按存量比例渲染。

- 输入单一流体，达到阈值（配置 `liquid_fountain.infinite_threshold`，默认 `10,000,000` mB = 1 万桶）后变为无限
- 只支持 1 种流体；配置 `auto_infinite_mods` 中命名空间的流体输入后直接无限
- **右键打开 GUI**（化学品储罐风格，`LiquidFountainMenu`/`LiquidFountainScreen`）：
  - 左上 `+ 槽`：放液体桶/带液容器 → 液体输入机器；放空桶/空容器 → 从机器装液体；操作完毕的桶/容器移到下方 `- 槽`
  - `- 槽` 只读，玩家/管道可取走
  - 中间背景块上六个按键逐面控制主动输出开关（NBT 持久化）
  - 右上进度条展示阈值进度，下方信息面板显示 `当前/阈值 mB 流体名` 与 `还需 X mB 达到无限`，无限后显示 `无限 流体名` / `流体已达到无限`
- 无限状态下每 tick 向六个面（受开关控制）输出 `Integer.MAX_VALUE` mB
- 手持空桶/带液容器右键可直接交互（`FluidUtil` 装取），否则打开 GUI
- 提供 `IFluidHandler`（管道输入输出，未无限时可输入/输出存量，无限后只输出）与 `IItemHandler`（+ 槽可插入、- 槽可抽取）capability
- 破坏时 + 槽 / - 槽中的物品掉落

### 5. 农场 (`FarmBlock` / `FarmEntity`)

42 种不同的怪物/资源农场。

- 每种农场对应一个 `FarmBlock` + `FarmEntity` + `FarmItem`
- 产物在 `DataConfig.java` 中定义，每个配置包含产物列表（物品 + 每级产量权重）
- 等级每 20 秒增长 1 级
- 产出计算: 每 tick `outputArray[i] += product.count * level`
- 使用 `CARRY = 10000` 进位机制
- 自动向六个面传输可产出物品
- 右击空手可取出产物，手持产物可批量取出
- 手持同种农场方块可叠加等级
- 物品 hover 显示详细产物信息和等级

**农场分类**:
| 阶级 | 包含生物 |
|------|---------|
| 1 (基础资源) | 圆石、树木、竹子、甘蔗、骨粉、冰、蜜蜂、青蛙、鱿鱼 |
| 2 (被动生物) | 鸡、兔、猪、羊、牛、村民 |
| 3 (敌对生物) | 僵尸、骷髅、苦力怕、蜘蛛、史莱姆、女巫、溺尸、末影人、守卫者、幻翼、岩浆怪、疣猪兽、僵尸猪灵、铁傀儡 |
| 4 (高级) | 潜影贝、恶魂、烈焰人、凋零骷髅 |
| 5 (袭击) | 掠夺者、唤魔者、劫掠兽 |
| 6 (Boss) | 监守者、凋零、末影龙 |

每种农场都产出对应刷怪蛋(1/10000权重) + 其他掉落物。

### 6. 平台方块 (`PlatformBlock`)

右击生成一个 3×3 区块的平台区域。

- 中心区块填满 `SMOOTH_STONE`
- 区块边界用 `STONE_BRICKS` 标识
- 只替换空气方块
- 手持物品时不会触发

### 7. 永恒图腾 (`EternalTotemItem` / `TotemEventHandler`)

类似不朽图腾但可重复使用且自动触发。

- `@Mod.EventBusSubscriber` 注册事件
- 监听 `LivingDeathEvent`，若玩家物品栏任意位置有永恒图腾且已启用，取消死亡并施加增益
- 死亡后给予: 吸收 IV、生命恢复 III、抗火 I、抗性 II、速度 II、力量 II (均为 60 秒)
- **额外功能**: 右击 Mekanism 的终极化学品储罐可升级为创造化学品储罐
- 右击切换开关状态
- 优先级 `HIGHEST` 确保在其他 mod 前处理

### 8. SmeltingCraftRecipe (自定义合成配方)

`recipe/SmeltingCraftRecipe.java` — 一个动态合成配方，可以用煤炭在合成台中批量烧炼物品，无需熔炉。

- 继承 `CustomRecipe`，使用 `SimpleCraftingRecipeSerializer`
- 配方模式 (3×3 合成台):
  - 外围 8 格放同一种可被烧炼/爆破/烟熏的物品
  - 中心 1 格放煤炭或木炭 (`ItemTags.COALS`)
- 输出: 8 个烧炼产物（不超过该物品的最大堆叠数）
- 动态查询 `RecipeManager`，支持来自任何 mod 的熔炉配方
- 按优先级依次查找 `SMELTING` → `BLASTING` → `SMOKING` 配方
- 服务端计算配方，客户端跳过
- `isSpecial()` 返回 `true`，不出现在配方书中
- 需要 3×3 合成台（2×2 背包合成格不适用）
- 使用 `cachedResult` 字段在 `matches()` 和 `assemble()` 之间传递结果
  - `matches()` 每次以本次网格为准：开头清空缓存，匹配成功才写入结果
  - `assemble()`/`getResultItem()` 只读缓存、不清空——避免 Polymorph / FastWorkbench 等 mod 在一次合成流程中多次调用 `getResultItem()`/`assemble()` 时读到空结果（否则手工放置时结果槽会被错误覆盖为空）

### 9. BrewingCraftRecipe (酿造合成配方)

`recipe/BrewingCraftRecipe.java` — 将原版酿造台配方转移到工作台的动态合成配方。

- 继承 `CustomRecipe`，使用 `SimpleCraftingRecipeSerializer`
- 无序配方：工作台中恰好放入 2 个物品（任意位置）
  - 1 个药水类物品（`isPotionItem()` 判断）
  - 1 个酿造材料（通过 `PotionBrewing.isIngredient()` 验证）
- 输出：1 瓶原版酿造后药水，类型与输入一致（喷溅入→喷溅出）
- 动态查询 `PotionBrewing` 获取所有酿造配方
- **与 PotionCombineRecipe 的分工**：原版药水 + 普通酿造材料走此配方；混合药水（有自定义效果）+ 火药/龙息/奶桶走 PotionCombineRecipe 的类型转换
- **关键 API 细节**：`PotionBrewing.hasMix(药水, 材料)` 和 `PotionBrewing.mix(材料, 药水)` 的参数顺序相反
- 尝试两种物品朝向（药水+材料 或 材料+药水）
- 客户端和服务端均可计算（`PotionBrewing` 数据两端一致）
- 支持 2×2 背包合成格（`canCraftInDimensions` 要求格子数 ≥ 2）

### 10. PotionCombineRecipe (混合药水合成配方)

`recipe/PotionCombineRecipe.java` — 混合药水合成与类型转换的动态配方。

- 继承 `CustomRecipe`，使用 `SimpleCraftingRecipeSerializer`
- 无序配方：工作台中恰好放入 2 个物品

**核心规则**：
| 输入 | 输出 | 说明 |
|------|------|------|
| 任意两瓶药水 | 混合药水 | 原版+原版、原版+混合、混合+混合 |
| 混合药水 + 火药 | 喷溅混合药水 | 仅混合药水可用 |
| 混合药水 + 龙息 | 滞留混合药水 | 仅混合药水可用 |
| 混合药水 + 奶桶 | 普通混合药水 | 仅混合药水可用 |

**原版与混合的识别**：混合药水有自定义效果（`PotionUtils.getCustomEffects()` 非空），原版药水没有。混合药水 + 火药/龙息/奶桶 → 走本配方类型转换；原版药水 + 酿造材料 → 走 `BrewingCraftRecipe`。

**输出类型优先级**（融合时）：滞留 > 喷溅 > 普通（`getOutputPotionType()`）。

**效果合并规则**（`mergeEffect()`）：
- 等级：取两者中更高者（`Math.max(ampA, ampB)`）
- 持续时间：
  - 等级来自 A（ampA > ampB）→ 取 A 的时间
  - 等级来自 B（ampB > ampA）→ 取 B 的时间
  - 等级相同（ampA == ampB）→ (d1 + d2) × 0.75

**效果读取**（`getResolvedEffects()`）：
- 混合药水（有自定义效果）→ 直接使用自定义效果
- 原版药水（无自定义效果）→ 从基础类型读取
- 容器类型（喷溅/滞留/普通）不参与时长计算，所有时长均为实际存储值

**命名**（`buildPotionName()`）：
- 按持续时间降序取前两种效果名称
- 根据输出类型添加后缀：混合药水 / 混合喷溅药水 / 混合滞留药水
- 6 个语言键（1/2 种效果 × 3 种类型）+ 1 个平凡药水键

### 11. 额外物品

- `package_material_x1/x2/x3` — 封装材料 (合成中间物)
- `block_diamond_x8/gold_x8/silicon_x8/quantum_alloy_x8/sky_steel_x8` — 合成材料

## NBT 数据存储

各 Entity 通过 `saveAdditional`/`load` 持久化数据:

- **FarmEntity**: `level`(long), `output_array`(long[]), `save_array`(long[])
- **StorageFountainEntity**: `output`(long), `save_stick`(ListTag: 每项含物品NBT + `Long_Count`), `directionState`(int[6]: 每面 0~10 对应 随机/禁用/槽1~槽9), `outputEnabled`(boolean), `markerSlot`(CompoundTag)
- **LiquidFountainEntity**: `fluid_id`(string), `fluid_amount`(int), `transferDown/Up/North/South/West/East`(boolean), `inputSlot`(+槽), `outputSlot`(-槽)
- 所有数值加载时经过 `Tool.suit()` 防负数处理

## Tool 工具方法

- `suit(long)` / `suitInt(long)`: 防溢出裁剪，负值返回 `Long.MAX_VALUE` / `Integer.MAX_VALUE`
- `takeItem(Player, ItemStack)`: 尝试给玩家物品，失败则丢到地上
- `toJsonArray` / `toItemList` / `toBlockList`: 物品列表 ↔ NBT ListTag 转换
- `sort`: 物品列表按注册名排序 (minecraft: 物品优先)

## 资源文件

- `src/main/resources/assets/alltheimbaium/textures/block/` — 方块纹理 PNG
- `src/generated/resources/assets/alltheimbaium/` — 自动生成的 blockstates, models, loot_tables, recipes, lang
- `src/generated/resources/data/alltheimbaium/recipes/` — 所有合成配方 JSON

## 依赖

- **Forge** 1.20.1-47.4.2 (唯一硬依赖)
- 可选联动 (通过标签/配方):
  - AE2 (Applied Energistics 2)
  - Mekanism (通用机械)
  - Blood Magic (血魔法)
  - Mystical Agriculture (神秘农业)
  - Silent Gear (寂静装备)
  - Thermal Series (热力系列)
  - Modern Industrialization / Extended Industrialization

## 命名规范

- 所有注册名不带前缀，依靠 mod ID 命名空间区分
- 语言键格式: `block.alltheimbaium.<name>`, `item.alltheimbaium.<name>`, `screen.alltheimbaium.<name>`, `chat.alltheimbaium.<name>`
- 类名驼峰命名，包名全小写
- 所有 BlockEntity 实现尾气级异常处理（try-catch + log error）

## 代码风格约定

- 所有文件使用 UTF-8 编码
- 使用 `@Nonnull`/`@Nullable` 注解标记参数
- 过时 API 用 `@SuppressWarnings("deprecation")` 抑制警告
- 内部使用 SLF4J 日志
- 方块/物品类中 tick 逻辑在私有方法中，通过匿名 `BlockEntityTicker` lambda 调用
- 所有公开交互方法（`use` 等）的最外层捕获 Throwable
