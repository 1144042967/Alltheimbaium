# -*- coding: utf-8 -*-
"""扫描 Java 源码里字符串字面量中的 CJK 字符（硬编码的界面文本，译文覆盖不到）。

只作提示，不自动修改。注释行会被跳过。

    python tools/scan_hardcoded_text.py
"""
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "java")
LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')
CJK = re.compile(u'[　-〿一-鿿＀-￯]')


def main():
    hits = 0
    for root, _dirs, files in os.walk(ROOT):
        for fn in sorted(files):
            if not fn.endswith(".java"):
                continue
            path = os.path.join(root, fn)
            for i, line in enumerate(open(path, encoding="utf-8"), 1):
                s = line.strip()
                if s.startswith("//") or s.startswith("*") or s.startswith("/*"):
                    continue
                for m in LITERAL.finditer(line):
                    if CJK.search(m.group(1)):
                        rel = os.path.relpath(path, ROOT).replace(os.sep, "/")
                        print("%s:%d  %s" % (rel, i, s[:110]))
                        hits += 1
                        break
    print("共 %d 处" % hits)


if __name__ == "__main__":
    main()
