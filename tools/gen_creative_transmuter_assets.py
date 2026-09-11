# -*- coding: utf-8 -*-
"""
ATI 创造物品质变器占位资源生成器（可重复执行微调）。
方块纹理 top/side/btm + GUI 底图 176x166。

GUI 走的是本模组通行的**原版容器外观**：浅灰底 + 圆角斜面边框 + 三面倒角槽位。
边框与槽位的像素规则是从 eternal_sword_gui.png 逐像素逆出来的，并用它回放校验过
（边框区域差异像素数为 0），所以这里直接内联规则，生成的图与其余容器界面同源。

槽位/箭头坐标与 Java 侧 Menu/Screen 常量保持一致；改 Java 坐标请同步重跑：
    CreativeTransmuterMenu：输入栏 30 + 列*18 / 17 + 行*18，输出栏 (124, 35)，
                            玩家背包 (8 + 列*18, 84 / 102 / 120)，快捷栏 y=142
    CreativeTransmuterScreen：imageWidth 176 / imageHeight 166
"""
import os

from PIL import Image

RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "alltheimbaium")

# ==================== 机器贴图调色板 ====================
# 与 instant_inscriber 等机器同一套机身色，仅把功能色换成淡紫（EPIC 品级色）
G = (86, 98, 128, 255)      # 顶面机身
B = (54, 62, 88, 255)       # 中间调
A = (20, 26, 38, 255)       # 内部深色
C = (28, 34, 50, 255)       # 侧面外圈
D = (40, 48, 72, 255)       # 侧面机身
E = (44, 50, 70, 255)       # 底面机身
F = (32, 38, 54, 255)       # 底面外圈
L = (150, 172, 210, 255)    # 通用亮蓝：角标 / 描边
LM = (120, 138, 168, 255)   # 底面角标蓝
P = (184, 127, 214, 255)    # 本机功能色：淡紫

# 四角亮蓝角标的位置（2×2），机器贴图的统一识别标记
CORNERS = ((1, 2), (13, 14))


def _blank(color):
    return Image.new("RGBA", (16, 16), color)


def make_block_textures():
    folder = os.path.join(RES, "textures", "block")
    os.makedirs(folder, exist_ok=True)

    # ---- 顶面：亮机身 + 角标 + 中央亮蓝框内嵌淡紫核心（暗示 9 格输入） ----
    img = _blank(G)
    px = img.load()
    for y in (2, 3, 12, 13):
        for x in range(16):
            px[x, y] = L if x in (2, 3, 12, 13) else B
    for y in range(4, 12):
        px[0, y] = px[1, y] = G
        px[2, y] = px[3, y] = B
        px[12, y] = px[13, y] = B
        px[14, y] = px[15, y] = G
        for x in range(4, 12):
            px[x, y] = A
    for x in range(5, 11):
        px[x, 5] = L
        px[x, 10] = L
    for y in range(5, 11):
        px[5, y] = L
        px[10, y] = L
    for y in range(6, 10):
        for x in range(6, 10):
            px[x, y] = P
    img.save(os.path.join(folder, "creative_transmuter_top.png"))

    # ---- 侧面：深色外壳 + 角标 + 中央淡紫方块 ----
    img = _blank(C)
    px = img.load()
    for y in (1, 2, 13, 14):
        px[0, y] = C
        px[1, y] = px[2, y] = L
        px[13, y] = px[14, y] = L
        px[15, y] = C
        for x in range(3, 13):
            px[x, y] = A if y in (2, 13) else B
    for y in range(3, 13):
        px[0, y] = px[1, y] = C
        px[2, y] = B
        px[3, y] = A
        px[12, y] = A
        px[13, y] = B
        px[14, y] = px[15, y] = C
        for x in range(4, 12):
            px[x, y] = D
    for y in range(5, 12):
        for x in range(6, 10):
            px[x, y] = P
    img.save(os.path.join(folder, "creative_transmuter_side.png"))

    # ---- 底面：统一的深色底座 + 角标 ----
    img = _blank(F)
    px = img.load()
    for y in (1, 2, 13, 14):
        px[0, y] = F
        px[1, y] = px[2, y] = LM
        px[13, y] = px[14, y] = LM
        px[15, y] = F
        for x in range(3, 13):
            px[x, y] = E
    for y in range(3, 13):
        px[0, y] = px[1, y] = F
        for x in range(2, 14):
            px[x, y] = E
        px[14, y] = px[15, y] = F
    img.save(os.path.join(folder, "creative_transmuter_btm.png"))


# ==================== GUI 176x166（工作台式布局） ====================
IMAGE_W = 176
IMAGE_H = 166

BG = (198, 198, 198, 255)     # 面板底色
SLOT = (139, 139, 139, 255)   # 槽位内部
WHITE = (255, 255, 255, 255)
SHADOW = (85, 85, 85, 255)
DARK = (55, 55, 55, 255)
BLACK = (0, 0, 0, 255)
CLEAR = (0, 0, 0, 0)

INPUT_ORIGIN = (30, 17)       # 3×3 输入栏左上角（菜单坐标）
OUTPUT_POS = (124, 35)        # 输出栏（菜单坐标）
PLAYER_ROW_YS = [84, 102, 120]
HOTBAR_Y = 142
ARROW_X = (88, 116)           # 箭头左右端点
ARROW_CY = 43                 # 箭头中心 y


def _panel(w, h):
    """
    原版容器面板。每行按"先整行铺底色，再覆盖若干区段"的方式画，
    区段用 (起, 止含, 颜色) 表示，CLEAR 用来切出透明的圆角。
    规则全部由 eternal_sword_gui.png 逐像素逆出，可用 --verify 回放校验。
    """
    img = Image.new("RGBA", (w, h), CLEAR)
    px = img.load()

    def row(y, segments):
        for x in range(w):
            px[x, y] = BG
        for x0, x1, color in segments:
            for x in range(x0, x1 + 1):
                px[x, y] = color

    # 左右边框（中段通用）
    sides = [(0, 0, BLACK), (1, 2, WHITE), (w - 3, w - 2, SHADOW), (w - 1, w - 1, BLACK)]

    # 顶部 3 行：黑描边沿圆角斜切，内侧白高光
    row(0, [(0, 1, CLEAR), (2, w - 4, BLACK), (w - 3, w - 1, CLEAR)])
    row(1, [(0, 0, CLEAR), (1, 1, BLACK), (2, w - 4, WHITE), (w - 3, w - 3, BLACK), (w - 2, w - 1, CLEAR)])
    row(2, [(0, 0, BLACK), (1, w - 4, WHITE), (w - 3, w - 3, BG), (w - 2, w - 2, BLACK), (w - 1, w - 1, CLEAR)])
    # 第 3 行：白高光收窄成 3px，右下阴影开始出现
    row(3, [(0, 0, BLACK), (1, 3, WHITE), (w - 3, w - 2, SHADOW), (w - 1, w - 1, BLACK)])
    # 中段与底部第 5 行同规则
    for y in range(4, h - 4):
        row(y, sides)
    # 底部 4 行：灰阴影沿圆角收口
    row(h - 4, [(0, 0, BLACK), (1, 2, WHITE), (w - 4, w - 2, SHADOW), (w - 1, w - 1, BLACK)])
    row(h - 3, [(0, 0, CLEAR), (1, 1, BLACK), (2, 2, BG), (3, w - 2, SHADOW), (w - 1, w - 1, BLACK)])
    row(h - 2, [(0, 1, CLEAR), (2, 2, BLACK), (3, w - 3, SHADOW), (w - 2, w - 2, BLACK), (w - 1, w - 1, CLEAR)])
    row(h - 1, [(0, 2, CLEAR), (3, w - 3, BLACK), (w - 2, w - 1, CLEAR)])
    return img


def verify_against(source):
    """
    回放校验：用本脚本的规则重画一张与 source 同尺寸的面板，
    比对**边框区域**（左 4 列、右 4 列、上 5 行、下 4 行）是否逐像素一致。
    内容区（槽位、文字）允许不同，不参与比对。返回差异像素数。
    """
    src = Image.open(source).convert("RGBA")
    w, h = src.size
    probe = _panel(w, h)
    diff = 0
    for y in range(h):
        for x in range(w):
            if 4 <= x < w - 4 and 5 <= y < h - 4:
                continue
            if src.getpixel((x, y)) != probe.getpixel((x, y)):
                diff += 1
    return diff


def _slot_box(img, mx, my):
    """18×18 三面倒角槽位方框（与 eternal_sword_gui 逐像素一致），(mx,my) 为菜单坐标。"""
    bx, by = mx - 1, my - 1
    px = img.load()
    for yo in range(18):
        for xo in range(18):
            if (yo == 0 and xo < 17) or (xo == 0 and yo < 17):
                px[bx + xo, by + yo] = DARK
            elif (xo == 17 and yo > 0) or (yo == 17 and xo > 0):
                px[bx + xo, by + yo] = WHITE
            else:
                px[bx + xo, by + yo] = SLOT


def _draw_arrow(img, x0, x1, cy):
    """朝右的实心箭头：主体槽位灰，边缘描深色"""
    shaft_end = x1 - 12
    shape = set()
    for x in range(x0, x1 + 1):
        half = 2 if x <= shaft_end else max(0, 5 - (x - shaft_end) * 5 // (x1 - shaft_end))
        for y in range(cy - half, cy + half + 1):
            shape.add((x, y))
    px = img.load()
    for (x, y) in shape:
        edge = any((x + dx, y + dy) not in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        px[x, y] = DARK if edge else SLOT


def make_gui():
    folder = os.path.join(RES, "textures", "gui")
    os.makedirs(folder, exist_ok=True)

    img = _panel(IMAGE_W, IMAGE_H)
    # 3×3 输入栏
    for row in range(3):
        for col in range(3):
            _slot_box(img, INPUT_ORIGIN[0] + col * 18, INPUT_ORIGIN[1] + row * 18)
    # 输出栏 + 输入→输出 箭头
    _slot_box(img, OUTPUT_POS[0], OUTPUT_POS[1])
    _draw_arrow(img, ARROW_X[0], ARROW_X[1], ARROW_CY)
    # 玩家背包 3 行 + 快捷栏
    for y in PLAYER_ROW_YS:
        for col in range(9):
            _slot_box(img, 8 + col * 18, y)
    for col in range(9):
        _slot_box(img, 8 + col * 18, HOTBAR_Y)

    img.save(os.path.join(folder, "creative_transmuter_gui.png"))


if __name__ == "__main__":
    import sys

    if "--verify" in sys.argv:
        # 拿本模组既有的干净容器界面回放校验边框规则，正常应输出 0
        target = os.path.join(RES, "textures", "gui", "eternal_sword_gui.png")
        print("边框差异像素数 =", verify_against(target), "(应为 0)")
    else:
        make_block_textures()
        make_gui()
        print("已生成 creative_transmuter 占位纹理与 GUI 底图 ->", os.path.normpath(os.path.join(RES, "textures")))
