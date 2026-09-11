# -*- coding: utf-8 -*-
"""
ATI 配方总表生成器。

扫描 src/main/resources/data/alltheimbaium/recipes/ 下的全部配方 JSON，
按目录（即本模组的联动分类）整理成 RECIPES.md。

配方有增删改后重新执行即可同步：
    python tools/gen_recipe_docs.py

注意事项：
- 大部分配方被 forge:conditional 包裹，内层才是真正的配方；条件里的 modid 就是生效前提。
- 物品一律写注册名（不查语言文件，因为其它 MOD 的译文不在本仓库里）。
- 各 MOD 的配方 schema 差异极大，这里用「递归收集物品/流体引用」的通用办法提取，
  新增未知配方类型时会自动退化到原样列出，不会漏条目。
"""
import json
import os
import re
from collections import Counter, OrderedDict

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RECIPE_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "alltheimbaium", "recipes")
OUT_FILE = os.path.join(ROOT, "RECIPES.md")

# 目录 → 该分类的中文名（目录即本模组的联动分类）
SECTION_NAMES = OrderedDict([
    ("main", "本模组机器"),
    ("crafting", "原版简化"),
    ("salvaging", "拆解回收"),
    ("ae2", "AE2 / 高级 AE"),
    ("mek", "通用机械 Mekanism"),
    ("thermal", "热力系列 Thermal"),
    ("mystical", "神秘农业 Mystical Agriculture"),
    ("blood", "血魔法 Blood Magic"),
    ("create", "机械动力 Create"),
    ("draconicevolution", "龙之研究 Draconic Evolution"),
])

# 配方类型 → 在哪台机器上做 / 这是什么配方
TYPE_LABELS = {
    "minecraft:crafting_shaped": "工作台（有序）",
    "minecraft:crafting_shapeless": "工作台（无序）",
    "mekanism:mek_data": "工作台（有序）",
    "silentgear:salvaging": "寂静装备 · 拆解台",
    "thermal:bottler": "热力 · 灌装机",
    "thermal:refinery": "热力 · 精炼厂",
    "advanced_ae:reaction": "高级 AE · 反应仓",
    "expatternprovider:circuit_cutter": "扩展 AE · 电路切割机",
    "mekanism:crushing": "通用机械 · 粉碎机",
    "mekanism:enriching": "通用机械 · 富集仓",
    "mekanism:metallurgic_infusing": "通用机械 · 冶金灌注机",
    "mysticalagriculture:infusion": "神秘农业 · 注魔祭坛",
    "create:mixing": "机械动力 · 混合搅拌",
    "alltheimbaium:smelting_craft": "本模组 · 熔炼合成配方",
    "alltheimbaium:brewing_craft": "本模组 · 酿造合成配方",
    "alltheimbaium:potion_combine": "本模组 · 药水融合配方",
}

# 自定义配方（行为写在代码里，JSON 只是个标记）
CODE_DRIVEN = {
    "alltheimbaium:smelting_craft": "3×3 外围 8 格放同一种可烧炼物品、中心放煤炭/木炭，一次产出 8 个烧炼结果。"
                                   "动态查询熔炉→高炉→烟熏炉三级配方，行为见 recipe/SmeltingCraftRecipe.java",
    "alltheimbaium:brewing_craft": "工作台中恰好 2 个物品（1 药水 + 1 酿造材料）无序合成，输出原版酿造结果。"
                                   "行为见 recipe/BrewingCraftRecipe.java",
    "alltheimbaium:potion_combine": "任意两瓶药水合成混合药水；混合药水 + 火药/龙息/奶桶 转换类型。"
                                    "等级取高者，同等级时长按配置系数叠加。行为见 recipe/PotionCombineRecipe.java",
}


# ==================== 通用提取 ====================

def _short_count(node, fallback):
    for key in ("count", "amount", "#"):
        v = node.get(key)
        if isinstance(v, int):
            return v
    return fallback


def walk(node, out, count=1, hint=""):
    """
    递归收集节点里的物品 / 流体 / 标签引用。

    hint 是父级键名，用来区分同名异物的引用：通用机械的 chemicalInput 也用 tag/amount
    表达，但它指的是**化学物质**而不是物品标签，混在一起会看错。
    """
    if isinstance(node, list):
        for item in node:
            walk(item, out, count, hint)
        return
    if not isinstance(node, dict):
        return

    chemical = "chemical" in hint.lower()

    # 物品：{"item": "ns:path", "count": n}
    if isinstance(node.get("item"), str):
        out.append(("item", node["item"], _short_count(node, count)))
        return
    # 高级 AE 的序列化物品：{"id": "ns:path", "#": n}
    if isinstance(node.get("id"), str) and "#" in node:
        out.append(("item", node["id"], _short_count(node, count)))
        return
    # 流体（1.20.1 序列化形式）：{"FluidName": "...", "Amount": n}
    if isinstance(node.get("FluidName"), str):
        out.append(("fluid", node["FluidName"], _short_count(node, count)))
        return
    # 流体：{"fluid": "ns:path", "amount": n}
    if isinstance(node.get("fluid"), str):
        out.append(("fluid", node["fluid"], _short_count(node, count)))
        return
    # 神秘农业作物部件：{"crop": "ns:path", "component": "seed"}
    if isinstance(node.get("crop"), str):
        comp = node.get("component")
        suffix = f"（{comp}）" if isinstance(comp, str) else ""
        out.append(("crop", node["crop"] + suffix, _short_count(node, count)))
        return
    # 标签 / 化学品：{"tag": "forge:..."}、{"tag": "mekanism:...", "amount": n}
    if isinstance(node.get("tag"), str):
        out.append(("chemical" if chemical else "tag", node["tag"], _short_count(node, count)))
        return

    local = _short_count(node, count)
    for key, value in node.items():
        if key in ("count", "amount", "#", "type", "conditions"):
            continue
        # 化学上下文要粘住：chemicalInput 里再嵌 ingredient 时也得认出来
        walk(value, out, local, (key + " chemical") if chemical else key)


def ingredient_text(node):
    """把 key 里的一项原料渲染成短文本"""
    buf = []
    walk(node, buf, 1)
    if not buf:
        return "?"
    return " + ".join(ref_text(kind, name, cnt) for kind, name, cnt in buf)


def merge_refs(refs):
    """合并同类引用并统计总数，保持首次出现顺序"""
    merged = OrderedDict()
    for kind, name, cnt in refs:
        key = (kind, name)
        merged[key] = merged.get(key, 0) + cnt
    return [(k[0], k[1], v) for k, v in merged.items()]


def ref_text(kind, name, cnt):
    """渲染单个引用：标签带 #，化学品额外标注"""
    if kind == "tag":
        text = f"`#{name}`"
    elif kind == "chemical":
        text = f"`{name}`（化学）"
    else:
        text = f"`{name}`"
    if cnt > 1:
        text += f"×{cnt}"
    return text


def refs_text(refs):
    if not refs:
        return "—"
    return " + ".join(ref_text(kind, name, cnt) for kind, name, cnt in refs)


def shaped_pattern(recipe):
    """有序配方的图案行 + 键位说明（键位数量按图案里出现的次数算）"""
    pattern = recipe.get("pattern")
    key = recipe.get("key")
    if not isinstance(pattern, list) or not isinstance(key, dict):
        return None, None
    counts = Counter(ch for row in pattern for ch in row if ch.strip())
    rows = " / ".join(f"`{row}`" for row in pattern)
    return rows, counts


# ==================== 解析一个配方文件 ====================

def parse_recipe(recipe):
    """把一个配方对象拆成 (输出引用, 输入引用, 图案行, 键位计数)"""
    outputs, inputs = [], []
    for key, value in recipe.items():
        if key in ("type", "conditions"):
            continue
        bucket = outputs if re.search(r"result|output", key, re.I) else inputs
        walk(value, bucket, 1, key)
    rows, counts = shaped_pattern(recipe)
    # 有序配方的材料以图案键位为准，避免 key 与 ingredients 重复列出
    if rows:
        keys = OrderedDict()
        for letter, node in (recipe.get("key") or {}).items():
            text = ingredient_text(node)
            n = counts.get(letter, 1)
            if n > 1:
                text += f"×{n}"
            keys[letter] = text
        return merge_refs(outputs), keys, rows, None
    return merge_refs(outputs), merge_refs(inputs), None, None


def iter_entries(path):
    """
    展开一个文件里的全部配方，产出 (内层配方, 条件 modid 列表)。

    生效条件有两个来源，都要认：
    1. `forge:conditional` 包裹时，写在外层条目的 conditions 上；
    2. 直接写在配方对象上的 `conditions`（Forge 会给每条配方调 processConditions）。
    """
    data = json.load(open(path, encoding="utf-8"))
    if data.get("type") == "forge:conditional":
        for entry in data.get("recipes", []):
            mods = [c.get("modid") for c in entry.get("conditions", []) if c.get("modid")]
            yield entry.get("recipe", {}), mods
    else:
        mods = [c.get("modid") for c in data.get("conditions", []) if c.get("modid")]
        yield data, mods


# ==================== 渲染 ====================

def render_recipe(recipe, mods):
    """渲染一条配方为 markdown 列表"""
    outputs, inputs, rows, _ = parse_recipe(recipe)
    rtype = recipe.get("type", "?")
    label = TYPE_LABELS.get(rtype, rtype)
    out_text = refs_text(outputs) if outputs else "无产物"

    lines = []
    if rows:
        lines.append(f"- **{out_text}**  ·  图案 {rows}")
        lines.append("  - 键位：" + "、".join(f"`{k}`={v}" for k, v in inputs.items()))
    else:
        lines.append(f"- **{out_text}**  ←  {refs_text(inputs)}")
    lines.append(f"  - 机器：{label} · `{rtype}`")
    if mods:
        lines.append("  - 前置：需要安装 " + "、".join(f"`{m}`" for m in mods))
    else:
        lines.append("  - 前置：无（始终生效）")
    return lines


def anchor_of(index, title):
    """与 GitHub 的标题锚点规则一致的近似实现"""
    text = f"{index}. {title}".lower()
    return re.sub(r"[^\w一-鿿-]", "", text.replace(" ", "-"))


def main():
    # 收集：目录 → [(文件名, 配方, mods)]；代码驱动的自定义配方单独归纳
    sections = OrderedDict()
    code_driven = []
    for folder in sorted(os.listdir(RECIPE_DIR)):
        full = os.path.join(RECIPE_DIR, folder)
        if not os.path.isdir(full):
            continue
        entries = []
        for name in sorted(os.listdir(full)):
            if not name.endswith(".json"):
                continue
            for recipe, mods in iter_entries(os.path.join(full, name)):
                entries.append((name, recipe, mods))
        if entries:
            sections[folder] = entries

    # 顶层 .json：本模组自定义配方的类型标记文件（行为写在代码里）
    for name in sorted(os.listdir(RECIPE_DIR)):
        path = os.path.join(RECIPE_DIR, name)
        if not name.endswith(".json"):
            continue
        for recipe, mods in iter_entries(path):
            code_driven.append((name, recipe, mods))

    # 按 SECTION_NAMES 的顺序排版，本模组放最前，其余按中文名排序
    ordered = OrderedDict()
    for folder in SECTION_NAMES:
        if folder in sections:
            ordered[folder] = sections.pop(folder)
    for folder in sorted(sections):
        ordered[folder] = sections[folder]
    sections = ordered

    total_files = sum(len({n for n, _, _ in v}) for v in sections.values()) + len(code_driven)
    total_recipes = sum(len(v) for v in sections.values()) + len(code_driven)
    section_titles = {f: SECTION_NAMES.get(f, f) for f in sections}
    custom_title = "本模组自定义配方"

    out = []
    out.append("# Alltheimbaium 配方总表")
    out.append("")
    out.append("> **本文件由 `tools/gen_recipe_docs.py` 自动生成，请勿手工修改。**")
    out.append("> 配方有增删改后重新执行 `python tools/gen_recipe_docs.py` 即可同步。")
    out.append("")
    out.append(f"- 共 **{total_files}** 个配方文件、**{total_recipes}** 条配方")
    out.append("- 物品一律写**注册名**；数量省略表示 1")
    out.append("- 物品流向一律写作：**产物 ← 材料**")
    out.append("- 带「前置」的条目被 `forge:conditional` 包裹，**未安装对应 MOD 时该配方根本不存在**")
    out.append("")

    # 总览
    out.append("## 总览")
    out.append("")
    out.append("| 分类 | 目录 | 配方数 | 涉及 MOD |")
    out.append("|------|------|-------:|----------|")
    for folder, entries in sections.items():
        mods = sorted({m for _, _, ms in entries for m in ms})
        mods_text = "、".join(f"`{m}`" for m in mods) or "—"
        out.append(f"| {section_titles[folder]} | `recipes/{folder}/` | {len(entries)} | {mods_text} |")
    out.append(f"| {custom_title} | `recipes/*.json` | {len(code_driven)} | — |")
    out.append("")

    # 目录
    out.append("## 目录")
    out.append("")
    for i, folder in enumerate(sections, 1):
        title = section_titles[folder]
        out.append(f"{i}. [{title}（{len(sections[folder])} 条）](#{anchor_of(i, title)})")
    last = len(sections) + 1
    out.append(f"{last}. [{custom_title}（{len(code_driven)} 条）](#{anchor_of(last, custom_title)})")
    out.append("")

    # 各分类正文
    for i, folder in enumerate(sections, 1):
        title = section_titles[folder]
        out.append(f"## {i}. {title}")
        out.append("")
        out.append(f"目录：`src/main/resources/data/alltheimbaium/recipes/{folder}/`")
        out.append("")
        current_file = None
        for name, recipe, mods in sections[folder]:
            if name != current_file:
                if current_file is not None:
                    out.append("")
                current_file = name
                out.append(f"### `{name}`")
                out.append("")
            out.extend(render_recipe(recipe, mods))
        out.append("")

    # 自定义配方类型
    out.append(f"## {last}. {custom_title}")
    out.append("")
    out.append("这几种配方**不是数据包配方**，而是由 Java 代码动态判定——JSON 只是一份类型标记文件，")
    out.append("配方书与 JEI 里都不会出现，改行为要改代码：")
    out.append("")
    for name, recipe, _ in code_driven:
        rtype = recipe.get("type", "?")
        out.append(f"### `{name}`")
        out.append("")
        out.append(f"- 配方类型：`{rtype}`")
        out.append(f"- 说明：{CODE_DRIVEN.get(rtype, '（未登记的代码驱动配方）')}")
        out.append("")

    with open(OUT_FILE, "w", encoding="utf-8", newline="\n") as fp:
        fp.write("\n".join(out).rstrip() + "\n")
    print(f"已生成 {os.path.normpath(OUT_FILE)}")
    print(f"  分类 {len(sections) + 1} 个，配方文件 {total_files} 个，配方 {total_recipes} 条")


if __name__ == "__main__":
    main()
