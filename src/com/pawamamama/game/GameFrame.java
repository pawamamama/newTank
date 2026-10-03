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
@SuppressWarnings({"all"})
public class GameFrame extends Frame {
    //初始化
    public GameFrame() throws HeadlessException {
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
}