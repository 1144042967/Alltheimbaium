
# All the imbaium

可以独立使用；存在大量与 ATM 整合包相关的配方内容，更建议一起使用。

## 物品品级

物品名称的颜色由原版稀有度决定，同时对应它的能力强度与合成成本。在创造模式标签页里自上而下扫一遍，颜色梯度就是能力梯度。

| 品级 | 颜色 | 定位 | 物品 |
|------|------|------|------|
| 材料级 | 白色 | 仅作合成中间物 | ATI 打包材料 |
| 便利级 | 黄色 | 省事、提速，但本身不产生资源 | ATI 耕地、生成平台、取出接口、加速时钟、补给箱 |
| 高效级 | 青色 | 需要输入或能量，批量加工 | ATI 自动耕地、零刻熔炉、零刻压印器、生物农场、资源农场 |
| 破坏平衡 | 淡紫 | 一次建立后无限产出，或绝对能力 | 存储方块制造机、液体无限制造机、永恒图腾、永恒之剑 |

合成成本同品级一致：便利级用铁锭、高效级用铁块、破坏平衡用钻石块，永恒系列用下界合金块。

## 机器

### ATI 耕地

- 上方种下的作物立即成熟，无需等待
- 采用直接设置生长阶段的方式，因此忽略作物自身的生长条件
- 始终视为湿润，无需引水，也不会退化为泥土
- 只接受作物类（小麦 / 胡萝卜 / 马铃薯 / 甜菜 / 下界疣，以及南瓜和西瓜的茎），树苗等无法种植
- 南瓜与西瓜的茎相邻放置多个时，茎可能掉落

### ATI 自动耕地

- 上方作物被反复收获，掉落直接存入机器，全程不消耗能量
- 只收割普通作物；下界疣与南瓜、西瓜的茎不会被收割
- 收获效率每级 +1%，等级每 20 秒 +1，无上限
- 产物按大数累计在 27 行内，可在界面取出或由管道抽取
- 每个面可单独指定输出槽位，也可整体关闭主动输出
- 顶面右键留给种植，界面需从侧面或底面打开

### ATI 加速时钟

- 额外驱动相邻机器，速度 2 ~ 1024 共 10 档
- 只对带方块实体的机器生效；普通方块的随机刻固定只额外触发 1 次
- 六个方向可逐个开关，单机开关与逐面开关都会持久化
- 全局开关对所有时钟同时生效，重进游戏恢复默认
- 不会加速：加速时钟、ATI 耕地、取出接口、补给箱、零刻熔炉、零刻压印器
  - 时钟与耕地是为了避免互相叠加
  - 取出接口与补给箱的 tick 本身不产生进度，熔炉与压印器每 tick 就按当前电量整批结算完，重复调用不会有额外产出

### GUI 标题颜色

所有带界面的机器，其界面标题颜色与对应物品名的颜色一致（即该物品的品级色），并统一带阴影，不需要额外记忆。

### ATI 存储方块制造机

- 在界面的标记槽放入受支持的物品即开始无限复制；放入已标记的物品则取消标记并清空存量
- 受支持的物品：带 `ores` / `storage_blocks` / `ingots` / `dusts` / `gems` 等标签，或来自 `modern_industrialization` / `extended_industrialization`
- 最多同时复制 9 种物品，产量初始 0.1/tick，每 20 秒再 +0.1/tick，无上限
- 取物：左键取 1、Shift 取 1 组、空格取到背包满
- 六个面的按钮可指定该面输出哪一个槽（或随机 / 禁用），该设置同时约束管道的被动抽取
- 支持管道抽取，建议用 AE2 的存储面板管理

### ATI 液体无限制造机

- 只接受一种流体，达到 1 万桶（可配置，默认 10,000,000 mB）后变为无限
- 未达阈值时不会主动输出，但仍可被管道抽走存量
- 达到无限后，六个面每 tick 各输出一次满量流体，受逐面开关控制
- 手持空桶或带液容器右键可直接装取；界面左上 + 槽也会自动处理进出容器
- 模型为流体储罐外观，内部液体随存量动态渲染
- 支持管道输入输出，建议用 AE2 的存储面板管理

### ATI 生物农场

- 未收容时右键捕捉附近 5 格内最近的生物；没捕捉到才会被放下
- 也可放下后在标记槽放入刷怪蛋、或该生物的特征掉落物来完成收容
- 每台只能收容一次，不可取消或更换
- 收容后同一个槽变为使用槽：放入剪刀 / 桶 / 碗 / 玻璃瓶可模拟右键获取对应产物，且不消耗耐久
- 产出速率 = 权重 × 等级 ÷ 500 件/秒，等级每 20 秒 +1
- 产物按大数累计在 27 行内，可在界面取出或由管道抽取

### ATI 资源农场

- 标记槽放入一个标记物即开始产出，标记物会留在槽内展示
- 标记物可以是圆石 / 任意树苗 / 竹子 / 甘蔗 / 冰 / 骨粉 / 花 / 蘑菇等
- 每台只能标记一次，不可取消或更换
- 产出速率 = 权重 × 等级 ÷ 500 件/秒，等级每 20 秒 +1
- 悬停界面右上角的 ? 可查看「标记物 → 产物」的完整对照表
- 产物按大数累计在 27 行内，可在界面取出或由管道抽取

### ATI 零刻熔炉

- 原料进入输入区即刻完成烧炼，每件耗 1000 FE
- 熔炉 / 高炉 / 烟熏配方均可，优先级为熔炉 → 高炉 → 烟熏
- 输入与输出各 18 种，以大数存储，不受 64 堆叠限制
- Shift+左键背包物品或点击投料按钮即可投料；取物统一为左键 1 / Shift 一组 / 空格取满
- 只接收 FE，不向外放电

### ATI 零刻压印器

- 读取 AE2 压印机配方即时生成，每件产物耗 1000 FE
- **压板**：只消耗中间那格原料，上下模板既不放入也不消耗，1 份原料产出它支持的全部压板
- **组装**：消耗上中下三格的全部材料，先算 3 材料配方再算 2 材料配方
- 输入 18 种 / 输出 9 种，以大数存储
- 依赖 AE2 配方，未安装 AE2 时不产出；只接收 FE，不向外放电

### ATI 取出接口

- 沿本模组方块连通搜索，把搜到的产物机器汇总成一个只读视图，交给管道抽取
- 不必紧贴机器：中间隔着本模组方块，隔多远都能搜到，搜索可以穿过机器继续往外找
- 可抽取：存储方块制造机 / 液体无限制造机 / 生物农场 / 资源农场 / 自动耕地
- 只传导不抽取：ATI 耕地 / 零刻熔炉 / 零刻压印器，以及取出接口自身
- 六个面访问到的都是同一份聚合内容；只读，无法向机器回灌
- 搜索止于非本模组方块：生成平台铺出的地面是原版方块，会断开连通
- 原版箱子与本模组以外的容器不会被聚合
- 每 1 秒重新搜索一次连通范围，新增机器最多 1 秒后生效
- 连通的方块超过 4096 个时会截断搜索，并在日志中告警

### ATI 生成平台

- 在所在高度铺开 3×3 区块（48×48 方块）的浮空平台
- Shift+右键直接生成整片区域；右键则打开界面，可逐区块生成或重建
- 平台四边为石砖、内部为平滑石，内部四角是荧光蛙明灯
- 界面里的伪装开关是全局的，开启后所有平台顶面显示为石砖
- 只替换空气与平台自身方块，遇到其它方块会跳过

### ATI 补给箱

- 用累计的补给点兑换随机物品，每次列出 10 个分类各 1 件
- 兑换消耗 3 点获得 1 件；花 1 点刷新可立即重掷整份列表
- 补给点来源：游玩每 30 分钟 +1，每完成一个成就 +5
- 列表每到整点自动重掷，界面开着也会刷新

## 装备

### 永恒图腾

- 死亡时自动触发，取消死亡并把血量留 1 点，可无限次使用
- 放在背包或饰品栏任意位置即可生效，无需手持
- 触发时清除全部状态效果，并给予 40 秒抗火 / 45 秒生命恢复 II / 5 秒伤害吸收 II
- 27 格药水槽里的药水会在基础效果之后逐个生效，可用来补回被清除的增益
- 按住 ALT 右键打开配置界面：药水槽，以及「终极化学品储罐 → 创造化学品储罐」的转化

### 永恒之剑

- 右击对范围内的生物同时造成剑的伤害
- 伤害 = 槽位中所有 ID 不同的带伤害物品的攻击力之和，最低 1
- 附魔来自槽位里的附魔书，等级直接相加（锋利 V + III → 锋利 VIII）
- 按住 ALT 右键打开配置界面：击杀模式 / 攻击距离 / 27 格物品槽
- 命中 Draconic-Evolution 的混沌守卫时可突破其免伤
- 无法附魔、无法锻造，也不能作为合成材料

## 额外配方

- ATM：添加了三种模板的合成
- AE2：增加了相关水晶的简单合成方式，多种产物的更高效合成配方
- Blood Magic：增加了血命果合成，增加了 Mek 肉汤转换血液配方
- Mek：增加了无中生有相关粉尘的合成配方
- Mystical：还原了盖亚之魂相关配方，增加了耕地回收配方，增加了生长加速器升级配方
- Silent Gear：增加了很多回收配方，Mek 钓鱼机产生的不可堆叠物品均可回收
- Thermal：添加了 Blood Magic 和 Evil Craft 血液的相互转化

### 煤炭合成熔炼

- 在 3×3 工作台中，外围 8 格放同一种可烧炼物品，中心放煤炭/木炭
- 一次合成产出 8 个烧炼结果（不超过物品最大堆叠数）
- 支持熔炉、高炉、烟熏炉中所有可烧炼的物品
- 无需燃料消耗，直接在工作台中完成

### 酿造合成

- 原版药水 + 酿造材料在工作台中按酿造配方合成
- 1 瓶原版药水 + 1 个酿造材料 = 1 瓶原版酿造结果

### 混合药水合成

- 任意两瓶药水（原版或混合）在工作台中合成 = 混合药水
- 输出类型优先级：滞留 > 喷溅 > 普通
- 混合药水 + 火药 → 喷溅混合药水
- 混合药水 + 龙息 → 滞留混合药水
- 混合药水 + 奶桶 → 普通混合药水
- 效果合并：保留最高等级；只在其中一瓶中则取该瓶时间，两瓶都有则 (两者之和) × 0.75

# All the imbaium

Can be used independently; There are a large number of recipe contents related to ATM integration packages, and it is recommended to use them together.

## Item tiers

An item's name colour comes from its vanilla rarity, and matches both its power and its crafting cost. Scan the creative tab top-down and the colour gradient is the power gradient.

| Tier | Colour | Role | Items |
|------|--------|------|-------|
| Material | White | Crafting intermediate only | ATI Package Material |
| Utility | Yellow | Convenience and speed, but produces nothing on its own | ATI Farmland, Generation Platform, Extraction Interface, Acceleration Clock, Supply Crate |
| Advanced | Aqua | Needs input or energy, batch processing | ATI Auto Farmland, Instant Furnace, Instant Inscriber, Mob Farm, Resource Farm |
| Overpowered | Light purple | Infinite output once set up, or absolute power | Storage Block Fountain, Liquid Infinity Fountain, Eternal Totem, Eternal Sword |

Cost tracks the tier: iron ingots for Utility, iron blocks for Advanced, diamond blocks for Overpowered, and netherite blocks for the Eternal items.

## Machines

### ATI Farmland

- Crops planted on top mature instantly
- Growth stages are set directly, so the crop's own growth conditions are ignored
- Always counts as hydrated — no water needed, and it never reverts to dirt
- Only crops are accepted (wheat / carrot / potato / beetroot / nether wart, plus pumpkin and melon stems); saplings cannot be planted
- Placing several pumpkin or melon stems next to each other may cause the stems to drop

### ATI Auto Farmland

- Crops on top are harvested over and over, with drops going straight into the machine — no energy required
- Only regular crops are harvested; nether wart and pumpkin/melon stems are not
- Harvest efficiency is +1% per level, and the level rises by 1 every 20 s with no cap
- Products accumulate as big numbers across 27 rows, extractable in the GUI or by pipes
- Each face can target a specific output slot, or active output can be switched off entirely
- The top face is reserved for planting — open the GUI from a side or the bottom

### ATI Acceleration Clock

- Drives adjacent machines up to 1024× speed, across 10 steps from 2 to 1024
- Only machines with a block entity are sped up; plain blocks get exactly 1 extra random tick
- Each of the six faces can be toggled; both the per-clock switch and the per-face switches persist
- The global switch affects every clock at once and resets to its default on relog
- Never accelerated: Acceleration Clocks, ATI Farmland, Extraction Interface, Supply Crate, Instant Furnace, Instant Inscriber
  - Clocks and farmland are excluded so they cannot stack on each other
  - The Extraction Interface and Supply Crate do no per-tick work, and the Instant Furnace and Inscriber already settle a whole batch each tick — repeating the call adds nothing

### GUI title colours

Every machine with a GUI titles it in the same colour as its item name (that item's tier colour), always drawn with a drop shadow, so there is nothing extra to memorise.

### ATI Storage Block Fountain

- Drop a supported item into the GUI's marker slot to duplicate it forever; dropping an already-marked item unmarks it and wipes its stock
- Supported items carry tags such as `ores` / `storage_blocks` / `ingots` / `dusts` / `gems`, or come from `modern_industrialization` / `extended_industrialization`
- Up to 9 item types at once; output starts at 0.1/tick and grows by 0.1/tick every 20 s with no cap
- Taking items: click for 1, Shift-click for a stack, Space-click to fill your inventory
- The six face buttons pick which slot that face outputs (or Random / Disabled), and that setting also restricts passive pipe extraction
- Supports pipe extraction, recommended to use AE2's storage panel for management

### ATI Liquid Infinity Fountain

- Accepts a single fluid and becomes infinite once it reaches 10,000 buckets (configurable, 10,000,000 mB by default)
- Below the threshold it never pushes outward, but pipes can still drain its stock
- Once infinite, all six faces push a full load every tick, controlled face by face
- Right-click with an empty or filled container to draw or pour directly; the + slot in the GUI handles containers automatically
- Modelled as a fluid tank, with the internal fluid rendered to match its stock
- Supports pipe input and output, recommended to use AE2's storage panel for management

### ATI Mob Farm

- While empty, right-click to capture the nearest mob within 5 blocks; it is only placed down if nothing was captured
- You can also contain the mob after placing it, by putting a spawn egg or that mob's signature drop into the marker slot
- Each farm can contain only once — no cancelling or swapping
- Once contained, that slot becomes a use slot: shears / a bucket / a bowl / a glass bottle are right-clicked on the mob for you, with no durability cost
- Output rate = weight × level ÷ 500 items/s, with the level rising by 1 every 20 s
- Products accumulate as big numbers across 27 rows, extractable in the GUI or by pipes

### ATI Resource Farm

- Drop one marker into the marker slot to start producing; the marker stays there on display
- Markers include cobblestone / any sapling / bamboo / sugar cane / ice / bone meal / flowers / mushrooms and more
- Each farm can be marked only once — no cancelling or swapping
- Output rate = weight × level ÷ 500 items/s, with the level rising by 1 every 20 s
- Hover the ? at the GUI's top-right for the full marker → products table
- Products accumulate as big numbers across 27 rows, extractable in the GUI or by pipes

### ATI Instant Furnace

- Anything entering the input area is smelted instantly, at 1000 FE per item
- Works with furnace / blast furnace / smoker recipes, in that priority order
- 18 input and 18 output item types, stored as big numbers with no 64 stack limit
- Shift+click an inventory item or use the deposit button to feed it; take items with click (1) / Shift-click (a stack) / Space-click (fill)
- Accepts FE only — it never outputs power

### ATI Instant Inscriber

- Generates AE2 inscriber recipes instantly, at 1000 FE per product
- **Press**: consumes only the middle material — the top and bottom templates are neither needed nor consumed, and one material yields every press it feeds into
- **Assemble**: consumes all materials in the top, middle and bottom slots, running 3-material recipes before 2-material ones
- 18 input / 9 output item types, stored as big numbers
- Depends on AE2 recipes and produces nothing without AE2; accepts FE only, never outputs power

### ATI Extraction Interface

- Searches along this mod's blocks and merges every machine it reaches into one read-only view for pipes to pull from
- No need to touch the machine — it is found as long as ATI blocks bridge the gap, and the search passes straight through machines
- Extracted: Storage Block Fountain / Liquid Infinity Fountain / Mob Farm / Resource Farm / Auto Farmland
- Conducts only, never extracted: ATI Farmland / Instant Furnace / Instant Inscriber, and the interface itself
- All six faces expose the same merged view; read-only, so nothing can be pushed back
- The search stops at blocks from other mods — the floor a Generation Platform lays down is vanilla blocks and breaks the link
- Vanilla chests and containers from other mods are never aggregated
- The connected range is rescanned every 1 s, so a new machine is picked up within a second
- Beyond 4096 connected blocks the search is truncated and a warning is written to the log

### ATI Generation Platform

- Lays out a floating platform of 3×3 chunks (48×48 blocks) at its own height
- Shift+right-click generates the whole area; right-click opens the GUI to generate or rebuild chunk by chunk
- The edges are stone bricks, the interior is smooth stone, and its inner corners are ochre froglights
- The disguise switch in the GUI is global: turning it on shows every platform top as stone bricks
- Only air and the platform's own blocks are replaced; anything else is skipped

### ATI Supply Crate

- Trade accumulated supply points for random items — one per category across 10 categories
- Redeeming costs 3 points for one item; spending 1 point rerolls the whole list immediately
- Supply points come from +1 per 30 min of playtime and +5 per achievement
- The list rerolls on every in-game hour boundary, even while the GUI stays open

## Equipment

### Eternal Totem

- Triggers on death, cancelling it and leaving you at 1 HP — reusable forever
- Works from anywhere in your inventory or accessory slots, no need to hold it
- On trigger it clears every effect, then grants 40 s Fire Resistance / 45 s Regeneration II / 5 s Absorption II
- The 27 potion slots apply one by one after those base effects — use them to restore the buffs that were cleared
- ALT+right-click opens the config GUI: the potion slots and the Ultimate → Creative Chemical Tank conversion

### Eternal Sword

- Right-click to deal the sword's damage to every mob in range at once
- Damage is the summed attack of every distinct damage item in its slots, minimum 1
- Enchantments come from books in its slots, with levels added outright (Sharpness V + III → Sharpness VIII)
- ALT+right-click opens the config GUI: kill mode / range / 27 item slots
- Strikes bypass the damage immunity of Draconic-Evolution's Chaos Guardian
- Cannot be enchanted, smithed, or used as a crafting material

## Additional Formulas

- ATM: adds synthesis of three templates
- AE2: adds a simple synthesis method for related crystals and more efficient synthesis formulas for various products
- Blood Magic: adds blood fruit synthesis and Mek broth to blood conversion recipe
- Mek: adds synthesis formulas for creating related dust out of nothing
- Mystical: restores the Gaia Spirit related formula, adds farmland recycling formula, and adds growth accelerator upgrade formula
- Silent Gear: adds many recycling formulas, non-stackable items from Mek fishing machines can all be recycled
- Thermal: adds Blood Magic and Evil Craft blood conversion to each other

### Coal Smelting Craft

- In a 3×3 crafting table, place 8 identical smeltable items in the outer ring and 1 coal/charcoal in the center
- Produces 8 smelted results at once (capped at max stack size)
- Works with all furnace, blast furnace, and smoker recipes
- No fuel required — smelting is done directly in the crafting table

### Brewing Craft

- Vanilla potion + brewing ingredient in the crafting table uses vanilla brewing recipes
- 1 vanilla potion + 1 brewing ingredient = 1 vanilla brewed result

### Mixed Potion Craft

- Any two potions (vanilla or mixed) in the crafting table = a mixed potion
- Output type priority: lingering > splash > regular
- Mixed potion + Gunpowder → splash mixed potion
- Mixed potion + Dragon's Breath → lingering mixed potion
- Mixed potion + Milk Bucket → regular mixed potion
- Effect merge: highest level wins; duration from the source with the higher level; if both have it, (sum) × 0.75
