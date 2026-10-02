#!/usr/bin/env python3
"""Generate textures + client/datapack resources for the blazing / arid biome blocks.

Usage (from the repo root, after `pip install Pillow`):

    python tools/gen_block_assets.py
    python tools/gen_block_assets.py --root . --client-jar <path to client jar>

16x16 textures are recoloured from the vanilla 1.21.1 client jar, so the mod
ships no hand-drawn art. Datapack JSON (loot tables, tags) and the lang files are
*merged* into the existing resources, never overwritten wholesale.
"""
from __future__ import annotations

import argparse
import colorsys
import glob
import io
import json
import math
import os
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import biome_data as bd  # noqa: E402

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("Pillow is required:  pip install Pillow")

MODID = bd.MODID


# --- vanilla jar / texture helpers ------------------------------------------
def find_client_jar(root: Path) -> Path:
    override = os.environ.get("MC_CLIENT_JAR")
    candidates = []
    if override:
        candidates.append(Path(override))
    home = Path.home()
    candidates += [Path(p) for p in glob.glob(str(home / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"))]
    for base in (root, root.parent, home):
        candidates += [Path(p) for p in glob.glob(str(base / ".minecraft/libraries/net/minecraft/client/**/*extra.jar"), recursive=True)]
    for c in candidates:
        if c.is_file():
            return c
    sys.exit("Could not find a vanilla client jar; pass --client-jar or set MC_CLIENT_JAR.")


def read_png(zf: zipfile.ZipFile, rel: str) -> Image.Image:
    path = f"assets/minecraft/textures/{rel}.png"
    with zf.open(path) as fh:
        return Image.open(io.BytesIO(fh.read())).convert("RGBA")


def read_text(zf: zipfile.ZipFile, rel: str) -> str:
    with zf.open(f"assets/minecraft/{rel}") as fh:
        return fh.read().decode("utf-8")


def write_template_blockstate(assets: Path, name: str, template_rel: str,
                              replacements) -> None:
    """Copy a vanilla blockstate and rename its model references (longest first)."""
    text = read_text(ZIP, f"blockstates/{template_rel}.json")
    for old, new in sorted(replacements, key=lambda r: len(r[0]), reverse=True):
        text = text.replace(old, new)
    path = assets / "blockstates" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def image_mean_hsv(img: Image.Image):
    """Mean ``(hue, sat, val)`` over opaque pixels; hue is a circular mean.

    Returns ``None`` when the image has no opaque pixel.
    """
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


def _hash01(x: int, y: int, seed: int) -> float:
    """Deterministic 0..1 hash of a pixel coordinate + seed (no RNG state)."""
    n = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    n &= 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n ^= n >> 16
    return (n & 0xFFFFFF) / 0xFFFFFF


def _weather(h, s, v, a, x, y, cfg, src_name):
    """Add deterministic 'wind-eroded wood' detail to one already-recoloured pixel.

    Follows the source sprite's grain direction (``*_log`` -> vertical, planks /
    doors -> horizontal, ``*_log_top`` -> no grain), darkens with a fixed
    coordinate hash for rot/bleach spots, and punches a few ``alpha=0`` holes.
    Pure function of ``(x, y, seed)``: identical output every run.
    """
    seed = cfg.get("seed", 0)
    grain = cfg.get("grain", 0.0)
    if grain and "log_top" not in src_name:
        stripe = x if "log" in src_name else y
        wave = math.sin(2.0 * math.pi * stripe / 4.0
                        + 0.9 * math.sin(stripe * 1.3 + seed * 0.017))
        v *= 1.0 - grain * (0.5 + 0.5 * wave)
    mottle = cfg.get("mottle", 0.0)
    if mottle:
        v *= 1.0 - mottle + 2.0 * mottle * _hash01(x, y, seed + 11)
    spot = cfg.get("spot", 0.0)
    if spot:
        cell = _hash01(x // 4, y // 4, seed + 29)
        if cell < spot:
            v *= 0.62
        elif cell > 1.0 - spot:
            v = 1.0 - (1.0 - v) * 0.75
    v = min(1.0, max(0.0, v))
    s = min(1.0, max(0.0, s))
    holes = cfg.get("holes", 0.0)
    if holes and a != 0 and _hash01(x, y, seed + 71) < holes:
        a = 0
    return h, s, v, a


def recolor(img: Image.Image, profile: dict, anchor_base: Image.Image | None = None,
            src_name: str = "") -> Image.Image:
    """Hue/saturation/value recolor. Saturated 'speck' pixels are protected.

    Two opt-in extensions, both deterministic:

    * ``profile['anchor_to_base']``: instead of letting the *source* sprite's
      saturation/value leak through (``val = src_val * val_mul``), map every pixel
      onto ``anchor_base``'s mean colour while keeping the source's relative
      lightness/counter-shade (``val = base_val * src_val / mean_src_val`` and the
      same for saturation). This anchors derived stone pieces to their own base
      rock's colour family.
    * ``profile['weather']``: per-pixel procedural weathering (grain, mottle,
      rot/bleach spots, ``alpha=0`` holes) driven by a coordinate hash.
    """
    out = img.copy().convert("RGBA")
    px = out.load()
    target_hue = profile.get("target_hue")
    sat_floor = profile.get("sat_floor", 0.0)
    sat_mul = profile.get("sat_mul", 1.0)
    val_mul = profile.get("val_mul", 1.0)
    protect_sat = profile.get("protect_sat")
    speck_shift = profile.get("speck_hue_shift", 0.0)
    anchor = bool(profile.get("anchor_to_base")) and anchor_base is not None
    weather = profile.get("weather")
    if anchor:
        base_stats = image_mean_hsv(anchor_base)
        src_stats = image_mean_hsv(out)
        if base_stats is None or src_stats is None:
            raise ValueError("cannot anchor recolor: empty base/source statistics")
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            if anchor:
                # The source (deepslate) is nearly greyscale, so per-pixel saturation/hue are
                # noise. Take the base's hue/saturation as the colour family outright and keep
                # only the source's relative *lightness* (relief / brick seams / cracks):
                # that makes the derived mean land on the base while retaining the pattern.
                hh = base_stats[0]
                ss = base_stats[1]
                vv = (min(1.0, base_stats[2] * (vv / src_stats[2]))
                      if src_stats[2] > 0 else base_stats[2])
            elif protect_sat is not None and ss >= protect_sat:
                hh = (hh + speck_shift) % 1.0
                vv = min(1.0, vv * val_mul)
            else:
                hh = target_hue if target_hue is not None else hh
                ss = min(1.0, max(ss, sat_floor) * sat_mul)
                vv = min(1.0, vv * val_mul)
            if weather is not None:
                hh, ss, vv, a = _weather(hh, ss, vv, a, x, y, weather, src_name)
            nr, ng, nb = colorsys.hsv_to_rgb(hh, ss, vv)
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


def palette(img: Image.Image) -> set:
    """Every distinct RGBA tuple present in ``img``."""
    px = img.load()
    return {px[x, y] for y in range(img.height) for x in range(img.width)}


def compose_ore(ore_img: Image.Image, vanilla_base: Image.Image, profile: dict,
                background: Image.Image) -> Image.Image:
    """Paste one vanilla ore's mineral pixels onto one of our own rock textures.

    The vanilla ore texture's stone background is *not* pixel-identical to
    ``stone.png``/``deepslate.png`` -- it reuses the same greys in a different
    arrangement -- so a per-pixel diff against the vanilla rock would misclassify
    half the background as mineral. The mineral mask is therefore taken by palette
    membership: any pixel whose colour does not occur in the vanilla rock palette is
    a mineral pixel. Those pixels are recoloured with ``profile`` and drawn over
    ``background`` (the already-generated rock texture), which makes the ore's
    background byte-for-byte identical to the block it belongs to.
    """
    recolored = recolor(ore_img, profile)
    base_palette = palette(vanilla_base)
    out = background.copy().convert("RGBA")
    po = ore_img.load()
    pr = recolored.load()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            if po[x, y] not in base_palette:
                px[x, y] = pr[x, y]
    return out


def write_json(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def merge_tag(path: Path, values) -> None:
    data = {"replace": False, "values": []}
    raw = None
    newline = "\n"
    if path.exists():
        raw = path.read_bytes()
        if b"\r\n" in raw:
            newline = "\r\n"
        data = json.loads(raw.decode("utf-8"))
        data.setdefault("replace", False)
        data.setdefault("values", [])
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    if newline != "\n":
        text = text.replace("\n", newline)
    encoded = text.encode("utf-8")
    if raw == encoded:  # no-op: leave hand-authored file (and its line endings) untouched
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(encoded)


# --- block specs -------------------------------------------------------------
def cover_specs():
    """The three per-biome topsoil covers."""
    return [
        dict(
            name=c["name"], model="cube_all", en=c["en"], zh=c["zh"], loot=("self",),
            textures=[(c["name"], c["src"], c["profile"])],
        )
        for c in bd.SURFACE_COVERS
    ]


def block_specs(theme):
    """Return the list of block descriptors generated for one theme."""
    specs = []
    base, deep = theme["base"], theme["deep"]
    # Themes flagged ``composite_ores`` build their ores by pasting the vanilla mineral
    # pixels onto our own rock texture, so the ore background always matches the rock
    # block byte-for-byte. Other themes keep the legacy whole-texture recolor.
    ore_composite = (
        {"background": base, "vanilla_base": "block/stone"} if theme.get("composite_ores") else None)
    deep_ore_composite = (
        {"background": deep, "vanilla_base": "block/deepslate"} if theme.get("composite_ores") else None)

    specs.append(dict(
        name=base, model="cube_all", en=theme["base_en"], zh=theme["base_zh"], loot=("self",),
        textures=[(base, theme["base_src"], theme["stone_profile"])],
    ))
    specs.append(dict(
        name=deep, model="cube_all", en=theme["deep_en"], zh=theme["deep_zh"], loot=("self",),
        textures=[(deep, theme["deep_src"], theme["deep_profile"])],
    ))
    specs.append(dict(
        name=theme["crystal_block"], model="cube_all",
        en=theme["crystal_block_en"], zh=theme["crystal_block_zh"], loot=("self",),
        textures=[(theme["crystal_block"], theme["crystal_block_src"], theme["crystal_profile"])],
    ))

    for ore in bd.ORE_ORDER:
        shallow = bd.shallow_ore(theme, ore)
        d = bd.deep_ore(theme, ore)
        specs.append(dict(
            name=shallow, model="cube_all",
            en=f"{theme['base_en']} {bd.ORE_EN[ore]} Ore", zh=f"{theme['base_zh']}{bd.ORE_ZH[ore]}矿",
            loot=("ore", ore),
            textures=[(shallow, f"block/{ore}_ore", theme["stone_profile"])],
            composite=ore_composite,
        ))
        specs.append(dict(
            name=d, model="cube_all",
            en=f"{theme['deep_en']} {bd.ORE_EN[ore]} Ore", zh=f"{theme['deep_zh']}{bd.ORE_ZH[ore]}矿",
            loot=("ore", ore),
            textures=[(d, f"block/deepslate_{ore}_ore", theme["deep_profile"])],
            composite=deep_ore_composite,
        ))

    cs = bd.shallow_crystal_ore(theme)
    cd = bd.deep_crystal_ore(theme)
    specs.append(dict(
        name=cs, model="cube_all",
        en=f"{theme['base_en']} {theme['crystal_en']} Ore", zh=f"{theme['base_zh']}{theme['crystal_zh']}矿",
        loot=("crystal_ore", theme["crystal"]),
        textures=[(cs, theme["crystal_ore_src"], theme["crystal_ore_profile"])],
        composite=ore_composite,
    ))
    specs.append(dict(
        name=cd, model="cube_all",
        en=f"{theme['deep_en']} {theme['crystal_en']} Ore", zh=f"{theme['deep_zh']}{theme['crystal_zh']}矿",
        loot=("crystal_ore", theme["crystal"]),
        textures=[(cd, theme["deep_crystal_ore_src"], theme["deep_crystal_ore_profile"])],
        composite=deep_ore_composite,
    ))

    wn = bd.wood_block_names(theme)
    wsrc = theme["wood_src"]
    # Wood pillars are opaque cubes: their textures must stay fully opaque, so no cutout.
    specs.append(dict(
        name=wn["log"], model="pillar", en=theme["wood_names_en"]["log"], zh=theme["wood_names_zh"]["log"],
        loot=("self",),
        textures=[(f"{theme['wood']}_log", f"block/{wsrc}_log", theme["wood_profile"]),
                  (f"{theme['wood']}_log_top", f"block/{wsrc}_log_top", theme["wood_profile"])],
        pillar_top=f"{theme['wood']}_log_top", pillar_side=f"{theme['wood']}_log",
    ))
    specs.append(dict(
        name=wn["wood"], model="pillar", en=theme["wood_names_en"]["wood"], zh=theme["wood_names_zh"]["wood"],
        loot=("self",),
        textures=[(f"{theme['wood']}_log", f"block/{wsrc}_log", theme["wood_profile"])],
        pillar_top=f"{theme['wood']}_log", pillar_side=f"{theme['wood']}_log",
    ))
    specs.append(dict(
        name=wn["stripped_log"], model="pillar", en=theme["wood_names_en"]["stripped_log"],
        zh=theme["wood_names_zh"]["stripped_log"], loot=("self",),
        textures=[(f"stripped_{theme['wood']}_log", f"block/stripped_{wsrc}_log", theme["wood_profile"]),
                  (f"stripped_{theme['wood']}_log_top", f"block/stripped_{wsrc}_log_top", theme["wood_profile"])],
        pillar_top=f"stripped_{theme['wood']}_log_top", pillar_side=f"stripped_{theme['wood']}_log",
    ))
    specs.append(dict(
        name=wn["leaves"], model="leaves", en=theme["wood_names_en"]["leaves"],
        zh=theme["wood_names_zh"]["leaves"], loot=("leaves", f"{theme['wood']}_sapling"),
        textures=[(f"{theme['wood']}_leaves", f"block/{wsrc}_leaves", theme["leaves_profile"])],
    ))

    for plant in theme["plants"]:
        specs.append(dict(
            name=plant["name"], model="cross", en=plant["en"], zh=plant["zh"], loot=("self",),
            textures=[(plant["name"], plant["src"], plant["profile"])],
        ))
    return specs


# --- resource writers --------------------------------------------------------
def _tx(spec, key):
    return f"{MODID}:block/{spec['tex'][key]}"


def write_block_client(root: Path, spec) -> None:
    assets = root / "src/main/resources/assets" / MODID
    name = spec["name"]
    for sub in ("textures/block", "textures/item", "models/block", "models/item", "blockstates"):
        (assets / sub).mkdir(parents=True, exist_ok=True)
    composite = spec.get("composite")
    rt = {"render_type": "minecraft:cutout"} if spec.get("cutout") else {}
    for out_tex, src_tex, profile in spec.get("textures", []):
        if composite is not None:
            background = Image.open(
                assets / "textures/block" / f"{composite['background']}.png").convert("RGBA")
            img = compose_ore(read_png(ZIP, src_tex), read_png(ZIP, composite["vanilla_base"]),
                              profile, background)
        else:
            anchor_img = None
            if profile.get("anchor_to_base"):
                anchor_path = assets / "textures/block" / f"{profile['anchor_base']}.png"
                if not anchor_path.is_file():
                    raise FileNotFoundError(
                        f"anchor base texture missing for {out_tex}: {anchor_path}")
                anchor_img = Image.open(anchor_path).convert("RGBA")
            img = recolor(read_png(ZIP, src_tex), profile, anchor_img, src_tex)
        img.save(assets / "textures/block" / f"{out_tex}.png")
    for out_tex, src_tex, profile in spec.get("item_textures", []):
        recolor(read_png(ZIP, src_tex), profile, None, src_tex).save(
            assets / "textures/item" / f"{out_tex}.png")

    model = spec.get("shape", spec["model"])
    if model == "pillar":
        side, top = spec["pillar_side"], spec["pillar_top"]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_column", **rt,
            "textures": {"end": f"{MODID}:block/{top}", "side": f"{MODID}:block/{side}"}})
        write_json(assets / "models/block" / f"{name}_horizontal.json", {
            "parent": "minecraft:block/cube_column_horizontal", **rt,
            "textures": {"end": f"{MODID}:block/{top}", "side": f"{MODID}:block/{side}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {
            "axis=x": {"model": f"{MODID}:block/{name}_horizontal", "x": 90, "y": 90},
            "axis=y": {"model": f"{MODID}:block/{name}"},
            "axis=z": {"model": f"{MODID}:block/{name}_horizontal", "x": 90}}})
    elif model == "cross":
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
            "textures": {"cross": f"{MODID}:block/{texture}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif model == "leaves":
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout",
            "textures": {"all": f"{MODID}:block/{texture}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif model == "cube_all":
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", **rt,
            "textures": {"all": f"{MODID}:block/{texture}"}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif model == "glass_block":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
            "textures": {"all": _tx(spec, "all")}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
    elif model == "chain":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/chain",
            "textures": {"all": _tx(spec, "all"), "particle": _tx(spec, "all")}})
        write_json(assets / "blockstates" / f"{name}.json", {"variants": {
            "axis=x": {"model": f"{MODID}:block/{name}", "x": 90, "y": 90},
            "axis=y": {"model": f"{MODID}:block/{name}"},
            "axis=z": {"model": f"{MODID}:block/{name}", "x": 90}}})
    elif model == "stairs":
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", **rt,
                "textures": {"bottom": _tx(spec, "parent"), "side": _tx(spec, "parent"),
                             "top": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_stairs", [
            ("minecraft:block/oak_stairs_inner", f"{MODID}:block/{name}_inner"),
            ("minecraft:block/oak_stairs_outer", f"{MODID}:block/{name}_outer"),
            ("minecraft:block/oak_stairs", f"{MODID}:block/{name}")])
    elif model == "slab":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/slab", **rt,
            "textures": {"bottom": _tx(spec, "parent"), "side": _tx(spec, "parent"),
                         "top": _tx(spec, "parent")}})
        write_json(assets / "models/block" / f"{name}_top.json", {
            "parent": "minecraft:block/slab_top", **rt,
            "textures": {"bottom": _tx(spec, "parent"), "side": _tx(spec, "parent"),
                         "top": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_slab", [
            ("minecraft:block/oak_slab_top", f"{MODID}:block/{name}_top"),
            ("minecraft:block/oak_planks", _tx(spec, "double")),
            ("minecraft:block/oak_slab", f"{MODID}:block/{name}")])
    elif model == "wall":
        for suffix, parent in (("_post", "template_wall_post"), ("_side", "template_wall_side"),
                               ("_side_tall", "template_wall_side_tall"),
                               ("_inventory", "wall_inventory")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"wall": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "cobblestone_wall", [
            ("minecraft:block/cobblestone_wall_side_tall", f"{MODID}:block/{name}_side_tall"),
            ("minecraft:block/cobblestone_wall_side", f"{MODID}:block/{name}_side"),
            ("minecraft:block/cobblestone_wall_post", f"{MODID}:block/{name}_post")])
    elif model == "fence":
        for suffix, parent in (("_post", "fence_post"), ("_side", "fence_side"),
                               ("_inventory", "fence_inventory")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", **rt,
                "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_fence", [
            ("minecraft:block/oak_fence_post", f"{MODID}:block/{name}_post"),
            ("minecraft:block/oak_fence_side", f"{MODID}:block/{name}_side")])
    elif model == "fence_gate":
        for suffix, parent in (("", "template_fence_gate"), ("_open", "template_fence_gate_open"),
                               ("_wall", "template_fence_gate_wall"),
                               ("_wall_open", "template_fence_gate_wall_open")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", **rt,
                "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_fence_gate", [
            ("minecraft:block/oak_fence_gate_wall_open", f"{MODID}:block/{name}_wall_open"),
            ("minecraft:block/oak_fence_gate_wall", f"{MODID}:block/{name}_wall"),
            ("minecraft:block/oak_fence_gate_open", f"{MODID}:block/{name}_open"),
            ("minecraft:block/oak_fence_gate", f"{MODID}:block/{name}")])
    elif model == "door":
        bottom = _tx(spec, "parent")
        top_ref = spec.get("tex", {}).get("top")
        if not top_ref:
            raise ValueError(
                f"door '{name}' is missing tex['top']: doors need distinct upper/lower sprites")
        top = f"{MODID}:block/{top_ref}"
        if top == bottom:
            raise ValueError(
                f"door '{name}' top and bottom both resolve to {top}: sprites must differ")
        tex = {"bottom": bottom, "top": top}
        for suffix, parent in (("_bottom_left", "door_bottom_left"),
                               ("_bottom_left_open", "door_bottom_left_open"),
                               ("_bottom_right", "door_bottom_right"),
                               ("_bottom_right_open", "door_bottom_right_open"),
                               ("_top_left", "door_top_left"),
                               ("_top_left_open", "door_top_left_open"),
                               ("_top_right", "door_top_right"),
                               ("_top_right_open", "door_top_right_open")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", **rt, "textures": tex})
        write_template_blockstate(assets, name, "oak_door", [
            (f"minecraft:block/oak_door_{part}", f"{MODID}:block/{name}_{part}")
            for part in ("bottom_left_open", "bottom_left", "bottom_right_open", "bottom_right",
                         "top_left_open", "top_left", "top_right_open", "top_right")])
    elif model == "trapdoor":
        for suffix, parent in (("_bottom", "template_trapdoor_bottom"),
                               ("_top", "template_trapdoor_top"), ("_open", "template_trapdoor_open")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", **rt,
                "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_trapdoor", [
            ("minecraft:block/oak_trapdoor_bottom", f"{MODID}:block/{name}_bottom"),
            ("minecraft:block/oak_trapdoor_top", f"{MODID}:block/{name}_top"),
            ("minecraft:block/oak_trapdoor_open", f"{MODID}:block/{name}_open")])
    elif model == "button":
        for suffix, parent in (("", "button"), ("_pressed", "button_pressed"),
                               ("_inventory", "button_inventory")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", **rt,
                "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_button", [
            ("minecraft:block/oak_button_pressed", f"{MODID}:block/{name}_pressed"),
            ("minecraft:block/oak_button", f"{MODID}:block/{name}")])
    elif model == "pressure_plate":
        for suffix, parent in (("", "pressure_plate_up"), ("_down", "pressure_plate_down")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", **rt,
                "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_pressure_plate", [
            ("minecraft:block/oak_pressure_plate_down", f"{MODID}:block/{name}_down"),
            ("minecraft:block/oak_pressure_plate", f"{MODID}:block/{name}")])
    elif model == "lantern":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/template_lantern", "textures": {"lantern": _tx(spec, "lantern")}})
        write_json(assets / "models/block" / f"{name}_hanging.json", {
            "parent": "minecraft:block/template_hanging_lantern",
            "textures": {"lantern": _tx(spec, "lantern")}})
        write_template_blockstate(assets, name, "lantern", [
            ("minecraft:block/lantern_hanging", f"{MODID}:block/{name}_hanging"),
            ("minecraft:block/lantern", f"{MODID}:block/{name}")])
    elif model == "pane":
        for suffix, parent in (("_post", "template_glass_pane_post"), ("_side", "template_glass_pane_side"),
                               ("_side_alt", "template_glass_pane_side_alt"),
                               ("_noside", "template_glass_pane_noside"),
                               ("_noside_alt", "template_glass_pane_noside_alt")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}",
                "textures": {"pane": _tx(spec, "pane"), "edge": _tx(spec, "edge")}})
        write_template_blockstate(assets, name, "glass_pane", [
            ("minecraft:block/glass_pane_side_alt", f"{MODID}:block/{name}_side_alt"),
            ("minecraft:block/glass_pane_noside_alt", f"{MODID}:block/{name}_noside_alt"),
            ("minecraft:block/glass_pane_side", f"{MODID}:block/{name}_side"),
            ("minecraft:block/glass_pane_noside", f"{MODID}:block/{name}_noside"),
            ("minecraft:block/glass_pane_post", f"{MODID}:block/{name}_post")])
    elif model == "cluster":
        # Vanilla ``AmethystClusterBlock``: facing (6) x waterlogged (2) = 12 states.
        texture = spec["textures"][0][0]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
            "textures": {"cross": f"{MODID}:block/{texture}"}})
        variants = {}
        for facing, rotation in (("down", {"x": 180}), ("east", {"x": 90, "y": 90}),
                                 ("north", {"x": 90}), ("south", {"x": 90, "y": 180}),
                                 ("up", {}), ("west", {"x": 90, "y": 270})):
            for waterlogged in (False, True):
                variant = {"model": f"{MODID}:block/{name}"}
                variant.update(rotation)
                variants[f"facing={facing},waterlogged={str(waterlogged).lower()}"] = variant
        write_json(assets / "blockstates" / f"{name}.json", {"variants": variants})
    elif model == "layer":
        # Vanilla ``SnowLayerBlock`` exposes only ``layers`` (1..8): 8 states, no
        # ``waterlogged`` property. Emitting a waterlogged dimension produced illegal
        # variants (``Unknown blockstate property: 'waterlogged'``) and left the block
        # unrendered. Segment models for 1..7, the full block for 8.
        for height in (2, 4, 6, 8, 10, 12, 14):
            write_json(assets / "models/block" / f"{name}_height{height}.json", {
                "parent": f"minecraft:block/snow_height{height}",
                "textures": {"texture": _tx(spec, "all"), "particle": _tx(spec, "all")}})
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_all", "textures": {"all": _tx(spec, "all")}})
        layer_height = {1: "height2", 2: "height4", 3: "height6", 4: "height8",
                        5: "height10", 6: "height12", 7: "height14"}
        variants = {}
        for layers in range(1, 9):
            suffix = f"_{layer_height[layers]}" if layers in layer_height else ""
            variants[f"layers={layers}"] = {"model": f"{MODID}:block/{name}{suffix}"}
        write_json(assets / "blockstates" / f"{name}.json", {"variants": variants})
    elif model == "spike":
        # Custom ``WeatherSpikeBlock``: thickness (4) x vertical_direction (2) x
        # waterlogged (2) = 16 states; 4 segment models, ``down`` reuses them rotated.
        for thickness in ("tip", "frustum", "middle", "base"):
            write_json(assets / "models/block" / f"{name}_{thickness}.json", {
                "parent": f"minecraft:block/pointed_dripstone_up_{thickness}",
                "render_type": "minecraft:cutout",
                "textures": {"cross": f"{MODID}:block/{name}_{thickness}"}})
        # Canonical ``<name>.json`` (the widest segment): the base ``cross`` shape contract
        # the resource verifier checks requires it, and the item model parents it.
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/pointed_dripstone_up_base",
            "render_type": "minecraft:cutout",
            "textures": {"cross": f"{MODID}:block/{name}_base"}})
        variants = {}
        for thickness in ("tip", "frustum", "middle", "base"):
            for direction, rotation in (("up", {}), ("down", {"x": 180})):
                for waterlogged in (False, True):
                    variant = {"model": f"{MODID}:block/{name}_{thickness}"}
                    variant.update(rotation)
                    variants[f"thickness={thickness},vertical_direction={direction},"
                             f"waterlogged={str(waterlogged).lower()}"] = variant
        write_json(assets / "blockstates" / f"{name}.json", {"variants": variants})
    else:
        raise ValueError(f"unknown model: {model}")

    kind, ref = spec.get("item", (None, None))
    if kind is None:
        if model == "cross":
            kind, ref = "generated", f"{MODID}:block/{spec['textures'][0][0]}"
        elif model == "pane":
            # No <name>.json block model exists for a pane: the item must be a
            # standalone sprite. Reuse the pane body texture as layer0.
            kind, ref = "generated", _tx(spec, "pane")
        elif model == "lantern":
            # Vanilla-style lantern item sprite: generated + layer0 pointing at
            # the lantern block texture (the item is not the block model).
            kind, ref = "generated", _tx(spec, "lantern")
        else:
            kind, ref = "parent", name
    if kind == "generated":
        write_json(assets / "models/item" / f"{name}.json", {
            "parent": "minecraft:item/generated", "textures": {"layer0": ref}})
    elif kind == "none":
        pass
    else:
        write_json(assets / "models/item" / f"{name}.json", {"parent": f"{MODID}:block/{ref}"})


def write_block_loot(root: Path, spec) -> None:
    data = root / "src/main/resources/data" / MODID
    name = spec["name"]
    when = spec["loot"]
    if when[0] == "self":
        table = bd.self_loot(name)
    elif when[0] == "ore":
        table = bd.ore_loot(name, when[1])
    elif when[0] == "crystal_ore":
        table = bd.crystal_ore_loot(name, when[1])
    elif when[0] == "leaves":
        table = bd.leaves_loot(name, when[1])
    elif when[0] == "glass":
        table = bd.glass_loot(name)
    else:  # pragma: no cover
        raise ValueError(when)
    write_json(data / "loot_table/blocks" / f"{name}.json", table)


def write_crystal_item(root: Path, theme) -> None:
    assets = root / "src/main/resources/assets" / MODID
    (assets / "textures/item").mkdir(parents=True, exist_ok=True)
    img = recolor(read_png(ZIP, theme["crystal_src"]), theme["crystal_profile"])
    img.save(assets / "textures/item" / f"{theme['crystal']}.png")
    write_json(assets / "models/item" / f"{theme['crystal']}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{MODID}:item/{theme['crystal']}"},
    })


# --- tags / lang -------------------------------------------------------------
def write_tags(root: Path, covers, specs, themes, building_specs) -> None:
    mc_tags = root / "src/main/resources/data/minecraft/tags"

    pickaxe, axe, shovel, logs, leaves, small_flowers, flowers = [], [], [], [], [], [], []
    needs_stone, needs_iron, needs_diamond = [], [], []
    planks, wooden_stairs, wooden_slabs, wooden_fences, wooden_fence_gates = [], [], [], [], []
    wooden_doors, wooden_trapdoors, wooden_pressure_plates, wooden_buttons = [], [], [], []
    walls = []

    for theme in themes:
        names = [s["name"] for s in specs[theme["key"]]]
        pickaxe += [n for n in names
                    if n.endswith("_ore") or n in (theme["base"], theme["deep"], theme["crystal_block"])]
        wn = bd.wood_block_names(theme)
        axe += [wn["log"], wn["wood"], wn["stripped_log"]]
        logs += [wn["log"], wn["wood"], wn["stripped_log"]]
        leaves.append(wn["leaves"])
        for plant in theme["plants"]:
            if plant["flower"]:
                small_flowers.append(plant["name"])
                flowers.append(plant["name"])
        for ore in ("copper", "lapis"):
            needs_stone += [bd.shallow_ore(theme, ore), bd.deep_ore(theme, ore)]
        for ore in ("gold", "redstone", "emerald", "diamond"):
            needs_iron += [bd.shallow_ore(theme, ore), bd.deep_ore(theme, ore)]
        needs_diamond += [bd.shallow_crystal_ore(theme), bd.deep_crystal_ore(theme)]
    shovel += [c["name"] for c in covers]

    tag_targets = {"planks": planks, "logs": logs, "wooden_stairs": wooden_stairs,
                   "wooden_slabs": wooden_slabs, "wooden_fences": wooden_fences,
                   "fence_gates": wooden_fence_gates, "wooden_doors": wooden_doors,
                   "wooden_trapdoors": wooden_trapdoors,
                   "wooden_pressure_plates": wooden_pressure_plates,
                   "wooden_buttons": wooden_buttons, "walls": walls}
    for spec in building_specs:
        name = spec["name"]
        tool = spec.get("tool")
        if tool == "pickaxe":
            pickaxe.append(name)
        elif tool == "axe":
            axe.append(name)
        elif tool == "shovel":
            shovel.append(name)
        needs = spec.get("needs")
        if needs == "stone":
            needs_stone.append(name)
        elif needs == "iron":
            needs_iron.append(name)
        elif needs == "diamond":
            needs_diamond.append(name)
        for tag in spec.get("tags", []):
            tag_targets[tag].append(name)

    def mod(items):
        return [f"{MODID}:{n}" for n in items]

    merge_tag(mc_tags / "block/mineable/pickaxe.json", mod(pickaxe))
    merge_tag(mc_tags / "block/mineable/axe.json", mod(axe))
    merge_tag(mc_tags / "block/mineable/shovel.json", mod(shovel))
    merge_tag(mc_tags / "block/logs.json", mod(logs))
    merge_tag(mc_tags / "block/leaves.json", mod(leaves))
    merge_tag(mc_tags / "block/small_flowers.json", mod(small_flowers))
    merge_tag(mc_tags / "block/flowers.json", mod(flowers))
    merge_tag(mc_tags / "block/needs_stone_tool.json", mod(needs_stone))
    merge_tag(mc_tags / "block/needs_iron_tool.json", mod(needs_iron))
    merge_tag(mc_tags / "block/needs_diamond_tool.json", mod(needs_diamond))
    merge_tag(mc_tags / "block/walls.json", mod(walls))
    for fname in ("planks", "wooden_stairs", "wooden_slabs", "wooden_fences",
                  "fence_gates", "wooden_doors", "wooden_trapdoors",
                  "wooden_pressure_plates", "wooden_buttons"):
        merge_tag(mc_tags / "block" / f"{fname}.json", mod(tag_targets[fname]))
        merge_tag(mc_tags / "item" / f"{fname}.json", mod(tag_targets[fname]))
    merge_tag(mc_tags / "item/logs.json", mod(logs))


def write_lang(root: Path, covers, specs, themes, building_specs) -> None:
    lang_dir = root / "src/main/resources/assets" / MODID / "lang"
    en, zh = {}, {}
    for spec in covers:
        en[f"block.{MODID}.{spec['name']}"] = spec["en"]
        zh[f"block.{MODID}.{spec['name']}"] = spec["zh"]
    for theme in themes:
        for spec in specs[theme["key"]]:
            en[f"block.{MODID}.{spec['name']}"] = spec["en"]
            zh[f"block.{MODID}.{spec['name']}"] = spec["zh"]
        en[f"item.{MODID}.{theme['crystal']}"] = theme["crystal_en"]
        zh[f"item.{MODID}.{theme['crystal']}"] = theme["crystal_zh"]
    for spec in building_specs:
        en[f"block.{MODID}.{spec['name']}"] = spec["en"]
        zh[f"block.{MODID}.{spec['name']}"] = spec["zh"]

    for filename, table in (("en_us.json", en), ("zh_cn.json", zh)):
        path = lang_dir / filename
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        data.update(table)
        write_json(path, data)


def _dump_crafting_recipe(recipe) -> str:
    """Serialize a crafted recipe in the hand-authored wood-family style.

    Mirrors ``data/weather_realm/recipe/frost_*.json``: 2-space indent, LF,
    compact ``key``/``ingredients`` entries and no trailing newline.
    """
    lines = ["{"]
    items = list(recipe.items())
    for idx, (key, value) in enumerate(items):
        tail = "," if idx < len(items) - 1 else ""
        if key == "key":
            pairs = list(value.items())
            lines.append('  "key": {')
            for j, (k, v) in enumerate(pairs):
                comma = "," if j < len(pairs) - 1 else ""
                lines.append(f'    "{k}": {{ "item": "{v["item"]}" }}{comma}')
            lines.append("  }" + tail)
        elif key == "ingredients":
            lines.append('  "ingredients": [')
            for j, ing in enumerate(value):
                comma = "," if j < len(value) - 1 else ""
                field, ident = next(iter(ing.items()))
                lines.append(f'    {{ "{field}": "{ident}" }}{comma}')
            lines.append("  ]" + tail)
        elif key == "pattern":
            lines.append('  "pattern": [')
            for j, row in enumerate(value):
                comma = "," if j < len(value) - 1 else ""
                lines.append(f'    "{row}"{comma}')
            lines.append("  ]" + tail)
        elif key == "result":
            pairs = list(value.items())
            lines.append('  "result": {')
            for j, (k, v) in enumerate(pairs):
                comma = "," if j < len(pairs) - 1 else ""
                rendered = f'"{v}"' if isinstance(v, str) else v
                lines.append(f'    "{k}": {rendered}{comma}')
            lines.append("  }" + tail)
        else:
            rendered = f'"{value}"' if isinstance(value, str) else value
            lines.append(f'  "{key}": {rendered}{tail}')
    lines.append("}")
    return "\n".join(lines)


def write_recipes(root: Path, recipes) -> None:
    """Write ``data/weather_realm/recipe/*.json`` from ``bd.building_recipes()``.

    Never deletes anything and is idempotent: byte-identical files are left
    alone. A name that collides with an existing file of different content is
    refused, so a hand-authored recipe can never be silently clobbered.
    """
    out_dir = root / "src/main/resources/data" / MODID / "recipe"
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, recipe in recipes.items():
        path = out_dir / f"{name}.json"
        if recipe["type"] == "minecraft:smelting":
            text = json.dumps(recipe, indent=2, ensure_ascii=False) + "\n"
        else:
            text = _dump_crafting_recipe(recipe)
        payload = text.encode("utf-8")
        if path.exists() and path.read_bytes() != payload:
            raise SystemExit(
                f"[gen] refusing to overwrite existing recipe {path} "
                f"(name collision with different content)")
        path.write_bytes(payload)


# --- themed tree saplings ----------------------------------------------------
# ``frost_sapling`` is hand-authored; this additively derives the two non-frost saplings from its
# sprite so all three keep one silhouette. The recolor profiles are deterministic (no RNG), which
# is what lets ``gen_block_assets.py`` be re-run byte-for-byte.
SAPLING_SPECS = [
    # 干枯米褐 / dry beige: desaturate + warm hue, keep the value roughly.
    ("arid_sapling", dict(target_hue=0.13, sat_floor=0.20, sat_mul=0.55, val_mul=0.95,
                          protect_sat=None, speck_hue_shift=0.0)),
    # 焦黑棕 / charred brown: deep red-brown hue, pushed dark.
    ("scorched_sapling", dict(target_hue=0.045, sat_floor=0.35, sat_mul=0.85, val_mul=0.55,
                              protect_sat=None, speck_hue_shift=0.0)),
]
SAPLING_SOURCE = "frost_sapling"


def write_sapling_assets(root: Path) -> int:
    """Derive the two themed saplings from the frozen sprite (texture + models + self loot)."""
    assets = root / "src/main/resources/assets" / MODID
    data = root / "src/main/resources/data" / MODID
    source = assets / "textures/block" / f"{SAPLING_SOURCE}.png"
    if not source.is_file():
        raise FileNotFoundError(f"sapling source texture missing: {source}")
    source_img = Image.open(source).convert("RGBA")
    for name, profile in SAPLING_SPECS:
        recolor(source_img, profile, None, name).save(assets / "textures/block" / f"{name}.png")
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
            "textures": {"cross": f"{MODID}:block/{name}"}})
        write_json(assets / "blockstates" / f"{name}.json",
                   {"variants": {"": {"model": f"{MODID}:block/{name}"}}})
        write_json(assets / "models/item" / f"{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{MODID}:block/{name}"}})
        write_json(data / "loot_table/blocks" / f"{name}.json", bd.self_loot(name))
    return len(SAPLING_SPECS)


# --- frost raspberry bush ----------------------------------------------------
# ``frost_raspberry_bush`` is an age-0..3 sweet-berry clone, so its four stage sprites are
# derived from the vanilla sweet-berry sprites: the alpha channel is carried through
# byte-for-byte (identical silhouette / cutout) and only RGB is remapped -- stems/leaves to
# the frost-plant cyan palette, red berries to pale ice-blue fruit. Pure function of the
# source pixels, so re-running the generator is byte-for-byte idempotent.
FROST_BUSH_NAME = "frost_raspberry_bush"
FROST_BUSH_SOURCES = (
    "sweet_berry_bush_stage0", "sweet_berry_bush_stage1",
    "sweet_berry_bush_stage2", "sweet_berry_bush_stage3",
)


def recolor_frost_bush(img: Image.Image) -> Image.Image:
    """Deterministic foliage/berry recolor that never touches the alpha channel."""
    out = img.copy().convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            berry = s > 0.30 and (h < 0.12 or h > 0.88)
            if berry:
                # Red fruit -> pale ice-blue berries; keep the highlight shading.
                h = 0.55
                s = min(1.0, 0.30 + s * 0.55)
                v = min(1.0, 0.45 + v * 0.55)
            else:
                # Green stems / leaves -> frost-plant cyan, lifted into the bright frost range.
                h = 0.52 + (h - 0.37) * 0.25
                s = min(1.0, s * 0.95)
                v = min(1.0, 0.30 + v * 1.05)
            nr, ng, nb = colorsys.hsv_to_rgb(h, s, v)
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


def write_frost_raspberry_bush_assets(root: Path) -> int:
    """Derive the four frost-bush stage textures from the vanilla sprites."""
    out_dir = root / "src/main/resources/assets" / MODID / "textures/block"
    out_dir.mkdir(parents=True, exist_ok=True)
    for stage, source in enumerate(FROST_BUSH_SOURCES):
        with ZIP.open(f"assets/minecraft/textures/block/{source}.png") as fh:
            img = Image.open(io.BytesIO(fh.read())).convert("RGBA")
        recolor_frost_bush(img).save(out_dir / f"{FROST_BUSH_NAME}_stage{stage}.png")
    return len(FROST_BUSH_SOURCES)


# --- entry point -------------------------------------------------------------
def collect_building_specs():
    """Expand ``bd.BUILDING_FAMILIES`` into concrete block specs.

    ``kind`` is validated through ``bd.family_kind`` so a family missing that
    required field fails here instead of the verifier silently skipping it.
    """
    specs = []
    for fam in bd.BUILDING_FAMILIES:
        kind = bd.family_kind(fam)
        if kind == "stone":
            specs += bd.stone_specs(fam)
        elif kind == "wood":
            specs += bd.wood_specs(fam)
        elif kind == "lantern":
            pass  # expanded once below
        elif kind == "decoration":
            pass  # expanded once below
    specs += bd.lantern_specs()
    specs += bd.decoration_specs()
    specs += bd.cluster_specs()
    specs += bd.layer_specs()
    specs += bd.spike_specs()
    return specs


def run(root: Path) -> None:
    global ZIP
    jar = find_client_jar(root)
    print(f"[gen] vanilla client jar: {jar}")
    ZIP = zipfile.ZipFile(jar)

    covers = cover_specs()
    for spec in covers:
        write_block_client(root, spec)
        write_block_loot(root, spec)

    specs = {theme["key"]: block_specs(theme) for theme in bd.THEMES}
    total = len(covers)
    for theme in bd.THEMES:
        for spec in specs[theme["key"]]:
            write_block_client(root, spec)
            write_block_loot(root, spec)
            total += 1
        write_crystal_item(root, theme)
    building_specs = collect_building_specs()
    for spec in building_specs:
        write_block_client(root, spec)
        write_block_loot(root, spec)
        total += 1
    write_tags(root, covers, specs, bd.THEMES, building_specs)
    write_lang(root, covers, specs, bd.THEMES, building_specs)
    recipes = bd.building_recipes()
    write_recipes(root, recipes)
    print(f"[gen] wrote {len(recipes)} building recipes")
    saplings = write_sapling_assets(root)
    print(f"[gen] wrote {saplings} themed saplings")
    bushes = write_frost_raspberry_bush_assets(root)
    print(f"[gen] wrote {bushes} frost raspberry bush stage textures")
    ZIP.close()
    print(f"[gen] wrote textures + resources for {total} blocks "
          f"({2 * total} block textures) under {root}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parents[1]))
    ap.add_argument("--client-jar", default=None)
    args = ap.parse_args()
    if args.client_jar:
        os.environ["MC_CLIENT_JAR"] = args.client_jar
    try:
        run(Path(args.root).resolve())
    except bd.FamilyDataError as exc:
        sys.exit(f"[gen] {exc}")


ZIP: zipfile.ZipFile | None = None

if __name__ == "__main__":
    main()
