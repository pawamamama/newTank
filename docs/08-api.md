# API 速查

> 返回 [README](../README.md)

本篇是写代码时最常查的速查表，不解释项目结构。

## 1 绘制

```java
g.setColor(Color.RED);
g.fillRect(x, y, w, h);          // 实心矩形
g.drawRect(x, y, w, h);          // 空心矩形
g.fillOval(x, y, w, h);          // 实心椭圆（w==h 时是圆）
g.drawOval(x, y, w, h);          // 空心椭圆
g.drawString("文字", x, baselineY);   // y 是基线，不是字顶
g.drawImage(img, x, y, null);          // 原尺寸
g.drawImage(img, x, y, w, h, null);    // 缩放到 w×h
g.drawImage(img, x, y, w, h, observer);
```

## 2 线宽

```java
Graphics2D g2d = (Graphics2D) g;
g2d.setStroke(new BasicStroke(3.0f));
```

`setStroke` 只对 `drawXxx` 生效，对 `fillXxx` 无效。

## 3 渲染提示

```java
g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // 像素画
// VALUE_INTERPOLATION_BILINEAR                                 // 照片
```

## 4 坐标系

- 原点 `(0,0)` 在画布左上角，标题栏下面。
- `x` 向右增大。
- `y` **向下**增大，和数学坐标相反。
- 往上走是 `y--`，往下走是 `y++`。

## 5 居中公式

```java
// 宽 w 的矩形水平居中于 centerX
int left = centerX - w / 2;

// 文字水平居中：宽度必须用 FontMetrics 量
FontMetrics fm = g.getFontMetrics();
int textW = fm.stringWidth(text);
g.drawString(text, centerX - textW / 2, baselineY);
```

宽度用偶数：`50 / 2 = 25` 精确；`51 / 2` 截断，左右差 1 像素。

## 6 位运算

| 写法 | 等价 | 优先级 |
|---|---|---|
| `n << 1` | `n * 2` | 低于 `+` `-`：`y - R << 1` 等于 `(y-R) << 1` |
| `n >> 1` | `n / 2`（整数除） | 正数时与 `/2` 相同 |
| `n << 2` | `n * 4` | 同上 |

优先级从高到低：`()` > `++ --` > `* / %` > `+ -` > `<< >>` > 比较 > `&& ||` > `=`。

## 7 整数除法

```java
hp / DEFAULT_HP * BAR_LENGTH     // 错：600/1000 = 0，结果 0
hp * BAR_LENGTH / DEFAULT_HP     // 对：600*50/1000 = 30
```

规律：整数除法前，把乘法提前。

## 8 访问修饰符

| 修饰符 | 本类 | 同包 | 子类（不同包） | 任何地方 |
|---|---|---|---|---|
| `private` | 是 | 否 | 否 | 否 |
| 默认（不写） | 是 | 是 | 否 | 否 |
| `protected` | 是 | 是 | 是 | 否 |
| `public` | 是 | 是 | 是 | 是 |

`protected` 的准确范围是「本类 + 同包 + 子类」，不只是子类。

## 9 抽象类

| 规则 | 说明 |
|---|---|
| 抽象方法只能写在抽象类或接口里 | 写在普通类里编译不过 |
| 类里有抽象方法 → 类必须 `abstract` | 否则编译错误 |
| 抽象类不能 `new` | `new Tank(...)` 不允许 |
| 抽象类可以有构造器 | 供子类 `super(...)` 调用 |
| 子类必须实现全部抽象方法 | 否则子类自己也得声明 `abstract` |
| 抽象方法不能配 `private/static/final/native/synchronized` | 与「等子类实现」矛盾 |

## 10 内部类

| 类型 | 写法 | 能访问外部实例字段 |
|---|---|---|
| 成员内部类 | `class A {}` | 能（编译器加 `this$0`） |
| 静态嵌套类 | `static class A {}` | 不能 |
| 局部内部类 | 方法里 `class A {}` | 能 |
| 匿名内部类 | `new Runnable() {}` | 能 |

能加 `static` 就加：非静态内部类持有外部引用，可能造成内存泄漏。

## 11 静态方法

```java
public static EnemyTank createTank() {
    // 不能直接访问实例字段（没有 this）
    // x = 100;

    // 静态字段可以直接用
    int y = GameFrame.titleBarHeight + RADIUS;

    // 实例成员通过对象引用访问
    EnemyTank e = EnemiesPool.get();
    e.setX(100);
    e.resetAiTime();
}
```

## 12 按键常量

```java
KeyEvent.VK_UP / VK_DOWN / VK_LEFT / VK_RIGHT
KeyEvent.VK_W / VK_S / VK_A / VK_D
KeyEvent.VK_ENTER
KeyEvent.VK_SPACE
KeyEvent.VK_ESCAPE
```

## 13 List 常用操作

| 操作 | 写法 | 注意 |
|---|---|---|
| 按索引删 | `list.remove(0)` | 返回被删元素 |
| 按对象删 | `list.remove(bullet)` | 返回 boolean |
| 遍历中删除 | 倒序 `for (int i = size - 1; i >= 0; i--)` | 正序会漏删 |
| 判空 | `list.isEmpty()` | 比 `size() == 0` 更表意 |

---

返回 [README](../README.md)
