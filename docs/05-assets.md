# 素材系统

> 返回 [README](../README.md) · 对应源码：`res/`、`Explode.java`、`MapTile.java`、`MyTank.java`、`EnemyTank.java`

项目不修改素材文件本身：保持原始尺寸，缩放全部交给 `drawImage`。这样像素画放大后边缘仍然锋利。

## 1 素材清单

| 文件 | 尺寸 | 使用方 |
|---|---|---|
| `player_up/down/left/right.png` | 16 × 16 | `MyTank` |
| `enemy_up/down/left/right.png` | 16 × 16 | `EnemyTank` |
| `boom_0.png` | 11 × 11 | `Explode` |
| `boom_1.png` | 15 × 14 | `Explode` |
| `boom_2.png` | 16 × 16 | `Explode` |
| `boom_3.png` | 28 × 29 | `Explode` |
| `boom_4.png` | 34 × 31 | `Explode` |
| `tiles/brick.png` | 40 × 40 | `MapTile` 的 `BRICK` |
| `tiles/steel_hp3/hp2/hp1.png` | 40 × 40 | `MapTile` 的 `STEEL`（按血量切换） |
| `tiles/water.png` | 40 × 40 | `MapTile` 的 `WATER`（已加载，地图暂未生成） |
| `tiles/grass.png` | 40 × 40 | `MapTile` 的 `GRASS`（已加载，地图暂未生成） |
| `tiles/house.png` | 40 × 40 | `MapTile` 的 `HOUSE` / `TankHouse` |
| `tiles/brick_preview.png` | 200 × 200 | 仅预览，游戏不读 |
| `gameover.png` | 416 × 416 | `GameFrame.drawOver()`，代码放大到 600×600 |

素材来源：

- 坦克与爆炸：NES 版 Battle City 图集，脚本在 `tools/extract_sprites.py`。
- 砖块、地图块、游戏结束图：本地 Python 生成，脚本在 `tools/gen_*.py`，运行时不依赖。

> `res/` 是相对工作目录的路径。启动时工作目录必须是项目根目录，否则图片加载不到。

## 2 16×16 的坦克为什么画成 40×40

```java
g.drawImage(tankImg[getDir()],
        getX() - RADIUS + FIX,
        getY() - RADIUS + FIX,
        RADIUS << 1, RADIUS << 1,
        null);
```

`drawImage` 的目标宽高传 40，由它在绘制时放大。不在文件层面放大，是因为图片软件缩放到 40×40 会做插值，像素边界被抹成半透明灰边；代码放大配合最近邻，每个像素块变成 2.5 倍的方块，边缘锋利。

代价：16 × 2.5 = 40 是非整数倍，某些像素块占 2 像素、某些占 3 像素。想完全对齐可以把 `RADIUS` 改成 24（16×3 = 48）或 16（16×2 = 32）。

地图块不走这条路：素材本身就是 40×40，和格子 1:1，直接原尺寸绘制。

## 3 FIX 偏移

```java
protected final int FIX = 1;
```

素材格子里坦克占的区域和格子几何中心不重合。放大 2.5 倍后偏差也被放大，表现为子弹从炮管侧边飞出去——因为 `fire()` 从逻辑坐标 `(x, y)` 发射，而画出来的炮管中心不在那个点。

`FIX = 1` 是实测补偿量，加上后炮管中心对齐，子弹从正中射出。`protected` 让子类能用，`final` 防止子类改成别的值。

## 4 爆炸序列帧

### 4.1 统一缩放倍数

5 张图尺寸不同，不能都拉成 40×40，否则像素块忽粗忽细，播放时会抖。做法是用统一倍数，让每帧保持原始比例长大：

```java
private static final int[] RES_W = {11, 15, 16, 28, 34};
private static final int[] RES_H = {11, 14, 16, 29, 31};
private static final int MULTIPLE_OF_SCALE = (Tank.RADIUS * 2) / 16;   // 40 / 16 = 2
```

| 帧 | 原始 | ×2 后 | 和坦克 40×40 比 |
|---|---|---|---|
| `boom_0` | 11×11 | 22×22 | 小一圈 |
| `boom_1` | 15×14 | 30×28 | 小 |
| `boom_2` | 16×16 | 32×32 | 略小 |
| `boom_3` | 28×29 | 56×58 | 大 1.4 倍 |
| `boom_4` | 34×31 | 68×62 | 大 1.7 倍 |

这条尺寸曲线决定了「受击用前 3 帧、死亡用 5 帧」：前 3 帧比坦克小，像装甲火花；后 2 帧比坦克大，像整车炸开。

### 4.2 每帧重算左上角

`drawImage` 画的是左上角，而每帧尺寸不同，所以要按各自尺寸反算：

```java
int w = RES_W[index] * MULTIPLE_OF_SCALE;
int h = RES_H[index] * MULTIPLE_OF_SCALE;
g.drawImage(boomImg[index], x - w / 2, y - h / 2, w, h, null);
```

`(x, y)` 是爆炸中心，从构造到销毁不变；变的是「从中心往外退多少」。如果只存左上角，图变大时多出来的部分全往右下挤，看起来像爆炸往右下飘。

×2 后所有尺寸都是偶数（22、30、32、56、68），`w/2` 精确。改成 2.5 倍会出现奇数，整数除法丢 0.5 像素，播放时会抖 1px。

### 4.3 SLOW 分帧

刷新间隔 30ms，如果每帧换图，5 张图 150ms 就播完，眼睛看不见。用 `SLOW` 把「播放帧」和「图片帧」分开：

```java
private static final int SLOW = 3;

public void drawBoom(Graphics g) {
    int realIndex = index / SLOW;
    // ...
    index++;
}
```

| 调用次数 | `index` | `index / 3` | 画哪张 |
|---|---|---|---|
| 1~3 | 0,1,2 | 0 | `boom_0` |
| 4~6 | 3,4,5 | 1 | `boom_1` |
| 7~9 | 6,7,8 | 2 | `boom_2` |
| 10~12 | 9,10,11 | 3 | `boom_3` |
| 13~15 | 12,13,14 | 4 | `boom_4` |

5 帧 × 3 次 × 30ms = 450ms。想调快慢只改 `SLOW`。

### 4.4 终止条件

`endIndex` 是放大后的值：受击 `3 * SLOW = 9`，死亡 `5 * SLOW = 15`。

```java
if (realIndex >= EXPLODE_FRAME_COUNT || index >= endIndex) {
    isVisible = false;
    return;
}
```

两个条件缺一不可：

- `realIndex >= 5` 防止访问 `RES_W[5]` 越界。
- `index >= endIndex` 让受击动画停在 3 帧。

## 5 素材里没有的东西

原始图集里没有子弹，所以子弹用 `fillOval` 画圆，半径 4，颜色跟随坦克。经典坦克大战的子弹本来就是一个白点，代码画完全够用。

## 6 素材与代码的对照表

| 素材 | 加载位置 | 选择依据 |
|---|---|---|
| `player_*.png` | `MyTank` 静态块 | `tankImg[dir]` |
| `enemy_*.png` | `EnemyTank` 静态块 | `enemyImg[dir]` |
| `boom_*.png` | `Explode` 静态块 | `boomImg[index / SLOW]` |
| `brick.png` | `MapTile` 静态块 | `BRICK` 或默认 |
| `steel_hp*.png` | `MapTile` 静态块 | `STEEL` 按 `hp` |
| `water.png` | `MapTile` 静态块 | `WATER` |
| `grass.png` | `MapTile` 静态块 | `GRASS` |
| `house.png` | `MapTile` 静态块 | `HOUSE` |
| `gameover.png` | `GameFrame.drawOver()` 首次调用 | 固定 |

---

下一篇：[完整链路](06-flows.md) · 返回 [README](../README.md)
