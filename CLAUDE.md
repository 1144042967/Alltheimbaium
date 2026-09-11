# Alltheimbaium (ATI) — Minecraft Forge 1.20.1 Mod

## 项目概述

一个添加"破坏平衡"机器的 Minecraft Forge 模组。可以独立使用，但设计目标是与 ATM (All The Mods) 整合包一起使用。添加了即时成熟耕地、无限资源制造机、生物/资源农场、零刻加工机器等功能。

- **Mod ID**: `alltheimbaium`
- **Group**: `cn.sd.jrz`
- **Minecraft 版本**: `1.20.1`
- **Forge 版本**: `47.2.20`
- **Java 版本**: `17`
- **Mappings**: `official` (Mojang)
- **许可证**: `GNU LGPL v3`
- **版本**: `1.20.1.13`

## 构建和开发

```bash
# 运行客户端（需 JDK 17）
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

`build.gradle` 声明了 `src/generated/resources/` 作为数据生成输出目录，但该目录当前**不存在**——所有 blockstate / 模型 / 战利品表 / 配方 / 语言文件均为手写，位于 `src/main/resources/` 下。改动数据文件后需自行保证 JSON 合法。

## 物品品级与 tooltip 规范

颜色 ⇄ 稀有度 ⇄ 能力度三者绑定：物品名称颜色由原版 `Item.Properties#rarity` 驱动（lang 里**不写** `§` 前缀），同一 Rarity 也决定 tooltip 首行显示的品级标签。

| 品级 | Rarity | 颜色 | 能力定位 | 物品 |
|------|--------|------|----------|------|
| 材料级 | `COMMON` | 白色 | 仅合成中间物 | `package_material_x1` |
| 便利级 | `UNCOMMON` | 黄色 | 省事、提速，本身不产生资源 | `farmland` `platform` `extraction_interface` `clock` `supply_crate` |
| 高效级 | `RARE` | 青色 | 需输入或能量，批量加工 | `auto_farmland` `instant_furnace` `instant_inscriber` `mob_farm` `resource_farm` |
| 破坏平衡 | `EPIC` | 淡紫 | 一次建立后无限产出，或绝对能力 | `storage_fountain` `liquid_fountain` `creative_transmuter` `eternal_totem` `eternal_sword` |

- `RARE` 与 `EPIC` 物品一律 `fireResistant()`。
- 合成成本随品级递增：便利级用铁锭、高效级用铁块、破坏平衡用钻石块，永恒系列用下界合金块。`auto_farmland` 走"耕地升级"路线（6 耕地 + 红石），不套用铁块环。
- **例外**：`creative_transmuter` 是破坏平衡级里唯一走**下界合金块环**的机器（它最终产出的是创造化学品储罐，成本对标永恒系列而非同级的存储/液体制造机）。

### tooltip 结构（由 `item/Tip.java` 统一生成）

```
①  §8<类型> · <品级>          ← head()，品级由 stack.getRarity() 自动推导
    (空行)
②  §7<一句话概述>              ← summary()
    (空行)
③  §7· <键>：§e<值>           ← state()，动态状态，可省略
    (空行)
④  §6■ 用法                    ← usage()，只写非显然的操作
    §7· …
    (空行)
⑤  §6■ 参数                    ← params()，有数值时才出现
    §7· <键>：§e<数值>
    (空行)
⑥  §c■ 注意                    ← warn()，有限制或风险时才出现
    §c· …
```

硬性约束：

- **不写**"右击打开 GUI""放下去即可生效"这类与原版方块一致的通用操作，也不写显而易见的说明。
- **只写**：如何开始工作、如何配置、非显然的限制、具体数值。
- 正文一律 `§7`，参数值一律 `§e`，警告一律 `§c`；段标题分隔符用 `■`。
- 中英文语言文件的段落数与顺序**逐行对齐**，键集必须完全一致。

用法：`Tip.of(tooltip).head(stack, "tip.alltheimbaium.type.xxx").summary(...).usage(...).params(...).warn(...)`。
条目过多时（如农场的 27 行产物）用 `Tip.inline(entries, "tip.alltheimbaium.more")` 压成一行，或 `Tip.join(...)` 自行控制上限。

### GUI 标题颜色

带界面的机器，其标题组件必须套上与对应物品相同的品级色：

```java
Component.translatable("block.alltheimbaium.clock").withStyle(Tip.rarityColor(Registration.CLOCK_ITEM.get()))
```

设置点是菜单标题的产生处——`BlockEntity.getDisplayName()`（实现 `MenuProvider` 的实体）、`SimpleMenuProvider` 的标题参数、或打开界面的网络包。**不要**去改各 Screen 里 `drawString` 的颜色参数：原版 `Font` 对每个字形是 `style.getColor() != null ? 用样式色 : 用传入色`，所以标题组件的样式色天然覆盖它与各处硬编码的 `0xFFAA00`/`0x404040`，一处着色、所有屏幕自动生效。同理，标题的语言值里**不能**带 `§` 颜色码，否则会盖掉品级样式（`screen.alltheimbaium.eternal_*_title` 已清理）。

**每个 Screen 都必须覆写 `renderLabels`**，不要依赖原版默认实现——原版默认虽然也画标题，但用的是写死的 `4210752` 且 `dropShadow = false`，会让该界面成为唯一不带阴影的一个：

```java
@Override
protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, true);
    guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    // …该界面自己的面板文字
}
```

标题必须 `true`：品级色（黄/青/淡紫）在浅色背景上不带阴影会糊。传进去的颜色参数已被标题组件的样式色覆盖，只是样式缺失时的兜底。`playerInventoryTitle` 不属于标题，维持 `false`。

### 大数存储的文案约定

**面向玩家的文本（tooltip / README / 界面文字）里不出现 "AE"**：

- 机器的大数存量（自动耕地 / 生物农场 / 资源农场 / 零刻熔炉 / 零刻压印器）统一表述为"大数"，不写"AE 大数"。
- 存量的**缩写显示方式**（1.1K / 2.1M、0.5 倍小字）参考 AE2 的实现——这属于实现细节，只在本文档记录，不进 tooltip 与 README。代码注释同理只写"缩写"。
- **例外**：AE2 作为**真实模组依赖**时必须点名。零刻压印器的配方来源就是 `ae2:inscriber`、未装 AE2 便不产出，tooltip 与 README 必须写明；"建议用 AE2 的存储面板管理"这类联动建议同理。去掉这些会让玩家不明白机器为什么不能用。

### 六面输出按钮的 hover tooltip

所有带六面输出按钮的界面（自动耕地 / 生物农场 / 资源农场 / 存储方块制造机 / 零刻熔炉 / 零刻压印器 / 液体无限制造机）一律走 `gui/FaceTooltip.java`，不要各自拼装：

```
§f<要输出的内容>              ① 无标签，有可选项的机器始终显示
§7输出方向：§e<方向>            ②
§7输出目标：§e<指向的方块>       ③ 无目标时显示 §8无
```

- 第一行内容取自当前面状态，共用键 `screen.alltheimbaium.output.*`：
  - 有槽位可选的机器（三个农场 + 存储机）：物品名 / 空槽显示 `.slot` 的"槽 N" / `.random` 随机 / `.disabled` 禁用
  - 只有开/关两态的机器（零刻熔炉 / 零刻压印器 / 液体无限制造机）：`.enabled` 启用 / `.disabled` 禁用
- `build(...)` 的 `outputContent` 传 `null` 时整行省略。该分支目前**没有调用方**（所有界面都始终显示内容行），保留是为了后续可能出现的"无可选输出"机器。
- 液体机的面状态要**直接读 `menu.isFaceEnabled(dir)`**，不要用 `FaceButton.state` 缓存字段——那个字段在 `render()` 里晚于 tooltip 渲染才刷新，用它会慢一帧。

### 六面按钮的图标

相邻位置有方块时，按钮直接铺该方块的物品贴图，不再画方向箭头：

- 零刻熔炉 / 零刻压印器的按钮是 16×16，与物品贴图等大，`renderItem(icon, getX(), getY())` 铺满即可。
- 液体机的按钮是 38×13，需按比例缩放到 13px 再居中（`pose().scale(...)`）。
- 无相邻方块时回落到原有的方向箭头 / 方向名文字。
- 三个农场与存储机的按钮是「[相邻贴图] ← [槽位贴图/槽号]」两段式，不走上面这套，见各自的 `FaceButton.renderWidget`。
- 标签 `§7` 暗、内容 `§e` 亮，内容行 `§f` 更亮，是这一组提示的固定读法。
- 方向名键三个农场共用 `screen.alltheimbaium.mob_farm.face.*`（**没有** `auto_farmland.face.*` 与 `resource_farm.face.*`）。
- 拼装一律字符串拼接，不用 `Component.append`——`§` 的格式状态不跨兄弟组件传递，前缀放进独立组件会被丢弃（同 `item/Tip.java`）。

### 六面按钮的点击

- **左键**正向循环（原有点击逻辑不变），**右键**反向循环。
- 实现：`FaceButton` 覆写 `mouseClicked`，`button == 1` 时发 `BUTTON_DIR_REVERSE_BASE + direction.ordinal()`，否则交回 `super`（走原有 `onPress`）。菜单侧把该区间转成 `entity.cycleDirectionState(dir, false)`。
- `BUTTON_DIR_REVERSE_BASE` 取值为紧邻各菜单 `BUTTON_OUTPUT` 之后：三个农场菜单 **88**、存储机菜单 **34**，各占 6 个 id。
- 实体侧 `cycleDirectionState(Direction, boolean forward)` 反向用 `(state + count - 1) % count`，`count == 1` 时仍安全。
- 该功能目前**不在 tooltip 里提示**（tooltip 行数由规范固定为三行），需要靠玩家自行发现；若要加提示行须先放宽上面的规范。

类型键 `tip.alltheimbaium.type.*`：material / farmland / building / accelerator / agriculture / processing / resource / logistics / supply / combat / survival。

## 物品创建规则

新增物品时逐项对照本节。上方「物品品级与 tooltip 规范」管**文案与配色**，本节管**要产出哪些文件、每个文件长什么样**——后者是硬约束，漏一个文件游戏里就是紫黑块或缺图。

### 1. 文件清单

带 GUI 的机器（完整链路）：

| # | 文件 | 要点 |
|---|------|------|
| 1 | `block/XxxBlock.java` | `extends Block implements EntityBlock`，含 `newBlockEntity` 与 `getTicker` |
| 2 | `entity/XxxEntity.java` | `extends BlockEntity implements ICapabilityProvider, MenuProvider` |
| 3 | `connection/XxxConnection.java` | 仅当对外暴露 `IItemHandler` / `IFluidHandler` 时 |
| 4 | `item/XxxItem.java` | `extends BlockItem` |
| 5 | `gui/XxxMenu.java` + `gui/XxxScreen.java` | 有界面时 |
| 6 | `gui/XxxRenderer.java` | 仅当要在方块表面渲染文字或标记物（BER）时 |
| 7 | `setup/Registration.java` | BLOCKS / ITEMS / ENTITIES / MENUS 各加一条，并补进 `displayItems` |
| 8 | `setup/Config.java` | 有可调数值时；同时接进 `Config.onConfigLoad()` 同步到方块类的静态缓存 |

纯物品（永恒之剑 / 永恒图腾 / 打包材料）只有 4、7、8 与资源文件，没有 1~3、5、6。

### 2. 资源清单

全部落在 `src/main/resources/`，键名用同一个 `<name>`：

```
assets/alltheimbaium/textures/block/<name>_top.png    16×16  顶面
assets/alltheimbaium/textures/block/<name>_side.png   16×16  侧面
assets/alltheimbaium/textures/block/<name>_btm.png    16×16  底面（后缀是 btm，不是 bottom）
assets/alltheimbaium/textures/gui/<name>_gui.png      176×N  GUI 背景
assets/alltheimbaium/models/block/<name>.json         方块模型
assets/alltheimbaium/models/item/<name>.json          物品模型（继承方块模型）
assets/alltheimbaium/blockstates/<name>.json          variants 通常只有 "" 一个分支
data/alltheimbaium/loot_tables/blocks/<name>.json
data/alltheimbaium/recipes/main/<name>.json
assets/alltheimbaium/lang/zh_cn.json + en_us.json
```

- 机器模型统一 `minecraft:block/cube_bottom_top` + `top` / `bottom` / `side` / `particle` 四个槽；单面贴图的方块（platform）用 `cube_all`；耕地类用 `minecraft:block/block` 手写 15/16 高的 `elements`（见 `models/block/farmland.json`），两者的 elements 结构完全一致，复制即可。
- 物品模型的父级只有三种：方块物品继承 `alltheimbaium:block/<name>`（**不需要** item 贴图）；手持/武器用 `minecraft:item/handheld` + `layer0`；纯图标物品用 `minecraft:item/generated` + `layer0`。
- 材料级方块的贴图**直接复用物品贴图**（`cube_all` 指向 `alltheimbaium:item/package_material`），不另画一份 block 贴图。
- 有方块状态的方块才在 `variants` 里多列分支（只有 `platform` 的 `disguised`），其余一律单分支。
- 别照抄的遗留文件：`textures/block/storage_fountain.png` 没有任何模型或代码引用（该方块用的是 `_top/_side/_btm` 三张），多画这样一张纯属浪费。
- **贴图与 GUI 底图一律写成 `tools/gen_<name>_assets.py` 生成器**（已有 `gen_instant_furnace_assets.py` / `gen_instant_inscriber_assets.py` / `gen_creative_transmuter_assets.py`），不要只提交 PNG：生成器可重复执行、便于微调，也是贴图与 Java 侧坐标耦合关系的唯一记录。脚本开头的 docstring 要写明该机器 `Menu`/`Screen` 的槽位坐标与 `imageWidth/imageHeight`，**改 Java 坐标必须同步改脚本并重跑**。
- 画 GUI 底图不要自己估摸边框，直接抄 `gen_creative_transmuter_assets.py` 里的 `_panel()` / `_slot_box()`：边框是 1px 黑描边（圆角处透明）+ 左上 2px 白高光 + 右下 2px 灰阴影，槽位是 18×18 三面倒角。该脚本带 `--verify` 开关，会用本模组既有的干净容器界面回放校验边框规则，正常输出 `边框差异像素数 = 0`；改过规则就跑一次。

### 3. 贴图风格

- 贴图一律 **16×16 手绘像素画**。
- **机器类**共用一套视觉语言：「**深蓝灰金属机身 + 亮蓝描边 + 四角亮蓝角标**」，四角角标是机器贴图的统一识别标记，新机器必须沿用。三面分工固定：顶面/侧面承担功能标识（开口、格栅、图案），底面是统一的深色底座；功能色按用途走——熔炉橙、压印器黄、生物与资源农场蓝绿、存储青白（深蓝底 + 青白格）。玻璃罐体类（存储 / 液体 / 生物 / 资源农场）用深色半透明罐体 + 玻璃外壳，并在客户端注册 `RenderType.cutout()`。
- **不套这套的**：耕地 / 自动耕地走草色与土色（`farmland_*` / `farmland_auto_*`），打包材料走木箱色，纯物品（剑 / 图腾）按各自的物品造型画。
- 物品贴图同规格同画风，放 `textures/item/`。
- GUI 贴图是**标准原版容器外观**：浅灰（`0xC6C6C6`）底 + 圆角斜面边框 + 斜面槽位方框。机器在顶部加一块**黑色信息面板**放进度条与数值；物品 GUI（剑 / 图腾 / 补给箱）不加。

### 4. 物品类骨架

```java
public class XxxItem extends BlockItem {

    public XxxItem(Block block) {
        // 品级见上方对照表；RARE 与 EPIC 一律再 .fireResistant()
        super(block, new Properties().rarity(Rarity.RARE).fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level,
                                @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);   // 硬性：必须先调 super
        Tip.of(tooltip)
                .head(stack, "tip.alltheimbaium.type.xxx")     // 品级行，类型键见上
                .summary("item.alltheimbaium.xxx.summary")
                .usage("item.alltheimbaium.xxx.usage.1")
                .params("item.alltheimbaium.xxx.param.1")      // 有数值才写
                .warn("item.alltheimbaium.xxx.warn.1");        // 有限制才写
    }
}
```

- 读 NBT 取动态状态（存量、标记物等）的 `appendHoverText` 要 `try-catch` 兜底，状态行走 `state()`。
- 材料级物品没有可配置项，**只留 `head` + `summary`**，不写用法（配方由 JEI 呈现）。

### 5. 注册与创造标签

- `Registration.java` 里**声明顺序 = 注册顺序 = 创造标签顺序**，三者必须一致：按品级升序（材料 → 便利 → 高效 → 破坏平衡），同级内按用途排。加物品时同时改 `displayItems` 与下方声明区两处。
- 方块与物品同名注册（`<name>`），方块用 `BLOCKS.register`、物品用 `ITEMS.register`。
- 菜单一律 `IForgeMenuType.create`：方块 GUI 在工厂里读 `data.readBlockPos()`，物品 GUI（剑 / 图腾）不读。
- 客户端还要在 `gui/ClientHandler.java` 补三处：`MenuScreens.register`、`EntityRenderersEvent.RegisterRenderers`（有 BER 时）、`ItemBlockRenderTypes.setRenderLayer`（罐体类 cutout）。

### 6. GUI 规范

- **`imageHeight` 必须等于 GUI 贴图高度**，`imageWidth` 固定 176（`platform` 是唯一例外，用 152）。绘制用 8 参 `blit`，纹理尺寸传 `imageWidth` / `imageHeight` 本身，不做 UV 缩放：

  ```java
  guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
  ```
- 玩家背包采用标准四行布局时，`inventoryLabelY = this.imageHeight - 94`；布局特殊（剑 / 图腾）才手填。
- **每个 Screen 都必须覆写 `renderLabels`**，标题传 `true`、`playerInventoryTitle` 传 `false`。标题颜色由 `Tip.rarityColor(...)` 给的样式色决定，`drawString` 的颜色参数只是样式缺失时的兜底（详见上方「GUI 标题颜色」）。
- 状态同步走 `DataSlot`：`long` 必须拆成高/低两个 int（`ClientboundContainerSetDataPacket` 用 `writeShort`，单槽只有 16 位，否则 ≥32768 的值会显示成 4.29M）。
- 交互走 `clickMenuButton` + 按钮 ID 常量，常量集中声明在 Menu 顶部；右键反向循环的区间取 `BUTTON_DIR_REVERSE_BASE`，值为紧邻各菜单 `BUTTON_OUTPUT` 之后。
- 六面输出按钮一律走 `gui/FaceTooltip.java`，不要各自拼装；图标与点击行为见上方三条「六面按钮」小节。
- **凡是 JEI 看不到的参考数据，都要在标题栏右侧放一个 `?` 帮助卡**（抄 `InstantInscriberScreen` / `CreativeTransmuterScreen`）：`init()` 里把位置右对齐到内边距 8，`renderLabels` 画黄色 `?`，hover 自绘半透明卡片（`HELP_Z = 400` 盖住槽位物品），`mouseClicked` 点击翻页，空态给一句"未安装 X"。写死的配方表、由配置推导的接受范围都属于这一类。
- 语言键：`screen.alltheimbaium.<name>.*`，六面提示共用 `screen.alltheimbaium.output.*`，只有物品 GUI 才有 `screen.alltheimbaium.<name>.title`。

### 7. 数据文件

- 配方统一放 `recipes/main/<name>.json`，`group` 固定 `alltheimbaium`，成本随品级递增（便利级铁锭 / 高效级铁块 / 破坏平衡钻石块 / 永恒系列下界合金块）。
- **有持久数据的机器**：战利品表要 `copy_name` + `copy_nbt`，把 `saveAdditional` 写下的每个键逐个搬到 `BlockEntityTag.<键>`，拆下重放才不丢数据。**无持久数据的机器**（platform / supply_crate / extraction_interface）用普通战利品表即可。
- 语言文件 `zh_cn.json` 与 `en_us.json` **键集必须完全一致、段落顺序逐行对齐**，改完用脚本比对一次（键数相等且差集为空）。

## 项目架构

```
src/main/java/cn/sd/jrz/alltheimbaium/
├── Alltheimbaium.java          # 主 mod 类 (@Mod)
├── block/                      # 方块类
│   ├── FarmlandBlock.java              # ATI 耕地
│   ├── AutoFarmlandBlock.java          # ATI 自动耕地
│   ├── ClockBlock.java                 # 加速时钟
│   ├── PlatformBlock.java              # 生成平台
│   ├── SupplyCrateBlock.java           # 补给箱
│   ├── StorageFountainBlock.java       # 存储方块制造机
│   ├── LiquidFountainBlock.java        # 液体无限制造机
│   ├── InstantFurnaceBlock.java        # 零刻熔炉
│   ├── InstantInscriberBlock.java      # 零刻压印器
│   ├── ExtractionInterfaceBlock.java   # 取出接口
│   ├── CreativeTransmuterBlock.java    # 创造物品质变器
│   ├── MobFarmBlock.java               # 生物农场
│   └── ResourceFarmBlock.java          # 资源农场
├── entity/                     # BlockEntity 类（与上方方块一一对应）
│   ├── CommonEntity.java               # 耕地占位实体
│   ├── AutoFarmlandEntity.java / ClockEntity.java
│   ├── StorageFountainEntity.java / LiquidFountainEntity.java
│   ├── InstantFurnaceEntity.java / InstantInscriberEntity.java
│   ├── ExtractionInterfaceEntity.java
│   ├── CreativeTransmuterEntity.java
│   └── MobFarmEntity.java / ResourceFarmEntity.java
├── item/                       # 物品类
│   ├── Tip.java                        # tooltip 规范构建器（所有物品共用）
│   ├── PackageMaterialItem.java        # 打包材料
│   ├── FarmlandItem.java / AutoFarmlandItem.java / ClockItem.java
│   ├── PlatformItem.java / SupplyCrateItem.java / ExtractionInterfaceItem.java
│   ├── StorageFountainItem.java / LiquidFountainItem.java
│   ├── CreativeTransmuterItem.java
│   ├── InstantFurnaceItem.java / InstantInscriberItem.java
│   ├── MobFarmItem.java / ResourceFarmItem.java
│   ├── EternalTotemItem.java / EternalSwordItem.java
│   └── TotemEventHandler.java / EternalSwordEventHandler.java
├── connection/                 # Forge Capability 实现（每台机器一个）
│   ├── AutoFarmlandConnection.java / MobFarmConnection.java / ResourceFarmConnection.java
│   ├── StorageFountainConnection.java / LiquidFountainConnection.java
│   ├── InstantFurnaceConnection.java / InstantInscriberConnection.java
│   ├── CreativeTransmuterConnection.java    # 输入栏可插不可抽、输出栏可抽不可插
│   └── ExtractionInterfaceConnection.java   # 聚合连通范围内的机器，只读
├── gui/                        # Menu / Screen / Renderer / 客户端事件
│   ├── <机器>Menu.java / <机器>Screen.java
│   ├── <机器>Renderer.java     # BER 方块表面渲染
│   ├── RightClickHandler.java           # ALT+右键打开剑/图腾配置界面
│   └── ClientHandler.java
├── recipe/                     # 自定义配方
│   ├── SmeltingCraftRecipe.java         # 熔炼合成配方 (煤炭+可烧炼物品)
│   ├── BrewingCraftRecipe.java          # 酿造合成配方 (药水+酿造材料)
│   └── PotionCombineRecipe.java         # 药水融合配方 (两药水合成/牛奶净化)
└── setup/                      # 注册和配置
    ├── Registration.java                # 所有方块/物品/实体/菜单的注册
    ├── Config.java                      # ForgeConfigSpec 服务端配置
    ├── Tool.java                        # 工具方法 (NBT, 数量裁剪, DE 反射)
    ├── TransmuteCatalog.java            # 创造物品质变器的固定配方表（注册名解析，可选联动）
    ├── MobFarmCatalog.java / MobFarmDefaultData.java / MobFarmInteraction.java
    ├── MobFarmMarkerIndex.java / MobFarmWhitelist.java / KillLootEstimator.java
    ├── ResourceData.java / ResourceDefaultData.java
    ├── SupplyData.java / SupplyEvents.java / SupplyRoll.java
    ├── PlatformEvents.java / CuriosHelper.java
```

## 注册体系

`Registration.java` 是中心注册文件，使用 Forge 的 `DeferredRegister` 模式：

- 6 个 `DeferredRegister`: BLOCKS, ITEMS, ENTITIES, RECIPE_SERIALIZERS, MENUS, CREATIVE_MODE_TABS
- 在 `init(FMLJavaModLoadingContext)` 中注册所有内容
- **声明顺序即物品注册顺序，也是创造标签页顺序**，两者保持一致：按品级升序（材料 → 便利 → 高效 → 破坏平衡），同级内按用途排列。新增物品时需同时在 `displayItems` 与下方的声明区里放到对应品级的位置。
- 公有的方块属性 (`BLOCK_PROPERTIES`): 蓝色、活塞推动时销毁、硬度/抗性 0.5；玻璃罐体类方块（存储/液体/生物/资源）改用 `Properties.copy(Blocks.GLASS).noOcclusion()`

## 功能模块

### 1. ATI 耕地 (`FarmlandBlock` / `CommonEntity`)

继承 `net.minecraft.world.level.block.FarmBlock`，实现 `EntityBlock`。

- 每 tick 检查上方方块：若为 `BonemealableBlock` 则催熟；若为 `CropBlock` 则设为最大生长阶段
- `canSurvive()` 始终返回 `true`，`isFertile()` 始终为真，不会退化
- 只在 `PlantType.CROP` 和 `StemBlock` 类型的作物上方允许种植
- 禁止树苗等非作物种植；有湿润动画但无实际效果
- `getDrops()` 覆写为直接返回自身（因此**没有** `loot_tables/blocks/farmland.json` 也能正常掉落；该文件现已补齐用于数据完整性）

### 2. ATI 自动耕地 (`AutoFarmlandBlock` / `AutoFarmlandEntity`)

每 tick 反复收获上方作物的机器，**不消耗能量**。

- 上方为 `CropBlock` 时强制设为最大年龄并立即 `getDrops()` 收取一次；作物方块不被移除、年龄不回退，因此每 tick 可无限收获同一株
- 只处理 `CropBlock`；下界疣与南瓜/西瓜的 `StemBlock` 不会被收割
- 收获效率 `eff = 1 + (level - 1) × 0.01`，乘在每种掉落的数量上，小数部分用 `frac` 累积
- 产物存在内存 `List<Row>`（AE 大数 `long` 存量 + 小数进位），行数上限取 `mob_farm.max_products`（默认 27）
- 等级复用 `mob_farm.*` 配置：初始 1，每 20 秒 +1，默认无上限
- 六面状态循环 `0=随机 / 1=禁用 / 2+N=槽N`；有主动输出总开关 `outputEnabled`；对外 `IItemHandler` **只读**
- GUI 打开方式：顶面右键返回 `PASS`（留给种植），侧面/底面右键打开

### 3. ATI 加速时钟 (`ClockBlock` / `ClockEntity`)

加速相邻方块 tick 的方块，类似加速火把。

- 使用顶/边/底三面贴图（`clock_top`/`clock_side`/`clock_btm`）
- 右击打开配置 GUI（`ClockMenu`/`ClockScreen`，纯配置界面，无物品栏）：
  - **全局开关**：所有时钟共享（`private static`），初始值来自配置 `clock.default_active`（默认开），每次配置加载时重置，GUI 切换不写 NBT
  - **单独开关** `enabled`：当前时钟总开关（NBT 持久化）
  - **六方向开关** `directionEnabled[6]`：逐面控制，按钮显示该方向实际相邻方块的物品图标（NBT 持久化）
  - **速度调节** `speed`：`SPEEDS = {2,4,8,16,32,64,128,256,512,1024}` 共 10 档（NBT 持久化）
- `ClockEntity.tick()`: 全局/单独开关均开启时，遍历 6 个面：
  - 先判 `isExcluded(block)` 决定是否整面跳过（在 randomTick 之前）
  - 对 `block.isRandomlyTicking(state)` 的方块**额外调用 1 次** `randomTick()`（与倍速无关）
  - 仅当方块实现 `EntityBlock` 时，额外调用 ticker `(speed - 1)` 次（加上原版自身的 1 次 = 共 speed 次）
- **不参与加速的方块**（`ClockEntity.isExcluded`）：`ClockBlock`、`FarmlandBlock`、`ExtractionInterfaceBlock`、`SupplyCrateBlock`、`InstantFurnaceBlock`、`InstantInscriberBlock`、`CreativeTransmuterBlock`。
  - 时钟与耕地是为了避免互相叠加；取出接口与补给箱的 tick 本身不产生进度；熔炉与压印器每 tick 已按当前电量整批结算完，质变器九格凑齐即转化、转完输入栏必空，重复调用只有性能开销没有额外产出。
  - 新增方块时若它"每 tick 结算一次就够了"，应加进这个列表。
- `ClockRenderer`（BER）：在方块四周侧面居中渲染当前倍速数值（`Font.drawInBatch`）
- 战利品表含 `copy_nbt`，拆下时保留 `enabled` / `directionEnabled` / `speed`

### 4. ATI 存储方块制造机 (`StorageFountainBlock` / `StorageFountainEntity`)

记录并无限复制物品（类似创造抽屉），右键打开 GUI（`StorageFountainMenu`/`StorageFountainScreen`）。

- **标记槽**（黑色信息面板右下角，容量恒为 1）：放入支持的物品 → 标记该物品类型并复制；放入已标记物品 → 取消标记并清空存量
- 支持判定见 `StorageFountainBlock.isAcceptedItem()`，三条依据命中其一即可，**白名单优先**：
  1. 物品白名单 `storage_fountain.accepted_items`——完整注册 ID（如 `minecraft:diamond`）**精确匹配**；
  2. 物品命名空间**子串**匹配 `accepted_mods`；
  3. 任一标签 path **子串**匹配 `accepted_tags`（默认含 `storage_blocks`/`ores`/`ingots`/`dusts`/`gems` 等 12 项）。
  配置值由 `StorageFountainBlock` 静态缓存，并暴露 `getAcceptedItems()/getAcceptedMods()/getAcceptedTags()` 供 GUI 与物品 tooltip 取用（`StorageFountainItem` 不再自行缓存一份）
- **标题栏右侧 `?` 帮助卡**（参考 `MobFarmScreen`）：hover 逐行列出当前生效的白名单 / MOD / 标签，值过长时用 `Font.split` 按像素宽换行、续行缩进到值列。纯客户端，数据直接读上面的静态 getter
- 最多 `max_item_types`（默认 9）种物品，对应 GUI 中 9 个已标记物品槽
- 每 `growth_interval_seconds`（默认 20 秒）`output += growth_step`（默认 +5）；`output` 初值 5，使用 `carry = 1000` 作为内部计数进位阈值，折合 `output/50` 件/秒。**代码中没有增长上限**
- GUI 黑色信息面板四行：增长进度条 / 增长百分比 / 下次增长数值 / 产量
- 9 个已标记物品槽支持单击提取 1 个、Shift 提取 1 组、空格+单击提取到背包满；槽位左下角以 AE2 风格缩写显示存量
- 六面输出按钮可逐面循环切换 11 个状态：随机 / 禁用 / 槽1~槽9；该状态**同时约束管道被动抽取**与主动输出；"输出"按钮控制整体主动输出开关 `outputEnabled`
- 提供 `IItemHandler` capability（只读，不可插入）
- **方块表面渲染**（`StorageFountainRenderer` BER）：在方块四个侧面按九宫格（3×3，左上开始）绘制已标记物品贴图

### 5. ATI 液体无限制造机 (`LiquidFountainBlock` / `LiquidFountainEntity`)

将有限流体变为无限。内部液体由 `LiquidFountainRenderer`（BER）按存量比例渲染。

- 输入单一流体，达到阈值（配置 `liquid_fountain.infinite_threshold`，默认 `10,000,000` mB）后变为无限；`amount == 阈值` 即算无限
- 只支持 1 种流体；配置 `auto_infinite_mods` 中命名空间的流体输入后下一 tick 直接 `amount = Integer.MAX_VALUE`
- **右键打开 GUI**（化学品储罐风格，`LiquidFountainMenu`/`LiquidFountainScreen`）：
  - 左上 `+ 槽`：放液体桶/带液容器 → 液体输入机器；放空桶/空容器 → 从机器装液体；操作完毕的桶/容器移到下方 `- 槽`
  - `- 槽` 只读，`getSlotLimit = 1`
  - 中间背景块上六个按键逐面控制主动输出开关（NBT 持久化）
  - 右上进度条展示阈值进度，下方信息面板显示存量/阈值与"还需 X mB 达到无限"
- 无限状态下每 tick 向六个面（受逐面开关 + 总开关控制）`fill(Integer.MAX_VALUE)`；`drain` 不再扣内部存量
- 手持空桶/带液容器右键：先尝试装出、再尝试倒入，两者都失败才打开 GUI
- **未达阈值时主动输出不执行**，但管道仍可 `drain` 存量
- 提供 `IFluidHandler` 与 `IItemHandler`（+ 槽可插入、- 槽可抽取）capability
- 破坏时 + 槽 / - 槽中的物品掉落

### 6. ATI 生物农场 (`MobFarmBlock` / `MobFarmEntity`)

收容制生物农场，右键打开 GUI（`MobFarmMenu`/`MobFarmScreen`）。

- **手持收容**（`MobFarmItem`）：未收容时右键，在玩家外扩 `mob_farm.capture_radius`（默认 5）格内找**最近**的有效 `LivingEntity`（白名单 `MobFarmCatalog.isWhitelisted()` 或 `KillLootEstimator.hasAnyDrop()` 有掉落），捕捉后实体 `discard()`
- 未收容时右击方块面：先尝试捕捉，**捕捉失败才当方块放下**
- **放置后收容**：GUI 的标记槽放入刷怪蛋 / 白名单特征物 / 动态掉落物即可收容
- **每台只能收容一次**：已收容后 `processSpecialSlotMarker` 直接返回，不可取消/更换
- 等级：初始 1，每 `level_up_interval_seconds`（默认 20 秒）+1，默认无上限
- 产物权重来源（`buildDropTable()`）：① 白名单 `mob_farm.product_whitelist` 优先；② `KillLootEstimator` 击杀掉落采样（`sample_kills` 默认 2000 次）补白名单没有的物品种，权重 `clamp(round(avgPerKill × 500), 1, 10000)`；③ 刷怪蛋兜底权重 1
- 产出：每行 `acc += weight × level`，`acc >= carry(默认 10000)` 进位成整件 → **速率 = weight × level / 500 件/秒**
- **使用槽**：收容后同一槽变为使用槽，放入剪刀/桶/碗/玻璃瓶（见 `MobFarmInteraction`）可按该生物刷怪蛋行的权重同速累积产出，**工具不消耗耐久**
- 六面状态 `0=随机 / 1=禁用 / 2+N=槽N` + 总开关 `outputEnabled`；对外 `IItemHandler` 只读

### 7. ATI 资源农场 (`ResourceFarmBlock` / `ResourceFarmEntity`)

标记制资源农场，右键打开 GUI（`ResourceFarmMenu`/`ResourceFarmScreen`）。

- 标记槽放入一个标记物（`ResourceData.isMarker()` 命中，来自配置 `resource_farm.whitelist`）即永久确定该资源并重建产物表；标记物常驻槽内展示
- **不能取消、不能更换**：`hasMarker()` 后 `isItemValid` 返回 false 且槽位不可取出
- 等级与产出机制同生物农场（`weight × level / 500` 件/秒，共用 `mob_farm.*` 配置）
- 六面状态 + 总开关同上；对外 `IItemHandler` 只读
- GUI 右上角 `?` 可翻页查看「标记物 → 产物」对照表

### 8. 零刻熔炉 (`InstantFurnaceBlock` / `InstantFurnaceEntity`)

- 输入区 **18 种**物品行、输出区 **18 种**，均为 AE 大数（`long`，无 64 上限）
- 每件耗能 `ENERGY_PER_SMELT = 1000 FE`，能量上限 `MAX_ENERGY = 200,000,000 FE`
- 配方按 `SMELTING → BLASTING → SMOKING` 三级兜底查表，结果缓存（不可烧的以 EMPTY 哨兵缓存）
- 无耗时：每 tick 按当前电量整批结算；输入/输出种类满时不部分消耗
- 投料：Shift+左键背包物品 / `BUTTON_DEPOSIT_INPUT` / 管道插入槽 0~17
- 取出：每种行支持 左键=1 / Shift=1组 / 空格=取满；管道只能从输出槽 18~35 抽取
- `BUTTON_SWAP` 交换输入/输出内容（仅交换，不触发熔炼）
- 六面输出只有"推送/禁用"两态（`STATE_PUSH`），逐面切换，**没有主动输出总开关**

### 9. 零刻压印器 (`InstantInscriberBlock` / `InstantInscriberEntity`)

读 AE2 `ae2:inscriber`（兼容旧 id `appliedenergistics2:inscriber`）配方；**未装 AE2 则配方缓存为空、不产出**。

- **压板模式 `MODE_INSCRIBE`（默认）**：取 `processType=INSCRIBE` 配方，只认 `getIngredients().get(1)`（middle）作为被消耗的原料；**上/下模板既不要求也不消耗**。每消耗 1 份原料，产出该原料命中的全部压板配方各一份
- **组装模式 `MODE_ASSEMBLY`**：取 `processType=PRESS` 配方，消耗 top/middle/bottom 中全部非空材料；**3 材料配方优先于 2 材料**，外层循环直到无配方可执行（guard 1024）
- 每件产物 1000 FE，上限 200,000,000 FE；输入 **18 种**、输出 **9 种**
- 交互与零刻熔炉同构：`BUTTON_MODE` 切模式、`BUTTON_DEPOSIT_INPUT` 投料、三档取出、六面推送/禁用两态
- **标题栏右侧两个 "?" 帮助卡**（参考 `MobFarmScreen`）：左=压板配方、右=组装配方，一行一条 `输出物品 ← 输入物品…`；点击可翻页，纯客户端逻辑。
  - 配方数据来自 `InstantInscriberEntity.inscribeSummaries(level)` / `assemblySummaries(level)`——**客户端可用**，因为 `ClientLevel.getRecipeManager()` 返回登录时同步下来的配方管理器（与 JEI、配方书同源）。未装 AE2 时返回空列表，卡片显示"未安装 AE2"。
  - 摘要把每个 `Ingredient` 折成其第一个候选物品（AE2 压印配方的材料实际都是单一物品），并按产物注册名排序，保证分页顺序稳定。

### 10. 取出接口 (`ExtractionInterfaceBlock` / `ExtractionInterfaceEntity`)

**聚合范围不是"相邻六面"，而是从本方块出发沿本模组方块与联动模组 AutoResource 的方块做的连通搜索。** 分类规则：

| 角色 | 方块 | 行为 |
|------|------|------|
| 导体 | 任何 `alltheimbaium` 或 `autoresource` 命名空间的方块 | 搜索可以穿过它继续往外找 |
| 产出源 | StorageFountain / MobFarm / ResourceFarm / AutoFarmland / LiquidFountain，以及 AutoResource 除水车马达外的全部机器 | 被纳入聚合，可被抽走 |
| 只传导不产出 | Farmland / InstantFurnace / InstantInscriber / Clock / Platform / SupplyCrate / **取出接口自身** / **AutoResource 水车马达** | 网络能穿过，但其中的物品不被抽走 |

- AutoResource 是**可选依赖**，判定只比对注册名（`ExtractionInterfaceEntity.isLinkedSource` 读 `BlockEntityType` 的注册名），**不能**写成 `instanceof`——那需要编译期依赖，本模组对 AutoResource 只做可选联动。方块侧的导体判定同理，读 `Block` 的注册名命名空间
- 判定"能抽出什么"的是机器自己暴露的 Capability：AutoResource 的方块生成机 / 流体生成机（物品 + 流体，液体机只从输出槽抽出）、FE 发电机（**只暴露能量**，因此它虽在产出源名单里，却抽不出任何物品与流体）
- 同一套判定被 `ExtractionInterfaceConnection.isItemSource` / `isFluidSource` 复用；产出源名单（`isSource`）只是 BFS 的粗筛，具体槽位仍由 Capability 解析结果决定

- 生成平台铺出的平滑石/石砖地面是**原版方块**，因此会自然断开连通——不需要特判
- 搜索只走已加载区块；不加载新区块
- `ExtractionInterfaceEntity.rescan()` 做 BFS：`visited` 集合记录所有到达过的位置（无论是否导体），`MAX_SCAN_BLOCKS = 4096` 为单次上限，超限时截断并**仅在状态翻转时**告警一次
- 搜索结果缓存在 `sources`（只存 `BlockPos`，不持有 handler 引用），每 `RESCAN_INTERVAL = 20` 刻重算；首次能力查询会触发一次同步搜索，`load()` 会把 `scanned` 置回 false
- **取出接口本身不被查询能力**（BFS 只把它当导体），因此两个接口相邻也不会递归
- `ExtractionInterfaceConnection` 的槽位列表**按游戏刻缓存**（`getGameTime()` 变化才重建）：管道一次取物会连续调用 `getSlots`/`getStackInSlot`/`extractItem`，逐次重扫会带来数量级的多余开销
- 能力：只读 `IItemHandler`（`insertItem` 原样退回、`isItemValid` false）+ 只读 `IFluidHandler`（`fill` 返回 0、可 `drain`）
- **方向无关**：任意面查询返回同一聚合实例
- 无 GUI、无主动输出；`getTicker` 返回服务端 ticker 用于周期性重算

### 11. 生成平台 (`PlatformBlock` / `PlatformMenu`)

- 交互：**空手右键打开 GUI；空手 Shift+右键整片生成**；手持物品时返回 `PASS`
- 生成范围：以所点方块所在区块为中心的 **3×3 区块**，Y 取所点方块的 Y
- 单格图案：四角 = 生成平台方块、四边 = 石砖、内部 = 平滑石头、内部 10×10 区域的 4 个角 = 荧光蛙明灯
- **只替换**空气 / 平滑石头 / 石砖 / 荧光蛙明灯 / 平台方块自身
- GUI 两件事：① 全局伪装开关（切换后回写配置文件 `platform.disguise_enabled`，所有世界共用）② 3×3 九宫格按键逐区块生成/重建
- 伪装仅切换方块状态 `DISGUISED`，使模型上表面贴图变石砖，不改变实际方块与功能
- 未加载区块在加载时按区块四个角格纵向扫描纠正伪装（`PlatformEvents`，每 tick 最多 256 个）

### 12. 补给箱 (`SupplyCrateBlock` / `SupplyRoll` / `SupplyEvents` / `SupplyData`)

- **补给点获得**：游玩时间累计每 **1800 秒** +1（存玩家 persistent data，跨会话累计）；每获得一个成就 **+5**（排除 `recipes/` 开头与 `/root` 结尾的成就）
- **消耗**：兑换选中物品 **3 点**，刷新 **1 点**；剩余 = max − used
- 随机种子 = `世界种子 | 世界游戏小时 | 已用补给点`（`hours = gameTime / 72000`），因此 GUI 保持打开时每到新的整点自动免费重掷
- 每次生成 **10 个分类各 1 件**；兑换给 1 件选中物品
- 黑名单配置 `supply_crate.blacklist`（精确匹配物品注册 ID）
- 方块本身不存数据，数据全在玩家身上

### 13. 永恒图腾 (`EternalTotemItem` / `TotemEventHandler`)

- `@Mod.EventBusSubscriber` 注册事件，监听 `LivingDeathEvent`，优先级 `HIGHEST`
- `findTotem()` 按 主手 → 副手 → 盔甲 → 背包 → Curios 饰品槽 的顺序查找，任意位置即可生效
- 触发：取消死亡 → `removeAllEffects()` → `setHealth(1)` → 给予 40 秒抗火 / 45 秒生命恢复 II / 5 秒伤害吸收 II → 广播实体事件 35
- 之后逐个应用 27 格槽位（NBT 键 `potion_items`）中药水的效果，再挨个"食用"槽位里的**食物**
  - 槽位名为药水 / 食物槽：`EternalTotemMenu.PotionFoodSlot.mayPlace` 接受 `PotionItem` 或 `isEdible()`
  - 食用走 `EternalTotemItem.eatStoredFood()`：对每个可食用物品调 `ItemStack.finishUsingItem(level, player)`，因此营养与原版食物效果（金苹果的生命恢复 / 伤害吸收等）都按原版逻辑生效；**忽略返回值、不消耗**，每次复活都能再用
  - NBT 键仍叫 `potion_items`：槽位含义已扩展，改键会让旧存档里的药水丢失
- ALT+右键打开配置界面（`RightClickHandler` → `OpenEternalTotemGuiPacket`）：27 格药水 / 食物槽 + 玩家背包，贴图 176×167，`inventoryLabelY = imageHeight - 94`
- **已移除右键开关功能**，`use()` 仅保留挥手动画
- **已移除 Mekanism 化学品储罐转化**（含右键升级与配置项 `eternal_totem.tank_conversion`）：贴图高度由 192 缩到 167，原 y=75 上的输入/输出两个槽位（x=8 / x=152）连同 `InputTankSlot` / `OutputTankSlot` 一起删除，玩家背包整体上移 25px（110→85、168→143）。改动这版 GUI 时注意对称：**贴图槽位坐标、`EternalTotemMenu` 的 addSlot 参数、`wouldMoveTotem` 里的快捷栏起算下标三者必须同步**

### 14. 永恒之剑 (`EternalSwordItem` / `EternalSwordEventHandler`)

- 继承 `SwordItem`，自定义 `ForgeTier`（攻击加成 0、耐久 0）保证剑本身攻击伤害为 0；`canBeDepleted()` 返回 false（无耐久条）
- `getAttributeModifiers()` 覆写：主手时把 `ATTACK_DAMAGE` 替换为 `getSwordDamage(stack) - 1`，使面板直接显示实际伤害
- 右击对 `player.getBoundingBox().inflate(range)` 内所有存活生物造成伤害；击杀模式 0=仅敌对生物（`Enemy` 接口），1=所有生物；跳过玩家与盔甲架
- 命中 Draconic-Evolution 混沌守卫时走 `Tool.bypassGuardianDamage()` 反射突破免伤（三段伤害都过这道判定）
- ALT+右键打开配置界面（27 格物品槽 + 击杀模式 + 攻击距离 8/16/24/32）
- **右击对每个目标结算三段**（`doAttack()`）：
  1. 剑自身伤害 = 所有 ID 不同的带伤害物品 `ATTACK_DAMAGE` modifier 之和（`calcDamage()`，同 ID 只计一次）+ `EnchantmentHelper.getDamageBonus`，最低 1；
  2. 剑上附魔的命中效果；
  3. 槽位里每把武器各做一次近战命中，用**那把武器自己**的伤害与命中附魔。
- 三段都在**同一 tick** 内结算，因此每次命中前必须清掉目标的受击无敌帧（`clearInvulnerable()` 直接置 `Entity.invulnerableTime = 0`）。原版 `LivingEntity.hurt()` 在 `invulnerableTime > 10` 时会丢弃不高于 `lastHurt` 的伤害、只补差额，而第一段就已把无敌帧顶到 20 刻——不清理的话第 2 段起基本被整段吞掉，三段只剩最大的一段生效
- 命中附魔效果统一走 `applyWeaponEffects()`：原版 `EnchantmentHelper.doPostHurtEffects/doPostDamageEffects` 都从"攻击者主手"读附魔，这里改为按附魔逐条调 `Enchantment#doPostHurt` / `#doPostAttack`（两者都是 public），既能指定任意一把武器，也不必临时替换玩家主手。原版走这两个钩子的只有 `ThornsEnchantment.doPostHurt` 与 `DamageEnchantment.doPostAttack`（节肢杀手的迟缓）
- **火焰附加与击退必须在这个方法里单独显式补**：原版把这两项硬编码在 `Player.attack()` 里（`getFireAspect` / `getKnockbackBonus` 配 `setSecondsOnFire(等级 × 4)` / `knockback(等级 × 0.5, 朝玩家朝向)`），`FireAspectEnchantment` 与 `KnockbackEnchantment` **都不覆写上面两个钩子**——只把附魔挂到剑上再调钩子，这两项完全不会有任何效果（曾因此出现"放了火焰附加 2 但生物不着火"）。新增附魔效果时，先确认它是走钩子还是像这两项一样被硬编码在原版攻击路径里
- 槽位武器按**物品 ID 去重**（`collectSlotWeapons()`），与剑伤害的"同 ID 只计一次"一致，避免塞满同一把武器刷命中次数
- **附魔 = 槽位中所有附魔书按点数累加后换算回等级**（`calcEnchantments()` / `levelForPoints()`）：
  - 等级 1~10 分别计 `2^0`~`2^9` 点，即 1/2/4/8/16/32/64/128/256/512；每类附魔各自累加
  - 累加点数够到哪一档就是哪一级，上限 `MAX_ENCHANT_LEVEL = 10`
  - 例：锋利 V + V = 16+16 = 32 点 → 锋利 VI；锋利 V + I = 17 点 → 仍是锋利 V
- 禁止附魔台/铁砧附魔（`isEnchantable`/`isBookEnchantable` 返回 false）

### 15. 自定义合成配方

- **SmeltingCraftRecipe**：3×3 外围 8 格放同一种可烧炼物品、中心放煤炭/木炭，一次产出 8 个烧炼结果。动态查询 `RecipeManager`，优先级 `SMELTING → BLASTING → SMOKING`。`isSpecial()` 返回 true 不出现在配方书中。用 `cachedResult` 在 `matches()` 与 `assemble()` 之间传递结果——`matches()` 开头清空缓存、匹配成功才写入，`assemble()`/`getResultItem()` 只读不清空，避免 Polymorph / FastWorkbench 多次调用时读到空结果
- **BrewingCraftRecipe**：无序，工作台中恰好 2 个物品（1 药水 + 1 酿造材料），输出原版酿造结果。关键 API：`PotionBrewing.hasMix(药水, 材料)` 与 `PotionBrewing.mix(材料, 药水)` 参数顺序相反。支持 2×2 背包合成格
- **PotionCombineRecipe**：无序，任意两瓶药水 → 混合药水；混合药水 + 火药/龙息/奶桶 → 类型转换。原版与混合的识别靠 `PotionUtils.getCustomEffects()` 是否非空。效果合并：等级取高者，等级相同则 `(d1 + d2) × duration_factor`（默认 0.75）

### 16. 合成材料

- `package_material_x1` — 打包材料：本模组所有机器与材料的通用合成基底，由木板与圆石互相合成，一次产出 8 个

### 17. 创造物品质变器 (`CreativeTransmuterBlock` / `CreativeTransmuterEntity`)

布局参考工作台的三段式转化机器，**不耗能、无耗时**。

- 内部就是一个 10 格 `ItemStackHandler`：槽 0~8 是 3×3 输入栏、槽 9 是输出栏，**每格上限都是 1**
- 每 tick 检查：九格填满、为同一种材料、命中配方表，且输出栏为空 → 一次清空九格并写入 1 个产物；输出栏被占用时停产
- **输入输出都持久化在机器里**（与工作台不同）：关界面、拆下重放都不丢，靠 `inventory` 一个 NBT 键存取
- **只收配方材料**：`TransmuteCatalog.isValidInput` 同时管住 GUI 槽位的 `mayPlace` 与管道插入，无关物品既放不进也塞不进，不会把九格堵死
- 对外 `IItemHandler`（`CreativeTransmuterConnection`，**方向无关**）两个方向不对称：输入栏可插不可抽、输出栏可抽不可插，每格上限 1，管道推入一组会自动摊到九格
- GUI 是工作台式布局（3×3 → 箭头 → 输出），**没有按钮也没有数据槽**——界面展示的就是机器真实物品栏，变更随容器自动同步
- 标题栏右侧有一个 **`?` 配方帮助卡**（参考零刻压印器 / 生物农场的同款卡片）：hover 弹出 `产物 ← 材料 ×N` 的逐行对齐列表，点击翻页，未装 Mekanism 时显示空态文案。配方不在 JEI 里，这里是游戏内唯一的查询入口，改动配方时**不要漏掉它**
- 配方见 `setup/TransmuteCatalog`：**写死在代码里**（不是数据包配方，因此 JEI 与配方书里都没有），用注册名书写并延迟解析，联动模组未安装时对应条目静默跳过、tooltip 显示"未安装 …"。当前仅一条：`9 × 终极化学品储罐 → 创造化学品储罐`
- `TransmuteCatalog.summaries()` 返回结构化的 `Summary`（产物 / 材料 / 个数）供帮助卡按列对齐绘制，`tooltipLines()` 在此基础上拼出带颜色码的 tooltip 行——两处共用同一份数据，新增配方只改配方表
- 每条配方固定是"**9 个同种材料 → 1 个产物**"，匹配逻辑要求九格物品完全同种
- 已加入 `ClockEntity.isExcluded`：九格凑齐即转化、转完输入栏必空，加速它只有性能开销

## NBT 数据存储

各 Entity 通过 `saveAdditional`/`load` 持久化数据：

- **FarmEntity（旧的专属农场体系已删除）**：不再存在
- **AutoFarmlandEntity**: `level`(long), `tickCount`(long), `rows`(ListTag: 物品NBT + `Stock`), `directionState`(int[6]), `outputEnabled`(bool)
- **StorageFountainEntity**: `output`(long), `save_stick`(ListTag: 每项含物品NBT + `Long_Count`), `directionState`(int[6]: 每面 0~10 对应 随机/禁用/槽1~槽9), `outputEnabled`(boolean), `markerSlot`(CompoundTag)
- **LiquidFountainEntity**: `fluid_id`(string), `fluid_amount`(int), `transferDown/Up/North/South/West/East`(boolean), `inputSlot`(+槽), `outputSlot`(-槽)
- **ClockEntity**: `enabled`(boolean), `directionEnabled`(int[6]), `speed`(int)
- **MobFarmEntity**: `entityTag`(CompoundTag), `level`(long), `tickCount`(long), `rows`(ListTag: 物品NBT + `Stock` + `Weight` + `Tool`), `directionState`(int[6]), `outputEnabled`(bool), `specialSlot`(CompoundTag)
- **ResourceFarmEntity**: `marker`(string), `level`(long), `tickCount`(long), `rows`(ListTag: 物品NBT + `Stock` + `Weight`), `directionState`(int[6]), `outputEnabled`(bool)
- **CreativeTransmuterEntity**: `inventory`(CompoundTag: `ItemStackHandler` 的序列化结果，槽 0~8 输入栏、槽 9 输出栏)
- 所有数值加载时经过 `Tool.suit()` 防负数处理

**战利品表**：`auto_farmland` / `instant_furnace` / `instant_inscriber` / `liquid_fountain` / `mob_farm` / `resource_farm` / `storage_fountain` / `clock` / `creative_transmuter` 均使用 `copy_name` + `copy_nbt` 把上述 BlockEntity 数据存进 `BlockEntityTag`，拆下后重放即可恢复；`platform` / `supply_crate` / `extraction_interface` 无持久数据，用普通战利品表。

## Tool 工具方法

- `suit(long)` / `suitInt(long)`: 防溢出裁剪，负值返回 `Long.MAX_VALUE` / `Integer.MAX_VALUE`
- `takeItem(Player, ItemStack)`: 尝试给玩家物品，失败则丢到地上
- `toJsonArray` / `toItemList` / `toBlockList`: 物品列表 ↔ NBT ListTag 转换
- `sort`: 物品列表按注册名排序 (minecraft: 物品优先)
- `isGuardian(Entity)` / `bypassGuardianDamage(...)`: Draconic-Evolution 混沌守卫的反射探测与免伤突破，未装 DE 时静默禁用

## 资源文件

- `src/main/resources/assets/alltheimbaium/blockstates/`、`models/block/`、`models/item/`、`textures/block/`、`textures/item/`
- `src/main/resources/assets/alltheimbaium/lang/zh_cn.json` + `en_us.json` — 两份键集必须完全一致
- `src/main/resources/data/alltheimbaium/recipes/`（复数，1.20.1 正确）— 按来源分子目录：`main/` 本模组机器、`crafting/` 原版简化、`ae2/`、`mek/`、`mystical/`、`thermal/`、`blood/`、`create/`、`draconicevolution/`、`salvaging/`
- `src/main/resources/data/alltheimbaium/loot_tables/blocks/`（复数）
- 本模组机器配方统一写在 `recipes/main/`，`group` 统一为 `alltheimbaium`；成本随品级递增

## 依赖

- **Forge** 1.20.1-47.2.20 (唯一硬依赖)
- 可选联动 (通过标签/配方):
  - AE2 (Applied Energistics 2) — 零刻压印器的配方来源
  - Mekanism (通用机械) — 图腾的化学品储罐转化
  - Blood Magic (血魔法)
  - Mystical Agriculture (神秘农业)
  - Silent Gear (寂静装备)
  - Thermal Series (热力系列)
  - Modern Industrialization / Extended Industrialization — 存储制造机的默认可接受命名空间

## 命名规范

- 所有注册名不带前缀，依靠 mod ID 命名空间区分
- 语言键格式: `block.alltheimbaium.<name>`, `item.alltheimbaium.<name>`, `screen.alltheimbaium.<name>`, `chat.alltheimbaium.<name>`, `tip.alltheimbaium.<name>`
- 物品名**不带** `§` 颜色前缀（由 Rarity 驱动）；tooltip 正文的 `§` 由 `Tip` 统一注入
- 类名驼峰命名，包名全小写
- 所有 BlockEntity 实现尾气级异常处理（try-catch + log error）

## 代码风格约定

- 所有文件使用 UTF-8 编码
- 使用 `@Nonnull`/`@Nullable` 注解标记参数
- 过时 API 用 `@SuppressWarnings("deprecation")` 抑制警告
- 内部使用 SLF4J 日志
- 方块/物品类中 tick 逻辑在私有方法中，通过匿名 `BlockEntityTicker` lambda 调用
- 所有公开交互方法（`use` 等）的最外层捕获 Throwable
- 物品的 `appendHoverText()` 一律先调 `super.appendHoverText(...)`，再用 `Tip.of(tooltip)` 链式构建内容
