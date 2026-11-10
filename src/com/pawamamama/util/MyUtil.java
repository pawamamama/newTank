package com.pawamamama.util;

import com.pawamamama.Tank.Tank;
import com.pawamamama.game.Explode;

import java.awt.*;

/**
 * Class: MyUtil
 *
 * <pre>工具类
 * </pre>
 *
 * @author pawamamamad
 * @date 2026/10/14
 */
@SuppressWarnings({"all"})
public class MyUtil {
    private MyUtil() {
    }

    /**
     *
     * @param min 包含
     * @param max 不包含
     * @return 随机数
     */
    public static final int getRandomNumber(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    /**
     *
     * @return 随机颜色
     */
    public static final Color getRandomColor() {
        final Color c = new Color(
                getRandomNumber(0, 256),
                getRandomNumber(0, 256),
                getRandomNumber(0, 256));
        return c;
    }

    //------------------名字池-------------------------
    //用数组管理，以后加/删名字不用改方法
    public static final String[] NAMES = {
            "铁锤",
            "闪电",
            "孤狼",
            "雷霆",
            "幽灵",
            "烈焰",
            "寒霜",
            "铁壁",
            "疾风",
            "暗影",
            "钢牙",
            "赤焰",
            "苍龙",
            "猛虎",
            "碎星",
    };

    /**
     * 随机取一个名字
     *
     * <p>复用 {@link #getRandomNumber(int, int)}：
     * 区间 [0, NAMES.length) 正好覆盖所有下标，不会越界。
     *
     * @return 名字池里的随机一个
     */
    public static final String getRandomName() {
        return NAMES[getRandomNumber(0, NAMES.length)];
    }

    /**
     * 矩形碰撞算法， 当一个物体（矩形）的中心点xy 于一个的xy，小于半径 说明发生碰撞
     *
     * @param rectX  正方形坐标
     * @param rectY  正方形坐标
     * @param radius 正方形边长的一半
     * @param pointX 点坐标
     * @param pointY 点坐标
     * @return 点在正方形内部返回true 否则false
     */
    public static final boolean isRectangleCollision(int rectX, int rectY, int radius, int pointX, int pointY) {
        int disX = Math.abs(rectX - pointX);
        int disY = Math.abs(rectY - pointY);
        if (disX <= radius && disY <= radius) {
            return true;
        }
        return false;
    }

    /**
     * wept collision（扫掠碰撞），意思是「不要只检查子弹这一帧落在哪，要检查它这一帧走过的整条线段有没有撞到砖
     * 一帧走过的像素，就是速度
     *
     * @param rectX  正方形坐标g
     * @param rectY  正方形坐标
     * @param radius 正方形边长的一半
     * @param pointX 点坐标
     * @param pointY 点坐标
     * @param speed  子弹速度
     * @return
     */
    public static final boolean sweptCollision(int rectX, int rectY, int rectRadius, int pointX, int pointY, int dir, int speed) {
        //记录原始x，y
        int oldX = pointX;
        int oldY = pointY;
        for (int step = 0; step < speed; step++) {//根据一次帧数位移speed 每次判断一帧内移动的帧数
            switch (dir) {//按照方向选择加的像素
                case Tank.DIR_UP:
                    oldY--;
                    break;
                case Tank.DIR_DOWN:
                    oldY++;
                    break;
                case Tank.DIR_LEFT:
                    oldX--;
                    break;
                case Tank.DIR_RIGHT:
                    oldX++;
                    break;
            }
            //每位移碰到了
            if (isRectangleCollision(rectX, rectY, rectRadius, oldX, oldY)) {
                return true;
            }
        }
        return false;
    }

    //写一个加载图片的方法
    //传入路径和第一个文件
    public static Image createImg(String path) {
        return Toolkit.getDefaultToolkit().getImage(path);
    }

    /**
     * 添加一个爆炸效果并指定呈现方式
     *
     * @param expectedNumber 期望播放的爆炸效果张数
     * @param x              期望坐标
     * @param y              期望坐标
     * @return 返回一个设置好的炸弹对象
     */
    public static Explode addExplode(int expectedNumber, int x, int y) {
        //防止调用方传入比真实图片张数多的情况
        if (expectedNumber > Explode.EXPLODE_FRAME_COUNT) {
            expectedNumber = Explode.EXPLODE_FRAME_COUNT;
            System.out.println("无效张数，已超过当前最大图片数量，已重置成真实张数为 =  " + expectedNumber);
        }
        final Explode explode = ExplodesPool.get();
        explode.setVisible(true);
        explode.setIndex(0);
        explode.setX(x);
        explode.setY(y);
        explode.setEndIndex(expectedNumber * Explode.SLOW);
        return explode;
    }
}