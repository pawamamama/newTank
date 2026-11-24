# 地图与地图块

> 返回 [README](../README.md) · 对应源码：`map/GameMap.java`、`map/MapTile.java`、`map/TankHouse.java`、`util/TilesPool.java`

地图把画布划成 19×14 个 40×40 的格子。`MapTile` 不只是「砖块」：它带着类型、血量和命中状态机，砖、钢、水、草、家都由同一个类表达。

## 1 网格与坐标

| 项 | 计算 | 结果 |
|---|---|---|
| 列 × 行 | `19 × 14` | 266 格 |
| 整图宽 | `19 × 40` | 760 |
| 整图高 | `14 × 40` | 560 |
| 水平偏移 `MAP_X` | `(900 - 760) / 2` | 70 |
| 垂直偏移 `MAP_Y` | `(titleBarHeight + 700 - 560) / 2` | 80 |
| 右边界 / 下边界 | `70 + 760` / `80 + 560` | 830 / 640 |

`MapTile` 存**左上角**坐标：`col * 40`、`row * 40` 直接就是绘制和碰撞的起点。

> 字段名 `MAP_WINTH` 少了一个 `D`，不影响运行，但容易拼错。

## 2 GameMap

### 2.1 字段与构造

```java
private final int MAP_WINTH  = 19 * Tank.RADIUS * 2;   // 760
private final int MAP_HEIGHT = 14 * Tank.RADIUS * 2;   // 560
private final int MAP_X = (Constant.FRAME_WIDTH - MAP_WINTH) / 2;
private final int MAP_Y =
        (GameFrame.titleBarHeight + Constant.FRAME_HEIGHT - MAP_HEIGHT) / 2;

private List<MapTile> tiles = new ArrayList<>();
private TankHouse tankHouse;

public GameMap() {
    initMap();
}
```

`MAP_Y` 加 `titleBarHeight` 是为了在「标题栏下方到画布底部」这段里居中，和坦克活动区对齐。

### 2.2 initMap 生成地图

```java
private void initMap() {
    final int COUNT = 19;

    for (int i = 0; i < COUNT; i++) {
        MapTile mapTile = TilesPool.get();
        mapTile.setType(MapTile.TileType.BRICK);
        int col = MyUtil.getRandomNumber(0, 19);   // 0~18
        int row = MyUtil.getRandomNumber(0, 14);   // 0~13
        mapTile.setX(MAP_X + col * 40);
        mapTile.setY(MAP_Y + row * 40);
        tiles.add(mapTile);
    }

    tankHouse = new TankHouse();
    addTiles(tankHouse.getMap());
}
```

- 19 块普通砖从池里借，`setType(BRICK)` 同时设好血量和命中状态。
- 老巢由 `TankHouse` 生成后整体追加进 `tiles`。
- 随机块之间、随机块与老巢之间**没有去重和避让**，可能重叠，也可能压住老巢。

### 2.3 绘制与清理

```java
public void draw(Graphics g) {
    for (MapTile tile : tiles) {
        tile.draw(g);
    }
}

public void clearDestoryTile() {
    for (int i = getTiles().size() - 1; i >= 0; i--) {
        MapTile tile = getTiles().get(i);
        if (!tile.isVisible()) {
            tiles.remove(i);
            TilesPool.theReturn(tile);
        }
    }
}
```

- 倒序删：`remove(i)` 后元素前移，正序会漏。
- 归还只在 `clearDestoryTile()` 一处，避免同一块进池两次。
- `clearDestoryTile()` 由 `GameFrame.drawExplodes()` 调用，所以块消失比命中晚一帧。
- 老巢的 6 块不是从池里借的，但被打碎后也会走这里归还进 `TilesPool`，属于已知问题。

## 3 MapTile

### 3.1 TileType

```java
public enum TileType {
    BRICK(1),   // 普通砖，一血
    STEEL(3),   // 钢块，三血
    WATER(0),   // 水，不参与血量
    GRASS(0),   // 草，不参与血量
    HOUSE(1);   // 家，一血

    public final int maxHp;

    TileType(int maxHp) {
        this.maxHp = maxHp;
    }
}
```

`maxHp` 有两个用途：`setType()` 设初始血量；静态块按 `TileType.STEEL.maxHp` 决定要加载几张钢墙图。

### 3.2 HitResult

```java
public enum HitResult {
    BLOCKED,     // 块还在
    DESTROYED,   // 块没了
    NEVERDIE,    // 块不参与销毁
    HOUSE_DOWN   // 家被打中
}
```

命中结果由块自己决定，坦克只根据结果播放爆炸或触发结束。这样加新块类型时，坦克的 `handleTileHit` 不用知道具体规则。

### 3.3 贴图与 imageOf

```java
private static Image brickImg;
private static Image[] steelImgs;   // 长度 = TileType.STEEL.maxHp = 3
private static Image waterImg, grassImg, houseImg;

private Image imageOf() {
    switch (type) {
        case STEEL: return steelImgs[hp - 1];   // 1~3 对应重损/中损/完整
        case WATER: return waterImg;
        case GRASS: return grassImg;
        case HOUSE: return houseImg;
        case BRICK:
        default:    return brickImg;
    }
}
```

所有素材都是 40×40，和格子 1:1，`draw()` 不传缩放参数。

### 3.4 setType 与 reset

```java
public void setType(TileType type) {
    this.type = type;
    this.hp = type.maxHp;
    this.hitResult = (hp > 0) ? HitResult.BLOCKED : HitResult.NEVERDIE;
}

public void reset() {
    hp = 0;
    type = TileType.BRICK;
    hitResult = HitResult.BLOCKED;
    isVisible = true;
}
```

- `setType()` 负责「这块是什么」，不碰 `isVisible`。
- `reset()` 负责池化对象回到干净状态，**不清坐标**：坐标由 `GameMap` 每次重新设置。
- 水/草的 `hp = 0`，所以初始命中结果直接是 `NEVERDIE`。

### 3.5 子弹判定与命中状态机

```java
public Bullet isCollideBullet(List<Bullet> bullets) {
    if (!isVisible) return null;

    for (Bullet bullet : bullets) {
        if (!bullet.isVisible()) continue;
        if (MyUtil.sweptCollision(x + RADIUS, y + RADIUS, RADIUS,
                bullet.getX(), bullet.getY(),
                bullet.getDir(), bullet.getSpeed())) {
            takeHit(bullet);
            return bullet;
        }
    }
    return null;
}
```

返回值是**命中的那颗子弹**，没命中返回 `null`。坦克用这个返回值决定小爆炸的位置。

```java
public HitResult takeHit(Bullet bullet) {
    switch (type) {
        case BRICK:
            bullet.setVisible(false);
            setVisible(false);
            setHitResult(HitResult.DESTROYED);
            break;
        case STEEL:
            if (hp > 1) {
                bullet.setVisible(false);
                hp--;
            } else {
                bullet.setVisible(false);
                setVisible(false);
                setHitResult(HitResult.DESTROYED);
            }
            break;
        case HOUSE:
            bullet.setVisible(false);
            setVisible(false);
            setHitResult(HitResult.HOUSE_DOWN);
            break;
    }
    return getHitResult();
}
```

| 类型 | 命中后 |
|---|---|
| `BRICK` | 子弹消失、块隐藏、`DESTROYED` |
| `STEEL` | 子弹消失、扣 1 血；血尽才隐藏并 `DESTROYED` |
| `HOUSE` | 子弹消失、块隐藏、`HOUSE_DOWN` |
| `WATER` / `GRASS` | **没有 case**，状态不变，子弹也不消失 |

> 水/草没有分支，是因为 `initMap()` 目前只生成砖和老巢。以后真正生成水/草时，这里必须补规则，否则会出现「子弹打在水上不消失也不阻挡」。

### 3.6 draw

```java
public void draw(Graphics g) {
    if (!isVisible) return;
    g.drawImage(imageOf(), x, y, null);
}
```

`isVisible` 既控制「画不画」，也作为「已被摧毁」的标记。`clearDestoryTile()` 据此移除并归还。

### 3.7 坦克判定与 Points8

```java
public boolean isCollideTank(int tankX, int tankY, int tankRadius) {
    Points8 points = new Points8();
    for (int i = 0; i < points.xs.length; i++) {
        if (MyUtil.isRectangleCollision(tankX, tankY, tankRadius,
                points.xs[i], points.ys[i])) {
            return true;
        }
    }
    return false;
}
```

`Points8` 是一个内部类，取块的 8 个点：

```text
左上 ── 上中 ── 右上
 │                │
左中            右中
 │                │
左下 ── 下中 ── 右下
```

每个点都用「点是否落在坦克方框内」判断。只查块的中心点不够：坦克从角上擦过时中心还在块外，车身已经进去了。8 个点覆盖四角和四边中点，能挡住绝大多数切入。

`Points8` 每次调用都 `new` 一个，因为块的坐标可能刚被 `setX/setY` 改过，缓存会过期。

## 4 TankHouse 老巢

```java
public class TankHouse {
    public static final int HOUSE_X = 390;
    public static final int HOUSE_Y = 620;

    public TankHouse() {
        final int ROWS = 2;
        final int COLS = 3;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                MapTile tile = new MapTile();
                tile.setX(HOUSE_X + col * MapTile.tileW);
                tile.setY(HOUSE_Y + row * MapTile.tileW);
                if (row == 0 && col == 1) {
                    tile.setType(MapTile.TileType.HOUSE);
                } else {
                    tile.setType(MapTile.TileType.BRICK);
                }
                tiles.add(tile);
            }
        }
    }
}
```

布局是 2 行 × 3 列，上排中间那格是 `HOUSE`，其余是普通砖：

```text
┌────┬────┬────┐
│砖  │家  │砖  │   row 0
├────┼────┼────┤
│砖  │砖  │砖  │   row 1
└────┴────┴────┘
```

- 老巢**不走对象池**，直接 `new MapTile()`。
- 没有自己的 `draw()`，块并入 `GameMap.tiles` 后由地图统一绘制。
- 碰撞与普通块完全一致：坦克会被挡住，子弹会扣血，`HOUSE` 命中返回 `HOUSE_DOWN`。
- `HOUSE_Y = 620`，两行占到 700；地图纵向只到 640，所以下排落在下方通道区。

## 5 TilesPool

```java
public static final int DEFAULT_POOL_SIZE = 200;
public static final int MAX_POOL_SIZE     = 400;

public static MapTile get() {
    return pool.isEmpty() ? new MapTile() : pool.remove(0);
}

public static void theReturn(MapTile tile) {
    if (pool.size() == MAX_POOL_SIZE) {
        return;
    }
    tile.reset();          // ★ 归还时重置
    pool.add(tile);
}
```

容量按整屏铺满估：`900/40 ≈ 22` 列 × `700/40 ≈ 17` 行 ≈ 374 块，取整到 400。

> 重置放在 `theReturn()` 而不是 `get()`：`get()` 只负责交付，归还方最清楚对象该怎么回到干净状态。代价是如果有人在对象仍在使用时误归还，它的状态会被提前重置——所以「归还只能在一处」这条规则必须守住。

## 6 地图系统的已知问题

| 问题 | 影响 | 建议 |
|---|---|---|
| `initMap()` 随机块不去重 | 块可能重叠，也可能压住老巢 | 改手工关卡数组或带约束生成 |
| 只生成 `BRICK` 和老巢 | 钢/水/草贴图已加载但永远看不到 | 在关卡数据里加入类型 |
| 水/草没有 `takeHit` 分支 | 真正生成后子弹打上去不消失 | 补 `NEVERDIE` 的消耗规则 |
| 水/草也会挡坦克 | 与「草可穿过、水不可穿过」的常见规则不符 | 让 `isCollideTank` 按类型判断 |
| `TankHouse` 绕过对象池 | 老巢块无法被池复用 | 改为 `TilesPool.get()` |
| 老巢块被打碎会还进 `TilesPool` | 池里混入非池来源对象，本身无害但语义混乱 | 归还前判断来源，或让老巢也走池 |
| `MAP_WINTH` 拼写错误 | 不影响运行 | 改名 `MAP_WIDTH` |

---

下一篇：[素材系统](05-assets.md) · 返回 [README](../README.md)
