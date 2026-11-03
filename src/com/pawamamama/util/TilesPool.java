package com.pawamamama.util;

import com.pawamamama.map.MapTile;

import java.util.ArrayList;
import java.util.List;

/**
 * Class: TilesPool
 *
 * <pre>砖块池类,工具类一般使用静态成员
 * </pre>
 *
 * @author pawamamama
 * @date 2026/11/1
 */
@SuppressWarnings({"all"})
public class TilesPool {
    //池塘默认值
    public static final int DEFAULT_POOL_SIZE = 200;
    //池塘最大容量：900/40=22列、700/40=17行，整屏摆满也就374块，400封顶够用
    public static final int MAX_POOL_SIZE = 400;
    //保存所有砖块的容器，地图也用
    private static List<MapTile> pool = new ArrayList<MapTile>();

    private TilesPool() {
    }

    //在类加载的时候创建200个砖块对象添加到容器中
    static {
        for (int i = 0; i < DEFAULT_POOL_SIZE; i++) {
            pool.add(new MapTile());//创建砖块对象池
        }
    }

    /**
     * 从池塘里获取砖块
     *
     * @return
     */
    public static MapTile get() {
        MapTile tile = null;
        if (pool.size() == 0) {//池塘没有砖块了
            tile = new MapTile();//就新new一个出来
        } else {//池塘中还有对象
            tile = pool.remove(0);//返回被删除的那个元素
        }
        return tile;
    }

    /**
     * 砖块被销毁的时候，归还到池塘中
     */
    public static void theReturn(MapTile tile) {
        if (pool.size() == MAX_POOL_SIZE) {//池满就不要了，加快gc回收
            return;//return 时 形参销毁
        }
        pool.add(tile);
    }
}
