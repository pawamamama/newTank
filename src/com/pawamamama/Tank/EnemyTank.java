package com.pawamamama.Tank;

import com.pawamamama.game.GameFrame;
import com.pawamamama.util.Constant;
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
    //记录ai开始的时间,用于判断ai决策
    private long aiStartTime;

    //静态代码块
    static {
        enemyImg = new Image[4];
        enemyImg[0] = MyUtil.createImg("res/enemy_up.png");
        enemyImg[1] = MyUtil.createImg("res/enemy_down.png");
        enemyImg[2] = MyUtil.createImg("res/enemy_left.png");
        enemyImg[3] = MyUtil.createImg("res/enemy_right.png");
    }

    public EnemyTank(int x, int y, int dir) {
        super(x, y, dir);
        //创建出来立马获取当前时间
        aiStartTime = System.currentTimeMillis();
    }

    @Override
    public void drawImgTank(Graphics g) {
        //决策完再画
        ai();
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

    //ai决策，每一帧都得判断所以在draw里去调用
    private void ai() {
        //系统时间-敌人创建时间 > ai决策间隔，就去设置一次状态
        if (System.currentTimeMillis() - aiStartTime > Constant.ENEMY_AI_INTERVAL) {
            //设置状态
            setState(MyUtil.getRandomNumber(STATE_STAND, STATE_MOVE + 1));
            //重新设置时间
            aiStartTime = System.currentTimeMillis();
            if (getState() == STATE_MOVE) {//如果为移动那就重新设置方向
                //设置随机方向 [min,max) 前闭后开 max 取不到
                setDir(MyUtil.getRandomNumber(0, 4));
            }

        }
        //子弹发射
        if (Math.random() < Constant.ENEMY_FIRE_PERCENT) {//小于设定概率就发射
            fire();
        }
    }

}