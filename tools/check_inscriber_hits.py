# -*- coding: utf-8 -*-
"""
零刻压印器"输出容量"回归检查。

压板模式一次会把某份原料命中的**全部**压板配方各产一份；一旦命中数超过
`InstantInscriberEntity.OUTPUT_MAX_TYPES`，`canFitOutputTypes` 永远为 false，
该原料就再也不出产、也不报错——表现为"机器坏了"。

本脚本扫一个整合包（或其 mods 目录）里的全部 `ae2:inscriber` 配方，解析
`c:` / 原版标签，算出每种可作为 middle 的物品能命中多少条 inscribe 配方，
并与代码里的上限比对。

    python tools/check_inscriber_hits.py <整合包目录或 mods 目录> [--limit 18]

命中数 >= 上限即判失败（等于上限时输出区必须一开始就全空，等于踩线，同样算失败）。
退出码非 0 表示需要抬高 OUTPUT_MAX_TYPES。
"""
import glob
import io
import json
import os
import re
import sys
import zipfile
from collections import defaultdict

# NeoForge 自带的 c: 默认标签所在位置（不同实例路径可能不同，找到几个算几个）
NEOFORGE_GLOBS = [
    os.path.expanduser("~/.minecraft/libraries/net/neoforged/neoforge/*/neoforge-*-universal.jar"),
    "C:/Software/PCL/.minecraft/libraries/net/neoforged/neoforge/*/neoforge-*-universal.jar",
]


def source_dir():
    """读取 InstantInscriberEntity 源码目录"""
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    return os.path.join(root, "src", "main", "java", "cn", "sd", "jrz", "alltheimbaium")


def code_limit():
    """从源码里取 OUTPUT_MAX_TYPES，避免脚本与代码各写一份"""
    src = io.open(os.path.join(source_dir(), "entity", "InstantInscriberEntity.java"),
                  "r", encoding="utf-8").read()
    m = re.search(r"OUTPUT_MAX_TYPES\s*=\s*(\d+)", src)
    if not m:
        raise SystemExit("在 InstantInscriberEntity.java 里找不到 OUTPUT_MAX_TYPES")
    return int(m.group(1))


def collect(pack_dir):
    """返回 (配方列表, 标签表)"""
    # 传整合包目录时 jar 在 mods/ 下，传 mods 目录时就在本层；两种都扫，避免漏掉
    jars = sorted(glob.glob(os.path.join(pack_dir, "*.jar")))
    jars += sorted(glob.glob(os.path.join(pack_dir, "mods", "*.jar")))
    if not jars:
        raise SystemExit("%s 下没有找到任何 jar" % pack_dir)
    for pattern in NEOFORGE_GLOBS:
        jars += sorted(glob.glob(pattern))

    recipes = []            # (middle 定义, 产物 id)
    tags = defaultdict(list)
    for jar in jars:
        try:
            z = zipfile.ZipFile(jar)
        except Exception:
            continue
        for n in z.namelist():
            if not n.endswith(".json"):
                continue
            if "/recipe" in n:
                try:
                    d = json.loads(z.read(n).decode("utf-8"))
                except Exception:
                    continue
                if isinstance(d, dict) and d.get("type") == "ae2:inscriber" \
                        and d.get("mode") == "inscribe":
                    mid = (d.get("ingredients") or {}).get("middle")
                    res = (d.get("result") or {}).get("id")
                    if mid and res:
                        recipes.append((mid, res))
            elif "/tags/item/" in n:
                # 路径是 data/<命名空间>/tags/item/<标签名>.json，键必须带上命名空间，
                # 否则配方里的 "c:storage_blocks/iron" 找不到 "storage_blocks/iron"
                m = re.match(r".*data/([^/]+)/tags/item/(.+)\.json$", n)
                if not m:
                    continue
                tag = "%s:%s" % (m.group(1), m.group(2))
                try:
                    d = json.loads(z.read(n).decode("utf-8"))
                except Exception:
                    continue
                for v in (d.get("values") or []):
                    if isinstance(v, str):
                        tags[tag].append(v)
                    elif isinstance(v, dict) and v.get("required", True):
                        tags[tag].append(v["id"])
    return recipes, tags


def resolve(tags, tag, seen=None):
    """展开标签（含嵌套 # 引用）"""
    seen = seen or set()
    if tag in seen:
        return set()
    seen.add(tag)
    out = set()
    for v in tags.get(tag, []):
        if v.startswith("#"):
            out |= resolve(tags, v[1:], seen)
        else:
            out.add(v)
    return out


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not args:
        raise SystemExit(__doc__)
    limit = code_limit()
    for a in sys.argv[1:]:
        if a.startswith("--limit="):
            limit = int(a.split("=", 1)[1])

    recipes, tags = collect(args[0])
    hits = defaultdict(list)
    for mid, res in recipes:
        items = {mid["item"]} if "item" in mid else resolve(tags, mid.get("tag", ""))
        for item in items:
            hits[item].append(res)

    ranked = sorted(hits.items(), key=lambda kv: (-len(kv[1]), kv[0]))
    print("inscribe 配方 %d 条，可作为 middle 的物品 %d 种，代码上限 %d 种\n"
          % (len(recipes), len(hits), limit))
    for item, outs in ranked[:10]:
        flag = "  <== 超出上限" if len(outs) >= limit else ""
        print("  %-45s %2d%s" % (item, len(outs), flag))

    worst = ranked[0] if ranked else ("-", [])
    print("\n最高：%s → %d 种产物" % (worst[0], len(worst[1])))
    if len(worst[1]) >= limit:
        print("失败：命中数 >= OUTPUT_MAX_TYPES(%d)，该原料会静默停产。" % limit)
        return 1
    print("通过：全部原料的命中数都低于上限。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
