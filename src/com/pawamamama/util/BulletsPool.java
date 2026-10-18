package com.pawamamama.util;

import com.pawamamama.game.Bullet;

import java.util.ArrayList;
import java.util.List;

/**
 * Cl对象子ass: BulletsPool
 * *
 * * <pre>弹池类,工具类一般使用静态成员 er1
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/18
 */
@SuppressWarnings({"all"})
public class BulletsPool {
    //池塘默认值
    public static final int DEFAULT_POOL_SIZE = 200;
    //池塘最大容量
    public static final int MAX_POOL_SIZE = 300;
    //用保存所有子弹的容器，敌人也用
    private static List<Bullet> pool = new ArrayList<Bullet>();

    //在类加载的时候创建200个子弹对象添加到容器中
    static {
        for (int i = 0; i < DEFAULT_POOL_SIZE; i++) {
            pool.add(new Bullet());//创建子弹常量池
        }
    }

    /**
     * 从池塘里获取子弹
     *
     * @return
     */
    public static Bullet get() {
        System.out.println("pool size: " + pool.size());
        Bullet bullet = null;
        if (pool.size() == 0) {//池塘没有子弹了
            bullet = new Bullet();
        } else {//池塘中还有对象
            bullet = pool.remove(0);//返回被删除的那个元素
        }
        return bullet;
    }

    /**
     * 子弹被销毁的时候，归还到池塘中
     */
    public static void theReturn(Bullet bullet) {
        if (pool.size() == MAX_POOL_SIZE) {//池满就把当前要加入的制空，加快jc回收
            return;//return 时 形参销毁
        }
        pool.add(bullet);
        System.out.println("对象池中归还了一个子弹对象，数量为" +  pool.size());
    }
}