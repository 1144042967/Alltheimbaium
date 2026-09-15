# -*- coding: utf-8 -*-
"""
把 3 个分支里 6 处玩家可见的硬编码中文改成语言键（一次性脚本，重复执行会跳过已改好的）。

  MobFarmItem / ResourceFarmItem 的产物列表："/秒"  -> item.alltheimbaium.tooltip.per_second
  两个农场 Screen 的帮助卡分隔符："、"            -> item.alltheimbaium.tooltip.separator

     python tools/patch_hardcoded.py
"""
import io
import os

PROJECTS = ["alltheimbaium-1.20.1", "alltheimbaium-1.21.1", "alltheimbaium-26.1.2"]
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")

RATE_OLD_TPL = ('products.add("§e" + speed.stripTrailingZeros().toPlainString() + "/秒§7 " + %s.getHoverName().getString());')
RATE_NEW_TPL = ('products.add("§e" + speed.stripTrailingZeros().toPlainString()\n'
                '                        + Component.translatable("item.alltheimbaium.tooltip.per_second").getString()\n'
                '                        + "§7 " + %s.getHoverName().getString());')

SEP_OLD = 'text.append(Component.literal("、").withStyle(ChatFormatting.GRAY));'
SEP_NEW = 'text.append(Component.translatable("item.alltheimbaium.tooltip.separator").withStyle(ChatFormatting.GRAY));'


def patch(path, old, new):
    text = io.open(path, encoding="utf-8").read()
    n = text.count(old)
    if n:
        io.open(path, "w", encoding="utf-8", newline="\n").write(text.replace(old, new))
    return n


def main():
    for proj in PROJECTS:
        base = os.path.join(ROOT, proj, "src", "main", "java", "cn", "sd", "jrz", "alltheimbaium")
        total = 0
        for item, var in (("MobFarmItem", "rowStack"), ("ResourceFarmItem", "rowStack")):
            total += patch(os.path.join(base, "item", item + ".java"),
                           RATE_OLD_TPL % var, RATE_NEW_TPL % var)
        for item, var in (("MobFarmItem", "row.item()"), ("ResourceFarmItem", "row.item()")):
            total += patch(os.path.join(base, "item", item + ".java"),
                           RATE_OLD_TPL % var, RATE_NEW_TPL % var)
        for screen in ("MobFarmScreen", "ResourceFarmScreen"):
            total += patch(os.path.join(base, "gui", screen + ".java"), SEP_OLD, SEP_NEW)
        print("%s：替换 %d 处" % (proj, total))


if __name__ == "__main__":
    main()
