# -*- coding: utf-8 -*-
"""
ATI 零刻压印器占位资源生成器（可重复执行微调）。
方块纹理 top/side/btm + GUI 底图 176x233。
槽位/能量条/模式按钮坐标与 Java 侧 Menu/Screen 常量保持一致；改 Java 坐标请同步重跑。
"""
import os

from PIL import Image, ImageDraw

RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "alltheimbaium")


def make_block_textures():
    folder = os.path.join(RES, "textures", "block")
    os.makedirs(folder, exist_ok=True)

    # ---- 顶面：蓝黑面板 + 边框 + 中央方形（压印台） + 角螺栓 ----
    img = Image.new("RGB", (16, 16))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(54, 62, 88))
    d.rectangle((0, 0, 15, 1), fill=(28, 34, 50))
    d.rectangle((0, 14, 15, 15), fill=(86, 98, 128))
    d.rectangle((0, 0, 1, 15), fill=(28, 34, 50))
    d.rectangle((14, 0, 15, 15), fill=(86, 98, 128))
    d.rectangle((5, 5, 10, 10), outline=(20, 26, 38), width=1)
    d.rectangle((6, 6, 9, 9), fill=(104, 122, 152))
    d.rectangle((6, 6, 9, 7), fill=(150, 172, 210))
    # 中央"压板"印记（金色横条）
    d.rectangle((6, 8, 9, 9), fill=(224, 168, 60))
    for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.rectangle((cx - 1, cy - 1, cx, cy), fill=(150, 172, 210))
    img.save(os.path.join(folder, "instant_inscriber_top.png"))

    # ---- 侧面：外壳 + 两个水平"压印板"（暗示上下压板） ----
    img = Image.new("RGB", (16, 16))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(54, 62, 88))
    d.rectangle((0, 0, 15, 1), fill=(28, 34, 50))
    d.rectangle((0, 14, 15, 15), fill=(86, 98, 128))
    d.rectangle((0, 0, 1, 15), fill=(28, 34, 50))
    d.rectangle((14, 0, 15, 15), fill=(86, 98, 128))
    d.rectangle((3, 2, 12, 13), outline=(20, 26, 38), width=1)
    d.rectangle((4, 3, 11, 12), fill=(40, 48, 72))
    # 上下两块金色压印板 + 中间缝隙高光
    d.rectangle((6, 5, 9, 7), fill=(224, 168, 60))
    d.rectangle((6, 9, 9, 11), fill=(224, 168, 60))
    d.rectangle((6, 7, 9, 9), fill=(120, 100, 50))
    for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.rectangle((cx - 1, cy - 1, cx, cy), fill=(150, 172, 210))
    img.save(os.path.join(folder, "instant_inscriber_side.png"))

    # ---- 底面 ----
    img = Image.new("RGB", (16, 16))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), fill=(44, 50, 70))
    d.rectangle((0, 0, 15, 1), fill=(32, 38, 54))
    d.rectangle((0, 14, 15, 15), fill=(70, 82, 110))
    d.rectangle((0, 0, 1, 15), fill=(32, 38, 54))
    d.rectangle((14, 0, 15, 15), fill=(70, 82, 110))
    for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.rectangle((cx - 1, cy - 1, cx, cy), fill=(120, 138, 168))
    img.save(os.path.join(folder, "instant_inscriber_btm.png"))


# ---- GUI 176x233 ----
IMAGE_W = 176
IMAGE_H = 233
INPUT_ROW_YS = [26, 44]   # 两行输入
OUTPUT_ROW_YS = [100]     # 一行输出(9)
PLAYER_ROW_YS = [151, 169, 187, 209]
SLOT_X = [8 + c * 18 for c in range(9)]


def _slot_box(d, x, y):
    d.rectangle((x - 1, y - 1, x + 17, y + 17), fill=(85, 85, 85))
    d.rectangle((x, y, x + 15, y + 15), fill=(110, 110, 110))
    d.rectangle((x, y + 15, x + 15, y + 16), fill=(139, 139, 139))
    d.rectangle((x + 15, y, x + 16, y + 16), fill=(139, 139, 139))
    d.rectangle((x, y, x + 15, y + 1), fill=(70, 70, 70))
    d.rectangle((x, y, x + 1, y + 15), fill=(70, 70, 70))


def make_gui():
    folder = os.path.join(RES, "textures", "gui")
    os.makedirs(folder, exist_ok=True)

    img = Image.new("RGB", (IMAGE_W, IMAGE_H))
    d = ImageDraw.Draw(img)
    for yy in range(IMAGE_H):
        t = yy / max(1, IMAGE_H - 1)
        base = [int(198 - 30 * t), int(198 - 30 * t), int(198 - 30 * t)]
        d.rectangle((0, yy, IMAGE_W, yy), fill=tuple(base))

    d.rectangle((0, 0, IMAGE_W - 1, 129), fill=(214, 211, 205))
    # 输入区（暖）两行带
    d.rectangle((0, 25, IMAGE_W - 1, 61), fill=(222, 214, 196))
    # 输出区（冷）一行带
    d.rectangle((0, 99, IMAGE_W - 1, 117), fill=(204, 211, 224))

    # FE 能量条提示外框（代码在上层画轨道/填充）
    d.rectangle((7, 15, 169, 23), outline=(31, 41, 55), width=1)

    for y in INPUT_ROW_YS:
        for x in SLOT_X:
            _slot_box(d, x, y)
    for y in OUTPUT_ROW_YS:
        for x in SLOT_X:
            _slot_box(d, x, y)
    for y in PLAYER_ROW_YS:
        for x in SLOT_X:
            _slot_box(d, x, y)

    # 分隔线（机器区与物品栏）
    d.rectangle((0, 130, IMAGE_W - 1, 130), fill=(120, 120, 120))
    img.save(os.path.join(folder, "instant_inscriber_gui.png"))


if __name__ == "__main__":
    make_block_textures()
    make_gui()
    print("已生成 instant_inscriber 占位纹理与 GUI 底图 ->", os.path.normpath(os.path.join(RES, "textures")))
