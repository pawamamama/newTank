<h1 align="center">坦克大战第二版说明</h1>

<h2 align="center">补全关卡，障碍物，动态效果 ai敌人加强</h2>

> 本文档按 `src` 目录的**当前实际代码**逐行核对编写，不是设想稿。
> 描述已有代码 + 记录重构思路 + 列出你自己留的 TODO。
> 没写完的地方按"待完成"描述，不当缺陷处理。

**当前进度**

```
窗口 → 菜单 → 状态机 → 双缓冲 → 坦克移动 → 图片绘制坦克
     → 开火 → 对象池回收子弹 → 抽象 Tank 类 → 玩家/敌人拆分 → 敌人定时生成
     → 子弹碰撞检测 → 爆炸效果 → 扣血系统 → 血条/名字 → 敌人对象池
     → 敌人死亡完整处理（防鞭尸 + 等爆炸播完 + 归还池子）
                                                    ▲ 你在这
下一步：gameover（玩家死亡 → STATE_OVER → 回菜单重开）
```

---

## 一、项目概览

| 项 | 内容 |
|---|---|
| 项目名 | newTank（坦克大战第二版） |
| 语言 / 图形库 | Java + AWT（`java.awt`） |
| JDK | 25（本机 `D:\java_learning\jdk25`） |
| 入口类 | `com.pawamamama.app.GameMain` |
| 根包 | `com.pawamamama` |
| 窗口尺寸 | 逻辑 900 × 700，显示 1350 × 1050（1.5 倍） |
| 刷新频率 | 30 ms 一次（约 33 FPS） |
| 渲染方式 | 双缓冲（`BufferedImage` + 整张贴屏放大） |
| 缩放算法 | 最近邻（像素画专用），**缓冲区和屏幕两层都要设** |
| 坦克尺寸 | 逻辑半径 20 → 屏幕 40 × 40 |
| 素材尺寸 | 坦克 16 × 16 原生；爆炸帧 11×11 ~ 34×31，缩放倍数 2 |
| 子弹 | 代码画圆（半径 4），素材里没有子弹 |
| 坦克生命 | `DEFAULT_HP = 1000` |
| 坦克攻击力 | `DEFAULT_ATK = 100` → **10 发打死** |
| 坦克名字 | 15 个内置名字随机取（`MyUtil.NAMES`） |
| 血条 | 内部类 `BloodBar`，三层叠加（黄底 / 红条 / 白框） |
| 子弹池 | 初始 200 个，上限 300 个 |
| 炸弹池 | 初始 10 个，上限 30 个 |
| 敌人池 | 初始 10 个，上限 15 个 |
| 爆炸帧率 | `SLOW = 3`，每张图停留 3 次刷新（90ms） |
| 敌人上限 | 10 辆（`ENEMY_MAX_COUNT`） |
| 敌人生成间隔 | 5000 ms（`ENEMY_BORN__INTERVAL`） |
| 敌人 AI 间隔 | 3000 ms 决策一次（`ENEMY_AI_INTERVAL`） |
| 敌人开火概率 | 3%（`ENEMY_FIRE_PERCENT`） |
| 核心设计 | 模板方法（抽象 `Tank`）、对象池×3、双缓冲、静态工厂、分帧动画 |

---

## 二、目录结构与分层

### 2.1 目录树

```
newTank/
├── res/                                  图片素材（18 张 PNG）
│   ├── player_up/down/left/right.png     我方坦克，四方向，16×16
│   ├── enemy_up/down/left/right.png      敌方坦克，四方向，16×16
│   └── boom_0~4.png                      爆炸序列帧，11×11 ~ 34×31（已接入）
│
├── src/com/pawamamama/
│   ├── app/
│   │   └── GameMain.java                 程序入口，只有一行 new GameFrame()
│   │
│   ├── game/
│   │   ├── GameFrame.java    主窗口：初始化、状态机、双缓冲、绘制分发、按键、重绘线程、敌人管理
│   │   ├── Bullet.java       子弹：坐标/方向/速度/可见性、移动与出界判定
│   │   └── Explode.java      爆炸：五帧动画、缩放、分帧播放、居中绘制
│   │
│   ├── Tank/                             ★ 坦克包
│   │   ├── Tank.java         抽象父类：坐标/血量/攻击力/方向/状态、draw()、logic()、mathMove()、
│   │   │                     fire()、collideBullets()、hurt()、die()、deawExlpodes()、内部类 BloodBar
│   │   ├── MyTank.java       玩家坦克：只负责画自己的图
│   │   └── EnemyTank.java    敌人坦克：画敌图 + ai() + resetAiTime() + createTank() 静态工厂
│   │
│   └── util/
│       ├── Constant.java     全局常量（尺寸、状态、菜单、字体、刷新间隔、敌人参数）
│       ├── MyUtil.java       工具类（随机数、随机颜色、随机名字、矩形碰撞、图片加载）
│       ├── BulletsPool.java  子弹对象池 200/300
│       ├── ExplodesPool.java 炸弹对象池 10/30
│       └── EnemiesPool.java  敌人对象池 10/15
│
└── README.md
```

> **包名备注**：Java 约定包名全小写，这里是 `com.pawamamama.Tank`（首字母大写）。
> 不影响运行，IDE 和编译器都接受。写的时候注意大小写——`import com.pawamamama.Tank.*`。

### 2.2 包的划分

| 包 | 职责 | 内容 |
|---|---|---|
| `app` | 启动 | 只有 `GameMain`，不承载任何游戏逻辑 |
| `game` | 游戏运行时 | 窗口、渲染、子弹、爆炸 |
| `Tank` | 坦克模型 | 抽象父类 + 两个具体子类 |
| `util` | 通用设施 | 常量、工具、**三个对象池** |

### 2.3 依赖方向

```
        ┌────────────────────────────────────────────────┐
        │  app 层   GameMain      只负责启动               │
        └───────────────────┬────────────────────────────┘
                            │ new
        ┌───────────────────▼────────────────────────────┐
        │  game 层  GameFrame                             │
        │           ├─ 窗口与渲染循环                      │
        │           ├─ 游戏状态机                          │
        │           ├─ 双缓冲                              │
        │           ├─ 按键分发                            │
        │           ├─ Tank myTank                         │
        │           └─ List<Tank> enemyTanks               │
        │                                                  │
        │           Bullet ── 依赖 Tank（取方向和速度）     │
        │           Explode ── 依赖 Tank（取 RADIUS 算缩放）│
        └───────┬──────────────────────┬──────────────────┘
                │                      │
                ▼                      ▼
        ┌───────────────┐      ┌──────────────────────────┐
        │  Tank 层      │      │  util 层                 │
        │  Tank(抽象)   │      │  Constant   常量池        │
        │   ├ MyTank    │◄─────┤  MyUtil     工具          │
        │   └ EnemyTank │      │  BulletsPool 子弹池       │
        └───────────────┘      │  ExplodesPool 炸弹池      │
                               │  EnemiesPool  敌人池      │
                               └──────────────────────────┘
```

**几点事实**：

- `Tank` 依赖 `game.GameFrame`（读 `GameFrame.titleBarHeight`）
- `Tank` 依赖 `util.BulletsPool`（开火借子弹）、`util.ExplodesPool`（命中借爆炸）
- `EnemyTank` 依赖 `game.GameFrame`（同上）+ `util.MyUtil`（随机位置）
- `EnemyTank` 依赖 `util.BulletsPool`（归还残留子弹）、`util.EnemiesPool`（自己进出池子）
- `Bullet` 依赖 `Tank.Tank`（取 `DIR_*` 和 `DEFAULT_SPEED`）、`game.GameFrame`（同上）
- `Explode` 依赖 `Tank.Tank`（取 `RADIUS` 算缩放倍数）
- `util.BulletsPool` 依赖 `game.Bullet`
- `util.ExplodesPool` 依赖 `game.Explode`
- `util.EnemiesPool` 依赖 `Tank.EnemyTank`

**这里有一个循环**：`game.GameFrame` → `Tank.*` → `game.GameFrame`。

能跑，因为 Java 是整体编译，不同包之间循环引用合法。只是说明「`titleBarHeight`
被当成全局配置用了」——以后如果把它挪进 `Constant`，这个环就断了，依赖方向会干净很多。

> **`util` → `Tank` 的反向依赖**：`EnemiesPool` 需要 `import com.pawamamama.Tank.EnemyTank`，
> 破了「util 不认识游戏对象」的约定。三个池里只有它这样。以后若要把池子挪包，
> 应该挪到 `Tank` 或新建 `pool` 包。

### 2.4 继承体系（重构后的核心结构）

```
                    ┌──────────────────────────────────┐
                    │        abstract Tank             │
                    │  ─────────────────────────────   │
                    │  常量：DIR_* / STATE_*            │
                    │        RADIUS / DEFAULT_SPEED     │
                    │        DEFAULT_HP = 1000          │
                    │        DEFAULT_ATK = 100          │
                    │  字段：name / x y / hp / atk def   │
                    │        dir / state / color        │
                    │        isEnemy                    │
                    │        List<Bullet> bullets       │
                    │        List<Explode> explodes     │
                    │        protected FIX              │
                    │        protected BloodBar bloodBar│
                    │  ─────────────────────────────   │
                    │  具体方法：                       │
                    │    draw()            每一帧入口    │
                    │    logic()           状态分派      │
                    │    mathMove()        位移+边界     │
                    │    fire()            开火         │
                    │    drawBullets()     画+回收      │
                    │    drawName()        名字居中     │
                    │    collideBullets()  碰撞检测     │
                    │    hurt()            扣血         │
                    │    die()             死亡分派     │
                    │    isDie()           血量判定     │
                    │    isExplodeFinished() 爆炸判定   │
                    │    deawExlpodes()    画+回收爆炸   │
                    │    一堆 getter/setter             │
                    │  ─────────────────────────────   │
                    │  抽象方法：                       │
                    │    abstract drawImgTank(g)       │
                    │  ─────────────────────────────   │
                    │  内部类：                         │
                    │    class BloodBar   三层叠加血条   │
                    └───────────────┬──────────────────┘
                                    │ extends
                     ┌──────────────┴──────────────┐
                     ▼                             ▼
        ┌────────────────────────┐   ┌────────────────────────────┐
        │      MyTank            │   │      EnemyTank             │
        │  ────────────────────  │   │  ────────────────────────  │
        │  static Image[] tankImg│   │  static Image[] enemyImg   │
        │  drawImgTank() → 玩家图 │   │  drawImgTank() → 敌图       │
        │                        │   │  ai()            AI 决策    │
        │                        │   │  resetAiTime()   重置计时   │
        │                        │   │  static createTank() 工厂   │
        └────────────────────────┘   └────────────────────────────┘
```

**一句话概括**：「怎么画」各管各的（抽象方法），「怎么动、怎么打、怎么死」大家共用（父类实现）。

细节见 4.7 和第九章。

---

## 三、构建流程

### 3.1 窗口初始化

对游戏窗口进行初始化，设置标题、大小、坐标、窗口的事件监听。

- 游戏窗口使用 `java.awt.Frame`（**不是 JFrame**，本项目直接用 AWT 的 `Frame`）
- 窗口属性初始化：`initFrame()`
- 标题、大小、坐标等常量使用常量类管理
- 静态导入：`import static com.pawamamama.util.Constant.*;`
- 窗口的事件监听：`initEventListener()`

### 3.2 游戏菜单的显示，游戏菜单按键控制

游戏被分成了若干个游戏状态

```java
// ------------------游戏菜单-------------------------
public static final int STATE_MENU = 0;   // 游戏主菜单
public static final int STATE_HELP = 1;   // 帮助界面
public static final int STATE_ABOUT = 2;  // 关于界面
public static final int STATE_RUN = 3;    // 游戏运行中
public static final int STATE_OVER = 4;   // 游戏结束
```

#### 不同的状态绘制不同内容，按键也走不同方法

- 菜单处理 `keyEventMenu`
- 帮助界面 `keyEventHelp`
- 关于界面 `keyEventAbout`
- 游戏运行时 `keyEventRun`
- 游戏结束 `keyEventOver`
- 用 `public static int gameState;` 管理游戏状态
- 用 `private int menuIndex;` 管理开始界面被选中项

#### 重新绘制机制

- FPS：frame per second 每秒帧数
- 每秒固定刷新整个窗口，30ms 刷新一次
- 单独启动一个线程用于窗口重绘，`GameFrame` 实现 `Runnable` 的 `run` 方法
- 构造器里 `new Thread(this).start();` 启动重绘线程

### 3.3 双缓冲：先画到图片上再贴屏

早期是**直接在屏幕画笔上画**：

```java
public void update(Graphics g) {
    g.setColor(Color.BLACK);
    g.fillRect(...);      // 先涂黑
    myTank.draw(g);       // 再画
}
```

问题：用户能**看见**"先涂黑、再画"这个过程，窗口会闪。

现在改成**先画到离屏图片，再一次性贴到屏幕**：

```java
private BufferedImage bufferedImage =
        new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);

public void update(Graphics g1) {
    Graphics2D g = (Graphics2D) bufferedImage.getGraphics();  // ① 图片的画笔
    switch (gameState) { ... }                                // ② 全画到图片上
    g1.drawImage(bufferedImage, 0, 0, this);                  // ③ 整张图贴到屏幕
}
```

**关键差别**：①②全在内存里完成，屏幕一帧都没变；只有③那一瞬间整块像素同时更新。
所以看不到闪，也看不到画到一半的样子。

两个画笔别搞混：

| 名字 | 是什么 | 画到哪 |
|---|---|---|
| `g1` | `update` 的参数，**屏幕**画笔 | 真实屏幕 |
| `g` | `bufferedImage.getGraphics()`，**缓冲区**画笔 | 内存里那张图 |

> 因为不再直接画屏幕，`update()` 里**不要调 `super.update(g1)`**。
> `Component.update()` 默认会先涂背景再调 `paint()`，会把双缓冲效果打回原形。

### 3.4 图片缩放要单独设渲染提示

```java
g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
```

`drawImage` 缩放时默认用**双线性插值**：新像素取周围 4 个原像素的加权平均。

对照片是好事（平滑），对**像素画是灾难**——16×16 放大到 40×40 后，
黑边会晕成灰色，整个画面像蒙了层雾。

`NEAREST_NEIGHBOR`（最近邻）**直接复制最近那个像素的颜色**，不做混合，
像素块边界依然锋利。

> 规律：**像素画 = 最近邻，照片 = 双线性**。

---

## 四、核心架构

### 4.1 游戏状态机（整个项目的骨架）

`GameFrame.gameState` 是 `public static int`，取值来自 `Constant`：

```java
public static final int STATE_MENU  = 0;   // 游戏主菜单
public static final int STATE_HELP  = 1;   // 帮助界面
public static final int STATE_ABOUT = 2;   // 关于界面
public static final int STATE_RUN   = 3;   // 游戏运行中
public static final int STATE_OVER  = 4;   // 游戏结束
```

**所有绘制和按键都靠这个值分叉**：

```
                    gameState
                        │
        ┌───────────────┼────────────────┬──────────────┐
        ▼               ▼                ▼              ▼
   update(g)      keyPressed()     keyReleased()    其它逻辑
        │               │
   ┌────┴────┐     ┌────┴─────┐
   ▼    ▼    ▼     ▼     ▼    ▼
drawMenu drawHelp ...  keyEventMenu  keyEventHelp ...
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

- **`repaint()` 不能换成 `paint()` 直接调用**。`repaint()` 只是"登记一次重绘请求"，
  由 AWT 的事件分发线程（EDT）稍后统一回调 `update(Graphics)`。
- **`update(Graphics)` 才是系统回调的入口**，由它按 `gameState` 分发。
- `repaint()` 线程安全（内部 post 到 EDT），所以可以在自己的线程里循环调用。

### 4.3 绘制的「清屏 + 重画」模式

每一帧全屏重画，`drawXxx` 里必须**先把整张缓冲区涂满**：

```java
private void drawRun(Graphics g) {
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);  // ① 清屏（清缓冲区）
    drawEnemyTanks(g);                             // ② 画敌人
    myTank.draw(g);                                // ③ 画玩家（最后画，压在上面）
}
```

**顺序不能反**。双缓冲下这个错误更"持久"：缓冲区是复用的同一张 `BufferedImage`，
一旦某帧没涂满，旧像素会一直留着，直到某帧把它盖掉。

**为什么玩家最后画**：AWT 没有 z 序管理，后画的覆盖先画的。玩家在最上层符合直觉。

### 4.4 按键事件的「按下 / 松开」模型

这是坦克能"持续移动"的关键设计：

| 事件 | 回调 | 做的事 |
|---|---|---|
| 按下方向键 | `keyPressed` → `keyEventRun` | `setDir(方向)` + `setState(STATE_MOVE)` |
| 松开方向键 | `keyReleased` → `keyReleasedEventRun` | `setState(STATE_STAND)` |
| 按下空格 | `keyPressed` → `keyEventRun` | `myTank.fire()` |

**注意**：`setDir()` 只改朝向，**不产生位移**。位移是每帧 `logic() → mathMove()` 干的。

```
        dir（方向）              state（状态）
           │                        │
   决定往哪个方向动          决定「动不动」
           └────────┬───────────────┘
                    ▼
            每帧 logic() 里
            state == MOVE 才调 mathMove()
```

**为什么按下和松开要分开**：操作系统把一次按键拆成两个事件。
按住不放时 Windows 会连续发 `keyPressed`（按键重复），但**不会**连续发 `keyReleased`。
所以不能靠 `keyPressed` 自己实现"一直走"，必须靠 `state` 这个开关：

```
按下 → STATE_MOVE  → 每帧都走
松开 → STATE_STAND → 每帧都不走
```

### 4.5 常量集中管理

`Constant` 是 `final class` + 全部 `static final`，并且被**静态导入**：

```java
import static com.pawamamama.util.Constant.*;
```

效果是代码里直接写 `FRAME_WIDTH`，不用 `Constant.FRAME_WIDTH`。

好处：所有魔法数字集中一处，改一个值全项目生效。
代价：静态导入后看不出来源，读代码时要知道它从哪来。

### 4.6 对象池（BulletsPool）

#### 为什么要有对象池

子弹是**高频创建、高频销毁**的对象。每次开火 `new Bullet()`，飞出屏幕就变垃圾。
一秒发 10 发，一分钟就是 600 个待回收对象。

对象池思路：**预先创建一批，用完不扔，还回来接着用**。

```
        开局：池子里躺着 200 个「空壳子弹」
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
   fire() 时借 1 个         子弹飞出屏幕
   get() 把它移出池子         theReturn() 塞回池子
        │                       │
        └───────────┬───────────┘
                    ▼
          池里对象反复循环使用
          理论上一共只创建 200 个
```

#### 三个角色

| 成员 | 作用 | 类比 |
|---|---|---|
| `static { ... }` | 类加载时创建 200 个 | 造好一批空弹壳 |
| `get()` | 移出 1 个并返回 | 从弹箱取弹壳 |
| `theReturn(bullet)` | 加回列表 | 打完的弹壳扔回箱子 |

#### `get()` 的两个分支

```java
public static Bullet get() {
    System.out.println("pool size: " + pool.size());
    Bullet bullet = null;
    if (pool.size() == 0) {         // 池子空了
        bullet = new Bullet();      // 兜底：现造一个，别让游戏崩
    } else {
        bullet = pool.remove(0);    // 取出索引 0，同时返回它
    }
    return bullet;
}
```

#### `pool.remove(0)` 到底是什么

`List` 有两个 `remove` 重载，靠**参数类型**区分：

```java
list.remove(0);           // ① 参数是 int  → 按「索引」删，返回被删的那个元素
list.remove(someBullet);  // ② 参数是对象   → 按「对象」删，返回 boolean
```

这里 `0` 是 `int`，走①，返回的就是被删掉的那个 `Bullet`。

> **经典陷阱**：如果是 `List<Integer>`，`remove(0)` 删的是索引 0，
> 而 `remove(Integer.valueOf(0))` 删的是值等于 0 的元素。`List<Bullet>` 没这个坑。

#### `theReturn()` 池满为什么直接 return

```java
public static void theReturn(Bullet bullet) {
    if (pool.size() == MAX_POOL_SIZE) {   // 池满
        return;                           // 丢弃本次归还
    }
    pool.add(bullet);
}
```

池满时正确的语义就是**丢弃这次归还**：

- 调用方 `Tank.drawBullets()` 已经把引用从 `bullets` 里删掉了
- 所以这颗子弹**没有任何人引用它**，自然变垃圾，GC 会回收
- 方法里的 `return` 只是结束方法，不需要额外做什么

> 早前这里曾写过 `bullet = null;` —— 那行是多余的。`bullet` 只是**形参**，
> 赋 null 只影响方法内部的局部变量，方法一结束就销毁，调用方的引用和对象本身都不受影响。
> 现在已删掉。

#### 为什么 `MAX_POOL_SIZE (300) > DEFAULT_POOL_SIZE (200)`

这是**弹性池**设计，给峰值留余量：

```
开局：池子 200 个
屏幕上同时飞 250 颗
  → 前 200 颗从池子拿，池子变 0
  → 剩下 50 颗走 get() 兜底分支 new 出来
子弹陆续消失，250 颗全归还
  → 池子变 250 个
```

所以池子数量**会超过初始的 200**，这是设计预期。
`MAX_POOL_SIZE = 300` 是上限保护，防止异常情况下无限膨胀。

**副作用**：池子**只涨不缩**。峰值到过 250，之后就算平时只有 10 颗在飞，
池里永远躺着 250 个空壳。占用很小（一个 Bullet 约 40 字节），可以不管。

#### 三个池对比

| 项 | BulletsPool | ExplodesPool | EnemiesPool |
|---|---|---|---|
| `DEFAULT_POOL_SIZE` | 200 | **10** | **10** |
| `MAX_POOL_SIZE` | 300 | **30** | **15** |
| 类型 | `Bullet` | `Explode` | `EnemyTank` |
| 创建频率 | 每秒十几颗 | 每次击杀 | **5 秒一辆** |
| 归还前要不要清理内部集合 | 否 | 否 | **要** |

**池子大小按「同屏最大并发数」估，不是按「创建频率」。**
子弹同屏可能几十颗，爆炸同时最多几个，敌人同屏上限 10 辆。

#### ⚠️ 敌人池的特殊性：归还前必须清理内部集合

`EnemyTank` 和其它两个池化对象不同，它**自己持有两个池化对象列表**：

```java
// Tank.java
private List<Bullet> bullets = new ArrayList();     // 子弹，本身也是池化对象
private List<Explode> explodes = new ArrayList();   // 爆炸，本身也是池化对象
```

**这是一层嵌套引用**。归还 `EnemyTank` 时，它身上的东西也得先还回各自的池子：

```
EnemyTank 归还
    ├─ bullets 里的 Bullet → 还给 BulletsPool
    └─ explodes 里的 Explode → 还给 ExplodesPool
         ↓
    才能清空列表并归还 EnemyTank 自己
```

**不归还会怎样**：那些子弹和爆炸对象失去引用、永久泄漏，池子少了几个，
下次 `get()` 拿不到就走兜底 `new`，**池化失效**。

**为什么会有残留**：敌人死亡瞬间身上可能有子弹正在飞（子弹要飞出屏幕才消失，
要飞 100 多帧）；而爆炸在尸体期还要播 450ms 才结束。这两个都可能比坦克活得久。

所以 `EnemyTank.createTank()` 里有这段：

```java
//归还残留子弹（敌人死时可能有子弹没飞完）
for (Bullet b : enemy.getBullets()) {
    BulletsPool.theReturn(b);
}
enemy.getBullets().clear();
```

> **规律**：池化对象如果持有**其他池化对象**，归还前必须把它们还回去再 `clear()`。
> 这是对象池最容易漏的一环，因为单看 `EnemyTank` 的字段，很容易只想到 `x`/`hp` 这些基本类型。

#### ⚠️ 对象池的脏数据陷阱

池子里的对象是**上一轮用过的**，所有字段都还是旧值。
从池子取出后**必须把每个字段都重置**，漏一个就出诡异 bug。

`Explode` 的字段一共 5 个，全都要重置：

```java
final Explode explode = ExplodesPool.get();
explode.setVisible(true);      // ① 可见
explode.setIndex(0);           // ② 播放进度归零
explode.setX(bullet_x);        // ③ 坐标
explode.setY(bullet_y);        // ④
explode.setEndIndex(3 * Explode.SLOW);   // ⑤ 终止帧
explodes.add(explode);
```

**`setIndex(0)` 是最容易漏的一个**：
不归零的话，下一轮爆炸会从上一轮结束的位置接着播，直接跳到最后一帧或立刻消失。

`EnemyTank` 除了基本字段，**多一个 `aiStartTime`**：

```java
// EnemyTank.createTank()
enemy.resetAiTime();       // ← 不能漏！
```

不重置的话，池里躺了几十秒的对象，`currentTimeMillis() - aiStartTime` 远大于
`ENEMY_AI_INTERVAL`，**新敌人第一帧就疯狂决策、乱换方向、乱开火**。

> `aiStartTime` 只在构造器里赋值，而池化后**构造器再也不执行**——这是它容易被忘的原因。

> 这和 `Bullet.setVisible` 曾经踩过的坑是同一类问题——见第十章 10.3。

### 4.7 模板方法模式（抽象 Tank 的用意）

这是本次重构最核心的变化。

#### 重构前：一个 Tank 类 + isEnemy 分支

```java
public class Tank {
    private static Image[] tankImg;
    private static Image[] enemyImg;
    private boolean isEnemy;

    private void drawImgTank(Graphics g) {
        if (isEnemy) {
            g.drawImage(enemyImg[dir], ...);
        } else {
            g.drawImage(tankImg[dir], ...);
        }
    }
}
```

**问题**：每加一种坦克（敌人生成器、Boss、护盾坦克），就要往 `drawImgTank` 里加一个 `if`。
而且 `tankImg` 和 `enemyImg` 都挂在同一个类上，敌人永远用不到玩家图、玩家永远用不到敌图。

#### 重构后：抽象父类 + 两个子类

```java
public abstract class Tank {
    public void draw(Graphics g) {
        logic();
        drawImgTank(g);      // ← 不知道具体怎么画，交给子类
        drawBullets(g);
    }

    public abstract void drawImgTank(Graphics g);   // 留给子类
}
```

```java
public class MyTank extends Tank {
    private static Image[] tankImg;          // 玩家图只在这个类里
    @Override
    public void drawImgTank(Graphics g) { ... }
}

public class EnemyTank extends Tank {
    private static Image[] enemyImg;         // 敌人图只在这个类里
    @Override
    public void drawImgTank(Graphics g) { ... }
}
```

#### 这叫「模板方法模式」

```
      父类定好流程骨架（模板）
      ┌───────────────────────────┐
      │ draw() {                  │
      │     logic();      ← 固定   │
      │     drawImgTank(); ← 变    │  ← 抽象方法 = 留给子类的"洞"
      │     drawBullets(); ← 固定  │
      │ }                         │
      └───────────────────────────┘
                │
      ┌─────────┴─────────┐
      ▼                   ▼
  MyTank 填玩家图     EnemyTank 填敌图
```

**好处**：

| 好处 | 说明 |
|---|---|
| 想加新坦克只需 `extends Tank` | 不用动父类一行代码 |
| 图片各自管理 | `MyTank` 不会加载敌人图，反之亦然 |
| 共用逻辑只有一份 | `mathMove`、`fire`、`drawBullets` 都在父类 |
| 编译期强制 | 子类忘了实现 `drawImgTank` 就编译不过 |

#### 抽象父类还起了一个作用：**禁止直接 new Tank**

```java
new Tank(1, 1, Tank.DIR_UP);   // ❌ 编译错误：Tank 是抽象的
```

这正好符合游戏语义——**坦克只有"我方"和"敌方"两种，不存在"一个坦克"**。
把它声明成 `abstract` 时，编译器就替你把这条规则守住了。

#### 顺便说说 `protected final int FIX = 1`

```java
// Tank.java
protected final int FIX = 1;
```

- `protected`：本类、同包、子类都能用（`MyTank`、`EnemyTank` 要用它）
- `final`：值定死，子类不能改成 2

为什么不写成 `private static final`？因为**子类要用**。
`MyTank.drawImgTank()` 和 `EnemyTank.drawImgTank()` 里都写了 `getX() - RADIUS + FIX`，
这个 `FIX` 就是从父类继承来的。

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

只有一行。`new GameFrame()` 的构造器里干了所有初始化，所以**不需要保存引用**——
对象被窗口系统持有，不会被 GC。

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
| `ENEMY_MAX_COUNT` | 10 | 同屏敌人上限 |
| `ENEMY_BORN__INTERVAL` | 5000 | 敌人生成间隔（毫秒） |
| `ENEMY_AI_INTERVAL` | 3000 | 敌人 AI 决策间隔（毫秒） |
| `ENEMY_FIRE_PERCENT` | 0.03 | 敌人每次决策时开火的概率 |

> **命名备忘**：`ENEMY_BORN__INTERVAL` 中间是**两个下划线**（`BORN__INTERVAL`）。
> 不影响运行，但引用时容易只打一个。

`MENUS` 用数组而不是 5 个变量，好处是配合 `MENUS.length` 做循环翻页：

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
    private MyUtil() {}                                              // ① 禁止 new
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

这里传 **256** 而不是 255：因为区间右开，`getRandomNumber(0, 255)` 只能取到 `0~254`，
永远取不到 255 这个分量。

> 小知识：`Color` 的整数构造器每个分量合法范围 `0 ~ 255`，超出抛 `IllegalArgumentException`。

#### getRandomName —— 名字池

```java
//用数组管理，以后加/删名字不用改方法
public static final String[] NAMES = {
        "铁锤", "闪电", "孤狼", "雷霆", "幽灵",
        "烈焰", "寒霜", "铁壁", "疾风", "暗影",
        "钢牙", "赤焰", "苍龙", "猛虎", "碎星",
};

public static final String getRandomName() {
    return NAMES[getRandomNumber(0, NAMES.length)];
}
```

**复用 `getRandomNumber`，不另写随机逻辑**：

`getRandomNumber(0, NAMES.length)` 因为右开，返回 `0 ~ 14`，
**正好覆盖全部 15 个下标，且不会越界**。这和 `getRandomColor()` 传 `256` 是同一个道理。

**用数组不用 `switch` / 一堆 `if`**：以后想加名字只改数组，`getRandomName()` 一个字不动。

> **重名问题**：15 个名字、10 辆敌人 + 1 个玩家 = 11 个使用者。
> 随机取必然可能重名。要唯一得改成「打乱后按顺序取」，目前没做。

#### isRectangleCollision —— 点是否在方框内

```java
public static final boolean isRectangleCollision(int rectX, int rectY, int radius,
                                                 int pointX, int pointY) {
    int disX = Math.abs(rectX - pointX);
    int disY = Math.abs(rectY - pointY);
    if (disX <= radius && disY <= radius) {
        return true;
    }
    return false;
}
```

**为什么必须 `Math.abs`**：横向判断 `disX <= radius`，如果 `disX` 是负数（点在左边），
`-100 <= 20` 永远成立，**不管点多远都算打中**。取绝对值后左右对称。

**为什么 `<=` 不是 `<`**：贴边也算命中，子弹擦边能打中，手感更宽容。

**为什么是「方框」不是「圆」**：坦克画的是圆（`fillOval`），但判定用方框。
斜角位置会出现「看着没碰到却算打中」。见 11.3 圆碰撞。

#### createImg —— 统一图片加载

```java
public static Image createImg(String path) {
    return Toolkit.getDefaultToolkit().getImage(path);
}
```

`MyTank` / `EnemyTank` / `Explode` 的静态块都用它，避免三处重复写 `Toolkit.getDefaultToolkit()`。

### 5.4 `GameFrame.java` —— 主窗口

继承 `java.awt.Frame` 并实现 `Runnable`。

#### 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `bufferedImage` | `private BufferedImage` | 离屏缓冲区，900 × 700 |
| `gameState` | `public static int` | 全局游戏状态 |
| `menuIndex` | `private int` | 菜单当前选中项 |
| `titleBarHeight` | `public static int` | 标题栏高度，`setVisible` 后取 `getInsets().top` |
| `myTank` | `private Tank` | 玩家坦克（**声明成父类型**，实际是 `MyTank`） |
| `enemyTanks` | `private List<Tank>` | 全部敌人（泛型写 `Tank`，实际装 `EnemyTank`） |

> **注意这两个字段声明的都是 `Tank` 而不是 `MyTank` / `EnemyTank`**。
> 这叫「面向父类编程」——`drawRun` 里只需要知道"这是一辆坦克"，
> 不需要知道它具体是玩家还是敌人。见 4.7 和 9.5。

#### initFrame() 与窗口坐标系（重要）

```java
setTitle(GAME_TITLE);
setSize(FRAME_WIDTH, FRAME_HEIGHT);  // 设的是「窗口外框」尺寸
setLocationRelativeTo(null);         // 居中
setResizable(false);
setVisible(true);                    // ★ 必须先显示
titleBarHeight = getInsets().top;    // ★ 之后才拿得到真实边框
```

**`getInsets()` 必须在 `setVisible(true)` 之后调用**，否则返回全 0。

原因：AWT 的 `Frame` 是重量级组件，背后有一个真实的操作系统窗口（native peer）。
`new Frame()` 时只有 Java 对象；`setVisible(true)` 才创建系统窗口，
系统此时才告诉 Java「我这里标题栏多高」。

坐标系关系：

```
窗口外框 y=0   ┌──────────────────┐
               │  标题栏           │  ← getInsets().top
画布原点 y=0   ├──────────────────┤  ★ Graphics 的原点在这里
               │                  │
               │   可绘区          │
               │                  │
可绘区底       ├──────────────────┤
               │  底边框           │  ← getInsets().bottom
窗口外框底     └──────────────────┘  ← y = 700
```

**`titleBarHeight` 的用途**：把「窗口中部坐标」换算成「画布坐标」。
`Tank.mathMove()` 里往上走用 `Math.max(GameFrame.titleBarHeight + RADIUS, ...)`，
`Bullet.move()` 里往上飞用 `y <= GameFrame.titleBarHeight` 判定出界——保持了一致。

#### update() —— 绘制分发 + 双缓冲

```java
public void update(Graphics g1) {
    Graphics2D g = (Graphics2D) bufferedImage.getGraphics();      // 缓冲区的画笔

    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.setFont(FONT);

    switch (gameState) {
        case STATE_MENU:  drawMenu(g);  break;
        case STATE_HELP:  drawHelp(g);  break;
        case STATE_ABOUT: drawAbout(g); break;
        case STATE_RUN:   drawRun(g);   break;
        case STATE_OVER:  drawOver(g);  break;
    }

    g1.drawImage(bufferedImage, 0, 0, this);                      // 整张贴屏
}
```

`update()` 是 `Frame` 从 `Component` 继承来的方法，**不能主动调**，只能靠 `repaint()` 回调。

#### drawRun() —— 游戏画面

```java
private void drawRun(Graphics g) {
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);   // 清屏
    drawEnemyTanks(g);                             // 敌人（可能 0~10 辆）
    myTank.draw(g);                                // 玩家
}

private void drawEnemyTanks(Graphics g) {
    for (int i = 0; i < enemyTanks.size(); i++) {
        enemyTanks.get(i).draw(g);
    }
}
```

> 这里用**索引 for**，而 `enemyTanks` 会被另一个线程往里 add（见 7.3）。

#### drawMenu() —— 菜单绘制

```java
g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);   // 黑底
int x = FRAME_WIDTH - STR_WIDTH >> 1;          // 水平居中
int y = FRAME_HEIGHT / 3;                      // 起始高度
for (int i = 0; i < MENUS.length; i++) {
    if (menuIndex == i) g.setColor(Color.RED);   // 选中红
    else                g.setColor(Color.WHITE); // 其余白
    g.drawString(MENUS[i], x, y + DIS * i);      // 逐行下移 50
}
```

`>> 1` 等价于 `/ 2`（整型除法）。

> **优先级提醒**：`-` 比 `>>` 优先级高，所以 `FRAME_WIDTH - STR_WIDTH >> 1`
> 实际是 `(FRAME_WIDTH - STR_WIDTH) >> 1`，这里**恰好是想要的**。
> 但 `FRAME_WIDTH - STR_WIDTH >> 2` 这种容易读错，建议一律加括号。

#### 按键监听：三层分发

```
KeyAdapter（匿名内部类）
    │
    ├─ keyPressed(e)  → 按 gameState 分发 → keyEventMenu/Help/About/Run/Over
    │                                        ├→ 方向键：setDir + setState
    │                                        └→ 空格  ：myTank.fire()
    │
    └─ keyReleased(e) → 只有 STATE_RUN 时 → keyReleasedEventRun
                                            └→ setState(STATE_STAND)
```

用 `KeyAdapter` 而不是 `KeyListener`，因为接口有 3 个方法，适配器类已给空实现，
只需重写关心的。用**匿名内部类**是因为这个监听器只在这个窗口用一次。

`keyEventMenu` 里的循环翻页：

```java
case VK_UP:    if (--menuIndex < 0) menuIndex = MENUS.length - 1; break;
case VK_DOWN:  if (++menuIndex > MENUS.length - 1) menuIndex = 0; break;
case VK_ENTER: newGame(); break;
```

#### newGame() —— 开始新游戏 + 启动敌人生成线程

```java
private void newGame() {
    gameState = STATE_RUN;
    myTank = new MyTank(100, 100, Tank.DIR_DOWN);   // ① 创建玩家

    new Thread(() -> {                               // ② 独立线程生成敌人
        while (true) {
            if (enemyTanks.size() < ENEMY_MAX_COUNT) {
                enemyTanks.add(EnemyTank.createTank());
            }
            try {
                Thread.sleep(ENEMY_BORN__INTERVAL);  // 每 5 秒一次
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }).start();
}
```

**两个要点**：

| 要点 | 说明 |
|---|---|
| 独立线程而不是在主循环里计数 | 生成间隔 5 秒，和 30ms 帧循环不同频；独立线程直接 `sleep` 最直观 |
| `enemyTanks` 用 `List<Tank>` | 装的是 `EnemyTank`，但声明成父类型，`drawEnemyTanks` 里不用转型 |

`myTank` 此刻之前是 `null`，所以 `drawRun` 必须在 `newGame` 之后才会被调到
（只有 `STATE_RUN` 才走 `drawRun`）。

### 5.5 `Tank.java` —— 抽象父类

#### 常量

| 分类 | 常量 | 值 |
|---|---|---|
| 方向 | `DIR_UP / DIR_DOWN / DIR_LEFT / DIR_RIGHT` | 0 / 1 / 2 / 3 |
| 状态 | `STATE_STAND / STATE_MOVE / STATE_DIE` | 0 / 1 / 2 |
| 尺寸 | `RADIUS` | 20 |
| 速度 | `speed` | 4（每帧 4 像素） |
| 生命 | `DEFAULT_HP` | 1000 |
| 攻击 | `DEFAULT_ATK` | 100 |

> **方向的数值顺序和旋转角度顺序不一致**：值是「上、下、左、右」，
> 按顺时针排是「上、右、下、左」。以后若改用旋转画法，**不能写 `dir * 90`**，
> 必须用 `switch` 映射。见 11.4。

#### 数据字段

| 字段 | 说明 |
|---|---|
| `name` | 随机名字，`initTank()` 里生成 |
| `x, y` | 坦克**中心点**坐标（不是左上角） |
| `hp` | 血量，初值 `DEFAULT_HP` |
| `atk` | 攻击力，初值 `DEFAULT_ATK`，可用 `setAtk()` 单独改 |
| `def` | 防御力（还没接进伤害公式） |
| `dir` | 当前朝向 |
| `state` | 当前状态，初值 `STATE_STAND` |
| `color` | 随机颜色，画子弹用 |
| `bullets` | 弹夹：`List<Bullet>` |
| `explodes` | 身上的爆炸：`List<Explode>` |
| `isEnemy` | 是否敌人（默认 `false`，`createTank()` 里置 true） |
| `FIX` | `protected final int = 1`，图片偏心补偿量，子类要用 |
| `bloodBar` | `protected final BloodBar`，内部类实例 |

`color` **必须是实例字段、不能加 static**：加了 static 所有坦克会共用一个颜色。

#### 两个构造器

```java
public Tank() {                    // 给池子用的空壳
    initTank();
}

public Tank(int x, int y, int dir) {
    this.x = x;
    this.y = y;
    this.dir = dir;
    initTank();
}

// 公共初始化抽出来，两个构造器都调
private void initTank() {
    color = MyUtil.getRandomColor();
    name = MyUtil.getRandomName();
}
```

**为什么抽 `initTank()`**：避免两个构造器重复写「随机颜色 + 随机名字」。

> **池化注意**：无参构造器只在池子静态块里执行一次（10 次）。
> 之后 `get()` 出来的对象**不会再走构造器**，所以 `color` / `name` 会一直是第一次的值。
> 如果想要每次不一样，得在 `createTank()` 里手动重置。

#### draw() —— 一帧干五件事

```java
public void draw(Graphics g) {
    logic();            // ① 推进逻辑
    drawImgTank(g);     // ② 画坦克（抽象方法，子类实现）
    drawBullets(g);     // ③ 画自己的所有子弹 + 回收
    drawName(g);        // ④ 名字
    bloodBar.draw(g);   // ⑤ 血条
}
```

这是**模板方法**：①③④⑤固定，②是留给子类的洞。见 4.7。

#### drawName() —— 名字居中

```java
private void drawName(Graphics g) {
    g.setColor(color);
    g.setFont(SMALL_FONT);

    //算文字宽度得到永远居中
    FontMetrics fm = g.getFontMetrics();
    int textW = fm.stringWidth(name);
    g.drawString(name, x - textW / 2, y - 35);
}
```

**`FontMetrics` 是量文字宽度的工具**：`stringWidth(name)` 返回这个字符串画出来有多宽（像素）。

**为什么不写 `x - RADIUS`**：坦克左边界固定，但名字长度不固定。「铁锤」两个字 32px、
「苍龙在天」四个字 64px，用左边界对齐会一个偏左一个超出屏幕。

**`x - textW / 2` 让文字中心落在坦克中心 `x` 上**，几个字都居中。

**基线位置 `y - 35`**：`drawString` 的 y 是**基线**（文字底部那条线，不含下伸部分）。
配合血条算：

```
y - 35  ← 名字基线
y - 30  ← 血条上边（RADIUS + BAR_HEIGHT*2 = 20 + 10）
y - 20  ← 坦克顶边
```

**名字底部和血条顶边隔 5 像素**，不挤。

#### drawImgTank() —— 抽象方法

```java
public abstract void drawImgTank(Graphics g);
```

父类里只有声明，没有方法体。**因为父类不知道"该用哪套图"**——
这个信息只有子类有。

原来的实现（带 `if (isEnemy)` 的版本）在代码里以注释形式保留着，
作为重构前后的对照。

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

**状态驱动的移动**：只有 `state == STATE_MOVE` 才真正改变坐标。

> `logic()` 只在 `draw()` 里被调用，所以**不调 `draw()` 的坦克就不动**。
> 这正是 `GameFrame.drawEnemyTanks` 第一趟跳过死者的依据——死了不画，自然不动，
> 不需要额外设 `STATE_DIE`。

#### mathMove() —— 边界限制

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

`if` 版（`move()` 那套，已废弃）是「先判断、再移动」，判断用的是**移动前**的位置，
移完可能越过边界：

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
| 往上（坐标变小） | `Math.max` 兜下限 | `titleBarHeight + RADIUS` |
| 往左（坐标变小） | `Math.max` 兜下限 | `RADIUS` |
| 往下（坐标变大） | `Math.min` 兜上限 | `FRAME_HEIGHT - RADIUS` |
| 往右（坐标变大） | `Math.min` 兜上限 | `FRAME_WIDTH - RADIUS` |

> 往上的边界多算了 `titleBarHeight`、往下没有，两处不对称。
> 这是你实测过的手感，以实际运行效果为准。

#### fire() —— 开火

```java
public void fire() {
    // ① 从坦克中心出发，按方向推到炮管口
    int bulletX = x;
    int bulletY = y;
    switch (dir) {
        case DIR_UP:    bulletY -= RADIUS; break;
        case DIR_DOWN:  bulletY += RADIUS; break;
        case DIR_LEFT:  bulletX -= RADIUS; break;
        case DIR_RIGHT: bulletX += RADIUS; break;
    }

    // ② 从对象池借一颗
    Bullet bullet = BulletsPool.get();

    // ③ 逐个字段赋值（池里的是「空壳」，上一轮脏数据要全部盖掉）
    bullet.setAtk(atk);        // 坦克持有伤害能力，子弹负责传达
    bullet.setX(bulletX);
    bullet.setY(bulletY);
    bullet.setDir(dir);
    bullet.setColor(color);
    bullet.setVisible(true);   // ★ 必须重新置为可见

    // ④ 放进弹夹
    bullets.add(bullet);
}
```

**① 为什么偏移 `RADIUS`**：子弹不能从坦克中心蹦出来，否则一开始就在车身里，
看起来像"从肚子中间冒出"。推到炮管口（中心 ± 半径）才自然。

**③ 为什么必须逐个 set**：池里拿到的是**上一轮用过的**对象，坐标、方向、颜色、
可见性全是脏值。少设一个就出诡异 bug——最典型的是漏 `visible`（见 10.3）。

> **`setAtk(atk)` 出现两次**（第 247 和 251 行），是重复调用，结果一样。留着无妨，可以删一行。

#### drawBullets() —— 绘制 + 回收

```java
private void drawBullets(Graphics g) {
    // 第一趟：画
    for (Bullet bullet : bullets) {
        bullet.draw(g);
    }
    // 第二趟：把不可见的移除，并归还给池子
    for (int i = bullets.size() - 1; i >= 0; i--) {
        Bullet bullet = bullets.get(i);
        if (!bullet.isVisible()) {
            bullets.remove(i);
            BulletsPool.theReturn(bullet);
        }
    }
}
```

**为什么分两趟**：第一趟用 `for-each` 画（不修改列表），第二趟才删。
如果合成一趟边画边删，`for-each` 会抛 `ConcurrentModificationException`。

**第二趟用倒序**：`remove(i)` 后后面元素前移，正序删会跳过下一个。

#### collideBullets() —— 碰撞检测 + 扣血 + 爆炸

```java
public void collideBullets(List<Bullet> bullets) {
    //已经死了就不再受击：防止尸体继续挨打导致爆炸无限叠加
    if (isDie()) return;

    for (Bullet bullet : bullets) {
        int bullet_x = bullet.getX();
        int bullet_y = bullet.getY();

        if (MyUtil.isRectangleCollision(x, y, RADIUS, bullet_x, bullet_y)) {
            bullet.setVisible(false);   // ① 子弹消失
            hurt(bullet);               // ② 扣血（里面可能触发 die()）

            // ③ 按扣血后的血量决定爆炸类型
            final Explode explode = ExplodesPool.get();
            if (hp > 0) {
                // 受击：3 帧，位置用子弹命中点
                explode.setVisible(true);
                explode.setIndex(0);
                explode.setX(bullet_x);
                explode.setY(bullet_y);
                explode.setEndIndex(3 * Explode.SLOW);
                explodes.add(explode);
            } else {
                // 死亡：5 帧，位置用坦克中心
                explode.setVisible(true);
                explode.setIndex(0);
                explode.setX(x);
                explode.setY(y);
                explode.setEndIndex(5 * Explode.SLOW);
                explodes.add(explode);
            }
        }
    }
}
```

**开头 `if (isDie()) return;` 是防鞭尸**，作用见下面「为什么必须加」。

**③ 为什么受击用命中点、死亡用坦克中心**：见 6.4 的尺寸表——
前 3 帧最大 32×32（比坦克小），贴命中点像装甲火花；
后 2 帧 56×58 / 62×68（比坦克大），必须居中对齐否则看起来炸歪。

#### hurt() —— 扣血

```java
private void hurt(Bullet bullet) {
    int atk = bullet.getAtk();
    System.out.println("atk: " + atk);   // 调试用，上线要删
    hp -= atk;
    if (hp <= 0) {
        die();
    }
}
```

**伤害从子弹取，不从坦克取**：坦克的 `atk` 是「我能打多少」，
子弹的 `atk` 是「这一下打多少」的**快照**。

**为什么不能实时读坦克 `atk`**：子弹飞出去后坦克可能升级或死亡，
实时读会导致「飞行中的子弹突然变强」。

#### die() —— 死亡分派

```java
private void die() {
    if (!isEnemy) {
        //gameover todo
    }
    //这里不处理敌方坦克，敌人没有死的概念，只是归还到对象池
}
```

**注意：这里没有归还敌人到池子。**

敌人的归还统一在 `GameFrame.drawEnemyTanks()` 里做，条件是
`isDie() && isExplodeFinished()`。**归还只能有一处**——如果这里也归还，
对象会同时存在于 `enemyTanks` 列表和 `EnemiesPool` 池子里，坐标、血量全乱。

#### isDie() / isExplodeFinished()

```java
public boolean isDie() {
    return hp <= 0;
}

/**
 * 死亡帧是否播放完成
 * explodes 里存的是这辆坦克身上正在播的爆炸动画。
 * deawExlpodes() 每帧画完后会把播完（!isVisible()）的爆炸移除并还池，
 * 所以列表非空 = 至少还有一个没播完，列表空 = 全播完了。
 */
public boolean isExplodeFinished() {
    return explodes.isEmpty();
}
```

**两个判定配合使用**：`isDie()` 判断「要不要画车身」，`isExplodeFinished()` 判断「能不能删出列表」。

#### deawExlpodes() —— 绘制 + 回收爆炸

```java
public void deawExlpodes(Graphics g) {
    for (Explode explode : explodes) {
        explode.drawBoom(g);
    }
    //将不可见的爆炸效果删除
    for (int i = explodes.size() - 1; i >= 0; i--) {
        Explode explode = explodes.get(i);
        if (!explode.isVisible()) {
            explodes.remove(i);
            ExplodesPool.theReturn(explode);
        }
    }
}
```

> 方法名 `deawExlpodes` 是 `drawExplodes` 打错了，能跑，改名要同步改 `GameFrame` 里的调用。

#### 内部类 BloodBar —— 三层叠加血条

```java
class BloodBar {
    public static final int BAR_LENGTH = 50;
    public static final int BAR_HEIGHT = 5;

    public void draw(Graphics g) {
        //血条左上角，三层共用同一个位置
        int barX = x - BAR_LENGTH / 2;
        int barY = y - RADIUS - BAR_HEIGHT * 2;

        //黄底：永远画满，表示血量上限
        g.setColor(Color.YELLOW);
        g.fillRect(barX, barY, BAR_LENGTH, BAR_HEIGHT);

        //红条：按剩余血量比例缩短
        g.setColor(Color.RED);
        g.fillRect(barX, barY, hp * BAR_LENGTH / DEFAULT_HP, BAR_HEIGHT);

        //白框：描边
        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, BAR_LENGTH, BAR_HEIGHT);
    }
}
```

**三层缺一不可**：

| 层 | 不画会怎样 |
|---|---|
| 黄底（满条） | 血少了之后，空出来的地方是透明，看不出血条总长 |
| 红条（当前血） | 只有黄底，看不出掉了多少 |
| 白框 | 血条和背景混在一起，边界不清 |

**`hp * BAR_LENGTH / DEFAULT_HP` 为什么必须「先乘后除」**：

```java
// 错：先除后乘
hp / DEFAULT_HP * BAR_LENGTH
= 600 / 1000 * 50          // 整数除法，600/1000 = 0
= 0 * 50 = 0               // 血条永远是空的

// 对：先乘后除
hp * BAR_LENGTH / DEFAULT_HP
= 600 * 50 / 1000
= 30000 / 1000 = 30        // 正确，30/50 = 60%
```

**整数除法会截断**，先除会把小数部分扔掉。

**`barX = x - BAR_LENGTH / 2` 为什么不是 `x - RADIUS`**：

血条宽 50，坦克宽 40，**血条比坦克宽 10**。

```
错：x - RADIUS（左对齐）
   ┌──────────────────────────┐
   │       血条 50            │
   └──────────────────────────┘
   ┌────────────────┐
   │    坦克 40     │
   └────────────────┘
   ↑ 起点一样，但血条右边多出 10px，整体偏右

对：x - BAR_LENGTH / 2（居中）
 ┌──────────────────────────┐
 │        血条 50           │
 └──────────────────────────┘
    ┌────────────────┐
    │    坦克 40     │
    └────────────────┘
   两边各超出 5px，对称
```

**`BAR_LENGTH` 用偶数**：50 / 2 = 25 精确；51 / 2 = 25（截断），左右差 1 像素。

**`barY = y - RADIUS - BAR_HEIGHT * 2` 为什么乘 2**：

```
y - RADIUS            → 坦克顶边
- BAR_HEIGHT          → 血条本身高 5
- BAR_HEIGHT          → 再留 5 给头顶空隙
= y - RADIUS - 10
```

写成 `BAR_HEIGHT * 2` 而不是硬编码 `10`，**以后改血条高度，间距自动跟着变**。

#### 内部类访问外部字段的原理

`BloodBar` 里直接用了 `x` / `hp` / `RADIUS`，这些是 `Tank` 的字段。

**编译器偷偷给内部类塞了一个外部引用**：

```java
// 编译后大致相当于
class Tank$BloodBar {
    final Tank this$0;              // ← 自动生成的字段

    void draw(Graphics g) {
        g.fillRect(this$0.x - ..., ...);    // ← 自动加 this$0.
    }
}
```

所以能直接写 `x`，它就是 `Tank.this.x`。

**`BloodBar` 没加 `static`**，所以每个 `Tank` 对象都有一个自己的 `BloodBar` 实例，
绑定的 `this$0` 就是那个 `Tank`——**一一对应，不会串**。

> **内存泄漏风险**：如果 `BloodBar` 被外部持有（比如 `tank.getBloodBar()`），
> 它会连带拖住整个 `Tank` 对象。你现在是 `protected final` 只有自己用，安全。
> 通用的规律：**不需要访问外部实例成员的内部类，一律加 `static`**。

### 5.6 `MyTank.java` —— 玩家坦克

```java
public class MyTank extends Tank {
    private static Image[] tankImg;      // 玩家图只在这个类里

    static {
        tankImg = new Image[4];
        tankImg[0] = Toolkit.getDefaultToolkit().getImage("res/player_up.png");
        tankImg[1] = Toolkit.getDefaultToolkit().getImage("res/player_down.png");
        tankImg[2] = Toolkit.getDefaultToolkit().getImage("res/player_left.png");
        tankImg[3] = Toolkit.getDefaultToolkit().getImage("res/player_right.png");
    }

    public MyTank(int x, int y, int dir) {
        super(x, y, dir);                // 坐标和方向交给父类存
    }

    @Override
    public void drawImgTank(Graphics g) {
        g.drawImage(tankImg[getDir()],
                getX() - RADIUS + FIX,
                getY() - RADIUS + FIX,
                RADIUS << 1,
                RADIUS << 1,
                null);
    }
}
```

#### 为什么图片数组是 static

`drawImage` 每帧要调。如果图片在构造器里加载，每 new 一辆坦克就要重新读一遍磁盘。
加 `static` 后**每个类只加载一次**，所有同类型坦克共用同一份 `Image`。

重构后玩家图和敌人图分家了：`MyTank.tankImg` 只在玩家类里，`EnemyTank.enemyImg`
只在敌人类里，互不干扰。

#### 数组下标的含义

`tankImg[0]` 对应 `DIR_UP`，`[1]` 对应 `DIR_DOWN`，`[2]` 对应 `DIR_LEFT`，
`[3]` 对应 `DIR_RIGHT`。所以能直接写 `tankImg[getDir()]`，
**依赖「常量值 == 数组下标」这个约定**。改常量值必须同步改数组赋值顺序。

#### 为什么用 `getDir()` 而不是 `dir`

`dir` 在父类里是 `private`，子类看不见，只能通过 `getDir()` 拿。
同理 `getX()` / `getY()`。

> 注意 `RADIUS` 和 `FIX` 能直接用，是因为前者是 `public static final`、
> 后者是 `protected`。**访问修饰符决定了子类能不能直接碰**。

### 5.7 `EnemyTank.java` —— 敌人坦克

```java
public class EnemyTank extends Tank {
    private static Image[] enemyImg;

    static {
        enemyImg = new Image[4];
        enemyImg[0] = Toolkit.getDefaultToolkit().getImage("res/enemy_up.png");
        enemyImg[1] = Toolkit.getDefaultToolkit().getImage("res/enemy_down.png");
        enemyImg[2] = Toolkit.getDefaultToolkit().getImage("res/enemy_left.png");
        enemyImg[3] = Toolkit.getDefaultToolkit().getImage("res/enemy_right.png");
    }

    public EnemyTank(int x, int y, int dir) {
        super(x, y, dir);
    }

    @Override
    public void drawImgTank(Graphics g) {
        g.drawImage(enemyImg[getDir()], getX() - RADIUS + FIX, getY() - RADIUS + FIX,
                RADIUS << 1, RADIUS << 1, null);
    }
```

跟 `MyTank` 结构完全一样，只有图片数组不同。

#### createTank() —— 静态工厂方法（同时承担池化重置）

```java
public static EnemyTank createTank() {
    //生成在左上角还是右上角
    int x = MyUtil.getRandomNumber(0, 2) == 0 ? RADIUS : FRAME_WIDTH - RADIUS;
    int y = GameFrame.titleBarHeight + RADIUS;//标题栏+半径
    int dir = DIR_DOWN;

    EnemyTank enemy = EnemiesPool.get();     // ★ 从池子借，不是 new

    //重置生成时间，因为要根据生成时间算出坦克行为
    enemy.resetAiTime();
    enemy.setX(x);
    enemy.setY(y);
    enemy.setDir(DIR_DOWN);
    enemy.setEnemy(true);
    enemy.setState(STATE_MOVE);
    enemy.setHp(Tank.DEFAULT_HP);

    //清空敌人的弹夹，防止污染
    //归还残留子弹（敌人死时可能有子弹没飞完）
    for (Bullet b : enemy.getBullets()) {
        BulletsPool.theReturn(b);
    }
    enemy.getBullets().clear();

    return enemy;
}
```

**逐行拆解**：

| 行 | 作用 |
|---|---|
| `getRandomNumber(0, 2)` | 返回 `0` 或 `1`（右开区间，取不到 2） |
| `== 0 ? RADIUS : FRAME_WIDTH - RADIUS` | 0 走左边贴边，1 走右边贴边 |
| `y = titleBarHeight + RADIUS` | 贴着可绘区顶部出生 |
| `EnemiesPool.get()` | **从池子借，不是 `new`** |
| `resetAiTime()` | ★ 重置 AI 计时器，见下 |
| `setX / setY / setDir` | 坐标和方向 |
| `setEnemy(true)` | 打上敌人标记 |
| `setState(STATE_MOVE)` | 父类构造器默认是 `STATE_STAND`，这里让它立刻开始移动 |
| `setHp(DEFAULT_HP)` | ★ 恢复满血 |
| 清弹夹那段 | ★ 归还残留子弹，见下 |

#### ⚠️ resetAiTime() 为什么不能漏

`aiStartTime` 是 `EnemyTank` 的私有字段，只在**构造器**里赋值：

```java
public EnemyTank() {
    super();
    aiStartTime = System.currentTimeMillis();   // ← 只有这里
}
```

但池化后，`get()` 出来的对象**构造器不再执行**（池子里那 10 个是类加载时 new 的）。

于是：

```
t=1s    池子静态块 new EnemyTank() → aiStartTime = 1000
t=1~11s 敌人活了 10 秒，被打死 → 归还池子，aiStartTime 还是 1000
t=16s   新敌人从池子取出来，此时 currentTimeMillis() = 16000

ai() 里算：16000 - 1000 = 15000
15000 > ENEMY_AI_INTERVAL(3000) → 成立！
```

**新敌人第一帧就触发 AI 决策**，随机换 `state`、随机换 `dir`——看起来像出生瞬间乱跳。

`resetAiTime()` 就是为了把差值归零：

```
16000 - 16000 = 0 > 3000 ❌ 不成立，乖乖等 3 秒
```

#### ⚠️ 为什么必须清弹夹

敌人死亡瞬间，它身上的 `bullets` 里**可能还有子弹在飞**（子弹要飞出屏幕才消失，
要飞 100 多帧，约 3.4 秒）。

而尸体只在 `enemyTanks` 列表里留 450ms（等爆炸播完）。这期间：

```
GameFrame.drawEnemyTanks 第一趟：if (!isDie()) 才 draw()
    ↓
死了不调 draw() → 不调 drawBullets() → 子弹不 move、不出界、不回收
    ↓
子弹永久冻结在 bullets 列表里
```

归还池子后，下一个敌人 `get()` 到它，**一出生就带着上一轮的旧子弹**，
某处凭空冒出几颗子弹开始飞。

所以要在 `createTank()` 里**先归还再清空**：

```java
for (Bullet b : enemy.getBullets()) {
    BulletsPool.theReturn(b);       // ① 还回子弹池
}
enemy.getBullets().clear();          // ② 清空列表
```

**只 `clear()` 不归还** = 那些子弹对象失去引用，池子永久少几个，下次走兜底 `new`，池化失效。

#### 为什么用静态工厂而不是构造器

```java
// 构造器版本
new EnemyTank(x, y, dir)      // 调用方要自己算 x、y，还要自己 set 一堆东西

// 工厂版本
EnemyTank.createTank()        // 调用方什么都不用知道
```

**工厂方法的价值**：把「一辆新敌人该长什么样」这个知识**收在敌人类自己手里**。
`GameFrame` 只需要说"给我来一辆"，不需要知道出生点在哪、初始状态是什么。

以后要改成"从左中右三个口出生"或"按关卡难度调整出生位置"，**只改 `createTank()` 一处**，
`GameFrame` 一行都不用动。

> 注意这里是 `static` 方法——它不需要访问任何实例字段，只负责**创建**新实例。
> 所以能用 `EnemyTank.createTank()` 直接调，不必先有对象。
>
> **静态方法里能用实例字段吗？** 不能直接写 `aiStartTime = ...`（没有 `this`），
> 但可以 `enemy.setX(x)` / `enemy.resetAiTime()`——**通过对象引用访问完全合法**。

#### ai() —— 敌人 AI

```java
private void ai() {
    //系统时间-敌人创建时间 > ai决策间隔，就去设置一次状态
    if (System.currentTimeMillis() - aiStartTime > Constant.ENEMY_AI_INTERVAL) {
        setState(MyUtil.getRandomNumber(STATE_STAND, STATE_MOVE + 1));
        aiStartTime = System.currentTimeMillis();

        if (getState() == STATE_MOVE) {
            setDir(MyUtil.getRandomNumber(0, 4));
        }
    }

    //子弹发射
    if (Math.random() < Constant.ENEMY_FIRE_PERCENT) {
        fire();
    }
}
```

**两个独立的判定**：

| 判定 | 频率 | 作用 |
|---|---|---|
| `aiStartTime` 那条 | **3 秒一次** | 换状态、换方向 |
| `Math.random()` 那条 | **每帧都判** | 3% 概率开火 |

**为什么 AI 不能每帧随机方向**：每帧随机一次会原地抽搐，看不出在动。
用计时器隔一段时间决策一次，中间保持方向，才有"在走"的感觉。

> **开火那条是每帧判的**，所以实际射速 = 33帧/秒 × 3% ≈ **每秒 1 发**。
> 如果觉得太快，改 `ENEMY_FIRE_PERCENT` 或也加计时器。

**`ai()` 在 `drawImgTank()` 里被调用**，所以**死了就不决策**（因为死了不调 `draw()`）。

### 5.8 `Bullet.java` —— 子弹

#### 常量

| 常量 | 值 | 说明 |
|---|---|---|
| `DEFAULT_SPEED` | `Tank.DEFAULT_SPEED * 2` = 8 | 子弹速度是坦克的 2 倍 |
| `RADIUS` | 4 | 绘制半径，实际直径 8 像素 |

#### 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `x, y` | `int` | 子弹**中心点**坐标 |
| `speed` | `int` | 每帧位移，初值 `DEFAULT_SPEED` |
| `dir` | `int` | 飞行方向，取自发射时坦克的 `dir` |
| `atk` | `int` | 攻击力，取自坦克的 `atk`（快照） |
| `color` | `Color` | 颜色，取自坦克 → 子弹和坦克同色 |
| `visible` | `boolean` | **是否可见**，初值 `true`，飞出屏幕后置 `false` |

#### 两个构造器

```java
// 正常创建（池子方案下不会被调，留着备用）
public Bullet(int x, int y, int dir, int atk, Color color) { ... }

// 给对象池用的「空壳」构造器
public Bullet() {
}
```

**为什么必须有无参构造器**：`BulletsPool` 的静态块要 `new Bullet()` 创建 200 个空壳。
没有无参构造器就**编译不过**。

#### draw() —— 绘制

```java
public void draw(Graphics g) {
    if (!visible) return;      // 不可见就别画，也别动
    logic();
    g.setColor(color);
    g.fillOval(x - RADIUS, y - RADIUS, RADIUS << 1, RADIUS << 1);
}
```

子弹没走图片路线，直接用 `fillOval` 画圆——因为**原始素材里根本没有子弹**（见 6.5）。

> `logic()` 在 `if (!visible) return;` **之后**：子弹一旦不可见就彻底冻结，
> 连坐标都不再更新。这个顺序是对的，别调换。

#### move() —— 移动 + 出界判定

```java
private void move() {
    switch (dir) {
        case Tank.DIR_UP:
            y -= speed;
            if (y <= GameFrame.titleBarHeight) visible = false;
            break;
        case Tank.DIR_DOWN:
            y += speed;
            if (y >= FRAME_HEIGHT) visible = false;
            break;
        case Tank.DIR_LEFT:
            x -= speed;
            if (x <= 0) visible = false;
            break;
        case Tank.DIR_RIGHT:
            x += speed;
            if (x >= FRAME_WIDTH) visible = false;
            break;
    }
}
```

**「移动」和「出界判定」写在一起**：因为判定用的就是移动后的新坐标，分开写反而要多传值。

**`visible = false` 是回收信号**：`Tank.drawBullets()` 看到它就删掉并归还池子。
所以 `visible` 不是给玩家看的，是给回收逻辑看的一面旗子。

> UP 分支用 `titleBarHeight` 当上边界，和 `Tank.mathMove()` 的处理保持一致。

#### ⚠️ 子弹的「生死」和坦克不一样

| 对象 | 有「死亡」概念吗 | 判断方式 |
|---|---|---|
| 坦克 | 有 | `isDie()` → `hp <= 0` |
| **子弹** | **没有** | `isVisible()` → 飞出屏幕就 `false` |

子弹只有「在飞」和「飞出去了」两种状态。**不存在「敌人死了但子弹没死」这种判断**——
坦克和子弹是两个独立生命周期的对象，只是子弹**存在坦克的列表里**。

### 5.9 `BulletsPool.java` —— 子弹池

结构速览（详细原理见 4.6）：

```java
public static final int DEFAULT_POOL_SIZE = 200;   // 初始预创建数量
public static final int MAX_POOL_SIZE     = 300;   // 上限，超出丢弃归还
private static List<Bullet> pool = new ArrayList<Bullet>();

static { for (int i = 0; i < DEFAULT_POOL_SIZE; i++) pool.add(new Bullet()); }

public static Bullet get() { ... }                   // 借出
public static void theReturn(Bullet bullet) { ... }  // 归还
```

### 5.10 `ExplodesPool.java` —— 炸弹池

和 `BulletsPool` 结构完全一致，只换了类型和数量：

```java
public static final int DEFAULT_POOL_SIZE = 10;    // 初始 10 个（爆炸同时少）
public static final int MAX_POOL_SIZE     = 30;    // 上限
private static List<Explode> pool = new ArrayList<Explode>();

static { for (int i = 0; i < DEFAULT_POOL_SIZE; i++) pool.add(new Explode()); }

public static Explode get() { ... }                   // 借出
public static void theReturn(Explode explode) { ... }  // 归还
```

**为什么是 10 而不是 200**：池子大小按**同屏最大并发数**估。
子弹同屏可能几十颗，爆炸同时最多几个，差 20 倍。

> 构造函数用了 `private ExplodesPool() {}` 禁止 new，这一点和 `BulletsPool` 不同——
> 两边都是工具类，加上更规范。

### 5.11 `EnemiesPool.java` —— 敌人池

结构同前两个，**多了一个需要注意的地方：`EnemyTank` 自己持有两个池化对象列表**。

```java
public static final int DEFAULT_POOL_SIZE = 10;    // 同屏上限就是 10
public static final int MAX_POOL_SIZE     = 15;    // 留 5 个余量
private static List<EnemyTank> pool = new ArrayList<EnemyTank>();

private EnemiesPool() { }

static { for (int i = 0; i < DEFAULT_POOL_SIZE; i++) pool.add(new EnemyTank()); }

public static EnemyTank get() { ... }                    // 借出
public static void theReturn(EnemyTank enemy) { ... }    // 归还
```

**数量是 10/15 而不是 200**：`ENEMY_MAX_COUNT = 10`，同屏最多 10 辆敌人，
开 10 个池子刚好够用，留 5 个余量应对"死了正要归还、同时又要生成"的瞬间。

**和 `BulletsPool` / `ExplodesPool` 的本质差异**：

| | BulletsPool | ExplodesPool | EnemiesPool |
|---|---|---|---|
| 池化对象持有其他池化对象吗 | ❌ | ❌ | ✅ **持有 bullets 和 explodes** |
| 归还前要不要额外清理 | 不用 | 不用 | **要，见 4.6** |

**`EnemiesPool` 是唯一一个破了「util 不依赖游戏对象」约定的池**：

```java
import com.pawamamama.Tank.EnemyTank;    // ← util 反过来依赖 Tank 包
```

另外两个池依赖的是 `game.Bullet` / `game.Explode`（game 包）。
以前可以说"util 不认识 Tank 包"，现在不行了。

**准确的依赖分层**：

```
util → game（Bullet / Explode）
util → Tank（EnemyTank）        ← EnemiesPool
Tank → util（三个池都要用）
```

`Tank ↔ util` 是**双向依赖**，Java 允许，但设计上不干净。
真要理顺，得把三个池挪到 `Tank` 包或新建 `pool` 包。

### 5.12 `Explode.java` —— 爆炸

详见 6.4。核心结构：

```java
public static final int EXPLODE_FRAME_COUNT = 5;
private static final int[] RES_W = {11, 15, 16, 28, 34};   // 每帧原始宽
private static final int[] RES_H = {11, 14, 16, 29, 31};   // 每帧原始高
private static final int MULTIPLE_OF_SCALE = (Tank.RADIUS * 2) / 16;   // = 2
public  static final int SLOW = 3;      // ★ 一张图停留 3 次刷新
private static Image[] boomImg;         // 静态加载 5 张

private int x, y;         // 爆炸中心（不是左上角）
private int index;        // 播放帧计数（每帧 +1）
private int endIndex;     // 终止值（★ 调用方传进来的，已经乘过 SLOW）
private boolean isVisible;

public Explode() { }                          // 给池子用的空壳
public Explode(int x, int y, int endIndex)    // 正常创建
public void drawBoom(Graphics g)              // 画一帧 + index++
```

**关键字段 `endIndex`**：
- 构造器里**不乘 `SLOW`**，直接 `this.endIndex = endIndex`
- **`SLOW` 由调用方乘**：受击 `setEndIndex(3 * SLOW)` = 9，死亡 `setEndIndex(5 * SLOW)` = 15

> 早期版本构造器里乘过 `SLOW`、`setEndIndex` 没乘，导致 `setEndIndex(3)` 只播了 1 帧。
> 现在统一由调用方乘，见 10.3 ③。

---

## 六、素材系统

### 6.1 res 目录清单

| 文件 | 尺寸 | 状态 |
|---|---|---|
| `player_up.png` | 16 × 16 | ✅ `MyTank` 使用 |
| `player_down.png` | 16 × 16 | ✅ `MyTank` 使用 |
| `player_left.png` | 16 × 16 | ✅ `MyTank` 使用 |
| `player_right.png` | 16 × 16 | ✅ `MyTank` 使用 |
| `enemy_up.png` | 16 × 16 | ✅ `EnemyTank` 使用 |
| `enemy_down.png` | 16 × 16 | ✅ `EnemyTank` 使用 |
| `enemy_left.png` | 16 × 16 | ✅ `EnemyTank` 使用 |
| `enemy_right.png` | 16 × 16 | ✅ `EnemyTank` 使用 |
| `boom_0.png` | 11 × 11 | ✅ `Explode` 使用 |
| `boom_1.png` | 15 × 14 | ✅ `Explode` 使用 |
| `boom_2.png` | 16 × 16 | ✅ `Explode` 使用 |
| `boom_3.png` | 28 × 29 | ✅ `Explode` 使用 |
| `boom_4.png` | 34 × 31 | ✅ `Explode` 使用 |

素材来源：NES 版 Battle City 官方图集（The Spriters Resource），
按连通域分析切分提取（背景色 `(0,0,1)`，排除青色边框提示色）。

### 6.2 为什么素材是 16×16，坦克却是 40×40

素材保持**原始尺寸不动**，缩放交给 `drawImage`：

```java
g.drawImage(img, getX() - RADIUS, getY() - RADIUS, RADIUS << 1, RADIUS << 1, null);
//               ↑左上角                            ↑目标宽高 40
```

**为什么不在文件层面放大**：

| 做法 | 结果 |
|---|---|
| 用画图软件把 png 缩到 40×40 | 像素边界被抹成半透明灰边，发糊 |
| 素材留 16×16，代码指定 40×40 + 最近邻 | 每个像素块放大成 2.5 倍方块，边缘锋利 |

**代价**：16 × 2.5 = 40 是**非整数倍**，某些像素块占 2 像素宽、某些占 3 像素宽，
放大后不是绝对均匀。想完美对齐可以把 `RADIUS` 改成 24（16×3 = 48 = 24×2），
或改成 16（16×2 = 32 = 16×2）。当前 40 是"够看就行"的折中。

### 6.3 FIX 偏移：素材不是绝对居中

父类里：

```java
protected final int FIX = 1;
```

子类里：

```java
g.drawImage(tankImg[getDir()],
        getX() - RADIUS + FIX,   // 左上角 x，+FIX 抵消素材的偏左
        getY() - RADIUS + FIX,   // 左上角 y，+FIX 抵消素材的偏上
        RADIUS << 1,
        RADIUS << 1,
        null);
```

**为什么需要补偿**：素材格子里，坦克占的区域和格子的几何中心不重合。
放大 2.5 倍后这个偏差也被放大，表现为**子弹从炮管侧边飞出去**（因为 `fire()`
是从逻辑坐标 `(x, y)` 发射的，而画出来的炮管中心不在这里）。

补偿量 `FIX = 1` 是实测确定的——加上之后炮管中心对齐，子弹从正中射出。

### 6.4 爆炸序列帧（已接入）

5 张图尺寸各不相同（11×11 → 34×31），**不能直接都拉成同一个尺寸**——
如果每帧都放大到 40×40，像素块会忽粗忽细，看着像在抖。

**做法：用统一的缩放倍数，让每帧保持原始比例长大。**

```java
private static final int[] RES_W = {11, 15, 16, 28, 34};
private static final int[] RES_H = {11, 14, 16, 29, 31};
private static final int MULTIPLE_OF_SCALE = (Tank.RADIUS * 2) / 16;   // 40/16 = 2
```

缩放后尺寸：

| 帧 | 原始 | ×2 后 | 和坦克（40×40）比 |
|---|---|---|---|
| boom_0 | 11×11 | 22×22 | 小一圈 |
| boom_1 | 15×14 | 30×28 | 小 |
| boom_2 | 16×16 | 32×32 | 略小 |
| boom_3 | 28×29 | 56×58 | 大 1.4 倍 |
| boom_4 | 34×31 | 68×62 | 大 1.7 倍 |

**这条尺寸曲线正是「受击用前 3 帧、死亡用 5 帧」的依据**：
前 3 帧比坦克小（像装甲上的火花），后 2 帧比坦克大（像整车炸开）。

#### 居中：每帧都要重算左上角

`drawImage` 画的是**左上角**，而 5 帧尺寸不同，所以要按各自尺寸反算：

```java
int w = RES_W[index] * MULTIPLE_OF_SCALE;
int h = RES_H[index] * MULTIPLE_OF_SCALE;
g.drawImage(boomImg[index], x - w / 2, y - h / 2, w, h, null);
//                            ↑           ↑
//                        中心-半宽    中心-半高
```

**`(x, y)` 是爆炸中心，从构造到销毁一直不变**，变的是「从中心往外退多少」。

```
boom_0  22×22        boom_4  68×62
  ┌─────┐          ┌───────────┐
  │  ●  │          │           │
  └─────┘          │     ●     │
                   │           │
                   └───────────┘

● 永远在同一个点上，火团一圈圈往外扩
```

**如果只存左上角**（固定 `absX, absY`），图变大时多出来的部分全往右下挤，
看起来像「爆炸在往右下飘」，而不是「在原地炸开」。

> **整数除法为什么没出问题**：×2 后所有尺寸都是偶数（22、30、32、56、68），
> `w/2` 是精确的。如果改成 2.5 倍会出现奇数（27、37），`w/2` 丢 0.5 像素，播放时会抖 1px。

#### 分帧播放：`SLOW` 控制每张图停留几帧

刷新间隔 30ms，如果每帧都换图，5 张图 150ms 就播完，眼睛看不见。

用 `SLOW` 把「播放帧」和「图片帧」分开：

```java
private static final int SLOW = 3;      // 每张图停留 3 次刷新

public void drawBoom(Graphics g) {
    int Real_index = index / SLOW;      // 除 3 得到真实图片下标
    ...
    index++;                            // 播放帧每帧 +1
}
```

`index` 每帧 +1，但**除以 3 之后要 3 帧才变一次**：

| 调用次数 | index | index/3 | 画哪张 |
|---|---|---|---|
| 1~3 | 0,1,2 | **0** | boom_0 |
| 4~6 | 3,4,5 | **1** | boom_1 |
| 7~9 | 6,7,8 | **2** | boom_2 |
| 10~12 | 9,10,11 | **3** | boom_3 |
| 13~15 | 12,13,14 | **4** | boom_4 |

**5 帧 × 3 次 × 30ms = 450ms**，接近原版手感。

想调快调慢只改 `SLOW`，不用动逻辑。

#### 终止帧：受击 3 帧 / 死亡 5 帧

`endIndex` 也是**放大后的值**：

```java
// 受击：播 3 帧 → endIndex = 3 × 3 = 9
explode.setEndIndex(3 * Explode.SLOW);

// 死亡：播 5 帧 → endIndex = 5 × 3 = 15
explode.setEndIndex(5 * Explode.SLOW);
```

判断条件：

```java
if (Real_index >= EXPLODE_FRAME_COUNT || index >= endIndex) {
    isVisible = false;
    return;                          // ★ 必须 return，否则继续往下画会数组越界
}
```

**两个条件缺一不可**：
- `Real_index >= 5` 防止 `RES_W[5]` 越界
- `index >= endIndex` 让受击能提前停在 3 帧

### 6.5 素材里没有的东西

原始图集里**没有子弹**。所以子弹用 `fillOval` 代码画圆（半径 4，颜色跟随坦克）。

这不是偷懒——经典坦克大战的子弹本来就是个白点，代码画完全够。

---

## 七、一次按键到屏幕的完整链路

### 7.1 按 ↑ 让坦克移动

```
① 用户按 ↑
      │
      ▼
② EDT 回调 KeyAdapter.keyPressed(e)      keyCode = VK_UP
      │
      ▼
③ switch (gameState) → STATE_RUN → keyEventRun(keyCode)
      │
      ▼
④ myTank.setDir(Tank.DIR_UP)         // 只改朝向
   myTank.setState(Tank.STATE_MOVE)  // 打开"移动开关"
      │   ← 这一步结束，屏幕还没变
      ▼
⑤ 30ms 后，重绘线程执行 repaint()
      │
      ▼
⑥ AWT 回调 GameFrame.update(g1)
      │
      ▼
⑦ drawRun(g)              ← g 是「缓冲区」画笔
      ├─ g.fillRect(...)  清屏
      ├─ drawEnemyTanks(g) → 每辆敌人各自 draw
      └─ myTank.draw(g)
             │
             ▼
⑧ Tank.draw(g)             ← 抽象父类的方法
      ├─ logic()          → state==MOVE → mathMove() → y -= 4（带夹紧）
      ├─ drawImgTank(g)   → ★ 多态：实际调 MyTank 的实现
      └─ drawBullets(g)   → 遍历弹夹，每颗子弹自己 draw
             │
             ▼
⑨ g1.drawImage(bufferedImage, 0, 0, this)   整张缓冲区贴到屏幕
      │
      ▼
⑩ 屏幕上的坦克往上挪了 4 像素
```

**⑧ 那一行是这次重构最关键的地方**：`myTank` 声明成 `Tank` 类型，
但实际对象是 `MyTank`，所以 `drawImgTank(g)` 调用时**走的是 `MyTank` 的实现**。
这叫**动态绑定 / 多态**。

```
myTank.draw(g)          myTank 的实际类型是 MyTank
    ↓
Tank.draw(g)            父类的 draw 先执行
    ↓
drawImgTank(g)          ← 运行时查表：MyTank 重写了 → 调 MyTank 的版本
    ↓
MyTank.drawImgTank()    画玩家图
```

如果是敌人，同一条链路会走到 `EnemyTank.drawImgTank()`。**父类的 `draw()` 一个字都不用改。**

松开 ↑ 键：

```
keyReleased → gameState==STATE_RUN → keyReleasedEventRun
            → myTank.setState(STATE_STAND)
            → 之后 logic() 走 STATE_STAND 分支，什么都不做，坦克停住
```

### 7.2 按空格开火

```
① 用户按空格
      │
      ▼
② keyEventRun → case VK_SPACE: myTank.fire()
      │
      ▼
③ Tank.fire()
      ├─ 按 dir 算出炮管口坐标 (bulletX, bulletY)
      ├─ BulletsPool.get()          ← 借一颗（池子 200 → 199）
      ├─ setX / setY / setDir / setAtk / setColor / setVisible(true)
      └─ bullets.add(bullet)        ← 进弹夹
      │   ← 此刻屏幕上还看不到（这一帧已经画过了）
      ▼
④ 下一帧 Tank.draw(g) → drawBullets(g) → bullet.draw(g)
      ├─ logic() → move()   y/x 按 dir 移动 8 像素
      └─ fillOval(...)      画半径 4 的圆
      │
      ▼
⑤ 之后每一帧它自己往前走 8 像素
      │
      ▼
⑥ 走到边界：move() 里 visible = false
      │
      ▼
⑦ 下一帧 drawBullets() 第二趟遍历：
      ├─ bullets.remove(i)
      └─ BulletsPool.theReturn(bullet)   ← 还给池子（池子 199 → 200）
```

**注意⑥⑦之间的时差**：`visible` 置 false 在它**自己**的 `move()` 里，
回收在**下一帧**的 `drawBullets()` 里。所以子弹会"多活一帧"——不影响体验。

> 回收循环用**倒序** `for (int i = bullets.size() - 1; i >= 0; i--)`，
> 因为正序删除时 `remove(i)` 后元素前移、`i++` 会跳过下一个。

### 7.3 子弹打中坦克：从命中到爆炸播完

这是本次新增的完整链路：

```
① 某一帧的 drawRun → bulletCollideTank()
      │
      ▼
② GameFrame.bulletCollideTank()  遍历每辆敌人：
      ├─ myTank.collideBullets(enemyTank.getBullets())   敌人子弹 → 我
      └─ enemyTank.collideBullets(myTank.getBullets())   我的子弹 → 敌人
      │
      ▼
③ Tank.collideBullets(bullets)
      └─ 遍历子弹，用 isRectangleCollision 判断是否进了坦克的方框
             │
             │ 命中！
             ▼
④ ① bullet.setVisible(false)             子弹消失
   ② ExplodesPool.get()                  从池子借爆炸（池子 10 → 9）
   ③ 逐个字段重置（★ 漏一个就出脏数据 bug）：
        setVisible(true)
        setIndex(0)                      ← 最容易漏，不归零会接着上轮播
        setX / setY                      位置
        setEndIndex(3 * SLOW)            受击 3 帧
   ④ explodes.add(explode)              进坦克自己的爆炸列表
      │   ← 这一帧只做了「创建」，还没画
      ▼
⑤ 同一帧稍后 Tank.draw → deawExlpodes(g)
      └─ 遍历 explodes，每个 drawBoom(g)
             │
             ▼
⑥ Explode.drawBoom(g)  第 1 次调用
      ├─ Real_index = 0 / 3 = 0        → boom_0
      ├─ 画在 (x - w/2, y - h/2)        居中
      └─ index++                        → index = 1
      │
      ▼
⑦ 之后每一帧都被调一次：
      第 2、3 次 → 还是 boom_0（index 1,2 → 0）
      第 4 次    → boom_1（index 3 → 1）
      ...
      第 9 次    → index = 9 ≥ endIndex(9) → isVisible = false
      │
      ▼
⑧ 下一次 deawExlpodes 的回收循环：
      └─ !isVisible → explodes.remove(i) + ExplodesPool.theReturn(explode)
```

**关键点**：

| 阶段 | 发生几次 |
|---|---|
| `collideBullets` 创建爆炸 | **1 次**（命中那一帧） |
| `drawBoom` 推进帧 | **9 次**（受击 3 帧 × SLOW 3） |
| 回收 | **1 次**（播完的下一帧） |

**「一次性事件」变成「持续一段时间的状态」**，靠的是把 `Explode` 对象放进 `List` 里，
然后每帧遍历它——对象活多久，动画就播多久。

### 7.4 敌人是怎么出现的

这是之前新增的完整链路：

```
① 用户在菜单按回车
      │
      ▼
② keyEventMenu → newGame()
      ├─ gameState = STATE_RUN
      ├─ myTank = new MyTank(100, 100, Tank.DIR_DOWN)
      └─ new Thread(lambda).start()        ★ 启动生成线程
      │
      ▼
③ 生成线程（独立于 30ms 重绘线程）循环：
      │
      ├─ if (enemyTanks.size() < 10)
      │       enemyTanks.add(EnemyTank.createTank())
      │              │
      │              ▼
      │        createTank() 内部：
      │          ├─ x = 随机左边或右边贴边
      │          ├─ y = titleBarHeight + RADIUS（贴顶部）
      │          ├─ dir = DIR_DOWN（朝下）
      │          ├─ EnemiesPool.get()        ← 从池子借，不是 new
      │          ├─ resetAiTime()            ← ★ 重置 AI 计时器
      │          ├─ setX / setY / setDir
      │          ├─ setEnemy(true)
      │          ├─ setState(STATE_MOVE)     ← 一出生就走
      │          ├─ setHp(DEFAULT_HP)
      │          └─ 清弹夹 + 归还残留子弹     ← ★ 见 5.7
      │
      └─ Thread.sleep(5000)                 ★ 每 5 秒一辆
      │
      ▼
④ 重绘线程下一次 repaint() → drawRun → drawEnemyTanks()
      └─ 遍历 enemyTanks，每辆调 draw(g)
             │
             ▼
       Tank.draw(g) → logic()（STATE_MOVE → 往下走 4 像素）
                    → drawImgTank(g) → 多态 → EnemyTank 的实现 → 画敌图
                    → drawBullets / drawName / bloodBar
      │
      ▼
⑤ 屏幕上多了一辆朝下开的敌人坦克
```

**关键点**：这里其实有**三个线程**在跑

| 线程 | 干什么 | 频率 |
|---|---|---|
| EDT（AWT 事件线程） | 处理按键、回调 `update()` 画图 | 按键时 / `repaint()` 后 |
| 重绘线程 | `while(true) repaint(); sleep(30)` | 30 ms |
| 生成线程 | `while(true) add敌人; sleep(5000)` | 5000 ms |

**所以 `enemyTanks` 会被两个线程同时访问**：生成线程往里 `add`，EDT/重绘链路里遍历 `draw` 和 `remove`。

目前能正常跑，因为：

- 生成线程只做 `add`（往列表**末尾**追加），不改已有元素
- 遍历用索引 for，`size()` 每轮重新读
- `sleep(5000)` 让 `add` 极其稀疏
- `remove` 是倒序原位删除，不改结构以外的部分

> **理论风险仍在**：`ArrayList` 不是线程安全的。`add` 触发的扩容（`Arrays.copyOf`）
> 如果正好撞上 EDT 的遍历，可能读到中间态。
> 概率极低（5 秒一次 vs 30ms 一次），但真要根治得换 `CopyOnWriteArrayList`。
> **目前不处理**，等真报 `ConcurrentModificationException` 再说。

### 7.5 敌人从被打死到归还池子

这是「敌人死亡处理」的完整链路，涉及三个类：

```
① 玩家子弹命中
      │  Tank.collideBullets(myTank.getBullets())
      ▼
② 命中判断 → bullet.setVisible(false)
      │
      ▼
③ hurt(bullet)   → hp -= atk
      │            if (hp <= 0) die()
      ▼
④ die()   → 敌人：什么都不做（不归还！）
      │     玩家：//gameover todo
      ▼
⑤ 回到 collideBullets，按扣血后的 hp 分流：
      ├─ hp > 0  → 受击爆炸（3 帧，命中点）
      └─ hp <= 0 → 死亡爆炸（5 帧，坦克中心）
      │
      ▼
⑥ 同一帧稍后 GameFrame.drawExplodes()
      └─ 遍历 enemyTanks，每辆 deawExlpodes(g)
         → 死亡爆炸开始播
      │
      ▼
⑦ 下一帧 GameFrame.drawEnemyTanks()
      ├─ 第一趟：if (!isDie()) draw()     ← 死者跳过，车身不画
      └─ 第二趟：if (isDie() && isExplodeFinished()) → remove + theReturn
      │
      │  ← 此时 explodes 里还有没播完的（15 次刷新），删不掉
      ▼
⑧ 尸体在 enemyTanks 里留 450ms（5 帧 × SLOW 3 × 30ms）
      每帧：drawExplodes 画它身上的爆炸 ✅
      每帧：collideBullets 开头 if (isDie()) return ← 不再挨打 ✅
      │
      ▼
⑨ 爆炸播完 → explodes 空 → isExplodeFinished() = true
      │
      ▼
⑩ drawEnemyTanks 第二趟：remove(i) + EnemiesPool.theReturn(enemyTank)
      │
      ▼
⑪ 下次 createTank() 取到这个对象时：
      ├─ 清弹夹 + 归还残留子弹   ← 因为尸体期间子弹冻结了
      ├─ resetAiTime()
      └─ 重置坐标/血量/状态
```

**时间轴对照**：

| 时刻 | explodes | 在 enemyTanks 里 | 车身画吗 | 爆炸画吗 | 能挨打吗 |
|---|---|---|---|---|---|
| 死亡当帧 | 1 个（5 帧） | ✅ | ❌ | ✅ | ❌ |
| 死亡后 15ms | 1 个 | ✅ | ❌ | ✅ | ❌ |
| 死亡后 449ms | 1 个（快播完） | ✅ | ❌ | ✅ | ❌ |
| 死亡后 450ms | 空 | ✅（这一帧删） | ❌ | ❌ | ❌ |
| 死亡后 480ms | — | ❌ | ❌ | ❌ | — |

**三个设计要点**：

**① 归还只能有一处**

`die()` 里**不归还**，`GameFrame.drawEnemyTanks` 里才归还。
如果两处都归还，对象会同时存在于列表和池子，坐标、血量全乱。

**② 删除条件必须带 `isExplodeFinished()`**

```java
if (enemyTank.isDie()) {            // ❌ 死就删
    remove + theReturn
}
// 死了立刻删 → 不在 enemyTanks 里 → drawExplodes 遍历不到
// → 5 帧死亡爆炸永远画不出来
```

```java
if (enemyTank.isDie() && enemyTank.isExplodeFinished()) {   // ✅ 等爆炸播完
    remove + theReturn
}
```

**③ 防鞭尸必须加**

尸体留 450ms，这期间玩家子弹还能打到它。不加 `if (isDie()) return;`：

```
打尸体 → hurt() → hp 从 0 变 -100 → die() 又执行
      → 又 add 一个 5 帧爆炸
      → explodes 永远不空
      → 永远不满足删除条件
      → 死循环，尸体永久留在列表里
```

**④ 归还前必须清弹夹**

尸体期间 `draw()` 不被调用 → `drawBullets()` 不被调用 → 子弹不 `move()`、不出界、不回收
→ 永久冻结在列表里 → 归还后新敌人一出生就带着旧子弹。

所以 `createTank()` 里要先归还再 `clear()`。

---

## 八、设计要点小结

| 设计 | 做法 | 好处 |
|---|---|---|
| 状态机 | `gameState` + `switch` | 各界面互不干扰，加新界面只加一个 case |
| 渲染循环 | 独立线程 + `repaint()` | 逻辑和绘制解耦，帧率可控 |
| 双缓冲 | `BufferedImage` + 整张 `drawImage` | 不闪屏、不撕裂 |
| 最近邻缩放 | `VALUE_INTERPOLATION_NEAREST_NEIGHBOR` | 像素画放大不发糊 |
| **抽象父类** | `abstract Tank` + `abstract drawImgTank()` | 加新坦克不改父类 |
| **多态** | 字段声明成 `Tank`，实际是子类 | 调用方不用关心具体类型 |
| **静态工厂** | `EnemyTank.createTank()` | 出生逻辑收在敌人类自己手里 |
| 方向与状态分离 | `dir` 管朝向、`state` 管动不动 | 转向和移动互不耦合 |
| 常量集中 | `Constant` + 静态导入 | 改数值不用满项目找 |
| 工具类 | 私有构造器 + 全 static | 语义清晰，不会被误 new |
| 坐标用中心点 | 绘制时 `-RADIUS` 换算左上角 | 旋转、碰撞检测都方便 |
| 边界用 clamp | `Math.max` / `Math.min` | 不会越界，能精确贴边 |
| 图片 static 缓存 | `static Image[]` + 静态块 | 每个类只读一次盘 |
| 常量值当数组下标 | `tankImg[getDir()]` | 少写一层 switch 映射 |
| 对象池 | `BulletsPool` / `ExplodesPool` / `EnemiesPool` | 三类高频对象反复复用，减少 GC |
| `visible` 当回收旗 | 置 false → 下一帧归还 | 回收逻辑和绘制逻辑解耦 |
| 独立生成线程 | `new Thread(lambda)` + `sleep` | 生成节奏和帧率解耦 |
| **分帧动画** | `index / SLOW` 把播放帧和图片帧分开 | 一个数字控制播放速度 |
| **每帧重算左上角** | `中心 - 该帧宽高/2` | 尺寸不同的帧也能围绕同一点炸开 |
| **尺寸决定位置** | 小帧贴命中点，大帧必须居中 | 大火团偏离中心会看起来像炸歪 |
| **倒序删列表** | `for (i = size-1; i >= 0; i--)` | 边遍历边删不会漏 |
| **攻击力存两处** | 坦克持有能力，子弹携带快照 | 子弹飞行中坦克升级/死亡不影响这颗子弹 |
| **血条三层叠加** | 黄底 / 红条 / 白框 | 少了任一层就看不出血量比例 |
| **`FontMetrics` 居中** | `x - stringWidth(name)/2` | 名字几个字都居中 |
| **内部类访问外部字段** | `BloodBar` 直接用 `x` / `hp` | 编译器自动加 `this$0`，代码短 |
| **死亡延迟删除** | `isDie() && isExplodeFinished()` | 保证死亡爆炸播得完 |
| **防鞭尸** | `collideBullets` 开头 `if (isDie()) return` | 尸体多留 450ms 也不会重复加爆炸 |

---

## 九、这次重构：从单类到继承

### 9.1 重构前的样子

```
Tank.java（一个类）
  ├─ static Image[] tankImg      ← 玩家图
  ├─ static Image[] enemyImg     ← 敌人图
  ├─ boolean isEnemy
  └─ drawImgTank() {
         if (isEnemy) 画敌人图
         else         画玩家图
     }
```

`GameFrame` 里是：

```java
private Tank myTank;
```

敌人还没做，所以 `isEnemy` 永远是 `false`，`enemyImg` 加载了却从来没用过。

### 9.2 触发重构的原因

要加敌人了。如果继续用 `if`：

```java
private void drawImgTank(Graphics g) {
    if (isEnemy) {
        g.drawImage(enemyImg[dir], ...);
    } else {
        g.drawImage(tankImg[dir], ...);
    }
}
```

**往下的路会越来越难走**：

| 想加的东西 | 用 if 的代价 |
|---|---|
| 敌人生成器 | `if (type == GENERATOR)` 加一层 |
| Boss 坦克 | 再加一层 |
| 护盾坦克 | 再加一层 |
| 每种的速度/血量不同 | 构造器里一堆 if |

`drawImgTank` 会变成一个巨大的 switch，而且**每加一种坦克都要回来改这个类**。

### 9.3 重构后

```
Tank.java（抽象父类）
  ├─ 所有共用字段和方法
  └─ abstract drawImgTank(g)        ← 只有声明

MyTank.java（40 行）
  ├─ static Image[] tankImg
  └─ drawImgTank() → 画玩家图

EnemyTank.java（58 行）
  ├─ static Image[] enemyImg
  ├─ drawImgTank() → 画敌人图
  └─ static createTank()
```

**关键动作**：

| 动作 | 效果 |
|---|---|
| `Tank` 加 `abstract` | 不能 `new Tank()`，编译器帮你守住"坦克必须具体" |
| `drawImgTank` 变抽象 | 子类不实现就编译不过，漏不掉 |
| `tankImg` 搬到 `MyTank` | 玩家类管玩家图 |
| `enemyImg` 搬到 `EnemyTank` | 敌人类管敌人图 |
| `FIX` 改成 `protected final` | 子类能用，但不能改 |
| `isEnemy` 保留 | 还能用，但 `drawImgTank` 不再依赖它 |

### 9.4 前后对比

| 项 | 重构前 | 重构后 |
|---|---|---|
| 类数量 | 1 | 3 |
| 加新坦克 | 改 `drawImgTank` 的 if | 新建一个 `extends Tank` 的类 |
| 图片归属 | 两套图挤在一个类 | 各归各类 |
| 编译期保障 | 无（忘了处理某个 type 也能过） | 有（不实现抽象方法就报错） |
| `GameFrame` 改动 | — | 只需 `List<Tank>`，不用管具体类型 |

### 9.5 一个具体的好处：`GameFrame` 不用改

看 `drawEnemyTanks()`：

```java
for (int i = 0; i < enemyTanks.size(); i++) {
    enemyTanks.get(i).draw(g);
}
```

这里**完全不知道** `enemyTanks` 里装的是 `EnemyTank`——它只知道"这是一堆 `Tank`"。
以后往列表里塞 `BossTank`、`ShieldTank`，这段代码**一个字都不用改**。

这就是抽象的价值：**让调用方只依赖"能做什么"，不依赖"是什么"。**

### 9.6 之后如果要做「敌人种类」

现在的写法已经留好了口子：

```java
public abstract class EnemyTank extends Tank {   // 变成抽象中间层
    // 所有敌人的共性：AI、开火冷却、掉落物
    public abstract int getScore();              // 每种敌人分值不同
}

public class NormalEnemy extends EnemyTank { ... }
public class FastEnemy extends EnemyTank { ... }
public class BossEnemy extends EnemyTank { ... }
```

`GameFrame` 里照样 `List<Tank> enemyTanks`，一行不用动。

---

## 十、当前进度与下一步

### 10.1 已完成

| 模块 | 状态 |
|---|---|
| 窗口 + 标题栏高度 | ✅ |
| 菜单显示与翻页 | ✅ |
| 状态机 5 态 | ✅ |
| 双缓冲 + 最近邻缩放（缓冲区 + 屏幕两层） | ✅ |
| 窗口放大 1.5 倍（逻辑 900×700，显示 1350×1050） | ✅ |
| 玩家移动（四方向 + 边界夹紧） | ✅ |
| 图片绘制坦克 | ✅ |
| 开火 + 子弹飞行 | ✅ |
| 对象池回收子弹 | ✅ |
| 抽象 Tank 类 | ✅ |
| MyTank / EnemyTank 拆分 | ✅ |
| 敌人定时生成 + 上限控制 | ✅ |
| 敌人随机 AI（定时换向 + 概率开火） | ✅ |
| 子弹↔坦克碰撞检测（矩形判定） | ✅ |
| 爆炸五帧动画 | ✅ |
| 爆炸缩放 + 居中 | ✅ |
| 分帧控制（`SLOW`） | ✅ |
| 受击 3 帧 / 死亡 5 帧 | ✅ |
| 炸弹对象池 | ✅ |
| **伤害系统**（`DEFAULT_ATK` / `Bullet.atk` / `hurt()`） | ✅ |
| **坦克随机名字**（`MyUtil.getRandomName()`） | ✅ |
| **血条**（内部类 `BloodBar`，三层叠加） | ✅ |
| **敌人对象池**（10/15，含级联清理） | ✅ |
| **敌人死亡完整处理**（防鞭尸 + 等爆炸播完 + 归还池子） | ✅ |

### 10.2 你代码里留的 TODO / 待接

| 位置 | 内容 |
|---|---|
| **`Tank.die()`** | **`!isEnemy` 分支是空的 —— gameover 待接** |
| **`GameFrame.drawOver`** | **空 —— 要画「游戏结束」界面** |
| **`GameFrame.keyEventOver`** | **空 —— 要处理「按回车回菜单」** |
| **`GameFrame.newGame`** | **无防重入 —— gameover 后重复调用会起多个生成线程** |
| `GameFrame.drawHelp` | 空 |
| `GameFrame.drawAbout` | 空 |
| `GameFrame.keyEventAbout` | 空 |
| `GameFrame.keyEventHelp` | 空 |
| `GameFrame.keyEventMenu` | `VK_ENTER` —— 5 个菜单项都走 `newGame()`，还没按 `menuIndex` 分发 |
| `Tank.bullets` / `Tank.explodes` | `new ArrayList()` 用了裸类型，可改成 `new ArrayList<>()` |
| `Tank.getBullets()` / `getExplodes()` | 返回裸 `List`，可泛型化 |
| `EnemyTank.createTank` | `int dir` 声明了但没用（`setDir(DIR_DOWN)` 用的是常量） |
| `Tank.hurt` | 有调试用的 `System.out.println("atk: " + atk)`，上线要删 |

**菜单分发的参考写法**（等你做到那一步）：

```java
case KeyEvent.VK_ENTER:
    switch (menuIndex) {
        case 0: newGame();                break;   // 开始游戏
        case 1: /* 继续游戏 */              break;
        case 2: gameState = STATE_HELP;   break;
        case 3: gameState = STATE_ABOUT;  break;
        case 4: System.exit(0);           break;   // 退出
    }
    break;
```

### 10.3 已踩过并修复的坑（附录）

这些都已经改对了，记下来是因为同类问题还会再遇到。

#### ① 遍历中删除必须倒序

```java
// 正序：remove(i) 后，i+1 的元素挪到 i，循环 i++ 会跳过它
for (int i = 0; i < list.size(); i++) { ... list.remove(i); ... }

// 倒序：被删元素后面的都已经遍历过了，不受影响
for (int i = list.size() - 1; i >= 0; i--) { ... list.remove(i); ... }
```

**踩过的坑**：`deawExlpodes` 曾写成 `for (int i = explodes.size(); i > 0; i--)`，
起点是 `size()` 而不是 `size()-1`，第一次就 `get(size())` 越界崩溃。
而且循环体里又写了 `i--`，和 for 自己的 `i--` 叠加，会跳过元素。

`drawBullets` 和 `deawExlpodes` 现在都改成了标准倒序。

#### ② 对象池取出后，所有字段都要重置

**踩过的坑（三次）**：

| 对象 | 漏掉的字段 | 症状 |
|---|---|---|
| `Bullet` | `visible` | 第二发子弹"发射即消失" |
| `Explode` | `index` | 第二轮爆炸从上一轮位置接着播 |
| `EnemyTank` | `aiStartTime` | 新敌人一出生就疯狂决策、乱换方向 |

**`aiStartTime` 这个最难发现**，因为它只在构造器里赋值，而池化后构造器不再执行。
而且它的症状不是崩溃，是"行为怪异"——敌人出生就乱跳，容易误判成 AI 逻辑写错。

**更省心的写法是给每个池化类加一个 `reset(...)`**，把重置收在一处，
以后加字段只改那一处，不用去所有调用点检查。

#### ③ `setter` 不做构造器做过的换算

**踩过的坑**：`Explode` 构造器里 `this.endIndex = endIndex * SLOW`，
但 `setEndIndex` 直接 `this.endIndex = endIndex`，**没乘 `SLOW`**。

结果 `setEndIndex(3)` 得到 `endIndex = 3`，`index` 涨到 3 就停，
**只播了 boom_0，一张图都没换过**。

现在改成构造器和 setter 都不乘，**统一由调用方乘**：
`explode.setEndIndex(3 * Explode.SLOW)`。

> 关键不是"谁来乘"，而是**只有一处乘**。一边乘一边不乘必然出错。

#### ④ `if (条件) 标记 = false` 之后必须 `return`

**踩过的坑**：`drawBoom` 里

```java
if (Real_index >= EXPLODE_FRAME_COUNT || index >= endIndex) {
    isVisible = false;      // ← 只是标记
}
drawScaling(g, Real_index); // ← 还是继续画了
```

死亡爆炸 `endIndex = 15`，`index` 到 15 时 `Real_index = 5`，
`RES_W[5]` **数组越界崩溃**。

**3 帧不崩、5 帧崩**：受击停在 `Real_index = 3`（合法），死亡停在 5（越界）。

修法是标记完立刻 `return`。

#### ⑤ 对象池归还前，要清空并归还内部集合

**踩过的坑**：`EnemyTank` 持有 `bullets` 和 `explodes` 两个列表，卸载时没清。

敌人死时身上可能有子弹在飞（子弹要飞 100+ 帧），尸体期间 `draw()` 不被调用，
子弹冻结在列表里无法回收。归还池子后，下一个敌人一出生就带着旧子弹。

**规律**：池化对象如果持有**其他池化对象**，归还前必须把它们还回去再 `clear()`。
只 `clear()` 不归还 = 那些对象失去引用，池子永久少几个，下次走兜底 `new`，池化失效。

#### ⑥ 归还只能有一处

**踩过的坑（差点踩）**：`die()` 里归还敌人，`drawEnemyTanks` 里也归还。

```
die() → theReturn → 对象进池子
     但还在 enemyTanks 列表里
     ↓
下一个 get() 拿到它 → createTank 改坐标
     ↓
列表里那个"尸体"坐标突然变成出生点
```

**一个对象被两个地方同时持有，改哪边都是错的。**
「从列表删除」和「归还池子」必须原子化，放在同一个循环里。

#### ⑦ 死亡延迟删除的条件不能只有「死了」

**踩过的坑**：`drawEnemyTanks` 里 `if (enemyTank.isDie())` 就删。

```
死了立刻删 → 不在 enemyTanks 里
          → drawExplodes 遍历不到
          → 5 帧死亡爆炸永远画不出来
```

必须加 `&& isExplodeFinished()`，让尸体留到爆炸播完。

**但加了之后必须配防鞭尸**（`collideBullets` 开头 `if (isDie()) return`），
否则尸体这 450ms 里还会挨打、还会加爆炸，`explodes` 永远不空，**死循环**。

---

## 十一、后续方向

### 11.1 敌人 AI

`Tank.logic()` 现在是：

```java
private void logic() {
    switch (state) {
        case STATE_STAND: break;
        case STATE_MOVE:  mathMove(); break;   // ← 这里加 AI
        case STATE_DIE:   break;
    }
}
```

**接入点就在 `STATE_MOVE` 那行之前**。最简单的做法是给父类加一个可重写的方法：

```java
// Tank.java
protected void beforeMove() {
    // 空实现，玩家坦克不需要
}

private void logic() {
    switch (state) {
        case STATE_STAND: break;
        case STATE_MOVE:
            beforeMove();      // ← 敌人重写它
            mathMove();
            break;
        case STATE_DIE:   break;
    }
}
```

```java
// EnemyTank.java
@Override
protected void beforeMove() {
    if (--thinkTick > 0) return;      // 还没到决策时间，沿用上次方向
    thinkTick = THINK_INTERVAL;

    int d = MyUtil.getRandomNumber(0, 4);
    if (!isBlocked(d)) {
        setDir(d);
    }
}
```

**关键：AI 不能每帧算**。每帧随机一次会原地抽搐，要用计数器隔一段时间决策一次。

难度分档靠**参数**，不是靠 AI 复杂度：

```java
// 思考间隔, 开火间隔, 朝目标概率, 速度
public static final int[][] DIFFICULTY = {
    {20, 90, 20, 2},   // 简单
    {14, 60, 40, 3},   // 普通
    {8,  40, 70, 4},   // 困难
    {5,  25, 90, 5},   // 地狱
};
```

> 注意 `Tank.speed` 现在是 `public static final`，所有坦克共用一个速度。
> 要让难度影响速度，得先把它改成实例字段：
> ```java
> public static final int DEFAULT_SPEED = 4;   // 默认值
> private int speed = DEFAULT_SPEED;           // 每辆车自己的
> ```

### 11.2 gameover 接入点（**下一步**）

**现状**：`Tank.die()` 里 `!isEnemy` 分支是空的

```java
private void die() {
    if (!isEnemy) {
        //gameover todo
    }
    //这里不处理敌方坦克，敌人没有死的概念，只是归还到对象池
}
```

**三个接入点，按依赖顺序**：

#### ① 玩家死亡 → 切状态

```java
private void die() {
    if (!isEnemy) {
        GameFrame.gameState = STATE_OVER;
    }
}
```

`gameState` 是 `GameFrame` 的 `public static` 字段，`Tank` 已经依赖 `GameFrame`
（`mathMove()` 里读 `titleBarHeight`），直接用没问题。

#### ② 防重入（**必须做**）

`newGame()` 每次都起一个**永不退出的死循环线程**：

```java
private void newGame() {
    myTank = new MyTank(100, 100, Tank.DIR_DOWN);
    new Thread(() -> {
        while (true) {                          // ← 永远不会结束
            if (enemyTanks.size() < ENEMY_MAX_COUNT) {
                enemyTanks.add(EnemyTank.createTank());
            }
            Thread.sleep(ENEMY_BORN__INTERVAL);
        }
    }).start();                                 // ← 每次调用都起一个新的
}
```

**现在没暴露**，因为没有回菜单的路径。**做完 gameover 后**：
死 → 回菜单 → 再开始 → **两个生成线程同时跑**，敌人成倍出现，
而且并发 `add` 的风险翻倍。

**最小修法**：

```java
private boolean spawnThreadStarted = false;

private void newGame() {
    ...
    if (!spawnThreadStarted) {
        spawnThreadStarted = true;
        new Thread(...).start();
    }
}
```

**更彻底的做法**：让线程检查状态，非运行中就不生成

```java
while (true) {
    if (gameState == STATE_RUN && enemyTanks.size() < ENEMY_MAX_COUNT) {
        enemyTanks.add(EnemyTank.createTank());
    }
    Thread.sleep(ENEMY_BORN__INTERVAL);
}
```

#### ③ 重开时要重置战场

`newGame()` 现在只做 `gameState = STATE_RUN` + `new MyTank(...)`，
**没清上一局的敌人**。重开后 `enemyTanks` 里还留着上一局的残兵——
它们的 `hp`、坐标、子弹全是旧的。

```java
private void newGame() {
    // 清空上一局敌人，全部归还池子
    for (int i = enemyTanks.size() - 1; i >= 0; i--) {
        EnemyTank e = enemyTanks.get(i);
        enemyTanks.remove(i);
        EnemiesPool.theReturn(e);
    }

    gameState = STATE_RUN;
    myTank = new MyTank(100, 100, Tank.DIR_DOWN);
    ...
}
```

> 不用在这里手动 `clearBullets()`——下次 `createTank()` 取到它时会清。

#### ④ 绘制与按键

`drawOver(g)` 和 `keyEventOver(keyCode)` 都还是空的：

```java
private void drawOver(Graphics g) {
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);

    g.setColor(Color.RED);
    g.setFont(FONT);
    g.drawString("游戏结束", FRAME_WIDTH / 2 - 48, FRAME_HEIGHT / 2);

    g.setColor(Color.WHITE);
    g.drawString("按回车返回菜单", FRAME_WIDTH / 2 - 84, FRAME_HEIGHT / 2 + 50);
}

private void keyEventOver(int keyCode) {
    if (keyCode == KeyEvent.VK_ENTER) {
        gameState = STATE_MENU;
        menuIndex = 0;
    }
}
```

**`drawString` 的 x 要手算居中**：`FRAME_WIDTH/2 - 文字宽度/2`，
和 `Tank.drawName()` 用的是同一套（`FontMetrics.stringWidth`）。
上面写死的 `-48` / `-84` 是估算值，精确做法：

```java
FontMetrics fm = g.getFontMetrics();
String s = "游戏结束";
g.drawString(s, (FRAME_WIDTH - fm.stringWidth(s)) / 2, FRAME_HEIGHT / 2);
```

#### ⑤ 注意时序

`die()` 在 `collideBullets` → `hurt()` 里被调用，而 `collideBullets` 在 `drawRun()` **末尾**。

所以玩家死的**当帧**，`drawRun()` 已经执行了一半（敌我坦克都画完了），
`gameState` 变成 `STATE_OVER` 要到**下一帧** `update()` 才生效。

**30ms 的延迟，看不出问题。**

#### 顺带：死人子弹为什么不管

敌人死亡后，它的子弹会**瞬间消失**（因为 `drawEnemyTanks` 第一趟对死者跳过 `draw()`，
而 `drawBullets()` 在 `draw()` 里）。

**这是已知行为，暂不修**。原因：

```
子弹要飞 100+ 帧（约 3.4 秒）才飞出屏幕
尸体只在列表里留 450ms（等爆炸播完）
```

**尸体的存活时间比子弹飞行时间短一个数量级**。就算让尸体多留，
`createTank()` 清理弹夹时还是会收走（见 5.7）。

**真要修，只有一条路**：把子弹从坦克身上搬到 `GameFrame` 全局管理。
那要改 `fire()` / `drawBullets()` / `collideBullets()` / 回收逻辑，跨 3 个文件。
**收益远小于成本，不做。**

### 11.3 圆形碰撞（如果要更精确）

现在的 `isRectangleCollision` 判的是**方框**，但坦克画出来是**圆**。
斜角位置会出现「看着没碰到却算打中」。

要更精确就加圆-圆判定：

```java
/**
 * 两个圆是否相交
 * 用平方比较，避免开根号（慢，且有精度问题）
 */
public static boolean isHit(int x1, int y1, int r1, int x2, int y2, int r2) {
    int dx = x1 - x2;
    int dy = y1 - y2;
    int rr = r1 + r2;
    return dx * dx + dy * dy <= rr * rr;
}
```

**为什么用平方比较**：判断 `√(dx²+dy²) ≤ r1+r2` 需要开根号。
两边平方后变成 `dx² + dy² ≤ (r1+r2)²`，等价且不用开根号。

**为什么圆-圆不用 `Math.abs`**：比较的是**平方**，
`(-100)² = 100² = 10000`，负号被平方自动吃掉。
而矩形判定要跟正数比大小，所以必须 `abs`。

### 11.4 用旋转实现四方向（如果要用代码画坦克）

现在用图片，四方向四张图，不需要旋转。
但如果以后想用代码画（`drawTank()` 那套），可以用旋转只画一次：

```java
Graphics2D g2d = (Graphics2D) g.create();
g2d.rotate(Math.toRadians(getAngle()), getX(), getY());   // 绕坦克中心
// 之后一律按"朝上"画
g2d.fillOval(getX() - RADIUS, getY() - RADIUS, RADIUS << 1, RADIUS << 1);
g2d.drawLine(getX(), getY(), getX(), getY() - RADIUS - 10);
g2d.dispose();
```

角度映射（**必须用 switch，不能写 `dir * 90`**）：

| 方向 | 常量值 | 角度 |
|---|---|---|
| `DIR_UP` | 0 | 0° |
| `DIR_RIGHT` | 3 | 90° |
| `DIR_DOWN` | 1 | 180° |
| `DIR_LEFT` | 2 | 270° |

### 11.5 坦克履带动画（素材已有第二帧）

原始图集里每个方向有**两帧**（履带位置不同），目前只提取了第一帧。
要做动画，每个方向再提取 2 张，然后把 `Image[]` 改成二维：

```java
// MyTank / EnemyTank 里
private static Image[][] tankImg = new Image[4][2];

@Override
public void drawImgTank(Graphics g) {
    int frame = (tick / 4) % 2;      // 每 4 次刷新换一帧
    g.drawImage(tankImg[getDir()][frame], ...);
}
```

`tick` 是每帧自增的计数器。注意**只在移动时**切帧，静止时固定第一帧，
不然原地"踏步"很怪。

> 爆炸已经用 `index / SLOW` 这套分帧写法跑通了，履带可以直接照搬。

### 11.6 其他待扩展

| 待做 | 涉及 |
|---|---|
| **gameover** | **`Tank.die()` 玩家分支 + `drawOver` + `keyEventOver`（见 11.2）** |
| 障碍物 / 围墙 | 新增地图层，绘制在坦克之前 |
| 关卡 | 地图数据（二维数组）驱动障碍物生成 |
| 敌人种类 | `EnemyTank` 变抽象，下挂普通/快速/Boss |
| 得分 | 新增 `score` 字段，每种敌人不同分值 |
| 爆炸音效 | 素材里没有音频，需另找 |
| 暂停 | 加一个 `STATE_PAUSE`，`logic()` 不推进但继续画 |
| 菜单分发 | `keyEventMenu` 按 `menuIndex` 分支（现在 5 项都走 `newGame`） |
| 子弹全局管理 | 想修正「死人子弹消失」，得把子弹从坦克搬到 `GameFrame`（见 11.2 末） |
| 三个池泛型化 | 学完反射后统一成 `GenericPool<T>`，现在是复制三份 |
| `enemyTanks` 换线程安全容器 | 目前 `ArrayList` 有理论并发风险，等真报错再换 |

---

## 十二、附：常用 API 速查

### 绘制

| 方法 | 说明 | 受 stroke 影响 |
|---|---|---|
| `g.fillRect(x, y, w, h)` | 实心矩形 | ❌ |
| `g.drawRect(x, y, w, h)` | 空心矩形框 | ✅ |
| `g.fillOval(x, y, w, h)` | 实心椭圆（w==h 时是正圆） | ❌ |
| `g.drawOval(x, y, w, h)` | 空心椭圆框 | ✅ |
| `g.drawLine(x1, y1, x2, y2)` | 直线 | ✅ |
| `g.drawString(s, x, y)` | 文字（y 是**基线**，不是顶端） | — |
| `g.drawImage(img, x, y, w, h, o)` | 图片缩放到 w×h | — |

**统一规律**：`x, y` 都是外接矩形的**左上角**，不是中心。

### 线宽

```java
Graphics2D g2d = (Graphics2D) g.create();   // 拿副本，别污染原画笔
try {
    g2d.setStroke(new BasicStroke(线宽));
    g2d.drawLine(...);
} finally {
    g2d.dispose();                          // 还回去
}
```

> `setStroke` 只对 `drawXxx` 生效，**对 `fillXxx` 完全无效**。

### 渲染提示

```java
g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // 像素画
//  VALUE_INTERPOLATION_BILINEAR   —— 照片用
```

### 坐标系

- 原点 `(0,0)` 在**画布左上角**（标题栏**下面**）
- `x` 向右增大
- **`y` 向下增大**（和数学坐标相反！）
- 往上走是 `y--`，往下走是 `y++`

### 居中公式

```java
// 让宽 w 的矩形水平居中于 centerX
int left = centerX - w / 2;

// 让文字水平居中（文字宽度不固定，必须用 FontMetrics 量）
FontMetrics fm = g.getFontMetrics();
int textW = fm.stringWidth(text);
g.drawString(text, centerX - textW / 2, baselineY);
```

**宽度用偶数**：`50 / 2 = 25` 精确；`51 / 2 = 25`（截断），左右差 1 像素。

### 位运算小抄

| 写法 | 等价 | 优先级注意 |
|---|---|---|
| `n << 1` | `n * 2` | 低于 `+` `-`：`y - R << 1` = `(y-R)<<1` |
| `n >> 1` | `n / 2`（整数除） | 正数时和 `/2` 完全一样 |
| `n << 2` | `n * 4` | 同上 |

**优先级（高 → 低）**：`()` > `++ --` > `* / %` > `+ -` > `<< >>` > 比较 > `&& ||` > `=`

### 整数除法陷阱

```java
// 错：先除后乘，小数被吃掉
hp / DEFAULT_HP * BAR_LENGTH     // 600/1000 = 0，结果 0

// 对：先乘后除
hp * BAR_LENGTH / DEFAULT_HP     // 600*50/1000 = 30
```

**规律：整数除法前，把乘法提前。** 涉及比例计算的地方都要注意。

### 访问修饰符

| 修饰符 | 本类 | 同包 | 子类（不同包） | 任何地方 |
|---|---|---|---|---|
| `private` | ✅ | ❌ | ❌ | ❌ |
| 默认（不写） | ✅ | ✅ | ❌ | ❌ |
| `protected` | ✅ | ✅ | ✅ | ❌ |
| `public` | ✅ | ✅ | ✅ | ✅ |

> `protected` 准确说法是「本类 + 同包 + 子类」，不只是子类。

### 抽象类规则

| 规则 | 说明 |
|---|---|
| 抽象方法只能写在**抽象类**或**接口**里 | 写在普通类里编译不过 |
| 类里有抽象方法 → 类必须 `abstract` | 否则编译错误 |
| 抽象类**不能 `new`** | `new Tank(...)` ❌ |
| 抽象类**可以有构造器** | 供子类 `super(...)` 调用 |
| 子类必须实现全部抽象方法 | 否则子类自己也得 `abstract` |
| 抽象方法不能配 `private` `static` `final` `native` `synchronized` | 都跟"等子类实现"矛盾 |

### 内部类

| 类型 | 写法 | 能访问外部实例字段吗 |
|---|---|---|
| 成员内部类 | `class A {}` | ✅ 能（编译器塞 `this$0`） |
| 静态嵌套类 | `static class A {}` | ❌ 不能 |
| 局部内部类 | 方法里 `class A {}` | ✅ 能 |
| 匿名内部类 | `new Runnable() {}` | ✅ 能 |

**能加 `static` 就加**：非静态内部类持有外部引用，可能造成内存泄漏。

### 静态方法的限制

```java
public static EnemyTank createTank() {
    // ❌ 不能直接访问实例字段（没有 this）
    // x = 100;
    // aiStartTime = ...;

    // ✅ 静态字段直接用
    int y = GameFrame.titleBarHeight + RADIUS;

    // ✅ 通过对象引用访问实例字段/方法
    EnemyTank e = EnemiesPool.get();
    e.setX(100);
    e.resetAiTime();
}
```

**规则**：静态方法没有 `this`，实例成员必须通过对象引用访问。

### 按键常量

```java
KeyEvent.VK_UP / VK_DOWN / VK_LEFT / VK_RIGHT
KeyEvent.VK_W / VK_S / VK_A / VK_D
KeyEvent.VK_ENTER
KeyEvent.VK_SPACE
```

### List 常用操作

| 操作 | 写法 | 注意 |
|---|---|---|
| 按索引删 | `list.remove(0)` | 返回被删元素 |
| 按对象删 | `list.remove(bullet)` | 返回 boolean |
| 安全遍历中删 | 倒序 `for (int i = size-1; i>=0; i--)` | 正序会漏删 |
| 或 | `Iterator.remove()` | 也行，代码长 |
| 判空 | `list.isEmpty()` | 比 `size() == 0` 语义清楚 |

> `for-each` 循环里调 `list.remove()` 会抛 `ConcurrentModificationException`。

---

*文档按 `src` 目录当前代码逐行核对编写。修改代码后记得同步更新对应章节。*
