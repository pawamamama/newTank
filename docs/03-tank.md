# 坦克模型

> 返回 [README](../README.md) · 对应源码：`Tank/Tank.java`、`MyTank.java`、`EnemyTank.java`

`Tank` 是抽象父类，负责「怎么动、怎么打、怎么死」；`MyTank` 和 `EnemyTank` 只负责「怎么画」和各自的出生/AI 逻辑。

## 1 类职责

| 类 | 职责 |
|---|---|
| `Tank`（abstract） | 坐标、血量、攻击、方向、状态、移动、开火、碰撞、血条、爆炸 |
| `MyTank` | 玩家贴图，构造时传入出生坐标 |
| `EnemyTank` | 敌人贴图、AI 决策、静态工厂 `createTank()` |

## 2 Tank 抽象父类

### 2.1 常量

| 分类 | 常量 | 值 |
|---|---|---|
| 方向 | `DIR_UP / DIR_DOWN / DIR_LEFT / DIR_RIGHT` | 0 / 1 / 2 / 3 |
| 状态 | `STATE_STAND / STATE_MOVE / STATE_DIE` | 0 / 1 / 2 |
| 尺寸 | `RADIUS` | 20 |
| 速度 | `DEFAULT_SPEED` / `speed` | 4 |
| 生命 / 攻击 | `DEFAULT_HP` / `DEFAULT_ATK` | 1000 / 100 |
| 开火 | `FIRE_PER_SECOND` | 8 |
| 开火间隔 | `FIRE_COOLDOWN_MS` | `1000 / 8 = 125`（private） |

> 方向值按「上、下、左、右」排列，不是顺时针。以后若用旋转画法，不能写 `dir * 90`，必须用 `switch` 映射。

### 2.2 字段

| 字段 | 说明 |
|---|---|
| `name` | 随机名字，`initTank()` 生成 |
| `x, y` | 坦克中心点坐标 |
| `oldX, oldY` | 上一帧坐标，`mathMove()` 开头记录，`back()` 回退用 |
| `hp` | 血量，初值 1000 |
| `atk` | 攻击力，初值 100 |
| `def` | 防御力（尚未接入伤害公式） |
| `dir` | 当前朝向 |
| `state` | 当前状态 |
| `color` | 随机颜色，画子弹用 |
| `isEnemy` | 是否敌人 |
| `lastFireTime` | 上次开火时间戳，0 表示本局没开过火 |
| `bullets` | 弹夹 `List<Bullet>` |
| `explodes` | 身上正在播的爆炸 `List<Explode>` |
| `FIX` | `protected final int = 1`，贴图偏心补偿 |
| `bloodBar` | 内部类 `BloodBar` 实例 |

`color` 必须是实例字段：加 `static` 会让所有坦克共用一个颜色。

### 2.3 构造与初始化

```java
public Tank() {                    // 给对象池用的空壳
    initTank();
}

public Tank(int x, int y, int dir) {
    this.x = x;
    this.y = y;
    this.dir = dir;
    initTank();
}

private void initTank() {
    color = MyUtil.getRandomColor();
    name = MyUtil.getRandomName();
}
```

无参构造器只在池子静态块里执行一次。之后 `get()` 出来的对象不再走构造器，`color` / `name` 会沿用第一次的值。

### 2.4 每帧入口

```java
public void draw(Graphics g) {
    logic();
    drawImgTank(g);     // 抽象方法，子类实现
    drawBullets(g);
    drawName(g);
    bloodBar.draw(g);
}
```

```java
private void logic() {
    switch (state) {
        case STATE_STAND: break;
        case STATE_MOVE:  mathMove(); break;
        case STATE_DIE:   break;
    }
}
```

### 2.5 移动与回退

```java
private void mathMove() {
    oldX = x;
    oldY = y;
    switch (dir) {
        case DIR_UP:    y = Math.max(GameFrame.titleBarHeight + RADIUS, y - speed); break;
        case DIR_DOWN:  y = Math.min(FRAME_HEIGHT - RADIUS, y + speed); break;
        case DIR_LEFT:  x = Math.max(RADIUS, x - speed); break;
        case DIR_RIGHT: x = Math.min(FRAME_WIDTH - RADIUS, x + speed); break;
    }
}
```

- `Math.max/min` 把坐标夹在活动区内，坦克能贴边但不会出去。
- `oldX/oldY` 记录移动前的位置，供地图块碰撞失败时回退：

```java
public void back() {
    x = oldX;
    y = oldY;
}
```

### 2.6 开火与冷却

```java
public void fire() {
    long now = System.currentTimeMillis();
    if (now - lastFireTime < FIRE_COOLDOWN_MS) {
        return;                       // 冷却中，丢弃这次开火
    }
    lastFireTime = now;

    int bulletX = x, bulletY = y;
    switch (dir) {
        case DIR_UP:    bulletY -= RADIUS; break;
        case DIR_DOWN:  bulletY += RADIUS; break;
        case DIR_LEFT:  bulletX -= RADIUS; break;
        case DIR_RIGHT: bulletX += RADIUS; break;
    }

    Bullet bullet = BulletsPool.get();
    bullet.setAtk(atk);
    bullet.setX(bulletX);
    bullet.setY(bulletY);
    bullet.setDir(dir);
    bullet.setColor(color);
    bullet.setVisible(true);
    bullets.add(bullet);
}
```

要点：

- 子弹从炮管口出生（中心 ± `RADIUS`），不从车身中间冒出。
- 池里借出的子弹是脏对象，六个字段必须全部覆盖。
- 冷却按「每秒最多 8 发」折算成 125ms；玩家按住空格、敌人每帧掷骰子都受它限制。

### 2.7 子弹的绘制与回收

```java
private void drawBullets(Graphics g) {
    for (Bullet bullet : bullets) {           // 第一趟：画
        bullet.draw(g);
    }
    for (int i = bullets.size() - 1; i >= 0; i--) {   // 第二趟：倒序删
        Bullet bullet = bullets.get(i);
        if (!bullet.isVisible()) {
            bullets.remove(i);
            BulletsPool.theReturn(bullet);
        }
    }
}
```

分两趟是为了避免 `for-each` 边遍历边删抛 `ConcurrentModificationException`；倒序是为了 `remove(i)` 后不跳过元素。

### 2.8 子弹 ↔ 坦克

```java
public void collideBullets(List<Bullet> bullets) {
    if (isDie()) return;                     // 防鞭尸

    for (Bullet bullet : bullets) {
        if (MyUtil.isRectangleCollision(x, y, RADIUS, bullet.getX(), bullet.getY())) {
            bullet.setVisible(false);
            hurt(bullet);
            if (hp > 0) {
                explodes.add(MyUtil.addExplode(3, bullet.getX(), bullet.getY()));
            } else {
                explodes.add(MyUtil.addExplode(Explode.EXPLODE_FRAME_COUNT, x, y));
            }
        }
    }
}
```

- 受击 3 帧，贴在命中点；死亡 5 帧，居中在坦克中心。
- 伤害取子弹的 `atk` 快照，不实时读坦克：子弹飞出去后坦克升级或死亡，不应影响这一发。

### 2.9 子弹 ↔ 地图块

```java
public void bulletCollideMapTiles(List<MapTile> tiles) {
    for (MapTile tile : tiles) {
        Bullet bullet;
        if ((bullet = tile.isCollideBullet(bullets)) != null) {
            handleTileHit(tile.getHitResult(), tile, bullet);
        }
    }
}
```

`isCollideBullet()` 命中后已经在块内部完成扣血/隐藏，并返回命中的那颗子弹。坦克只根据块的 `HitResult` 做表现：

```java
private void handleTileHit(MapTile.HitResult hitResult, MapTile tile, Bullet bullet) {
    switch (hitResult) {
        case BLOCKED:                       // 块还在
            explodes.add(MyUtil.addExplode(3, bullet.getX(), bullet.getY()));
            break;
        case DESTROYED:                     // 块没了
            explodes.add(MyUtil.addExplode(Explode.EXPLODE_FRAME_COUNT,
                    tile.getX() + MapTile.RADIUS,
                    tile.getY() + MapTile.RADIUS));
            break;
        case NEVERDIE:                      // 水/草/家，子弹不消耗
            break;
        case HOUSE_DOWN:                    // 家被打中
            delaySecondsToOver(2000);
            break;
    }
}
```

| `HitResult` | 块的状态 | 表现 |
|---|---|---|
| `BLOCKED` | 还在（钢块掉血） | 命中点小爆炸 3 帧 |
| `DESTROYED` | 已隐藏 | 块中心大爆炸 5 帧 |
| `NEVERDIE` | 不参与销毁 | 不处理 |
| `HOUSE_DOWN` | 家被打中 | 2 秒后游戏结束 |

> 爆炸挂在**开火方坦克**的 `explodes` 列表里，不是地图块身上。块只负责改自己的状态。

### 2.10 坦克 ↔ 地图块

```java
public boolean isCollideTile(List<MapTile> tiles) {
    for (MapTile tile : tiles) {
        if (tile.isCollideTank(x, y, RADIUS)) {
            return true;
        }
    }
    return false;
}
```

`MapTile.isCollideTank()` 用块的 8 个采样点（4 角 + 4 边中点）判断是否落进坦克方框。`GameFrame.bulletCollideMapTile()` 每帧对玩家和每个敌人调用一次，撞上就 `back()`：

```java
if (myTank.isCollideTile(tiles)) {
    myTank.back();
}
```

> 水、草目前也会挡住坦克，`isCollideTank()` 不区分块类型。通行规则是后续待做项。

### 2.11 扣血、死亡与延迟结束

```java
private void hurt(Bullet bullet) {
    hp -= bullet.getAtk();
    if (hp <= 0) {
        die();
    }
}

private void die() {
    if (!isEnemy) {
        delaySecondsToOver(2000);       // 玩家死亡：2 秒后结束
    }
    // 敌人不在这里归还，交给 GameFrame.drawEnemyTanks()
}
```

```java
private void delaySecondsToOver(int milliseconds) {
    new Thread(() -> {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        GameFrame.setGameState(STATE_OVER);
    }).start();
}
```

两个触发点：玩家血量归零、家块被 `HOUSE_DOWN`。每次调用都会新开线程，没有「只结束一次」的判重。

### 2.12 爆炸与血条

```java
public void deawExlpodes(Graphics g) {
    for (Explode explode : explodes) {
        explode.drawBoom(g);
    }
    for (int i = explodes.size() - 1; i >= 0; i--) {
        Explode explode = explodes.get(i);
        if (!explode.isVisible()) {
            explodes.remove(i);
            ExplodesPool.theReturn(explode);
        }
    }
}
```

> 方法名 `deawExlpodes` 是 `drawExplodes` 的笔误，能跑；改名要同步 `GameFrame`。

`BloodBar` 三层叠加：

| 层 | 作用 |
|---|---|
| 黄底满条 | 显示血量上限 |
| 红条 | 按 `hp * BAR_LENGTH / DEFAULT_HP` 缩短 |
| 白框 | 描边，避免和背景混在一起 |

`hp * BAR_LENGTH / DEFAULT_HP` 必须先乘后除：整数除法下 `hp / DEFAULT_HP` 会先变成 0，血条永远空。

血条宽 50、坦克宽 40，所以 `barX = x - BAR_LENGTH / 2` 居中，左右各超出 5px。

## 3 MyTank

```java
public class MyTank extends Tank {
    private static Image[] tankImg;      // 玩家图只在这个类里

    public MyTank(int x, int y, int dir) {
        super(x, y, dir);
    }

    @Override
    public void drawImgTank(Graphics g) {
        g.drawImage(tankImg[getDir()],
                getX() - RADIUS + FIX,
                getY() - RADIUS + FIX,
                RADIUS << 1, RADIUS << 1, null);
    }
}
```

- 图片数组 `static`：每个类只读一次盘，所有玩家坦克共用。
- `tankImg[getDir()]` 依赖「方向常量值 == 数组下标」的约定。
- `dir` 在父类是 private，子类只能通过 `getDir()` 访问。

## 4 EnemyTank

### 4.1 AI

```java
private void ai() {
    if (System.currentTimeMillis() - aiStartTime > Constant.ENEMY_AI_INTERVAL) {
        setState(MyUtil.getRandomNumber(STATE_STAND, STATE_MOVE + 1));
        aiStartTime = System.currentTimeMillis();
        if (getState() == STATE_MOVE) {
            setDir(MyUtil.getRandomNumber(0, 4));
        }
    }
    if (Math.random() < Constant.ENEMY_FIRE_PERCENT) {
        fire();
    }
}
```

`ai()` 在 `drawImgTank()` 里每帧调用一次：

- 每 3 秒决策一次状态和方向，避免每帧随机导致原地抽搐。
- 每帧以 3% 概率尝试开火，但 `fire()` 内部有 125ms 冷却，实际射速不会超过每秒 8 发。
- 每帧掷骰子的期望约每 1 秒开一次火（33 帧 × 3% ≈ 1），冷却只封顶，不会让它更快。

### 4.2 静态工厂 createTank

```java
public static EnemyTank createTank() {
    int x = MyUtil.getRandomNumber(0, 2) == 0 ? RADIUS : FRAME_WIDTH - RADIUS;
    int y = GameFrame.titleBarHeight + RADIUS;
    EnemyTank enemy = EnemiesPool.get();

    enemy.resetAiTime();
    enemy.setX(x);
    enemy.setY(y);
    enemy.setDir(DIR_DOWN);
    enemy.setEnemy(true);
    enemy.setState(STATE_MOVE);
    enemy.setHp(Tank.DEFAULT_HP);

    for (Bullet b : enemy.getBullets()) {
        BulletsPool.theReturn(b);
    }
    enemy.getBullets().clear();

    return enemy;
}
```

- 出生在左上角或右上角，方向朝下。
- 归还残留子弹再清空弹夹：敌人死亡时可能有子弹没飞完，不清会污染下一个使用者。
- `resetAiTime()` 必须调：池对象的 `aiStartTime` 可能停留在很久以前，新敌人第一帧就会乱决策。
- 目前未重置 `lastFireTime`、`color`、`name`，见 [进度与待修](07-progress.md)。

## 5 池化注意

| 对象 | 池化后不会执行构造器 | 需要手动重置的字段 |
|---|---|---|
| `EnemyTank` | 是 | `x/y/dir/state/hp/isEnemy/bullets/aiStartTime`（`createTank()`） |
| `Bullet` | 是 | `atk/x/y/dir/color/visible`（`fire()`） |
| `Explode` | 是 | `visible/index/x/y/endIndex`（`MyUtil.addExplode()`） |
| `MapTile` | 是 | `hp/type/hitResult/isVisible`（`reset()`） |

漏掉任何一个字段，都会以「上一世的数据」出现在新对象上，这类 bug 往往只在连续玩几局后才出现。

---

下一篇：[地图与地图块](04-map.md) · 返回 [README](../README.md)
