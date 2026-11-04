package com.pawamamama.map;

import com.pawamamama.Tank.Tank;
import com.pawamamama.game.GameFrame;
import com.pawamamama.util.Constant;
import com.pawamamama.util.MyUtil;
import com.pawamamama.util.TilesPool;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class: Map
 *
 * <pre>地图类，地图类在中心四边有坦克1.5倍宽度的通道
 * </pre>
 *
 * @author pawamamama
 * @date 2026/11/1
 */
@SuppressWarnings({"all"})
public class GameMap {
    //地图尺寸：19 列 × 14 行，每格 40（= 一辆坦克的宽度），共 760 × 560
    //宽高放在前面先声明，下面 MAP_X/MAP_Y 的公式要用到它们
    private final int MAP_WINTH = 19 * Tank.RADIUS * 2;                            // 760
    private final int MAP_HEIGHT = 14 * Tank.RADIUS * 2;                           // 560
    //地图通道
    //水平：坦克车体左右能贴到 0 和 900，地图居中，左右各留 70（约 1.75 辆坦克）
    private final int MAP_X = (Constant.FRAME_WIDTH - MAP_WINTH) / 2;    // 70
    //垂直：坦克车体顶边最高到 titleBarHeight（按 MULTIPLE 换算后的逻辑值，约 20），
    //底边最低到 700。地图在坦克活动区里垂直居中，上下各留 60（正好 1.5 辆坦克）。
    private final int MAP_Y = (GameFrame.titleBarHeight + Constant.FRAME_HEIGHT - MAP_HEIGHT) / 2;  // 80
    //游戏地图块的容器
    private List<MapTile> tiles = new ArrayList<>();

    public GameMap() {
        initMap();
    }

    /**
     * 初始化地图元素块,把地图分割成19*14的地图块
     * 先随机生成,
     */
    private void initMap() {
        final int COUNT = 19 * 14;

        for (int i = 0; i < COUNT; i++) {
            //随机得到一个地图块
            MapTile mapTile = TilesPool.get();
            //x允许生成的范围，起点为地图x坐标，终点为了考虑到地图块本身的大小
            // 列号 0~18，行号 0~13
            int col = MyUtil.getRandomNumber(0, 19);
            int row = MyUtil.getRandomNumber(0, 14);
            mapTile.setX(MAP_X + col * 40);
            mapTile.setY(MAP_Y + row * 40);
            tiles.add(mapTile);//把生成的地图添加在集合内
        }

    }

    public void draw(Graphics g) {
        for (MapTile tile : tiles) {
            tile.draw(g);
        }
    }

}