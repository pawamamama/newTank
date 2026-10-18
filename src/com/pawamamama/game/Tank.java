package com.pawamamama.game;

import com.pawamamama.util.BulletsPool;
import com.pawamamama.util.MyUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.pawamamama.game.Bullet;

import static com.pawamamama.util.Constant.*;

/**
 * Class: Tank 坦克类
 *
 * <pre>
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/14
 */
@SuppressWarnings({"all"})
public class Tank {
    //不写成静态的话，每次创建对象都可能加载（敌人坦克）
    private static Image[] tankImg;
    private static Image[] enemyImg;

    //静态代码快
    static {
        tankImg = new Image[4];
        tankImg[0] = Toolkit.getDefaultToolkit().getImage("res/player_up.png");
        tankImg[1] = Toolkit.getDefaultToolkit().getImage("res/player_down.png");
        tankImg[2] = Toolkit.getDefaultToolkit().getImage("res/player_left.png");
        tankImg[3] = Toolkit.getDefaultToolkit().getImage("res/player_right.png");
        enemyImg = new Image[4];
        enemyImg[0] = Toolkit.getDefaultToolkit().getImage("res/enemy_up.png");
        enemyImg[1] = Toolkit.getDefaultToolkit().getImage("res/enemy_down.png");
        enemyImg[2] = Toolkit.getDefaultToolkit().getImage("res/enemy_left.png");
        enemyImg[3] = Toolkit.getDefaultToolkit().getImage("res/enemy_right.png");
    }

    //在这定义可以直接继承直接 类名.常量名 去使用
    //四个方向,上下左右
    public static final int DIR_UP = 0;
    public static final int DIR_DOWN = 1;
    public static final int DIR_LEFT = 2;
    public static final int DIR_RIGHT = 3;
    //坦克状态
    public static final int STATE_STAND = 0;//静止
    public static final int STATE_MOVE = 1;//移动
    public static final int STATE_DIE = 2;//死亡
    //坦克半径
    public static final int RADIUS = 20;
    //默认速度
    public static final int DEFAULT_SPEED = 4;
    public static final int speed = DEFAULT_SPEED;//每次刷新跑4个像素
    //坦克初始生命
    public static final int DEFAULT_HP = 1000;
    //坐标
    private int x, y;
    //血量
    private int hp = DEFAULT_HP;
    //攻击力
    private int atk;
    //防御力
    private int def;
    //方向
    private int dir;
    //坦克状态
    private int state = STATE_STAND;
    //颜色
    private Color color;
    //TODO 炮弹泛型类
    private List<Bullet> bullets = new ArrayList();

    public Tank(int x, int y, int dir) {
        this.x = x;
        this.y = y;
        this.dir = dir;
        //随机颜色
        color = MyUtil.getRandomColor();//工具类实现
    }

    /**
     * 绘制坦克,每一帧都要调用的
     *
     * @param g
     */
    public void draw(Graphics g) {
        logic();
        //画坦克
        //drawTank(g);
        drawImgTank(g);
        //画所有子弹
        drawBullets(g);
    }

    /**
     * 使用图片的方式画坦克
     * <p>
     * 素材是原生 16x16，绘制时放大到 40x40（RADIUS*2）。
     * <p>
     * 为什么要加 FIX 偏移：
     * 原图格子是 16x16，几何中心在 8.0；
     * 但坦克实际占 x=1~13、炮管占 x=7，两者的中心都是 7.5；
     * 也就是说整辆坦克在格子里偏左 0.5 像素、偏上 0.5 像素。
     * 放大 2.5 倍后，屏幕上就偏了约 1.25 像素。
     * 不补这个偏移，炮管中心就不在逻辑坐标 (x, y) 上，
     * 而 fire() 是从 (x, y) 发射的，看起来子弹就歪到了炮管侧边。
     */
    private void drawImgTank(Graphics g) {
        // 补偿量：图片整体右下移 1 像素，让炮管中心对齐到 (x, y)
        final int FIX = 1;

        g.drawImage(tankImg[dir],
                x - RADIUS + FIX,   // 左上角 x，+FIX 抵消素材的偏左
                y - RADIUS + FIX,   // 左上角 y，+FIX 抵消素材的偏上
                RADIUS << 1,        // 目标宽 40（= RADIUS*2）
                RADIUS << 1,        // 目标高 40
                null);
    }

    //纯画的方式
    private void drawTank(Graphics g) {
        //设置画笔
        g.setColor(color);
        //绘制坦克的圆
        //整体左上方移动才能正确让xy在中
        // <<1 左移是乘2
        g.fillOval(x - RADIUS, y - RADIUS, RADIUS << 1, RADIUS << 1);
        //炮筒中点一定是圆心，刚才圆向左上方偏移了，中点就是x,y
        int endX = x;
        int endY = y;
        switch (dir) {
            case DIR_UP:
                endY = y - RADIUS * 2;
                break;
            case DIR_DOWN:
                endY = y + RADIUS * 2;
                break;
            case DIR_LEFT:
                endX = x - RADIUS * 2;
                break;
            case DIR_RIGHT:
                endX = x + RADIUS * 2;
                break;
        }
        //加粗
        Graphics2D g2d = (Graphics2D) g.create();   // 拿副本，用完扔掉
        g2d.setStroke(new BasicStroke(3));          // 设粗
        g2d.setColor(color);
        g2d.drawLine(x, y, endX, endY);
        g2d.dispose();                              // 还原
    }

    //坦克逻辑处理,每一帧都要处理，所以在draw里面
    private void logic() {
        switch (state) {
            case STATE_STAND:
                break;
            case STATE_MOVE:
                mathMove();
                break;
            case STATE_DIE:
                break;
        }
    }

    private void mathMove() {

        switch (dir) {
            case DIR_UP:
                y = Math.max(GameFrame.titleBarHeight + RADIUS, y - speed);
                break;
            case DIR_DOWN:
                y = Math.min(FRAME_HEIGHT - RADIUS, y + speed);
                break;
            case DIR_LEFT:
                x = Math.max(RADIUS, x - speed);
                break;
            case DIR_RIGHT:
                x = Math.min(FRAME_WIDTH - RADIUS, x + speed);
                break;
        }
    }

    //当坦克开火方法
    //创建一个子弹对象，子弹对象的属性通过坦克信息获得
    //然后将创建的子弹添加到弹夹容器中
    public void fire() {
        int bulletX = x;
        int bulletY = y;
        switch (dir) {
            case DIR_UP:
                bulletY -= RADIUS;
                break;
            case DIR_DOWN:
                bulletY += RADIUS;
                break;
            case DIR_LEFT:
                bulletX -= RADIUS;
                break;
            case DIR_RIGHT:
                bulletX += RADIUS;
                break;
        }
        /**
         * 坦克的atk 和 颜色
         */
        Bullet bullet = BulletsPool.get();
        bullet.setX(bulletX);
        bullet.setY(bulletY);
        bullet.setDir(dir);
        bullet.setAtk(atk);
        bullet.setColor(color);
        bullet.setVisible(true);
        bullets.add(bullet);
    }

    /**
     * 将当前坦克发射的所有子弹绘制出来
     *
     * @param g
     */
    private void drawBullets(Graphics g) {
        for (Bullet bullet : bullets) {
            bullet.draw(g);
        }
        //遍历所有子弹，将不可见的子弹移除，并还原回对象池
        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);
            if (!bullet.isVisible()) {
                bullets.remove(i);//删除弹夹里的
                BulletsPool.theReturn(bullet);//放回子弹池
            }
        }
       // System.out.println("坦克子弹的数量" + bullets.size());
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

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getAtk() {
        return atk;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }


    public int getDef() {
        return def;
    }

    public void setDef(int def) {
        this.def = def;
    }

    public int getDir() {
        return dir;
    }

    public void setDir(int dir) {
        this.dir = dir;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public List getBullets() {
        return bullets;
    }

    public void setBullets(List bullets) {
        this.bullets = bullets;
    }

}