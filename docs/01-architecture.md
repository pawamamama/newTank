# 项目架构与分层

> 返回 [README](../README.md) · 对应源码：`src/com/pawamamama/`

本篇说明项目由哪些包组成、依赖朝哪个方向走、继承体系怎么搭起来，以及为什么从「一个 Tank 类」重构成「抽象父类 + 两个子类」。

## 1 项目概览

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
| 缩放算法 | 最近邻（像素画专用），缓冲区与屏幕两层都要设 |
| 坦克尺寸 | 逻辑半径 20 → 屏幕 40 × 40 |
| 地图网格 | 19 列 × 14 行，每格 40 × 40，整图 760 × 560，居中于 (70, 80) |
| 地图块类型 | `BRICK`（1 血）/ `STEEL`（3 血）/ `WATER` / `GRASS` / `HOUSE`（家） |
| 坦克生命 / 攻击 | `DEFAULT_HP = 1000` / `DEFAULT_ATK = 100` → 10 发打死 |
| 开火上限 | 每秒 8 发（`FIRE_COOLDOWN_MS = 125`） |
| 对象池 | 子弹 200/300、爆炸 10/30、敌人 10/15、地图块 200/400 |

## 2 代码分层

### 2.1 目录树

```text
newTank/
├── res/                                  图片素材（根目录 14 张 + tiles/ 8 张）
│   ├── player_up/down/left/right.png     我方坦克，四方向，16×16
│   ├── enemy_up/down/left/right.png      敌方坦克，四方向，16×16
│   ├── boom_0~4.png                      爆炸序列帧，11×11 ~ 34×31
│   ├── gameover.png                      游戏结束图，416×416
│   └── tiles/                            地图块素材，全部 40×40
│       ├── brick.png                     普通砖
│       ├── steel_hp3/hp2/hp1.png         钢墙三态：完整 / 中损 / 重损
│       ├── water.png                     水面
│       ├── grass.png                     草地（RGBA）
│       └── house.png                     老巢
│
├── tools/                                Python 素材生成脚本（运行不依赖）
├── src/com/pawamamama/
│   ├── app/GameMain.java                 程序入口
│   ├── game/
│   │   ├── GameFrame.java                主窗口、状态机、渲染、按键、碰撞调度
│   │   ├── Bullet.java                   子弹：坐标/方向/速度/可见性
│   │   └── Explode.java                  爆炸：五帧动画、缩放、分帧播放
│   ├── Tank/
│   │   ├── Tank.java                     抽象父类：移动、开火、碰撞、血量、血条
│   │   ├── MyTank.java                   玩家坦克
│   │   └── EnemyTank.java                敌人坦克 + AI + 静态工厂
│   ├── map/
│   │   ├── GameMap.java                  地图容器：网格、随机摆块、老巢、清理
│   │   ├── MapTile.java                  地图块：类型/血量/命中状态机
│   │   └── TankHouse.java                老巢：2×3 共 6 块
│   └── util/
│       ├── Constant.java                 全局常量
│       ├── MyUtil.java                   随机、碰撞、图片、爆炸工具
│       ├── BulletsPool.java              子弹池 200/300
│       ├── ExplodesPool.java             爆炸池 10/30
│       ├── EnemiesPool.java              敌人池 10/15
│       └── TilesPool.java                地图块池 200/400
└── docs/                                 本篇及同系列文档
```

> 包名 `com.pawamamama.Tank` 首字母大写，不符合 Java 全小写约定，但编译器和 IDE 都接受。写 `import` 时注意大小写。

### 2.2 包的职责

| 包 | 职责 | 内容 |
|---|---|---|
| `app` | 启动 | 只有 `GameMain`，不承载游戏逻辑 |
| `game` | 游戏运行时 | 窗口、状态机、渲染、子弹、爆炸 |
| `Tank` | 坦克模型 | 抽象父类 + 两个具体子类 |
| `map` | 地图模型 | `GameMap` 容器 + `MapTile` 地图块 + `TankHouse` 老巢 |
| `util` | 通用设施 | 常量、工具、四个对象池 |

### 2.3 依赖方向

```text
        app: GameMain
              │ new
              ▼
        game: GameFrame ── 状态机 / 渲染 / 双缓冲 / 按键 / 碰撞调度
              │
      ┌───────┴────────┬────────────────┐
      ▼                ▼                ▼
   Tank 层           map 层            game: Bullet / Explode
   Tank(抽象)        GameMap
    ├ MyTank         MapTile
    └ EnemyTank      TankHouse
      │                │
      └────────┬───────┘
               ▼
        util: Constant / MyUtil / 四个对象池
```

**关键依赖事实**：

- `Tank` 依赖 `game.GameFrame`（读 `titleBarHeight`）、`game.Bullet`、`game.Explode`。
- `Tank` 依赖 `map.MapTile`：子弹↔块、坦克↔块两套碰撞都在这里发生。
- `Tank` 依赖 `util` 的三个池：开火借子弹、命中借爆炸、碎块归还地图块池。
- `GameMap` 依赖 `Tank`（取 `RADIUS` 算格子）、`util.Constant`、`util.TilesPool`、`util.MyUtil`、`game.GameFrame`。
- `MapTile` 依赖 `Tank`（取 `RADIUS`）、`game.Bullet`、`util.MyUtil`。
- `EnemyTank` 依赖 `EnemiesPool`（自己进出池）、`BulletsPool`（归还残留子弹）。
- `util.TilesPool` 依赖 `map.MapTile`；`util.EnemiesPool` 依赖 `Tank.EnemyTank`。

**存在循环依赖**：`game.GameFrame` → `Tank` → `game.GameFrame`；`GameFrame` → `GameMap` → `GameFrame`。Java 整体编译，循环合法。根因是 `titleBarHeight` 被当成全局配置用；把它挪进 `Constant` 就能断开。

> `util → Tank` 的反向依赖只出现在 `EnemiesPool`。以后若重构包结构，应把池挪到对应模型包，或新建 `pool` 包。

### 2.4 继承体系

```text
                    ┌──────────────────────────────────┐
                    │        abstract Tank             │
                    │  常量：DIR_* / STATE_*            │
                    │        RADIUS / DEFAULT_SPEED     │
                    │        DEFAULT_HP / DEFAULT_ATK   │
                    │        FIRE_PER_SECOND            │
                    │  字段：name / x y / oldX oldY     │
                    │        hp / atk / def             │
                    │        dir / state / color        │
                    │        isEnemy / lastFireTime     │
                    │        List<Bullet> bullets       │
                    │        List<Explode> explodes     │
                    │        BloodBar bloodBar          │
                    │  方法：draw / logic / mathMove     │
                    │        fire / drawBullets         │
                    │        collideBullets             │
                    │        bulletCollideMapTiles      │
                    │        handleTileHit              │
                    │        isCollideTile / back       │
                    │        hurt / die                 │
                    │        delaySecondsToOver         │
                    │        deawExlpodes               │
                    │  抽象：drawImgTank(g)             │
                    └───────────────┬──────────────────┘
                                    │ extends
                     ┌──────────────┴──────────────┐
                     ▼                             ▼
        ┌────────────────────────┐   ┌────────────────────────────┐
        │      MyTank            │   │      EnemyTank             │
        │  static Image[] tankImg│   │  static Image[] enemyImg   │
        │  drawImgTank() → 玩家图 │   │  drawImgTank() → 敌图       │
        │                        │   │  ai() / resetAiTime()      │
        │                        │   │  static createTank()       │
        └────────────────────────┘   └────────────────────────────┘
```

一句话：「怎么画」各管各的（抽象方法），「怎么动、怎么打、怎么死」大家共用（父类实现）。

## 3 设计要点

| 设计 | 做法 | 好处 |
|---|---|---|
| 状态机 | `gameState` + `switch` | 各界面互不干扰，加新界面只加一个 case |
| 渲染循环 | 独立线程 + `repaint()` | 逻辑和绘制解耦，帧率可控 |
| 双缓冲 | `BufferedImage` + 整张 `drawImage` | 不闪屏、不撕裂 |
| 最近邻缩放 | `VALUE_INTERPOLATION_NEAREST_NEIGHBOR` | 像素画放大不发糊 |
| 抽象父类 | `abstract Tank` + `abstract drawImgTank()` | 加新坦克不改父类 |
| 多态 | 字段声明成 `Tank`，实际是子类 | 调用方不用关心具体类型 |
| 静态工厂 | `EnemyTank.createTank()` | 出生与池化重置收在敌人类自己手里 |
| 方向与状态分离 | `dir` 管朝向、`state` 管动不动 | 转向和移动互不耦合 |
| 对象池 | 四个池 | 高频对象反复复用，减少 GC |
| 扫掠碰撞 | 按速度逐像素检查本帧路径 | 高速子弹不会跳过地图块 |
| 块类型状态机 | `TileType` + `HitResult` | 命中结果由块自己决定，坦克只做表现 |
| 坦克↔块 8 点采样 | `Points8` + `back()` | 方框角也能挡住，不会半个车身进块 |
| 开火冷却 | `lastFireTime` + 125ms | 按住空格不会变成激光枪 |
| 死亡延迟删除 | `isDie() && isExplodeFinished()` | 死亡爆炸播得完 |
| 防鞭尸 | `collideBullets` 开头 `if (isDie()) return` | 尸体多留 450ms 也不会重复加爆炸 |

## 4 从单类到继承

### 4.1 重构前

```text
Tank.java（一个类）
  ├─ static Image[] tankImg      ← 玩家图
  ├─ static Image[] enemyImg     ← 敌人图
  ├─ boolean isEnemy
  └─ drawImgTank() {
         if (isEnemy) 画敌人图
         else         画玩家图
     }
```

敌人还没做时，`isEnemy` 永远是 `false`，`enemyImg` 加载了却从没用过。

### 4.2 触发原因

要加敌人了。继续用 `if` 的代价：

| 想加的东西 | 用 if 的代价 |
|---|---|
| 敌人生成器 | `if (type == GENERATOR)` 加一层 |
| Boss 坦克 | 再加一层 |
| 护盾坦克 | 再加一层 |
| 每种的速度/血量不同 | 构造器里一堆 if |

`drawImgTank` 会变成巨大的 switch，每加一种坦克都要回来改这个类。

### 4.3 重构后

```text
Tank.java（抽象父类）
  ├─ 所有共用字段和方法
  └─ abstract drawImgTank(g)

MyTank.java
  ├─ static Image[] tankImg
  └─ drawImgTank() → 画玩家图

EnemyTank.java
  ├─ static Image[] enemyImg
  ├─ drawImgTank() → 画敌人图
  └─ static createTank()
```

| 动作 | 效果 |
|---|---|
| `Tank` 加 `abstract` | 不能 `new Tank()`，编译器守住「坦克必须具体」 |
| `drawImgTank` 变抽象 | 子类不实现就编译不过 |
| 图片数组搬到各自子类 | 玩家图/敌人图各归各 |
| `FIX` 改成 `protected final` | 子类能用，但不能改 |
| `isEnemy` 保留 | 仍用于死亡分支，但绘制不再依赖它 |

### 4.4 前后对比

| 项 | 重构前 | 重构后 |
|---|---|---|
| 类数量 | 1 | 3 |
| 加新坦克 | 改 `drawImgTank` 的 if | 新建一个 `extends Tank` 的类 |
| 图片归属 | 两套图挤在一个类 | 各归各类 |
| 编译期保障 | 无 | 不实现抽象方法就报错 |
| `GameFrame` 改动 | — | 只依赖 `Tank`，不用管具体类型 |

### 4.5 对扩展的意义

`drawEnemyTanks()` 只调用父类方法 `draw(g)`，完全不知道列表里装的是 `EnemyTank`。以后塞 `BossTank`、`ShieldTank`，这段代码一个字都不用改。这就是抽象的价值：**让调用方只依赖「能做什么」，不依赖「是什么」**。

---

下一篇：[运行时与渲染](02-runtime.md) · 返回 [README](../README.md)
