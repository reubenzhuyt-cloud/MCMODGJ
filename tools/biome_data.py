"""Shared data tables for the Weather Realm blazing / arid biome generators.

The entry-point script imports this module:

    python tools/gen_block_assets.py        # textures + client/datapack resources

Everything here is plain data so the generator and any future worldgen script
stay in sync (DRY).  Names mirror the Java registrations in WeatherRealm.java.
"""

MODID = "weather_realm"

# --- Vanilla ore analogues ---------------------------------------------------
ORE_ORDER = ["coal", "copper", "iron", "gold", "redstone", "emerald", "lapis", "diamond"]

ORE_EN = {
    "coal": "Coal", "copper": "Copper", "iron": "Iron", "gold": "Gold",
    "redstone": "Redstone", "emerald": "Emerald", "lapis": "Lapis", "diamond": "Diamond",
}
ORE_ZH = {
    "coal": "煤", "copper": "铜", "iron": "铁", "gold": "金",
    "redstone": "红石", "emerald": "绿宝石", "lapis": "青金石", "diamond": "钻石",
}

# ore -> (drop_item_id, min_count, max_count, fortune_formula, bonus_multiplier_or_None)
ORE_DROPS = {
    "coal": ("minecraft:coal", 1, 1, "minecraft:ore_drops", None),
    "copper": ("minecraft:raw_copper", 2, 5, "minecraft:ore_drops", None),
    "iron": ("minecraft:raw_iron", 1, 1, "minecraft:ore_drops", None),
    "gold": ("minecraft:raw_gold", 1, 1, "minecraft:ore_drops", None),
    "redstone": ("minecraft:redstone", 4, 5, "minecraft:uniform_bonus_count", 1),
    "emerald": ("minecraft:emerald", 1, 1, "minecraft:ore_drops", None),
    "lapis": ("minecraft:lapis_lazuli", 4, 9, "minecraft:ore_drops", None),
    "diamond": ("minecraft:diamond", 1, 1, "minecraft:ore_drops", None),
}

# --- Biome surface covers ----------------------------------------------------
# One topsoil block per climate biome, recoloured from a vanilla texture.
SURFACE_COVERS = [
    {
        "name": "frost_moss", "en": "Frost Moss", "zh": "耐寒苔藓",
        "src": "block/moss_block",
        "profile": dict(target_hue=0.55, sat_floor=0.20, sat_mul=0.55, val_mul=1.05,
                        protect_sat=None, speck_hue_shift=0.0),
    },
    {
        "name": "dry_turf", "en": "Dry Turf", "zh": "干草坪",
        "src": "block/grass_block_top",
        "profile": dict(target_hue=0.13, sat_floor=0.35, sat_mul=0.85, val_mul=0.90,
                        protect_sat=None, speck_hue_shift=0.0),
    },
    {
        "name": "volcanic_ash", "en": "Volcanic Ash", "zh": "火山灰",
        "src": "block/basalt_top",
        "profile": dict(target_hue=0.02, sat_floor=0.05, sat_mul=0.60, val_mul=0.55,
                        protect_sat=None, speck_hue_shift=0.0),
    },
]

# --- Theme definitions -------------------------------------------------------
# Every entry is self-contained: names, vanilla source textures and recolor
# profiles for one climate biome.
THEMES = [
    {
        "key": "fire",
        "base": "fire_stone",
        "base_en": "Fire Stone",
        "base_zh": "火石",
        "deep": "deep_fire_stone",
        "deep_en": "Deep Fire Stone",
        "deep_zh": "深层火石",
        "base_src": "block/stone",
        "deep_src": "block/deepslate",
        # Vanilla ore backgrounds reuse the stone palette in their own arrangement and the
        # crystal ore uses a different profile, so composite for construction-level parity.
        "composite_ores": True,
        "stone_profile": dict(target_hue=0.045, sat_floor=0.35, sat_mul=1.0, val_mul=0.85,
                              protect_sat=0.55, speck_hue_shift=0.0),
        "deep_profile": dict(target_hue=0.020, sat_floor=0.35, sat_mul=1.0, val_mul=0.60,
                             protect_sat=0.55, speck_hue_shift=0.0),
        "crystal": "blaze_crystal",
        "crystal_en": "Blaze Crystal",
        "crystal_zh": "烈火晶石",
        "crystal_block": "blaze_crystal_block",
        "crystal_block_en": "Blaze Crystal Block",
        "crystal_block_zh": "烈火晶石块",
        "crystal_src": "item/amethyst_shard",
        "crystal_block_src": "block/amethyst_block",
        "crystal_ore_src": "block/diamond_ore",
        "deep_crystal_ore_src": "block/deepslate_diamond_ore",
        "crystal_profile": dict(target_hue=0.06, sat_floor=0.50, sat_mul=1.0, val_mul=1.0,
                                protect_sat=None, speck_hue_shift=0.0),
        "crystal_ore_profile": dict(target_hue=0.05, sat_floor=0.35, sat_mul=1.0, val_mul=0.90,
                                    protect_sat=0.50, speck_hue_shift=-0.45),
        "deep_crystal_ore_profile": dict(target_hue=0.03, sat_floor=0.35, sat_mul=1.0, val_mul=0.70,
                                         protect_sat=0.50, speck_hue_shift=-0.45),
        "wood": "scorched",
        "wood_src": "oak",
        "wood_profile": dict(target_hue=0.030, sat_floor=0.15, sat_mul=0.85, val_mul=0.32,
                             protect_sat=None, speck_hue_shift=0.0),
        "leaves_profile": dict(target_hue=0.040, sat_floor=0.45, sat_mul=1.0, val_mul=0.55,
                               protect_sat=None, speck_hue_shift=0.0),
        "wood_names_en": {
            "log": "Scorched Log", "wood": "Scorched Wood",
            "stripped_log": "Stripped Scorched Log", "leaves": "Scorched Leaves",
        },
        "wood_names_zh": {
            "log": "焦木原木", "wood": "焦木",
            "stripped_log": "去皮焦木原木", "leaves": "焦木树叶",
        },
        "plants": [
            {"name": "cinder_bloom", "en": "Cinder Bloom", "zh": "灰烬兰",
             "src": "block/dandelion", "flower": True,
             "profile": dict(target_hue=0.00, sat_floor=0.60, sat_mul=1.0, val_mul=0.90,
                             protect_sat=None, speck_hue_shift=0.0)},
            {"name": "flame_sprout", "en": "Flame Sprout", "zh": "烈焰草幼芽",
             "src": "block/short_grass", "flower": False,
             "profile": dict(target_hue=0.035, sat_floor=0.50, sat_mul=1.0, val_mul=1.05,
                             protect_sat=None, speck_hue_shift=0.0)},
            {"name": "fire_flower", "en": "Fire Flower", "zh": "火晶花",
             "src": "block/dandelion", "flower": True,
             "profile": dict(target_hue=0.06, sat_floor=0.70, sat_mul=1.0, val_mul=1.10,
                             protect_sat=None, speck_hue_shift=0.0)},
        ],
    },
    {
        "key": "wind",
        "base": "weathered_sandstone",
        "base_en": "Weathered Sandstone",
        "base_zh": "风化砂石",
        "deep": "deep_weathered_sandstone",
        "deep_en": "Deep Weathered Sandstone",
        "deep_zh": "深层风化砂石",
        "base_src": "block/sandstone",
        "deep_src": "block/sandstone",
        # Ores are composited onto our own sandstone so their background matches the rock
        # (the legacy whole-texture recolor left a grey stone background under the sand).
        "composite_ores": True,
        "stone_profile": dict(target_hue=0.11, sat_floor=0.28, sat_mul=0.80, val_mul=1.00,
                              protect_sat=0.55, speck_hue_shift=0.0),
        "deep_profile": dict(target_hue=0.09, sat_floor=0.30, sat_mul=0.85, val_mul=0.62,
                             protect_sat=0.55, speck_hue_shift=0.0),
        "crystal": "wind_crystal",
        "crystal_en": "Wind Crystal",
        "crystal_zh": "风沙晶石",
        "crystal_block": "wind_crystal_block",
        "crystal_block_en": "Wind Crystal Block",
        "crystal_block_zh": "风沙晶石块",
        "crystal_src": "item/amethyst_shard",
        "crystal_block_src": "block/amethyst_block",
        "crystal_ore_src": "block/diamond_ore",
        "deep_crystal_ore_src": "block/deepslate_diamond_ore",
        "crystal_profile": dict(target_hue=0.12, sat_floor=0.45, sat_mul=1.0, val_mul=1.0,
                                protect_sat=None, speck_hue_shift=0.0),
        "crystal_ore_profile": dict(target_hue=0.11, sat_floor=0.28, sat_mul=1.0, val_mul=1.00,
                                    protect_sat=0.50, speck_hue_shift=-0.38),
        "deep_crystal_ore_profile": dict(target_hue=0.10, sat_floor=0.30, sat_mul=1.0, val_mul=0.70,
                                         protect_sat=0.50, speck_hue_shift=-0.38),
        "wood": "arid",
        "wood_src": "birch",
        "wood_profile": dict(target_hue=0.090, sat_floor=0.22, sat_mul=1.0, val_mul=0.95,
                             protect_sat=None, speck_hue_shift=0.0),
        "leaves_profile": dict(target_hue=0.130, sat_floor=0.35, sat_mul=1.0, val_mul=0.85,
                               protect_sat=None, speck_hue_shift=0.0),
        "wood_names_en": {
            "log": "Arid Log", "wood": "Arid Wood",
            "stripped_log": "Stripped Arid Log", "leaves": "Arid Leaves",
        },
        "wood_names_zh": {
            "log": "风化原木", "wood": "风化木",
            "stripped_log": "去皮风化原木", "leaves": "风化树叶",
        },
        "plants": [
            {"name": "dune_flower", "en": "Dune Flower", "zh": "沙丘花",
             "src": "block/dandelion", "flower": True,
             "profile": dict(target_hue=0.13, sat_floor=0.55, sat_mul=1.0, val_mul=1.0,
                             protect_sat=None, speck_hue_shift=0.0)},
            {"name": "wind_sprout", "en": "Wind Sprout", "zh": "风生草",
             "src": "block/fern", "flower": False,
             "profile": dict(target_hue=0.13, sat_floor=0.35, sat_mul=1.0, val_mul=1.0,
                             protect_sat=None, speck_hue_shift=0.0)},
            {"name": "arid_bush", "en": "Arid Bush", "zh": "风沙灌木",
             "src": "block/dead_bush", "flower": False,
             "profile": dict(target_hue=0.09, sat_floor=0.30, sat_mul=1.0, val_mul=0.90,
                             protect_sat=None, speck_hue_shift=0.0)},
        ],
    },
]


def wood_block_names(theme):
    """Return the four generated block IDs of a theme's tree family."""
    w = theme["wood"]
    return {
        "log": f"{w}_log",
        "wood": f"{w}_wood",
        "stripped_log": f"stripped_{w}_log",
        "leaves": f"{w}_leaves",
    }


def shallow_ore(theme, ore):
    return f"{theme['base']}_{ore}_ore"


def deep_ore(theme, ore):
    return f"deep_{theme['base']}_{ore}_ore"


def shallow_crystal_ore(theme):
    return f"{theme['base']}_{theme['crystal']}_ore"


def deep_crystal_ore(theme):
    return f"deep_{theme['base']}_{theme['crystal']}_ore"


def ore_loot(block, ore):
    """Silk-touch / fortune aware loot table for a vanilla-ore analogue."""
    item, lo, hi, formula, mult = ORE_DROPS[ore]
    funcs = []
    if (lo, hi) != (1, 1):
        funcs.append({
            "add": False,
            "count": {"type": "minecraft:uniform", "max": float(hi), "min": float(lo)},
            "function": "minecraft:set_count",
        })
    bonus = {
        "enchantment": "minecraft:fortune",
        "formula": formula,
        "function": "minecraft:apply_bonus",
    }
    if mult is not None:
        bonus["parameters"] = {"bonusMultiplier": mult}
    funcs.append(bonus)
    funcs.append({"function": "minecraft:explosion_decay"})
    return _ore_loot(block, item, funcs)


def crystal_ore_loot(block, crystal):
    """Silk-touch / fortune aware loot table dropping a mod crystal item."""
    funcs = [
        {"enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops", "function": "minecraft:apply_bonus"},
        {"function": "minecraft:explosion_decay"},
    ]
    return _ore_loot(block, f"{MODID}:{crystal}", funcs)


def _ore_loot(block, drop_item, functions):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "rolls": 1.0,
                "entries": [
                    {
                        "type": "minecraft:alternatives",
                        "children": [
                            {
                                "type": "minecraft:item",
                                "name": f"{MODID}:{block}",
                                "conditions": [
                                    {
                                        "condition": "minecraft:match_tool",
                                        "predicate": {
                                            "predicates": {
                                                "minecraft:enchantments": [
                                                    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}
                                                ]
                                            }
                                        },
                                    }
                                ],
                            },
                            {"type": "minecraft:item", "name": drop_item, "functions": functions},
                        ],
                    }
                ],
            }
        ],
        "random_sequence": f"{MODID}:blocks/{block}",
    }


def self_loot(block):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "rolls": 1.0,
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{block}"}],
            }
        ],
        "random_sequence": f"{MODID}:blocks/{block}",
    }


def leaves_loot(block):
    """Vanilla-style leaf loot: shears/silk-touch drop the leaf, else a stick."""
    fortune = {
        "function": "minecraft:set_count",
        "conditions": [
            {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
             "chances": [0.05, 0.0625, 0.083333336, 0.1]}
        ],
        "count": 1.0,
    }
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "rolls": 1.0,
                "entries": [
                    {
                        "type": "minecraft:alternatives",
                        "children": [
                            {
                                "type": "minecraft:item",
                                "conditions": [
                                    {
                                        "condition": "minecraft:any_of",
                                        "terms": [
                                            {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
                                            {
                                                "condition": "minecraft:match_tool",
                                                "predicate": {
                                                    "predicates": {
                                                        "minecraft:enchantments": [
                                                            {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}
                                                        ]
                                                    }
                                                },
                                            },
                                        ],
                                    }
                                ],
                                "name": f"{MODID}:{block}",
                            },
                            {
                                "type": "minecraft:item",
                                "conditions": [{"condition": "minecraft:survives_explosion"}],
                                "name": "minecraft:stick",
                                "functions": [dict(fortune)],
                            },
                        ],
                    }
                ],
            },
        ],
        "random_sequence": f"{MODID}:blocks/{block}",
    }


# ============================================================================
# 建筑方块族（子项目 C）/ Building-block families
# ============================================================================
THEME_ORDER = ["frost", "fire", "wind"]

FROST_STONE_PROFILE = dict(target_hue=0.55, sat_floor=0.10, sat_mul=0.55, val_mul=1.02,
                           protect_sat=None, speck_hue_shift=0.0)
FROST_DEEP_PROFILE = dict(target_hue=0.55, sat_floor=0.10, sat_mul=0.50, val_mul=0.62,
                          protect_sat=None, speck_hue_shift=0.0)

STONE_BASES = [
    dict(theme="frost", name="permafrost", deep="deep_permafrost",
         en="Permafrost", zh="冻土", deep_en="Deep Permafrost", deep_zh="深层冻土",
         stone_profile=FROST_STONE_PROFILE, deep_profile=FROST_DEEP_PROFILE),
    dict(theme="fire", name="fire_stone", deep="deep_fire_stone",
         en="Fire Stone", zh="火石", deep_en="Deep Fire Stone", deep_zh="深层火石",
         stone_profile=THEMES[0]["stone_profile"], deep_profile=THEMES[0]["deep_profile"]),
    dict(theme="wind", name="weathered_sandstone", deep="deep_weathered_sandstone",
         en="Weathered Sandstone", zh="风化砂石",
         deep_en="Deep Weathered Sandstone", deep_zh="深层风化砂石",
         stone_profile=THEMES[1]["stone_profile"], deep_profile=THEMES[1]["deep_profile"]),
]

WOOD_BASES = [
    dict(theme="fire", prefix="scorched", en="Scorched", zh="焦木",
         planks_src="block/oak_planks", profile=THEMES[0]["wood_profile"],
         log="scorched_log", stripped_log="stripped_scorched_log"),
    dict(theme="wind", prefix="arid", en="Arid", zh="风化木",
         planks_src="block/oak_planks", profile=THEMES[1]["wood_profile"],
         log="arid_log", stripped_log="stripped_arid_log"),
]

# Phase B 为空；Phase C 用真实族填充。
BUILDING_FAMILIES = []


class FamilyDataError(ValueError):
    """A building-family declaration is malformed (missing/unknown ``kind``)."""


def _family_id(fam) -> str:
    """Best-effort stable identifier for error messages."""
    for key in ("name", "prefix", "key", "theme"):
        value = fam.get(key)
        if value:
            return str(value)
    return "<unnamed>"


# Every kind the pipeline understands; keep in sync with the dispatch below,
# in ``gen_block_assets.collect_building_specs`` and in the design doc.
BUILDING_KINDS = ("stone", "wood", "lantern", "decoration")


def family_kind(fam) -> str:
    """Return ``fam['kind']``, failing loudly when it is absent or unknown.

    Both the verifier (``all_building_block_ids``/``all_building_specs``) and the
    generator (``gen_block_assets.collect_building_specs``) route through here so
    a forgotten ``kind`` can never make the verifier green while the generator
    crashes.
    """
    if "kind" not in fam:
        raise FamilyDataError(
            f"family '{_family_id(fam)}' missing required field 'kind'")
    kind = fam["kind"]
    if kind not in BUILDING_KINDS:
        raise FamilyDataError(
            f"family '{_family_id(fam)}' has unknown kind {kind!r} "
            f"(expected one of {', '.join(BUILDING_KINDS)})")
    return kind


def build_stone_families():
    return list(STONE_BASES)


def build_wood_families():
    return list(WOOD_BASES)


# (suffix, en_suffix, zh_suffix, model, needs_parent_model)
_SHALLOW_DERIVED = [
    ("polished", "Polished", "磨制", "cube_all", False),
    ("polished_stairs", "Polished Stairs", "磨制楼梯", "stairs", True),
    ("polished_slab", "Polished Slab", "磨制台阶", "slab", True),
    ("polished_wall", "Polished Wall", "磨制墙", "wall", True),
    ("bricks", "Bricks", "砖", "cube_all", False),
    ("brick_stairs", "Brick Stairs", "砖楼梯", "stairs", True),
    ("brick_slab", "Brick Slab", "砖台阶", "slab", True),
    ("brick_wall", "Brick Wall", "砖墙", "wall", True),
    ("cracked_bricks", "Cracked Bricks", "裂纹砖", "cube_all", False),
    ("chiseled", "Chiseled", "雕纹", "cube_all", False),
    ("pillar", "Pillar", "柱", "pillar", True),
]
_DEEP_DERIVED = [d for d in _SHALLOW_DERIVED if d[0] not in ("cracked_bricks", "pillar")]


def _stone_display(base_en, base_zh, suffix_en, suffix_zh):
    # 磨制/雕纹 为限定词前置，其余为族名前置，与设计文档 §9.3 样例一致。
    if suffix_en in ("Polished", "Chiseled"):
        return f"{suffix_en} {base_en}", f"{suffix_zh}{base_zh}"
    return f"{base_en} {suffix_en}", f"{base_zh}{suffix_zh}"


def _is_deep(name):
    return name.startswith("deep_")


def stone_specs(family):
    specs = []
    for base_name, base_en, base_zh, profile, is_deep in (
            (family["name"], family["en"], family["zh"], family["stone_profile"], False),
            (family["deep"], family["deep_en"], family["deep_zh"], family["deep_profile"], True)):
        derived = _DEEP_DERIVED if is_deep else _SHALLOW_DERIVED
        for suffix, sen, szh, model, has_parent in derived:
            name = f"{base_name}_{suffix}"
            en, zh = _stone_display(base_en, base_zh, sen, szh)
            spec = dict(name=name, model=model, en=en, zh=zh, loot=("self",),
                        tool="pickaxe", needs="iron" if is_deep else "stone",
                        textures=[], textures_src=[], item=("parent", name), tags=[])
            if has_parent:
                spec["tex"] = {"parent": f"{base_name}_{_parent_of(suffix)}"}
            if model == "slab":
                spec["tex"] = {"parent": f"{base_name}_{_parent_of(suffix)}",
                               "double": f"{base_name}_{_parent_of(suffix)}"}
            if model == "wall":
                spec["tags"] = ["walls"]
                spec["item"] = ("parent", name + "_inventory")
            if not has_parent:  # polished / bricks / cracked / chiseled -> 新贴图
                src = {"polished": "block/polished_deepslate", "bricks": "block/deepslate_bricks",
                       "cracked_bricks": "block/cracked_deepslate_bricks",
                       "chiseled": "block/chiseled_deepslate"}[suffix]
                spec["textures"] = [(name, src, profile)]
                spec["tex"] = {"parent": name}
            if model == "pillar":
                spec["textures"] = [(f"{name}", "block/deepslate", profile),
                                    (f"{name}_top", "block/deepslate_top", profile)]
                spec["pillar_side"] = name
                spec["pillar_top"] = f"{name}_top"
            specs.append(spec)
    return specs


def _parent_of(suffix):
    if suffix.startswith("polished"):
        return "polished"
    if suffix.startswith("brick") or suffix in ("bricks",):
        return "bricks"
    return suffix


_WOOD_DERIVED = [
    ("planks", "Planks", "木板", "cube_all"),
    ("stairs", "Stairs", "楼梯", "stairs"),
    ("slab", "Slab", "台阶", "slab"),
    ("fence", "Fence", "栅栏", "fence"),
    ("fence_gate", "Fence Gate", "栅栏门", "fence_gate"),
    ("door", "Door", "门", "door"),
    ("trapdoor", "Trapdoor", "活板门", "trapdoor"),
    ("pressure_plate", "Pressure Plate", "压力板", "pressure_plate"),
    ("button", "Button", "按钮", "button"),
]


def wood_specs(family):
    specs = [dict(name=f"stripped_{family['prefix']}_wood", model="pillar",
                  en=f"Stripped {family['en']} Wood", zh=f"去皮{family['zh']}",
                  loot=("self",), tool="axe", needs=None, tags=["logs"],
                  textures=[], tex={"parent": family["stripped_log"]},
                  pillar_side=family["stripped_log"], pillar_top=family["stripped_log"])]
    for suffix, sen, szh, model in _WOOD_DERIVED:
        name = f"{family['prefix']}_{suffix}"
        spec = dict(name=name, model=model, en=f"{family['en']} {sen}", zh=f"{family['zh']}{szh}",
                    loot=("self",), tool="axe", needs=None, tags=[], tex={"parent": f"{family['prefix']}_planks"},
                    textures=[], item=("parent", name))
        if suffix == "planks":
            spec["textures"] = [(name, family["planks_src"], family["profile"])]
            spec["tex"] = {"parent": name}
            spec["tags"] = ["planks"]
        elif suffix == "stairs":
            spec["tags"] = ["wooden_stairs"]
        elif suffix == "slab":
            spec["tags"] = ["wooden_slabs"]
            spec["tex"] = {"parent": f"{family['prefix']}_planks", "double": f"{family['prefix']}_planks"}
        elif suffix == "fence":
            spec["tags"] = ["wooden_fences"]
        elif suffix == "fence_gate":
            spec["tags"] = ["fence_gates"]
        elif suffix == "door":
            spec["tags"] = ["wooden_doors"]
            spec["item"] = ("generated", "weather_realm:item/" + name)
            spec["item_textures"] = [(name, "item/oak_door", family["profile"])]
        elif suffix == "trapdoor":
            spec["tags"] = ["wooden_trapdoors"]
            spec["item"] = ("parent", name + "_bottom")
        elif suffix == "pressure_plate":
            spec["tags"] = ["wooden_pressure_plates"]
        elif suffix == "button":
            spec["tags"] = ["wooden_buttons"]
            spec["item"] = ("parent", name + "_inventory")
        if model == "fence":
            spec["item"] = ("parent", name + "_inventory")
        if model == "wall":
            spec["item"] = ("parent", name + "_inventory")
        specs.append(spec)
    return specs


def all_building_specs():
    """Expanded specs of every family that produces block specs (stone/wood).

    ``lantern``/``decoration`` families are registered as bare ids elsewhere and
    carry no per-shape spec, so they contribute no tag/tex expectations.
    """
    specs = []
    for fam in BUILDING_FAMILIES:
        kind = family_kind(fam)
        if kind == "stone":
            specs += stone_specs(fam)
        elif kind == "wood":
            specs += wood_specs(fam)
    return specs


def all_building_block_ids():
    ids = []
    for fam in BUILDING_FAMILIES:
        kind = family_kind(fam)
        if kind == "stone":
            ids += [s["name"] for s in stone_specs(fam)]
        elif kind == "wood":
            ids += [s["name"] for s in wood_specs(fam)]
        elif kind == "lantern":
            ids.append(fam["name"])
        elif kind == "decoration":
            ids += [f"{fam['prefix']}_{s}" for s in ("glass", "glass_pane", "grate", "chain")]
    return ids
