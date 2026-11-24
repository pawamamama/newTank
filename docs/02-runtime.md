# 运行时与渲染

> 返回 [README](../README.md) · 对应源码：`GameFrame.java`、`Bullet.java`、`Explode.java`、`util/*`

本篇讲程序跑起来之后的事：状态机怎么切换、30ms 的重绘线程怎么工作、双缓冲如何避免闪烁、一帧里谁先画谁后画。

## 1 启动流程

```text
GameMain.main()
  └─ new GameFrame()
       ├─ initGame()            建缓冲区、初始化状态
       ├─ initFrame()           设置窗口标题/大小/位置，setVisible(true)
       ├─ initEventListener()   注册窗口与键盘监听
       └─ new Thread(this).start()   启动重绘线程
```

`GameMain` 只有一行 `new GameFrame()`，不承载任何游戏逻辑。

## 2 游戏状态机

状态用 `Constant` 里的五个 `int` 表示：

```java
public static final int STATE_MENU  = 0;   // 主菜单
public static final int STATE_HELP  = 1;   // 帮助
public static final int STATE_ABOUT = 2;   // 关于
public static final int STATE_RUN   = 3;   // 游戏中
public static final int STATE_OVER  = 4;   // 游戏结束
```

`GameFrame` 用 `private static int gameState` 保存。绘制和按键都按它分叉：

| 状态 | 绘制 | 按键处理 |
|---|---|---|
| `STATE_MENU` | `drawMenu()` | `keyEventMenu()` |
| `STATE_HELP` | `drawHelp()`（空） | `keyEventHelp()`（空） |
| `STATE_ABOUT` | `drawAbout()`（空） | `keyEventAbout()`（空） |
| `STATE_RUN` | `drawRun()` | `keyEventRun()` / `keyEventReleasedRun()` |
| `STATE_OVER` | `drawOver()` | `keyEventOver()` |

> 当前只有 `MENU → RUN → OVER → MENU` 这条主路径。菜单的第 2~5 项（继续、帮助、关于、退出）还没有分发，`ENTER` 无条件调用 `newGame()`。这属于「待完成」，不是 bug。

## 3 重绘线程与双缓冲

### 3.1 30ms 重绘线程

`GameFrame` 实现 `Runnable`，构造器里启动线程：

```java
public void run() {
    while (true) {
        repaint();                          // 登记一次重绘请求
        Thread.sleep(REPAINT_INTERVAL);     // 30ms
    }
}
```

- `repaint()` 不直接画，它把请求交给 AWT 的事件分发线程（EDT）。
- 真正的绘制入口是 `update(Graphics)`，由 AWT 回调。
- 逻辑更新和碰撞结算也都挂在绘制流程里，所以「一帧」等于「一次 `update`」。

### 3.2 双缓冲

如果直接在屏幕画笔上「先涂黑、再画」，用户能看见中间过程，窗口会闪。现在先画到离屏图片，再一次性贴到屏幕：

```java
BufferedImage bufferedImage =
        new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);

public void update(Graphics g1) {
    Graphics2D screen = (Graphics2D) g1;
    screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

    Graphics2D g = (Graphics2D) bufferedImage.getGraphics();   // 缓冲区画笔
    switch (gameState) { /* 全部画到缓冲区 */ }

    screen.drawImage(bufferedImage, 0, 0, WIN_WIDTH, WIN_HEIGHT, this);
}
```

两个画笔不要混：

| 变量 | 是什么 | 画到哪 |
|---|---|---|
| `g1` / `screen` | `update` 参数，屏幕画笔 | 真实屏幕 |
| `g` | `bufferedImage.getGraphics()` | 内存里的图 |

> 不要调用 `super.update(g1)`：父类默认会先涂背景再调 `paint()`，双缓冲会失效。

### 3.3 最近邻缩放

```java
g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
```

`drawImage` 缩放默认用双线性插值，会把像素画的硬边晕成灰边。最近邻直接复制最近的像素，边缘保持锋利。

**缓冲区和屏幕两层都要设**：缓冲区 900×700 → 屏幕 1350×1050 还要再放大 1.5 倍，只设一层仍会糊。

## 4 一帧的绘制顺序

`drawRun()` 的顺序是固定的：

```java
g.setColor(Color.BLACK);
g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);   // ① 清屏
gameMap.draw(g);                               // ② 地图（最底层）
drawEnemyTanks(g);                             // ③ 敌人
myTank.draw(g);                                // ④ 玩家
drawExplodes(g);                               // ⑤ 爆炸 + 清理碎块
bulletCollideTank();                           // ⑥ 子弹 ↔ 坦克
bulletCollideMapTile();                        // ⑦ 子弹 ↔ 块、坦克 ↔ 块
```

要点：

- **先画地图再画坦克**：AWT 没有 z 序，后画的覆盖先画的。
- **碰撞在绘制之后**：这一帧先呈现当前状态，再结算命中；命中的子弹和碎块要到下一帧才消失，视觉延迟约 30ms。
- **`clearDestoryTile()` 在 `drawExplodes()` 里**，不在碰撞方法末尾。所以块被打碎后，本帧仍会画一次，下一帧才真正消失。
- `drawExplodes()` 先画所有敌人的爆炸，再清理碎块，最后画玩家的爆炸。

## 5 按键模型

```text
KeyAdapter
  ├─ keyPressed(e)  → 按 gameState 分发
  │                    ├─ 方向键：setDir + setState(STATE_MOVE)
  │                    └─ 空格  ：myTank.fire()
  └─ keyReleased(e) → 仅 STATE_RUN → setState(STATE_STAND)
```

**按下和松开必须分开处理**：按下只打开「移动开关」，松开才关掉。这样按住方向键时，每一帧 `logic()` 都会继续走 `mathMove()`。

开火是「按下触发」而不是「按住连发」：`fire()` 内部有 125ms 冷却，即使按住空格，射速也不会超过每秒 8 发。

## 6 窗口与坐标系

```java
setTitle(GAME_TITLE);
setSize(WIN_WIDTH, WIN_HEIGHT);    // 窗口外框 1350×1050
setLocationRelativeTo(null);
setResizable(false);
setVisible(true);
titleBarHeight = (int) (getInsets().top / MULTIPLE);
```

- `getInsets()` 必须在 `setVisible(true)` 之后调用，否则返回 0。
- 逻辑画布 900×700 从标题栏下方开始，`Graphics` 的 y=0 在可绘区顶部。
- `titleBarHeight` 是标题栏的逻辑高度，坦克上边界和子弹上出界都用它。

```text
窗口外框 y=0   ┌──────────────────┐
               │  标题栏           │  getInsets().top
画布原点 y=0   ├──────────────────┤  Graphics 原点
               │   可绘区 900×700  │
               └──────────────────┘
```

> `titleBarHeight` 被 `Tank`、`GameMap`、`Bullet` 直接读取，形成了 `game ↔ Tank/map` 的循环依赖。挪进 `Constant` 可以断开。

## 7 对象池

### 7.1 为什么需要

子弹和爆炸是高频创建、高频销毁的对象。对象池预先创建一批，用完归还，避免反复 `new` 触发 GC。

### 7.2 四个池对照

| 池 | 初始 / 上限 | 池化对象 | 归还时重置 |
|---|---|---|---|
| `BulletsPool` | 200 / 300 | `Bullet` | 不重置（`fire()` 逐字段覆盖） |
| `ExplodesPool` | 10 / 30 | `Explode` | 不重置（`MyUtil.addExplode()` 重置） |
| `EnemiesPool` | 10 / 15 | `EnemyTank` | 不重置（`createTank()` 重置） |
| `TilesPool` | 200 / 400 | `MapTile` | **`theReturn()` 调 `tile.reset()`** |

### 7.3 重置职责

池只负责「借出 / 收回」，重置由最了解字段使用方负责：

- `Bullet`：`fire()` 每次设置 `atk/x/y/dir/color/visible`，池里旧值会被全部覆盖。
- `Explode`：`MyUtil.addExplode()` 统一重置 `visible/index/x/y/endIndex`。
- `EnemyTank`：`createTank()` 重置坐标、方向、状态、血量、AI 计时、弹夹。
- `MapTile`：`TilesPool.theReturn()` 调 `reset()`，清 `hp/type/hitResult/isVisible`；坐标不清，由 `GameMap` 重新设置。

> `EnemyTank.createTank()` 目前没有重置 `lastFireTime`、`color`、`name`。前者影响很小（旧时间戳早于当前，冷却必然已过），后两者会导致复用敌人沿用上一世的名字和颜色。已记入待修清单。

## 8 工具类 MyUtil

### 8.1 随机数

```java
public static int getRandomNumber(int min, int max) {
    return (int) (Math.random() * (max - min) + min);
}
```

区间是 `[min, max)`，`max` 取不到。`getRandomColor()` 传 256 才能取到 255；`getRandomName()` 传 `NAMES.length` 才能覆盖全部下标。

### 8.2 碰撞判定

```java
public static boolean isRectangleCollision(int rectX, int rectY, int radius,
                                          int pointX, int pointY) {
    return Math.abs(rectX - pointX) <= radius
        && Math.abs(rectY - pointY) <= radius;
}
```

这是「点是否落在方框内」的判定，方框边长 `2 × radius`。必须取绝对值，否则点在一侧时距离为负，永远成立。

### 8.3 扫掠碰撞

```java
public static boolean sweptCollision(int rectX, int rectY, int rectRadius,
                                     int pointX, int pointY, int dir, int speed) {
    int oldX = pointX, oldY = pointY;
    for (int step = 0; step < speed; step++) {
        switch (dir) {
            case Tank.DIR_UP:    oldY--; break;
            case Tank.DIR_DOWN:  oldY++; break;
            case Tank.DIR_LEFT:  oldX--; break;
            case Tank.DIR_RIGHT: oldX++; break;
        }
        if (isRectangleCollision(rectX, rectY, rectRadius, oldX, oldY)) {
            return true;
        }
    }
    return false;
}
```

子弹每帧走 8 像素，只查终点可能「这一帧在块前、下一帧在块后」。扫掠把这一帧的整条路径逐像素查一遍，保证不穿透。

### 8.4 图片与爆炸

- `createImg(path)`：统一 `Toolkit.getDefaultToolkit().getImage(path)`。
- `addExplode(frames, x, y)`：从 `ExplodesPool` 借爆炸并重置五个字段，`endIndex = frames * SLOW`。

## 9 子弹与爆炸

### 9.1 Bullet

| 字段 | 含义 |
|---|---|
| `x, y` | 中心点坐标 |
| `speed` | 每帧位移，`DEFAULT_SPEED = Tank.DEFAULT_SPEED * 2 = 8` |
| `dir` | 飞行方向 |
| `atk` | 伤害快照，发射时从坦克复制 |
| `color` | 颜色快照 |
| `visible` | 是否在飞；飞出屏幕后置 `false` |

子弹没有「死亡」概念，只有「在飞」和「出界」。`draw()` 里先判 `visible`，不可见就冻结坐标；`Tank.drawBullets()` 每帧回收不可见子弹。

### 9.2 Explode

- 5 张不同尺寸的序列帧，用统一缩放倍数 `(RADIUS * 2) / 16 = 2`。
- `SLOW = 3`：每张图停留 3 次刷新。
- `endIndex` 由调用方传入，`MyUtil.addExplode()` 已经乘过 `SLOW`。
- 播完置 `isVisible(false)`，下一帧由 `Tank.deawExlpodes()` 归还池子。

## 10 全局常量表

`Constant.java` 只放与窗口、菜单、敌人节奏相关的常量；坦克和地图的常量在各自类里。

| 常量 | 值 | 用途 |
|---|---|---|
| `GAME_TITLE` | 坦克大战第二版:pawamamama | 窗口标题 |
| `FRAME_WIDTH` / `FRAME_HEIGHT` | 900 / 700 | 逻辑画布 |
| `MULTIPLE` | 1.5 | 显示缩放倍数 |
| `WIN_WIDTH` / `WIN_HEIGHT` | 1350 / 1050 | 实际窗口尺寸 |
| `STATE_MENU` ~ `STATE_OVER` | 0 ~ 4 | 游戏状态 |
| `MENUS` | 5 个字符串 | 菜单项 |
| `OVER_STR0` / `OVER_STR1` | ESC / ENTER 提示 | 结束界面两行文字 |
| `FONT` / `SMALL_FONT` | 宋体 24 / 16 加粗 | 全局字体 / 名字字体 |
| `REPAINT_INTERVAL` | 30 | 重绘间隔（毫秒） |
| `ENEMY_MAX_COUNT` | 10 | 同屏敌人上限 |
| `ENEMY_BORN__INTERVAL` | 5000 | 敌人生成间隔（毫秒，中间是两个下划线） |
| `ENEMY_AI_INTERVAL` | 3000 | 敌人 AI 决策间隔 |
| `ENEMY_FIRE_PERCENT` | 0.03 | 每帧尝试开火的概率 |

---

下一篇：[坦克模型](03-tank.md) · 返回 [README](../README.md)
