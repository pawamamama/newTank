package com.pawamamama.map;

import com.pawamamama.util.MyUtil;

import java.awt.*;

/**
 * Class: MapTile
 *
 * <pre>砖块类
 * </pre>
 *
 * @author pawamamama
 * @date 2026/11/1
 */
@SuppressWarnings({"all"})
public class MapTile {
    private static Image tileImg;
    private static int tileW;

    static {
        tileImg = MyUtil.createImg("src/brick.png");
        if (tileW <= 0) {//大小
            tileW = tileImg.getWidth(null);
        }
    }

    private int x, y;

    public MapTile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g) {
        if (tileW <= 0) {//大小
            tileW = tileImg.getWidth(null);
        }
        g.drawImage(tileImg, x, y, null);
    }
}