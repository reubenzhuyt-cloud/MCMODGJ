#!/usr/bin/env python3
"""Static worldgen gate for Weather Realm.

Small, stable assertions only; no invented thresholds.  Everything is read from
the real repo files and (for ``minecraft:`` ids) the vanilla 1.21.1 client jar;
nothing is silently skipped -- an entry that cannot be confirmed is reported and
fails the gate.

Checks
------
1. **Ice guard** -- ``data/weather_realm/tags/block/frost_plantable_on.json``
   must not list ``minecraft:ice`` / ``minecraft:packed_ice`` /
   ``minecraft:blue_ice`` (frost plants must never grow on bare ice).
2. **Tag members exist** -- every entry of that tag is a real block id: mod ids
   are checked against the Java ``BLOCKS`` registration names (+ the generated
   ore family and ``biome_data.all_building_block_ids``); ``minecraft:`` ids are
   checked against the client jar's ``assets/minecraft/blockstates/*.json``;
   ``#ns:path`` entries must resolve to an existing block tag file.
3. **Reference integrity** -- every placed_feature referenced by the three mod
   biome JSONs exists, and every mod placed_feature's ``feature`` points to an
   existing configured_feature.  Dangling references are named.
4. **Parse-zero is a failure** and a summary line always prints the counts.

Exit codes
----------
* ``0``  gate passed.
* ``1``  gate failure.
* ``2``  environment failure (vanilla client jar not found), printed with an
         ``[ENV]`` prefix so a missing jar is never mistaken for broken data.
"""
from __future__ import annotations

import argparse
import glob
import json
import os
import re
import sys
import zipfile
from pathlib import Path

# UTF-8 diagnostics on the Windows console (GBK would otherwise choke on 中文).
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(encoding="utf-8", errors="replace")

MODID = "weather_realm"
BIOMES = ["crystal_plains", "arid_wasteland", "blazing_plains"]
TAG_REL = "src/main/resources/data/weather_realm/tags/block/frost_plantable_on.json"
FORBIDDEN_TAG_BLOCKS = ("minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice")

# Java block registration calls whose id is a plain string literal.
LITERAL_BLOCK_RE = re.compile(
    r'(?:BLOCKS\.register|\.registerBlock|\.registerSimpleBlock)\(\s*"([a-z0-9_]+)"'
)
# Generated ore family in ModBlocks.registerOrePair (same shape as verify_tab_coverage).
ORE_ARRAY_RE = re.compile(r'String\[\]\s+(\w+)\s*=\s*\{([^}]*)\}')
ORE_FOR_RE = re.compile(r'for\s*\(\s*String\s+(\w+)\s*:\s*(\w+)\s*\)')
ORE_TEMPLATE_RE = re.compile(
    r'registerOrePair\(\s*"([^"]*)"\s*\+\s*(\w+)\s*\+\s*"_ore"\s*,\s*'
    r'"([^"]*)"\s*\+\s*(\w+)\s*\+\s*"_ore"'
)
ORE_LITERAL_RE = re.compile(r'registerOrePair\(\s*"([a-z0-9_]+)"\s*,\s*"([a-z0-9_]+)"')
QUOTED_RE = re.compile(r'"([a-z0-9_]+)"')

failures: list[str] = []


def fail(msg: str) -> None:
    failures.append(msg)


def split_id(rid: str) -> tuple[str, str]:
    if ":" not in rid:
        return "minecraft", rid
    ns, path = rid.split(":", 1)
    return ns, path


def find_client_jar() -> Path:
    override = os.environ.get("MC_CLIENT_JAR")
    candidates = []
    if override:
        candidates.append(Path(override))
    home = Path.home()
    candidates += [
        Path(p)
        for p in glob.glob(
            str(home / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar")
        )
    ]
    for c in candidates:
        if c.is_file():
            return c
    print(
        "[ENV] [verify_worldgen] cannot find the vanilla client jar; "
        "pass MC_CLIENT_JAR or download the 1.21.1 client.",
        file=sys.stderr,
    )
    raise SystemExit(2)


def java_dir(root: Path) -> Path:
    return root / "src/main/java/com/example/weather_realm"


def mod_block_ids(root: Path) -> set:
    """Best-effort set of every ``weather_realm`` block id registered in Java."""
    ids: set[str] = set()
    src = java_dir(root)
    if not src.is_dir():
        return ids
    for path in src.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        ids.update(LITERAL_BLOCK_RE.findall(text))
        if path.name == "ModBlocks.java":
            arrays = {name: QUOTED_RE.findall(body) for name, body in ORE_ARRAY_RE.findall(text)}
            loop_map = dict(ORE_FOR_RE.findall(text))

            def resolve(var: str):
                return arrays.get(loop_map.get(var, var))

            for s_pre, s_var, d_pre, d_var in ORE_TEMPLATE_RE.findall(text):
                shallow, deep = resolve(s_var), resolve(d_var)
                if not shallow or not deep:
                    fail(f"[tag] can't resolve ore arrays {s_var}/{d_var} while building block ids")
                    continue
                ids.update(f"{s_pre}{o}_ore" for o in shallow)
                ids.update(f"{d_pre}{o}_ore" for o in deep)
            for shallow, deep in ORE_LITERAL_RE.findall(text):
                ids.add(shallow)
                ids.add(deep)
    # Building families are registered from concatenated names; the generator's
    # data table is the single source of truth for those.
    try:
        sys.path.insert(0, str(Path(__file__).resolve().parent))
        import biome_data as bd  # noqa: E402
    except Exception as exc:  # pragma: no cover - tooling failure
        fail(f"[tag] could not import biome_data for building block ids: {exc}")
    else:
        hook = getattr(bd, "all_building_block_ids", None)
        if callable(hook):
            ids |= set(hook())
    return ids


def tag_values(tag_obj) -> list:
    """Yield raw entry strings from a block-tag ``values`` list (str or object)."""
    raw = tag_obj.get("values")
    if not isinstance(raw, list):
        return []
    out = []
    for entry in raw:
        if isinstance(entry, str):
            out.append(entry)
        elif isinstance(entry, dict) and isinstance(entry.get("id"), str):
            out.append(entry["id"])
        else:
            fail(f"[tag] unsupported tag entry: {entry!r}")
    return out


def check_tag(root: Path, jar_blocks: set, mod_blocks: set) -> tuple[int, int]:
    """Returns ``(entries_checked, illegal_entries)``."""
    tag_path = root / TAG_REL
    if not tag_path.is_file():
        fail(f"[tag] missing {TAG_REL}")
        return 0, 0
    try:
        tag_obj = json.loads(tag_path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"[tag] {TAG_REL} is not valid JSON: {exc}")
        return 0, 0
    if not isinstance(tag_obj, dict):
        fail(f"[tag] {TAG_REL} is not a JSON object")
        return 0, 0

    entries = tag_values(tag_obj)
    if not entries:
        fail(f"[tag] {TAG_REL} has no values (parse-zero is a failure)")
        return 0, 0

    illegal = 0
    for entry in entries:
        is_tag = entry.startswith("#")
        rid = entry[1:] if is_tag else entry

        # Core ice guard applies to direct block ids (and, defensively, to ids
        # hidden behind a '#' marker -- '#minecraft:ice' is not a real block tag).
        if rid in FORBIDDEN_TAG_BLOCKS:
            illegal += 1
            fail(
                f"[tag] ICE-GUARD (霜系植被不得以冰为底): {entry!r} is forbidden in {TAG_REL} "
                f"(allowed ground is snow / powder_snow / frost_moss)"
            )
            continue

        ns, path = split_id(rid)
        if is_tag:
            tag_file = root / "src/main/resources/data" / ns / "tags/block" / f"{path}.json"
            if ns == "minecraft":
                if f"data/minecraft/tags/block/{path}.json" not in JAR_NAMES:
                    fail(f"[tag] entry {entry!r}: vanilla block tag data/minecraft/tags/block/{path}.json not in client jar")
            elif not tag_file.is_file():
                fail(f"[tag] entry {entry!r} points to missing block tag file")
        elif ns == MODID:
            if path not in mod_blocks:
                fail(
                    f"[tag] entry {entry!r}: not found among {MODID} block registrations "
                    f"(concatenated/family ids must still resolve) -- cannot confirm"
                )
        elif ns == "minecraft":
            if path not in jar_blocks:
                fail(f"[tag] entry {entry!r}: minecraft:{path} is not a vanilla block id")
        else:
            fail(f"[tag] entry {entry!r}: unknown namespace {ns!r}, cannot confirm")
    return len(entries), illegal


def check_worldgen(root: Path) -> tuple[int, int, int, int]:
    """Returns ``(biomes, placed_files, configured_refs, placed_refs)``."""
    biome_dir = root / "src/main/resources/data" / MODID / "worldgen/biome"
    placed_dir = root / "src/main/resources/data" / MODID / "worldgen/placed_feature"
    configured_dir = root / "src/main/resources/data" / MODID / "worldgen/configured_feature"

    def ref_ok(rid: str, kind: str) -> bool:
        ns, path = split_id(rid)
        if ns == MODID:
            return (root / "src/main/resources/data" / MODID / "worldgen" / kind / f"{path}.json").is_file()
        if ns == "minecraft":
            return f"data/minecraft/worldgen/{kind}/{path}.json" in JAR_NAMES
        return False

    biomes = 0
    placed_refs = 0
    for biome in BIOMES:
        path = biome_dir / f"{biome}.json"
        if not path.is_file():
            fail(f"[ref] missing biome {path.relative_to(root)}")
            continue
        try:
            obj = json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:
            fail(f"[ref] {path.relative_to(root)} is not valid JSON: {exc}")
            continue
        biomes += 1
        features = obj.get("features")
        if not isinstance(features, list):
            fail(f"[ref] {path.relative_to(root)}: features is {type(features).__name__}, expected list")
            continue
        stage_ids = []
        for stage in features:
            if isinstance(stage, str):
                stage_ids.append(stage)
            elif isinstance(stage, list):
                stage_ids.extend(x for x in stage if isinstance(x, str))
            else:
                fail(f"[ref] {path.relative_to(root)}: bad feature stage {stage!r}")
        for rid in stage_ids:
            placed_refs += 1
            if not ref_ok(rid, "placed_feature"):
                fail(f"[ref] {path.relative_to(root)} references missing placed_feature {rid!r}")

    placed_files = 0
    configured_refs = 0
    if not placed_dir.is_dir():
        fail(f"[ref] missing placed_feature dir {placed_dir.relative_to(root)}")
    else:
        for path in sorted(placed_dir.glob("*.json")):
            try:
                obj = json.loads(path.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"[ref] {path.relative_to(root)} is not valid JSON: {exc}")
                continue
            placed_files += 1
            target = obj.get("feature")
            if not isinstance(target, str):
                fail(f"[ref] {path.relative_to(root)}: 'feature' is {type(target).__name__}, expected string")
                continue
            configured_refs += 1
            if not ref_ok(target, "configured_feature"):
                fail(f"[ref] {path.relative_to(root)} references missing configured_feature {target!r}")

    return biomes, placed_files, configured_refs, placed_refs


def main() -> int:
    parser = argparse.ArgumentParser(description="Weather Realm worldgen static gate")
    parser.add_argument("--root", default=str(Path(__file__).resolve().parents[1]))
    args = parser.parse_args()
    root = Path(args.root).resolve()

    global JAR_NAMES
    jar = find_client_jar()
    with zipfile.ZipFile(jar) as zf:
        JAR_NAMES = set(zf.namelist())
    jar_blocks = {
        name[len("assets/minecraft/blockstates/"):-len(".json")]
        for name in JAR_NAMES
        if name.startswith("assets/minecraft/blockstates/") and name.endswith(".json")
    }

    mod_blocks = mod_block_ids(root)
    tag_entries, illegal = check_tag(root, jar_blocks, mod_blocks)
    biomes, placed_files, configured_refs, placed_refs = check_worldgen(root)

    print(
        f"worldgen: {biomes} biomes / {placed_files} placed_feature / "
        f"{configured_refs} configured_feature checked; frost tag {illegal} illegal"
    )
    print(
        f"  placed_feature refs resolved: {placed_refs}; "
        f"frost tag entries checked: {tag_entries}; "
        f"mod block ids known: {len(mod_blocks)}"
    )

    # Parse-zero is a failure (a silently-empty parse must never look green).
    if biomes == 0:
        fail("[ref] parsed 0 biome JSONs")
    if placed_files == 0:
        fail("[ref] parsed 0 placed_feature files")
    if configured_refs == 0:
        fail("[ref] parsed 0 configured_feature references")
    if tag_entries == 0:
        fail("[tag] parsed 0 tag entries")
    if not mod_blocks:
        fail("[tag] parsed 0 mod block ids")

    if failures:
        print("[verify_worldgen] FAIL:")
        for msg in failures:
            print(f"  - {msg}")
        return 1
    print("[verify_worldgen] OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
