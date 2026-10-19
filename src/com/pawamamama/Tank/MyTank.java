package com.pawamamama.Tank;

import java.awt.*;

/**
 * Class: MyTank
 *
 * <pre>玩家坦克类
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/19
 */
@SuppressWarnings({"all"})
public class MyTank extends Tank {
    //不写成静态的话，每次创建对象都可能加载（敌人坦克）
    private static Image[] tankImg;

    static {
        tankImg = new Image[4];
        tankImg[0] = Toolkit.getDefaultToolkit().getImage("res/player_up.png");
        tankImg[1] = Toolkit.getDefaultToolkit().getImage("res/player_down.png");
        tankImg[2] = Toolkit.getDefaultToolkit().getImage("res/player_left.png");
        tankImg[3] = Toolkit.getDefaultToolkit().getImage("res/player_right.png");
    }

    public MyTank(int x, int y, int dir) {
        super(x, y, dir);
    }

    @Override
    public void drawImgTank(Graphics g) {
        g.drawImage(tankImg[getDir()],
                getX() - RADIUS + FIX,   // 左上角 x，+FIX 抵消素材的偏左
                getY() - RADIUS + FIX,   // 左上角 y，+FIX 抵消素材的偏上
                RADIUS << 1,        // 目标宽 40（= RADIUS*2）
                RADIUS << 1,        // 目标高 40
                null);
    }
}