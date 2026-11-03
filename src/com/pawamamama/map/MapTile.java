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
    public static int tileW;
    static {
        tileImg = MyUtil.createImg("res/brick.png");
              

        if (tileW <= 0) {//大小
            tileW = tileImg.getWidth(null);
        }
}


    private int x, y;



    /**
     * 无参构造：给对象池预创建对象用，坐标由池的使用方再设置
     */
    public MapTile() {


    }

    public MapTile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void draw(Graphics g) {
        if (tileW <= 0) {//大小
            tileW = tileImg.getWidth(null);
        }
        g.drawImage(tileImg, x, y, null);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}