package com.pawamamama.util;

import java.awt.*;

/**
 * Class: MyUtil
 *
 * <pre>工具类
 * </pre>
 *
 * @author pawamamama
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
}