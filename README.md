<h1 align="center">坦克大战第二版说明</h1>

<h2 align="center">补全关卡，障碍物，动态效果 ai敌人加强</h2>

## 构建流程

### 1. 窗口初始化

对游戏窗口进行初始化，设置标题、大小、坐标、窗口的事件监听。

- 游戏窗口使用 `JFrame`
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

#### 在不同的状态下，绘制不同的内容，按键有不同的处理方式

- 在GameFrame类里使用 `public static int gameState;`管理游戏状态，
