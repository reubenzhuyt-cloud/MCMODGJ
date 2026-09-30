# `/build_portal` 指令 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增一条 OP 调试指令 `/build_portal`，在执行玩家脚下直接搭建一座结构完整、**尚未激活**的 4×4 气候传送门底座（2×2 水源 + 12 格外围框架），随后投掷 `weather_realm:climate_shard` 即可点燃。

**Architecture:** 单个 `final` 工具类 `PortalCommands` 以声明式 `@EventBusSubscriber(bus = GAME)` 监听 `RegisterCommandsEvent`，向 Brigadier 调度器注册一个 `requires(hasPermission(2))` 的 `build_portal` 字面量节点。执行时取 `player.blockPosition()` 为基准，分四层（`py-2` 底座 / `py-1` 框架+水源 / `py` 与 `py+1` 净空）逐格 `level.setBlock(..., 3)`，全部逻辑为纯服务端 Common 代码，**不引用任何 `net.minecraft.client.*`**。

**Tech Stack:** Minecraft 1.21.1 · NeoForge 21.1.252 · JDK 21 · ModDevGradle 2.0.x · Brigadier (`Commands` / `CommandSourceStack`) · Parchment 2024.11.17。

## Global Constraints

以下为项目级硬约束，**每个 Task 都隐含遵守**（摘自仓库根 `AGENTS.md`）：

- Minecraft **1.21.1**；加载器 **NeoForge 21.1.252**（`neo_version` 写作 `21.1.252`，**不得**写成 `1.21.1-21.1.252`）。
- JDK **21**；Gradle 由 `.\gradlew.bat` wrapper 管理；**Windows 命令一律以 `.\gradlew.bat` 为前缀**。
- 构建插件 **ModDevGradle 2.0.x**（`net.neoforged.moddev`）；映射 **Parchment 2024.11.17**。
- 命名空间固定为 `weather_realm`，Mod ID 常量 `WeatherRealm.MODID`。
- 事件总线：初始化/加载期事件挂 **Mod 事件总线**；每帧/每 Tick 事件挂 **Game 事件总线** `NeoForge.EVENT_BUS`。
- **Common 代码禁止引用 `net.minecraft.client.*`**；合并前必须能干净跑起 `.\gradlew.bat runServer`。
- **禁止** ItemStack NBT（`ItemStack.getTag()` 在 1.20.5+ 已移除）；需要数据用强类型 `DataComponentType<T>`。
- 声明式事件绑定统一用 `@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME)` + `@SubscribeEvent static void`。
- 本任务**不新增**任何 Block/Item/EntityType 注册项，因此不触碰注册表冻结问题；但新增 `@SubscribeEvent` 监听属于启动期扫描项，**首次引入后必须重启客户端**。
- 采用跳板模式的思想：本指令不涉及 Mixin；`PortalCommands` 为普通类，方法体改动可走 JBR HotSwap。

---

## File Structure

| 文件 | 操作 | 职责 |
| :- | :- | :- |
| `src/main/java/com/example/weather_realm/command/PortalCommands.java` | **Create** | 注册 `/build_portal` 指令；依据执行者坐标分层搭建未激活传送门底座 |
| `src/main/resources/data/weather_realm/tags/block/climate_portal_frames.json` | Read-only 依赖 | 提供 `#weather_realm:climate_portal_frames` 标签（本指令所选四种方块均落入其子标签） |
| `src/main/java/com/example/weather_realm/portal/ClimatePortalHandler.java` | Read-only 依赖 | 负责后续「投掷碎片点燃」判定链路（`isPoolValid` + `isRingValid`），本指令不修改 |
| `build/libs/weather_realm-*.jar` | 构建产物 | Task 2 校验其中含 `com/example/weather_realm/command/PortalCommands.class` |

**设计说明（为什么不做自动化测试）：** Brigadier 指令需要携带 `ServerPlayer` 的 `CommandSourceStack`，而 NeoForge GameTest 环境不提供玩家实体，无法在无头 `runGameTestServer` 中驱动本指令。因此本计划的自动门禁为**编译通过 + jar 字节码核查**，功能正确性由 Task 2 的游戏内清单人工验收。

---

## 参考：4×4 标准矩阵与方块映射

图案取自 `mod.md` §6.2 的 Patchouli `multiblock` 矩阵（四角对称），相对 4×4 最小角 `(px-1, py-1, pz-1)` 的偏移为 `(dx, dz)`：

```
        dx=0  dx=1  dx=2  dx=3
dz=0:    o     i     s     n
dz=1:    i     w     w     s
dz=2:    s     w     w     i
dz=3:    n     i     s     o
```

| 字符 | 主题 | 本指令选用方块 | 偏移 `(dx, dz)` |
| :-: | :- | :- | :- |
| `i` | 极寒之石 → `#weather_realm:portal_frames_ice` | `minecraft:packed_ice` | `(1,0) (0,1) (3,2) (2,3)` |
| `o` | 晦暗之石 → `#weather_realm:portal_frames_abyss` | `minecraft:obsidian` | `(0,0) (3,3)` |
| `s` | 荒漠之石 → `#weather_realm:portal_frames_sand` | `minecraft:sandstone` | `(2,0) (3,1) (0,2) (1,3)` |
| `n` | 烈焰之石 → `#weather_realm:portal_frames_nether` | `minecraft:magma_block` | `(3,0) (0,3)` |
| `w` | 2×2 水源 | `minecraft:water` | `(1,1) (2,1) (1,2) (2,2)` |

- 框架方块数 `4 + 2 + 4 + 2 = 12`；水源 4 格；合计 16 格。
- 四个材质均落在 `#weather_realm:climate_portal_frames` 的子标签中，可被 `ClimatePortalHandler.isRingValid` 判定通过。

---

## Task 1: 编写 `PortalCommands.java`

**Files:**
- Create: `src/main/java/com/example/weather_realm/command/PortalCommands.java`
- Read-only 依赖: `src/main/resources/data/weather_realm/tags/block/climate_portal_frames.json`
- Read-only 依赖: `src/main/java/com/example/weather_realm/WeatherRealm.java`（`MODID` 常量）

**Interfaces:**
- Consumes:
  - `WeatherRealm.MODID`（`String`，值 `"weather_realm"`）。
  - NeoForge `RegisterCommandsEvent`（`net.neoforged.neoforge.event.RegisterCommandsEvent`），提供 `getDispatcher()`。
  - MC `Commands.literal(String)` / `CommandSourceStack.hasPermission(int)` / `CommandSourceStack.getPlayerOrException()` / `ServerPlayer.serverLevel()` / `ServerPlayer.blockPosition()` / `ServerLevel.setBlock(BlockPos, BlockState, int)` / `CommandSourceStack.sendSuccess(Supplier<Component>, boolean)`。
- Produces:
  - `public final class com.example.weather_realm.command.PortalCommands`（私有构造器，无实例状态）。
  - `public static void onRegisterCommands(RegisterCommandsEvent event)`（`@SubscribeEvent`，注册字面量指令 `build_portal`）。
  - `private static int buildPortal(CommandSourceStack source) throws CommandSyntaxException`（执行体，返回 `1`）。

- [ ] **Step 1: 核对只读依赖（确认标签与坐标约定，无需修改）**

Run:
```powershell
Get-Content .\src\main\resources\data\weather_realm\tags\block\climate_portal_frames.json
Get-Content .\src\main\resources\data\weather_realm\tags\block\portal_frames_ice.json
```

Expected：第一个文件列出 4 个子标签 `#weather_realm:portal_frames_ice/abyss/sand/nether`；`portal_frames_ice.json` 的 `values` 含 `minecraft:packed_ice`。这确认 `packed_ice / obsidian / sandstone / magma_block` 四种方块均满足 `#weather_realm:climate_portal_frames`。

- [ ] **Step 2: 创建 `PortalCommands.java`（完整文件）**

Create `src/main/java/com/example/weather_realm/command/PortalCommands.java` with exactly:

```java
package com.example.weather_realm.command;

import com.example.weather_realm.WeatherRealm;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 调试指令 / Debug command: {@code /build_portal}.
 *
 * <p>在指令执行者脚下直接搭建一座结构完整、尚未点燃的 4x4 气候传送门底座
 * （{@code py-2} 平滑石承台 + {@code py-1} 层 2x2 水源与 12 格外围框架 + {@code py..py+1} 净空），
 * 便于开发期验证 {@link com.example.weather_realm.portal.ClimatePortalHandler} 的
 * {@code isPoolValid} + {@code isRingValid} 判定链路。指令本身<b>不</b>点燃传送门——
 * 点燃仍由玩家向水池投掷 {@code weather_realm:climate_shard} 触发。</p>
 */
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

    /**
     * 以玩家脚下方块为基准分层搭建未激活传送门。
     *
     * @return 恒为 {@code 1}（Brigadier 成功返回码）。
     */
    private static int buildPortal(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        BlockPos foot = player.blockPosition();
        int px = foot.getX();
        int py = foot.getY();
        int pz = foot.getZ();

        // 4x4 区域最小角（py-1 层的最小角）。
        BlockPos min = new BlockPos(px - 1, py - 1, pz - 1);

        // 1) y = py-2：4x4 平滑石承台，防止水流向下泄漏。
        BlockState smoothStone = Blocks.SMOOTH_STONE.defaultBlockState();
        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 4; dz++) {
                level.setBlock(min.offset(dx, -1, dz), smoothStone, 3);
            }
        }

        // 2) y = py-1：按标准矩阵铺设 12 格框架 + 2x2 水源。
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState packedIce = Blocks.PACKED_ICE.defaultBlockState();
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        char[][] pattern = {
                {'o', 'i', 's', 'n'},
                {'i', 'w', 'w', 's'},
                {'s', 'w', 'w', 'i'},
                {'n', 'i', 's', 'o'}
        };

        for (int dz = 0; dz < 4; dz++) {
            for (int dx = 0; dx < 4; dx++) {
                BlockState state = switch (pattern[dz][dx]) {
                    case 'o' -> obsidian;
                    case 'i' -> packedIce;
                    case 's' -> sandstone;
                    case 'n' -> magma;
                    default -> water;
                };
                level.setBlock(min.offset(dx, 0, dz), state, 3);
            }
        }

        // 3) y = py 与 y = py+1：4x4 置空，保证站位与头部净空。
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dy = 1; dy <= 2; dy++) {
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    level.setBlock(min.offset(dx, dy, dz), air, 3);
                }
            }
        }

        source.sendSuccess(() -> Component.literal("§a[天象之境] 已在脚下成功搭建未激活传送门！向水中丢入气候碎片即可激活。"), true);
        return 1;
    }
}
```

- [ ] **Step 3: 编译验证（自动化门禁）**

Run:
```powershell
.\gradlew.bat compileJava
```

Expected：`BUILD SUCCESSFUL`；无 `cannot find symbol` / `incompatible types` / `unreported exception` 报错。若 `net.neoforged.neoforge.event.RegisterCommandsEvent` 报找不到符号，运行 `.\gradlew.bat genSources` 后重试。

- [ ] **Step 4: 提交**

```powershell
git add src/main/java/com/example/weather_realm/command/PortalCommands.java
git commit -m "feat: add /build_portal debug command to place an inactive climate portal"
```

---

## Task 2: 构建部署与字节码核查

**Files:**
- Read-only 依赖: `deploy.ps1`（一键构建 + 注入 PCL 客户端 mods 目录）
- Verify: `build/libs/weather_realm-*.jar` 内含 `com/example/weather_realm/command/PortalCommands.class`

**Interfaces:**
- Consumes: Task 1 产出的 `PortalCommands.class`。
- Produces: 注入到 `C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\` 的可联调 jar。

- [ ] **Step 1: 一键构建并注入**

Run:
```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\deploy.ps1
```

Expected：脚本依次输出 `[deploy] JAVA_HOME OK`（或切换到 JDK 21）、`BUILD SUCCESSFUL`、`[deploy] ===== DEPLOY OK =====`，并打印 jar 名 / 大小 / 目标目录。任一环节非零退出码都会被脚本中止透传，**不得**带着报错继续。

- [ ] **Step 2: 核查 jar 内含 `PortalCommands.class`**

Run:
```powershell
$jar = Get-ChildItem .\build\libs\weather_realm-*.jar |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" -and $_.Name -notlike "*-dev.jar" } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
Write-Host "Inspecting: $($jar.Name)"
& "$env:JAVA_HOME\bin\jar.exe" tf $jar.FullName | Select-String "com/example/weather_realm/command/PortalCommands.class"
```

Expected：至少输出一行 `com/example/weather_realm/command/PortalCommands.class`。若为空，说明 Task 1 的文件未被编入主 sourceSet，检查包路径与 `src/main/java` 目录结构。

- [ ] **Step 3: 游戏内功能验收清单（人工）**

在 PCL 客户端内**重启**后逐项核对（新增 `@SubscribeEvent` 属启动期扫描项，必须重启，不可热更）：

| # | 操作 | 预期结果 |
| :- | :- | :- |
| 1 | OP 等级 2+ 执行 `/build_portal` | 聊天栏出现绿色提示 `[天象之境] 已在脚下成功搭建未激活传送门！向水中丢入气候碎片即可激活。` |
| 2 | 观察脚下 `y=py-2` 层 | 4×4 `smooth_stone` 承台 |
| 3 | 观察 `y=py-1` 层 | 中心 2×2 为水源；外圈 12 格依次为 `obsidian / packed_ice / sandstone / magma_block`（四角对称，符合标准矩阵） |
| 4 | 观察 `y=py` 与 `y=py+1` 层 | 4×4 全部为空气，玩家站在水源正上方且头部无障碍 |
| 5 | 向水池投掷 `weather_realm:climate_shard` | `ClimatePortalHandler` 判定通过，2×2 覆写为 `weather_portal` 并播放雷霆音效与粒子 |
| 6 | 无 OP / 未开作弊执行 `/build_portal` | 指令不可见或被拒绝（`hasPermission(2)` 拦截） |
| 7 | 执行 `.\gradlew.bat runServer` | 独立服务端干净启动，无 `ClassNotFoundException`（证明未引用 `net.minecraft.client.*`） |

- [ ] **Step 4: 提交部署产物（如仓库跟踪 `build/` 则跳过，通常 `build/` 已被忽略）**

```powershell
git status --short
```

Expected：`build/` 若在 `.gitignore` 中则无待提交项；本 Task **不产生源码改动**，无需新增提交。若 Step 3 暴露出逻辑问题，则回到 Task 1 修改 `buildPortal` 方法体（普通类方法体可走 JBR HotSwap，无需重启即可验证，除非改动了指令注册节点本身）。

---

## Self-Review

**1. 规格覆盖：**
- 目标（脚下搭建未激活 4×4 传送门）→ Task 1 Step 2。
- `command` 包 + `@EventBusSubscriber(GAME)` + `RegisterCommandsEvent` → Task 1 Step 2。
- `Commands.literal("build_portal").requires(hasPermission(2)).executes(...)` → Task 1 Step 2。
- `py-2` 平滑石承台 / `py-1` 中心 2×2 水源 + 12 格框架 / `py..py+1` 净空 → Task 1 Step 2 的矩阵与分层循环。
- 指定成功反馈文案（`§a[天象之境]...`，广播 `true`）→ Task 1 Step 2 的 `sendSuccess`。
- 部署与字节码核查 → Task 2 Step 1–2。
- 游戏内验证步骤 → Task 2 Step 3。

**2. 占位符扫描：** 无 `TBD` / `TODO` / “类似上文” / “补充错误处理” 等表述；所有代码步骤均给出完整可编译代码；所有命令均给出精确命令与预期输出。

**3. 类型一致性：** 类名 `PortalCommands`、事件 `RegisterCommandsEvent`、入口 `onRegisterCommands(RegisterCommandsEvent)`、执行体 `buildPortal(CommandSourceStack)` 在全文档中拼写一致；矩阵字符 `o/i/s/n/w` 与 `switch` 分支、方块映射表逐一对应；4×4 最小角统一为 `(px-1, py-1, pz-1)`，`py-2` 层写作 `min.offset(dx, -1, dz)`。
