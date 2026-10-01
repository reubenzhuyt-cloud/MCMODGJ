#!/usr/bin/env python3
"""Static gate: every registered Weather Realm item is placed in a creative tab exactly once.

The verifier never loads Minecraft. It derives two sets straight from the sources:

* **registered items** -- the literal registration calls in ``ModItems.java``
  (``registerSimpleBlockItem("id"`` / ``registerSimpleItem("id"`` / ``ITEMS.register("id"``)
  plus the ore ``BlockItem``s that ``ModItems``' static block registers in a loop over
  ``ModBlocks.GENERATED_ORES``. Those ids are *not* string literals in ``ModItems``; they are
  built by ``ModBlocks.registerOrePair(...)`` from a ``String[]`` loop plus two literal pairs,
  so ``ModBlocks.java`` is parsed as well. Once Phase C adds
  ``biome_data.all_building_block_ids()`` its ids are merged in too (additive hook).
* **tab entries** -- the id literals inside the ``TabCategory`` tables
  (``BUILDING_CATEGORIES`` / ``ITEM_CATEGORIES``) of ``ModCreativeTabs.java``.

It then asserts both directions match, no id repeats, and every tab's title translation key
exists in every ``lang/*.json`` file. A parse that extracts *nothing* is treated as a failure
(a silently-empty regex must never pass as "zero differences").

Usage (from the repo root):

    python tools/verify_tab_coverage.py

Exits 0 when the coverage is exact, otherwise 1.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path

# 让中文诊断在 Windows 控制台也按 UTF-8 输出，避免 GBK 编码报错。
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(encoding="utf-8", errors="replace")


class ParseError(Exception):
    """源码结构与校验脚本不同步（正则没抓到任何东西）。"""


# ModItems.java：三类字面注册调用。
ITEM_LITERAL_RE = re.compile(
    r'(?:registerSimpleBlockItem|registerSimpleItem|ITEMS\.register)\(\s*"([a-z0-9_]+)"')

# ModCreativeTabs.java：分类表字面串。
CATEGORY_RE = re.compile(r'new\s+TabCategory\(\s*"(\w+)"\s*,\s*List\.of\((.*?)\)\)', re.DOTALL)
QUOTED_ID_RE = re.compile(r'"([a-z0-9_]+)"')

# ModCreativeTabs.java：两个页签的注册 id 与其 .title(...) 翻译键。
TAB_RE = re.compile(
    r'CREATIVE_MODE_TABS\.register\(\s*"([a-z0-9_]+)"\s*,\s*\(\)\s*->\s*CreativeModeTab\.builder\(\)'
    r'(.*?)\.build\(\)', re.DOTALL)
TITLE_RE = re.compile(r'\.title\(\s*Component\.translatable\(\s*"([^"]+)"\s*\)\s*\)')

# ModBlocks.java：批量矿石 id 的构造。
ORE_ARRAY_RE = re.compile(r'String\[\]\s+(\w+)\s*=\s*\{([^}]*)\}')
ORE_FOR_RE = re.compile(r'for\s*\(\s*String\s+(\w+)\s*:\s*(\w+)\s*\)')
ORE_TEMPLATE_RE = re.compile(
    r'registerOrePair\(\s*"([^"]*)"\s*\+\s*(\w+)\s*\+\s*"_ore"\s*,\s*'
    r'"([^"]*)"\s*\+\s*(\w+)\s*\+\s*"_ore"')
ORE_LITERAL_RE = re.compile(r'registerOrePair\(\s*"([a-z0-9_]+)"\s*,\s*"([a-z0-9_]+)"')

# WeatherRealm.java：modId。
MODID_RE = re.compile(r'MODID\s*=\s*"([a-z0-9_]+)"')


def _read(path: Path) -> str:
    if not path.is_file():
        raise ParseError(f"缺少源文件: {path}")
    return path.read_text(encoding="utf-8")


def _java_dir(root: Path) -> Path:
    return root / "src/main/java/com/example/weather_realm"


def mod_id(root: Path) -> str:
    text = _read(_java_dir(root) / "WeatherRealm.java")
    match = MODID_RE.search(text)
    if not match:
        raise ParseError("无法从 WeatherRealm.java 解析 MODID")
    return match.group(1)


def literal_item_ids(root: Path) -> set:
    """ModItems.java 中三个字面注册调用里的 id。"""
    text = _read(_java_dir(root) / "ModItems.java")
    ids = set(ITEM_LITERAL_RE.findall(text))
    if not ids:
        raise ParseError("ModItems.java 未解析出任何字面注册 id（正则与代码不同步？）")
    return ids


def generated_ore_ids(root: Path) -> set:
    """ModBlocks.registerOrePair 生成的矿石 id（循环模板 + 字面成对）。"""
    text = _read(_java_dir(root) / "ModBlocks.java")
    arrays = {name: QUOTED_ID_RE.findall(body) for name, body in ORE_ARRAY_RE.findall(text)}
    # for-each 别名：循环变量 -> 数组变量（模板里用的是循环变量名）。
    loop_map = dict(ORE_FOR_RE.findall(text))

    def resolve(var: str):
        return arrays.get(loop_map.get(var, var))

    ids = set()
    for shallow_prefix, shallow_var, deep_prefix, deep_var in ORE_TEMPLATE_RE.findall(text):
        shallow_ores = resolve(shallow_var)
        deep_ores = resolve(deep_var)
        if not shallow_ores or not deep_ores:
            raise ParseError(f"ModBlocks 矿石模板引用了未知/空数组: {shallow_var}, {deep_var}")
        for ore in shallow_ores:
            ids.add(f"{shallow_prefix}{ore}_ore")
        for ore in deep_ores:
            ids.add(f"{deep_prefix}{ore}_ore")

    for shallow, deep in ORE_LITERAL_RE.findall(text):
        ids.add(shallow)
        ids.add(deep)

    if not ids:
        raise ParseError("ModBlocks.java 未解析出任何批量矿石 id（正则与代码不同步？）")
    return ids


def registered_item_ids(root: Path) -> set:
    ids = literal_item_ids(root) | generated_ore_ids(root)

    # Phase C 前向兼容钩子：若 biome_data 提供了 all_building_block_ids() 则一并纳入。
    try:
        sys.path.insert(0, str(Path(__file__).resolve().parent))
        import biome_data as bd  # noqa: E402
    except ImportError:
        bd = None
    hook = getattr(bd, "all_building_block_ids", None) if bd is not None else None
    if callable(hook):
        ids |= set(hook())
    return ids


def tab_entry_ids(root: Path) -> list:
    """按出现次序返回分类表里的所有 id（保留重复以检测）。"""
    text = _read(_java_dir(root) / "ModCreativeTabs.java")
    found = []
    for _key, body in CATEGORY_RE.findall(text):
        found.extend(QUOTED_ID_RE.findall(body))
    if not found:
        raise ParseError("ModCreativeTabs.java 未解析出任何分类项 id（TabCategory 结构变了？）")
    return found


def tab_title_keys(root: Path) -> list:
    """返回 [(注册 id, 翻译键), ...]。"""
    text = _read(_java_dir(root) / "ModCreativeTabs.java")
    tabs = []
    for tab_id, body in TAB_RE.findall(text):
        match = TITLE_RE.search(body)
        if not match:
            raise ParseError(f"页签 '{tab_id}' 未找到 .title(Component.translatable(...))")
        tabs.append((tab_id, match.group(1)))
    if not tabs:
        raise ParseError("ModCreativeTabs.java 未解析出任何页签注册（正则与代码不同步？）")
    return tabs


def lang_keys(root: Path, modid: str) -> dict:
    """{lang 文件名: 键集合}。"""
    lang_dir = root / "src/main/resources/assets" / modid / "lang"
    files = sorted(lang_dir.glob("*.json"))
    if not files:
        raise ParseError(f"未找到语言文件: {lang_dir}/*.json")
    result = {}
    for path in files:
        data = json.loads(path.read_text(encoding="utf-8"))
        result[path.name] = set(data)
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description="创造标签页覆盖率静态校验")
    parser.add_argument("--root", default=str(Path(__file__).resolve().parents[1]),
                        help="仓库根目录（默认脚本上一级）")
    args = parser.parse_args()
    root = Path(args.root).resolve()

    try:
        modid = mod_id(root)
        registered = registered_item_ids(root)
        entries = tab_entry_ids(root)
        tabs = tab_title_keys(root)
        langs = lang_keys(root, modid)
    except (ParseError, json.JSONDecodeError) as exc:
        print("TAB COVERAGE FAILED: 解析错误", file=sys.stderr)
        print(f"  - {exc}", file=sys.stderr)
        sys.exit(1)

    counts = Counter(entries)
    tab_ids = set(entries)

    print(f"[tab] registered items : {len(registered)}")
    print(f"[tab] tab entries      : {len(entries)} (distinct {len(counts)})")
    print(f"[tab] tabs             : {len(tabs)} "
          f"[{', '.join(f'{i}->{k}' for i, k in tabs)}]")

    duplicates = sorted(i for i, c in counts.items() if c > 1)
    missing = sorted(registered - tab_ids)
    extra = sorted(tab_ids - registered)

    # 页签翻译键：注册页必须能在每个 lang 文件里找到对应的 itemGroup.* 键。
    missing_keys = []
    for tab_id, key in tabs:
        if not key.startswith("itemGroup."):
            missing_keys.append(f"{tab_id} 的标题键 '{key}' 不是 itemGroup.*")
            continue
        for filename, keys in langs.items():
            if key not in keys:
                missing_keys.append(f"{tab_id} 的标题键 '{key}' 在 {filename} 中缺失")

    if len(tabs) != 2:
        missing_keys.append(f"期望 2 个创造页，实际 {len(tabs)} 个")

    problems = []
    if missing:
        problems.append(f"缺失 (registered but not in any tab): {missing}")
    if duplicates:
        problems.append(f"重复 (duplicated in tabs): {duplicates}")
    if extra:
        problems.append(f"多余 (in a tab but not registered): {extra}")
    if missing_keys:
        problems.append(f"键缺失 (missing lang keys): {missing_keys}")

    if problems:
        print("TAB COVERAGE FAILED", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        sys.exit(1)

    print("TAB COVERAGE OK")
    sys.exit(0)


if __name__ == "__main__":
    main()
