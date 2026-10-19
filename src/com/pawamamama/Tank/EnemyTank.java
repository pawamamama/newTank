package com.pawamamama.Tank;

import com.pawamamama.game.GameFrame;
import com.pawamamama.util.MyUtil;

import java.awt.*;

import static com.pawamamama.util.Constant.FRAME_WIDTH;

/**
 * Class: EnemyTank
 *
 * <pre>敌人坦克类
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/19
 */
@SuppressWarnings({"all"})
public class EnemyTank extends Tank {
    private static Image[] enemyImg;

    //静态代码块
    static {
        enemyImg = new Image[4];
        enemyImg[0] = Toolkit.getDefaultToolkit().getImage("res/enemy_up.png");
        enemyImg[1] = Toolkit.getDefaultToolkit().getImage("res/enemy_down.png");
        enemyImg[2] = Toolkit.getDefaultToolkit().getImage("res/enemy_left.png");
        enemyImg[3] = Toolkit.getDefaultToolkit().getImage("res/enemy_right.png");
    }

    public EnemyTank(int x, int y, int dir) {
        super(x, y, dir);
    }

    @Override
    public void drawImgTank(Graphics g) {
        g.drawImage(enemyImg[getDir()],
                getX() - RADIUS + FIX,   // 左上角 x，+FIX 抵消素材的偏左
                getY() - RADIUS + FIX,   // 左上角 y，+FIX 抵消素材的偏上
                RADIUS << 1,        // 目标宽 40（= RADIUS*2）
                RADIUS << 1,        // 目标高 40
                null);
    }

    //创建一个敌人坦克
    public static EnemyTank createTank() {
        //生成在左上角还是右上角，
        int x = MyUtil.getRandomNumber(0, 2) == 0 ? RADIUS : FRAME_WIDTH - RADIUS;
        int y = GameFrame.titleBarHeight + RADIUS;//标题栏+半径
        int dir = DIR_DOWN;
         EnemyTank enemy = new EnemyTank(x, y, dir);
        enemy.setEnemy(true);
        //TODO
        enemy.setState(STATE_MOVE);
        return enemy;
    }
}