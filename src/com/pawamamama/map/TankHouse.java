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
    //坦克家生成器
    public TankHouse() {
        final int ROWS = 2;
        final int COLS = 3;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                MapTile tile = new MapTile();
                tile.setX(HOUSE_X + col * MapTile.tileW);
                tile.setY(HOUSE_Y + row * MapTile.tileW);
                if (row == 0 && col == 1) {//上排中间那块是家
                    tile.setType(MapTile.TileType.HOUSE);
                }else {//其余的是普通
                    tile.setType(MapTile.TileType.BRICK);
                }
                tiles.add(tile);
            }
        }
    }
    public List<MapTile> getMap() {
        return tiles;
    }
}
