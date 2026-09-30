#!/usr/bin/env python3
"""Self-check the generated ore textures against their own rock texture.

Composite themes (``composite_ores`` in :mod:`biome_data`) build ores by pasting the
vanilla mineral pixels onto our rock texture, so every non-mineral pixel of the ore
must be byte-for-byte identical to the rock texture. The mineral mask comes from the
*vanilla* ore texture: a pixel is mineral when its colour is absent from the vanilla
rock (stone / deepslate) palette.

Legacy themes recolor the vanilla ore wholesale, so their background is only the same
*palette* as the rock (different arrangement) -- the script reports that figure for the
audit but does not fail on it.

Usage (from the repo root, after ``python tools/gen_block_assets.py``):

    python tools/verify_ore_textures.py
"""
from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import biome_data as bd  # noqa: E402
import gen_block_assets as gba  # noqa: E402

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("Pillow is required:  pip install Pillow")


def ore_rows(theme):
    """Yield (output_name, vanilla_ore_src, vanilla_rock_src, rock_out_name) for a theme."""
    for ore in bd.ORE_ORDER:
        yield bd.shallow_ore(theme, ore), f"block/{ore}_ore", "block/stone", theme["base"]
        yield bd.deep_ore(theme, ore), f"block/deepslate_{ore}_ore", "block/deepslate", theme["deep"]
    yield bd.shallow_crystal_ore(theme), theme["crystal_ore_src"], "block/stone", theme["base"]
    yield bd.deep_crystal_ore(theme), theme["deep_crystal_ore_src"], "block/deepslate", theme["deep"]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(Path(__file__).resolve().parents[1]))
    args = ap.parse_args()
    root = Path(args.root).resolve()
    assets = root / "src/main/resources/assets" / bd.MODID / "textures/block"
    palette_cache = {}

    def vanilla_palette(zf, rel):
        if rel not in palette_cache:
            palette_cache[rel] = gba.palette(gba.read_png(zf, rel))
        return palette_cache[rel]

    failures = 0
    with zipfile.ZipFile(gba.find_client_jar(root)) as zf:
        for theme in bd.THEMES:
            composite = bool(theme.get("composite_ores"))
            print(f"\n=== theme '{theme['key']}' (composite_ores={composite}) ===")
            for name, ore_src, rock_src, rock_name in ore_rows(theme):
                ore = Image.open(assets / f"{name}.png").convert("RGBA")
                rock = Image.open(assets / f"{rock_name}.png").convert("RGBA")
                if ore.size != (16, 16) or rock.size != (16, 16):
                    print(f"  [FAIL] {name}: bad size ore={ore.size} rock={rock.size}")
                    failures += 1
                    continue
                alphas = {ore.load()[x, y][3] for y in range(16) for x in range(16)}
                if alphas != {255}:
                    print(f"  [FAIL] {name}: unexpected alpha values {sorted(alphas)}")
                    failures += 1
                    continue

                src = gba.read_png(zf, ore_src)
                ps, po, pr = src.load(), ore.load(), rock.load()
                rock_pal = vanilla_palette(zf, rock_src)
                mask = {(x, y) for y in range(16) for x in range(16) if ps[x, y] not in rock_pal}
                diff = sum(1 for y in range(16) for x in range(16)
                           if (x, y) not in mask and po[x, y] != pr[x, y])
                if composite:
                    status = "ok" if diff == 0 else "MISMATCH"
                    if diff:
                        failures += 1
                    print(f"  [{status:8s}] {name:42s} background-vs-rock diff={diff:2d} "
                          f"mineral_px={len(mask):3d}")
                else:
                    same = sum(1 for y in range(16) for x in range(16) if po[x, y] == pr[x, y])
                    print(f"  [legacy  ] {name:42s} pixels == rock: {same:3d}/256 "
                          f"mineral_px={len(mask):3d}")

    print("\nRESULT:", "ALL COMPOSITE ORES OK" if failures == 0 else f"{failures} FAILURE(S)")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
