# -*- coding: utf-8 -*-
"""
向全部语言文件补齐两个「工具提示」键（一次性脚本）。

背景：`MobFarmItem` / `ResourceFarmItem` 的 tooltip 与两个农场的帮助卡原先在代码里
硬编码了中文的「/秒」与「、」，其它语言会原样显示中文。改为走语言键后需要全语言补齐。

  item.alltheimbaium.tooltip.per_second   每秒单位（如 /s、/秒、/с）
  item.alltheimbaium.tooltip.separator    列表分隔符（中文用「、」，其余用「, 」）

per_second 的单位直接从各语言既有的 `screen.alltheimbaium.mob_farm.rate`（"Rate: %s/s"）
里推导，保证与其它界面显示的速率单位一致。

    python tools/add_lang_keys.py
"""
import io
import json
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
LANG_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "alltheimbaium", "lang")

RATE_KEY = "screen.alltheimbaium.mob_farm.rate"
PER_SECOND = "item.alltheimbaium.tooltip.per_second"
SEPARATOR = "item.alltheimbaium.tooltip.separator"

# 顿号系语言（中/日）用「、」作列表分隔符，其余语言用「, 」
IDEOGRAPHIC_COMMA = {"zh_cn", "zh_tw", "ja_jp", "en_ud"}


def unit_of(value):
    """从 "Rate: %s/s" 之类的文案里取出 %s 之后的单位"""
    tail = re.sub(r"§.", "", value).strip()
    m = re.search(r"%s(.*)$", tail)
    return m.group(1).strip() if m else "/s"


def main():
    changed = 0
    for fn in sorted(os.listdir(LANG_DIR)):
        if not fn.endswith(".json"):
            continue
        name = fn[:-5]
        path = os.path.join(LANG_DIR, fn)
        data = json.loads(io.open(path, encoding="utf-8").read())

        per_second = unit_of(data.get(RATE_KEY, "%s/s"))
        if name == "en_ud":  # 倒置英语：单位也要倒过来
            per_second = "\\s"
        separator = "、" if name in IDEOGRAPHIC_COMMA else ", "
        if name == "en_ud":
            separator = "' "  # 逗号在 en_ud 里映射成单引号

        # 先摘掉旧位置（脚本可重复执行），再插到 item.* 组末尾（第一个 itemGroup.* 之前）
        # —— 保持各文件键序一致，且不让工具提示键跑到 block.* 前面
        data = {k: v for k, v in data.items() if k not in (PER_SECOND, SEPARATOR)}
        out = {}
        inserted = False
        for k, v in data.items():
            if not inserted and k.startswith("itemGroup."):
                out[PER_SECOND] = per_second
                out[SEPARATOR] = separator
                inserted = True
            out[k] = v
        if not inserted:
            out[PER_SECOND] = per_second
            out[SEPARATOR] = separator

        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(out, ensure_ascii=False, indent=2) + "\n")
        changed += 1
    print("已补齐 %d 个语言文件（%s / %s）" % (changed, PER_SECOND, SEPARATOR))


if __name__ == "__main__":
    main()
