# -*- coding: utf-8 -*-
"""
ATI 零刻熔炉占位资源生成器（可重复执行微调）。
- 方块纹理: instant_furnace_top/side/btm (16x16)
- GUI 底图: instant_furnace_gui.png (176x233)
槽位/能量条/按钮坐标与 Java 侧 Menu/Screen 常量保持一致；若改 Java 坐标请同步本文件再重跑。
"""
import os

from PIL import Image, ImageDraw

RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "alltheimbaium")


def px(x, y):
    return (x, y, x + 1, y + 1)


# ============================================================ 方块纹理 16x16
def make_block_textures():
    folder = os.path.join(RES, "textures", "block")
    os.makedirs(folder, exist_ok=True)

    # ---- 顶面：深蓝面板 + 边框 + 中央方形检修口 + 角螺栓 ----
    img = Image.new("RGB", (16, 16))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(59, 74, 99))          # 基色
    d.rectangle((0, 0, 15, 1), fill=(31, 41, 55))           # 上缘阴影
    d.rectangle((0, 14, 15, 15), fill=(88, 108, 140))       # 下缘高光
    d.rectangle((0, 0, 1, 15), fill=(31, 41, 55))           # 左缘阴影
    d.rectangle((14, 0, 15, 15), fill=(88, 108, 140))       # 右缘高光
    d.rectangle((5, 5, 10, 10), outline=(20, 27, 38), width=1)  # 中央检修口外框
    d.rectangle((6, 6, 9, 9), fill=(96, 114, 146))          # 检修口
    d.rectangle((6, 6, 9, 7), fill=(150, 170, 205))         # 检修口高光
    for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):      # 角螺栓
        d.rectangle((cx - 1, cy - 1, cx, cy), fill=(150, 170, 205))
    img.save(os.path.join(folder, "instant_furnace_top.png"))

    # ---- 侧面：蓝色外壳 + 中央"炉门" + 熔炉热口 ----
    img = Image.new("RGB", (16, 16))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(59, 74, 99))
    d.rectangle((0, 0, 15, 1), fill=(31, 41, 55))
    d.rectangle((0, 14, 15, 15), fill=(88, 108, 140))
    d.rectangle((0, 0, 1, 15), fill=(31, 41, 55))
    d.rectangle((14, 0, 15, 15), fill=(88, 108, 140))
    # 门框（略凹）
    d.rectangle((3, 2, 12, 13), outline=(20, 27, 38), width=1)
    d.rectangle((4, 3, 11, 12), fill=(44, 56, 77))
    # 中央热口（橙红色，暗示即时熔炼）
    d.rectangle((6, 5, 9, 10), fill=(224, 112, 42))
    d.rectangle((6, 5, 9, 6), fill=(255, 178, 102))
    d.rectangle((7, 7, 8, 8), fill=(255, 220, 160))         # 高光
    for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.rectangle((cx - 1, cy - 1, cx, cy), fill=(150, 170, 205))
    img.save(os.path.join(folder, "instant_furnace_side.png"))

    # ---- 底面：深色底 + 边框 + 角螺栓 ----
    img = Image.new("RGB", (16, 16))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(48, 58, 76))
    d.rectangle((0, 0, 15, 1), fill=(35, 43, 58))
    d.rectangle((0, 14, 15, 15), fill=(72, 88, 114))
    d.rectangle((0, 0, 1, 15), fill=(35, 43, 58))
    d.rectangle((14, 0, 15, 15), fill=(72, 88, 114))
    for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.rectangle((cx - 1, cy - 1, cx, cy), fill=(120, 140, 170))
    img.save(os.path.join(folder, "instant_furnace_btm.png"))


# ============================================================ GUI 底图 176x233
# 与 Java 侧坐标保持一致的常量
IMAGE_W = 176
IMAGE_H = 233
INPUT_ROW_YS = [26, 44]      # 输入两行
OUTPUT_ROW_YS = [90, 108]    # 输出两行
PLAYER_ROW_YS = [151, 169, 187, 209]  # 玩家物品栏三行 + 快捷栏
SLOT_X = [8 + c * 18 for c in range(9)]   # 每行 9 格


def _slot_box(d, x, y):
    """在 (x,y) 处画一格"凹陷"槽位框：外 18x18 边框、内 16x16 填充。"""
    d.rectangle((x - 1, y - 1, x + 17, y + 17), fill=(85, 85, 85))
    d.rectangle((x, y, x + 15, y + 15), fill=(110, 110, 110))
    # 左下高光 / 右上阴影，形成凹陷观感
    d.rectangle((x, y + 15, x + 15, y + 16), fill=(139, 139, 139))
    d.rectangle((x + 15, y, x + 16, y + 16), fill=(139, 139, 139))
    d.rectangle((x, y, x + 15, y + 1), fill=(70, 70, 70))
    d.rectangle((x, y, x + 1, y + 15), fill=(70, 70, 70))


def make_gui():
    folder = os.path.join(RES, "textures", "gui")
    os.makedirs(folder, exist_ok=True)

    # 纵向渐变背景
    img = Image.new("RGB", (IMAGE_W, IMAGE_H))
    d = ImageDraw.Draw(img)
    for yy in range(IMAGE_H):
        t = yy / max(1, IMAGE_H - 1)
        base = [int(198 - 30 * t), int(198 - 30 * t), int(198 - 30 * t)]
        d.rectangle((0, yy, IMAGE_W, yy), fill=tuple(base))

    # 机器区（0..~130）淡淡的暖色面板，与下方物品栏略区分
    d.rectangle((0, 0, IMAGE_W - 1, 129), fill=(214, 211, 205))

    # 输入区（暖）与输出区（冷）两条色带，便于一眼分辨上下两块
    d.rectangle((0, 25, IMAGE_W - 1, 61), fill=(222, 214, 196))
    d.rectangle((0, 89, IMAGE_W - 1, 125), fill=(204, 211, 224))

    # FE 能量条轨槽（代码会在其上覆盖深色轨道与填充色，这里只画提示外框）
    d.rectangle((7, 14, 169, 22), outline=(31, 41, 55), width=1)

    # 机器输入/输出槽位
    for y in INPUT_ROW_YS:
        for x in SLOT_X:
            _slot_box(d, x, y)
    for y in OUTPUT_ROW_YS:
        for x in SLOT_X:
            _slot_box(d, x, y)

    # 玩家物品栏/快捷栏槽位
    for y in PLAYER_ROW_YS:
        for x in SLOT_X:
            _slot_box(d, x, y)

    # 分隔线（机器区与物品栏之间）
    d.rectangle((0, 130, IMAGE_W - 1, 130), fill=(120, 120, 120))

    img.save(os.path.join(folder, "instant_furnace_gui.png"))


if __name__ == "__main__":
    make_block_textures()
    make_gui()
    print("已生成 instant_furnace 占位纹理与 GUI 底图 ->", os.path.normpath(os.path.join(RES, "textures")))
