package com.pawamamama.game;

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
    private  static  Image[] tankImg;
    private static  Image[] enemyImg;
    //静态代码快
    static {
        tankImg = new Image[4];
        tankImg[0] = Toolkit.getDefaultToolkit().getImage("res/player_up.png");
        tankImg[1] = Toolkit.getDefaultToolkit().getImage("res/player_down.png");
        tankImg[2] = Toolkit.getDefaultToolkit().getImage("res/player_left.png");
        tankImg[3] = Toolkit.getDefaultToolkit().getImage("res/player_right.png");
        enemyImg =  new Image[4];
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
     * 使用图片的方式来画坦克
     * 素材是原生 16x16，这里指定画成 40x40（RADIUS*2），由 Java 放大
     * 配合 update() 里的 NEAREST 渲染提示，放大后边缘依然锐利
     *
     * @param g
     */
    private void drawImgTank(Graphics g) {
        g.drawImage(tankImg[dir],
                x - RADIUS, y - RADIUS,
                RADIUS << 1, RADIUS << 1,
                null);
    }
    //纯画的方式
    private void drawTank(Graphics g) {
        //设置画笔
        g.setColor(color);
        //绘制坦克的圆
        //整体左上方移动才能正确让xy在中心点
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
                bulletY -= RADIUS ;
                break;
            case DIR_DOWN:
                bulletY += RADIUS ;
                break;
            case DIR_LEFT:
                bulletX -= RADIUS ;
                break;
            case DIR_RIGHT:
                bulletX += RADIUS ;
                break;
        }
        /**
         * 坦克的atk 和 颜色
         */
        Bullet bullet = new Bullet(bulletX,bulletY,dir,atk,color);
        bullets.add(bullet);
    }

    /**
     * 将当前坦克发射的所有子弹绘制出来
     * @param g
     */
    private void drawBullets(Graphics g) {
        for (Bullet bullet : bullets) {
            bullet.draw(g);
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