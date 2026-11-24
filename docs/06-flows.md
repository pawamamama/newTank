# 从按键到屏幕的完整链路

> 返回 [README](../README.md) · 对应源码：`GameFrame.java`、`Tank.java`、`MapTile.java`、`Explode.java`

这一篇按时间顺序串起六条主链路。每条都从「用户动作或帧循环」开始，到「屏幕变化或对象归还」结束。

## 1 按方向键移动

```text
① 用户按 ↑
      │
      ▼
② EDT → keyPressed → keyEventRun(VK_UP)
      ├─ myTank.setDir(DIR_UP)
      └─ myTank.setState(STATE_MOVE)
      │
      ▼
③ 30ms 后 repaint() → update() → drawRun()
      │
      ▼
④ myTank.draw(g) → Tank.draw(g)
      ├─ logic() → mathMove()
      │     ├─ oldX/oldY = x/y
      │     └─ y = max(titleBarHeight + RADIUS, y - 4)
      ├─ drawImgTank(g) → MyTank 的实现
      ├─ drawBullets / drawName / bloodBar
      │
      ▼
⑤ 整张缓冲区放大 1.5 倍贴屏
```

松开方向键后，`keyReleasedEventRun` 把状态改回 `STATE_STAND`，`logic()` 不再移动。

## 2 按空格开火

```text
① 用户按空格
      │
      ▼
② keyEventRun(VK_SPACE) → myTank.fire()
      │
      ▼
③ Tank.fire()
      ├─ now - lastFireTime < 125ms ? → 直接 return（冷却中）
      ├─ lastFireTime = now
      ├─ 按 dir 算出炮管口坐标
      ├─ BulletsPool.get()
      ├─ setAtk / setX / setY / setDir / setColor / setVisible(true)
      └─ bullets.add(bullet)
      │
      ▼
④ 下一帧 drawBullets()
      ├─ bullet.draw() → logic() → move()，每帧 8 像素
      └─ 出界后 visible = false
      │
      ▼
⑤ 再下一帧 drawBullets() 第二趟
      └─ 倒序 remove + BulletsPool.theReturn()
```

要点：

- 冷却检查发生在**借子弹之前**，冷却中的按键不会产生任何对象。
- `visible = false` 在子弹自己的 `move()` 里，回收在下一帧，所以子弹会多活一帧。
- 玩家按住空格会不断触发 `keyPressed`，但 `fire()` 把实际射速封在每秒 8 发。

## 3 子弹打中坦克

```text
① drawRun → bulletCollideTank()
      │
      ▼
② 遍历每辆敌人
      ├─ myTank.collideBullets(enemy.getBullets())
      └─ enemy.collideBullets(myTank.getBullets())
      │
      ▼
③ Tank.collideBullets()
      ├─ if (isDie()) return          ← 防鞭尸
      ├─ isRectangleCollision(x, y, RADIUS, bullet.x, bullet.y)
      ├─ bullet.setVisible(false)
      ├─ hurt(bullet) → hp -= bullet.atk → hp <= 0 ? die()
      └─ 按扣血后的 hp 加爆炸：
           hp > 0  → 3 帧，贴命中点
           hp <= 0 → 5 帧，居中坦克
      │
      ▼
④ 同一帧稍后 drawExplodes()
      └─ 各坦克 deawExlpodes(g) → drawBoom(g)
      │
      ▼
⑤ 爆炸播完（受击 9 次刷新 / 死亡 15 次刷新）
      └─ 下一帧 deawExlpodes() 回收并还 ExplodesPool
```

## 4 子弹打中地图块

```text
① drawRun → bulletCollideMapTile()
      │
      ▼
② myTank.bulletCollideMapTiles(tiles)
   每个敌人也调一次
      │
      ▼
③ Tank.bulletCollideMapTiles(tiles)
      └─ 遍历块，对每块调 tile.isCollideBullet(bullets)
             │
             ▼
④ MapTile.isCollideBullet(bullets)
      ├─ 跳过不可见块和不可见子弹
      ├─ sweptCollision(块中心, 半径, 子弹坐标, 方向, 速度)
      ├─ 命中 → takeHit(bullet)
      │     ├─ BRICK：子弹隐藏、块隐藏、DESTROYED
      │     ├─ STEEL：子弹隐藏、hp--；血尽才隐藏并 DESTROYED
      │     ├─ HOUSE：子弹隐藏、块隐藏、HOUSE_DOWN
      │     └─ WATER/GRASS：无分支
      └─ 返回命中的那颗 Bullet（没命中返回 null）
      │
      ▼
⑤ Tank.handleTileHit(hitResult, tile, bullet)
      ├─ BLOCKED  → 命中点小爆炸 3 帧
      ├─ DESTROYED→ 块中心大爆炸 5 帧
      ├─ NEVERDIE → 不处理
      └─ HOUSE_DOWN → delaySecondsToOver(2000)
      │
      ▼
⑥ 同一帧 drawExplodes() → gameMap.clearDestoryTile()
      └─ 倒序移除 !isVisible 的块并归还 TilesPool
      │
      ▼
⑦ 下一帧 gameMap.draw() 不再画这块
```

**关键点**：

| 阶段 | 说明 |
|---|---|
| 碰撞由块发起 | `MapTile.isCollideBullet()` 一次检查一辆坦克的全部子弹 |
| 状态由块自己改 | 扣血、隐藏、命中结果都在 `takeHit()` 内完成 |
| 坦克只做表现 | `handleTileHit()` 只根据 `HitResult` 播爆炸或结束游戏 |
| 爆炸挂在开火方 | 爆炸进开火坦克的 `explodes`，不进地图块 |
| 清理集中在一处 | `clearDestoryTile()` 是唯一的归还点 |

## 5 坦克撞上地图块

```text
① drawRun 末尾 → bulletCollideMapTile()
      │
      ▼
② myTank.isCollideTile(tiles)
      └─ 遍历每个块，调 tile.isCollideTank(x, y, RADIUS)
             │
             ▼
③ MapTile.isCollideTank()
      ├─ new Points8()：4 角 + 4 边中点
      └─ 任一点落进坦克方框 → true
      │
      ▼
④ 命中 → myTank.back()
      └─ x = oldX; y = oldY
      │
      ▼
⑤ 敌人也逐个检查，撞上同样 back()
```

`oldX/oldY` 在 `mathMove()` 开头记录，所以 `back()` 退回到的是「这一帧移动之前」的位置。下一帧如果仍朝同方向走，会再次撞上、再次回退，表现为坦克贴住块停下。

> 当前 `isCollideTank()` 不区分块类型，水/草也会挡坦克。这是待做项。

## 6 敌人出现

```text
① 菜单按回车 → newGame()
      ├─ gameMap = new GameMap()
      ├─ gameState = STATE_RUN
      ├─ myTank = new MyTank(100, 100, DIR_DOWN)
      └─ 启动敌人生成线程
      │
      ▼
② 生成线程循环
      ├─ enemyTanks.size() < 10 ? → enemyTanks.add(EnemyTank.createTank())
      ├─ createTank()
      │     ├─ 左上或右上出生
      │     ├─ EnemiesPool.get()
      │     ├─ resetAiTime()
      │     ├─ 重置坐标/方向/状态/血量/isEnemy
      │     └─ 归还残留子弹 + 清空弹夹
      └─ sleep(5000)
      │
      ▼
③ 重绘线程下一帧 → drawEnemyTanks()
      └─ enemy.draw(g)
           ├─ logic() → 向下移动
           ├─ drawImgTank() → ai() → 画敌图
           └─ 子弹/名字/血条
```

程序里有三个线程：

| 线程 | 职责 | 频率 |
|---|---|---|
| EDT | 按键、回调 `update()` | 按键时 / `repaint()` 后 |
| 重绘线程 | `repaint()` | 30ms |
| 生成线程 | 追加敌人 | 5000ms |

`enemyTanks` 会被生成线程 `add`、被 EDT 遍历和 `remove`。目前靠「add 只在尾部、频率极低」勉强安全，`ArrayList` 本身不是线程安全的。根治方案是 `CopyOnWriteArrayList`，目前未处理。

## 7 敌人从被打死到归还池子

```text
① 玩家子弹命中敌人
      │
      ▼
② collideBullets → hurt → hp <= 0
      │
      ▼
③ die()
      └─ 敌人：什么都不做（不归还）
      │
      ▼
④ 按扣血后的 hp 加死亡爆炸（5 帧，坦克中心）
      │
      ▼
⑤ drawExplodes() 开始播死亡爆炸
      │
      ▼
⑥ 下一帧 drawEnemyTanks()
      ├─ 第一趟：if (!isDie()) draw()     ← 死者跳过车身
      └─ 第二趟：if (isDie() && isExplodeFinished())
                   remove + EnemiesPool.theReturn
      │
      ▼
⑦ 尸体在列表里留约 450ms，期间：
      ├─ 爆炸继续播（drawExplodes 遍历得到它）
      └─ collideBullets 开头 return，不再挨打
      │
      ▼
⑧ 爆炸播完 → explodes 空 → 移除并还池
      │
      ▼
⑨ 下次 createTank() 取到它时重置全部状态
```

**为什么删除放在 `drawEnemyTanks()` 而不是 `die()`**：`die()` 只把血量判成 0；死亡爆炸还要靠 `drawExplodes()` 画完。如果 `die()` 立刻删除，`drawExplodes()` 就遍历不到这辆坦克，死亡爆炸永远画不出来。

## 8 玩家死亡或家被打中

```text
①a 玩家 hp <= 0 → Tank.die() → delaySecondsToOver(2000)
①b 子弹命中 HOUSE → takeHit 返回 HOUSE_DOWN
        → Tank.handleTileHit(HOUSE_DOWN) → delaySecondsToOver(2000)
      │
      ▼
② delaySecondsToOver()
      └─ 新开线程 sleep(2000) → GameFrame.setGameState(STATE_OVER)
      │
      ▼
③ 下一帧 update() → drawOver()
      ├─ 首次调用加载 res/gameover.png
      ├─ 画布填 #636363
      └─ 图片放大到 600×600 + 两行提示
      │
      ▼
④ keyEventOver()
      ├─ ESC   → System.exit(0)
      └─ ENTER → restGame() → gameState = STATE_MENU
```

`restGame()` 当前只归还子弹：`myTank.bulletsReturn()`、每个敌人 `bulletsReturn()`，然后 `enemyTanks.clear()`、`gameMap = null`。敌人和地图块没有还回各自的池，属于已知问题。

> 敌人打中家也会结束游戏：`HOUSE_DOWN` 分支挂在开火坦克身上，不区分敌我。

---

下一篇：[进度与待修](07-progress.md) · 返回 [README](../README.md)
