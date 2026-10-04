package com.pawamamama.game;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
//静态导入常量类所有的成员
import static com.pawamamama.util.Constant.*;

/**
 * Class: GameFrame
 *
 * <pre>游戏的主窗口类
 *      游戏展示内容都要在该类中实现
 * </pre>
 *
 * @author pawamamama
 * @date 2026/10/4
 */
@SuppressWarnings({"all" })
public class GameFrame extends Frame {
    //游戏状态
    public static int gameState;

    //初始化
    public GameFrame() throws HeadlessException {
        initGame();
        initFrame();
        initEventListener();
    }

    //窗口属性初始化
    private void initFrame() {
        //设置标题
        setTitle(GAME_TITLE);
        //设置窗口大小
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        //窗口居中
        setLocationRelativeTo(null);
        //设置大小不可改变
        setResizable(false);
        //设置窗口可见
        setVisible(true);
        //重绘
        repaint();
    }

    //对游戏状态进行初始化
    private void initGame() {
        gameState = STATE_MENU;
    }

    // 初始化事件监听
    private void initEventListener() {
        //注册监听事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0); // 关闭
            }
        });
    }

    /**
     * 是Frame类方法，继承下来的方法，该方法负责绘制工作
     * 不能主动调用，必须通过 repaint();去回调重绘方法
     *
     * @param g the specified Graphics window
     */
    public void paint(Graphics g) {
        g.setFont(FONT);
        //根据不同状态绘制不同内容
        switch (gameState) {
            case STATE_MENU:
                drawMenu(g);
                break;
            case STATE_HELP:
                drawHelp(g);
                break;
            case STATE_ABOUT:
                drawAbout(g);
                break;
            case STATE_RUN:
                drawRun(g);
                break;
            case STATE_OVER:
                drawOver(g);
                break;
        }
    }


    /**
     * 绘制菜单状态下的方法
     *
     * @param g 画笔,系统提供
     */
    private void drawMenu(Graphics g) {
        //对菜单进行绘制
        //绘制黑色背景
        g.setColor(Color.BLACK);
        //填充矩形
        g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
        //绘制字
        g.setColor(Color.WHITE);
        //字符宽度
        final int STR_WIDTH = 110;
        //居中宽度
        int x = FRAME_WIDTH - STR_WIDTH >> 1;
        //高度
        int y = FRAME_HEIGHT / 3;
        //行间距
        final int DIS = 50;
        for (int i = 0; i < MENUS.length; i++) {
            //递增行间距
            g.drawString(MENUS[i], x, y + DIS * i);
        }
    }
    private void drawHelp(Graphics g) {

    }

    private void drawAbout(Graphics g) {

    }

    private void drawRun(Graphics g) {

    }

    private void drawOver(Graphics g) {
    }

}