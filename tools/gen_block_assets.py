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


def recolor(img: Image.Image, profile: dict) -> Image.Image:
    """Hue/saturation/value recolor. Saturated 'speck' pixels are protected."""
    out = img.copy().convert("RGBA")
    px = out.load()
    target_hue = profile.get("target_hue")
    sat_floor = profile.get("sat_floor", 0.0)
    sat_mul = profile.get("sat_mul", 1.0)
    val_mul = profile.get("val_mul", 1.0)
    protect_sat = profile.get("protect_sat")
    speck_shift = profile.get("speck_hue_shift", 0.0)
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            if protect_sat is not None and ss >= protect_sat:
                hh = (hh + speck_shift) % 1.0
                vv = min(1.0, vv * val_mul)
            else:
                hh = target_hue if target_hue is not None else hh
                ss = min(1.0, max(ss, sat_floor) * sat_mul)
                vv = min(1.0, vv * val_mul)
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
        zh=theme["wood_names_zh"]["leaves"], loot=("leaves",),
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
    for out_tex, src_tex, profile in spec.get("textures", []):
        if composite is not None:
            background = Image.open(
                assets / "textures/block" / f"{composite['background']}.png").convert("RGBA")
            img = compose_ore(read_png(ZIP, src_tex), read_png(ZIP, composite["vanilla_base"]),
                              profile, background)
        else:
            img = recolor(read_png(ZIP, src_tex), profile)
        img.save(assets / "textures/block" / f"{out_tex}.png")
    for out_tex, src_tex, profile in spec.get("item_textures", []):
        recolor(read_png(ZIP, src_tex), profile).save(assets / "textures/item" / f"{out_tex}.png")

    model = spec.get("shape", spec["model"])
    if model == "pillar":
        side, top = spec["pillar_side"], spec["pillar_top"]
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": f"{MODID}:block/{top}", "side": f"{MODID}:block/{side}"}})
        write_json(assets / "models/block" / f"{name}_horizontal.json", {
            "parent": "minecraft:block/cube_column_horizontal",
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
            "parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{texture}"}})
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
                "parent": f"minecraft:block/{parent}",
                "textures": {"bottom": _tx(spec, "parent"), "side": _tx(spec, "parent"),
                             "top": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_stairs", [
            ("minecraft:block/oak_stairs_inner", f"{MODID}:block/{name}_inner"),
            ("minecraft:block/oak_stairs_outer", f"{MODID}:block/{name}_outer"),
            ("minecraft:block/oak_stairs", f"{MODID}:block/{name}")])
    elif model == "slab":
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/slab",
            "textures": {"bottom": _tx(spec, "parent"), "side": _tx(spec, "parent"),
                         "top": _tx(spec, "parent")}})
        write_json(assets / "models/block" / f"{name}_top.json", {
            "parent": "minecraft:block/slab_top",
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
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_fence", [
            ("minecraft:block/oak_fence_post", f"{MODID}:block/{name}_post"),
            ("minecraft:block/oak_fence_side", f"{MODID}:block/{name}_side")])
    elif model == "fence_gate":
        for suffix, parent in (("", "template_fence_gate"), ("_open", "template_fence_gate_open"),
                               ("_wall", "template_fence_gate_wall"),
                               ("_wall_open", "template_fence_gate_wall_open")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
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
                "parent": f"minecraft:block/{parent}", "textures": tex})
        write_template_blockstate(assets, name, "oak_door", [
            (f"minecraft:block/oak_door_{part}", f"{MODID}:block/{name}_{part}")
            for part in ("bottom_left_open", "bottom_left", "bottom_right_open", "bottom_right",
                         "top_left_open", "top_left", "top_right_open", "top_right")])
    elif model == "trapdoor":
        for suffix, parent in (("_bottom", "template_trapdoor_bottom"),
                               ("_top", "template_trapdoor_top"), ("_open", "template_trapdoor_open")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_trapdoor", [
            ("minecraft:block/oak_trapdoor_bottom", f"{MODID}:block/{name}_bottom"),
            ("minecraft:block/oak_trapdoor_top", f"{MODID}:block/{name}_top"),
            ("minecraft:block/oak_trapdoor_open", f"{MODID}:block/{name}_open")])
    elif model == "button":
        for suffix, parent in (("", "button"), ("_pressed", "button_pressed"),
                               ("_inventory", "button_inventory")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
        write_template_blockstate(assets, name, "oak_button", [
            ("minecraft:block/oak_button_pressed", f"{MODID}:block/{name}_pressed"),
            ("minecraft:block/oak_button", f"{MODID}:block/{name}")])
    elif model == "pressure_plate":
        for suffix, parent in (("", "pressure_plate_up"), ("_down", "pressure_plate_down")):
            write_json(assets / "models/block" / f"{name}{suffix}.json", {
                "parent": f"minecraft:block/{parent}", "textures": {"texture": _tx(spec, "parent")}})
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
        # Vanilla ``SnowLayerBlock``: layers (1..8) x waterlogged (2) = 16 states.
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
            for waterlogged in (False, True):
                variants[f"layers={layers},waterlogged={str(waterlogged).lower()}"] = {
                    "model": f"{MODID}:block/{name}{suffix}"}
        write_json(assets / "blockstates" / f"{name}.json", {"variants": variants})
    elif model == "spike":
        # Custom ``WeatherSpikeBlock``: thickness (4) x vertical_direction (2) x
        # waterlogged (2) = 16 states; 4 segment models, ``down`` reuses them rotated.
        for thickness in ("tip", "frustum", "middle", "base"):
            write_json(assets / "models/block" / f"{name}_{thickness}.json", {
                "parent": f"minecraft:block/pointed_dripstone_up_{thickness}",
                "textures": {"cross": f"{MODID}:block/{name}_{thickness}"}})
        # Canonical ``<name>.json`` (the widest segment): the base ``cross`` shape contract
        # the resource verifier checks requires it, and the item model parents it.
        write_json(assets / "models/block" / f"{name}.json", {
            "parent": "minecraft:block/pointed_dripstone_up_base",
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
        table = bd.leaves_loot(name)
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
