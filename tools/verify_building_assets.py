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


# --- eco layer cover-noun: Java layerWord(...) <-> Python _ECO_BASES.word ---------------
# The layer block id is ``<prefix>_<word>_layer``. The *word* half exists twice, hard-coded:
#   * Java  : ``ModBuildingBlocks.layerWord(String prefix)`` (a switch expression), whose
#             result feeds ``BLOCKS.register(prefix + "_" + word + "_layer", ...)``;
#   * Python: the 5th field of every ``biome_data._ECO_BASES`` tuple, consumed by
#             ``layer_specs()`` / ``all_building_block_ids()`` to name the generated
#             resources.
# A drift between the two silently produces "registered id != resource id" (e.g. Java
# registers ``frost_snow_layer`` while Python generates ``frost_snowx_layer``); the static
# checks cannot see the dynamic Java registration and the mismatch only surfaces at runtime
# (creative tab ``orElseThrow``). The assertion below closes that hole.
LAYER_WORD_METHOD_RE = re.compile(
    r'private\s+static\s+String\s+layerWord\s*\([^)]*\)\s*\{(.*?)\n\s*\}', re.DOTALL)
LAYER_WORD_CASE_RE = re.compile(r'case\s+"([a-z0-9_]+)"\s*->\s*"([a-z0-9_]+)"\s*;')
LAYER_WORD_DEFAULT_RE = re.compile(r'default\s*->\s*"([a-z0-9_]+)"\s*;')


def java_layer_word_map(root: Path) -> tuple:
    """Parse ``ModBuildingBlocks.layerWord(...)`` -> ``(explicit_cases, default_word)``.

    ``explicit_cases`` maps the literal theme prefix to its literal cover-noun; the
    ``default`` arm (if present) is the fallback for any prefix not listed. A missing
    method or 0 parsed ``case`` arms is a parse failure -- never a silent "0 differences".
    """
    text = _read(root / JAVA_REL / "ModBuildingBlocks.java")
    match = LAYER_WORD_METHOD_RE.search(text)
    if not match:
        raise ParseError("ModBuildingBlocks.java 未找到 layerWord(...) 方法（结构变了？）")
    body = match.group(1)
    explicit = dict(LAYER_WORD_CASE_RE.findall(body))
    default_match = LAYER_WORD_DEFAULT_RE.search(body)
    default = default_match.group(1) if default_match else None
    if not explicit:
        raise ParseError("layerWord(...) 未解析出任何 case 映射（正则与代码不同步？）")
    return explicit, default


def layer_word_problems(root: Path) -> list:
    """Java ``layerWord`` vs Python ``_ECO_BASES[..].word`` -- the mirror must not drift.

    Every Python eco prefix must resolve in Java (explicit ``case`` or the ``default``
    arm) to the same word; conversely every explicit Java ``case`` must correspond to a
    Python eco prefix. Any difference is named per theme with both values shown.
    """
    explicit, default = java_layer_word_map(root)
    py_map = {prefix: word for prefix, _zh, _en, _profile, word in bd._ECO_BASES}
    problems: list = []
    for prefix, py_word in sorted(py_map.items()):
        if prefix in explicit:
            java_word = explicit[prefix]
        elif default is not None:
            java_word = default
        else:
            problems.append(
                f"{prefix}: Java layerWord(...) 无该前缀映射（case 与 default 都缺）")
            continue
        if java_word != py_word:
            problems.append(f"{prefix}: Java={java_word!r} Python={py_word!r}")
    for prefix in sorted(explicit):
        if prefix not in py_map:
            problems.append(
                f"{prefix}: Java layerWord(...) 有映射，但 Python _ECO_BASES 无该生态前缀")
    if not py_map:
        problems.append("_ECO_BASES 无生态条目，无法比对 layerWord 映射（防假绿）")
    return problems


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


# Every block-model file each engine shape must emit (gen_block_assets.write_block_client).
# These are *exact* stems, not prefix globs: a missing base model must never be masked by
# a collapsed sibling such as ``<name>_stairs.json``. The derived shapes ``_inner`` /
# ``_top`` / ``_side`` / ... are still required, list by list.
#
# The key is the **real** shape the generator branches on (``gen_block_assets`` does
# ``model = spec.get("shape", spec["model"])``), not the base parent a spec reuses:
# an eco cluster renders through ``minecraft:block/cross`` but its emitted branch is
# ``cluster``, and a layer's ``_height*`` segments / a spike's ``_tip`` / ``_frustum``
# / ``_middle`` / ``_base`` segments are only produced by those dedicated branches.
# Dispatching on the raw ``model`` here would leave every derived eco model unverified.
_BLOCK_MODEL_SUFFIXES = {
    "cross": ("",),
    "leaves": ("",),
    "cube_all": ("",),
    "glass_block": ("",),
    "chain": ("",),
    "pillar": ("", "_horizontal"),
    "stairs": ("", "_inner", "_outer"),
    "slab": ("", "_top"),
    "wall": ("_post", "_side", "_side_tall", "_inventory"),
    "fence": ("_post", "_side", "_inventory"),
    "fence_gate": ("", "_open", "_wall", "_wall_open"),
    "door": ("_bottom_left", "_bottom_left_open", "_bottom_right", "_bottom_right_open",
             "_top_left", "_top_left_open", "_top_right", "_top_right_open"),
    "trapdoor": ("_bottom", "_top", "_open"),
    "button": ("", "_pressed", "_inventory"),
    "pressure_plate": ("", "_down"),
    "lantern": ("", "_hanging"),
    "pane": ("_post", "_side", "_side_alt", "_noside", "_noside_alt"),
    # Eco decor dedicated branches (sub-project D): the derived segment models live here.
    "cluster": ("",),
    "layer": ("", "_height2", "_height4", "_height6", "_height8",
              "_height10", "_height12", "_height14"),
    "spike": ("", "_tip", "_frustum", "_middle", "_base"),
}


def real_shape(spec) -> str:
    """The branch ``write_block_client`` actually dispatches on for this spec."""
    return spec.get("shape", spec.get("model"))


def check_known_shapes(specs) -> None:
    """Fail loudly when a spec declares a shape/model the verifier has no suffixes for.

    ``_BLOCK_MODEL_SUFFIXES`` is the *closed* allow-list of engine shapes the resource
    check knows how to walk. When a new shape/model appears in the data table without a
    matching entry here, dispatching on it would leave its derived models unverified
    (e.g. ``cluster`` / ``layer`` / ``spike`` were once silently missed) -- a false green.
    Raise instead of skipping, naming every offending id + shape and the fix to apply.
    """
    unknown = []
    for spec in specs:
        shape = real_shape(spec)
        if shape not in _BLOCK_MODEL_SUFFIXES:
            unknown.append((spec.get("name", "<unnamed>"), shape))
    if unknown:
        detail = "; ".join(f"{name!r} -> {shape!r}" for name, shape in unknown)
        raise ParseError(
            "数据表出现校验器不认识的形状/model 值："
            f"{detail}；请把该形状的派生模型后缀补进后缀表 _BLOCK_MODEL_SUFFIXES")


def block_model_problems(root: Path, specs, modid: str) -> list:
    """Each building block's shape must have every *exact* model file on disk."""
    problems: list = []
    block_dir_rel = ASSETS_REL / modid / "models/block"
    for spec in specs:
        name = spec["name"]
        model = real_shape(spec)
        suffixes = _BLOCK_MODEL_SUFFIXES.get(model)
        if suffixes is None:
            problems.append(
                f"{name}: unknown shape/model {model!r}; "
                f"请把该形状的派生模型后缀补进后缀表 _BLOCK_MODEL_SUFFIXES")
            continue
        for suffix in suffixes:
            rel = block_dir_rel / f"{name}{suffix}.json"
            if not (root / rel).is_file():
                problems.append(_rel(root, root / rel))
    return problems


def door_texture_problems(root: Path, specs, modid: str) -> list:
    """Doors must reference two *distinct* sprites (upper hatch / lower planks).

    Guards the fix that split each wood family's door onto its own
    ``<prefix>_door_top`` / ``<prefix>_door_bottom`` textures: a regression back to a
    single shared texture would otherwise still pass every other check.
    """
    problems: list = []
    for spec in specs:
        if spec.get("model") != "door":
            continue
        name = spec["name"]
        top_ref = spec.get("tex", {}).get("top")
        bottom_ref = spec.get("tex", {}).get("parent")
        if not top_ref or not bottom_ref or top_ref == bottom_ref:
            problems.append(
                f"{name}: data table top/bottom tex not distinct ({top_ref!r} vs {bottom_ref!r})")
            continue
        for suffix in ("_bottom_left", "_top_left"):
            rel = ASSETS_REL / modid / "models/block" / f"{name}{suffix}.json"
            path = root / rel
            if not path.is_file():
                problems.append(f"{_rel(root, path)}: missing door model")
                continue
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as exc:
                problems.append(f"{_rel(root, path)}: invalid JSON ({exc})")
                continue
            got_bottom = data.get("textures", {}).get("bottom")
            got_top = data.get("textures", {}).get("top")
            if got_bottom != f"{modid}:block/{bottom_ref}":
                problems.append(f"{_rel(root, path)}: bottom -> {got_bottom!r} "
                                f"(expected {modid}:block/{bottom_ref})")
            if got_top != f"{modid}:block/{top_ref}":
                problems.append(f"{_rel(root, path)}: top -> {got_top!r} "
                                f"(expected {modid}:block/{top_ref})")
    return problems


# --- tag / item-texture expectations (B3 write_tags contract, §9.3) ----------------------
MC_TAGS_REL = Path("src/main/resources/data/minecraft/tags")

_TOOL_TAG_FILES = {
    "pickaxe": MC_TAGS_REL / "block/mineable/pickaxe.json",
    "axe": MC_TAGS_REL / "block/mineable/axe.json",
    "shovel": MC_TAGS_REL / "block/mineable/shovel.json",
}
_NEEDS_TAG_FILES = {
    "stone": MC_TAGS_REL / "block/needs_stone_tool.json",
    "iron": MC_TAGS_REL / "block/needs_iron_tool.json",
    "diamond": MC_TAGS_REL / "block/needs_diamond_tool.json",
}


def _spec_tag_files() -> dict:
    """spec ``tags`` key -> the vanilla tag file(s) ``write_tags`` must add it to."""
    table = {
        "walls": (MC_TAGS_REL / "block/walls.json",),
        "logs": (MC_TAGS_REL / "block/logs.json", MC_TAGS_REL / "item/logs.json"),
    }
    for key in ("planks", "wooden_stairs", "wooden_slabs", "wooden_fences",
                "fence_gates", "wooden_doors", "wooden_trapdoors",
                "wooden_pressure_plates", "wooden_buttons"):
        table[key] = (MC_TAGS_REL / "block" / f"{key}.json",
                      MC_TAGS_REL / "item" / f"{key}.json")
    return table


_SPEC_TAG_FILES = _spec_tag_files()


# --- hand-written tag members the pipeline never generates (retention guard) ---------------
# The generator only ever *merges* ids into these tag files; it never removes. The ids below
# are the ones already present in the tags that the pipeline is **not** responsible for
# generating -- the pre-existing, hand-maintained frost / permafrost / blizzard families and
# the frost flora. They are pinned here as a frozen baseline so a regeneration (or a bad
# manual edit) that silently wipes one is caught instead of shipping a shorter tag.
#
# This is deliberately a *closed* allow-list: only these known hand-written ids are asserted,
# never "any unknown id". Ordinary tag additions -- or additions of brand-new hand-written ids
# -- therefore stay legal. The only way to trip it is to delete one of these specific ids; if
# such a deletion is intentional, remove the id from this table in the same change.
_HANDWRITTEN_TAG_MEMBERS = {
    MC_TAGS_REL / "block/fence_gates.json": {
        "weather_realm:frost_fence_gate",
    },
    MC_TAGS_REL / "block/flowers.json": {
        "weather_realm:frost_flower", "weather_realm:glacier_bloom", "weather_realm:tall_frost_flower",
    },
    MC_TAGS_REL / "block/leaves.json": {
        "weather_realm:frost_leaves",
    },
    MC_TAGS_REL / "block/logs.json": {
        "weather_realm:frost_log", "weather_realm:frost_wood", "weather_realm:stripped_frost_log",
        "weather_realm:stripped_frost_wood",
    },
    MC_TAGS_REL / "block/mineable/axe.json": {
        "weather_realm:frost_button", "weather_realm:frost_door", "weather_realm:frost_fence",
        "weather_realm:frost_fence_gate", "weather_realm:frost_log", "weather_realm:frost_planks",
        "weather_realm:frost_pressure_plate", "weather_realm:frost_slab", "weather_realm:frost_stairs",
        "weather_realm:frost_trapdoor", "weather_realm:frost_wood", "weather_realm:stripped_frost_log",
        "weather_realm:stripped_frost_wood",
    },
    MC_TAGS_REL / "block/mineable/hoe.json": {
        "weather_realm:frost_leaves",
    },
    MC_TAGS_REL / "block/mineable/pickaxe.json": {
        "weather_realm:blizzard_crystal_block", "weather_realm:deep_permafrost", "weather_realm:deep_permafrost_blizzard_crystal_ore",
        "weather_realm:deep_permafrost_coal_ore", "weather_realm:deep_permafrost_copper_ore", "weather_realm:deep_permafrost_diamond_ore",
        "weather_realm:deep_permafrost_emerald_ore", "weather_realm:deep_permafrost_gold_ore", "weather_realm:deep_permafrost_iron_ore",
        "weather_realm:deep_permafrost_lapis_ore", "weather_realm:deep_permafrost_redstone_ore", "weather_realm:permafrost",
        "weather_realm:permafrost_blizzard_crystal_ore", "weather_realm:permafrost_coal_ore", "weather_realm:permafrost_copper_ore",
        "weather_realm:permafrost_diamond_ore", "weather_realm:permafrost_emerald_ore", "weather_realm:permafrost_gold_ore",
        "weather_realm:permafrost_iron_ore", "weather_realm:permafrost_lapis_ore", "weather_realm:permafrost_redstone_ore",
    },
    MC_TAGS_REL / "block/needs_diamond_tool.json": {
        "weather_realm:deep_permafrost_blizzard_crystal_ore", "weather_realm:permafrost_blizzard_crystal_ore",
    },
    MC_TAGS_REL / "block/needs_iron_tool.json": {
        "weather_realm:deep_permafrost_diamond_ore", "weather_realm:deep_permafrost_emerald_ore", "weather_realm:deep_permafrost_gold_ore",
        "weather_realm:deep_permafrost_redstone_ore", "weather_realm:permafrost_diamond_ore", "weather_realm:permafrost_emerald_ore",
        "weather_realm:permafrost_gold_ore", "weather_realm:permafrost_redstone_ore",
    },
    MC_TAGS_REL / "block/needs_stone_tool.json": {
        "weather_realm:deep_permafrost_copper_ore", "weather_realm:deep_permafrost_iron_ore", "weather_realm:deep_permafrost_lapis_ore",
        "weather_realm:permafrost_copper_ore", "weather_realm:permafrost_iron_ore", "weather_realm:permafrost_lapis_ore",
    },
    MC_TAGS_REL / "block/planks.json": {
        "weather_realm:frost_planks",
    },
    MC_TAGS_REL / "block/small_flowers.json": {
        "weather_realm:frost_flower", "weather_realm:glacier_bloom",
    },
    MC_TAGS_REL / "block/tall_flowers.json": {
        "weather_realm:tall_frost_flower",
    },
    MC_TAGS_REL / "block/wooden_buttons.json": {
        "weather_realm:frost_button",
    },
    MC_TAGS_REL / "block/wooden_doors.json": {
        "weather_realm:frost_door",
    },
    MC_TAGS_REL / "block/wooden_fences.json": {
        "weather_realm:frost_fence",
    },
    MC_TAGS_REL / "block/wooden_pressure_plates.json": {
        "weather_realm:frost_pressure_plate",
    },
    MC_TAGS_REL / "block/wooden_slabs.json": {
        "weather_realm:frost_slab",
    },
    MC_TAGS_REL / "block/wooden_stairs.json": {
        "weather_realm:frost_stairs",
    },
    MC_TAGS_REL / "block/wooden_trapdoors.json": {
        "weather_realm:frost_trapdoor",
    },
    MC_TAGS_REL / "block/wool.json": {
        "weather_realm:frost_wool",
    },
    MC_TAGS_REL / "item/fence_gates.json": {
        "weather_realm:frost_fence_gate",
    },
    MC_TAGS_REL / "item/logs.json": {
        "weather_realm:frost_log", "weather_realm:frost_wood", "weather_realm:stripped_frost_log",
        "weather_realm:stripped_frost_wood",
    },
    MC_TAGS_REL / "item/planks.json": {
        "weather_realm:frost_planks",
    },
    MC_TAGS_REL / "item/wooden_buttons.json": {
        "weather_realm:frost_button",
    },
    MC_TAGS_REL / "item/wooden_doors.json": {
        "weather_realm:frost_door",
    },
    MC_TAGS_REL / "item/wooden_fences.json": {
        "weather_realm:frost_fence",
    },
    MC_TAGS_REL / "item/wooden_pressure_plates.json": {
        "weather_realm:frost_pressure_plate",
    },
    MC_TAGS_REL / "item/wooden_slabs.json": {
        "weather_realm:frost_slab",
    },
    MC_TAGS_REL / "item/wooden_stairs.json": {
        "weather_realm:frost_stairs",
    },
    MC_TAGS_REL / "item/wooden_trapdoors.json": {
        "weather_realm:frost_trapdoor",
    },
}


def expected_tag_membership(specs) -> dict:
    """Map each expected tag file (repo-relative) to the block ids it must contain.

    Derived purely from the family spec fields (``tool`` / ``needs`` / ``tags``),
    mirroring ``gen_block_assets.write_tags``.
    """
    out: dict = {}

    def add(path: Path, name: str) -> None:
        out.setdefault(path, set()).add(name)

    for spec in specs:
        name = spec["name"]
        tool = spec.get("tool")
        if tool in _TOOL_TAG_FILES:
            add(_TOOL_TAG_FILES[tool], name)
        needs = spec.get("needs")
        if needs in _NEEDS_TAG_FILES:
            add(_NEEDS_TAG_FILES[needs], name)
        for tag in spec.get("tags", []):
            for path in _SPEC_TAG_FILES.get(tag, ()):
                add(path, name)
    return out


def _tag_values(data) -> set:
    values: set = set()
    for entry in data.get("values", []):
        if isinstance(entry, str):
            values.add(entry)
        elif isinstance(entry, dict) and isinstance(entry.get("id"), str):
            values.add(entry["id"])
    return values


def tag_problems(root: Path, expected: dict, modid: str) -> list:
    """Every expected id must be present in its tag file; missing -> problem."""
    problems: list = []
    for rel, ids in sorted(expected.items()):
        path = root / rel
        if not path.is_file():
            problems.append(
                f"{_rel(root, path)}: missing tag file (expected {len(ids)} ids)")
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            problems.append(f"{_rel(root, path)}: invalid JSON ({exc})")
            continue
        values = _tag_values(data)
        for bid in sorted(ids):
            ref = f"{modid}:{bid}"
            if ref not in values:
                problems.append(f"{_rel(root, path)}: missing '{ref}'")
    return problems


def handwritten_tag_problems(root: Path) -> list:
    """Pinned hand-written tag members must still be present (retention guard).

    Complements ``tag_problems`` (which only checks that *expected* ids are present) by
    asserting the ids the pipeline never generates were not wiped by a regeneration.
    """
    problems: list = []
    for rel, refs in sorted(_HANDWRITTEN_TAG_MEMBERS.items()):
        path = root / rel
        if not path.is_file():
            problems.append(
                f"{_rel(root, path)}: missing tag file (expected {len(refs)} hand-written ids)")
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            problems.append(f"{_rel(root, path)}: invalid JSON ({exc})")
            continue
        values = _tag_values(data)
        for ref in sorted(refs):
            if ref not in values:
                problems.append(
                    f"{_rel(root, path)}: hand-written '{ref}' missing "
                    f"(regeneration wiped it?)")
    return problems


def expected_item_textures(specs, modid: str) -> dict:
    """out_tex (no extension) -> owning block id for every required item sprite.

    Covers explicit ``item_textures`` declarations and any ``("generated", ...)``
    item model whose layer0 points into ``<modid>:item/``.
    """
    expected: dict = {}
    for spec in specs:
        name = spec["name"]
        for out_tex, _src, _profile in spec.get("item_textures", []):
            expected.setdefault(out_tex, name)
        kind, ref = spec.get("item", (None, None))
        if (kind == "generated" and isinstance(ref, str)
                and ref.startswith(f"{modid}:item/")):
            expected.setdefault(ref.split(":", 1)[1][len("item/"):], name)
    return expected


def item_texture_problems(root: Path, expected: dict, modid: str) -> list:
    problems: list = []
    for out_tex, owner in sorted(expected.items()):
        rel = ASSETS_REL / modid / "textures" / "item" / f"{out_tex}.png"
        if not (root / rel).is_file():
            problems.append(
                f"{_rel(root, root / rel)}: missing item texture for '{owner}'")
    return problems


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
        building_specs = bd.all_building_specs()
        check_known_shapes(building_specs)
        java_ids, scanned = java_literal_ids(root)
        tabs = tab_ids(root)
        langs = lang_keys(root, modid)
        layer_word_diffs = layer_word_problems(root)
    except (ParseError, json.JSONDecodeError, bd.FamilyDataError) as exc:
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
        for fname, keys in langs.items():
            key = f"block.{modid}.{bid}"
            if key not in keys:
                missing_lang.append(f"{fname}: {key}")

    # Exact per-shape block-model files (no prefix globs: a missing base model must
    # not be masked by a sibling such as ``<name>_stairs.json``).
    missing_resources += block_model_problems(root, building_specs, modid)

    missing_tags = tag_problems(root, expected_tag_membership(building_specs), modid)
    missing_handwritten_tags = handwritten_tag_problems(root)
    door_tex_problems = door_texture_problems(root, building_specs, modid)
    missing_item_tex = item_texture_problems(
        root, expected_item_textures(building_specs, modid), modid)

    print(f"[build] table ids    : {len(expected)} (distinct {len(counts)})")
    print(f"[build] java ids     : {len(java_ids)} (from {', '.join(scanned)})")
    print(f"[build] tab ids      : {len(tabs)}")
    print(f"[build] lang files   : {', '.join(langs)}")
    print(f"[build] layerWord map : {len(bd._ECO_BASES)} eco themes vs "
          f"ModBuildingBlocks.layerWord ({len(layer_word_diffs)} diff)")

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
    if missing_tags:
        problems.append(("标签缺失", "building id missing from vanilla tag", missing_tags))
    if missing_handwritten_tags:
        problems.append(("手写标签被删", "hand-written tag member missing", missing_handwritten_tags))
    if door_tex_problems:
        problems.append(("门贴图错误", "door top/bottom texture not distinct/wrong", door_tex_problems))
    if missing_item_tex:
        problems.append(("物品贴图缺失", "missing textures/item png", missing_item_tex))
    if layer_word_diffs:
        problems.append(("生态映射漂移", "Java layerWord vs Python _ECO_BASES", layer_word_diffs))

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
