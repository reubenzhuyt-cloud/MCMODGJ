#!/usr/bin/env python3
"""双向断言：建材数据表 ↔ 生成资源 ↔ Java 注册（设计文档 §7.4）。

本脚本**纯静态**：不加载 Minecraft、不启动游戏；除生成贴图配色校验需要 Pillow
（与 `gen_block_assets` 依赖一致）外只用标准库。它把三处来源拆开解析，再做双向对称比较：

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
import colorsys
import json
import math
import os
import re
import sys
import zipfile
from collections import Counter
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # pragma: no cover - the texture-colour assertion needs it
    Image = None

# 让中文诊断在 Windows 控制台也按 UTF-8 输出，避免 GBK 编码报错。
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(encoding="utf-8", errors="replace")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import biome_data as bd  # noqa: E402
import gen_block_assets as gba  # noqa: E402

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


# --- Task A: stone derived pieces must match their own base colour -------------------------
# Every stone derived sprite is recoloured from a *deepslate* source; the old recolor kept
# the source's saturation/value, so the pieces came out as dark grey while the base rock is
# pale blue / sand. The generator now anchors them to the base texture's mean colour, and
# this assertion pins that: each derived texture's opaque-pixel mean HSV must stay within
# (dH, dS, dV) of its base, *and* the stone relief must not collapse to a flat block.
# Every theme/depth group that yields 0 sprites is a hard failure, never a green.
STONE_MAX_DH = 0.04
STONE_MAX_DS = 0.08
STONE_MAX_DV = 0.12
# Float guard for boundary values (e.g. 0.04000000000000004 must not fail). This does NOT
# relax the limits: 0.04 / 0.08 / 0.12 remain the boundary.
COLOR_EPS = 1e-6
# Pattern floor: opaque-pixel value sigma and distinct-colour count. Pre-fix values dipped to
# sigma ~0.041; repaired sprites sit at ~0.16-0.21 with 4-8 distinct colours, so both floors
# are safe for the current assets and only catch an actual collapse to a solid block.
STONE_MIN_VALUE_SIGMA = 0.05
STONE_MIN_UNIQUE_COLORS = 4


def _load_rgba(path: Path, root: Path, problems: list):
    if not path.is_file():
        problems.append(f"{_rel(root, path)}: missing texture")
        return None
    try:
        return Image.open(path).convert("RGBA")
    except Exception as exc:  # pragma: no cover - corrupt png
        problems.append(f"{_rel(root, path)}: cannot read ({exc})")
        return None


def _mean_hsv(img) -> tuple | None:
    """Opaque-pixel mean (hue circular, sat, val); ``None`` when fully transparent."""
    px = img.load()
    sx = sy = ss = sv = 0.0
    n = 0
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            sx += math.cos(2.0 * math.pi * h)
            sy += math.sin(2.0 * math.pi * h)
            ss += s
            sv += v
            n += 1
    if n == 0:
        return None
    return (math.atan2(sy, sx) / (2.0 * math.pi)) % 1.0, ss / n, sv / n


def _hue_delta(a: float, b: float) -> float:
    d = abs(a - b) % 1.0
    return min(d, 1.0 - d)


def _value_sigma(img):
    """Stddev of opaque-pixel value (0..1); ``None`` when fully transparent."""
    px = img.load()
    vals = []
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            vals.append(colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)[2])
    if not vals:
        return None
    mean = sum(vals) / len(vals)
    return math.sqrt(sum((v - mean) ** 2 for v in vals) / len(vals))


def _unique_color_count(img):
    """Number of distinct opaque RGB colours; ``None`` when fully transparent."""
    px = img.load()
    colours = set()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a != 0:
                colours.add((r, g, b))
    return len(colours) if colours else None


def stone_pattern_problem(name: str, img) -> str | None:
    """M2: a problem string when a sprite's relief has collapsed to a flat block, else None."""
    sigma = _value_sigma(img)
    colours = _unique_color_count(img)
    if sigma is None or colours is None:
        return f"{name}: no opaque pixels"
    if sigma < STONE_MIN_VALUE_SIGMA - COLOR_EPS:
        return (f"{name}: pattern collapsed, opaque value sigma={sigma:.4f} "
                f"< {STONE_MIN_VALUE_SIGMA}")
    if colours < STONE_MIN_UNIQUE_COLORS:
        return (f"{name}: pattern collapsed, unique colours={colours} "
                f"< {STONE_MIN_UNIQUE_COLORS}")
    return None


def stone_color_problems(root: Path, modid: str) -> tuple:
    """Task A: derived stone sprites vs base mean HSV + pattern-collapse floor.

    Returns ``(problems, summary)``. ``summary`` reports ``compared`` (total sprites),
    ``over`` (colour violations), ``pattern_over``, ``per_base`` counts and the observed
    minima. Any theme/depth group that yields 0 sprites raises (a partially deleted data
    table must not pass on the remaining groups).
    """
    if Image is None:
        raise ParseError("石材配色校验需要 Pillow（pip install Pillow）")
    tex_dir = ASSETS_REL / modid / "textures/block"
    problems: list = []
    compared = 0
    over = 0
    pattern_over = 0
    per_base: dict = {}
    min_sigma = None
    min_colours = None
    for fam in bd.STONE_BASES:
        for is_deep in (False, True):
            base_id = fam["deep"] if is_deep else fam["name"]
            base_path = root / tex_dir / f"{base_id}.png"
            base_img = _load_rgba(base_path, root, problems)
            if base_img is None:
                continue
            base_mean = _mean_hsv(base_img)
            if base_mean is None:
                problems.append(f"{_rel(root, base_path)}: no opaque pixels")
                continue
            group_n = 0
            for spec in bd.stone_specs(fam):
                name = spec["name"]
                deep_spec = name == fam["deep"] or name.startswith(fam["deep"] + "_")
                if deep_spec != is_deep:
                    continue
                for out_tex, _src, _profile in spec.get("textures", []):
                    group_n += 1
                    compared += 1
                    path = root / tex_dir / f"{out_tex}.png"
                    img = _load_rgba(path, root, problems)
                    if img is None:
                        continue
                    m = _mean_hsv(img)
                    if m is None:
                        problems.append(f"{_rel(root, path)}: no opaque pixels")
                        continue
                    dh = _hue_delta(m[0], base_mean[0])
                    ds = abs(m[1] - base_mean[1])
                    dv = abs(m[2] - base_mean[2])
                    if (dh > STONE_MAX_DH + COLOR_EPS
                            or ds > STONE_MAX_DS + COLOR_EPS
                            or dv > STONE_MAX_DV + COLOR_EPS):
                        over += 1
                        problems.append(
                            f"{out_tex}.png: meanHSV=({m[0]:.3f},{m[1]:.3f},{m[2]:.3f}) vs "
                            f"base {base_id}=({base_mean[0]:.3f},{base_mean[1]:.3f},"
                            f"{base_mean[2]:.3f}) -> dH={dh:.4f} dS={ds:.4f} dV={dv:.4f} "
                            f"(max dH<={STONE_MAX_DH} dS<={STONE_MAX_DS} dV<={STONE_MAX_DV})")
                    pat = stone_pattern_problem(out_tex, img)
                    if pat is not None:
                        pattern_over += 1
                        problems.append(pat)
                    sigma = _value_sigma(img)
                    colours = _unique_color_count(img)
                    if sigma is not None:
                        min_sigma = sigma if min_sigma is None else min(min_sigma, sigma)
                    if colours is not None:
                        min_colours = colours if min_colours is None else min(min_colours, colours)
            per_base[base_id] = group_n
            if group_n == 0:
                raise ParseError(
                    f"石材配色校验: 基材 {base_id} 解析到 0 张派生贴图（数据表被删小了？）")
    if compared == 0:
        raise ParseError("石材配色校验解析到 0 组派生贴图（数据表结构变了？）")
    summary = {"compared": compared, "over": over, "pattern_over": pattern_over,
               "per_base": per_base, "min_sigma": min_sigma, "min_colours": min_colours}
    return problems, summary


# --- Task B: blockstate variants may only use a shape's whitelisted properties -------------
# A generator bug once emitted ``layers=<n>,waterlogged=<bool>`` for vanilla ``SnowLayerBlock``,
# which has no ``waterlogged`` property -> every layer blockstate was invalid and the blocks
# rendered invisible. Pin the legal property keys per shape (a superset of the keys vanilla
# actually lists is fine; anything outside it is a bug). ``layer`` -> ``{layers}`` only.
_BLOCKSTATE_ALLOWED_PROPS = {
    "cross": frozenset(),
    "leaves": frozenset(),
    "cube_all": frozenset(),
    "glass_block": frozenset(),
    "pillar": frozenset({"axis"}),
    "stairs": frozenset({"facing", "half", "shape", "waterlogged"}),
    "slab": frozenset({"type", "waterlogged"}),
    "wall": frozenset({"up", "north", "east", "south", "west", "waterlogged"}),
    "fence": frozenset({"north", "east", "south", "west", "waterlogged"}),
    "fence_gate": frozenset({"facing", "in_wall", "open", "powered"}),
    "door": frozenset({"facing", "half", "hinge", "open", "powered"}),
    "trapdoor": frozenset({"facing", "half", "open", "powered", "waterlogged"}),
    "button": frozenset({"face", "facing", "powered"}),
    "pressure_plate": frozenset({"powered"}),
    "lantern": frozenset({"hanging", "waterlogged"}),
    "pane": frozenset({"north", "east", "south", "west", "waterlogged"}),
    "chain": frozenset({"axis", "waterlogged"}),
    "cluster": frozenset({"facing", "waterlogged"}),
    "layer": frozenset({"layers"}),
    "spike": frozenset({"thickness", "vertical_direction", "waterlogged"}),
}


def _variant_prop_keys(key: str) -> set:
    keys: set = set()
    for part in key.split(","):
        part = part.strip()
        if "=" in part:
            keys.add(part.split("=", 1)[0])
    return keys


def _when_prop_keys(when) -> set:
    keys: set = set()
    if isinstance(when, str):
        return _variant_prop_keys(when)
    if isinstance(when, dict):
        for key, value in when.items():
            if key in ("OR", "AND") and isinstance(value, list):
                for sub in value:
                    keys |= _when_prop_keys(sub)
            else:
                keys.add(key)
    return keys


def _blockstate_prop_keys(data: dict) -> set:
    keys: set = set()
    variants = data.get("variants")
    if isinstance(variants, dict):
        for variant_key in variants:
            keys |= _variant_prop_keys(variant_key)
    multipart = data.get("multipart")
    if isinstance(multipart, list):
        for part in multipart:
            if isinstance(part, dict) and "when" in part:
                keys |= _when_prop_keys(part["when"])
    return keys


def blockstate_property_problems(root: Path, specs, modid: str) -> list:
    """Task B: a generated blockstate may only reference its shape's whitelisted properties."""
    problems: list = []
    bs_dir = ASSETS_REL / modid / "blockstates"
    checked = 0
    for spec in specs:
        name = spec["name"]
        shape = real_shape(spec)
        allowed = _BLOCKSTATE_ALLOWED_PROPS.get(shape)
        if allowed is None:
            problems.append(f"{name}: shape {shape!r} missing from blockstate property whitelist")
            continue
        path = root / bs_dir / f"{name}.json"
        if not path.is_file():
            problems.append(f"{_rel(root, path)}: missing blockstate")
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            problems.append(f"{_rel(root, path)}: invalid JSON ({exc})")
            continue
        if not isinstance(data.get("variants"), dict) and not isinstance(data.get("multipart"), list):
            problems.append(f"{_rel(root, path)}: no variants/multipart")
            continue
        checked += 1
        illegal = sorted(_blockstate_prop_keys(data) - allowed)
        if illegal:
            problems.append(
                f"{_rel(root, path)}: variant uses non-whitelisted propert"
                f"{'ies' if len(illegal) > 1 else 'y'} {illegal} "
                f"(shape={shape!r} allowed={sorted(allowed)})")
    if checked == 0:
        raise ParseError("blockstate 属性白名单校验解析到 0 个 blockstate（数据表结构变了？）")
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


# --- leaf loot + sapling model assertions -------------------------------------------------
# A `table_bonus` / `random_chance` condition attached to a *function* is inert: the function
# still runs unconditionally, which is exactly the old "leaves always drop a stick/sapling" bug.
# Those conditions only do anything on an *entry*, so any occurrence under functions[*].conditions
# means the loot table is broken even though it still parses.
INERT_FUNCTION_CONDITIONS = ("minecraft:table_bonus", "minecraft:random_chance")
FORTUNE_TABLE_BONUS = "minecraft:table_bonus"


def _iter_loot_entries(node):
    """Yield every loot entry dict (a dict carrying a string ``name``)."""
    if isinstance(node, dict):
        if isinstance(node.get("name"), str):
            yield node
        for value in node.values():
            yield from _iter_loot_entries(value)
    elif isinstance(node, list):
        for value in node:
            yield from _iter_loot_entries(value)


def _scan_function_conditions(node, path, out) -> None:
    if isinstance(node, dict):
        funcs = node.get("functions")
        if isinstance(funcs, list):
            for fi, func in enumerate(funcs):
                if not isinstance(func, dict):
                    continue
                for ci, cond in enumerate(func.get("conditions") or []):
                    if isinstance(cond, dict) and cond.get("condition") in INERT_FUNCTION_CONDITIONS:
                        out.append(f"{path}/functions[{fi}]/conditions[{ci}]"
                                   f" = {cond.get('condition')}")
        for key, value in node.items():
            _scan_function_conditions(value, f"{path}/{key}", out)
    elif isinstance(node, list):
        for index, value in enumerate(node):
            _scan_function_conditions(value, f"{path}[{index}]", out)


def function_condition_problems(root: Path, modid: str):
    """Guard every loot table: no function may carry an inert table_bonus/random_chance condition."""
    loot_dir = root / DATA_REL / modid / "loot_table" / "blocks"
    files = sorted(loot_dir.glob("*.json"))
    if not files:
        raise ParseError(f"未找到任何掉落表: {loot_dir}/*.json")
    problems: list = []
    for path in files:
        data = json.loads(path.read_text(encoding="utf-8"))
        hits: list = []
        _scan_function_conditions(data, "", hits)
        for hit in hits:
            problems.append(f"{_rel(root, path)}: {hit.lstrip('/')}")
    return problems, len(files)


def _is_shears_silk_anyof(cond) -> bool:
    if not isinstance(cond, dict) or cond.get("condition") != "minecraft:any_of":
        return False
    has_shears = has_silk = False
    for term in cond.get("terms", []) or []:
        if not isinstance(term, dict):
            continue
        pred = term.get("predicate", {}) or {}
        if pred.get("items") == "minecraft:shears":
            has_shears = True
        enchants = (pred.get("predicates") or {}).get("minecraft:enchantments") or []
        for ench in enchants:
            if isinstance(ench, dict) and ench.get("enchantments") == "minecraft:silk_touch":
                has_silk = True
    return has_shears and has_silk


def _entry_table_bonus(entry):
    """Return the entry-level fortune table_bonus chances, or None."""
    for cond in entry.get("conditions", []) or []:
        if (isinstance(cond, dict) and cond.get("condition") == FORTUNE_TABLE_BONUS
                and cond.get("enchantment") == "minecraft:fortune"):
            return cond.get("chances")
    return None


def _is_degenerate_chances(chances) -> bool:
    """A length-1 or all-1.0 chance array is the 'guaranteed drop' smell."""
    return bool(
        isinstance(chances, list) and chances
        and (len(chances) == 1
             or all(isinstance(c, (int, float)) and abs(c - 1.0) < 1e-9 for c in chances)))


def _has_set_count_uniform_1_2(functions) -> bool:
    for func in functions or []:
        if not isinstance(func, dict) or func.get("function") != "minecraft:set_count":
            continue
        count = func.get("count")
        if (isinstance(count, dict) and count.get("type") == "minecraft:uniform"
                and float(count.get("min", -1)) == 1.0 and float(count.get("max", -1)) == 2.0):
            return True
    return False


def _vanilla_leaf_reference(zf):
    """Read the authoritative sapling / stick fortune chances from the client jar's oak leaves."""
    path = "data/minecraft/loot_table/blocks/oak_leaves.json"
    try:
        data = json.loads(zf.read(path).decode("utf-8"))
    except KeyError as exc:
        raise ParseError(f"client jar 缺少 {path}（原版树叶掉落结构不可得）") from exc
    ref = {"sapling_chances": None, "stick_chances": None}
    for entry in _iter_loot_entries(data):
        name = entry["name"]
        for cond in entry.get("conditions", []) or []:
            if not (isinstance(cond, dict) and cond.get("condition") == FORTUNE_TABLE_BONUS):
                continue
            if name == "minecraft:stick":
                ref["stick_chances"] = cond.get("chances")
            elif name.endswith("_sapling"):
                ref["sapling_chances"] = cond.get("chances")
    if ref["sapling_chances"] is None or ref["stick_chances"] is None:
        raise ParseError(f"无法从 {path} 读取树苗/木棍 table_bonus 概率")
    return ref


def leaves_structure_problems(root: Path, modid: str, ref):
    """Assert the three mod leaf tables mirror vanilla's three-pool oak-leaves shape."""
    loot_dir = root / DATA_REL / modid / "loot_table" / "blocks"
    files = sorted(loot_dir.glob("*_leaves.json"))
    if not files:
        raise ParseError(f"未找到任何树叶掉落表: {loot_dir}/*_leaves.json")
    problems: list = []
    for path in files:
        rel = _rel(root, path)
        data = json.loads(path.read_text(encoding="utf-8"))
        prefix = path.stem[: -len("_leaves")]
        expect_leaf = f"{modid}:{path.stem}"
        expect_sapling = f"{modid}:{prefix}_sapling"
        pools = data.get("pools", []) or []

        leaf_child = sapling_child = None
        for entry in _iter_loot_entries(data):
            if entry.get("name") == expect_leaf:
                leaf_child = entry
            elif entry.get("name") == expect_sapling:
                sapling_child = entry

        if leaf_child is None:
            problems.append(f"{rel}: 缺少本体 entry '{expect_leaf}'")
        else:
            conds = leaf_child.get("conditions", []) or []
            if len(conds) != 1 or not _is_shears_silk_anyof(conds[0]):
                problems.append(f"{rel}: 本体 entry 缺少 entry 级 any_of[shears, silk_touch]")

        if sapling_child is None:
            problems.append(f"{rel}: 缺少该主题树苗 entry '{expect_sapling}'")
        else:
            chances = _entry_table_bonus(sapling_child)
            if chances is None:
                problems.append(f"{rel}: 树苗 entry 缺少 entry 级 table_bonus")
            else:
                if chances != ref["sapling_chances"]:
                    problems.append(
                        f"{rel}: 树苗 chances={chances} != 原版 {ref['sapling_chances']}")
                if _is_degenerate_chances(chances):
                    problems.append(f"{rel}: 树苗 chances 疑似必掉 {chances}")
            if not any(c.get("condition") == "minecraft:survives_explosion"
                       for c in sapling_child.get("conditions", []) or []):
                problems.append(f"{rel}: 树苗 entry 缺少 survives_explosion")

        stick_pool = stick_entry = None
        for pool in pools:
            for entry in pool.get("entries", []) or []:
                if entry.get("name") == "minecraft:stick":
                    stick_pool = pool
                    stick_entry = entry
        if stick_pool is None:
            problems.append(f"{rel}: 缺少木棍池")
        else:
            pool_conds = stick_pool.get("conditions", []) or []
            ok_inverted = (len(pool_conds) == 1
                           and pool_conds[0].get("condition") == "minecraft:inverted"
                           and _is_shears_silk_anyof(pool_conds[0].get("term")))
            if not ok_inverted:
                problems.append(f"{rel}: 木棍池缺少池级 inverted(any_of[shears, silk_touch])")
            chances = _entry_table_bonus(stick_entry)
            if chances is None:
                problems.append(f"{rel}: 木棍 entry 缺少 entry 级 table_bonus")
            else:
                if chances != ref["stick_chances"]:
                    problems.append(f"{rel}: 木棍 chances={chances} != 原版 {ref['stick_chances']}")
                if _is_degenerate_chances(chances):
                    problems.append(f"{rel}: 木棍 chances 疑似必掉 {chances}")
            funcs = stick_entry.get("functions", []) or []
            if not _has_set_count_uniform_1_2(funcs):
                problems.append(f"{rel}: 木棍 functions 缺少 set_count uniform 1..2")
            if not any(f.get("function") == "minecraft:explosion_decay" for f in funcs):
                problems.append(f"{rel}: 木棍 functions 缺少 explosion_decay")
    return problems, len(files)


def sapling_model_problems(root: Path, modid: str):
    """Resource-layer guard: every *_sapling block model is a cutout `cross`."""
    assets = root / ASSETS_REL / modid
    blockstates = sorted((assets / "blockstates").glob("*_sapling.json"))
    if not blockstates:
        raise ParseError(f"未找到任何树苗 blockstate: {assets / 'blockstates'}/*_sapling.json")
    problems: list = []
    for blockstate in blockstates:
        name = blockstate.stem
        model_path = assets / "models/block" / f"{name}.json"
        if not model_path.is_file():
            problems.append(f"{_rel(root, model_path)}: 缺少方块模型")
            continue
        model = json.loads(model_path.read_text(encoding="utf-8"))
        if model.get("parent") != "minecraft:block/cross":
            problems.append(f"{_rel(root, model_path)}: parent != minecraft:block/cross")
        if model.get("render_type") != "minecraft:cutout":
            problems.append(f"{_rel(root, model_path)}: render_type != minecraft:cutout")
        item_path = assets / "models/item" / f"{name}.json"
        if not item_path.is_file():
            problems.append(f"{_rel(root, item_path)}: 缺少物品模型")
    return problems, len(blockstates)


# --- alpha / non-occluding guard ------------------------------------------------------------
# A model that references a texture with alpha=0 pixels must be drawn cutout AND its block must
# not occlude neighbour faces. An occluding full cube culls the touching neighbour face; with
# alpha holes you would see straight through the hole to that culled face (the arid-wood
# see-through regression). Fully opaque solid cubes must therefore stay fully opaque and must NOT
# declare cutout.
#
# Ground truth for "does not occlude" is the Java registration: BlockBehaviour.Properties
# .noOcclusion() clears canOcclude (BlockBehaviour.java:1165-1168) and .noCollission() clears it
# too (1159-1162). Family ids are built by concatenation (``prefix + "_planks"``), so a literal id
# regex cannot recover them; the concrete ids are therefore listed explicitly below. The list
# cannot go stale silently: every entry must exist among the emitted specs, and the full-cube
# weathered entries are additionally pinned to the Java props factories (aridWoodPillar /
# aridPlanks) -- deleting a real .noOcclusion() makes this guard FAIL instead of silently passing.
_SOLID_CUBE_SHAPES = frozenset({"cube_all", "pillar"})
# Inherently transparent shapes whose vanilla-template models carry no render_type and rely on the
# engine's default render layer (glass panes, chains, lanterns, glass blocks, doors with a vanilla
# glass window). They are NOT opaque cubes, so they cannot cause the void bug; exempt from the
# explicit-cutout rule so pre-existing hand-authored/vanilla-template models keep working
# unchanged. NOTE: this is a deliberate, documented exemption -- everything else (cross / leaves /
# cluster / spike and all derived solid shapes) IS enforced.
_CUTOUT_EXEMPT_SHAPES = frozenset({"door", "pane", "chain", "lantern", "glass_block"})

# Every block id whose emitted models reference at least one alpha=0 texture. All of them are
# registered non-occluding (noOcclusion() or noCollission()); anything else with an alpha sprite
# is rejected by the guard below. Concrete ids (dynamic family ids expanded).
_ALPHA_ALLOWED_BLOCKS = frozenset({
    # 风化木 / weathered (arid) wood -- restored alpha=0 holes, registered noOcclusion
    "arid_bush", "arid_button", "arid_door", "arid_fence", "arid_fence_gate", "arid_leaves",
    "arid_log", "arid_planks", "arid_pressure_plate", "arid_slab", "arid_stairs", "arid_trapdoor",
    "arid_wood", "stripped_arid_log", "stripped_arid_wood",
    # 火主题 / blaze theme: glass family, lantern, chain, eco cluster + spike, cross plants
    "blaze_chain", "blaze_crystal_cluster", "blaze_glass", "blaze_glass_pane", "blaze_grate",
    "blaze_lantern", "blaze_spike",
    "cinder_bloom", "fire_flower", "flame_sprout",
    # 冰主题 / frost theme
    "frost_chain", "frost_crystal_cluster", "frost_glass", "frost_glass_pane", "frost_grate",
    "frost_lantern", "frost_spike",
    # 焦木门上半玻璃窗 + 树叶 / scorched door glass window + leaves
    "scorched_door", "scorched_leaves",
    # 风主题 / wind theme
    "wind_chain", "wind_crystal_cluster", "wind_glass", "wind_glass_pane", "wind_grate",
    "wind_lantern", "wind_spike", "wind_sprout",
    # 风沙植被 / arid cross plants (wind variant)
    "dune_flower",
})

# The weathered full cubes take their non-occlusion from these two ModBlockProperties factories:
#   * aridWoodPillar()  -> arid_log / arid_wood / stripped_arid_log / stripped_arid_wood
#   * aridPlanks()      -> arid_planks (+ stairs/slab/fence/fence_gate via ofFullCopy)
# Pinning the guard to the factory bodies means the "holes + occluding cube" regression cannot
# pass just because the id is still listed.
_ALPHA_JAVA_EVIDENCE = (
    ("ModBlockProperties.java",
     re.compile(r"aridWoodPillar\s*\([^)]*\)\s*\{[^}]*\.noOcclusion\s*\(", re.DOTALL),
     "ModBlockProperties.aridWoodPillar() 缺少 noOcclusion()：arid 原木/木干会遮挡邻面，"
     "镂空贴图将透视"),
    ("ModBlockProperties.java",
     re.compile(r"aridPlanks\s*\([^)]*\)\s*\{[^}]*\.noOcclusion\s*\(", re.DOTALL),
     "ModBlockProperties.aridPlanks() 缺少 noOcclusion()：arid 木板（及其 stairs/slab/fence/"
     "fence_gate 全拷贝派生）会遮挡邻面，镂空贴图将透视"),
)


def _all_asset_specs() -> list:
    """Every block spec the generator emits: building families + covers + per-theme blocks."""
    specs = list(bd.all_building_specs())
    specs += list(gba.cover_specs())
    for theme in bd.THEMES:
        specs += list(gba.block_specs(theme))
    return specs


def _transparent_java_problems(root: Path) -> list:
    """Java evidence that the weathered cube families really are non-occluding."""
    problems: list = []
    for fname, regex, detail in _ALPHA_JAVA_EVIDENCE:
        if not regex.search(_read(root / JAVA_REL / fname)):
            problems.append(f"{fname}: {detail}")
    return problems


def alpha_guard_problems(root: Path, modid: str):
    """Transparency => non-occluding block + cutout; opaque cubes stay opaque.

    Returns ``(problems, summary)``.
    """
    if Image is None:
        raise ParseError("实心立方 alpha 门禁需要 Pillow（pip install Pillow）")
    assets = root / ASSETS_REL / modid
    model_dir = assets / "models/block"
    tex_dir = assets / "textures"
    cache: dict = {}

    def alpha0(texid: str):
        if texid in cache:
            return cache[texid]
        path = tex_dir / f"{texid.split(':', 1)[1]}.png"
        count = None
        if path.is_file():
            im = Image.open(path).convert("RGBA")
            px = im.load()
            count = sum(1 for y in range(im.height) for x in range(im.width) if px[x, y][3] == 0)
        cache[texid] = count
        return count

    problems: list = []
    seen_ids: set = set()
    solid_checked = 0
    transparent_checked = 0
    occludable_transparent = 0
    for spec in _all_asset_specs():
        name = spec["name"]
        seen_ids.add(name)
        shape = real_shape(spec)
        suffixes = _BLOCK_MODEL_SUFFIXES.get(shape)
        if suffixes is None:  # unknown shapes are already a hard failure via check_known_shapes
            continue
        is_solid = shape in _SOLID_CUBE_SHAPES
        for suffix in suffixes:
            model_path = model_dir / f"{name}{suffix}.json"
            if not model_path.is_file():
                continue  # missing models are reported by block_model_problems
            data = json.loads(model_path.read_text(encoding="utf-8"))
            render_type = data.get("render_type")
            tex_ids = sorted({v for v in data.get("textures", {}).values()
                              if isinstance(v, str) and v.startswith(f"{modid}:block/")})
            counts = {t: alpha0(t) for t in tex_ids}
            transparent = any(c for c in counts.values() if c)
            if is_solid:
                solid_checked += 1
            if transparent:
                transparent_checked += 1
                if name not in _ALPHA_ALLOWED_BLOCKS:
                    occludable_transparent += 1
                    problems.append(
                        f"{_rel(root, model_path)}: 引用含 alpha=0 的贴图 {tex_ids}，但方块 "
                        f"{name} 不在非遮挡允许清单（noOcclusion/noCollission）内 "
                        f"-> 邻面被剔除会透视")
                    continue
                if shape not in _CUTOUT_EXEMPT_SHAPES and render_type != "minecraft:cutout":
                    problems.append(
                        f"{_rel(root, model_path)}: 引用含 alpha=0 的贴图 {tex_ids} "
                        f"但 render_type != minecraft:cutout")
            else:
                if is_solid and render_type is not None:
                    # Fully opaque solid cube that still declares cutout -> stray cutout.
                    problems.append(
                        f"{_rel(root, model_path)}: 实心不透明立方体声明了 "
                        f"render_type={render_type!r}（不透明贴图无需 cutout）；请移除")
                if not is_solid:
                    # Non-solid model with no alpha: still part of the transparency surface
                    # (matches the previous round's counter so the count cannot drop).
                    transparent_checked += 1

    stale = sorted(_ALPHA_ALLOWED_BLOCKS - seen_ids)
    if stale:
        problems.append("允许清单 _ALPHA_ALLOWED_BLOCKS 含陈旧项（已无对应方块资源）: "
                        + ", ".join(stale))
    problems.extend(_transparent_java_problems(root))

    if solid_checked == 0:
        raise ParseError("实心立方 alpha 门禁解析到 0 个模型（数据表结构变了？）")
    if transparent_checked == 0:
        raise ParseError("alpha 门禁解析到 0 个含 alpha=0 的模型（贴图/数据表结构变了？）")
    summary = {"solid_checked": solid_checked, "transparent_checked": transparent_checked,
               "occludable_transparent": occludable_transparent, "over": len(problems)}
    return problems, summary


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
        stone_color_diffs, stone_summary = stone_color_problems(root, modid)
        blockstate_prop_diffs = blockstate_property_problems(root, building_specs, modid)
        function_cond_diffs, loot_tables_scanned = function_condition_problems(root, modid)
        sapling_model_diffs, saplings_checked = sapling_model_problems(root, modid)
        alpha_diffs, alpha_summary = alpha_guard_problems(root, modid)
        with zipfile.ZipFile(gba.find_client_jar(root)) as zf:
            vanilla_leaf_ref = _vanilla_leaf_reference(zf)
        leaves_diffs, leaves_checked = leaves_structure_problems(root, modid, vanilla_leaf_ref)
    except (ParseError, json.JSONDecodeError, bd.FamilyDataError,
            KeyError, OSError, zipfile.BadZipFile) as exc:
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
    print(f"[build] stone colour : base-matched derived sprites "
          f"(compared {stone_summary['compared']}, {stone_summary['over']} over; "
          f"per base {stone_summary['per_base']})")
    print(f"[build] stone pattern : not-collapsed "
          f"(min value sigma {stone_summary['min_sigma']:.4f}, "
          f"min colours {stone_summary['min_colours']}, "
          f"{stone_summary['pattern_over']} over)")
    print(f"[build] blockstate props : per-shape whitelist "
          f"({len(blockstate_prop_diffs)} illegal)")
    print(f"[build] leaves loot  : {leaves_checked}/{leaves_checked} checked, "
          f"{len(leaves_diffs)} over")
    print(f"[build] loot fn conds : {loot_tables_scanned} tables scanned, "
          f"{len(function_cond_diffs)} illegal")
    print(f"[build] sapling model : cross + cutout "
          f"({saplings_checked} checked, {len(sapling_model_diffs)} over)")
    print(f"[build] alpha guard  : transparency => non-occluding + cutout "
          f"({alpha_summary['solid_checked']} solid checked, "
          f"{alpha_summary['transparent_checked']} transparent checked, "
          f"{alpha_summary['occludable_transparent']} occludable, "
          f"{alpha_summary['over']} over)")

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
    if stone_color_diffs:
        problems.append(("石材配色/图案超差", "derived stone mean HSV or pattern vs base",
                         stone_color_diffs))
    if blockstate_prop_diffs:
        problems.append(("blockstate 属性越界", "variant property outside shape whitelist",
                         blockstate_prop_diffs))
    if function_cond_diffs:
        problems.append(("掉落函数条件层级错误",
                         "table_bonus/random_chance under functions[*].conditions",
                         function_cond_diffs))
    if leaves_diffs:
        problems.append(("树叶掉落结构错误", "leaf loot not vanilla-shaped", leaves_diffs))
    if sapling_model_diffs:
        problems.append(("树苗模型错误", "sapling block model not cross+cutout",
                         sapling_model_diffs))
    if alpha_diffs:
        problems.append(("透明贴图/遮挡方块/多余 cutout",
                         "transparent texture on occluding block, missing cutout, or stray cutout",
                         alpha_diffs))

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
