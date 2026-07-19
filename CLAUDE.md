# Alltheimbaium (ATI) — Minecraft NeoForge 1.21.1 Mod

## 项目概述

一个添加"破坏平衡"机器的 Minecraft NeoForge 模组。可以独立使用，但设计目标是与 ATM (All The Mods) 整合包一起使用。添加了即时成熟耕地、无限资源制造机、多种生物农场等功能。

- **Mod ID**: `alltheimbaium`
- **Group**: `cn.sd.jrz`
- **Minecraft 版本**: `1.21.1`
- **NeoForge 版本**: `21.1.191`
- **Java 版本**: `21`
- **Mappings**: `parchment` (2024.11.17)
- **许可证**: `GNU LGPL v3`
- **版本**: `1.21.1.9`

> **与 1.20.1 版本的区别**: 本版本基于 NeoForge，使用 DataComponent 系统、Parchment mappings，并移除了永恒图腾和 SmeltingCraftRecipe 功能。详见项目 master 分支中的合并文档。

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
├── Alltheimbaium.java              # 主 mod 类 (@Mod, NeoForge IEventBus)
├── block/                          # 方块类
│   ├── AlltheimbaiumFarmlandBlock.java  # ATI 耕地
│   ├── ClockBlock.java                  # 时钟加速方块 (含tick逻辑)
│   ├── FarmBlock.java                   # 农场方块
│   ├── LiquidFountainBlock.java         # 液体无限制造机
│   ├── StorageFountainBlock.java        # 存储方块制造机
│   └── PlatformBlock.java               # 平台生成方块
├── entity/                         # BlockEntity 类
│   ├── CommonEntity.java                # 通用实体 (时钟、耕地用)
│   ├── FarmEntity.java                  # 农场实体
│   ├── LiquidFountainEntity.java        # 液体制造机实体
│   └── StorageFountainEntity.java       # 存储制造机实体
├── item/                           # 物品类
│   ├── ClockItem.java                   # 时钟物品
│   ├── FarmItem.java                    # 农场物品
│   ├── LiquidFountainItem.java          # 液体制造机物品
│   ├── StorageFountainItem.java         # 存储制造机物品
│   └── PlatformItem.java                # 平台物品
├── connection/                     # NeoForge Capability 实现
│   ├── FarmConnection.java              # 农场 IItemHandler
│   ├── LiquidFountainConnection.java    # 液体制造机 IFluidHandler
│   └── StorageFountainConnection.java   # 存储制造机 IItemHandler
└── setup/                          # 注册和配置
    ├── Registration.java                # 所有方块/物品/实体的注册
    ├── DataConfig.java                  # 农场产出数据配置
    └── Tool.java                        # 工具方法 (NBT, 数量裁剪等)
```

## 注册体系

`Registration.java` 是中心注册文件，使用 NeoForge 的 `DeferredRegister` 模式：

- 5 个 `DeferredRegister`: DATA_COMPONENT_TYPES, BLOCKS, ITEMS, ENTITIES, CREATIVE_MODE_TABS
- 在 `init(IEventBus)` 中注册所有内容
- Capability 通过 `RegisterCapabilitiesEvent` 事件注册
- 所有物品都在创造模式标签页中按顺序排列
- 公有的方块属性 (`BLOCK_PROPERTIES`): 蓝色、活塞推动时销毁、硬度/抗性 0.5

### DataComponent 系统 (1.21.1 独有)

使用 NeoForge 的 `DataComponentType` 进行数据持久化：
- `BLOCK_DATA` (`DataComponentType<String>`): 存储农场产出数据（逗号分隔的字符串）
- 通过 `Codec.STRING` + `ByteBufCodecs.STRING_UTF8` 实现序列化与网络同步
- 在物品构造时通过 `component(BLOCK_DATA.get(), "")` 注册默认值
- Item hover 文本通过 `stack.getOrDefault()` 读取数据

## 功能模块

### 1. ATI 耕地 (`AlltheimbaiumFarmlandBlock`)

继承 `net.minecraft.world.level.block.FarmBlock`，实现 `EntityBlock`。

- 每 tick 检查上方方块：若为 `BonemealableBlock` 则催熟；若为 `CropBlock` 则设为最大生长阶段
- `canSurvive()` 始终返回 `true`，不会退化
- 只在 `PlantType.CROP` 和 `StemBlock` 类型的作物上方允许种植
- 禁止树苗等非作物种植
- 有湿润动画但无实际效果
- 掉落自身

### 2. 时钟方块 (`ClockBlock` + `CommonEntity`)

加速相邻方块 tick 的方块，类似加速火把。

- **注意**: 在 1.21.1 版本中，tick 加速逻辑直接内嵌在 `ClockBlock` 中，使用 `CommonEntity` 作为基础的 BlockEntity
- 4 个等级: x2, x4, x16, x256
- 使用 `static boolean active` 全局开关状态（所有时钟共享）
- 右击切换开关状态
- tick 逻辑: 遍历6个面，对邻接的 `EntityBlock` 额外调用 ticker `(speedMultiplier - 1)` 次
- 跳过 `ClockBlock` 和 `AlltheimbaiumFarmlandBlock`（避免无限循环/过度加速）
- 对 `randomTick` 方块额外调用 `randomTick()`
- `useWithoutItem()` 和 `useItemOn()` 两个交互方法（1.21.1 NeoForge 的新交互模式）

### 3. 存储方块制造机 (`StorageFountainBlock` / `StorageFountainEntity`)

记录并无限复制物品（类似创造抽屉）。

- 手持带有 `forge:ores`/`forge:storage_blocks`/`forge:ingots` 等标签的物品右击注册
- 也支持 `modern_industrialization`/`extended_industrialization` 命名空间的物品
- 最多存储 9 种不同物品
- 每 20 秒 (`20 * 20 ticks`) 产出速度 +5
- 使用 `CARRY = 1000` 作为内部计数器进位阈值
- 自动向六个面传输物品，支持管道抽取
- 可手持同种方块叠加等级和已记录物品
- 提供 `IItemHandler` capability（只读，不可插入）

### 4. 液体无限制造机 (`LiquidFountainBlock` / `LiquidFountainEntity`)

将有限流体变为无限。

- 输入单一流体，达到 `10,000 * 1,000 = 10,000,000` mB 后变为无限
- 只支持 1 种流体
- 无限状态下每 tick 向六个面输出 `Integer.MAX_VALUE` mB
- 提供 `IFluidHandler` capability

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
- 物品 hover 显示详细产物信息和等级（使用 `DataComponentType` 读取数据）

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

### 7. 额外物品

- `package_material_x1/x2/x3` — 封装材料 (合成中间物)
- `block_diamond_x8/gold_x8/silicon_x8/quantum_alloy_x8/sky_steel_x8` — 合成材料

## NBT 数据存储

各 Entity 通过 `saveAdditional`/`loadAdditional` 持久化数据（1.21.1 新增 `HolderLookup.Provider` 参数）:

- **FarmEntity**: `level`(long), `output_array`(long[]), `save_array`(long[])
- **StorageFountainEntity**: `output`(long), `save_stick`(ListTag: 每项含物品NBT + `Long_Count`)
- **LiquidFountainEntity**: `fluid_id`(string), `fluid_amount`(int)
- 所有数值加载时经过 `Tool.suit()` 防负数处理

物品级别的数据通过 `DataComponentType` + `applyImplicitComponents`/`collectImplicitComponents` 实现，替代了旧版的 NBT `BlockEntityTag` 方式。

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

- **NeoForge** 21.1.191 (唯一硬依赖)
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
- 内部使用 SLF4J 日志
- 使用 NeoForge 的 `@OnlyIn(Dist.CLIENT)` 注解
- 使用 `useWithoutItem()` + `useItemOn()` 替代旧版 `use()` 交互方法

## 与 1.20.1 (Forge) 版本的关键差异

| 特性 | 1.20.1 (Forge) | 1.21.1 (NeoForge) |
|------|---------------|-------------------|
| 加载器 | Forge 47.4.2 | NeoForge 21.1.191 |
| Java | 17 | 21 |
| Mappings | official (Mojang) | parchment |
| 时钟加速逻辑 | ClockEntity 独立类 | 内嵌在 ClockBlock |
| 时钟开关状态 | 每个时钟独立 | 全局static共享 |
| 永恒图腾 | ✅ EternalTotemItem | ❌ 已移除 |
| 自定义配方 | ✅ SmeltingCraftRecipe | ❌ 已移除 |
| 数据持久化 | NBT BlockEntityTag | DataComponentType |
| Capability注册 | ICapabilityProvider | RegisterCapabilitiesEvent |
| 交互方法 | use() | useWithoutItem() + useItemOn() |
| 文件数量 | 25 Java | 22 Java |
