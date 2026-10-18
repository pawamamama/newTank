package com.pawamamama.game;

import java.awt.*;
import java.util.Vector;

import static com.pawamamama.util.Constant.FRAME_HEIGHT;
import static com.pawamamama.util.Constant.FRAME_WIDTH;

/**
 * Class: Bullet
 *
 * <pre>
 *     子弹类
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/17
 */
@SuppressWarnings({"all"})
public class Bullet {
    //子弹默认速度为坦克二倍
    public static final int DEFAULT_SPEED = Tank.DEFAULT_SPEED * 2;
    //子弹大小
    public static final int RADIUS = 4;
    private int x;
    private int y;
    private int speed = DEFAULT_SPEED;
    private int atk;
    private Color color;
    private int dir;
    private boolean visible = true; //子弹是否可见

    public Bullet(int x, int y, int dir, int atk, Color color) {
        this.x = x;
        this.y = y;
        this.dir = dir;
        this.atk = atk;
        this.color = color;
    }

    //给对象池用的
    public Bullet() {

    }

    //绘制子弹方法
    public void draw(Graphics g) {
        if (!visible) return;//如果不可见了就不绘制
        logic();
        g.setColor(color);
        //绘制坦克圆和炮弹圆是一样地
        g.fillOval(x - RADIUS, y - RADIUS, RADIUS << 1, RADIUS << 1);
    }

    //子弹逻辑
    private void logic() {
        move();
    }

    private void move() {
        switch (dir) {
            case Tank.DIR_UP:
                y -= speed;
                if (y <= GameFrame.titleBarHeight) {
                    visible = false;
                }
                break;
            case Tank.DIR_DOWN:
                y += speed;
                if (y >= FRAME_HEIGHT) {
                    visible = false;
                }
                break;
            case Tank.DIR_LEFT:
                x -= speed;
                if (x <= 0) {
                    visible = false;
                }
                break;
            case Tank.DIR_RIGHT:
                x += speed;
                if (x >= FRAME_WIDTH) {
                    visible = false;
                }
                break;
        }
    }
    

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getDir() {
        return dir;
    }

    public void setDir(int dir) {
        this.dir = dir;
    }

    public int getAtk() {
        return atk;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean b) {
        this.visible = b;
    }
}