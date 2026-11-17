package com.pawamamama.map;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class: TankHouse
 *
 * <pre> 坦克老巢
 *
 * </pre>
 *
 * @author pawamamama
 * @date 2026/11/13
 */
@SuppressWarnings({"all"})
public class TankHouse {
    //坦克老巢左上角坐标
    public static final int HOUSE_X = 390;
    public static final int HOUSE_Y = 620;
    //老巢的地图块
    private List<MapTile> tiles = new ArrayList<MapTile>();

    public TankHouse() {
        tiles.add(new MapTile(HOUSE_X, HOUSE_Y));
        tiles.add(new MapTile(HOUSE_X + MapTile.tileW, HOUSE_Y));
        tiles.add(new MapTile(HOUSE_X + MapTile.tileW * 2, HOUSE_Y));
        tiles.add(new MapTile(HOUSE_X, HOUSE_Y + MapTile.tileW));
        tiles.add(new MapTile(HOUSE_X + MapTile.tileW, HOUSE_Y + MapTile.tileW));
        tiles.add(new MapTile(HOUSE_X + MapTile.tileW * 2, HOUSE_Y + MapTile.tileW));
        final MapTile mapTile = tiles.get(tiles.size() -5);
        mapTile.setName("家");
    }
    public List<MapTile> getMap() {
        return tiles;
    }
}
