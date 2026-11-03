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
    //地图通道
    private final int MAP_X = Tank.RADIUS * 3;
    private final int MAP_Y = Tank.RADIUS * 3 + GameFrame.titleBarHeight;//标题栏高度也要算
    private final int MAP_WINTH = Constant.FRAME_WIDTH - Tank.RADIUS * 6;
    private final int MAP_HEIGHT = Constant.FRAME_HEIGHT - Tank.RADIUS * 8 - GameFrame.titleBarHeight;
    //游戏地图块的容器
    private List<MapTile> tiles = new ArrayList<>();

    public GameMap() {
        initMap();
    }

    /**
     * 初始化地图元素块
     * 先随机生成
     */
    private void initMap() {
        final int COUNT = 20;
        //随机得到一个地图块
        MapTile mapTile = TilesPool.get();
        //x允许生成的范围，起点为地图x坐标，终点为了考虑到
        int x = MyUtil.getRandomNumber(MAP_X, MAP_X + MAP_WINTH - mapTile.tileW);
        int y = MyUtil.getRandomNumber(MAP_Y, MAP_Y + MAP_WINTH - mapTile.tileW);
        mapTile.setX(x);
        mapTile.setY(y);
        tiles.add(mapTile);//把生成的地图添加在集合内
    }
    public void draw(Graphics g) {
        for (MapTile tile : tiles) {
            tile.draw(g);
        }
    }

}