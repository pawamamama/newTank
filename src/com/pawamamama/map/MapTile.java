package com.pawamamama.map;

import com.pawamamama.Tank.Tank;
import com.pawamamama.game.Bullet;
import com.pawamamama.util.MyUtil;

import java.awt.*;
import java.util.List;

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
    public static int RADIUS = Tank.RADIUS;
    public static int tileW = RADIUS << 1;
    static {
        tileImg = MyUtil.createImg("res/brick.png");
      /*  if (tileW <= 0) {//大小
            tileW = tileImg.getWidth(null);
        }*/
    }

    private int x, y;
    private boolean isVisible = true;//可见性
    /**
     * 无参构造：给对象池预创建对象用，坐标由池的使用方再设置
     */
    public MapTile() {
    }

    public MapTile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    //绘制块
    public void draw(Graphics g) {
        if (!isVisible)//不可键直接不绘制了
            return;
        if (tileW <= 0) {//大小
            tileW = tileImg.getWidth(null);
        }
        g.drawImage(tileImg, x, y, null);
    }
    //块的碰撞

    /**
     * 地图块和一个坦克单位的子弹弹夹是否有碰撞，全场所有坦克的子弹不知道落到哪里所以这一步是对了
     *
     * @param bullets 坦克的弹夹
     * @return
     */
    public boolean isCollideBullet(List<Bullet> bullets) {
        if (!isVisible)//不可见直接不判断了
            return false;
        //子弹坐标
        for (Bullet bullet : bullets) {
            int bullet_x = bullet.getX();
            int bullet_y = bullet.getY();
            //判断每帧每像素的位移，只要子弹中心点碰到了就true
            Boolean collide = MyUtil.sweptCollision(x + RADIUS, y + RADIUS, RADIUS,
                    bullet_x, bullet_y,
                    bullet.getDir(), bullet.getSpeed());
            if (collide) {
                //子弹销毁
                bullet.setVisible(false);
                return collide;
            }
        }
        return false;
    }
    /**
     * 地图块的 8 个碰撞采样点
     * 4 个角 + 4 条边的中点
     */
    class Points8 {
        final int[] xs = new int[8];
        final int[] ys = new int[8];

        Points8() {
            // 四个角
            xs[0] = x;              ys[0] = y;
            xs[1] = x + tileW;      ys[1] = y;
            xs[2] = x;              ys[2] = y + tileW;
            xs[3] = x + tileW;      ys[3] = y + tileW;

            // 上下左右的边中点
            xs[4] = x + tileW / 2;  ys[4] = y;
            xs[5] = x + tileW / 2;  ys[5] = y + tileW;
            xs[6] = x;              ys[6] = y + tileW / 2;
            xs[7] = x + tileW;      ys[7] = y + tileW / 2;
        }
    }
    //一个地图块和坦克发生碰撞了吗
    public boolean isCollideTank(int tankX, int tankY, int tankRadius) {
        Points8 points = new Points8();      // 每次 new，坐标才是最新的
        for (int i = 0; i < points.xs.length; i++) {
            if (MyUtil.isRectangleCollision(tankX, tankY, tankRadius,
                    points.xs[i], points.ys[i])) {
                return true;
            }
        }
        return false;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }
}