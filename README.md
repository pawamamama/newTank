<h1 align="center">坦克大战第二版说明</h1>

<h2 align="center">补全关卡，障碍物，动态效果 ai敌人加强</h2>

> 本文档按 `src` 目录的**当前实际代码**编写，不是设想稿。
> 已实现的部分如实记录，未实现或有问题的部分单独列在「已知问题」里。

---

## 一、项目概览

| 项 | 内容 |
|---|---|
| 项目名 | newTank（坦克大战第二版） |
| 语言 / 图形库 | Java + AWT（`java.awt`） |
| 入口类 | `com.pawamamama.app.GameMain` |
| 根包 | `com.pawamamama` |
| 窗口尺寸 | 900 × 700（`Constant.FRAME_WIDTH / FRAME_HEIGHT`） |
| 刷新频率 | 30 ms 一次（约 33 FPS） |
| 目标玩法 | 经典坦克大战：移动、射击、敌人 AI、关卡与障碍物 |

---

## 二、目录结构与职责

```
src/
└── com/pawamamama/
    ├── app/
    │   └── GameMain.java     程序入口，只有一行 new GameFrame()
    ├── game/
    │   ├── GameFrame.java    游戏主窗口：窗口初始化、状态机、绘制分发、按键、重绘线程
    │   └── Tank.java         坦克模型：坐标、方向、状态、绘制、移动逻辑
    └── util/
        ├── Constant.java     全局常量（尺寸、游戏状态、菜单文案、字体、刷新间隔）
        └── MyUtil.java       工具类（随机数、随机颜色）
```

### 分层思想

```
        ┌─────────────────────────────────────────┐
        │  app 层   GameMain      只负责启动        │
        └───────────────────┬─────────────────────┘
                            │ new
        ┌───────────────────▼─────────────────────┐
        │  game 层  GameFrame                      │
        │           ├─ 窗口与渲染循环（View）        │
        │           ├─ 游戏状态机（Controller）      │
        │           ├─ 按键分发                    │
        │           └─ 持有 Tank 对象               │
        │           Tank                          │
        │           ├─ 数据：坐标/血量/方向/状态     │
        │           ├─ draw()  绘制                │
        │           └─ logic() 移动                │
        └───────────────────┬─────────────────────┘
                            │ 依赖
        ┌───────────────────▼─────────────────────┐
        │  util 层  Constant  常量池                │
        │           MyUtil    通用工具              │
        └─────────────────────────────────────────┘
```

依赖方向是**单向**的：`app → game → util`。`util` 不反过来依赖 `game`，所以工具类可以被任何地方复用。

---

## 三、构建流程

### 1. 窗口初始化

对游戏窗口进行初始化，设置标题、大小、坐标、窗口的事件监听。

- 游戏窗口使用 `java.awt.Frame`（注意：**不是 JFrame**，本项目直接用 AWT 的 `Frame`）
- 窗口属性初始化：`initFrame()`
- 标题、大小、坐标等常量使用常量类管理
- 静态导入：`import static com.pawamamama.util.Constant.*;`
- 窗口的事件监听：`initEventListener()`

### 2. 游戏菜单的显示，游戏菜单按键控制

游戏被分成了若干个游戏状态

```java
// ------------------游戏菜单-------------------------
public static final int STATE_MENU = 0;   // 游戏主菜单
public static final int STATE_HELP = 1;   // 帮助界面
public static final int STATE_ABOUT = 2;  // 关于界面
public static final int STATE_RUN = 3;    // 游戏运行中
public static final int STATE_OVER = 4;   // 游戏结束
```

#### 在不同的状态下，绘制不同的内容，按键有不同的处理方式 keycode是整数数值

- 在不同状态时按下按键要分方法来去处理
- 菜单处理 `keyEventMenu`
- 帮助界面 `keyEventHelp`
- 关于界面 `keyEventAbout`
- 游戏运行时 `keyEventRun`
- 游戏结束 `keyEventOver`
- 在 GameFrame 类里使用 `public static int gameState;` 管理游戏状态
- 在 GameFrame 类里使用 `private int menuIndex;` 管理开始界面被选中时候的项

#### 调整游戏窗口的重新绘制的机制

- FPS：frame per second 每秒帧数
- 每秒固定刷新整个窗口，30ms 刷新一次，对整个窗口进行重绘
- 单独启动一个线程用于窗口重绘，`GameFrame` 类使用 Runnable 接口的 run 方法实现线程
- 在构造器中使用 `new Thread(this).start();` 启动重绘线程

---

## 四、核心架构

### 4.1 游戏状态机（整个项目的骨架）

`GameFrame.gameState` 是一个 `public static int`，取值来自 `Constant`：

```java
public static final int STATE_MENU  = 0;   // 游戏主菜单
public static final int STATE_HELP  = 1;   // 帮助界面
public static final int STATE_ABOUT = 2;   // 关于界面
public static final int STATE_RUN   = 3;   // 游戏运行中
public static final int STATE_OVER  = 4;   // 游戏结束
```

**所有绘制和按键都靠这个值分叉**，两个对称的 `switch`：

```
                    gameState
                        │
        ┌───────────────┼────────────────┬──────────────┐
        ▼               ▼                ▼              ▼
   update(g)      keyPressed()     keyReleased()    其它逻辑
        │               │
   ┌────┴────┐     ┌────┴─────┐
   ▼    ▼    ▼     ▼     ▼    ▼
 drawMenu  drawHelp ...  keyEventMenu  keyEventHelp ...
```

用整数常量而不是枚举，是为了跟课阶段简单；条件分支用 `switch`，比一堆 `if` 好读。

### 4.2 渲染循环

`GameFrame implements Runnable`，构造器里直接起线程：

```java
public GameFrame() {
    initGame();                // 1. 初始化游戏状态
    initFrame();               // 2. 初始化窗口
    initEventListener();       // 3. 注册监听
    new Thread(this).start();  // 4. 启动重绘线程
}
```

`run()` 是死循环：

```java
public void run() {
    while (true) {
        repaint();                      // 请求重绘
        Thread.sleep(REPAINT_INTERVAL); // 休眠 30ms
    }
}
```

关键点：

- **`repaint()` 不能换成 `paint()` 直接调用**。`repaint()` 只是"登记一次重绘请求"，由 AWT 的事件分发线程（EDT）稍后统一回调 `update(Graphics)`。
- **`update(Graphics)` 才是被系统回调的入口**，由它再按 `gameState` 分发到 `drawMenu / drawRun / ...`。
- `repaint()` 是线程安全的（内部会 post 到 EDT），所以可以在自己的线程里循环调用。

### 4.3 绘制的「清屏 + 重画」模式

每一帧都是全屏重画，`drawRun` 里必须先涂背景：

```java
private void drawRun(Graphics g) {
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);  // ① 清屏
    myTank.draw(g);                                // ② 画对象
}
```

**顺序不能反**，否则上一帧的坦克会残留在屏幕上（拖影）。

### 4.4 按键事件的「按下 / 松开」模型

这是坦克能"持续移动"的关键设计：

| 事件 | 回调 | 做的事 |
|---|---|---|
| 按下方向键 | `keyPressed` → `keyEventRun` | `setDir(方向)` + `setState(STATE_MOVE)` |
| 松开方向键 | `keyReleased` → `keyReleasedEventRun` | `setState(STATE_STAND)` |

**注意**：`setDir()` 只改变朝向，**不产生位移**。位移是每帧 `logic() → mathMove()` 干的。

所以坦克的移动其实是两个独立维度：

```
        dir（方向）              state（状态）
           │                        │
   决定往哪个方向动          决定「动不动」
           └────────┬───────────────┘
                    ▼
            每帧 logic() 里
            state == MOVE 才调 mathMove()
```

### 4.5 常量集中管理

`Constant` 是 `final class` + 全部 `static final`，并且被**静态导入**：

```java
import static com.pawamamama.util.Constant.*;
```

效果是代码里直接写 `FRAME_WIDTH`，不用 `Constant.FRAME_WIDTH`。

好处：所有魔法数字集中一处，改一个值全项目生效。
代价：静态导入后看不出来源，读代码时要知道它从哪来。

---

## 五、各文件详解

### 5.1 `GameMain.java` —— 入口

```java
public class GameMain {
    public static void main(String[] args) {
        new GameFrame();
    }
}
```

只有一行。`new GameFrame()` 的构造器里干了所有初始化，所以**不需要保存引用**——对象被窗口系统持有，不会被 GC。

### 5.2 `Constant.java` —— 常量池

| 常量 | 值 | 用途 |
|---|---|---|
| `GAME_TITLE` | 坦克大战第二版:pawamamama | 窗口标题 |
| `FRAME_WIDTH` | 900 | 窗口宽 |
| `FRAME_HEIGHT` | 700 | 窗口高 |
| `STATE_MENU` ~ `STATE_OVER` | 0 ~ 4 | 游戏状态 |
| `MENUS` | 5 个字符串 | 菜单项，用数组便于遍历 |
| `FONT` | 宋体 24 加粗 | 全局字体 |
| `REPAINT_INTERVAL` | 30 | 重绘间隔（毫秒） |

`MENUS` 用数组而不是 5 个变量，好处是配合 `MENUS.length` 做循环和循环翻页：

```java
if (--menuIndex < 0) {
    menuIndex = MENUS.length - 1;   // 到顶了就跳到最后一项
}
```

以后加/删菜单项，这段边界逻辑**不用改**。

### 5.3 `MyUtil.java` —— 工具类

标准工具类写法，两个特征：

```java
public class MyUtil {
    private MyUtil() {}   // ① 私有构造器：禁止 new
    public static final int getRandomNumber(int min, int max) { ... } // ② 全 static
}
```

#### getRandomNumber

```java
public static final int getRandomNumber(int min, int max) {
    return (int) (Math.random() * (max - min) + min);
}
```

**区间是 `[min, max)`：`min` 能取到，`max` 取不到。**

推导：

```
Math.random()             →  [0.0, 1.0)
Math.random() * (max-min) →  [0, max-min)
                + min     →  [min, max)
(int) 强转（截断）         →  取整，所以 max 永远到不了
```

例：`getRandomNumber(0, 100)` → `0 ~ 99`。

#### getRandomColor

```java
public static final Color getRandomColor() {
    return new Color(
            getRandomNumber(0, 256),   // 256 → 结果 0~255，正好是 RGB 合法范围
            getRandomNumber(0, 256),
            getRandomNumber(0, 256));
}
```

这里传 **256** 而不是 255：因为区间右开，`getRandomNumber(0, 255)` 只能取到 `0~254`，永远取不到 255 这个分量。

> 小知识：`Color` 的整数构造器每个分量合法范围是 `0 ~ 255`，超出会抛 `IllegalArgumentException`。

### 5.4 `GameFrame.java` —— 主窗口

继承 `java.awt.Frame` 并实现 `Runnable`。

#### 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `gameState` | `public static int` | 全局游戏状态，`static` 因为整个游戏只有一份 |
| `menuIndex` | `private int` | 菜单当前选中项 |
| `titleBarHeight` | `public static int` | 标题栏高度，见下方"窗口坐标系" |
| `myTank` | `private Tank` | 玩家坦克，`newGame()` 时才创建 |

#### initFrame() 与窗口坐标系（重要）

```java
setSize(FRAME_WIDTH, FRAME_HEIGHT);  // 设的是「窗口外框」尺寸
setResizable(false);                 // 必须在 setVisible 之前，它会改变边框厚度
setLocationRelativeTo(null);         // 居中
setVisible(true);                    // ★ 必须先显示
titleBarHeight = getInsets().top;    // ★ 之后才拿得到真实边框
```

**`getInsets()` 必须在 `setVisible(true)` 之后调用**，否则返回全 0。

原因：AWT 的 `Frame` 是重量级组件，背后有一个真实的操作系统窗口（native peer）。
`new Frame()` 时只有 Java 对象；`setVisible(true)` 才创建系统窗口，系统此时才告诉 Java「我这里标题栏 31px」。

坐标系关系：

```
窗口外框 y=0   ┌──────────────────┐
               │  标题栏           │  ← getInsets().top，约 31px
画布原点 y=0   ├──────────────────┤  ← Graphics 的原点在这里
               │                  │
               │   可绘区          │  ≈ 900 × 661
               │                  │
可绘区底       ├──────────────────┤  ← 约 y = 661
               │  底边框           │  ← getInsets().bottom，约 8px
窗口外框底     └──────────────────┘  ← 700
```

**这正是 `titleBarHeight` 存在的意义**：窗口外框 700 高，但可绘区只有约 661 高。
如果用 `FRAME_HEIGHT` 当坦克的边界，坦克会跑到可绘区外面被裁掉。
所以 `Tank.mathMove()` 里移动时要减 `titleBarHeight`。

#### update() —— 绘制分发入口

```java
public void update(Graphics g) {
    g.setFont(FONT);
    switch (gameState) {
        case STATE_MENU:  drawMenu(g);  break;
        case STATE_HELP:  drawHelp(g);  break;
        case STATE_ABOUT: drawAbout(g); break;
        case STATE_RUN:   drawRun(g);   break;
        case STATE_OVER:  drawOver(g);  break;
    }
}
```

`update()` 是 `Frame` 从 `Component` 继承来的方法，**不能主动调**，只能通过 `repaint()` 触发回调。

#### drawMenu() —— 菜单绘制

```java
g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);   // 黑底
int x = FRAME_WIDTH - STR_WIDTH >> 1;          // 水平居中
int y = FRAME_HEIGHT / 3;                      // 起始高度
for (int i = 0; i < MENUS.length; i++) {
    // 选中的画红色，其余白色
    g.drawString(MENUS[i], x, y + DIS * i);    // 逐行下移
}
```

`>> 1` 等价于 `/ 2`（整型除法）。这里用 `>>` 只是习惯写法，用 `/ 2` 更好读。

#### 按键监听：三层分发

```
KeyAdapter（匿名内部类）
    │
    ├─ keyPressed(e)  → 按 gameState 分发 → keyEventMenu/Help/About/Run/Over
    │                                        └→ 只调 setDir + setState
    │
    └─ keyReleased(e) → 只有 STATE_RUN 时 → keyReleasedEventRun
                                            └→ setState(STATE_STAND)
```

用 `KeyAdapter` 而不是 `KeyListener`，是因为接口有 3 个方法，适配器类已经给了空实现，只需重写关心的。
用**匿名内部类**是因为这个监听器只在这个窗口用一次，没必要单独建类。

`keyEventMenu` 里的循环翻页：

```java
case VK_UP:    if (--menuIndex < 0) menuIndex = MENUS.length - 1; break;
case VK_DOWN:  if (++menuIndex > MENUS.length - 1) menuIndex = 0; break;
case VK_ENTER: newGame(); break;
```

`newGame()` 做的事：

```java
gameState = STATE_RUN;                          // 切状态
myTank = new Tank(100, 100, Tank.DIR_DOWN);     // 创建玩家坦克
```

`myTank` 此刻之前是 `null`，所以 `drawRun` 必须在 `newGame` 之后才会被调到（因为只有 `STATE_RUN` 才走 `drawRun`）。

### 5.5 `Tank.java` —— 坦克模型

#### 常量

| 分类 | 常量 | 值 |
|---|---|---|
| 方向 | `DIR_UP / DIR_DOWN / DIR_LEFT / DIR_RIGHT` | 0 / 1 / 2 / 3 |
| 状态 | `STATE_STAND / STATE_MOVE / STATE_DIE` | 0 / 1 / 2 |
| 尺寸 | `RADIUS` | 20 |
| 速度 | `speed` | 4（每帧 4 像素） |
| 生命 | `DEFAULT_HP` | 1000 |

> ⚠️ **方向的数值顺序和旋转角度顺序不一致**：值是「上、下、左、右」，而按顺时针排是「上、右、下、左」。
> 如果以后改用旋转画法，**不能写 `dir * 90`**，必须用 `switch` 映射。见「后续方向」一节。

#### 数据字段

| 字段 | 说明 |
|---|---|
| `x, y` | 坦克**中心点**坐标（不是左上角） |
| `hp` | 血量，初值 `DEFAULT_HP` |
| `atk / def` | 攻击力 / 防御力（暂未使用） |
| `dir` | 当前朝向 |
| `state` | 当前状态，初值 `STATE_STAND` |
| `color` | 随机颜色，构造器里生成 |
| `bullets` | 炮弹列表（`TODO`，暂未实现） |

`color` **必须是实例字段、不能加 static**：加了 static 所有坦克会共用一个颜色。

#### draw() —— 绘制

```java
public void draw(Graphics g) {
    logic();                                    // ① 先处理逻辑
    g.setColor(color);
    g.fillOval(x - RADIUS, y - RADIUS, RADIUS << 1, RADIUS << 1);  // ② 车身（圆）

    int endX = x, endY = y;
    switch (dir) {                              // ③ 按方向算炮筒终点
        case DIR_UP:    endY = y - RADIUS * 2; break;
        case DIR_DOWN:  endY = y + RADIUS * 2; break;
        case DIR_LEFT:  endX = x - RADIUS * 2; break;
        case DIR_RIGHT: endX = x + RADIUS * 2; break;
    }

    Graphics2D g2d = (Graphics2D) g.create();    // ④ 拿画笔副本
    g2d.setStroke(new BasicStroke(3));           //    设线宽 3
    g2d.setColor(color);
    g2d.drawLine(x, y, endX, endY);              // ⑤ 画炮筒
    g2d.dispose();                               // ⑥ 还回画笔
}
```

三个关键点：

**① `x - RADIUS, y - RADIUS` 为什么要减半径**

`fillOval` 的参数是**外接矩形的左上角**，不是圆心。而游戏逻辑里 `(x, y)` 表示坦克中心。
所以要把矩形往左上挪半个身位，中心才落在 `(x, y)`：

```
        x-R          x          x+R
         │           │           │
   y-R ──┼───────────┼───────────┼──
        ┌───────────────────────┐
        │           •(x,y)      │
        └───────────────────────┘
   y+R ──┴───────────┴───────────┴──
```

宽高用 `RADIUS << 1`（= `RADIUS * 2`），所以直径 40。

**② 为什么用 `Graphics2D` 画炮筒**

`Graphics.drawLine` **画不了粗线**——`Graphics` 没有线宽概念，永远是 1 像素。
要加粗必须换成 `Graphics2D` + `BasicStroke`：

```java
Graphics2D g2d = (Graphics2D) g;           // g 本身就是 Graphics2D，强转必成功
g2d.setStroke(new BasicStroke(3));         // 线宽 3
```

**③ 为什么 `g.create()` + `g.dispose()`**

`setStroke` 改的是画笔状态，会**影响后续所有绘制**。`create()` 拿一份副本，在副本上改，`dispose()` 扔掉副本，原画笔不受影响。

> 补充：`setStroke` 只对 `drawXxx` 系列生效，**对 `fillXxx` 完全无效**。
> 所以画炮筒用的 `drawLine` 能加粗，画车身用的 `fillOval` 加不了粗（粗细由宽高决定）。

#### logic() —— 逻辑入口

```java
private void logic() {
    switch (state) {
        case STATE_STAND: break;              // 静止：什么都不做
        case STATE_MOVE:  mathMove(); break;  // 移动：更新坐标
        case STATE_DIE:   break;              // 死亡：什么都不做
    }
}
```

**状态驱动的移动**：只有 `state == STATE_MOVE` 才会真正改变坐标。

#### mathMove() —— 边界限制（当前使用）

```java
private void mathMove() {
    switch (dir) {
        case DIR_UP:
            y = Math.max(GameFrame.titleBarHeight + RADIUS, y - speed);
            break;
        case DIR_DOWN:
            y = Math.min(FRAME_HEIGHT - RADIUS, y + speed);
            break;
        case DIR_LEFT:
            x = Math.max(RADIUS, x - speed);
            break;
        case DIR_RIGHT:
            x = Math.min(FRAME_WIDTH - RADIUS, x + speed);
            break;
    }
}
```

**为什么用 `Math.max` / `Math.min` 而不是 `if` 判断**

`if` 版（`move()` 里那套）是「先判断、再移动」，判断用的是**移动前**的位置，移完可能越过边界：

```
RADIUS=20, speed=4
移动前 y = 22  →  if (22 > 20) 成立  →  y = 18  →  冲出 2 像素
```

clamp 版直接「算完再夹紧」：

```
y = Math.max(20, 22 - 4) = Math.max(20, 18) = 20   →  正好贴边，不冲出
```

规律：

| 移动方向 | 用的方法 | 边界 |
|---|---|---|
| 往上 / 往左（坐标变小） | `Math.max` 兜住下限 | `RADIUS`（+ `titleBarHeight`） |
| 往下 / 往右（坐标变大） | `Math.min` 兜住上限 | `尺寸 - RADIUS` |

---

## 六、一次按键到屏幕的完整链路

按一下 ↑ 键，发生了什么：

```
① 用户按 ↑
      │
      ▼
② EDT 回调 GameFrame 里 KeyAdapter.keyPressed(e)
      │  keyCode = VK_UP
      ▼
③ switch (gameState)  →  STATE_RUN  →  keyEventRun(keyCode)
      │
      ▼
④ myTank.setDir(Tank.DIR_UP)         // 只改朝向
   myTank.setState(Tank.STATE_MOVE)  // 打开"移动开关"
      │
      │  ← 这一步结束，屏幕还没变
      ▼
⑤ 30ms 后，重绘线程执行 repaint()
      │
      ▼
⑥ AWT 回调 GameFrame.update(g)
      │  gameState == STATE_RUN
      ▼
⑦ drawRun(g)
      ├─ g.fillRect(...)  清屏
      └─ myTank.draw(g)
             │
             ▼
⑧ Tank.draw(g)
      ├─ logic()           → state==MOVE → mathMove()
      │                        └─ y -= 4（带边界夹紧）
      ├─ g.fillOval(...)   画圆
      └─ g2d.drawLine(...) 画炮筒
      │
      ▼
⑨ 屏幕上的坦克往上挪了 4 像素
```

松开 ↑ 键：

```
keyReleased → gameState==STATE_RUN → keyReleasedEventRun
            → myTank.setState(STATE_STAND)
            → 之后 logic() 走 STATE_STAND 分支，什么都不做，坦克停住
```

---

## 七、设计要点小结

| 设计 | 做法 | 好处 |
|---|---|---|
| 状态机 | `gameState` + `switch` | 各界面互不干扰，加新界面只加一个 case |
| 渲染循环 | 独立线程 + `repaint()` | 逻辑和绘制解耦，帧率可控 |
| 方向与状态分离 | `dir` 管朝向、`state` 管动不动 | 转向和移动互不耦合，好扩展（比如转向但不移动） |
| 常量集中 | `Constant` + 静态导入 | 改数值不用满项目找 |
| 工具类 | 私有构造器 + 全 static | 语义清晰，不会被误 new |
| 坐标用中心点 | 绘制时 `-RADIUS` 换算成左上角 | 旋转、碰撞检测都方便（以后用得上） |
| 边界用 clamp | `Math.max` / `Math.min` | 不会越过边界，能精确贴边 |

---

## 八、已知问题（当前代码状态）

> 这些是读代码发现的，**不影响你现在跑，但会影响后续扩展**，按优先级排列。

### 8.1 `move()` 是死代码

`logic()` 现在调的是 `mathMove()`，`move()`（`if` 版本）已经没人调用。

建议：确认不再需要对照后删掉，或者改名 `moveIfStyle()` 加注释说明是历史版本。留两份生效逻辑容易踩错。

### 8.2 向下移动的边界没减 `titleBarHeight`

```java
y = Math.min(FRAME_HEIGHT - RADIUS, y + speed);   // 用的是窗口外框高度
```

可绘区高度只有约 `700 - 31 - 8 = 661`，所以坦克往下走到 `y > 641` 后会被底边框裁掉一截。

对比一下向上移动已经处理了：

```java
y = Math.max(GameFrame.titleBarHeight + RADIUS, y - speed);   // 已减
```

**对称的修法**：

```java
case DIR_DOWN:
    y = Math.min(FRAME_HEIGHT - GameFrame.titleBarHeight - RADIUS, y + speed);
    break;
```

更精确的话还应该再减 `getInsets().bottom`（约 8px），但那需要额外传值，可以先不管。

### 8.3 `logic()` 在 `draw()` 里被调用，职责混了

```java
public void draw(Graphics g) {
    logic();   // ← 绘制方法里推进了游戏逻辑
    ...
}
```

能跑，但语义不对：`draw` 应该只负责画。

**副作用**：如果哪天在"帮助界面"或"暂停"时也调了 `myTank.draw(g)`，坦克会莫名其妙地继续移动。

**建议拆开**：

```java
// GameFrame.drawRun
private void drawRun(Graphics g) {
    myTank.logic();       // 先算（logic 要改成 public）
    g.fillRect(...);
    myTank.draw(g);       // 再画
}
```

### 8.4 炮筒会超出窗口

炮筒长度是 `RADIUS * 2 = 40`，比车身半径还长一倍。坦克贴着上边界时（`y ≈ 51`），炮筒顶端在 `y - 40 = 11`，还在画布内——**目前刚好安全**。

但如果以后把边界改成 `titleBarHeight + RADIUS * 2`，或者炮筒画得更长，就会出现"炮筒伸出窗口"的情况。

这属于设计取舍（很多坦克游戏允许炮筒和墙重叠），想避免就让边界按炮筒长度算。

### 8.5 `bullets` 还没实现

```java
private List bullets = new ArrayList();   // 裸类型，会有编译警告
```

等做射击功能时，建议泛型化：`private List<Bullet> bullets = new ArrayList<>();`

---

## 九、后续方向（代码里已经埋好的接口）

### 9.1 用旋转实现四方向（如果要做）

现在的做法是 `switch (dir)` 算炮筒终点，四个方向要写四段。改成旋转可以只画一次：

```java
Graphics2D g2d = (Graphics2D) g.create();
g2d.rotate(Math.toRadians(getAngle()), x, y);   // 绕坦克中心旋转
// 之后一律按"朝上"画
g2d.fillOval(x - RADIUS, y - RADIUS, RADIUS << 1, RADIUS << 1);
g2d.drawLine(x, y, x, y - RADIUS - 10);
g2d.dispose();
```

角度映射（**必须用 switch，不能写 `dir * 90`**）：

| 方向 | 常量值 | 角度 |
|---|---|---|
| `DIR_UP` | 0 | 0° |
| `DIR_RIGHT` | 3 | 90° |
| `DIR_DOWN` | 1 | 180° |
| `DIR_LEFT` | 2 | 270° |

注意 `rotate` 的 pivot 用 `(x, y)` 正好——因为绘制时已经把中心对齐到 `(x, y)` 了。

### 9.2 其他待扩展

- 敌人坦克：`List<Tank>` 统一管理，`for` 循环遍历 `draw`
- 炮弹：新建 `Bullet` 类，`Tank` 里维护列表
- 障碍物 / 围墙：新增地图层，绘制在坦克之前
- 碰撞检测：用中心点 + 半径做圆-圆判定
- `atk / def / hp`：已经定义好了，等战斗系统接上

---

## 十、附：常用 API 速查

### 绘制

| 方法 | 说明 | 受 stroke 影响 |
|---|---|---|
| `g.fillRect(x, y, w, h)` | 实心矩形 | ❌ |
| `g.drawRect(x, y, w, h)` | 空心矩形框 | ✅ |
| `g.fillOval(x, y, w, h)` | 实心椭圆（w==h 时是正圆） | ❌ |
| `g.drawOval(x, y, w, h)` | 空心椭圆框 | ✅ |
| `g.drawLine(x1, y1, x2, y2)` | 直线 | ✅ |
| `g.drawString(s, x, y)` | 文字（y 是**基线**，不是顶端） | — |

**统一规律**：`x, y` 都是外接矩形的**左上角**，不是中心。

### 线宽

```java
Graphics2D g2d = (Graphics2D) g;
g2d.setStroke(new BasicStroke(线宽));
g2d.drawLine(...);
```

### 坐标系

- 原点 `(0,0)` 在**画布左上角**（标题栏下面）
- `x` 向右增大
- **`y` 向下增大**（和数学坐标相反！）
- 所以：往上走是 `y--`，往下走是 `y++`

### 按键常量

```java
KeyEvent.VK_UP / VK_DOWN / VK_LEFT / VK_RIGHT
KeyEvent.VK_W / VK_S / VK_A / VK_D
KeyEvent.VK_ENTER
```

---

*文档按 `src` 目录当前代码状态编写。修改代码后记得同步更新对应章节。*
