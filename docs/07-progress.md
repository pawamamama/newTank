# 进度与待修

> 返回 [README](../README.md) · 对齐提交：`fdca795`（2026-11-23）

这一篇记录「已经能跑什么」「还欠什么」「踩过的坑」和「下一步可以做什么」。判断标准以当前源码为准。

## 1 已完成

| 模块 | 状态 |
|---|---|
| 窗口 + 标题栏高度 | 已完成 |
| 菜单显示与翻页 | 已完成 |
| 状态机 5 态 | 已完成 |
| 双缓冲 + 最近邻缩放 | 已完成 |
| 窗口放大 1.5 倍（900×700 → 1350×1050） | 已完成 |
| 玩家移动（四方向 + 边界夹紧） | 已完成 |
| 图片绘制坦克 | 已完成 |
| 开火 + 子弹飞行 | 已完成 |
| 开火冷却（每秒最多 8 发） | 已完成 |
| 子弹对象池 | 已完成 |
| 抽象 Tank 类 + 多态绘制 | 已完成 |
| 玩家/敌人拆分 | 已完成 |
| 敌人定时生成 + 上限控制 | 已完成 |
| 敌人 AI（定时换向 + 概率开火） | 已完成 |
| 子弹 ↔ 坦克碰撞（方框判定） | 已完成 |
| 伤害系统（`DEFAULT_ATK` / `Bullet.atk` / `hurt()`） | 已完成 |
| 爆炸五帧动画 + 对象池 | 已完成 |
| 血条 / 随机名字 | 已完成 |
| 敌人对象池 + 死亡完整处理 | 已完成 |
| 玩家死亡 → 延迟 2 秒 → 游戏结束 | 已完成 |
| 家块被打中 → 延迟 2 秒 → 游戏结束 | 已完成 |
| 重开（子弹归还、回菜单） | 已完成 |
| 地图层（19×14 居中） | 已完成 |
| 地图块对象池 | 已完成 |
| 地图块类型（砖/钢/水/草/家） | 已完成（贴图与状态机就绪） |
| 按类型绘制 + 钢墙三态 | 已完成 |
| 子弹 ↔ 地图块（扫掠 + 按类型扣血） | 已完成 |
| 炸弹式大/小爆炸区分 | 已完成 |
| 坦克 ↔ 地图块阻挡（8 点采样 + 回退） | 已完成 |
| 老巢 `TankHouse`（2×3） | 已完成 |

## 2 待修与未完成

### 2.1 功能未完成

| 位置 | 内容 |
|---|---|
| 菜单分发 | `keyEventMenu` 里 `ENTER` 无条件 `newGame()`，5 个菜单项没有按 `menuIndex` 分支 |
| `drawHelp` / `drawAbout` | 空方法，且没有代码把状态切到 `STATE_HELP` / `STATE_ABOUT` |
| `keyEventHelp` / `keyEventAbout` | 空方法 |
| 关卡 | 没有关卡数据、没有胜利条件；地图是随机 19 块，不是手工布局 |
| 地图块类型生成 | `initMap()` 只生成 `BRICK` + 老巢，钢/水/草贴图加载了但不会出现 |
| 水/草规则 | `takeHit()` 没有 `WATER` / `GRASS` 分支；`isCollideTank()` 也不区分类型 |

### 2.2 正确性隐患

| 位置 | 问题 | 建议 |
|---|---|---|
| `GameMap.initMap()` | 19 块随机砖不去重，可能互相重叠，也可能压住老巢 | 改手工关卡数组或带避让的生成 |
| `GameMap.clearDestoryTile()` | 会移除老巢的块，玩家家被打碎后消失；老巢块本来不走池，却会被还进 `TilesPool` | 让 `TankHouse` 也走池，或归还前判断来源 |
| `TankHouse` | 直接用 `new MapTile()`，绕过对象池 | 改为 `TilesPool.get()` |
| `Tank.delaySecondsToOver()` | 每次调用新开线程，没有「只结束一次」的判重 | 加 `gameOvering` 标记或保存线程引用 |
| `GameFrame.restGame()` | 只归还子弹；存活的敌人和地图块没有还回各自池 | 遍历 `EnemiesPool` / `TilesPool` 归还，还敌人前先清空 `bullets` |
| 敌人生成线程 | 旧线程 `sleep(5000)` 后才检查 `gameState`；5 秒内重开会并存两条生成线程 | 保存线程引用，`newGame()` 前 `interrupt()` |
| `enemyTanks` 并发 | 生成线程 `add`，EDT 遍历/`remove`，`ArrayList` 非线程安全 | 换 `CopyOnWriteArrayList`，或把生成收进主循环 |
| `EnemyTank.createTank()` | 未重置 `lastFireTime`、`color`、`name` | 补三个字段的重置 |
| `EnemyTank.ai()` | 每帧 3% 概率掷骰子，期望约每秒 1 发；冷却只是上限 | 若要更可控，改成显式开火计时器 |
| `Bullet` 池 | `get()` / `theReturn()` 不重置，完全依赖 `fire()` 覆盖 | 可接受，但建议统一重置风格 |

### 2.3 代码整洁

| 位置 | 内容 |
|---|---|
| `MapTile.takeHit()` | 留有 `System.out.println("进入test")` |
| `Tank.hurt()` | 留有 `System.out.println("atk: ...")` |
| `BulletsPool` / `EnemiesPool` / `ExplodesPool` | `get()` / `theReturn()` 打印池大小，上线要删 |
| `Tank.bullets` / `explodes` | `new ArrayList()` 用了裸类型 |
| `EnemyTank.createTank()` | 局部变量 `int dir = DIR_DOWN;` 声明后未使用 |
| `GameMap` | 字段名 `MAP_WINTH` 拼写错误 |
| `MapTile.tileW` | 仅用于 `TankHouse` 布局和 `Points8`，命名可更明确 |
| `Tank.deawExlpodes` | 方法名 `drawExplodes` 的笔误 |
| `Tank.drawTank()` | 私有且未被使用的遗留实现 |
| `Tank.java` 149-167 | 整段被注释掉的旧绘制代码 |

## 3 已踩过并修复的坑

这些都已经改对，记下来是因为同类问题还会再遇到。

### 3.1 遍历中删除必须倒序

```java
// 正序：remove(i) 后，i+1 的元素挪到 i，i++ 会跳过它
for (int i = 0; i < list.size(); i++) { ... list.remove(i); ... }

// 倒序：被删元素后面的都已经遍历过了
for (int i = list.size() - 1; i >= 0; i--) { ... list.remove(i); ... }
```

`deawExlpodes` 曾写成 `for (int i = explodes.size(); i > 0; i--)`，第一次就 `get(size())` 越界。

### 3.2 池化对象取出后必须重置全部字段

| 对象 | 漏掉的字段 | 症状 |
|---|---|---|
| `Bullet` | `visible` | 第二发子弹「发射即消失」 |
| `Explode` | `index` | 第二轮爆炸从上一轮位置接着播 |
| `EnemyTank` | `aiStartTime` | 新敌人一出生就乱决策、乱换方向 |

`aiStartTime` 最难发现：它只在构造器里赋值，池化后构造器不再执行。症状不是崩溃，是「行为怪异」。

### 3.3 setter 不要重复构造器做过的换算

`Explode` 构造器里乘 `SLOW`，`setEndIndex` 又没乘，导致 `setEndIndex(3)` 只播了第一张图。现在统一由调用方乘：`setEndIndex(3 * SLOW)`。

关键不是「谁来乘」，而是**只有一处乘**。

### 3.4 标记不可见后必须 return

```java
if (realIndex >= EXPLODE_FRAME_COUNT || index >= endIndex) {
    isVisible = false;
    return;         // 少了这句，下一行会访问 RES_W[5]
}
```

受击停在 `realIndex = 3` 合法，死亡停在 5 越界，所以表现为「3 帧不崩、5 帧崩」。

### 3.5 池化对象持有其他池对象时，归还前要清空

`EnemyTank` 持有 `bullets` 和 `explodes`。敌人死亡时子弹可能还在飞，尸体期间不调 `draw()`，子弹冻结。归还池后，下一个敌人会带着旧子弹出生。

只 `clear()` 不归还 = 对象失去引用，池子永久少几个。

### 3.6 归还只能有一处

`die()` 和 `drawEnemyTanks()` 同时归还会导致：对象已进池、却还在 `enemyTanks` 列表里；下一个 `get()` 改它的坐标，「尸体」突然跳到出生点。

「从列表删除」和「归还池子」必须放在同一个循环里。

### 3.7 死亡延迟删除不能只看「死了」

`if (isDie())` 就删，会导致 `drawExplodes()` 遍历不到尸体，死亡爆炸永远画不出来。必须加 `&& isExplodeFinished()`。

加了之后还必须配防鞭尸（`collideBullets` 开头 `if (isDie()) return`），否则尸体在 450ms 里继续挨打、继续加爆炸，`explodes` 永远不空。

### 3.8 高速物体不能只判定终点

子弹每帧 8 像素，只查终点会「这一帧在块前、下一帧在块后」。现在地图块用 `sweptCollision()` 逐像素检查本帧路径。

## 4 后续方向

### 4.1 敌人 AI 加强

当前 `EnemyTank.ai()` 已经能定时换向和概率开火。可扩展的方向：

- 朝玩家方向移动的概率，而不是纯随机。
- 每种敌人不同速度/血量/开火概率，`EnemyTank` 变抽象中间层，下挂 `NormalEnemy` / `FastEnemy` / `BossEnemy`。
- 难度档位用参数表控制，而不是改 AI 逻辑。

### 4.2 手工关卡

把 `GameMap.initMap()` 的随机 19 块换成二维数组或文本地图：

```java
int[][] LEVEL_1 = {
    {1, 1, 1, 0, 0, 0, 1, 1, 1},
    // 0 空、1 砖、2 钢、3 水、4 草、5 家
};
```

这样可以同时解决「块重叠」「压住老巢」「钢/水/草不生成」三个问题。

### 4.3 圆形碰撞（更精确）

当前 `isRectangleCollision` 判的是方框，坦克画出来是圆，斜角会「看着没碰到却算打中」。

```java
public static boolean isHit(int x1, int y1, int r1, int x2, int y2, int r2) {
    int dx = x1 - x2;
    int dy = y1 - y2;
    int rr = r1 + r2;
    return dx * dx + dy * dy <= rr * rr;
}
```

用平方比较避免开根号。圆-圆不需要 `Math.abs`，因为负号会被平方吃掉。

### 4.4 旋转绘制四方向（仅当改用代码画坦克）

现在四方向靠四张图。若改成代码画，可以用 `Graphics2D.rotate()` 只画一次「朝上」的坦克：

| 方向 | 常量值 | 角度 |
|---|---|---|
| `DIR_UP` | 0 | 0° |
| `DIR_RIGHT` | 3 | 90° |
| `DIR_DOWN` | 1 | 180° |
| `DIR_LEFT` | 2 | 270° |

方向常量值不是顺时针，**必须用 switch 映射，不能写 `dir * 90`**。

### 4.5 坦克履带动画

原图集每个方向有两帧，目前只提取了第一帧。做法：

```java
private static Image[][] tankImg = new Image[4][2];

int frame = (tick / 4) % 2;      // 每 4 次刷新换一帧
g.drawImage(tankImg[getDir()][frame], ...);
```

只在移动时切帧，静止时固定第一帧。

### 4.6 其他待扩展

| 待做 | 涉及 |
|---|---|
| 地图边界 / 出生点保护 | 玩家和敌人出生位置留空，避免被砖块堵死 |
| 得分 | 新增 `score` 字段，每种敌人不同分值 |
| 爆炸音效 | 素材里没有音频，需另找 |
| 暂停 | 加 `STATE_PAUSE`，`logic()` 不推进但继续画 |
| 菜单分发 | `keyEventMenu` 按 `menuIndex` 分支 |

---

下一篇：[API 速查](08-api.md) · 返回 [README](../README.md)
