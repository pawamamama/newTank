package com.pawamamama.game;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
@SuppressWarnings({"all"})
public class GameFrame extends Frame implements Runnable {
    //游戏状态
    public static int gameState;
    //菜单指向
    private int menuIndex;
    //标题栏的高度
    public static int titleBarHeight;
    //坦克
    private Tank myTank;
    //初始化
    public GameFrame() throws HeadlessException {
        initGame();
        initFrame();
        initEventListener();
        //线程启动用于刷新
        new Thread(this).start();
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
        //求标题栏的高度
        titleBarHeight  = getInsets().top;


    }

    //对游戏状态进行初始化
    private void initGame() {
        gameState = STATE_MENU;
    }

    /**
     * 是Frame类方法，继承下来的方法，该方法负责绘制工作
     * 不能主动调用，必须通过 repaint();去回调重绘方法
     *
     * @param g the specified Graphics window
     */
    public void update(Graphics g) {
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
            //选中时颜色变化
            if (menuIndex == i) {//选中时字体为红色
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.WHITE);
            }
            //递增行间距
            g.drawString(MENUS[i], x, y + DIS * i);
        }
    }

    private void drawHelp(Graphics g) {

    }

    private void drawAbout(Graphics g) {

    }

    private void drawRun(Graphics g) {
        //对菜单进行绘制
        //绘制黑色背景
        g.setColor(Color.BLACK);
        //填充矩形
        g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
        //画坦克
        myTank.draw(g);
    }

    private void drawOver(Graphics g) {
    }

    // 初始化事件监听，按键处理
    private void initEventListener() {
        //注册监听事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0); // 关闭
            }
        });

        //添加按键监听事件
        addKeyListener(new KeyAdapter() {
            //按键被按下的时候被回调用的方法
            @Override
            public void keyPressed(KeyEvent e) {
                //被按下的键值
                int keyCode = e.getKeyCode();
                //不同的游戏状态，给出不同的方法处理
                switch (gameState) {
                    case STATE_MENU:
                        //当在菜单状态时候按下的按键处理
                        keyEventMenu(keyCode);
                        break;
                    case STATE_HELP:
                        keyEventHelp(keyCode);
                        break;
                    case STATE_ABOUT:
                        keyEventAbout(keyCode);
                        break;
                    case STATE_RUN:
                        keyEventRun(keyCode);
                        break;
                    case STATE_OVER:
                        keyEventOver(keyCode);
                        break;
                }
            }

            //按键被松开的时候
            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (gameState == STATE_RUN) {
                    keyReleasedEventRun(keyCode);
                }
            }

        });
    }

    private void keyEventOver(int keyCode) {
    }

    //按键松开，游戏中的处理方法
    private void keyReleasedEventRun(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                //松开就是静止状态
                myTank.setState(Tank.STATE_STAND);
        }
    }

    private void keyEventRun(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                myTank.setDir(Tank.DIR_UP);
                //坦克状态
                myTank.setState(Tank.STATE_MOVE);
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                myTank.setDir(Tank.DIR_DOWN);
                myTank.setState(Tank.STATE_MOVE);
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                myTank.setDir(Tank.DIR_LEFT);
                myTank.setState(Tank.STATE_MOVE);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                myTank.setDir(Tank.DIR_RIGHT);
                myTank.setState(Tank.STATE_MOVE);
                break;
        }

    }

    private void keyEventAbout(int keyCode) {

    }

    private void keyEventHelp(int keyCode) {

    }

    //菜单状态按键处理
    private void keyEventMenu(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                if (--menuIndex < 0) {//如果向上键是第一个自动跳到最后一个
                    menuIndex = MENUS.length - 1;//以后加入标题可以自动获取到正确数值
                }
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:

                if (++menuIndex > MENUS.length - 1) {//说明到最后一行了，直接跳到首行
                    menuIndex = 0;
                }
                break;
            case KeyEvent.VK_ENTER://开始游戏
                newGame();
                break;
        }

    }

    /**
     * 开始新游戏的方法
     */
    private void newGame() {
        gameState = STATE_RUN;
        //创建坦克对象，敌人的坦克对象
        myTank = new Tank(100, 100, Tank.DIR_DOWN);
    }

    @Override
    public void run() {
        while (true) {
            repaint();
            //线程休眠
            try {
                Thread.sleep(REPAINT_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

