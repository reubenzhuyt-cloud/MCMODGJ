# TEST_COMMANDS.md — 天象之境 开发 / 联调命令速查

> 本文件是 **weather_realm（天象之境）** 的测试与调试命令速查表。
> 约定见根目录 `AGENTS.md`；命令前缀在 Windows 一律用 `.\gradlew.bat`。
> 文中所有 id 均取自实际资源：`assets/weather_realm/lang/zh_cn.json` 与 `data/weather_realm/**`。

## 目录

1. [维度与传送调试](#1-维度与传送调试-dimension--worldgen)
2. [物品、方块与数据组件测试](#2-物品方块与数据组件测试-items--blocks)
3. [动态重载与数据包测试](#3-动态重载与数据包测试-hot-reload--datapacks)
4. [实体与环境调试](#4-实体与环境调试-entities--environment)
5. [实用快捷键与 HUD](#5-实用快捷键与-hud-dev-hotkeys)
6. [GameTest 自动化测试](#6-gametest-自动化测试-gametest-framework)
7. [编译与一键注入](#7-编译与一键注入-build--deploy)

> **权限前提**：绝大多数命令需要**管理员权限**（创造模式 / OP）。
> 单人存档：在「对局域网开放」中开启作弊，或使用开启作弊的存档；
> 独立服务端：控制台直接执行，或将自己加入 `ops.json`。

---

## 1. 维度与传送调试 (Dimension & Worldgen)

| 目的 | 命令 | 说明 |
| :- | :- | :- |
| 传送到天象之境 | `/execute in weather_realm:crystal_realm run tp @s 0 100 0` | `y=100` 仅为占位，落点可能悬空 |
| 安全地表落点传送 | `/execute in weather_realm:crystal_realm run spreadplayers 0 0 10 50 under 256 false @s` | 自动寻找安全地表，避免卡方块 / 掉虚空 |
| 返回主世界 | `/execute in minecraft:overworld run tp @s 0 100 0` | 切回原版维度 |
| 寻找群系（水晶平原） | `/locate biome weather_realm:crystal_plains` | 三个群系同理：`blazing_plains` / `arid_wasteland` |
| 寻找结构 | `/locate structure weather_realm:crystal_village` | 另有 `weather_realm:shelter` / `yanjiang` / `fire` |
| 放置结构 | `/place structure weather_realm:weather_altar` | 气象祭坛缺 `structure_set`，**不会自然生成**，只能手动放置 |
| 一键搭建未激活传送门 | `/build_portal`（OP 2+） | 脚下生成 4×4 底座；点燃仍需投掷气候碎片 |

```mcfunction
# 推荐：进入天象之境的省事姿势
/execute in weather_realm:crystal_realm run spreadplayers 0 0 10 50 under 256 false @s

# 调试传送门：先搭底座，再丢气候碎片
/build_portal
```

> **Tip**：`spreadplayers <x> <z> <spreadDistance> <maxRange> under <maxHeight> <respectTeams> <targets>`。
> `spreadDistance` 需小于 `maxRange`；`under 256` 表示只落在 `y<=256` 的地表。
>
> **⚠️ 数据驱动提醒**：新增维度 / 群系 / 结构属于**动态注册表**，`/reload` **不生效**，
> 需**保存退出到主菜单 → 重进存档**（见第 3 节）。

---

## 2. 物品、方块与数据组件测试 (Items & Blocks)

| 目的 | 命令 | 说明 |
| :- | :- | :- |
| 给予气候碎片 | `/give @s weather_realm:climate_shard 16` | 投掷入水可点燃传送门 |
| 给予晶石 / 手记 / 天象图 | `/give @s weather_realm:blizzard_crystal 64` | 另有 `blaze_crystal` / `wind_crystal` / `ancient_weather_tome` / `biome_map` |
| 给予方块（物品形式） | `/give @s weather_realm:frost_log 64` | 任意方块 id 同理 |
| 放置方块 | `/setblock ~ ~ ~ weather_realm:weather_altar_core` | 可加 `replace` / `destroy` / `keep` |
| 放置调谐基座 | `/setblock ~ ~-1 ~ weather_realm:weather_pedestal` | 核心在基座之上时右键可打开天象调控仪 |
| 检查手持物品组件数据 | `/data get entity @s SelectedItem` | **1.20.5+ 强类型 DataComponent**，不要再找 NBT |
| 查看组件详情 | `/data get entity @s SelectedItem.components` | 天象图的关键组件是 `minecraft:map_id`（值为 `-1`） |
| 原地生成物品实体 | `/summon item ~ ~1 ~ {Item:{id:"weather_realm:climate_shard",count:1}}` | 测试掉落 / 拾取；也用于测试碎片点燃 |

```mcfunction
# 读取手持物品的全部数据（含强类型组件）
/data get entity @s SelectedItem

# 天象图出厂即带固定的负值 map_id
/give @s weather_realm:biome_map
/data get entity @s SelectedItem.components
```

> **Tip**：1.20.5+ **禁止** `ItemStack` NBT，自定义数据一律走
> `DataComponentType<T>` + `stack.set(COMPONENT.get(), val)`。
> 用 `F3 + H` 可让工具提示直接显示物品 ID、耐久与**组件数量**（见第 5 节）。
> 本项目目前唯一自定义视图组件是原版 `minecraft:map_id`（由 `BiomeMapItem` 承载）。

---

## 3. 动态重载与数据包测试 (Hot Reload & Datapacks)

不同资源类型对应**不同的重载动作**，选错会白等或误判：

| 资源类型 | 重载动作 | 生效时间 | 备注 |
| :- | :- | :- | :- |
| 配方 / 标签 / 战利品表 / 函数 | 游戏内 `/reload` | ~1 秒 | 最快；数据包热重载 |
| 贴图 / 模型 / blockstates / 语言包 | IDE `Ctrl + F9` 增量构建 → 游戏内 `F3 + T` | 2~5 秒 | 客户端**必须**先构建 |
| 附魔 / 伤害类型 / 画 / 唱片机歌曲 | 退出到主菜单 → 重进存档 | 3~5 秒 | 动态注册表 |
| 维度 / 结构 / 生物群系 / worldgen | 退出到主菜单 → 重进存档 | 3~5 秒 | **`/reload` 无效** |

```mcfunction
# 1) 数据包热重载（配方/标签/战利品/函数）
/reload

# 2) 客户端资源：先在 IDE 执行 Ctrl+F9，再在游戏内按 F3+T
#    （F3+T 不可用指令触发，只能按热键）
```

> **⚠️ 语言文件特殊**：语言包**不自动监视**，改完必须按 **`F3 + T`** 或切换一次语言。
>
> **⚠️ 运行中的客户端读 `build/resources/main`**：贴图 / 模型改动前，
> 务必先在 IDE 执行 `Ctrl + F9`，否则改了也不生效。

---

## 4. 实体与环境调试 (Entities & Environment)

| 目的 | 命令 | 说明 |
| :- | :- | :- |
| 召唤冰原生物 | `/summon weather_realm:frost_sheep ~ ~ ~` | 另有 `frost_cow` / `frost_pig` / `frost_cat` |
| 带 NBT 召唤 | `/summon weather_realm:frost_cow ~ ~ ~ {NoAI:1b}` | 便于观察，防乱跑 |
| 清理所有非玩家实体 | `/kill @e[type=!player]` | 清场；慎用（会杀掉落物/动物） |
| 清理指定类型 | `/kill @e[type=weather_realm:frost_pig]` | 精准清理 |
| 固定晴天 | `/weather clear` | 可加时长，如 `/weather clear 1000000` |
| 设为白天 | `/time set noon` | 也可 `day` / `midnight` / `night` |
| 禁用昼夜更替 | `/gamerule doDaylightCycle false` | 保持固定时间便于观察光照 |
| 禁用天气更替 | `/gamerule doWeatherCycle false` | 天象之境不锁全局天气，靠客户端按群系渲染 |
| 死亡不掉落 | `/gamerule keepInventory true` | 联调利器 |
| 禁止生物破坏 | `/gamerule mobGriefing false` | 防苦力怕/末影人拆家 |
| 立即刷新随机刻 | `/gamerule randomTickSpeed 3` | 调高可加速作物/方块更新观察 |

```mcfunction
# 一键「测试环境初始化」宏（可粘进聊天框逐条执行）
/gamerule doDaylightCycle false
/gamerule doWeatherCycle false
/gamerule keepInventory true
/gamerule mobGriefing false
/weather clear
/time set noon
```

> **Tip**：`@e[type=!player]` 中的 `!` 是取反；组合条件可用
> `@e[type=weather_realm:frost_sheep,distance=..32]`。
>
> **⚠️ 双端隔离**：任何 `summon` / 实体逻辑都应在 `runServer` 下复测，
> 防止 Common 代码误引 `net.minecraft.client.*`（见第 7 节 `runServer`）。

---

## 5. 实用快捷键与 HUD (Dev Hotkeys)

| 快捷键 | 功能 | 用途 |
| :- | :- | :- |
| `F3 + B` | 显示/隐藏实体碰撞箱与视线朝向 | 校验实体尺寸、攻击判定、朝向 |
| `F3 + H` | 显示/隐藏高级物品提示 | 提示中显示物品 ID、耐久、**组件数** |
| `F3 + A` | 强制重载所有可见区块渲染 | 模型/贴图错位时强制刷新 |
| `F3 + F4` | 快速切换游戏模式 | 快速往返创造与旁观 |
| `F3 + G` | 显示区块边界 | 校验结构与区块对齐、贴图接缝 |
| `F3` | 调试信息面板（坐标/朝向/群系/光照等） | 通用排查 |
| `F3 + T` | 重载客户端资源（贴图/模型/语言） | 配合 IDE `Ctrl+F9` |
| `F3 + P` | 切换自动暂停 | 多客户端联调时避免后台暂停 |

> **Tip**：`F3 + H` 是验证「强类型组件是否真的写进 ItemStack」的最快可视化手段，
> 与第 2 节的 `/data get entity @s SelectedItem` 互为佐证。
>
> **联调**：多客户端 / 光影 / 渲染等 `runClient` 覆盖不到的场景，请走第 7 节的
> `deploy.ps1` 注入 PCL 客户端。

---

## 6. GameTest 自动化测试 (GameTest Framework)

| 目的 | 命令 | 说明 |
| :- | :- | :- |
| 运行全部用例 | `/test runall` | 需在测试世界 / 集成服务端；本地亦可 `runGameTestServer` |
| 运行指定用例 | `/test run <test_name>` | 例：`/test run weather_realm:<test_id>` |
| 清空测试产物 | `/test clearall` | 清除生成的测试建筑与实体 |
| 定位失败用例 | `/test runfailed` | 仅重跑上次失败的用例（便于快速迭代） |

```powershell
# 无头运行（推荐纳入 CI / 质量门禁）
.\gradlew.bat runGameTestServer
```

> **⚠️ 单元测试边界**：`src/test/java` 的 JUnit 5 **不加载** MC 引擎与注册表，
> 只能测纯算法；任何依赖 `net.minecraft.*` 的代码**只能**用 GameTest。
> 注：本仓库当前**没有** `src/test/java` 用例，`runGameTestServer` 暂无实际被测用例。
>
> **质量门禁**要求 GameTest 全绿（见 `AGENTS.md` 第 9 节）。

---

## 7. 编译与一键注入 (Build & Deploy)

| 场景 | 命令 | 说明 |
| :- | :- | :- |
| 一键构建并注入 PCL 客户端 | `.\deploy.ps1` | **推荐**；构建成功后自动复制 jar 到版本 mods 目录 |
| CMD / 双击运行 | `.\deploy.bat` | 与 `deploy.ps1` 等价的薄封装 |
| 源码原生运行客户端 | `.\gradlew.bat runClient` | 内置集成服务端；IDE HotSwap 联调首选 |
| 独立服务端验证 | `.\gradlew.bat runServer` | 复现 C-S 同步 / 权限 / 双端隔离问题 |
| 仅构建 jar | `.\gradlew.bat build` | 产物在 `build\libs\` |
| 执行 DataGen | `.\gradlew.bat runData` | 导出配方/模型/标签/语言到 `src/generated/resources/` |

### 7.1 一键构建与客户端注入工作流 (PCL 联调)

- 命令：`.\deploy.ps1`（PowerShell）或 `.\deploy.bat`（cmd 封装）。
- 流程：校验 / 设置 `JAVA_HOME` 为 JDK 21 → `.\gradlew.bat build`（失败立即中止并透传错误码）
  → 取 `build\libs\` 下最新可部署 jar（`weather_realm-1.21.1-1.0.0.jar`，排除 `-sources`/`-javadoc`/`-dev`）
  → **覆盖**复制到注入目标目录，并清理旧 `weather_realm-*.jar` 与遗留的 `examplemod-*.jar`。
- 注入目标：`C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\`（不存在时自动创建）。
- ⚠️ **部署前必须完全退出 Minecraft 客户端**：脚本会检测目标实例是否仍有客户端进程在运行，若在运行则**拒绝部署**（防热覆盖 jar 触发 `NoClassDefFoundError`）；确需强制时用 `-Force`（不建议）。
- 适用时机：用 **PCL 启动器**游玩联调、**多客户端**联机、**光影 / 渲染**等无法用 `runClient` 覆盖的场景。
- 注意：注入后需**完全重启客户端**；新增注册项不可热更。纯 Java 逻辑迭代仍优先走 IDE HotSwap。

```powershell
# 推荐流程（先完全退出客户端）
.\deploy.ps1

# 自定义注入目录（其他 PCL 版本 / 其他实例）
.\deploy.ps1 -TargetModsDir "D:\other\.minecraft\versions\X\mods"
```

> **⚠️ 环境前置**：脚本要求 JDK **21**。若 `JAVA_HOME` 非 21，
> 脚本会自动切换到 `C:\Program Files\Microsoft\jdk-21.0.7.6-hotspot`。

---

## 附：常见问题速查

| 现象 | 可能原因 | 处理 |
| :- | :- | :- |
| 改了配方不生效 | 未 `/reload` | 游戏内执行 `/reload` |
| 改了贴图/语言不生效 | 未 `Ctrl+F9` 构建，或未 `F3+T` | 先构建再 `F3+T` |
| 维度/群系找不到 | `/reload` 对动态注册表无效 | 退到主菜单重进存档 |
| `/locate biome` 找不到 | 群系未随维度加载 / 命名空间写错 | 确认 `weather_realm:crystal_plains` 已进 jar |
| `/build_portal` 不可见 | 权限不足（需 OP 2） | 提升权限 / 开启作弊 |
| 传送后卡方块/掉虚空 | 落点非安全地表 | 用 `spreadplayers ... under 256 false` |
| `deploy.ps1` 构建失败 | `JAVA_HOME` 非 JDK 21 | 脚本自动切换；仍失败请手动设 `JAVA_HOME` |
| `deploy.ps1` 拒绝部署 | 检测到客户端仍在运行 | 完全退出 Minecraft 后重跑（必要时 `-Force`） |
| 联机看不到 mod 内容 | 客户端/服务端 mod 版本不一致 | 两端使用同一 jar（`deploy.ps1` 注入） |
