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

}