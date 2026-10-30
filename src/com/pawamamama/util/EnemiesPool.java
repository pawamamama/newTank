package com.pawamamama.util;

import com.pawamamama.Tank.EnemyTank;

import java.util.ArrayList;
import java.util.List;

/**
 * Class: EnemiesPool
 *
 * <pre>敌人池类,结构同 BulletsPool / ExplodesPool
 *       敌人同屏最多 10 辆，池子不用开太大
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/25
 */
@SuppressWarnings({"all"})
public class EnemiesPool {
    //池塘默认值
    public static final int DEFAULT_POOL_SIZE = 10;
    //池塘最大容量
    public static final int MAX_POOL_SIZE = 15;
    //用来保存所有敌人的容器
    private static List<EnemyTank> pool = new ArrayList<EnemyTank>();

    private EnemiesPool() {
    }

    //在类加载的时候创建 10 个敌人对象添加到容器中
    static {
        for (int i = 0; i < DEFAULT_POOL_SIZE; i++) {
            pool.add(new EnemyTank());//创建敌人常量池
        }
    }

    /**
     * 从池塘里获取敌人
     *
     * @return
     */
    public static EnemyTank get() {
        System.out.println("pool size: " + pool.size());
        EnemyTank enemy = null;
        if (pool.size() == 0) {//池塘没有敌人了
            enemy = new EnemyTank();//就新new一个出来
        } else {//池塘中还有对象
            enemy = pool.remove(0);//返回被删除的那个元素
        }
        return enemy;
    }

    /**
     * 敌人被销毁的时候，归还到池塘中
     */
    public static void theReturn(EnemyTank enemy) {
        if (pool.size() == MAX_POOL_SIZE) {//池满就把当前要加入的制空，加快jc回收
            return;//return 时 形参销毁
        }
        pool.add(enemy);
        System.out.println("对象池中归还了一个敌人对象，数量为" + pool.size());
    }
}
