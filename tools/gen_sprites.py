# -*- coding: utf-8 -*-
"""
坦克大战素材生成器
--------------------
生成经典 Battle City 风格的像素素材：
  - 黄色我方坦克 / 红色敌方坦克，各 4 个方向
  - 5 帧爆炸效果
输出为 PNG（透明背景），尺寸 40x40，正好匹配 Tank.RADIUS = 20（直径 40）。

原理：先在 20x20 的像素网格上手绘，再用最近邻放大 2 倍到 40x40，
保证像素边缘锐利、不发虚。

用法：
    python gen_sprites.py
"""
import os
from PIL import Image

# ---------------- 配色 ----------------
OUTLINE = (32, 32, 32, 255)

# 我方：经典黄色
YELLOW = {
    "k": OUTLINE,
    "m": (240, 192, 0, 255),    # 主体
    "l": (255, 232, 96, 255),   # 高光
    "d": (138, 106, 0, 255),    # 阴影
}

# 敌方：红色
RED = {
    "k": OUTLINE,
    "m": (208, 48, 48, 255),
    "l": (255, 112, 112, 255),
    "d": (112, 16, 16, 255),
}

SIZE = 20          # 设计网格尺寸
SCALE = 2          # 放大倍数
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "tank")


def tank_up(c):
    """画一辆朝上的坦克（20x20）"""
    im = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    p = im.load()

    def rect(x0, y0, x1, y1, col):
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1):
                if 0 <= xx < SIZE and 0 <= yy < SIZE:
                    p[xx, yy] = col

    # ---- 履带：左右各 4px 宽，y 2~18 ----
    for base_x in (0, 16):
        rect(base_x, 2, base_x + 3, 18, c["k"])          # 外描边
        rect(base_x + 1, 3, base_x + 2, 17, c["m"])      # 内填充
        for y in range(3, 18, 3):                        # 履带节纹（横向暗条）
            rect(base_x + 1, y, base_x + 2, y, c["d"])

    # ---- 车体 ----
    rect(4, 3, 15, 18, c["k"])
    rect(5, 4, 14, 17, c["m"])

    # ---- 炮塔（中央凸起）----
    rect(7, 5, 12, 13, c["k"])
    rect(8, 6, 11, 12, c["l"])

    # ---- 炮筒（朝上）----
    rect(8, 0, 11, 4, c["k"])
    rect(9, 1, 10, 3, c["m"])

    return im


def explode_frames():
    """生成 5 帧爆炸，返回 20x20 的图列表"""
    import math
    # 每帧：外半径, 核心白半径比例, 主色, 边色
    specs = [
        (4.2, 0.45, (255, 255, 255, 255), (255, 232, 64, 255)),
        (6.4, 0.40, (255, 255, 200, 255), (255, 200, 0, 255)),
        (8.4, 0.30, (255, 224, 96, 255), (255, 128, 0, 255)),
        (9.8, 0.18, (255, 160, 32, 255), (208, 64, 0, 255)),
        (10.6, 0.08, (208, 96, 32, 255), (128, 32, 0, 255)),
    ]
    frames = []
    cx = cy = (SIZE - 1) / 2.0

    for idx, (r_out, core_ratio, core_col, rim_col) in enumerate(specs):
        im = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        p = im.load()
        mid_col = tuple(((a + b) // 2) for a, b in zip(core_col, rim_col))
        phase = idx * 0.7

        for yy in range(SIZE):
            for xx in range(SIZE):
                dx, dy = xx - cx, yy - cy
                d = math.hypot(dx, dy)
                ang = math.atan2(dy, dx)
                # 星芒：让边缘呈 4 瓣不规则形状
                r_eff = r_out * (1.0 + 0.22 * math.sin(4 * ang + phase))
                if d > r_eff:
                    continue
                # 边缘做点抖动，避免死板的正圆
                if d > r_eff - 0.9 and idx >= 2:
                    if (xx * 7 + yy * 13 + idx * 5) % 3 == 0:
                        continue
                ratio = d / max(r_eff, 0.001)
                if ratio < core_ratio:
                    col = core_col
                elif ratio < (core_ratio + 0.45) / 2:
                    col = mid_col
                else:
                    col = rim_col
                p[xx, yy] = col
        frames.append(im)

    return frames


def rotate_dir(im, direction):
    """把朝上的坦克转成指定方向"""
    if direction == "up":
        return im
    if direction == "right":
        return im.rotate(-90, expand=True)     # 顺时针
    if direction == "down":
        return im.rotate(180, expand=True)
    if direction == "left":
        return im.rotate(90, expand=True)      # 逆时针
    raise ValueError(direction)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    up_y = tank_up(YELLOW)
    up_r = tank_up(RED)

    written = []
    for d in ("up", "down", "left", "right"):
        for prefix, base in (("player", up_y), ("enemy", up_r)):
            img = rotate_dir(base, d).resize(
                (SIZE * SCALE, SIZE * SCALE), Image.NEAREST)
            name = f"{prefix}_{d}.png"
            img.save(os.path.join(OUT_DIR, name))
            written.append(name)

    for i, fr in enumerate(explode_frames(), start=1):
        img = fr.resize((SIZE * SCALE, SIZE * SCALE), Image.NEAREST)
        name = f"explosion_{i}.png"
        img.save(os.path.join(OUT_DIR, name))
        written.append(name)

    # ---- 预览图（深色底，放大 4 倍，方便肉眼检查）----
    preview_scale = 4
    cell = SIZE * preview_scale
    cols, rows = 5, 3
    gap = 8
    sheet = Image.new("RGBA",
                      (cols * cell + gap * (cols + 1),
                       rows * cell + gap * (rows + 1)),
                      (24, 24, 28, 255))

    order = [
        (0, 0, up_y), (1, 0, rotate_dir(up_y, "right")),
        (2, 0, rotate_dir(up_y, "down")), (3, 0, rotate_dir(up_y, "left")),
        (0, 1, up_r), (1, 1, rotate_dir(up_r, "right")),
        (2, 1, rotate_dir(up_r, "down")), (3, 1, rotate_dir(up_r, "left")),
    ]
    for cx_, cy_, im in order:
        big = im.resize((cell, cell), Image.NEAREST)
        sheet.alpha_composite(
            big, (gap + cx_ * (cell + gap), gap + cy_ * (cell + gap)))

    # 爆炸帧放第三行
    for i, fr in enumerate(explode_frames()):
        big = fr.resize((cell, cell), Image.NEAREST)
        sheet.alpha_composite(
            big, (gap + i * (cell + gap), gap + 2 * (cell + gap)))

    preview_path = os.path.join(OUT_DIR, "_preview.png")
    sheet.save(preview_path)

    print(f"输出目录: {os.path.abspath(OUT_DIR)}")
    for n in written:
        print("  ", n)
    print("   _preview.png")


if __name__ == "__main__":
    main()
