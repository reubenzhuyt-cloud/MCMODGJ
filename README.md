# 天象之境 · Weather Realm

一个面向 **Minecraft 1.21.1 + NeoForge** 的天气主题冒险维度模组（Game Jam 项目）。玩家循着一位古代炼金术士的研究轨迹，进入自建维度 **天象之境**，在永冻、燃焰、风沙三重群系中勘探、采集，并（愿景中）逐步掌握天气本身。

- **modId**：`weather_realm`
- **中文名**：天象之境 · **英文名**：Weather Realm
- **当前版本**：`1.0.0`（开发中，见 [docs/DESIGN.md](docs/DESIGN.md) 的状态标注）

> 玩法设计愿景（含每项功能的实现状态）见 [docs/DESIGN.md](docs/DESIGN.md)；现状架构见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

---

## 版本矩阵（不可自行更改）

| 组件 | 锁定值 |
| :- | :- |
| Minecraft | **1.21.1** |
| 加载器 | **NeoForge 21.1.252**（`21.1.x` 段，勿写成 `1.21.1-21.1.252`） |
| JDK | **21** |
| Gradle | **8.10.2**（由 `gradlew.bat` wrapper 管理，勿手装） |
| 构建插件 | **ModDevGradle 2.0.147**（`net.neoforged.moddev`） |
| 映射 | **Parchment 2024.11.17** |

完整规约（代码风格、热重载边界、质量门禁、worldgen 红线）见 [AGENTS.md](AGENTS.md)。

---

## 快速开始

前置：**JDK 21**（Temurin 21 或 JBR 21）。Windows 下命令一律以 `.\gradlew.bat` 为前缀。

| 命令 | 用途 |
| :- | :- |
| `.\gradlew.bat build` | 构建 jar 到 `build/libs/` |
| `.\gradlew.bat runClient` | 启动开发客户端（内置集成服务端）；建议用 Debug 模式以获得 HotSwap |
| `.\gradlew.bat runServer` | 启动独立 Dedicated Server（验证双端隔离） |
| `.\gradlew.bat runGameTestServer` | 无头 GameTest 服务器（自动化门禁） |
| `.\gradlew.bat runData` | 执行 DataGen，输出到 `src/generated/resources/` |
| `.\deploy.ps1`（或 `.\deploy.bat`） | 一键构建并注入 PCL 客户端 mods 目录 |

### `.\deploy.ps1` 说明

- 校验/切换 `JAVA_HOME` 为 JDK 21 → 执行 `.\gradlew.bat build`（失败即中止）→ 将最新 `weather_realm-*.jar` 覆盖复制到 PCL 实例的 `mods\` 目录。
- ⚠️ **部署前必须完全退出 Minecraft 客户端**。脚本会检测目标实例是否仍有客户端进程在运行，若在运行则**拒绝部署**（避免热覆盖 jar 导致 `NoClassDefFoundError`）；确认已退出后重跑即可。仅供必要时使用的 `-Force` 参数可绕过该检查，**不建议**在有客户端运行时使用。
- 适用于 PCL 启动器联调、多客户端联机、光影/渲染等 `runClient` 覆盖不到的场景。注入后需重启客户端；**新增注册项不可热更**（见 `AGENTS.md` §6）。

---

## 目录结构速览

```
ModDevelopGamejam/
├── AGENTS.md                  # 项目级技术权威约定（版本/规约/门禁）
├── build.gradle / gradle.properties / settings.gradle
├── gradlew.bat, gradlew       # Gradle wrapper
├── deploy.ps1 / deploy.bat    # 一键构建 + 注入客户端 mods
├── docs/
│   ├── DESIGN.md              # 玩法设计愿景（现状状态标注）
│   ├── ARCHITECTURE.md        # 现状架构（实现层）
│   └── design/                # 专项技术设计（如 build-portal-command.md）
└── src/
    ├── main/
    │   ├── java/com/example/weather_realm/
    │   │   ├── WeatherRealm.java        # 主类 + 注册中枢
    │   │   ├── ModDimensions / ModTags / ModEntities / ModEntityEvents
    │   │   ├── ServerWeatherHandler.java
    │   │   ├── block/                   # 方块与方块实体（祭坛、传送门、木族、植被…）
    │   │   ├── entity/                  # 冰原羊/牛/猪/猫
    │   │   ├── item/                    # 晶石、手记、天象图、气候碎片
    │   │   ├── network/                 # 自定义网络载荷（天气切换）
    │   │   ├── portal/                  # 气候传送门点燃逻辑
    │   │   ├── command/                 # /build_portal 调试指令
    │   │   ├── world/                   # 表面规则、村庄结构、木质替换处理器
    │   │   ├── client/                  # 仅客户端：渲染/粒子/UI/地图（禁止被 Common 引用）
    │   │   └── mixin/                   # SurfaceSystemMixin（跳板模式）
    │   ├── resources/
    │   │   ├── assets/weather_realm/    # lang / models / textures / sounds / particles / patchouli
    │   │   └── data/weather_realm/      # dimension / worldgen / recipe / loot_table / tags
    │   └── templates/META-INF/neoforge.mods.toml   # 元数据模板（构建期替换变量）
    └── test/                            # 单元测试（不加载 MC 引擎）
```

---

## 文档索引

| 文档 | 内容 |
| :- | :- |
| [AGENTS.md](AGENTS.md) | 版本矩阵、代码规约、热重载边界、质量门禁、已知坑（**技术权威**） |
| [docs/DESIGN.md](docs/DESIGN.md) | 玩法设计愿景：核心概念、世界观、群系、天气、路线图、**逐项实现状态** |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 实现现状：类结构、注册项、数据流（由另一流程产出） |
| [docs/design/build-portal-command.md](docs/design/build-portal-command.md) | `/build_portal` 调试指令技术设计 |
| [TEST_COMMANDS.md](TEST_COMMANDS.md) | 游戏内调试/测试命令速查 |

---

## 许可与致谢

- **许可**：`All Rights Reserved`（见 `gradle.properties` 的 `mod_license`）。
- **模板来源**：本项目基于官方 [NeoForge MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)（MDK 1.21.1 · ModDevGradle）搭建。
- **依赖**：NeoForge；Mappings 使用 [Parchment](https://parchmentmc.org/)；可选集成 [Patchouli](https://modrinth.com/mod/patchouli)（未安装时自动降级，不崩溃）。
- 参考文档：[NeoForge Docs](https://docs.neoforged.net/) · [ModDevGradle](https://github.com/neoforged/ModDevGradle) · [Misode](https://misode.github.io/)。
