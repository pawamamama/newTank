package com.pawamamama.game;

import com.pawamamama.Tank.Tank;
import com.pawamamama.util.MyUtil;

import java.awt.*;

/**
 * Class: Explode
 *
 * <pre>炸弹类，用于坦克受伤之后的爆炸效果直到坦克死亡
 *       偏离教程的功能
 *       0.因为素材原因所以图片使用了缩放技术
 *       1.图片分帧 3帧小爆炸，5帧大爆炸，通过坦克扣完的血量鉴别
 *       2.播放帧 和图片帧分离 可以让图片在多帧留存
 *       3.小爆炸用子弹坐标 坦克死亡使用坦克坐标
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/21
 */
@SuppressWarnings({"all"})
public class Explode {
    //图片数量
    public static final int EXPLODE_FRAME_COUNT = 5;
    //图片原始宽高
    private static final int[] RES_W = {11, 15, 16, 28, 34};
    private static final int[] RES_H = {11, 14, 16, 29, 31};
    //缩放倍数
    private static final int MULTIPLE_OF_SCALE = (Tank.RADIUS * 2) / 16;
    //倍帧功能->原来是5帧 一张图片留存一帧，使用这个可以让一张图片显示在x 帧中，由slow控制
    //如果爆炸效果快就用这个调整到合适
    public static final int SLOW = 3;
    private static Image[] boomImg;

    //加载图片
    static {
        boomImg = new Image[EXPLODE_FRAME_COUNT];
        for (int i = 0; i < boomImg.length; i++) {
            boomImg[i] = MyUtil.createImg("res/boom_" + i + ".png");
        }


    }

    private int x, y;
    //播放下标
    private int index;
    //终止下标,播到哪停止
    private int endIndex;
    //设置可见性 
    private boolean isVisible;
    public Explode() {
    }

    public Explode(int x, int y, int endIndex) {
        this.x = x;
        this.y = y;
        this.endIndex = endIndex * SLOW;
        this.isVisible = true;//刚创建可见性肯定是true
    }

    //绘制炸弹效果，一帧播放一张图片
    public void drawBoom(Graphics g) {
        //把播放帧还原回图片真实数量，index++只加播放帧
        int Real_index = index / SLOW;
        if (!isVisible) {//不可见直接返回
            return;
        }
        //当index 大于图片数量的时候设置为不可见
        if (Real_index >= EXPLODE_FRAME_COUNT || index >= endIndex) {
            isVisible = false;            
            return;
        }  
        //真正开始画爆炸效果
        drawScaling(g,Real_index);
        index++;//加播放帧数
    }

    //绘制缩放系数方法，因为图片和演示教程不一致，要使用坦克的缩放倍数
    private void drawScaling(Graphics g , int index) {
        //先取缩放后的大小，但不是半径是全长     
        int w = RES_W[index] * MULTIPLE_OF_SCALE;
        int h = RES_H[index] * MULTIPLE_OF_SCALE;
        //开始绘制
        //默认左上角整体左上方移动，对齐坦克中心
        g.drawImage(boomImg[index], x - w / 2, y - h / 2, w, h, null);
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

}