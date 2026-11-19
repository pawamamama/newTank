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
    }


    //todo块种类枚举
    public enum TileType {
        BRICK,//普通块 一血
        STEEL,//钢块 三血
        WATER,//水块 todo子弹可穿透 不参与血量计算也不能消失
        GRASS,//草   todo最后做 敌人进去玩家看不见 不参与血量计算也不能消失
        HOUSE //家   被敌人打了就死掉 一血
    }

    //todo块状态枚举
    public enum HitResult {
        BLOCKED,     // 块还在
        DESTROYED,   // 块没了
        NEVERDIE,    //块无法死亡,坦克也无法打到
        HOUSE_DOWN   // 家被打中
    }

    private TileType type;
    private HitResult hitResult;
    //血量，可以根据hp表示破损程度
    private int hp;

    private int x, y;
    private boolean isVisible = true;//可见性


    /**
     * 无参构造：给对象池预创建对象用，坐标由池的使用方再设置
     *
     */
    public MapTile() {
        //池中创建时候不该有任何数值交由settype解决
    }

    private MapTile(int x, int y) {
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

    /**
     * 地图块和一个坦克单位的子弹弹夹是否有碰撞，全场所有坦克的子弹不知道落到哪里所以这一步是对了
     * 11.17改成只负责看打没打到
     * 11.18改成负责状态
     *
     * @param bullets 坦克的弹夹
     * @return
     */
    public boolean isCollideBullet(List<Bullet> bullets) {
        if (!isVisible)//不可见直接不判断了
            return false;
        //子弹坐标
        for (Bullet bullet : bullets) {
            //子弹不可见
            if (!bullet.isVisible()) continue;
            int bullet_x = bullet.getX();
            int bullet_y = bullet.getY();
            //判断每帧每像素的位移，只要子弹中心点碰到了就true
            Boolean bool = MyUtil.sweptCollision(x + RADIUS, y + RADIUS, RADIUS,
                    bullet_x, bullet_y,
                    bullet.getDir(), bullet.getSpeed());
            if (bool) {
                //调用地图块的状态让他状态变化
                test(bullet);
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return 返回子弹击中后的状态，并在该方法内负责块自己的状态变化，变化完成由
     */
    public HitResult test(Bullet bullet) {
        switch (type) {
            //普通块打到直接 不可见
            case BRICK:
                System.out.println("进入test");
                bullet.setVisible(false);
                setVisible(false);
                setHitResult(HitResult.DESTROYED);
                break;
            //钢块打到先扣血
            case STEEL:
                if (hp > 1) {
                    bullet.setVisible(false);
                    hp--;
                } else {
                    //块死亡
                    bullet.setVisible(false);
                    setVisible(false);
                    setHitResult(HitResult.DESTROYED);
                }
                ;
                break;
            //家块打到直接返回游戏结束
            case HOUSE:
                bullet.setVisible(false);
                setVisible(false);
                setHitResult(HitResult.HOUSE_DOWN);
                break;
        }
        return getHitResult(); //这边直接获取返回
    }

    /*
     * 地图块的 8 个碰撞采样点
     * 4 个角 + 4 条边的中点
     */
    class Points8 {
        final int[] xs = new int[8];
        final int[] ys = new int[8];

        Points8() {
            // 四个角
            xs[0] = x;
            ys[0] = y;
            xs[1] = x + tileW;
            ys[1] = y;
            xs[2] = x;
            ys[2] = y + tileW;
            xs[3] = x + tileW;
            ys[3] = y + tileW;

            // 上下左右的边中点
            xs[4] = x + tileW / 2;
            ys[4] = y;
            xs[5] = x + tileW / 2;
            ys[5] = y + tileW;
            xs[6] = x;
            ys[6] = y + tileW / 2;
            xs[7] = x + tileW;
            ys[7] = y + tileW / 2;
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

    //重置地图块到初始状态
    public void reset() {
        hp = 0;
        type = TileType.BRICK;
        hitResult = HitResult.BLOCKED;

        isVisible = true;
    }

    public void setType(TileType type) {
        this.type = type;
        if (type == TileType.BRICK || type == TileType.HOUSE) {
            //一血
            this.hp = 1;
            hitResult = HitResult.BLOCKED;
        } else if (type == TileType.STEEL) {
            //三血
            this.hp = 3;
            hitResult = HitResult.BLOCKED;
        } else {
            //不参与,无法被打坏
            hitResult = HitResult.NEVERDIE;
        }

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

    public TileType getType() {
        return type;
    }

    public HitResult getHitResult() {
        return hitResult;
    }

    public void setHitResult(HitResult hitResult) {
        this.hitResult = hitResult;
    }
}