# `/build_portal` 调试指令设计文档

> **文档类型**：技术设计规范（Technical Design Spec）
> **版本**：v1.1.0（迁移并据最终代码校正）
> **日期**：2026-09-30（迁移 2026-10-01）
> **平台**：Minecraft **1.21.1** + **NeoForge 21.1.252** · JDK 21 · ModDevGradle 2.0.x
> **命名空间**：`weather_realm`
> **关联文档**：[`docs/ARCHITECTURE.md`](../ARCHITECTURE.md) §5.1（传送门激活数据流）、[`docs/DESIGN.md`](../DESIGN.md) §4（天气系统与气候传送门）
> **权威约束**：技术骨架以仓库根目录 [`AGENTS.md`](../../AGENTS.md) 为准。
> **实现文件**：`src/main/java/com/example/weather_realm/command/PortalCommands.java`（106 行）。

---

## 1. 目标

提供一条**一键搭建未激活传送门**的调试指令 `/build_portal`，用于开发期快速复现气候传送门的点燃流程，免去手动摆放 4×4 框架与注水的重复劳动。

- **定位**：仅调试 / 联调用途，便于验证 [`ClimatePortalHandler`](../../src/main/java/com/example/weather_realm/portal/ClimatePortalHandler.java) 的 `isPoolValid` + `isRingValid` 判定链路。
- **产物**：在指令执行者**脚下**生成一座结构完整、**尚未点燃**的传送门底座（2×2 水源 + 12 格外围框架），玩家随后投掷 `climate_shard` 即可点燃。
- **非目标**：不负责点燃（点燃仍由投掷碎片的 `ClimatePortalHandler` 完成）、不生成已激活 `weather_portal`、不处理跨维度传送。

---

## 2. 架构与类设计

### 2.1 类与包

| 项 | 值 |
| :- | :- |
| 全限定类名 | `com.example.weather_realm.command.PortalCommands` |
| 类型 | `final` 工具类（私有构造器，纯静态方法） |
| 职责 | 注册 `/build_portal` 指令并执行结构生成 |

### 2.2 事件监听与总线

- **事件**：`net.neoforged.neoforge.event.RegisterCommandsEvent`
- **总线**：Game 事件总线（声明式 `@EventBusSubscriber`，`PortalCommands.java:27`）
- **绑定方式**：`@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME)`

```java
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PortalCommands {

    private PortalCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("build_portal")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> buildPortal(ctx.getSource())));
    }
}
```

### 2.3 权限

- 通过 `.requires(source -> source.hasPermission(2))` 限定，语义即 **OP 等级 2 或作弊开启时可用**；不满足者指令不可见。
- 执行者必须是实体玩家（`ctx.getSource().getPlayerOrException()`），以取得脚下坐标（`PortalCommands.java:47-53`）。

---

## 3. 结构生成逻辑

### 3.1 坐标系定义

设执行者脚下方块坐标为 `(px, py, pz)` = `player.blockPosition()`：

| 基准 | 值 | 说明 |
| :- | :- | :- |
| 玩家脚下 | `px, py, pz` | 指令执行位置 |
| 4×4 区域最小角 | `px - 1, py - 1, pz - 1` | 含 12 格边框的完整范围，向 `+x / +z` 扩展（`min`，`:56`） |

### 3.2 分层生成表

| 层 (Y) | 范围 | 方块 | 目的 |
| :- | :- | :- | :- |
| `py - 2` | 4×4 底座 | `Blocks.SMOOTH_STONE` | 承载水源，**防止水流泄漏**（`:58-64`） |
| `py - 1` | 2×2 中心 | `Blocks.WATER` | 水源池（`isPoolValid` 判定必需，`:71,87`） |
| `py - 1` | 12 格外框 | 见 §3.3 图案 | 框架标签匹配（`isRingValid` 判定必需，`:67-91`） |
| `py` | 4×4 | `Blocks.AIR` | 清空，保证站位通畅（`:93-101`） |
| `py + 1` | 4×4 | `Blocks.AIR` | 清空，保证头部空间通畅（`:93-101`） |

> 放置统一使用 `level.setBlock(pos, state, 3)`（flag `3`），保证流体更新正确。

### 3.3 12 格外框图案（与游戏内手记一致）

图案取自 Patchouli 图鉴的 4×4 `multiblock` 矩阵，四角对称（`PortalCommands.java:73-78`）：

```
        dx=0  dx=1  dx=2  dx=3
dz=0:    o     i     s     n
dz=1:    i     w     w     s
dz=2:    s     w     w     i
dz=3:    n     i     s     o
```

坐标相对 4×4 最小角 `(px-1, py-1, pz-1)` 偏移 `(dx, dz)`。

| 图案字符 | 对应标签 | 本指令选用方块 | 偏移 `(dx, dz)` |
| :-: | :- | :- | :- |
| `i` | `#weather_realm:portal_frames_ice`（极寒之石） | `minecraft:packed_ice`（浮冰） | `(1,0) (0,1) (3,2) (2,3)` |
| `o` | `#weather_realm:portal_frames_abyss`（晦暗之石） | `minecraft:obsidian`（黑曜石） | `(0,0) (3,3)` |
| `s` | `#weather_realm:portal_frames_sand`（荒漠之石） | `minecraft:sandstone`（砂岩） | `(2,0) (3,1) (0,2) (1,3)` |
| `n` | `#weather_realm:portal_frames_nether`（烈焰之石） | `minecraft:magma_block`（岩浆块） | `(3,0) (0,3)` |
| `w` | `minecraft:water`（2×2 水源） | `minecraft:water` | `(1,1) (2,1) (1,2) (2,2)` |

- 框架方块数：`4 + 2 + 4 + 2 = 12`，与 §3.2 一致。
- 四个材质均落在 `#weather_realm:climate_portal_frames` 的子标签中（`data/weather_realm/tags/block/climate_portal_frames.json`），**可直接通过 `isRingValid` 判定**。

### 3.4 生成算法（与实现一致的伪代码）

```java
private static int buildPortal(CommandSourceStack source) throws CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    ServerLevel level = player.serverLevel();

    BlockPos foot = player.blockPosition();
    int px = foot.getX();
    int py = foot.getY();
    int pz = foot.getZ();

    // 4x4 区域最小角（py-1 层的最小角）
    BlockPos min = new BlockPos(px - 1, py - 1, pz - 1);

    // 1) y = py-2 : 4x4 平滑石承台，防止水流下泄
    for (int dx = 0; dx < 4; dx++)
        for (int dz = 0; dz < 4; dz++)
            level.setBlock(min.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState(), 3);

    // 2) y = py-1 : 按 3.3 图案稀疏写入 16 格（12 边框 + 4 水源）
    for (int dz = 0; dz < 4; dz++)
        for (int dx = 0; dx < 4; dx++)
            level.setBlock(min.offset(dx, 0, dz), stateFor(pattern[dz][dx]), 3);

    // 3) y = py 与 y = py+1 : 4x4 置空
    for (int dy = 1; dy <= 2; dy++)
        for (int dx = 0; dx < 4; dx++)
            for (int dz = 0; dz < 4; dz++)
                level.setBlock(min.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);

    // 4) 操作成功反馈（广播 sendSuccess(..., true)）
    source.sendSuccess(() -> Component.literal(
            "§a[天象之境] 已在脚下成功搭建未激活传送门！向水中丢入气候碎片即可激活。"), true);
    return 1;
}
```

- `pattern` 为 §3.3 的 `char[4][4]`；`stateFor` 把 `o/i/s/n` 映射为黑曜石/浮冰/砂岩/岩浆块，其余（`w`）为水。
- 该实现**不调用** `source.sendFailure`，也不依赖任何翻译键。

### 3.5 成功反馈文字（实现现状）

| 触发 | 文本（字面量，硬编码中文） | 广播 |
| :- | :- | :- |
| `buildPortal` 末尾 | `§a[天象之境] 已在脚下成功搭建未激活传送门！向水中丢入气候碎片即可激活。` | 是（`sendSuccess(..., true)`，`PortalCommands.java:103`） |

> 迁移校正：早期设计稿曾使用翻译键 `message.weather_realm.build_portal.success` 并计划非广播；**最终实现使用硬编码字面量并广播**，此处按代码更正。

---

## 4. 验证与门禁

### 4.1 编译与部署校验

一键构建并注入 PCL 客户端 mods 目录：

```powershell
.\deploy.ps1
```

`deploy.ps1` 会校验 / 切换 JDK 21 → 执行 `.\gradlew.bat build`（非零退出码立即中止）→ **检测到目标实例客户端在运行时拒绝部署**（可用 `-Force` 跳过）→ 将最新 `weather_realm-1.21.1-1.0.0.jar` 覆盖注入目标 mods 目录。部署后必须**完全重启客户端**。

### 4.2 功能验收清单

| # | 步骤 | 预期结果 |
| :- | :- | :- |
| 1 | `.\gradlew.bat build` | 编译通过，产出 jar |
| 2 | 客户端内 `/build_portal`（OP 2+） | 脚下生成 4×4 未激活传送门底座 |
| 3 | 观察脚下 | `y=py-2` 平滑石承台 / `y=py-1` 2×2 水源 + 12 格四类框架 / `y=py..py+1` 为空气 |
| 4 | 投掷 `weather_realm:climate_shard` 入池 | `ClimatePortalHandler` 判定通过并点燃为 `weather_portal` |
| 5 | 无 OP / 未开作弊执行 | 指令不可见 / 被拒绝 |
| 6 | `.\gradlew.bat runServer` | 服务端干净启动（无客户端类泄漏，Common 代码未引用 `net.minecraft.client.*`） |

### 4.3 热重载提示

- 新增 `PortalCommands` 并注册 `@SubscribeEvent` 属于**启动期扫描项**，首次加入后需**重启客户端**；其后仅改方法体可走 JBR HotSwap（见 `AGENTS.md` §6）。
