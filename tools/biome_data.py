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

_LANTERN_BASES = [
    ("frost", "坚冰", "Frost", "block/lantern", FROST_STONE_PROFILE),
    ("blaze", "燃焰", "Blaze", "block/lantern", THEMES[0]["stone_profile"]),
    ("wind", "风沙", "Wind", "block/lantern", THEMES[1]["stone_profile"]),
]
_DECORATION_BASES = [
    ("frost", "坚冰", "Frost", FROST_STONE_PROFILE),
    ("blaze", "燃焰", "Blaze", THEMES[0]["stone_profile"]),
    ("wind", "风沙", "Wind", THEMES[1]["stone_profile"]),
]
# 生态美术小物 / eco decor (sub-project D): 3 themes x (cluster, layer, spike).
# Cluster textures come from vanilla ``amethyst_cluster``, layers from ``snow`` and
# spikes from the four ``pointed_dripstone_up_*`` segment sprites, all recoloured with
# the theme's stone profile (design §7.7).
_ECO_BASES = [
    ("frost", "坚冰", "Frost", FROST_STONE_PROFILE, "snow"),
    ("blaze", "燃焰", "Blaze", THEMES[0]["stone_profile"], "ash"),
    ("wind", "风沙", "Wind", THEMES[1]["stone_profile"], "sand"),
]

# Phase B 为空；Phase C 用真实族填充。顺序：石（frost→fire→wind）→
# 木（scorched→arid）→ 灯笼（frost→blaze→wind）→ 装饰（frost→blaze→wind）。
BUILDING_FAMILIES = (
    [dict(kind="stone", **f) for f in STONE_BASES]
    + [dict(kind="wood", **f) for f in WOOD_BASES]
    + [dict(kind="lantern", name=f"{p}_lantern") for p, *_ in _LANTERN_BASES]
    + [dict(kind="decoration", prefix=p) for p, *_ in _DECORATION_BASES]
)


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
            # A door uses two distinct sprites: a hatched upper panel and a plank
            # lower panel. Mirror the vanilla oak_door_top / oak_door_bottom pair
            # (and the hand-authored frost_door_top / frost_door_bottom) instead of
            # collapsing both halves onto the planks texture.
            spec["tags"] = ["wooden_doors"]
            spec["tex"] = {"parent": f"{family['prefix']}_door_bottom",
                           "top": f"{family['prefix']}_door_top"}
            spec["textures"] = [
                (f"{family['prefix']}_door_bottom", "block/oak_door_bottom", family["profile"]),
                (f"{family['prefix']}_door_top", "block/oak_door_top", family["profile"]),
            ]
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


def lantern_specs():
    specs = []
    for prefix, zh, en, src, profile in _LANTERN_BASES:
        name = f"{prefix}_lantern"
        specs.append(dict(name=name, model="lantern", en=f"{en} Lantern", zh=f"{zh}灯笼",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(name, src, profile)], tex={"lantern": name},
                          item=("generated", f"{MODID}:item/{name}"),
                          item_textures=[(name, "item/lantern", profile)]))
    return specs


def decoration_specs():
    specs = []
    for prefix, zh, en, profile in _DECORATION_BASES:
        glass = f"{prefix}_glass"
        pane = f"{prefix}_glass_pane"
        grate = f"{prefix}_grate"
        chain = f"{prefix}_chain"
        specs.append(dict(name=glass, model="glass_block", en=f"{en} Glass", zh=f"{zh}玻璃",
                          loot=("glass",), tool=None, needs=None, tags=[],
                          textures=[(glass, "block/glass", profile)], tex={"all": glass},
                          item=("parent", glass)))
        specs.append(dict(name=pane, model="pane", en=f"{en} Glass Pane", zh=f"{zh}玻璃板",
                          loot=("glass",), tool=None, needs=None, tags=[],
                          textures=[(pane, "block/glass", profile),
                                    (f"{pane}_top", "block/glass_pane_top", profile)],
                          tex={"pane": pane, "edge": f"{pane}_top"},
                          item=("generated", f"{MODID}:block/{pane}")))
        specs.append(dict(name=grate, model="pane", en=f"{en} Grate", zh=f"{zh}格栅",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(grate, "block/iron_bars", profile)],
                          tex={"pane": grate, "edge": grate},
                          item=("generated", f"{MODID}:block/{grate}")))
        specs.append(dict(name=chain, model="chain", en=f"{en} Chain", zh=f"{zh}锁链",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(chain, "block/chain", profile)], tex={"all": chain},
                          item=("generated", f"{MODID}:item/{chain}"),
                          item_textures=[(chain, "item/chain", profile)]))
    return specs


# --- eco decor specs (sub-project D) ----------------------------------------
# ``model`` is the base parent a cluster/layer reuses (``minecraft:block/cross`` /
# ``cube_all``); ``shape`` drives the generator's dedicated ``cluster`` / ``layer`` /
# ``spike`` branches, which differ from those base shapes in the blockstate variant set
# and in the **extra segment models** (a layer's ``_height2..14``, a spike's ``_tip`` /
# ``_frustum`` / ``_middle`` / ``_base``). The resource verifier now dispatches on
# ``shape`` (``verify_building_assets.real_shape``) so those derived models are validated
# too; the base ``model`` alone would leave them unchecked.
def cluster_specs():
    """The 3 attachable crystal clusters (vanilla ``AmethystClusterBlock`` reuse).

    Real blockstate: ``facing`` (6) x ``waterlogged`` (2) = 12 combinations. The generated
    blockstate lists all 12 explicitly (design §6.2 / coordinator ruling).
    """
    specs = []
    for prefix, zh, en, profile, _word in _ECO_BASES:
        name = f"{prefix}_crystal_cluster"
        specs.append(dict(name=name, model="cross", shape="cluster",
                          en=f"{en} Crystal Cluster", zh=f"{zh}晶簇",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(name, "block/amethyst_cluster", profile)], tex={"cross": name},
                          item=("generated", f"{MODID}:block/{name}")))
    return specs


def layer_specs():
    """The 3 layered covers (vanilla ``SnowLayerBlock`` reuse).

    Real blockstate: ``layers`` (1..8) x ``waterlogged`` (2) = 16 combinations. The generated
    blockstate lists all 16 explicitly (design §6.3 / coordinator ruling).
    """
    specs = []
    for prefix, zh, en, profile, word in _ECO_BASES:
        name = f"{prefix}_{word}_layer"
        specs.append(dict(name=name, model="cube_all", shape="layer",
                          en={"snow": "Frost Snow Layer", "ash": "Volcanic Ash Layer",
                              "sand": "Wind Sand Layer"}[word],
                          zh={"snow": f"{zh}雪层", "ash": "火山灰层",
                              "sand": f"{zh}沙层"}[word],
                          loot=("self",), tool="shovel", needs=None, tags=[],
                          textures=[(name, "block/snow", profile)], tex={"all": name},
                          item=("generated", f"{MODID}:block/{name}")))
    return specs


def spike_specs():
    """The 3 decorative spikes (custom ``WeatherSpikeBlock``).

    Real blockstate: ``thickness`` (4) x ``vertical_direction`` (2) x ``waterlogged`` (2)
    = 16 combinations. The generated blockstate lists all 16 explicitly, reusing 4
    segment models x 2 direction rotations (design §6.4 / coordinator ruling).
    """
    specs = []
    for prefix, zh, en, profile, _word in _ECO_BASES:
        name = f"{prefix}_spike"
        specs.append(dict(name=name, model="cross", shape="spike",
                          en=f"{en} Spike", zh=f"{zh}尖锥",
                          loot=("self",), tool="pickaxe", needs=None, tags=[],
                          textures=[(f"{name}_{t}", f"block/pointed_dripstone_up_{t}", profile)
                                    for t in ("tip", "frustum", "middle", "base")],
                          item=("parent", f"{name}_base")))
    return specs


def glass_loot(block):
    """Vanilla glass semantics: only silk touch drops the block itself."""
    return {
        "type": "minecraft:block",
        "pools": [{
            "bonus_rolls": 0.0,
            "rolls": 1.0,
            "entries": [{
                "type": "minecraft:item",
                "name": f"{MODID}:{block}",
                "conditions": [
                    {"condition": "minecraft:match_tool",
                     "predicate": {"predicates": {"minecraft:enchantments": [
                         {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}},
                    {"condition": "minecraft:survives_explosion"},
                ],
            }],
        }],
        "random_sequence": f"{MODID}:blocks/{block}",
    }


def all_building_specs():
    """Expanded specs of every building family (stone/wood/lantern/decoration)."""
    specs = []
    for fam in BUILDING_FAMILIES:
        kind = family_kind(fam)
        if kind == "stone":
            specs += stone_specs(fam)
        elif kind == "wood":
            specs += wood_specs(fam)
        elif kind == "lantern":
            pass  # expanded once via lantern_specs(), not per family
        elif kind == "decoration":
            pass  # expanded once via decoration_specs(), not per family
    specs += lantern_specs()
    specs += decoration_specs()
    specs += cluster_specs()
    specs += layer_specs()
    specs += spike_specs()
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
    ids += [s["name"] for s in cluster_specs()]
    ids += [s["name"] for s in layer_specs()]
    ids += [s["name"] for s in spike_specs()]
    return ids


# ============================================================================
# 建筑方块配方（子项目 C / Task C5）
#   产出形状与数量对齐原版：磨制/砖 2×2→4、雕纹 2 竖→1、柱 2 竖→2、
#   楼梯 6 合 4、台阶 3 合 6、墙 6 合 6、木板 1 原木→4、木干 4 合 3、
#   栅栏 3、栅栏门 1、门 3、活板门 2、压力板 1、按钮 1、玻璃（烧炼）、
#   玻璃板 6 合 16、灯笼 1。裂砖走熔炉（原版惯例）。
# ============================================================================
def _shaped(pattern, key, result, count, category, group=None):
    recipe = {"type": "minecraft:crafting_shaped", "category": category}
    if group:
        recipe["group"] = group
    recipe["key"] = {k: {"item": v} for k, v in key.items()}
    recipe["pattern"] = pattern
    recipe["result"] = {"id": result, "count": count}
    return recipe


def _shapeless(ingredients, result, count, category, group=None):
    recipe = {"type": "minecraft:crafting_shapeless", "category": category}
    if group:
        recipe["group"] = group
    recipe["ingredients"] = [{"item": i} for i in ingredients]
    recipe["result"] = {"id": result, "count": count}
    return recipe


def _smelting(ingredient, result, category="blocks", group=None,
              cookingtime=200, experience=0.1):
    recipe = {"type": "minecraft:smelting", "category": category,
              "cookingtime": cookingtime, "experience": experience}
    if group:
        recipe["group"] = group
    recipe["ingredient"] = {"item": ingredient}
    recipe["result"] = {"id": result}
    return recipe


def building_recipes():
    """Every crafting/smelting recipe for the building blocks, keyed by file name.

    Keys are the recipe file stems (== result id without namespace). The id set
    is derived from ``STONE_BASES``/``WOOD_BASES``/``_DECORATION_BASES``/
    ``_LANTERN_BASES`` so names can never drift from what the Java side registers
    (note the singular ``<base>_brick_stairs`` used by ``_SHALLOW_DERIVED``).
    ``{prefix}_wood`` and ``stripped_{prefix}_wood`` reuse the pre-existing
    ``scorched_wood``/``arid_wood`` blocks.
    """
    r = {}
    # --- 石族：每族 20 条（浅层 11 + 深层 9），×3 = 60 ---
    for fam in STONE_BASES:
        for base, is_shallow in ((fam["name"], True), (fam["deep"], False)):
            b = f"{MODID}:{base}"
            pol, bricks = f"{base}_polished", f"{base}_bricks"
            r[pol] = _shaped(["##", "##"], {"#": b}, f"{MODID}:{pol}", 4, "building")
            r[bricks] = _shaped(["##", "##"], {"#": b}, f"{MODID}:{bricks}", 4, "building")
            r[f"{base}_chiseled"] = _shaped(["#", "#"], {"#": b},
                                            f"{MODID}:{base}_chiseled", 1, "building")
            r[f"{pol}_stairs"] = _shaped(["#  ", "## ", "###"], {"#": f"{MODID}:{pol}"},
                                         f"{MODID}:{pol}_stairs", 4, "building")
            r[f"{pol}_slab"] = _shaped(["###"], {"#": f"{MODID}:{pol}"},
                                       f"{MODID}:{pol}_slab", 6, "building")
            r[f"{pol}_wall"] = _shaped(["###", "###"], {"#": f"{MODID}:{pol}"},
                                       f"{MODID}:{pol}_wall", 6, "misc")
            r[f"{base}_brick_stairs"] = _shaped(["#  ", "## ", "###"], {"#": f"{MODID}:{bricks}"},
                                                f"{MODID}:{base}_brick_stairs", 4, "building")
            r[f"{base}_brick_slab"] = _shaped(["###"], {"#": f"{MODID}:{bricks}"},
                                              f"{MODID}:{base}_brick_slab", 6, "building")
            r[f"{base}_brick_wall"] = _shaped(["###", "###"], {"#": f"{MODID}:{bricks}"},
                                              f"{MODID}:{base}_brick_wall", 6, "misc")
            if is_shallow:  # cracked bricks 与 pillar 仅存在于浅层
                r[f"{base}_pillar"] = _shaped(["#", "#"], {"#": b},
                                              f"{MODID}:{base}_pillar", 2, "building")
                r[f"{base}_cracked_bricks"] = _smelting(
                    f"{MODID}:{bricks}", f"{MODID}:{base}_cracked_bricks")
    # --- 木族：每族 11 条（含复用既有 {prefix}_wood），×2 = 22 ---
    for fam in WOOD_BASES:
        p = fam["prefix"]
        log = f"{MODID}:{fam['log']}"
        slog = f"{MODID}:{fam['stripped_log']}"
        planks = f"{MODID}:{p}_planks"
        r[f"{p}_planks"] = _shapeless([log], planks, 4, "building", group="planks")
        r[f"{p}_wood"] = _shaped(["##", "##"], {"#": log},
                                 f"{MODID}:{p}_wood", 3, "building", group="bark")
        r[f"stripped_{p}_wood"] = _shaped(["##", "##"], {"#": slog},
                                          f"{MODID}:stripped_{p}_wood", 3, "building", group="bark")
        r[f"{p}_stairs"] = _shaped(["#  ", "## ", "###"], {"#": planks},
                                   f"{MODID}:{p}_stairs", 4, "building", group="wooden_stairs")
        r[f"{p}_slab"] = _shaped(["###"], {"#": planks},
                                 f"{MODID}:{p}_slab", 6, "building", group="wooden_slab")
        r[f"{p}_fence"] = _shaped(["#S#", "#S#"], {"#": planks, "S": "minecraft:stick"},
                                  f"{MODID}:{p}_fence", 3, "misc", group="wooden_fence")
        r[f"{p}_fence_gate"] = _shaped(["S#S", "S#S"], {"#": planks, "S": "minecraft:stick"},
                                       f"{MODID}:{p}_fence_gate", 1, "redstone",
                                       group="wooden_fence_gate")
        r[f"{p}_door"] = _shaped(["##", "##", "##"], {"#": planks},
                                 f"{MODID}:{p}_door", 3, "redstone", group="wooden_door")
        r[f"{p}_trapdoor"] = _shaped(["###", "###"], {"#": planks},
                                     f"{MODID}:{p}_trapdoor", 2, "redstone", group="wooden_trapdoor")
        r[f"{p}_pressure_plate"] = _shaped(["##"], {"#": planks},
                                           f"{MODID}:{p}_pressure_plate", 1, "redstone",
                                           group="wooden_pressure_plate")
        r[f"{p}_button"] = _shapeless([planks], f"{MODID}:{p}_button", 1, "redstone",
                                      group="wooden_button")
    # --- 玻璃 / 玻璃板：每主题 2 条，×3 = 6 ---
    for prefix, *_ in _DECORATION_BASES:
        r[f"{prefix}_glass"] = _smelting("minecraft:sand", f"{MODID}:{prefix}_glass")
        r[f"{prefix}_glass_pane"] = _shaped(["###", "###"], {"#": f"{MODID}:{prefix}_glass"},
                                            f"{MODID}:{prefix}_glass_pane", 16, "misc")
    # --- 灯笼：3 ---
    for prefix, *_ in _LANTERN_BASES:
        r[f"{prefix}_lantern"] = _shaped(
            ["NNN", "NTN", "NNN"],
            {"N": "minecraft:iron_nugget", "T": "minecraft:torch"},
            f"{MODID}:{prefix}_lantern", 1, "misc")
    return r
