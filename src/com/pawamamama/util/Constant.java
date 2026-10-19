package com.pawamamama.util;

import java.awt.*;

/**
 * Class: Constant
 *
 * <pre>游戏常量类，都在该类进行维护
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/4
 */
@SuppressWarnings({"all"})
public final class Constant {
    //------------------游戏窗口-------------------------
    public static final String GAME_TITLE = "坦克大战第二版:pawamamama";
    //宽和高
    public static final int FRAME_WIDTH = 900;
    public static final int FRAME_HEIGHT = 700;


    // ------------------游戏菜单-------------------------
    public static final int STATE_MENU = 0;   // 游戏主菜单
    public static final int STATE_HELP = 1;   // 帮助界面
    public static final int STATE_ABOUT = 2;  // 关于界面
    public static final int STATE_RUN = 3;    // 游戏运行中
    public static final int STATE_OVER = 4;   // 游戏结束
    //使用String数组管理
    public static final String[] MENUS = {
            "开始游戏",
            "继续游戏",
            "游戏帮助",
            "游戏关于",
            "退出游戏",
    };
    //字体
    public static final Font FONT = new Font("宋体", Font.BOLD, 24);
    //线程休眠多长时间
    public static final int REPAINT_INTERVAL = 30;
    //敌人最大数量
    public static final int ENEMY_MAX_COUNT = 10;
    //敌人生成间隔
    public static final int ENEMY_BORN__INTERVAL = 5000;
    //敌人ai 决策时间
    public static final int ENEMY_AI_INTERVAL = 3000;
    //敌人开火百分比
    public static final double ENEMY_FIRE_PERCENT = 0.03;
}