# -*- coding: utf-8 -*-
"""
从原版 Battle City 精灵图中提取素材（原生 16x16 尺寸）
=====================================================
输出到 res/，供 Tank.java 静态加载。

设计原则：
  * 保持原图原生像素：坦克 16x16，一格就是一格，绝不重采样
  * 只把背景色 (0,0,1) 置为透明，其余像素一律不动
  * 绘制时由 Java 端放大（drawImage 传目标宽高），配合 NEAREST 保持锐利

坐标（原图实测）：
  坦克区 x=0..127, y=0..127，16x16 网格
    每方向 2 列（帧1/帧2），每行一种坦克
      x   0,16  → 朝上
      x  32,48  → 朝左
      x  64,80  → 朝下
      x  96,112 → 朝右
  取 y=16 这一行（车体最饱满、四方向齐全），帧1。

  爆炸区（爆炸专用配色连通域实测）：
      x 259-269  y 130-140   11x11
      x 272-286  y 129-142   15x14
      x 288-303  y 128-143   16x16
      x 306-333  y 129-157   28x29
      x 334-367  y 129-159   34x31
"""
import os
from PIL import Image

SRC = r"C:\Users\wangw\Downloads\NES - Battle City (JPN) - Miscellaneous - General Sprites.png"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "res")
BG = (0, 0, 1)
TOL = 6


def is_bg(p):
    return (abs(p[0] - BG[0]) <= TOL and
            abs(p[1] - BG[1]) <= TOL and
            abs(p[2] - BG[2]) <= TOL)


def cut(im, bx, by, w, h):
    """裁一块，背景转透明，原样返回（不缩放、不裁留白）"""
    c = im.crop((bx, by, bx + w, by + h)).convert("RGBA")
    px = c.load()
    for y in range(c.height):
        for x in range(c.width):
            if is_bg(px[x, y][:3]):
                px[x, y] = (0, 0, 0, 0)
    return c


# 原图橙黄配色 -> 红色（敌人）
TO_RED = {
    (231, 156, 33): (214, 52, 40),
    (107, 107, 0):  (96, 14, 10),
    (231, 231, 148): (255, 150, 138),
}


def recolor(im, mapping):
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            m = mapping.get((r, g, b))
            if m:
                px[x, y] = m + (a,)
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    im = Image.open(SRC).convert("RGBA")

    # 清掉旧素材（保留 _ 开头的勘察图）
    for f in os.listdir(OUT):
        if f.endswith(".png") and not f.startswith("_"):
            os.remove(os.path.join(OUT, f))

    # ---- 坦克：原生 16x16 ----
    ROW = 16
    for d, bx in [("up", 0), ("left", 32), ("down", 64), ("right", 96)]:
        spr = cut(im, bx, ROW, 16, 16)
        spr.save(os.path.join(OUT, f"player_{d}.png"))
        recolor(spr, TO_RED).save(os.path.join(OUT, f"enemy_{d}.png"))
        print(f"  player_{d}.png / enemy_{d}.png   16x16  来自 ({bx},{ROW})")

    # ---- 爆炸：原生尺寸，不做缩放 ----
    BOOMS = [
        (259, 130, 11, 11),
        (272, 129, 15, 14),
        (288, 128, 16, 16),
        (306, 128, 28, 29),
        (334, 128, 34, 31),
    ]
    for i, (bx, by, w, h) in enumerate(BOOMS, start=1):
        spr = cut(im, bx, by, w, h)
        spr.save(os.path.join(OUT, f"explosion_{i}.png"))
        print(f"  explosion_{i}.png   {w}x{h}  来自 ({bx},{by})")

    print("\n完成 ->", os.path.abspath(OUT))


if __name__ == "__main__":
    main()
