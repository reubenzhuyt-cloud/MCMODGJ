# 《天气冒险维度模组：极寒之境与天气掌控体系》GDD 设计规范

> **文档类型**:Game Design Document(GDD)+ 技术落地规范(合并本)
> **项目代号**:Glacial Realm / 极寒之境
> **版本**:v1.0.0(定稿)
> **日期**:2026-09-29
> **平台**:Minecraft **1.21.1** + **NeoForge 21.1.252** · JDK 21 · ModDevGradle 2.0.x · Parchment 2024.11.17
> **命名空间**:`examplemod`
> **发布名**:`Glacial Realm`(en_us) / `极寒之境`(zh_cn)
> **权威约束**:技术骨架以仓库根目录 [`AGENTS.md`](../../../AGENTS.md) 为准;若本文与 `AGENTS.md` 冲突,以 `AGENTS.md` 的技术约束为准(本文为创意层 + 落地细化层)。
> **关联文档**:[`mod.md`](../../../mod.md)(实现现状归档)· [`TEST_COMMANDS.md`](../../../TEST_COMMANDS.md)(测试命令)。

---

## 目录

- [0. 文档控制与阅读指引](#0-文档控制与阅读指引)
- [1. 模组概述与核心设计哲学](#1-模组概述与核心设计哲学-overview--core-pillars)
  - [1.1 项目定位](#11-项目定位)
  - [1.2 核心叙事理念:重走先哲之路](#12-核心叙事理念重走先哲之路-retracing-the-alchemists-path)
  - [1.3 玩法双轨驱动模型](#13-玩法双轨驱动模型)
  - [1.4 五大设计支柱](#14-五大设计支柱-five-pillars)
  - [1.5 首期范围与架构预留](#15-首期范围与架构预留)
- [2. 世界观设定与环境叙事详述](#2-世界观设定与环境叙事详述-worldview--narrative-deep-dive)
  - [2.1 纪元与地理设定](#21-纪元与地理设定)
  - [2.2 先哲生平与裂隙之灾](#22-先哲生平与裂隙之灾)
  - [2.3 极寒气象观测所编年史](#23-极寒气象观测所编年史)
  - [2.4 古籍文献碎片全文本](#24-古籍文献碎片全文本-patchouli-全书)
  - [2.5 环境叙事落地规范](#25-环境叙事落地规范-environmental-storytelling)
- [3. 第一阶段:入界闭环与村庄仪式](#3-第一阶段入界闭环与村庄仪式-overworld-progression--entry)
  - [3.1 村庄气象祭坛结构](#31-村庄气象祭坛结构-weather-altar)
  - [3.2 重构仪式与跨维度传送](#32-重构仪式与跨维度传送-reconstruction-ritual)
  - [3.3 仪式状态机](#33-仪式状态机-ritual-state-machine)
  - [3.4 入界闭环游戏循环](#34-入界闭环游戏循环)
- [4. 第二阶段:极寒天气双轨系统](#4-第二阶段极寒天气双轨系统-weather-dynamics--survival)
  - [4.1 严寒侵蚀与失温系统](#41-严寒侵蚀与失温系统-hypothermia-system)
  - [4.2 御寒装备梯队](#42-御寒装备梯队-thermal-progression)
  - [4.3 动态风暴潮周期](#43-动态风暴潮周期-dynamic-blizzard-cycles)
  - [4.4 风暴能量捕集与充能](#44-风暴能量捕集与充能-storm-energy-harvesting)
  - [4.5 第二阶段整体状态机](#45-第二阶段整体状态机)
- [5. 第三阶段:地牢探索与导流解密](#5-第三阶段地牢探索与导流解密-frost-observatory--conduits)
  - [5.1 观测所总体架构](#51-观测所总体架构-frost_observatory)
  - [5.2 模块化拼图房间与环境叙事](#52-模块化拼图房间与环境叙事)
  - [5.3 天气导流柱机关](#53-天气导流柱机关-weather-conduit-pylons)
  - [5.4 决战结界](#54-决战结界-glacier-barrier)
  - [5.5 地牢探索状态机与筹码经济](#55-地牢探索状态机与能量经济)
- [6. 第四阶段:决战极寒主宰与天气神器](#6-第四阶段决战极寒主宰与天气神器-boss-battle--weather-mastery)
  - [6.1 Boss 档案](#61-boss-档案极寒机枢泰坦-tempest-core-golem)
  - [6.2 战斗阶段机制](#62-战斗阶段机制)
  - [6.3 终极战利品与天气神器](#63-终极战利品与天气神器)
  - [6.4 天气掌控器运行规范](#64-天气掌控器-weather-harmonizer-运行规范)
- [7. 研发实施路线与技术落地规范](#7-研发实施路线与技术落地规范-implementation-roadmap)
  - [7.1 代码与资源组织规范](#71-代码与资源组织规范)
  - [7.2 四大里程碑垂直切片](#72-四大里程碑垂直切片-vertical-slices)
  - [7.3 网络与双端同步规范](#73-网络与双端同步规范)
  - [7.4 数据驱动与 DataGen 规范](#74-数据驱动与-datagen-规范)
  - [7.5 热重载适配规范](#75-热重载适配规范)
  - [7.6 质量门禁与验收清单](#76-质量门禁与验收清单)
- [附录 A:完整注册清单(Namespace Registry)](#附录-a完整注册清单namespace-registry)
- [附录 B:核心数值总表(Numeric Codex)](#附录-b核心数值总表numeric-codex)
- [附录 C:状态机与流程图总索引](#附录-c状态机与流程图总索引)
- [附录 D:本地化键规范](#附录-d本地化键规范)

---

## 0. 文档控制与阅读指引

### 0.1 术语表

| 术语 | 英文 / ID | 释义 |
| :- | :- | :- |
| 极寒之境 | `examplemod:crystal_realm` | 本模组的天气异维度,主舞台 |
| 水晶平原 | `examplemod:crystal_plains` | 极寒之境唯一基底群系(已实现) |
| 先哲 | The Alchemist | 数百年前发现并滥用天气晶石的炼金术士,全篇叙事核心 |
| 天气晶石 | `blizzard_crystal` | 极寒本源结晶,已实现材料 |
| 霜冻值 | `frostValue` | 失温系统标量,0–140,玩家级 |
| 暴风潮 | Blizzard Surge | 动态天气周期中的狂暴相位 |
| 凝霜平静期 | Calm Phase | 动态天气周期中的安全相位 |
| 充能风暴核 | `charged_storm_core` | 地牢与高阶合成的通用能源筹码 |
| 导流柱 | `conduit_pylon` | 地牢解密机关方块 |
| 结界 | `glacier_barrier` | Boss 门前四重寒冰封印 |
| 机枢泰坦 | `tempest_core_golem` | 终局 Boss |
| 天气掌控器 | `weather_harmonizer` | 通关后的自由度天气控制神器 |

### 0.2 阅读约定

- 本文中所有形如 `examplemod:<id>` 的标识均为**最终注册 ID**,命名规则见 [§7.1](#71-代码与资源组织规范)。
- 所有数值为**设计目标值**,实现时须经 `Config` 或数据包暴露以便调优(详见 `AGENTS.md` §5.2)。
- 状态机图使用 `状态 —(触发/条件)→ 状态` 记法;方框图为 ASCII 拓扑图。
- 本文**不含**任何未决事项;所有曾讨论的开放问题均已在此收敛为唯一结论。

---

## 1. 模组概述与核心设计哲学 (Overview & Core Pillars)

### 1.1 项目定位

- **性质**:Game Jam 旗舰企划 —— MC 1.21.1 + NeoForge 的**天气主题冒险维度模组**,交付一个"入界 → 生存 → 探索 → 决战 → 掌控"的完整可玩闭环。
- **对标体验**:暮色森林(Twilight Forest)式的**分区纵深推进**,叠加 **天气科技树**的能源循环。
- **核心差异化**:把"天灾"转译为"资源"——风暴既是致死威胁,也是唯一的高阶能源产地。
- **主舞台**:自建维度 `examplemod:crystal_realm`,以已落地的永久暴风雪渲染与冻土地质为地基。

### 1.2 核心叙事理念:重走先哲之路 (Retracing the Alchemist's Path)

玩家不是异界的过客,而是一名**沿古代炼金术士研发轨迹逆行的考古学者**:

```
主世界村庄祭坛(遗迹)  →  极寒之境(其流放地)  →  极寒气象观测所(其实验室)
        →  导流柱与结界(其安防)  →  机枢泰坦(其终极兵器)  →  天气掌控(其遗志)
```

每一个玩法模块都对应先哲研发史上的一个阶段:祭坛=初代原型,观测所=工程化,泰坦=神化巅峰,掌控器=玩家的继承与超越。

### 1.3 玩法双轨驱动模型

| 轨道 | 名称 | 承载内容 | 反馈周期 |
| :- | :- | :- | :- |
| **轨道 A** | 维度宏观气象机制 | 严寒失温生存 + 动态风暴潮周期 + 风暴能量捕集 | 分钟级(10 min 平静 / 3–4 min 风暴) |
| **轨道 B** | 区域地牢与试炼挑战 | 极寒气象观测所拼图地牢 + 导流柱解密 + 人造泰坦 Boss 战 | 小时级(单次通关 40–60 min) |

两轨**互相供能**:轨道 A 产出的 `charged_storm_core` 是轨道 B 的通行筹码;轨道 B 的 Boss 掉落 `glacial_storm_core` 又反哺轨道 A 的终极自由(天气掌控器)。

### 1.4 五大设计支柱 (Five Pillars)

| # | 支柱 | 定义 | 落地硬约束 |
| :- | :- | :- | :- |
| **P1** | **天灾即资源** | 每种环境威胁必须同时是唯一的生产资料 | 暴风潮产 `storm_shard`;失温逼出御寒装备链 |
| **P2** | **读环境即读关卡** | 环境既是氛围也是提示,零 UI 也能读懂状态 | 霜花暗角、风向粒子、导流柱光色均编码状态 |
| **P3** | **数据驱动优先** | 数值/生成/掉落/配方全部走 datapack + DataGen | 见 `AGENTS.md` §5.2;Java 内不写死数值 |
| **P4** | **复用原版状态** | 冰冻条、降水、光照、怪物生成优先复用 | 私有状态机仅作最薄封装(跳板模式) |
| **P5** | **垂直切片交付** | 每里程碑产出端到端可玩闭环 | 拒绝"半成品系统";每刀必须可演示 |

### 1.5 首期范围与架构预留

| 范围内(In,首期交付) | 范围外(Out,仅预留接口) |
| :- | :- |
| ❄️ 极寒之境单维度 + 暴风雪周期 + 失温 | 🌧️ 雷暴裂隙维度(下一章预告) |
| 🏛️ 村庄气象祭坛 + 手记古籍 + 入界仪式 | 🏜️ 焦阳风沙维度 |
| 🧊 观测所 Jigsaw 地牢 + 4 导流柱 + 结界 | 跨维度物流 / 天气网络 |
| 🤖 霜风怨灵 / 霜纹铁卫 / 机枢泰坦 | 多人协作 Boss 机制 |
| ⚔️ 暴风雪权杖 / 霜痕巨刃 / 天气掌控器 | 自研噪声 / 自研传送门 POI(`AGENTS.md` §7 禁令) |

---

## 2. 世界观设定与环境叙事详述 (Worldview & Narrative Deep Dive)

### 2.1 纪元与地理设定

- **时间线**:距今约三百年的"大旱纪元"。原版主世界经历了百年罕见的持续性干旱,作物绝收、河流枯竭、村庄南迁。
- **地理锚点**:旱灾重灾区位于北方雪原带边缘的定居点——正是现代主世界村庄的遗址所在。这解释了**村庄气象祭坛**为何生成于`平原村庄`与`雪原村庄`:前者是旱灾受害者,后者是炼金术士北迁后的落脚点。
- **气候异常**:大旱并非终局,而是后续"极寒之境"诞生的前兆——被强行扭曲的气象能量需要一个泄压口,这个泄压口就是被撕裂出的异次元空间。

### 2.2 先哲生平与裂隙之灾

**人物卡:先哲(无名炼金术士)**

| 维度 | 设定 |
| :- | :- |
| 身份 | 旱灾时期的天才气候炼金术士,村庄祭坛的建造者 |
| 动机 | 以炼金术操纵雨云,终结旱灾(善意的开端) |
| 成果 | 研制出初阶**天气晶石** `blizzard_crystal`,建立气象调谐祭坛 |
| 转折 | 超限催化仪式失控,气象反噬撕裂空间 |
| 结局 | 与祭坛一同被吸入暴风雪肆虐的异次元——即今"极寒之境" |
| 沉沦 | 在异界建立观测所,陷入"造神"狂热,以机械 + 永冻石躯壳封存本源天气之核 |
| 遗留 | 古籍五卷、四座导流柱、四重结界、终极兵器机枢泰坦 |

**裂隙之灾(Fracture)事件链**

1. **催化过载**:先哲在祭坛核心中投入超量晶石,试图一次性拉满降雨覆盖半径。
2. **气象反噬**:过量的天气能量无法被大地承载,在祭坛上空形成一个负压奇点。
3. **空间撕裂**:奇点以祭坛为中心撕开维度裂缝,暴风雪自裂缝内泄而出。
4. **吞噬**:祭坛、先哲连带周边地貌被整体拽入裂缝,主世界重归平静,但留下**密封石箱**深埋地表。
5. **异界重启**:先哲在极寒之境苏醒,意识到自己再也无法返回,转而开始"驯服此地气象"的第二次研发。

### 2.3 极寒气象观测所编年史

先哲在异界的三个研发纪元,直接对应地牢的三个空间层级:

| 纪元 | 时间 | 成果 | 地牢对应 | 叙事残片 |
| :- | :- | :- | :- | :- |
| **第一纪元·求生** | 入界后前 5 年 | 利用暴雪凝练出纯净晶核 | 地牢上层:荒废炼金工坊 | 《残页·其一:初遇晶核》 |
| **第二纪元·建所** | 5–20 年 | 建成极寒气象观测所,绘制维度气象图 | 地牢中层:极寒档案室 / 标本冷凝室 | 《残页·其二:裂隙吞噬》 |
| **第三纪元·造神** | 20 年后 | 以永冻土构装体 + 反应炉核心打造机枢泰坦 | 地牢深层:封印储藏室 / 反应炉核心大厅 | 《残页·其三:造神构装》 |

**观测所空间拓扑**

```
地表: 覆雪半塌的冰霜石塔(建筑入口 / 导流柱 #1 所在翼室)
  └─ 螺旋楼梯下沉 ────────────────┐
地下一层: 荒废炼金工坊 / 极寒档案室 / 导流柱 #2 翼室
下二层:   标本冷凝室 / 封印储藏室 / 导流柱 #3 翼室
下三层:   反应炉外环 / 冷凝风道 / 导流柱 #4 翼室
底层:     4 重寒冰结界(glacier_barrier) → 反应炉核心大厅(BOSS 殿堂)
```

### 2.4 古籍文献碎片全文本 (Patchouli 全书)

> 全部文本以 **Patchouli** 图鉴形式呈现(可选依赖:`patchouli`)。书籍标题:`《古代天气研究手记》`(`examplemod:ancient_weather_notes`)。残页由地牢宝箱产出,终章由 Boss 掉落。

#### 2.4.1 《古代天气研究手记·序章》

> 获取途径:破坏村庄气象祭坛核心或基座 → 掉落**密闭石箱** → 开启必得。

```
我写下这些字的时候，窗外的天空已经连续四百天没有落下一滴雨。

村里的井干了，河床裂成了龟壳， elders 说这是神明收回了恩赐。
我不信神明。我只信配平好的反应与可复现的结果。

我注意到一件奇怪的事：每当北方雪原的云飘过来，村里的空气就会变冷一瞬。
冷，是有质地的。既然冷可以由云带来，那么雨——为什么不能？

从此我开始了研究。我称第一块被我凝出的结晶为「天气晶石」。
它很冷，冷得让我想起那个还没有旱灾的冬天。

如果有一天我成功了，整片大地都会重新湿润。
如果有人读到这页，而我不在了，请替我看看雨。
                                                        —— 先哲 亲笔
```

#### 2.4.2 《残页·其一:初遇晶核》

> 获取途径:地牢上层「荒废炼金工坊」宝箱。

```
我不记得自己是怎么到这里来的。

醒来时，天地间只剩下一种颜色——白。雪没有停过，风没有停过。
我一度以为自己死了，这里大概是死后的世界。

但寒冷的疼痛告诉我：我还活着。而疼痛意味着物质还在，规律还在。
规律还在，就意味着一切还可以被研究。

我试着像在主世界那样呼出云气，却发现这里的空气根本不需要我催动，
它自己就是一场永不落幕的暴风雪。

我把随身残存的仪器架起来，对着风口站了整整一夜。
拂晓时分（如果这里也算有拂晓的话），仪器底部的托盘里，凝出了一枚从未见过的晶核。
它比主世界的天气晶石纯粹得多，像把一整场雪压进了一粒沙子里。

我给它取名叫「暴风雪结晶」。
我笑了。这是我这辈子第一次，在灾难面前笑出声来。
                                                        —— 残页·其一
```

#### 2.4.3 《残页·其二:裂隙吞噬》

> 获取途径:地牢中层「极寒档案室」手写笔记宝箱。

```
今天我必须记下那件事，趁我还记得。

催化仪式进行到第九个循环时，炉心的光变成了我不认识的颜色。
不是蓝，不是白，是一种会流动的、像伤口一样的东西。

我听见了风声——不是窗外的风，是从炉心内部传来的风。
那风在喊我的名字，用我母亲的声音。

我想撤下晶石，但我的手已经不听使唤了。整个祭坛在震颤，
天空像一块被拧干的布，拧出一个洞。

洞的另一边是什么，我到现在也没看清。我只记得自己被拽进去的那一瞬间，
回头看见了村子。它好小，小得像一枚即将被雪覆盖的脚印。

如果还能回去，我想对村里的人说声对不起。
                                                        —— 残页·其二
```

#### 2.4.4 《残页·其三:造神构装》

> 获取途径:地牢中层「标本冷凝室」解剖台暗格。

```
我花了二十年才想明白一件事：
人驯服不了天气，因为人太脆弱了。

我的手指在第三个冬天就冻掉了两根，我的肺每到风暴季就会结霜。
我建造了这座观测所，我绘制了整片维度的气象图，
可我本人，连走出塔门多站一刻都做不到。

所以我做出了决定：
既然血肉之躯承受不了本源的寒冷，那就造一具能承受的躯壳。

永冻土为骨，寒铁为筋，反应炉为核心。
再取一枚最纯的天气之核封入它的胸腔——
它将成为我的替身，替我站在风暴的中央。

我称它为「机枢」。
机枢不会冷，不会老，不会流泪。
机枢会替我，把这片天地永远地、彻底地握在手里。

愿它原谅我把它造成这个样子。
                                                        —— 残页·其三
```

#### 2.4.5 《手札·终章》

> 获取途径:击杀 `tempest_core_golem` 后必定掉落。

```
你打败了机枢。

我猜你一定很冷吧。我当年也是。
请允许我，对于一个能走到这里的人，说几句实话。

机枢并没有错，错的是我。
我把对寒冷的恐惧，铸成了一具只会服从命令的躯壳，
然后骗自己说，这就是"掌控"。
可掌控一个自己不敢面对的东西，从来都不是掌控。

我在机枢的胸腔里留下了一枚核心——极寒风暴之核。
它不冷不热，它只是"天气"本身。带它走吧，它属于愿意理解天气的人，而不是恐惧天气的人。

另外，我在维度壁上观测到了四道裂缝。
我封住了三道，但第四道，我还没来得及处理。
它通往的地方没有雪，只有无尽的雷与电。
我在地图上标了出来——如果你还想继续走下去的话。

这一次，记得带上朋友。
                                                        —— 老炼金术士·绝笔
```

### 2.5 环境叙事落地规范 (Environmental Storytelling)

| 叙事载体 | 落地形式 | 技术实现 |
| :- | :- | :- |
| 祭坛遗址 | 破损石制祭坛 + 深埋石箱 | 结构模板 `weather_altar` |
| 工坊残迹 | 蒸馏台(`brewing_stand`)+ 炼金残渣 + 破损器皿 | Jigsaw 模块 + 刷怪/战利品表 |
| 档案室 | 冰封书架 + 手写笔记宝箱 | 模块内 `chiseled_bookshelf` + 自定义箱子方法 `openNarrativeCrate` |
| 标本室 | 冰封极寒动植物标本(冻结的 `frost_sheep`/`frost_cow` 雕像) | 结构方块 `packed_ice` + 实体展示实体 |
| 反应炉大厅 | 冷凝风道(`condenser_vent`)+ 泄压管道(`pressure_pipe`) | 纯装饰方块 + Boss 机制触发源 |
| 维度壁裂缝 | 末章线索的视觉暗示 | 维度天空贴图 / 远景粒子(仅预告,不实现) |

---

## 3. 第一阶段:入界闭环与村庄仪式 (Overworld Progression & Entry)

> **目标**:玩家在主世界通过考古发现祭坛,完成重构仪式,撕裂裂隙进入极寒之境。全程约 30–50 分钟。

### 3.1 村庄气象祭坛结构 (Weather Altar)

**自然生成**

| 属性 | 值 |
| :- | :- |
| 结构 ID | `examplemod:weather_altar` |
| 生成生物群系 | `#minecraft:has_structure/village_plains`、`#minecraft:has_structure/village_snowy` |
| 注入方式 | `neoforge:add_structures` 型 BiomeModifier(不改写村庄本体,作为独立小品结构随机散布) |
| 结构类型 | `minecraft:jigsaw`(单池小品,`size=1`,`terrain_adaptation=beard_thin`) |
| 间距 / 分离 | `spacing=18`,`separation=6`,`salt=8273641` |
| 地表锚定 | `project_start_to_heightmap=WORLD_SURFACE_WG`,`start_height.absolute=0` |

**构成方块(模组自创)**

| 注册 ID | 中文名 | 硬度 / 抗爆 | 说明 |
| :- | :- | :- | :- |
| `examplemod:weather_stone` | 天气石 | 1.5F / 6.0F | 祭坛建材基材(青色纹路石) |
| `examplemod:chiseled_weather_stone` | 雕纹天气石 | 1.5F / 6.0F | 祭坛墙面/柱身雕饰 |
| `examplemod:weather_altar_core` | 失控天气核心 | 3.0F / 6.0F | 仪式中枢,钩子方块 |
| `examplemod:weather_pedestal` | 调谐基座 | 2.0F / 6.0F | 四角能量介质承载座 |
| `examplemod:sealed_weather_crate` | 密闭石箱 | — | **物品形态**,开箱产出 |

> 贴图规范:基于原版 `stone` / `chiseled_stone_bricks` / `deepslate_tiles` 重着色精修,统一色相 `#5FA8C7`(冷青),在 `assets/examplemod/textures/block/` 下产出。

**破坏机制**

- 挖掘 `weather_altar_core` 或 `weather_pedestal` 时,若**仪式未激活**,掉落 1 个 `examplemod:sealed_weather_crate`。
- `sealed_weather_crate` 右键开启,必得:
  - 1 × `examplemod:ancient_weather_notes`(《古代天气研究手记》,Patchouli 书);
  - 1 × `examplemod:discordant_crystal_shard`(失调晶核残片)。
- 若仪式已激活,方块破坏掉落本体(允许玩家回收建材)。

### 3.2 重构仪式与跨维度传送 (Reconstruction Ritual)

**仪式结构规格**

```
         [P]              [P]            P = weather_pedestal(调谐基座)
    ┌───────────┐                     [C] = weather_altar_core(失控天气核心)
    │  S  S  S  │                     S  = weather_stone / chiseled_weather_stone
    │  S [C] S  │                     [P] = 基座上放置调谐介质
    │  S  S  S  │
    └───────────┘
   3×3 天气石基台,高 1 格;四角外侧各 1 座基座,
   基座正上方放置 1 个调谐介质(浮冰 / 雪块 / 铜棒 任选其一)。
```

| 步骤 | 条件 | 结果 |
| :- | :- | :- |
| S1 结构判定 | 3×3 石基台完整 + 中央 `weather_altar_core` + 四角 `weather_pedestal` 各含介质 | 祭坛进入 `PRIMED` |
| S2 催化投放 | 中央核心投入 1 × `minecraft:blue_ice` 或 1 × `examplemod:blizzard_crystal` | 核心充能,粒子预演 |
| S3 手记激活 | 手持 `ancient_weather_notes` **右键**核心 | 触发仪式演出 → 开启裂隙 |
| S4 传送 | 玩家踏入裂隙 3×3×4 区域 | `teleportTo(crystal_realm, safeX, safeY, safeZ, yRot, xRot)` |

**视觉与音效演出(时长 4.0 秒 / 80 ticks)**

| Tick | 演出 |
| :- | :- |
| 0 | 播放 `examplemod:weather.altar_awaken`;天空色度以 0.05/tick 向 `#2B3A55` 转暗 |
| 0–60 | 四周生成 `examplemod:frost_vortex` 粒子(白 + 青),以核心为中心半径 2→5 螺旋升腾 |
| 40 | 播放 `examplemod:weather.blizzard_roar`(低沉风暴咆哮,音量 1.0,衰减 24) |
| 60–80 | 核心上方撕裂 3×3×4 的旋转传送通道(自定义 `weather_rift` 方块,渲染为半透明旋转冰纹) |
| 80 | 传送区域就绪,粒子转为持续循环直到玩家进入 |

**安全落点算法**

```java
// 逻辑外置于 WeatherRitualManager(普通类,可 HotSwap);Mixin 只做跳转
BlockPos findSafeLanding(ServerLevel realm, BlockPos origin) {
    // 1. 以 origin 为中心,在半径 32 内搜索 WORLD_SURFACE 高度
    // 2. 拒绝液面(y 处为 fluid)与 powder_snow 落点
    // 3. 从地表向上找第一个 2 格净空
    // 4. 兜底:origin.x, getHeight(MOTION_BLOCKING)+1, origin.z
}
```

> 指令兜底:`/execute in examplemod:crystal_realm run spreadplayers 0 0 10 50 under 256 false @s`

### 3.3 仪式状态机 (Ritual State Machine)

```
        ┌──────────┐  结构不完整
        │  BROKEN  │◀─────────────┐
        └────┬─────┘              │
             │ 结构完整             │ 任一方块被破坏
             ▼                     │
        ┌──────────┐  催化物投入     │
        │  PRIMED  │──────────────▶ ┌──────────┐
        └────┬─────┘   (blue_ice / │ CHARGING │
             │        blizzard_    └────┬─────┘
             │             crystal)     │ 核心已充能
             │                          ▼
             │                     ┌──────────┐  手持手记右键
             └────────────────────▶│  READY   │──────────────▶ ┌──────────────┐
                      已充能        └──────────┘   演出 80t     │ RIFT_OPEN    │
                                                              └──────┬───────┘
                                                                     │ 玩家踏入裂隙
                                                                     ▼
                                                              ┌──────────────┐
                                                              │  TELEPORTED  │
                                                              └──────────────┘
```

- **权威端**:服务端。状态存于 `WeatherAltarCoreBlockEntity`(若坚持纯数据可退化为方块状态 + 邻域扫描,推荐 BE)。
- **幂等性**:`TELEPORTED` 后裂隙保留 10 分钟供组队成员使用,随后回到 `BROKEN`。

### 3.4 入界闭环游戏循环

```
┌─────────────────────────────────────────────────────────────────────┐
│                       第一阶段:入界闭环                                │
│                                                                     │
│   探索村庄 ──▶ 发现祭坛 ──▶ 挖掘石箱 ──▶ 获得手记 & 残片              │
│       ▲                                                    │        │
│       │                                                    ▼        │
│   收集中    ◀── 清点建材 ◀── 采集天气石/介质 ◀── 阅读手记(引导)      │
│       │                                                    │        │
│       ▼                                                    ▼        │
│   重构祭坛 ◀──────── 搭建 3×3 基台 + 四角基座 ◀────────────┘        │
│       │                                                             │
│       ▼                                                             │
│   投放催化 ──▶ 手记激活 ──▶ 裂隙撕开 ──▶ 【进入极寒之境】             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. 第二阶段:极寒天气双轨系统 (Weather Dynamics & Survival)

> **目标**:建立"失温压力"与"风暴潮节奏"两大宏观系统,并产出 `charged_storm_core` 能源循环。全程约 60–90 分钟。

### 4.1 严寒侵蚀与失温系统 (Hypothermia System)

**核心参数**

| 参数 | 值 | 说明 |
| :- | :- | :- |
| 霜冻值上限 `MAX_FROST` | **140.0** | 满值进入冻结状态 |
| 基础累积速率 | **+5.0 / 秒**(= 0.25 / tick) | 露天 + 无热源时 |
| 暴风潮倍率 | **×2.0** | 暴风潮期间累积翻倍(= +10.0 / 秒) |
| 抗寒减免 | `accumulation × (1 − frostResistance)` | `frostResistance` ∈ [0,1] |
| 满值冻伤 | **1.5 点 / 每 1.5 秒** | 真实伤害,`bypasses_armor` |
| 冻伤伤害类型 | `examplemod:frostbite` | `data/damage_type/frostbite.json` |

**触发条件(必须同时满足)**

1. 玩家位于 `examplemod:crystal_realm`;
2. `!player.level().canSeeSky(player.blockPosition())` 为 **false**(即露天);
3. 周围 8 格内**无有效热源**。

**失温分层与视觉/机制反馈**

| 霜冻值区间 | 状态名 | HUD 表现 | 机制效果 |
| :- | :- | :- | :- |
| 0 – 39 | `WARM`(常态) | 无 | 无 |
| 40 – 79 | `CHILLED`(轻霜) | 屏幕边缘淡蓝霜晕 | 移动速度 **−10%** |
| 80 – 119 | `MODERATE`(中度失温) | 霜晕加深 + **视角轻微颤抖**(±0.6° 随机) | 移动速度 **−20%**、挖掘速度 **−15%** |
| 120 – 139 | `SEVERE`(重度失温) | 屏幕四角结晶暗角 + 视野收缩(FOV −6) | 移动速度 **−30%**、挖掘速度 **−25%**、攻击力 **−20%** |
| 140(满值) | `FROZEN`(冻结) | **心形血条替换为冰晶冻结图标** + 全屏霜冻覆盖 | 每 1.5 s 扣除 **1.5** 点 `frostbite` 真实伤害;移动速度 **−40%** |

**驱寒与热源结算(每秒)**

| 热源 | 作用半径 | 消退速率 | 备注 |
| :- | :- | :- | :- |
| 火把 / 灯笼 / 灵魂营火 | 4 格 | **−8 / 秒** | 暴风潮期间效果 **×0.5** |
| 营火 / 燃烧的熔炉 | 6 格 | **−20 / 秒** | 同上 |
| 岩浆 / 岩浆湖 | 8 格 | **−30 / 秒** | 同上 |
| 有遮蔽(`canSeeSky == false`) | 自身 | **−6 / 秒** | 停止累积并缓慢回暖 |

> **优先级**:同一 tick 内取**最强热源**结算,不叠加,防止站岩浆边瞬清。

**实现规范(跳板模式)**

```java
// Mixin 只做一行委托(见 AGENTS.md §5.1)
@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void examplemod$tickHook(CallbackInfo ci) {
        HypothermiaManager.tick((Player)(Object)this);
    }
}
```

- 服务端权威:累积、扣血、速度修饰全部在 `HypothermiaManager`(普通类,可 HotSwap)。
- 同步:每 10 ticks 通过自定义 `CustomPacketPayload`(`FrostStatePayload`)下发霜冻值给该玩家客户端,供 HUD 渲染。
- 复刻原版:`HypothermiaManager` 同时调用 `player.setTicksFrozen(计算值)`,让原版发抖动画与 `renderFrozen` 生效。

### 4.2 御寒装备梯队 (Thermal Progression)

新增自定义属性 **`examplemod:frost_resistance`**(范围 0.0–1.0,默认 0.0,`RangedAttribute`)。

#### T1 霜绒御寒套 (Frost Wool Armor)

| 项 | 值 |
| :- | :- |
| 材料 | `examplemod:frost_wool`(由 `frost_sheep` 剪毛获得,已实现) |
| 部件 | `frost_wool_helmet` / `chestplate` / `leggings` / `boots` |
| 护甲值 | 头 1 / 胸 3 / 腿 2 / 靴 1(共 7) |
| 韧性 | 0.0 |
| 霜冻抗性(全套) | **0.70**(减缓 70% 霜冻积累) |
| 单件抗性 | 头盔 0.10 / 胸甲 0.25 / 护腿 0.20 / 靴 0.15 |
| 耐久系数 | 7(羊毛档,易损耗) |
| 装备音效 | `minecraft:item.armor.equip_wool` |

#### T2 冰晶装甲全套 (Blizzard Crystal Armor)

| 项 | 值 |
| :- | :- |
| 材料 | `examplemod:blizzard_crystal`(已实现) |
| 部件 | `blizzard_crystal_helmet` / `chestplate` / `leggings` / `boots`(已实现) |
| 护甲值 | 头 3 / 胸 8 / 腿 6 / 靴 3(共 20,已实现) |
| 霜冻抗性(全套) | **1.00**(绝对极寒免疫,累积为 0) |
| 单件抗性 | 头盔 0.20 / 胸甲 0.35 / 护腿 0.25 / 靴 0.20 |
| **机动加成** | 全套且脚下为 `snow_block` / `powder_snow` / `permafrost` / `packed_ice` / `blue_ice` 时,**移动速度 +15%** |
| 韧性 / 击退抗性 | 2.5F / 0.1F(已实现) |

> **设计意图**:T1 是"能在外面待着"的通行证;T2 是"能在风暴里作业"的自由。机动加成鼓励 T2 玩家在雪原高速勘探,强化"天气即地形"的体验。

**获取路径**

```
frost_sheep(冰原羊) ──剪毛──▶ frost_wool ──▶ T1 霜绒套
                                     │
blizzard_crystal(矿石/怨灵掉落) ──────┼──▶ T2 冰晶套
                                     │
storm_shard(风暴怨灵掉落) ───────────┘(用于 T2 强化配方)
```

### 4.3 动态风暴潮周期 (Dynamic Blizzard Cycles)

**全局调度器**:`BlizzardCycleManager`(服务端单例,绑在 `crystal_realm` 的 `ServerLevel` 上,随存档持久化)。

**周期参数**

| 相位 | 时长 | 天气表现 |
| :- | :- | :- |
| `CALM` 凝霜平静期 | **12000 ticks**(10 分钟) | 微雪轻拂,能见度正常,降水维持轻微雪 |
| `RAMP_UP` 风力攀升 | **300 ticks**(15 秒) | 风力渐强,粒子密度线性上升,天空转暗 |
| `SURGE` 呼啸风暴潮 | **3600–4800 ticks**(3–4 分钟,随机) | 狂暴暴风雪 |
| `RAMP_DOWN` 风势回落 | **600 ticks**(30 秒) | 风力衰减,天空回亮 |

> 单周期平均 ≈ 12800 + 300 + 4200 + 600 = **17900 ticks ≈ 14.9 分钟**。

**SURGE 相位环境效果**

| 效果 | 数值 |
| :- | :- |
| 光照骤降 | 环境光附加 −0.35(与维度 `ambient_light=0.2` 叠加,近似黄昏) |
| 暴风雪粒子 | `examplemod:blizzard_snow` 密度 ×3.0(相对平静期) |
| 迎面风力 | 对露天实体施加固定风向推力,向量长度 **0.04 格/tick**;逆风冲刺额外消耗疲劳 |
| 能见度 | 雾距收缩至 **8 格**(客户端 `FogRenderer` 覆写) |
| 失温倍率 | 累积速率 **×2.0**(见 §4.1) |
| 敌对刷新 | `blizzard_wraith` 露天刷新权重 20,每组 1–2 只 |

**霜风怨灵 (Blizzard Wraith)** —— `examplemod:blizzard_wraith`

| 属性 | 值 |
| :- | :- |
| 分类 | `MONSTER`,飞行(`FlyingMob`) |
| 体型 | 0.7 × 1.4 |
| 生命值 | **30**(15 心) |
| 攻击力 | **6.0** |
| 移动速度 | 0.30 |
| 护甲 | 2.0 |
| 击退抗性 | 0.4 |
| 攻击方式 | **冲刺撕裂**:前摇 0.8 s(身体后仰 + 音效 `examplemod:entity.blizzard_wraith.charge`)→ 向玩家直线冲刺,命中造成 6 点伤害 + 冻伤冻值 +8 |
| 免疫 | 摔落 / 冰冻 |
| 掉落 | `storm_shard` 1–2(100%)、`blizzard_crystal` 0–1(35%) |
| 克制关系 | 被「暴风雪权杖」标记后转为**中立**(不再主动索敌) |

**风力向量规范**

- 每个 `SURGE` 周期开始时,由 `BlizzardCycleManager` 用世界种子 + 周期序号确定一个固定风向 `θ`(八方位之一),全维度一致。
- 推力 = `(cos θ, 0, sin θ) × 0.04` 每 tick 施加给 `!onGround` 或露天的 `LivingEntity`。
- 结算在服务端,客户端只做粒子飘移表现(与已有 `BlizzardSnowParticle` 的 +X 漂移一致,方向改为动态)。

### 4.4 风暴能量捕集与充能 (Storm Energy Harvesting)

**能源阶梯**

```
storm_shard(狂暴风暴碎片,怨灵掉落)
        └─▶ 合成 dormant_storm_core(休眠风暴晶石)
                    └─▶ 暴风潮中充能 ──▶ charged_storm_core(充能风暴核)★ 地牢通用筹码
```

**关键道具**

| 注册 ID | 中文名 | 类型 | 功能 |
| :- | :- | :- | :- |
| `examplemod:dormant_storm_core` | 休眠风暴晶石 | 物品 | 充能载体,初始能量 0 |
| `examplemod:charged_storm_core` | 充能风暴核 | 物品 | 能量满值产物,地牢机关燃料 |
| `examplemod:weather_condenser` | 天气引风瓶 | 物品 | 手持充能(便携,×1.0 速率) |
| `examplemod:storm_relay` | 风暴引风基座 | 方块 | 户外高台部署(×3.0 速率) |

**充能规则**

| 条件 | 速率(能量单位/秒) |
| :- | :- |
| `SURGE` 期间,玩家手持 `dormant_storm_core` 且露天 | **+1.0 / 秒** |
| 上者 + 背包/副手持有 `weather_condenser` | **+2.0 / 秒**(引风瓶增幅) |
| `dormant_storm_core` 被放入 `storm_relay`,且基座露天且 `y ≥ 160` | **+3.0 / 秒** |
| `CALM` 期间(任意方式) | **−0.5 / 秒**(自然泄能,下限 0) |
| 单个晶石能量上限 | **180** |

> **节奏推导**:纯手持需 3 分钟(≈ 一次完整风暴潮)充满 1 枚;引风瓶缩短至 1.5 分钟;引风基座 1 分钟。地牢通关需 4 枚,恰好多轮风暴潮。

**能量数据存储**

- 使用 **DataComponent**:`examplemod:storm_charge`(`DataComponentType<Integer>`,0–180),挂在 `dormant_storm_core` / `charged_storm_core` 上。
- 达 180 时,物品在服务端**原地转化**为 `charged_storm_core`(保留组件值),并播放 `examplemod:item.storm_core_charged` + 紫色闪电粒子。
- `storm_relay` 为 `BlockEntity`,内部持有一枚晶石的组件数据;破坏时完整掉落(含能量)。

### 4.5 第二阶段整体状态机

**风暴周期状态机**

```
   ┌────────┐  计时 12000t 满          ┌──────────┐  300t 满
   │  CALM  │─────────────────────────▶│ RAMP_UP  │──────────┐
   └───▲────┘                          └──────────┘          │
       │                                                     ▼
       │ 600t 满                                    ┌────────────────┐
   ┌───────────┐                                    │     SURGE      │
   │ RAMP_DOWN │◀───────────────────────────────────│ 3600–4800t(随) │
   └───────────┘                                    └────────────────┘
```

**玩家失温状态机(可逆,带滞回)**

```
  WARM ──(f≥40)──▶ CHILLED ──(f≥80)──▶ MODERATE ──(f≥120)──▶ SEVERE ──(f=140)──▶ FROZEN
    ▲                │                  │                    │                    │
    └────────────────┴──────────────────┴────────────────────┘                    │
              f 低于上一档阈值 −10(滞回)时降级 ◀─────────────────────────────────┘
```

- **滞回带** `10 点`:避免在阈值附近反复抖屏/抖 FOV。
- `FROZEN` 一旦触及,即便回暖也只降到 `SEVERE`,直到 `f ≤ 120` 才继续下降(防"擦线逃脱"的廉价操作)。

**能源闭环**

```
   [SURGE] ──▶ 怨灵刷新 ──▶ storm_shard ──┐
                                          ├──▶ dormant_storm_core ──▶ 充能 ──▶ charged_storm_core
   [SURGE] ──▶ 露天充能(瓶/基座) ────────┘                                          │
                                                                                    ▼
                                                          【第三阶段:导流柱 ×4 消耗】
```

---

## 5. 第三阶段:地牢探索与导流解密 (Frost Observatory & Conduits)

> **目标**:以 Jigsaw 拼图地牢承载叙事探索,以导流柱解密串联战斗节奏,以结界收束进入 Boss 战。全程约 40–60 分钟。

### 5.1 观测所总体架构 (`frost_observatory`)

| 属性 | 值 |
| :- | :- |
| 结构 ID | `examplemod:frost_observatory` |
| 生成位置 | `examplemod:crystal_plains` 群系,地表 |
| 结构类型 | `minecraft:jigsaw`,`size=20`,`max_distance_from_center=128` |
| 起始高度 | `WORLD_SURFACE_WG` |
| 地表适应 | `terrain_adaptation=beard_thin` |
| 稀疏度 | `spacing=32`,`separation=8`,`salt=5591042` |
| 起始池 | `examplemod:frost_observatory/tower_base` |

**建筑外观**:覆雪半塌的冰霜石塔,塔顶塌陷,螺旋楼梯自塔心深入地下。地牢纵向跨度约 y=64(地表)至 y=−48(底层殿堂)。

**新增方块**

| 注册 ID | 中文名 | 硬度 / 抗爆 | 说明 |
| :- | :- | :- | :- |
| `examplemod:frost_stone` | 霜纹石 | 2.25F / 6.0F | 地牢基材 |
| `examplemod:frost_stone_bricks` | 霜纹石砖 | 2.25F / 6.0F | 主建材 |
| `examplemod:cracked_frost_stone_bricks` | 开裂霜纹石砖 | 2.25F / 6.0F | 半塌视觉 |
| `examplemod:chiseled_frost_stone` | 雕纹霜纹石 | 2.25F / 6.0F | 叙事浮雕 |
| `examplemod:frost_iron_bars` | 极寒铁栏杆 | 5.0F / 6.0F | 窗户/牢笼 |
| `examplemod:conduit_pylon` | 天气导流柱 | 4.5F / 12.0F | 机关方块(BE) |
| `examplemod:glacier_barrier` | 决战结界 | 不可破坏 | 4 重封印(BE) |
| `examplemod:condenser_vent` | 冷凝风道 | 3.5F / 6.0F | 装饰 + Boss 机制源 |
| `examplemod:pressure_pipe` | 泄压管道 | 3.5F / 6.0F | 装饰 |
| `examplemod:reactor_core` | 反应炉核心 | 不可破坏 | Boss 殿堂中枢(机制) |

### 5.2 模块化拼图房间与环境叙事

**Jigsaw 池结构**

```
frost_observatory/
├── tower_base            (地表塔基,含导流柱#1 翼室)
├── spiral_stair          (螺旋楼梯 ×2,纵向连接)
├── workshop              (荒废炼金工坊)
├── archive               (极寒档案室)
├── specimen_chamber      (标本冷凝室)
├── sealed_vault          (封印储藏室)
├── conduit_wing_a/b/c    (导流柱 #2/#3/#4 翼室)
├── reactor_ring          (反应炉外环)
└── core_hall             (反应炉核心大厅,Boss 殿堂)
```

**叙事房间规格**

| 房间 | 关键陈设 | 交互 | 产出 |
| :- | :- | :- | :- |
| **荒废炼金工坊** | 蒸馏台(`brewing_stand`)、破损水晶器皿(`blizzard_crystal_block` 半砖)、炼金残渣(`dormant_storm_core` 展示) | 砸碎器皿 | `discordant_crystal_shard` 1–2、工坊宝箱(残页其一) |
| **极寒档案室** | 冰封书架(`chiseled_bookshelf`)、手写笔记宝箱 | 开箱 | **《残页·其二:裂隙吞噬》**、`blizzard_crystal` 2–4 |
| **标本冷凝室** | 冰封 `frost_sheep`/`frost_cow` 标本、解剖台 | 解剖台暗格 | **《残页·其三:造神构装》**、`frost_leather` 2–3 |
| **封印储藏室** | 古代宝箱(需 `conduit_key` 或 1 × `charged_storm_core`) | 解锁 | `charged_storm_core` 1–2、`storm_shard` 3–6、附魔金苹果 0–1 |

> **文物一致律**:每个叙事房间**必掉**对应文献残页,保证剧情可收集性;文献在 Patchouli 中解锁对应章节。

### 5.3 天气导流柱机关 (Weather Conduit Pylons)

**分布**:全地牢 **4 座**,分别位于塔基翼室(#1)、一层(#2)、二层(#3)、三层外环(#4),每座处于独立翼室。

**交互闭环(严格四步)**

```
① DORMANT(休眠冻结)  导流柱表面覆冰,冰蓝光熄灭,基座空置
        │ 玩家消耗 1 × charged_storm_core 嵌入基座
        ▼
② ACTIVATING(升能)    导流柱从底部向上亮起旋转冰蓝光束(1.5s)
        │ 升能完成瞬间
        ▼
③ CONTESTED(激战)     惊醒 2–3 只 frost_automaton(霜纹铁卫),必须全歼
        │ 场上铁卫清零
        ▼
④ LIT(点亮)           导流柱稳定,向深处射出一道贯穿通道的能量光束,
                       全地牢回响 examplemod:weather.conduit_lock
```

**霜纹铁卫 (Frost Automaton)** —— `examplemod:frost_automaton`

| 属性 | 值 |
| :- | :- |
| 分类 | `MONSTER`,地面 |
| 体型 | 0.85 × 1.9 |
| 生命值 | **40**(20 心) |
| 攻击力 | **8.0** |
| 移动速度 | 0.25 |
| 护甲 | 8.0 |
| 击退抗性 | 0.6 |
| 攻击方式 | 重锤横扫(前方 180° 扇形,2.0 格) |
| 弱点 | 被 `frostcleaver` 命中时护甲临时归零 2 秒 |
| 掉落 | `conduit_key` 1(每座导流柱由最后一只铁卫必掉)、`storm_shard` 1–2、`blizzard_crystal` 0–2 |
| 免疫 | 摔落、冰冻、击退(高韧性) |

**导流柱状态机**

```
  ┌─────────┐  嵌入 charged_storm_core     ┌────────────┐  1.5s 满
  │ DORMANT │─────────────────────────────▶│ ACTIVATING │──────────┐
  └─────────┘                              └────────────┘          │
       ▲                                                          ▼
       │ 玩家未嵌入                                    ┌──────────────────┐
       │                                              │    CONTESTED     │
       │                                              │ spawn 2–3 铁卫   │
       │                                              └────────┬─────────┘
       │                                                       │ 场上铁卫 == 0
       │                                                       ▼
       │                                              ┌──────────────────┐
       └──────────────────────────────────────────────│       LIT        │
                     (不可逆,永久点亮)                  └──────────────────┘
```

- 每座被点亮的导流柱向 `glacier_barrier` 发出一条逻辑信号(`level.getBlockEntity` 直连或 `BarrierManager` 计数)。
- 光束渲染:客户端在导流柱与结界之间绘制半透明青色光束(`RenderType.translucent`),仅在该柱 `LIT` 且玩家处于同层可见时渲染。

### 5.4 决战结界 (Glacier Barrier)

| 属性 | 值 |
| :- | :- |
| 注册 ID | `examplemod:glacier_barrier` |
| 层数 | **4 重**(对应 4 座导流柱) |
| 位置 | 地牢底层,`core_hall` 入口 |
| 初始状态 | 4 层全部凝固,冰霜符文熄灭,不可通行 |
| 点亮规则 | 每点亮 1 座导流柱,对应序号的符文层绽放强光并半透明化 |
| 碎裂条件 | 4 层全部点亮 → 结界整体碎裂消失,大门洞开 |

**符文层对应关系**

| 结界层 | 联动导流柱 | 符文颜色 | 音效 |
| :- | :- | :- | :- |
| 第 1 层 | #1(塔基翼室) | 淡青 `#8FD8F0` | `examplemod:weather.barrier_seal_1` |
| 第 2 层 | #2(一层) | 冰蓝 `#5FB8E8` | `examplemod:weather.barrier_seal_2` |
| 第 3 层 | #3(二层) | 深蓝 `#3A8FD0` | `examplemod:weather.barrier_seal_3` |
| 第 4 层 | #4(三层外环) | 靛紫 `#5A6FD0` | `examplemod:weather.barrier_seal_4` |
| 全亮 | — | 白光过曝 + 碎裂 | `examplemod:weather.barrier_shatter` |

### 5.5 地牢探索状态机与能量经济

**地牢通关状态机(玩家视角)**

```
  进入观测所
      │
      ▼
  ① 探索叙事房间 ──▶ 收集文献 & 资源
      │
      ▼
  ② 寻找导流柱 #1 ──▶ 嵌入 charged_storm_core ──▶ 清剿铁卫 ──▶ 符文 1 亮
      │                    ▲
      │                    │ 需回地表/上层继续充能(风暴潮周期)
      ▼                    │
  ③ 重复 #2 / #3 / #4 ─────┘  (每座之间穿插充能与探索)
      │
      ▼
  ④ 结界碎裂 ──▶ 进入反应炉核心大厅 ──▶ 【Boss 战】
```

**能量经济守恒表**

| 筹码 | 产出 | 消耗 | 单次通关需求量 |
| :- | :- | :- | :- |
| `charged_storm_core` | 风暴潮充能 | 导流柱 ×4、封印储藏室 0–1 | **4–5** |
| `conduit_key` | 铁卫必掉(每柱 1) | 封印储藏室开锁 | 1–2 |
| `storm_shard` | 怨灵 / 铁卫掉落 | 合成 `dormant_storm_core`、神器合成 | 6–10 |
| `discordant_crystal_shard` | 祭坛石箱 / 工坊 | 合成 `dormant_storm_core` | 1–2 |

> **节奏设计**:4 座导流柱之间必须间隔至少一次完整风暴潮充能,天然把地牢探索切成 4 段,避免一次性长跑疲劳。

---

## 6. 第四阶段:决战极寒主宰与天气神器 (Boss Battle & Weather Mastery)

> **目标**:以两阶段 Boss 战收束全部剧情线,交付天气神器与天气掌控器,完成从"求生者"到"气象之主"的玩家身份跃迁。

### 6.1 Boss 档案:极寒机枢·泰坦 (Tempest Core Golem)

| 属性 | 值 |
| :- | :- |
| 注册 ID | `examplemod:tempest_core_golem` |
| 中文名 | 极寒机枢·泰坦 |
| 分类 | `MONSTER`,地面构装体 |
| 体型 | 3.0 × 5.2 |
| 生命值 | **400**(200 心) |
| 护甲 | **12.0** |
| 护甲韧性 | 8.0 |
| 击退抗性 | **1.0**(完全免疫击退) |
| 移动速度 | 0.22 |
| 跟随半径 | 64 格 |
| 免疫 | 冰冻、摔落、火焰、击退、溺水 |
| Boss 血条 | 显示,标题 `entity.examplemod.tempest_core_golem`,颜色 `#5FB8E8` |
| 决战舞台 | `core_hall` 反应炉核心大厅(半径约 24×24,四周为 `condenser_vent` / `pressure_pipe`) |

**设定**:老炼金术士以永冻土构装体与反应炉核心打造的人造天候终极神化实体,胸腔内封存本源天气之核,用于约束整个维度的冰暴伟力。它安静、精确、无情——因为它的创造者再也感受不到冷了。

### 6.2 战斗阶段机制

**总览状态机**

```
┌───────────┐  玩家进入 core_hall 且结界已碎
│  DORMANT  │──────────────────────────────▶ ┌────────────┐  3.0s 唤醒演出
└───────────┘                                │ AWAKENING  │──────────┐
                                             └────────────┘          │
                                                                     ▼
                        ┌─────────────────────────────────────────────────────┐
                        │              PHASE 1  机枢运转 (100%–50%)            │
                        │   状态: IDLE ⇄ SLAM ⇄ GALE ⇄ SUMMON                 │
                        └──────────────────────────┬──────────────────────────┘
                                                   │ HP ≤ 50%
                                                   ▼
                        ┌─────────────────────────────────────────────────────┐
                        │    PHASE_TRANSITION(2.5s 全场冲击波 + 强制超限暴风潮)  │
                        └──────────────────────────┬──────────────────────────┘
                                                   ▼
                        ┌─────────────────────────────────────────────────────┐
                        │        PHASE 2  反应炉超频·绝对零度 (50%–0%)         │
                        │   状态: IDLE ⇄ RAY ⇄ ICICLE ⇄ SHIELD                 │
                        └──────────────────────────┬──────────────────────────┘
                                                   │ HP = 0
                                                   ▼
                                             ┌───────────┐
                                             │   DEATH   │(3.5s 倒地演出 + 掉落)
                                             └───────────┘
```

**Phase 1(100% → 50% HP)机枢运转**

| 技能 | 触发 | 效果 | 冷却 |
| :- | :- | :- | :- |
| **永冻冰刺浪潮** | 距离 ≤ 10 格 | 重拳轰地,以自身为圆心向前方 120° 扇形扩散 3 排冰刺,每排推进 4 格,命中造成 **10** 点伤害 + 冻结 4 s + 霜冻 +25 | 6.0 s |
| **胸腔风道·极寒气浪** | 距离 ≤ 6 格 | 胸腔风道喷发,半径 6 格击退 **2.5** 格/玩家,施加缓慢 IV 10 s + 霜冻 +40 | 9.0 s |
| **召唤风暴工蜂** | 场上工蜂 < 3 时 | 召唤 3 只 `storm_drone`(风暴工蜂),骚扰玩家 | 15.0 s |

**风暴工蜂 (Storm Drone)** —— `examplemod:storm_drone`

| 属性 | 值 |
| :- | :- |
| 生命值 | 12 |
| 攻击力 | 3.0 |
| 移动速度 | 0.32(飞行) |
| 行为 | 环绕玩家巡航,近身自爆式撞击(伤害 3 + 霜冻 +5) |
| 掉落 | `storm_shard` 0–1 |

**Phase 2(50% → 0% HP)反应炉超频·绝对零度**

进入 Phase 2 时,`core_hall` 强制切换为 **「超限暴风潮」(Overlimit Surge)** 局部环境:

| 效果 | 数值 |
| :- | :- |
| 能见度 | 雾距收缩至 **4 格** |
| 失温倍率 | 在 §4.1 基础上**再 ×2.0**(即平静期的 4 倍) |
| 环境光 | 附加 −0.5 |
| 屏幕 | 持续轻微抖动(±0.4°) |
| 与全局周期关系 | **覆盖**全局天气,战斗期间始终狂暴;战斗结束后恢复全局周期 |

| 技能 | 触发 | 效果 | 冷却 |
| :- | :- | :- | :- |
| **极寒风暴射线** | 持续旋转 | 从胸腔向 4 个方向发射缓慢旋转的光束臂;光束扫过命中玩家造成 **12** 点伤害 + 霜冻 +30;转速 0.75°/tick | 常驻,每 20 s 换一次转向 |
| **天花板冰锥坍塌** | 每 12 s | 玩家头顶上方 8–16 格生成 3 处下坠冰锥投影,1.5 s 后坠落,命中造成 **8** 点伤害 + 短暂眩晕 | 12.0 s |
| **充能球护盾** | HP 首次 ≤ 50% 时;以及 ≤ 25% 时各一次 | 释放 **3 颗**浮空充能球(HP 40/颗,环绕 Boss 半径 6 格),Boss 进入**无敌**;击破全部 3 颗方可继续输出 | 触发式 |

**充能球 (Charging Orb)**

| 属性 | 值 |
| :- | :- |
| 生命值 | 40 |
| 行为 | 围绕 Boss 匀速公转,周期 8 s |
| 击破反馈 | 播放 `examplemod:entity.tempest_core_golem.orb_break`,Boss 短暂露出核心弱点 |
| 未击破惩罚 | 每存在 1 颗充能球,Boss 每秒回复 **2** 点生命 |

### 6.3 终极战利品与天气神器

**Boss 掉落表(必掉 + 概率)**

| 物品 | 数量 | 概率 |
| :- | :- | :- |
| `examplemod:glacial_storm_core`(极寒风暴之核) | 1 | 100% |
| `examplemod:alchemist_journal_finale`(《老炼金术士的手札·终章》) | 1 | 100% |
| `examplemod:blizzard_scepter`(暴风雪权杖,已铸成) | 1 | 100%(首次击杀) |
| `examplemod:storm_shard` | 8–12 | 100% |
| `examplemod:blizzard_crystal` | 6–10 | 100% |
| `examplemod:charged_storm_core` | 2–3 | 100% |
| 附魔金苹果 | 1 | 35% |

**天气神器规格**

| 注册 ID | 中文名 | 类型 | 主动效果 | 冷却 | 耐久 |
| :- | :- | :- | :- | :- | :- |
| `examplemod:blizzard_scepter` | 【法杖】暴风雪权杖 | 法杖(远程引导) | 蓄力 1.0 s 后在准星处召唤半径 5 的暴风雪涡流(持续 6 s):范围内敌人**冰冻**、获得**易伤(受到伤害 +25%)**、**缓慢 III**;**冰霜生物(含怨灵/铁卫)转为中立** | 12 s | 640 |
| `examplemod:frostcleaver` | 【近战】霜痕巨刃 | 大剑(蓄力重劈) | 蓄力 1.2 s 后斩出直线冰霜地裂(长 12 格,宽 2 格):路径敌人被**冻结**并**向上击飞** 1.5 格 | 5 s | 1280 |

**神器合成链(畅玩后期内容)**

| 产物 | 配方(有序/无序) |
| :- | :- |
| `frostcleaver` | 1 × `glacial_storm_core` + 4 × `storm_shard` + 2 × `blizzard_crystal_block` + 2 × `minecraft:netherite_ingot`(排列为剑形) |
| `weather_harmonizer` | 1 × `glacial_storm_core` + 1 × `weather_condenser` + 4 × `charged_storm_core` + 1 × `minecraft:compass` |
| `storm_relay`(可再制) | 4 × `blizzard_crystal` + 2 × `charged_storm_core` + 1 × `minecraft:copper_block` |
| `dormant_storm_core` | 4 × `blizzard_crystal` + 4 × `storm_shard` + 1 × `discordant_crystal_shard` |
| `weather_condenser` | 3 × `minecraft:copper_ingot` + 2 × `blizzard_crystal` + 1 × `minecraft:amethyst_shard` |

### 6.4 天气掌控器 (Weather Harmonizer) 运行规范

| 属性 | 值 |
| :- | :- |
| 注册 ID | `examplemod:weather_harmonizer` |
| 类型 | 便携罗盘(右键使用) |
| 作用范围 | 玩家所在**维度**全局 |
| 内部能量 | `examplemod:harmonizer_energy`(`DataComponentType<Integer>`,0–200) |
| 单次切换消耗 | 50 能量 |
| 能量恢复 | 每 40 ticks(2 s)+1,仅当玩家处于**露天**且所在维度正在降水时;若在 `crystal_realm` 的 `SURGE` 相位则 ×3 |
| 使用冷却 | 3 s(防止误触连点) |

**可切换天气模式**

| 维度 | 模式 1 | 模式 2 | 模式 3 |
| :- | :- | :- | :- |
| 主世界(及其他原版维度) | `CLEAR` 晴天 | `RAIN` 降雨 | `THUNDER` 雷雨 |
| `examplemod:crystal_realm` | `CALM` 凝霜平静 | `SNOW` 降雨(常规雪) | `BLIZZARD` 暴风雪 |

> 对 `crystal_realm` 使用 `BLIZZARD` 时,直接强制 `BlizzardCycleManager` 进入 `SURGE`;使用 `CALM` 时强制重置周期计时器。这给予了玩家主动"刷风暴充能"的高阶玩法。

**状态机**

```
   ┌───────────┐  右键(能量 ≥ 50 且冷却结束)   ┌──────────────┐
   │  IDLE     │──────────────────────────────▶│  SWITCHING   │
   │(可切换)   │       消耗 50 能量             │ (0.5s 演出)   │
   └─────▲─────┘                               └──────┬───────┘
         │  0.5s 结束,冷却 3s                          │ 切换目标维度天气参数
         └────────────────────────────────────────────┘
  能量不足? ──▶ 播放 examplemod:item.harmonizer_denied + 提示"能量不足"
```

**通关身份跃迁**

> 获得掌控器后,玩家从"环境的受害者"正式转为"气象之主":可以在任意维度召晴、唤雨、掀起暴风雪。这是全篇叙事「重走先哲之路」的终点——**不是重复先哲的恐惧,而是完成他未能做到的理解式掌控**。

---

## 7. 研发实施路线与技术落地规范 (Implementation Roadmap)

### 7.1 代码与资源组织规范

**包结构(遵循 `AGENTS.md` §5.3 双端隔离)**

```
com.example.examplemod
├── ExampleMod.java                     注册中枢(已存在)
├── ModDimensions.java / ModTags.java / ModEntities.java  (已存在)
├── Config.java
├── weather/
│   ├── BlizzardCycleManager.java        风暴潮周期调度
│   ├── HypothermiaManager.java          失温结算(HotSwap 友好)
│   ├── StormChargeManager.java          充能结算
│   └── WeatherAltarRitualManager.java   祭坛仪式状态机
├── network/
│   ├── ModPayloads.java                 注册 RegisterPayloadHandlersEvent
│   ├── FrostStatePayload.java           S2C 霜冻值同步
│   └── HarmonizerActionPayload.java     C2S 天气切换请求
├── block/                               (已存在若干)
│   ├── WeatherAltarCoreBlock.java
│   ├── ConduitPylonBlock.java
│   ├── GlacierBarrierBlock.java
│   └── StormRelayBlock.java
├── blockentity/
│   ├── WeatherAltarCoreBlockEntity.java
│   ├── ConduitPylonBlockEntity.java
│   └── StormRelayBlockEntity.java
├── entity/
│   ├── BlizzardWraith.java
│   ├── FrostAutomaton.java
│   ├── StormDrone.java
│   └── TempestCoreGolem.java
├── item/
│   ├── AncientWeatherNotesItem.java
│   ├── WeatherCondenserItem.java
│   ├── BlizzardScepterItem.java
│   ├── FrostcleaverItem.java
│   └── WeatherHarmonizerItem.java
├── mixin/
│   └── PlayerMixin.java                仅一行跳板(见 §7.5)
├── client/                              ★ 所有 net.minecraft.client.* 仅限此包
│   ├── FrostHudOverlay.java
│   ├── StormFogHandler.java
│   ├── ConduitBeamRenderer.java
│   └── ClientParticleProviders.java(已存在)
├── datagen/                             DataGen Providers
│   ├── ModRecipeProvider.java
│   ├── ModLootTableProvider.java
│   ├── ModBlockStateProvider.java
│   ├── ModItemModelProvider.java
│   ├── ModBlockTagsProvider.java
│   ├── ModDamageTypeProvider.java
│   └── ModWorldGenProvider.java
└── compat/
    └── PatchouliCompat.java             Patchouli 可选集成
```

**命名规范**

| 类别 | 规则 | 示例 |
| :- | :- | :- |
| 注册 ID | 全小写 `snake_case`,`examplemod:` 命名空间 | `blizzard_scepter` |
| 方块 | 名词短语,建筑类用 `_bricks` / `_bars` 后缀 | `frost_stone_bricks` |
| 物品 | 材料用单数名词,器皿用 `_scepter` / `_cleaver` | `glacial_storm_core` |
| 实体 | 名词短语,生物用物种名,构装体用 `_golem` / `_automaton` | `frost_automaton` |
| 音效 | `examplemod:<类别>.<对象>.<动作>` | `examplemod:weather.conduit_lock` |
| 数据组件 | `examplemod:<用途>` | `examplemod:storm_charge` |
| 伤害类型 | `examplemod:<现象>` | `examplemod:frostbite` |
| 结构池 | `examplemod:frost_observatory/<模块>` | `.../conduit_wing_a` |
| 状态类字段 | 常量 `UPPER_SNAKE`,实例字段 `lowerCamel` | `MAX_FROST` / `frostValue` |

### 7.2 四大里程碑垂直切片 (Vertical Slices)

#### Milestone 1:入界闭环(祭坛 + 古籍 + 仪式 + 传送)

| 项 | 交付物 |
| :- | :- |
| 方块 | `weather_stone` / `chiseled_weather_stone` / `weather_altar_core` / `weather_pedestal`(含 BE) |
| 物品 | `sealed_weather_crate` / `ancient_weather_notes` / `discordant_crystal_shard` |
| 结构 | `examplemod:weather_altar` Jigsaw 单池 + `structure_set` + BiomeModifier 注入 |
| 系统 | `WeatherAltarRitualManager` 全状态机 + 安全落点算法 + 裂隙方块与粒子 |
| Patchouli | `data/examplemod/patchouli_books/ancient_weather_notes/` 全书结构(序章全文 + 残页其一/二/三章节框架;残页正文随 M3 地牢宝箱联动解锁) |
| **验收** | 新世界雪原/平原村庄附近能找到祭坛;挖核心掉石箱;开箱得手记;搭建结构 → 激活 → 进入 `crystal_realm`;`runServer` 无客户端类泄漏 |

#### Milestone 2:极寒双轨系统(失温 + 风暴潮 + 充能)

| 项 | 交付物 |
| :- | :- |
| 属性 | `examplemod:frost_resistance` + T1/T2 装备属性接入 |
| 装备 | `frost_wool_helmet/chestplate/leggings/boots` |
| 系统 | `HypothermiaManager`(分层/滞回/热源/伤害)+ `FrostHudOverlay` + `FrostStatePayload` |
| 系统 | `BlizzardCycleManager`(四相位 → `ServerWeatherHandler` 联动)+ `StormFogHandler`(雾)+ 动态风力 |
| 实体 | `blizzard_wraith`(含冲刺撕裂 AI) |
| 物品 | `dormant_storm_core` / `charged_storm_core` / `weather_condenser` / `storm_relay`(BE)+ `storm_charge` 组件 |
| **验收** | 暴风潮按 10/3–4 分钟循环;失温 HUD 五层反馈正确;T1 减缓 70%、T2 免疫 100%;户外高台充能 1 分钟产 1 枚核;`runServer` 独立可跑 |

#### Milestone 3:观测所地牢(结构 + 导流柱 + 结界)

| 项 | 交付物 |
| :- | :- |
| 方块 | `frost_stone` / `frost_stone_bricks` / `cracked_frost_stone_bricks` / `chiseled_frost_stone` / `frost_iron_bars` |
| 结构 | `frost_observatory` Jigsaw 全套池(塔基/楼梯/四大房间/导流翼/反应炉环/核心大厅) |
| 系统 | `ConduitPylonBlockEntity` 四态状态机 + `frost_automaton` 激战 + 能量光束渲染(`ConduitBeamRenderer`) |
| 系统 | `GlacierBarrierBlock` + 4 层符文点亮逻辑 + 碎裂演出 |
| 叙事 | 房间战利品表刷入《残页其一/二/三》 + 封印储藏室锁逻辑 |
| **验收** | `spacing=32` 稀疏度下可稳定定位;4 座导流柱逐个点亮;铁卫全歼后 `LIT`;4 层符文亮齐后结界碎裂;地牢内 `runGameTestServer` 用例覆盖状态机 |

#### Milestone 4:决战与神器(Boss + 战利品 + 掌控器)

| 项 | 交付物 |
| :- | :- |
| 实体 | `tempest_core_golem`(两阶段 AI 状态机 + 4 技能 + 充能球护盾)+ `storm_drone` |
| 机制 | 超限暴风潮局部环境覆写 + 旋转射线 + 冰锥坍塌 |
| 战利品 | `glacial_storm_core` / `alchemist_journal_finale` / `blizzard_scepter` / `frostcleaver` |
| 神器 | `blizzard_scepter` / `frostcleaver` 主动技能实现 + `weather_harmonizer` 天气切换网络包 |
| 叙事 | 《手札·终章》全文入库 + Patchouli 终章章节解锁 |
| **验收** | Boss 血条正常;50% 转阶段演出;两轮充能球护盾可破;击杀掉落完整;掌控器可切换主世界三态与维度三态;`.\gradlew.bat build` 通过 |

### 7.3 网络与双端同步规范

**Payload 清单(全部 `record ... implements CustomPacketPayload`,注册于 Mod 总线 `RegisterPayloadHandlersEvent`)**

| Payload | 方向 | 内容 | 频率 |
| :- | :- | :- | :- |
| `FrostStatePayload` | S→C | `float frostValue`、`byte tierIndex` | 每 10 ticks / 状态变化时 |
| `HarmonizerActionPayload` | C→S | `int modeId`(0–2)+ 校验手柄 slot | 玩家右键时 |
| `ConduitSyncPayload` | S→C | `BlockPos pylon`、`byte state` | 状态变化时 |
| `BossPhasePayload` | S→C | `int entityId`、`byte phase` | 阶段切换时 |

**权威端铁律**

- 失温累积/伤害、充能结算、导流柱状态、Boss 血量与无敌判定、天气切换 -> **全部服务端权威**。
- 客户端仅负责:霜冻 HUD、雾效、粒子、光束渲染、音效。
- 严禁 `net.minecraft.client.*` 出现在 `common/` 包(合并前 `runServer` 为硬门禁)。

### 7.4 数据驱动与 DataGen 规范

| 数据 | 路径 | 生成器 |
| :- | :- | :- |
| 方块状态/模型 | `assets/examplemod/blockstates|models/` | `ModBlockStateProvider` |
| 物品模型 | `assets/examplemod/models/item/` | `ModItemModelProvider` |
| 配方 | `data/examplemod/recipe/` | `ModRecipeProvider` |
| 战利品表 | `data/examplemod/loot_table/` | `ModLootTableProvider` |
| 方块标签 | `data/examplemod/tags/block/` | `ModBlockTagsProvider` |
| 伤害类型 | `data/examplemod/damage_type/` | `ModDamageTypeProvider` |
| 世界生成(结构/池/群系) | `data/examplemod/worldgen/` | `ModWorldGenProvider` |
| Patchouli 书籍 | `data/examplemod/patchouli_books/` | 手写(静态资源) |

- 全部输出至 `src/generated/resources/`,**严禁**指向 `src/main/resources`(`AGENTS.md` §4.6)。
- 所有可调数值(失温速率、霜冻上限、充能速率、Boss 血量、冷却)优先经 `Config.COMMON` 或数据驱动暴露。
- worldgen JSON 首次使用前**必须**经 Misode 1.21.1 校验(`AGENTS.md` §7)。

### 7.5 热重载适配规范

| 改动类型 | 载体 | 重载动作 |
| :- | :- | :- |
| 失温/充能/Boss AI 逻辑 | `*Manager.java` / 实体普通方法 | JBR HotSwap,无需重启 |
| 新增技能数值 | `manager` / Config | JBR HotSwap / `/reload` |
| Mixin 注入点本身 | `mixin/PlayerMixin.java` | **必须重启** |
| 新增方块/物品/实体注册项 | `DeferredRegister` | **必须重启**(注册表冻结) |
| 结构/Jigsaw 池/配方/标签 | `data/` | `/reload`(结构需重进存档) |
| Patchouli 文本 | `patchouli_books/` | `/reload` + 重开书 |
| 粒子/雾/HUD | `client/` | `Ctrl+Shift+F9`(类)+ `F3+T`(资源) |

- **跳板模式强制**:`PlayerMixin` 只允许 `HypothermiaManager.tick((Player)(Object)this);` 一行;业务全部外置。

### 7.6 质量门禁与验收清单

- [ ] `.\gradlew.bat build` 通过,产物含全部注册项。
- [ ] `.\gradlew.bat runServer` 干净启动,零 `net.minecraft.client.*` 泄漏。
- [ ] `.\gradlew.bat runGameTestServer` 全绿(至少覆盖:失温分层、充能上限、导流柱四态、结界计数、Boss 阶段切换)。
- [ ] `src/generated/resources/` 产物已提交。
- [ ] `neoforge.mods.toml`:`modId=examplemod`、`version=1.0.0`、`license` 正确;`patchouli` 声明为 **optional** 依赖。
- [ ] 首次击杀 Boss 掉落完整;掌控器三态可切换且服务端权威。

---

## 附录 A:完整注册清单 (Namespace Registry)

> 命名空间 `examplemod`。✅ = 已实现;🆕 = 本 GDD 新增。

### A.1 方块 (Blocks)

| ID | 中文名 | 状态 |
| :- | :- | :- |
| `frost_flower` / `frost_grass` / `frost_sapling` / `frost_log` / `frost_leaves` | 冰晶花/冰雪草/坚冰木树苗/坚冰木/坚冰木树叶 | ✅ |
| `permafrost` / `deep_permafrost` | 冻土/深层冻土 | ✅ |
| `blizzard_crystal_block` | 暴风雪结晶块 | ✅ |
| 17 种冻土矿石(煤/铜/金/红石/绿宝石/青金石/钻石/铁 × 浅/深) | — | ✅ |
| `frost_wool` | 冰原羊毛 | ✅ |
| `weather_stone` | 天气石 | 🆕 |
| `chiseled_weather_stone` | 雕纹天气石 | 🆕 |
| `weather_altar_core` | 失控天气核心 | 🆕 |
| `weather_pedestal` | 调谐基座 | 🆕 |
| `frost_stone` | 霜纹石 | 🆕 |
| `frost_stone_bricks` | 霜纹石砖 | 🆕 |
| `cracked_frost_stone_bricks` | 开裂霜纹石砖 | 🆕 |
| `chiseled_frost_stone` | 雕纹霜纹石 | 🆕 |
| `frost_iron_bars` | 极寒铁栏杆 | 🆕 |
| `conduit_pylon` | 天气导流柱 | 🆕 |
| `glacier_barrier` | 决战结界 | 🆕 |
| `condenser_vent` | 冷凝风道 | 🆕 |
| `pressure_pipe` | 泄压管道 | 🆕 |
| `reactor_core` | 反应炉核心 | 🆕 |
| `storm_relay` | 风暴引风基座 | 🆕 |

### A.2 物品 (Items)

| ID | 中文名 | 状态 |
| :- | :- | :- |
| `blizzard_crystal` | 暴风雪结晶 | ✅ |
| `blizzard_crystal_helmet/chestplate/leggings/boots` | 冰晶四件套 | ✅ |
| `blizzard_crystal_sword/pickaxe/axe/shovel/hoe` | 冰晶五工具 | ✅ |
| `frost_mutton` / `cooked_frost_mutton` / `frost_beef` / `cooked_frost_beef` / `frost_leather` | 冰原食物与皮革 | ✅ |
| `ancient_weather_notes` | 《古代天气研究手记》 | 🆕 |
| `sealed_weather_crate` | 密闭石箱 | 🆕 |
| `discordant_crystal_shard` | 失调晶核残片 | 🆕 |
| `dormant_storm_core` | 休眠风暴晶石 | 🆕 |
| `charged_storm_core` | 充能风暴核 | 🆕 |
| `storm_shard` | 狂暴风暴碎片 | 🆕 |
| `alchemist_journal_finale` | 《老炼金术士的手札·终章》 | 🆕 |
| `glacial_storm_core` | 极寒风暴之核 | 🆕 |
| `weather_condenser` | 天气引风瓶 | 🆕 |
| `frost_wool_helmet/chestplate/leggings/boots` | 霜绒御寒四件套 | 🆕 |
| `blizzard_scepter` | 【法杖】暴风雪权杖 | 🆕 |
| `frostcleaver` | 【近战】霜痕巨刃 | 🆕 |
| `weather_harmonizer` | 天气掌控器 | 🆕 |
| `conduit_key` | 导流钥匙 | 🆕 |

### A.3 实体 (Entities)

| ID | 中文名 | 状态 |
| :- | :- | :- |
| `frost_sheep` / `frost_cow` | 冰原羊/冰原牛 | ✅ |
| `blizzard_wraith` | 霜风怨灵 | 🆕 |
| `frost_automaton` | 霜纹铁卫 | 🆕 |
| `storm_drone` | 风暴工蜂 | 🆕 |
| `tempest_core_golem` | 极寒机枢·泰坦 | 🆕 |

### A.4 属性 / 数据组件 / 伤害类型 / 粒子 / 音效

| 类别 | ID | 状态 |
| :- | :- | :- |
| 属性 | `examplemod:frost_resistance` | 🆕 |
| 数据组件 | `examplemod:storm_charge` | 🆕 |
| 数据组件 | `examplemod:harmonizer_energy` | 🆕 |
| 伤害类型 | `examplemod:frostbite` | 🆕 |
| 粒子 | `examplemod:blizzard_snow` | ✅ |
| 粒子 | `examplemod:frost_vortex` / `conduit_beam` / `reactor_ray` / `frost_shard_ambient` | 🆕 |
| 音效 | `weather.altar_awaken` / `weather.blizzard_roar` / `weather.blizzard_surge_begin` / `weather.conduit_charge` / `weather.conduit_lock` / `weather.barrier_seal_1..4` / `weather.barrier_shatter` | 🆕 |
| 音效 | `entity.blizzard_wraith.charge/ambient` / `entity.tempest_core_golem.step/roar/orb_break` / `item.storm_core_charged` / `item.harmonizer_denied` | 🆕 |

### A.5 结构 / 维度 / 群系 / 标签

| 类别 | ID | 状态 |
| :- | :- | :- |
| 维度 | `examplemod:crystal_realm` | ✅ |
| 群系 | `examplemod:crystal_plains` | ✅ |
| 结构 | `examplemod:crystal_village` | ✅ |
| 结构 | `examplemod:weather_altar` | 🆕 |
| 结构 | `examplemod:frost_observatory` | 🆕 |
| 标签 | `examplemod:frost_plantable_on` | ✅ |
| 标签 | `examplemod:permafrost_ore_replaceables` / `deep_permafrost_ore_replaceables` | ✅ |
| 标签 | `examplemod:conductive_armor`(预留扩展) | 🆕 |

---

## 附录 B:核心数值总表 (Numeric Codex)

### B.1 失温系统

| 参数 | 值 |
| :- | :- |
| `MAX_FROST` | 140.0 |
| 基础累积 | +5.0 / 秒 |
| 暴风潮倍率 | ×2.0 |
| 抗寒公式 | `× (1 − frost_resistance)` |
| 冻结伤害 | 1.5 / 1.5 秒,`bypasses_armor` |
| 状态阈值 | 40 / 80 / 120 / 140 |
| 滞回带 | 10 |
| 热源消退 | 火把 −8 / 营火 −20 / 岩浆 −30 / 遮蔽 −6(取最大值) |
| 暴风潮热源衰减 | ×0.5 |

### B.2 风暴潮周期

| 相位 | ticks | 时长 |
| :- | :- | :- |
| `CALM` | 12000 | 10 min |
| `RAMP_UP` | 300 | 15 s |
| `SURGE` | 3600–4800 | 3–4 min |
| `RAMP_DOWN` | 600 | 30 s |
| 平均周期 | 17900 | 14.9 min |

### B.3 充能系统

| 参数 | 值 |
| :- | :- |
| 单晶石上限 | 180 |
| 手持速率 | +1.0 / 秒 |
| 引风瓶增幅 | +2.0 / 秒 |
| 引风基座 | +3.0 / 秒(y≥160 且露天) |
| 平静期泄能 | −0.5 / 秒 |

### B.4 生物数值

| 生物 | HP | ATK | 护甲 | 移速 | 击退抗性 |
| :- | :- | :- | :- | :- | :- |
| `blizzard_wraith` | 30 | 6.0 | 2.0 | 0.30 | 0.4 |
| `frost_automaton` | 40 | 8.0 | 8.0 | 0.25 | 0.6 |
| `storm_drone` | 12 | 3.0 | 0.0 | 0.32 | 0.0 |
| `tempest_core_golem` | 400 | 10/6/12/8(分技能) | 12.0 | 0.22 | 1.0 |
| 充能球 | 40 | 0 | 0 | 公转 | — |

### B.5 Boss 技能冷却

| 技能 | 阶段 | 冷却 |
| :- | :- | :- |
| 永冻冰刺浪潮 | P1 | 6.0 s |
| 极寒气浪 | P1 | 9.0 s |
| 召唤风暴工蜂 | P1 | 15.0 s |
| 极寒风暴射线 | P2 | 常驻(20 s 换向) |
| 天花板冰锥坍塌 | P2 | 12.0 s |
| 充能球护盾 | P2 | 触发式(50% / 25%) |

### B.6 装备数值

| 装备 | 霜冻抗性 | 备注 |
| :- | :- | :- |
| 霜绒套(全套) | 0.70 | 头0.10/胸0.25/腿0.20/靴0.15 |
| 冰晶套(全套) | 1.00 | 雪地/冻土移速 +15% |
| 暴风雪权杖 | — | 冷却 12 s,耐久 640 |
| 霜痕巨刃 | — | 冷却 5 s,耐久 1280 |
| 天气掌控器 | — | 能量 200,切换耗 50,冷却 3 s |

---

## 附录 C:状态机与流程图总索引

| # | 状态机/流程图 | 位置 |
| :- | :- | :- |
| 1 | 入界闭环游戏循环图 | [§3.4](#34-入界闭环游戏循环) |
| 2 | 祭坛仪式状态机 | [§3.3](#33-仪式状态机-ritual-state-machine) |
| 3 | 失温分层状态机(带滞回) | [§4.5](#45-第二阶段整体状态机) |
| 4 | 风暴周期状态机 | [§4.5](#45-第二阶段整体状态机) |
| 5 | 能源闭环图 | [§4.5](#45-第二阶段整体状态机) |
| 6 | 导流柱四态状态机 | [§5.3](#53-天气导流柱机关-weather-conduit-pylons) |
| 7 | 地牢通关状态机与能量经济 | [§5.5](#55-地牢探索状态机与能量经济) |
| 8 | Boss 两阶段状态机 | [§6.2](#62-战斗阶段机制) |
| 9 | 天气掌控器状态机 | [§6.4](#64-天气掌控器-weather-harmonizer-运行规范) |
| 10 | 观测所空间拓扑 | [§2.3](#23-极寒气象观测所编年史) |

---

## 附录 D:本地化键规范

> 一律遵循 `类别.examplemod.<id>` 格式;英文 en_us + 中文 zh_cn 双份。

| 类别前缀 | 示例 |
| :- | :- |
| `block.examplemod.` | `block.examplemod.conduit_pylon` |
| `item.examplemod.` | `item.examplemod.blizzard_scepter` |
| `item.examplemod.<id>.desc` | 工具提示描述行 |
| `entity.examplemod.` | `entity.examplemod.tempest_core_golem` |
| `effect.examplemod.` | (若未来引入失温效果) |
| `biome.examplemod.` | `biome.examplemod.crystal_plains` |
| `itemGroup.examplemod` | 创造标签页标题 |
| `death.attack.examplemod.frostbite` | 冻伤死亡信息 |
| `message.examplemod.harmonizer.low_energy` | 掌控器能量不足提示 |
| `subtitles.examplemod.weather.conduit_lock` | 音效字幕 |

**示例(zh_cn)**

```json
{
  "item.examplemod.ancient_weather_notes": "《古代天气研究手记》",
  "item.examplemod.discordant_crystal_shard": "失调晶核残片",
  "item.examplemod.dormant_storm_core": "休眠风暴晶石",
  "item.examplemod.charged_storm_core": "充能风暴核",
  "item.examplemod.storm_shard": "狂暴风暴碎片",
  "item.examplemod.glacial_storm_core": "极寒风暴之核",
  "item.examplemod.blizzard_scepter": "【法杖】暴风雪权杖",
  "item.examplemod.frostcleaver": "【近战】霜痕巨刃",
  "item.examplemod.weather_harmonizer": "天气掌控器",
  "item.examplemod.weather_condenser": "天气引风瓶",
  "entity.examplemod.blizzard_wraith": "霜风怨灵",
  "entity.examplemod.frost_automaton": "霜纹铁卫",
  "entity.examplemod.storm_drone": "风暴工蜂",
  "entity.examplemod.tempest_core_golem": "极寒机枢·泰坦",
  "block.examplemod.weather_altar_core": "失控天气核心",
  "block.examplemod.weather_pedestal": "调谐基座",
  "block.examplemod.chiseled_weather_stone": "雕纹天气石",
  "block.examplemod.frost_stone_bricks": "霜纹石砖",
  "block.examplemod.cracked_frost_stone_bricks": "开裂霜纹石砖",
  "block.examplemod.frost_iron_bars": "极寒铁栏杆",
  "block.examplemod.conduit_pylon": "天气导流柱",
  "block.examplemod.glacier_barrier": "决战结界",
  "block.examplemod.storm_relay": "风暴引风基座",
  "death.attack.examplemod.frostbite": "%1$s 被严寒冻成了冰雕",
  "message.examplemod.harmonizer.low_energy": "天气掌控器能量不足"
}
```

---

> **结语**:本规范为《天气冒险维度模组:极寒之境与天气掌控体系》的定稿设计基线。所有玩法模块、数值、命名与流程均已收敛,可直接进入 Milestone 1 编码。任何后续数值调整须经 `Config` / 数据包生效,并同步回本文件与 `mod.md`。
