# -*- coding: utf-8 -*-
"""
ATI 语言文件一致性检查。

以 en_us.json 为基准，逐个检查其余语言文件：

**错误（导致退出码非 0）**
  1. 键集与顺序完全一致（不多、不少、不重排）
  2. `%s` / `%1$s` 占位符的完整有序序列逐键一致
  3. UTF-8 无 BOM、以 `\n` 结尾

**警告（不影响退出码）**
  4. `§` 颜色码的完整有序序列逐键一致 —— 这一条只作提示：译文改写语序时着色位置
     本就会移动（zh_cn 就有 9 处），不一定是缺陷。加 `--strict` 可把它升级为错误。

     python tools/check_lang.py            # 全量检查
     python tools/check_lang.py ro_ro      # 只检查指定语言
     python tools/check_lang.py --strict   # 颜色码差异也判为失败
"""
import io
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
LANG_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "alltheimbaium", "lang")
BASE = "en_us"

PLACEHOLDER = re.compile(r"%[0-9]*\$?[sd]|%%")
COLOR = re.compile(r"§.")


def load(path):
    raw = io.open(path, "rb").read()
    if raw.startswith(b"\xef\xbb\xbf"):
        raise ValueError("文件带 UTF-8 BOM")
    if not raw.endswith(b"\n"):
        raise ValueError("文件末尾缺少换行")
    return json.loads(raw.decode("utf-8"))


def check(base, name, path, errors, warnings):
    try:
        data = load(path)
    except Exception as e:
        errors.append("%s: 无法解析 —— %s" % (name, e))
        return
    if list(data) != list(base):
        missing = [k for k in base if k not in data]
        extra = [k for k in data if k not in base]
        if missing:
            errors.append("%s: 缺少 %d 个键（如 %s）" % (name, len(missing), missing[0]))
        if extra:
            errors.append("%s: 多出 %d 个键（如 %s）" % (name, len(extra), extra[0]))
        if not missing and not extra:
            errors.append("%s: 键集相同但顺序与 %s 不一致" % (name, BASE))
        return
    bad_ph = bad_color = 0
    color_keys = []
    for k, v in data.items():
        if PLACEHOLDER.findall(v) != PLACEHOLDER.findall(base[k]):
            bad_ph += 1
            if bad_ph <= 3:
                errors.append("%s: 占位符不符 %s  << %s" % (name, k, base[k]))
        if COLOR.findall(v) != COLOR.findall(base[k]):
            bad_color += 1
            color_keys.append(k)
    if bad_ph:
        errors.append("%s: 共 %d 个键占位符不符" % (name, bad_ph))
    if bad_color:
        warnings.append("%s: %d 个键颜色码序列与 %s 不同（%s）"
                        % (name, bad_color, BASE, "、".join(color_keys[:3])))


def main():
    args = [a for a in sys.argv[1:] if a != "--strict"]
    strict = "--strict" in sys.argv
    base = load(os.path.join(LANG_DIR, BASE + ".json"))
    names = args or sorted(
        f[:-5] for f in os.listdir(LANG_DIR) if f.endswith(".json") and f[:-5] != BASE
    )
    errors, warnings = [], []
    for name in names:
        path = os.path.join(LANG_DIR, name + ".json")
        if not os.path.exists(path):
            errors.append("%s: 文件不存在" % name)
            continue
        check(base, name, path, errors, warnings)
    if warnings:
        print("警告（%d 条，不影响判定）：" % len(warnings))
        for w in warnings:
            print("  -", w)
    if errors or (strict and warnings):
        print("检查未通过：")
        for p in errors + (warnings if strict else []):
            print("  -", p)
        return 1
    print("检查通过：%s + %d 种语言，键集/顺序/占位符全部一致" % (BASE + ".json", len(names)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
