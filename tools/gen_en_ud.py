# -*- coding: utf-8 -*-
"""
en_ud（倒置英语）生成器。

把 en_us.json 的每条文本按 Minecraft 官方的倒置字符表逐字符映射，生成 en_ud.json。
`§` 颜色码与 `%s` / `%1$s` / `%%` 原样保留（占位符里的数字不参与映射）。

映射表已用 auto-resource 既有的 en_ud.json 逐键反向校验通过。

    python tools/gen_en_ud.py
"""
import io
import json
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
LANG_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "alltheimbaium", "lang")

FLIP = {}
for a, b in zip("abcdefghijklmnopqrstuvwxyz", "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz"):
    FLIP[a] = b
for a, b in zip("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "∀ᒰƆᗡƎℲ⅁HIſʞ˥WNOԀÒᴚS⊥∩ΛMX⅄Z"):
    FLIP[a] = b
for a, b in zip("0123456789", "0ƖᄅƐㄣϛ9ㄥ86"):
    FLIP[a] = b
FLIP.update({'.': '˙', ',': "'", '?': '¿', '!': '¡', "'": ',',
             '(': ')', ')': '(', '[': ']', ']': '[', '{': '}', '}': '{',
             '<': '>', '>': '<', '&': '⅋', '_': '‾', '/': '\\'})
# 双引号不翻转（与 auto-resource 既有的 en_ud.json 一致）

# § 颜色码与 %s / %1$s / %% 原样保留
TOKEN = re.compile(r'(§.|%[0-9]*\$?[sdf]|%%)')


def flip(text):
    """逐字符倒置；占位符与颜色码整段跳过"""
    out = []
    for i, part in enumerate(TOKEN.split(text)):
        # re.split 带捕获组时，奇数下标是分隔符本身（即要原样保留的 token）
        out.append(part if i % 2 else "".join(FLIP.get(c, c) for c in part))
    return "".join(out)


def main():
    src = io.open(os.path.join(LANG_DIR, "en_us.json"), encoding="utf-8")
    en_us = json.load(src)
    out = {k: flip(v) for k, v in en_us.items()}
    dst = os.path.join(LANG_DIR, "en_ud.json")
    with io.open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(out, ensure_ascii=False, indent=2) + "\n")
    print("已生成 %s（%d 键）" % (dst, len(out)))


if __name__ == "__main__":
    main()
