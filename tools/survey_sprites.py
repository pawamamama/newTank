# -*- coding: utf-8 -*-
"""
精灵图勘察工具：把原图放大并标上坐标网格，便于逐格确认。
输出到 assets/tank/_survey_*.png
"""
import os
from PIL import Image, ImageDraw

SRC = r"C:\Users\wangw\Downloads\NES - Battle City (JPN) - Miscellaneous - General Sprites.png"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "tank")


def survey(x0, y0, x1, y1, scale, grid, name, margin=28):
    im = Image.open(SRC).convert("RGBA")
    crop = im.crop((x0, y0, x1, y1))
    w, h = crop.size
    big = crop.resize((w * scale, h * scale), Image.NEAREST)

    canvas = Image.new("RGBA",
                       (w * scale + margin, h * scale + margin),
                       (24, 24, 28, 255))
    canvas.alpha_composite(big, (margin, margin))
    d = ImageDraw.Draw(canvas)

    # 网格线
    for gx in range(0, w + 1, grid):
        px = margin + gx * scale
        d.line([(px, margin), (px, margin + h * scale)], fill=(90, 90, 110, 255))
        d.text((px + 2, 4), str(x0 + gx), fill=(200, 220, 255, 255))
    for gy in range(0, h + 1, grid):
        py = margin + gy * scale
        d.line([(margin, py), (margin + w * scale, py)], fill=(90, 90, 110, 255))
        d.text((2, py), str(y0 + gy), fill=(200, 220, 255, 255))

    p = os.path.join(OUT, name)
    canvas.save(p)
    print("saved", p, canvas.size)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    # 坦克区（黄）：128x128，16px 网格
    survey(0, 0, 128, 128, 6, 16, "_survey_tank.png")
    # 爆炸区：x 250..345, y 120..215
    survey(250, 120, 345, 215, 6, 16, "_survey_boom.png")
