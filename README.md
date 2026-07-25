
# All the imbaium

可以独立使用；存在大量与 ATM 整合包相关的配方内容，更建议一起使用。

### 添加了几类相当破坏平衡的机器

- 瞬间使作物成熟且不会退化的耕地，ATI 耕地
- 加速相邻方块工作的时钟方块（4 个等级：x2、x4、x16、x256）
- 能过滤标签复制矿物的机器，ATI 存储方块制造机
- 能使流体变为无限的机器，ATI 液体无限制造机
- 自动产生怪物资源的机器，多种 ATI 农场
- 一键生成 3×3 区块平台的平台方块
- 可重复使用的永恒图腾，死亡时自动触发
- 多个对 ATM 整合包有效的简化配方

### ATI 耕地

- 使种植的作物立即成熟
- 采用了设置生长阶段的方式，所以能忽略作物生长条件
- 避免了大部分退化为耕地的问题
- 会有水浸湿的动画，但实际不产生作用
- 种植带有梗的作物时，多个相邻可能会导致梗掉落
- 禁止了树苗等作物种植在上面

### ATI 时钟方块

- 4 个等级：x2、x4、x16、x256 加速倍率
- 右击切换全局开关状态
- 加速邻接 6 个方块的机器工作速度
- 还会额外触发随机刻（如作物生长）
- 不会加速其他时钟方块和 ATI 耕地

### ATI 存储方块制造机

- 主手持带有 forge:ores 或 forge:storage_blocks 标签的物品右击时，将记录该物品
- 会自动产生所记录的物品
- 会随时间增加物品的产出速度
- 物品的子标签同样支持，如 forge:ores/iron
- 一个机器可以最多记录9种物品
- 会自动向六个面传输物品
- 支持管道抽取，建议用 AE2 的存储面板管理

### ATI 液体无限制造机

- 可以输入任何单一流体
- 如果输入的液体达到1万桶，将变为无限
- 一个机器可以支持1种流体
- 会自动向六个面传输流体
- 支持管道抽取，建议用 AE2 的存储面板管理

### ATI 农场

- 42 种不同的怪物/资源农场
- 可以通过物品的描述了解相关产物
- 会随时间增加物品的产出速度
- 会自动向六个面传输物品
- 支持管道抽取，建议用 AE2 的存储面板管理

### 平台方块

- 右击生成一个 3×3 区块的平滑石头平台
- 区块边界用石砖标识
- 手持物品时不会触发

### 永恒图腾

- 类似不死图腾，但可重复使用
- 死亡时自动触发，无需手持
- 触发后给予多种强力增益效果
- 右击可切换开关状态

### 额外配方

- ATM： 添加了三种模板的合成
- AE2： 增加了相关水晶的简单合成方式，多种产物的更高效合成配方
- Blood Magic： 增加了血命果合成，增加了 Mek 肉汤转换血液配方
- Mek： 增加了无中生有相关粉尘的合成配方
- Mystical： 还原了盖亚之魂相关配方，增加了耕地回收配方，增加了生长加速器升级配方
- Silent Gear： 增加了很多回收配方，Mek 钓鱼机产生的不可堆叠物品均可回收
- Thermal： 添加了 Blood Magic 和 Evil Craft 血液的相互转化

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

### Added several types of machines that are quite disruptive to balance

- Farmland that instantly ripens crops without degradation, ATI Farmland
- Clock blocks that accelerate adjacent machines (4 tiers: x2, x4, x16, x256)
- Machine capable of filtering tags and copying minerals, ATI Storage Block Fountain
- A machine that can turn fluids into infinity, ATI Liquid Infinity Fountain
- A machine that automatically generates monster resources, ATI Farm
- A platform block that generates a 3×3 chunk platform with one click
- A reusable Eternal Totem that automatically triggers on death
- Multiple effective simplified formulas for ATM integration packages

### ATI Farmland

- Make the planted crops mature immediately
- The method of setting growth stages has been adopted, so crop growth conditions can be ignored
- Avoiding the problem of most degradation into arable land
- There will be water soaked animations, but they will not actually have any effect
- When planting crops with stems, multiple adjacent ones may cause the stems to fall off
- Planting seedlings and other crops on it is prohibited

### ATI Clock Block

- 4 tiers: x2, x4, x16, x256 acceleration multipliers
- Right-click to toggle global on/off state
- Accelerates machines on all 6 adjacent sides
- Also triggers additional random ticks (e.g., crop growth)
- Skips other clock blocks and ATI farmland

### ATI Storage Block Fountain

- When the owner right-clicks with an item that has 'forge:ores' or 'forge:storage_blocks' tags, the item will be recorded
- Automatically generates recorded items
- The output speed of items will increase over time
- Sub-tags of items are also supported, such as forge:ores/iron
- A machine can record up to 9 types of items
- Automatically transfers items to six sides
- Supports pipe extraction, recommended to use AE2's storage panel for management

### ATI Liquid Infinity Fountain

- Can input any single fluid
- If the input liquid reaches 10,000 barrels, it will become infinite
- One machine can support one type of fluid
- Automatically transfers fluid to six sides
- Supports pipe extraction, recommended to use AE2's storage panel for management

### ATI Farm

- 42 different mob/resource farms
- You can learn about related products through the description of the item
- The output speed of items will increase over time
- Automatically transfers items to six sides
- Supports pipe extraction, recommended to use AE2's storage panel for management

### Platform Block

- Right-click to generate a 3×3 chunk platform made of smooth stone
- Chunk borders are marked with stone bricks
- Does not trigger when holding an item

### Eternal Totem

- Similar to Totem of Undying, but reusable
- Automatically triggers on death, no need to hold in hand
- Grants multiple powerful buff effects after triggering
- Right-click to toggle on/off state

### Additional Formulas

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
