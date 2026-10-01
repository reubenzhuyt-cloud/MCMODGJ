#!/usr/bin/env python3
"""Static gate for the village-altar injection chain.

Fails loudly (non-zero exit + named files/fields) if any link of the chain is
broken.  It parses the real reexported structures, template pools and anchor
NBT files; it never silently skips.

Checks
------
1. All five vanilla village structure overrides exist under
   ``data/minecraft/worldgen/structure/`` and each one differs from the vanilla
   jar original *only* by ``start_pool`` (field-by-field comparison).
2. Each override's ``start_pool`` template pool file exists.
3. That pool's anchor ``.nbt`` exists and contains both
   ``pool=weather_realm:village/altar_pool`` and
   ``pool=minecraft:village/<type>/town_centers`` jigsaw blocks.
4. ``weather_realm:village/altar_pool`` exists and its ``weather_altar.nbt``
   exists.
5. Every jigsaw ``pool`` string found in those NBT files resolves to an
   existing template pool (mod pools in the repo, ``minecraft:`` pools in the
   vanilla client jar); unresolved pools are named.
6. The three mod biomes' ``effects.music.sound`` id is registered in
   ``assets/weather_realm/sounds.json``.
7. Zero parsed villages / zero parsed NBT files is an error.
"""
from __future__ import annotations

import gzip
import io
import json
import os
import struct
import sys
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
MODID = "weather_realm"
RESOURCES = REPO / "src" / "main" / "resources"
STRUCT_OVERRIDE_DIR = RESOURCES / "data" / "minecraft" / "worldgen" / "structure"
MOD_TEMPLATE_POOL_DIR = RESOURCES / "data" / MODID / "worldgen" / "template_pool"
MOD_STRUCTURE_DIR = RESOURCES / "data" / MODID / "structure"
MOD_BIOME_DIR = RESOURCES / "data" / MODID / "worldgen" / "biome"
SOUNDS_JSON = RESOURCES / "assets" / MODID / "sounds.json"

sys.path.insert(0, str(Path(__file__).resolve().parent))
try:
    import make_village_start_nbt as nbtlib
except Exception as exc:  # pragma: no cover - hard failure is the point
    sys.exit(f"[verify_village_altar] cannot import NBT library: {exc}")

# --- expected chain ---------------------------------------------------------
VILLAGE_TYPES = ["plains", "snowy", "desert", "savanna", "taiga"]
ALTAR_POOL = "weather_realm:village/altar_pool"
ALTAR_NBT_ID = "weather_realm:weather_altar"
BIOMES = ["crystal_plains", "arid_wasteland", "blazing_plains"]

failures: list[str] = []
notes: list[str] = []


def fail(msg: str) -> None:
    failures.append(msg)


def find_client_jar() -> Path:
    override = os.environ.get("MC_CLIENT_JAR")
    candidates = []
    if override:
        candidates.append(Path(override))
    import glob

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
    sys.exit(
        "[verify_village_altar] cannot find the vanilla client jar; "
        "pass MC_CLIENT_JAR or download the 1.21.1 client."
    )


def split_id(rid: str):
    if ":" not in rid:
        return "minecraft", rid
    ns, path = rid.split(":", 1)
    return ns, path


# --- pool resolution --------------------------------------------------------
def mod_pool_path(rid: str) -> Path:
    _ns, path = split_id(rid)
    return MOD_TEMPLATE_POOL_DIR / f"{path}.json"


def mod_vanilla_pool_exists(rid: str, jar_names: set[str]) -> bool:
    ns, path = split_id(rid)
    if ns == MODID:
        return (MOD_TEMPLATE_POOL_DIR / f"{path}.json").is_file()
    if ns == "minecraft":
        return f"data/minecraft/worldgen/template_pool/{path}.json" in jar_names
    return False


def iter_pool_elements(pool_obj: dict):
    """Yield every structure-template id referenced by a template pool."""
    for element in pool_obj.get("elements", []):
        body = element.get("element", {})
        etype = body.get("element_type", "")
        if etype in (
            "minecraft:single_pool_element",
            "minecraft:legacy_single_pool_element",
        ):
            loc = body.get("location")
            if loc:
                yield loc
        elif etype == "minecraft:list_pool_element":
            for sub in body.get("elements", []):
                sub_type = sub.get("element_type", "")
                loc = sub.get("location")
                if loc and sub_type in (
                    "minecraft:single_pool_element",
                    "minecraft:legacy_single_pool_element",
                ):
                    yield loc
        # feature/empty elements reference no template


def template_nbt_path(rid: str) -> Path:
    """Resolve a structure-template id to its on-disk .nbt path."""
    ns, path = split_id(rid)
    return RESOURCES / "data" / ns / "structure" / f"{path}.nbt"


def main() -> int:
    jar = find_client_jar()
    with zipfile.ZipFile(jar) as zf:
        jar_names = set(zf.namelist())

        def read_vanilla_structure(village_type: str):
            name = f"data/minecraft/worldgen/structure/village_{village_type}.json"
            if name not in jar_names:
                fail(f"vanilla jar is missing {name}")
                return None
            return json.loads(zf.read(name).decode("utf-8"))

        # --- check 1: overrides match vanilla except start_pool ------------
        villages_checked = 0
        for village_type in VILLAGE_TYPES:
            from_file = STRUCT_OVERRIDE_DIR / f"village_{village_type}.json"
            if not from_file.is_file():
                fail(f"[1] missing structure override: {from_file.relative_to(REPO)}")
                continue
            try:
                override = json.loads(from_file.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"[1] {from_file.relative_to(REPO)} is not valid JSON: {exc}")
                continue
            vanilla = read_vanilla_structure(village_type)
            if vanilla is None:
                continue
            villages_checked += 1
            expected_pool = f"{MODID}:village/{village_type}/start"
            if override.get("start_pool") != expected_pool:
                fail(
                    f"[1] {from_file.relative_to(REPO)}: start_pool is "
                    f"{override.get('start_pool')!r}, expected {expected_pool!r}"
                )
            keys = set(override) | set(vanilla)
            for key in sorted(keys):
                if key == "start_pool":
                    continue
                if override.get(key) != vanilla.get(key):
                    fail(
                        f"[1] {from_file.relative_to(REPO)}: field {key!r} differs "
                        f"from vanilla (override={override.get(key)!r}, "
                        f"vanilla={vanilla.get(key)!r}); only start_pool may differ"
                    )

        if villages_checked == 0:
            fail("[7] parsed 0 village structures")

        # --- checks 2 & 3: start pool -> anchor NBT ------------------------
        nbt_parsed = 0
        all_pools: list[tuple[str, str]] = []  # (pool, source description)
        anchor_pools = 0
        for village_type in VILLAGE_TYPES:
            pool_id = f"{MODID}:village/{village_type}/start"
            pool_path = mod_pool_path(pool_id)
            if not pool_path.is_file():
                fail(f"[2] missing template pool for {pool_id}: {pool_path.relative_to(REPO)}")
                continue
            pool_obj = json.loads(pool_path.read_text(encoding="utf-8"))
            locations = list(iter_pool_elements(pool_obj))
            if not locations:
                fail(f"[2] {pool_path.relative_to(REPO)} references no template")
                continue
            for loc in locations:
                nbt_path = template_nbt_path(loc)
                if not nbt_path.is_file():
                    fail(f"[3] element location {loc} has no NBT at {nbt_path.relative_to(REPO)}")
                    continue
                try:
                    _rn, _rt, root = nbtlib.read_nbt_file(nbt_path)
                    jigsaws = nbtlib.find_jigsaws(root)
                except Exception as exc:
                    fail(f"[3] cannot parse {nbt_path.relative_to(REPO)}: {exc}")
                    continue
                nbt_parsed += 1
                pools = {
                    j.get("pool")[1]
                    for j in jigsaws
                    if j.get("pool") is not None and j["pool"][0] == nbtlib.TAG_STRING
                }
                targets = {
                    j.get("target")[1]
                    for j in jigsaws
                    if j.get("target") is not None and j["target"][0] == nbtlib.TAG_STRING
                }
                expected_town = f"minecraft:village/{village_type}/town_centers"
                if ALTAR_POOL not in pools:
                    fail(f"[3] {nbt_path.relative_to(REPO)} has no jigsaw pool={ALTAR_POOL}")
                if "minecraft:building_entrance" not in targets:
                    fail(
                        f"[3] {nbt_path.relative_to(REPO)} has no jigsaw "
                        f"target=minecraft:building_entrance"
                    )
                if expected_town not in pools:
                    fail(
                        f"[3] {nbt_path.relative_to(REPO)} has no jigsaw "
                        f"pool={expected_town}"
                    )
                for p in pools:
                    all_pools.append((p, str(nbt_path.relative_to(REPO))))
                anchor_pools += 1

        if nbt_parsed == 0:
            fail("[7] parsed 0 anchor NBT files")

        # --- check 4: altar pool -> weather_altar.nbt ----------------------
        altar_pool_path = mod_pool_path(ALTAR_POOL)
        if not altar_pool_path.is_file():
            fail(f"[4] missing altar pool: {altar_pool_path.relative_to(REPO)}")
        else:
            altar_pool = json.loads(altar_pool_path.read_text(encoding="utf-8"))
            altar_locs = list(iter_pool_elements(altar_pool))
            if ALTAR_NBT_ID not in altar_locs:
                fail(
                    f"[4] {altar_pool_path.relative_to(REPO)} does not reference "
                    f"{ALTAR_NBT_ID} (found {altar_locs})"
                )
            for loc in altar_locs:
                altar_nbt = template_nbt_path(loc)
                if not altar_nbt.is_file():
                    fail(f"[4] altar element {loc} has no NBT at {altar_nbt.relative_to(REPO)}")
                    continue
                try:
                    _rn, _rt, root = nbtlib.read_nbt_file(altar_nbt)
                    jigsaws = nbtlib.find_jigsaws(root)
                except Exception as exc:
                    fail(f"[4] cannot parse {altar_nbt.relative_to(REPO)}: {exc}")
                    continue
                nbt_parsed += 1
                for j in jigsaws:
                    pool_tag = j.get("pool")
                    if pool_tag is not None and pool_tag[0] == nbtlib.TAG_STRING:
                        all_pools.append((pool_tag[1], str(altar_nbt.relative_to(REPO))))

        # --- check 5: resolve every referenced pool ------------------------
        resolved_pools = set()
        for pool, source in all_pools:
            if mod_vanilla_pool_exists(pool, jar_names):
                resolved_pools.add(pool)
            else:
                fail(f"[5] unresolved jigsaw pool {pool!r} referenced by {source}")

        # --- check 6: biome music registered in sounds.json ----------------
        try:
            sounds = json.loads(SOUNDS_JSON.read_text(encoding="utf-8"))
        except Exception as exc:
            sounds = {}
            fail(f"[6] cannot read {SOUNDS_JSON.relative_to(REPO)}: {exc}")
        for biome in BIOMES:
            biome_path = MOD_BIOME_DIR / f"{biome}.json"
            if not biome_path.is_file():
                fail(f"[6] missing biome {biome_path.relative_to(REPO)}")
                continue
            try:
                biome_obj = json.loads(biome_path.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"[6] {biome_path.relative_to(REPO)} is not valid JSON: {exc}")
                continue
            music = biome_obj.get("effects", {}).get("music")
            if not music:
                fail(f"[6] {biome_path.relative_to(REPO)}: effects.music is missing")
                continue
            sound = music.get("sound")
            if not sound:
                fail(f"[6] {biome_path.relative_to(REPO)}: effects.music.sound is missing")
                continue
            _ns, path = split_id(sound)
            if path not in sounds:
                fail(
                    f"[6] {biome_path.relative_to(REPO)}: music sound {sound!r} is not "
                    f"registered in {SOUNDS_JSON.relative_to(REPO)}"
                )

    # --- summary -----------------------------------------------------------
    print("[verify_village_altar] parsed:")
    print(f"  village structures : {villages_checked}/{len(VILLAGE_TYPES)}")
    print(f"  anchor NBT files   : {anchor_pools}/{len(VILLAGE_TYPES)}")
    print(f"  NBT files parsed   : {nbt_parsed}")
    print(f"  jigsaw pools seen  : {len(all_pools)} references, {len(resolved_pools)} distinct resolved")
    if notes:
        for n in notes:
            print(f"  note: {n}")

    if failures:
        print("[verify_village_altar] FAIL:")
        for f in failures:
            print(f"  - {f}")
        return 1
    print("[verify_village_altar] OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
