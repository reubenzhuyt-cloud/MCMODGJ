#!/usr/bin/env python3
"""双向断言：建材数据表 ↔ 生成资源 ↔ Java 注册（设计文档 §7.4）。

本脚本**纯静态**：不加载 Minecraft、不启动游戏、不依赖第三方库，只用标准库。
它把三处来源拆开解析，再做双向对称比较：

* **数据表**——``biome_data.all_building_block_ids()``（族展开后的权威 id 全集）。
* **Java 注册**——``ModBlocks.java`` / ``ModItems.java`` 的**字面**注册调用
  （外加阶段 C 新增的 ``ModBuildingBlocks.java``，存在才扫描），以及
  ``ModCreativeTabs.java`` 里 ``TabCategory`` 分类表的**字面** id 表。
  阶段 C 的建材按变量名/族工厂循环注册，id 不一定以字符串字面量出现在
  ``ModBuildingBlocks.java`` 中，因此**标签页字面表**也是表 → Java 方向的合法来源
  （B3/B4 报告 §9.2、设计 §7.4）。B5 计划 verbatim 亦如此。
* **生成资源**——每个表内 id 的 blockstate / 方块模型 / 物品模型 / 掉落表，以及
  ``lang`` 中英文键。

**方向 1（表 → Java）**：表内每个 id 必须出现在 Java 字面注册或标签页字面表中。
**方向 2（Java → 表 / 资源）**：表内每个 id 必须有配套资源与中英文 lang 键；凡命中
「新建材命名模式」的 Java/标签页字面 id 必须在表内（识别"多注册"）。

**防"永远通过"**：任何解析器抓到 **0 个 id**（Java 字面注册、标签页表、lang 文件）、
或"新建材命名模式"自检失败时，直接 ``sys.exit(1)``——绝不把空解析当成"零差异通过"。
Phase B 的 ``BUILDING_FAMILIES = []`` 时数据表合法为空，此时两方向均为空 → 退出 0；
一旦阶段 C 填入真实族，本脚本即开始逐条校验。

用法（仓库根目录）：

    python tools/verify_building_assets.py

双向对称差为空时退出 0，否则列出差异并退出 1。
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import Counter
from pathlib import Path

# 让中文诊断在 Windows 控制台也按 UTF-8 输出，避免 GBK 编码报错。
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(encoding="utf-8", errors="replace")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import biome_data as bd  # noqa: E402

# --- 路径约定（相对仓库根；不硬编码绝对路径）----------------------------------------------
JAVA_REL = Path("src/main/java/com/example/weather_realm")
CORE_JAVA = ("ModBlocks.java", "ModItems.java")  # 必须存在
OPTIONAL_JAVA = ("ModBuildingBlocks.java",)      # 阶段 C 新增，存在才扫描
ASSETS_REL = Path("src/main/resources/assets")
DATA_REL = Path("src/main/resources/data")

# --- 解析规则 ----------------------------------------------------------------------------
class ParseError(Exception):
    """源码结构与校验脚本不同步（正则没抓到任何东西）。"""


# Java 字面注册：设计 §7.4 的三类 + 物品侧 registerSimpleBlockItem/BLOCKS.register。
# 注意 registerBlock( 与 registerSimpleBlock( 不互相误配：前者要求 "registerBlock(" 紧邻。
REGISTER_RES = [
    re.compile(r'registerSimpleBlock\(\s*"([a-z0-9_]+)"'),
    re.compile(r'registerBlock\(\s*"([a-z0-9_]+)"'),
    re.compile(r'registerSimpleBlockItem\(\s*"([a-z0-9_]+)"'),
    re.compile(r'(?:ITEMS|BLOCKS)\.register\(\s*"([a-z0-9_]+)"'),
]

# ModCreativeTabs.java：TabCategory 分类表（key, List.of(...)）与其 id 字面量。
TAB_CATEGORY_RE = re.compile(r'new\s+TabCategory\(\s*"(\w+)"\s*,\s*List\.of\((.*?)\)\)', re.DOTALL)
QUOTED_ID_RE = re.compile(r'"([a-z0-9_]+)"')

# 新建材命名模式（用于识别"Java/标签页里有、表里没有"的多余建材；不误伤 frost_* 手写件）。
BUILDING_PATTERNS = [
    r'^(?:permafrost|deep_permafrost|fire_stone|deep_fire_stone|weathered_sandstone|deep_weathered_sandstone)_'
    r'(?:polished|polished_stairs|polished_slab|polished_wall|bricks|brick_stairs|brick_slab|brick_wall|cracked_bricks|chiseled|pillar)$',
    r'^(?:scorched|arid)_(?:planks|stairs|slab|fence|fence_gate|door|trapdoor|pressure_plate|button)$',
    r'^stripped_(?:scorched|arid)_wood$',
    r'^(?:frost|blaze|wind)_(?:lantern|glass|glass_pane|grate|chain)$',
    r'^(?:frost|blaze|wind)_(?:crystal_cluster|snow_layer|ash_layer|sand_layer|spike)$',
]
BUILDING_RE = [re.compile(p) for p in BUILDING_PATTERNS]

# 命名模式自检样本：若模式被改坏（永远不命中或误伤），脚本必须失败而不是"零差异通过"。
PATTERN_MUST_MATCH = (
    "permafrost_polished", "fire_stone_brick_wall", "scorched_planks",
    "stripped_arid_wood", "frost_lantern", "blaze_glass_pane",
)
PATTERN_MUST_NOT_MATCH = (
    "frost_planks", "permafrost", "scorched_log", "blaze_crystal_block", "frost_wool",
)


def is_building_id(name: str) -> bool:
    """是否命中「新建材命名模式」。"""
    return any(regex.match(name) for regex in BUILDING_RE)


def _selfcheck_patterns() -> None:
    """模式自检：命中集/非命中集任一不符即视为脚本失效。"""
    for sample in PATTERN_MUST_MATCH:
        if not is_building_id(sample):
            raise ParseError(f"建材命名模式自检失败：'{sample}' 应命中却未命中（模式已损坏？）")
    for sample in PATTERN_MUST_NOT_MATCH:
        if is_building_id(sample):
            raise ParseError(f"建材命名模式自检失败：'{sample}' 不应命中却命中（会误伤既有 id）")


def _read(path: Path) -> str:
    if not path.is_file():
        raise ParseError(f"缺少源文件: {path}")
    return path.read_text(encoding="utf-8")


def java_literal_ids(root: Path) -> tuple[set, list]:
    """ModBlocks/ModItems（必选）+ ModBuildingBlocks（可选）里的字面注册 id。

    抓到 0 个 id 直接报错——ModBlocks/ModItems 必然有注册，空解析说明正则与代码脱节。
    """
    ids: set = set()
    scanned: list = []
    for fname in CORE_JAVA:
        text = _read(root / JAVA_REL / fname)
        scanned.append(fname)
        for regex in REGISTER_RES:
            ids.update(regex.findall(text))
    for fname in OPTIONAL_JAVA:
        path = root / JAVA_REL / fname
        if not path.is_file():
            continue
        scanned.append(fname)
        text = path.read_text(encoding="utf-8")
        for regex in REGISTER_RES:
            ids.update(regex.findall(text))
    if not ids:
        raise ParseError(
            f"Java 源码未解析出任何字面注册 id（扫描 {scanned}；正则与代码不同步？）")
    return ids, scanned


def tab_ids(root: Path) -> set:
    """ModCreativeTabs.java 分类表里的字面 id（阶段 C 建材的权威来源）。

    抓到 0 个 id 直接报错，避免"表→Java"方向在标签页结构变化后静默通过。
    """
    text = _read(root / JAVA_REL / "ModCreativeTabs.java")
    ids: set = set()
    for _key, body in TAB_CATEGORY_RE.findall(text):
        ids.update(QUOTED_ID_RE.findall(body))
    if not ids:
        raise ParseError("ModCreativeTabs.java 未解析出任何 TabCategory id（结构变了？）")
    return ids


def lang_keys(root: Path, modid: str) -> dict:
    """{lang 文件名: 键集合}；必须含 en_us.json / zh_cn.json 且非空。"""
    lang_dir = root / ASSETS_REL / modid / "lang"
    files = sorted(lang_dir.glob("*.json"))
    if not files:
        raise ParseError(f"未找到语言文件: {lang_dir}/*.json")
    result: dict = {}
    for path in files:
        data = json.loads(path.read_text(encoding="utf-8"))
        if not data:
            raise ParseError(f"语言文件为空: {path}")
        result[path.name] = set(data)
    for required in ("en_us.json", "zh_cn.json"):
        if required not in result:
            raise ParseError(f"缺少必需的 {required}（中英文名无法完整校验）")
    return result


def _rel(root: Path, path: Path) -> str:
    try:
        return path.relative_to(root).as_posix()
    except ValueError:
        return path.as_posix()


def main() -> None:
    parser = argparse.ArgumentParser(description="建材资源与注册双向校验")
    parser.add_argument("--root", default=str(Path(__file__).resolve().parents[1]),
                        help="仓库根目录（默认脚本上一级）")
    args = parser.parse_args()
    root = Path(args.root).resolve()
    modid = bd.MODID

    try:
        _selfcheck_patterns()
        expected = list(bd.all_building_block_ids())
        java_ids, scanned = java_literal_ids(root)
        tabs = tab_ids(root)
        langs = lang_keys(root, modid)
    except (ParseError, json.JSONDecodeError) as exc:
        print("BUILDING ASSETS FAILED: 解析错误", file=sys.stderr)
        print(f"  - {exc}", file=sys.stderr)
        sys.exit(1)

    registered = java_ids | tabs
    expected_set = set(expected)
    counts = Counter(expected)

    duplicates = sorted(i for i, c in counts.items() if c > 1)
    # 缺失：表内 id 未在 Java 字面注册或标签页字面表中出现。
    missing_java = sorted(expected_set - registered)
    # 多余：命中新建材命名模式的 Java/标签页字面 id 不在表内。
    extra_java = sorted(i for i in registered if is_building_id(i) and i not in expected_set)

    missing_resources: list = []
    missing_lang: list = []
    for bid in sorted(expected_set):
        required = (
            ASSETS_REL / modid / "blockstates" / f"{bid}.json",
            ASSETS_REL / modid / "models/item" / f"{bid}.json",
            DATA_REL / modid / "loot_table/blocks" / f"{bid}.json",
        )
        for rel in required:
            if not (root / rel).is_file():
                missing_resources.append(_rel(root, root / rel))
        block_dir = root / ASSETS_REL / modid / "models/block"
        if not list(block_dir.glob(f"{bid}*.json")):
            missing_resources.append(f"{_rel(root, block_dir)}/{bid}*.json")
        for fname, keys in langs.items():
            key = f"block.{modid}.{bid}"
            if key not in keys:
                missing_lang.append(f"{fname}: {key}")

    print(f"[build] table ids    : {len(expected)} (distinct {len(counts)})")
    print(f"[build] java ids     : {len(java_ids)} (from {', '.join(scanned)})")
    print(f"[build] tab ids      : {len(tabs)}")
    print(f"[build] lang files   : {', '.join(langs)}")

    problems = []
    if missing_java:
        problems.append(("缺失", "table -> Java/tab", missing_java))
    if duplicates:
        problems.append(("重复", "duplicated table ids", duplicates))
    if extra_java:
        problems.append(("多余", "Java/tab building id not in table", extra_java))
    if missing_resources:
        problems.append(("资源缺失", "missing resources", missing_resources))
    if missing_lang:
        problems.append(("lang 缺失", "missing lang keys", missing_lang))

    if problems:
        print("BUILDING ASSETS FAILED", file=sys.stderr)
        for label, detail, items in problems:
            print(f"  [{label}] {detail}:", file=sys.stderr)
            for item in items[:100]:
                print(f"    - {item}", file=sys.stderr)
            if len(items) > 100:
                print(f"    ... (+{len(items) - 100} more)", file=sys.stderr)
        sys.exit(1)

    print("BUILDING ASSETS OK (0 differences)")
    sys.exit(0)


if __name__ == "__main__":
    main()
