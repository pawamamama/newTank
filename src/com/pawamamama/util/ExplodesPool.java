package com.pawamamama.util;

import com.pawamamama.game.Explode;

import java.util.ArrayList;
import java.util.List;

/**
 * Cl对象子ass: ExplodesPool
 * *
 * * <pre>炸弹池类,工具类一般使用静态成员 er1
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/22
 */
@SuppressWarnings({"all"})
public class ExplodesPool {
    //池塘默认值
    public static final int DEFAULT_POOL_SIZE = 200;
    //池塘最大容量
    public static final int MAX_POOL_SIZE = 300;
    //用保存所有炸弹的容器，敌人也用
    private static List<Explode> pool = new ArrayList<Explode>();
    private ExplodesPool() {
    }

    //在类加载的时候创建200个炸弹对象添加到容器中
    static {
        for (int i = 0; i < DEFAULT_POOL_SIZE; i++) {
            pool.add(new Explode());//创建炸弹常量池
        }
    }

    /**
     * 从池塘里获取炸弹
     *
     * @return
     */
    public static Explode get() {
        System.out.println("pool size: " + pool.size());
        Explode explode = null;
        if (pool.size() == 0) {//池塘没有炸弹了
            explode = new Explode();
        } else {//池塘中还有对象
            explode = pool.remove(0);//返回被删除的那个元素
        }
        return explode;
    }

    /**
     * 炸弹被销毁的时候，归还到池塘中
     */
    public static void theReturn(Explode explode) {
        if (pool.size() == MAX_POOL_SIZE) {//池满就把当前要加入的制空，加快jc回收
            return;//return 时 形参销毁
        }
        pool.add(explode);
        System.out.println("对象池中归还了一个炸弹对象，数量为" +  pool.size());
    }
}
