#!/usr/bin/env python3
"""Generate + inspect the per-village ``start.nbt`` anchor structures.

The mod overrides the vanilla village structure definitions so that their
``start_pool`` points at ``weather_realm:village/<type>/start``.  Each of those
template pools references an anchor ``start.nbt``.  The anchor contains two
jigsaw blocks:

* one whose ``pool`` reconnects the generated village to
  ``minecraft:village/<type>/town_centers`` (the vanilla village);
* one with ``target=minecraft:building_entrance`` and
  ``pool=weather_realm:village/altar_pool`` which injects the weather altar.

This script derives the desert / savanna / taiga anchors from the existing
plains anchor by rewriting *only* the reconnect jigsaw's ``pool`` string.  It
uses a self-contained, dependency-free NBT reader/writer so the length prefixes
stay correct even though ``savanna`` (7) / ``taiga`` (5) differ in length from
``plains`` (6).

Usage:

    python tools/make_village_start_nbt.py            # generate anchors
    python tools/make_village_start_nbt.py --inspect <nbt>...   # print jigsaws
    python tools/make_village_start_nbt.py --diff <a> <b>       # compare anchors

Generation is deterministic and idempotent: gzip is written with ``mtime=0``
so running it twice yields byte-identical files.
"""
from __future__ import annotations

import argparse
import gzip
import io
import struct
import sys
from pathlib import Path

# --- NBT tag ids ------------------------------------------------------------
TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12

TAG_NAMES = {
    TAG_END: "TAG_End",
    TAG_BYTE: "TAG_Byte",
    TAG_SHORT: "TAG_Short",
    TAG_INT: "TAG_Int",
    TAG_LONG: "TAG_Long",
    TAG_FLOAT: "TAG_Float",
    TAG_DOUBLE: "TAG_Double",
    TAG_BYTE_ARRAY: "TAG_Byte_Array",
    TAG_STRING: "TAG_String",
    TAG_LIST: "TAG_List",
    TAG_COMPOUND: "TAG_Compound",
    TAG_INT_ARRAY: "TAG_Int_Array",
    TAG_LONG_ARRAY: "TAG_Long_Array",
}


class NbtError(Exception):
    pass


# --- reader -----------------------------------------------------------------
# A tag is represented as ``(type_id, payload)``.  Compound payloads are
# insertion-ordered ``dict`` name -> tag; list payloads are
# ``(element_type, [payload, ...])`` so the writer can reproduce the byte stream
# exactly.
def _read_exact(buf: io.BytesIO, n: int) -> bytes:
    data = buf.read(n)
    if len(data) != n:
        raise NbtError(f"unexpected end of NBT (wanted {n} bytes, got {len(data)})")
    return data


def _read_payload(buf: io.BytesIO, tag_id: int):
    if tag_id == TAG_BYTE:
        return struct.unpack(">b", _read_exact(buf, 1))[0]
    if tag_id == TAG_SHORT:
        return struct.unpack(">h", _read_exact(buf, 2))[0]
    if tag_id == TAG_INT:
        return struct.unpack(">i", _read_exact(buf, 4))[0]
    if tag_id == TAG_LONG:
        return struct.unpack(">q", _read_exact(buf, 8))[0]
    if tag_id == TAG_FLOAT:
        return struct.unpack(">f", _read_exact(buf, 4))[0]
    if tag_id == TAG_DOUBLE:
        return struct.unpack(">d", _read_exact(buf, 8))[0]
    if tag_id == TAG_BYTE_ARRAY:
        n = struct.unpack(">i", _read_exact(buf, 4))[0]
        if n < 0:
            raise NbtError("negative byte-array length")
        return _read_exact(buf, n)
    if tag_id == TAG_STRING:
        n = struct.unpack(">H", _read_exact(buf, 2))[0]
        return _read_exact(buf, n).decode("utf-8")
    if tag_id == TAG_LIST:
        elem_type = struct.unpack(">b", _read_exact(buf, 1))[0]
        n = struct.unpack(">i", _read_exact(buf, 4))[0]
        if n < 0:
            raise NbtError("negative list length")
        items = [_read_payload(buf, elem_type) for _ in range(n)]
        return (elem_type, items)
    if tag_id == TAG_COMPOUND:
        out: dict[str, tuple] = {}
        while True:
            child_type = struct.unpack(">b", _read_exact(buf, 1))[0]
            if child_type == TAG_END:
                break
            name = _read_payload(buf, TAG_STRING)
            out[name] = (child_type, _read_payload(buf, child_type))
        return out
    if tag_id == TAG_INT_ARRAY:
        n = struct.unpack(">i", _read_exact(buf, 4))[0]
        if n < 0:
            raise NbtError("negative int-array length")
        return [struct.unpack(">i", _read_exact(buf, 4))[0] for _ in range(n)]
    if tag_id == TAG_LONG_ARRAY:
        n = struct.unpack(">i", _read_exact(buf, 4))[0]
        if n < 0:
            raise NbtError("negative long-array length")
        return [struct.unpack(">q", _read_exact(buf, 8))[0] for _ in range(n)]
    raise NbtError(f"unknown NBT tag id {tag_id}")


def read_nbt_bytes(raw: bytes):
    """Parse gzip-framed NBT bytes -> (root_name, root_type, root_payload)."""
    with gzip.GzipFile(fileobj=io.BytesIO(raw), mode="rb") as gz:
        data = gz.read()
    buf = io.BytesIO(data)
    root_type = struct.unpack(">b", _read_exact(buf, 1))[0]
    if root_type != TAG_COMPOUND:
        raise NbtError(f"NBT root must be a compound, got {TAG_NAMES.get(root_type)}")
    root_name = _read_payload(buf, TAG_STRING)
    root_payload = _read_payload(buf, root_type)
    trailing = buf.read()
    if trailing:
        raise NbtError(f"trailing {len(trailing)} bytes after root compound")
    return root_name, root_type, root_payload


def read_nbt_file(path: Path):
    return read_nbt_bytes(Path(path).read_bytes())


# --- writer -----------------------------------------------------------------
def _write_payload(buf: io.BytesIO, tag_id: int, payload):
    if tag_id == TAG_BYTE:
        buf.write(struct.pack(">b", payload))
    elif tag_id == TAG_SHORT:
        buf.write(struct.pack(">h", payload))
    elif tag_id == TAG_INT:
        buf.write(struct.pack(">i", payload))
    elif tag_id == TAG_LONG:
        buf.write(struct.pack(">q", payload))
    elif tag_id == TAG_FLOAT:
        buf.write(struct.pack(">f", payload))
    elif tag_id == TAG_DOUBLE:
        buf.write(struct.pack(">d", payload))
    elif tag_id == TAG_BYTE_ARRAY:
        buf.write(struct.pack(">i", len(payload)))
        buf.write(bytes(payload))
    elif tag_id == TAG_STRING:
        raw = payload.encode("utf-8")
        buf.write(struct.pack(">H", len(raw)))
        buf.write(raw)
    elif tag_id == TAG_LIST:
        elem_type, items = payload
        buf.write(struct.pack(">b", elem_type))
        buf.write(struct.pack(">i", len(items)))
        for item in items:
            _write_payload(buf, elem_type, item)
    elif tag_id == TAG_COMPOUND:
        for name, (child_type, child_payload) in payload.items():
            buf.write(struct.pack(">b", child_type))
            _write_payload(buf, TAG_STRING, name)
            _write_payload(buf, child_type, child_payload)
        buf.write(struct.pack(">b", TAG_END))
    elif tag_id == TAG_INT_ARRAY:
        buf.write(struct.pack(">i", len(payload)))
        for v in payload:
            buf.write(struct.pack(">i", v))
    elif tag_id == TAG_LONG_ARRAY:
        buf.write(struct.pack(">i", len(payload)))
        for v in payload:
            buf.write(struct.pack(">q", v))
    else:
        raise NbtError(f"cannot write unknown tag id {tag_id}")


def write_nbt_bytes(root_name: str, root_payload) -> bytes:
    """Serialise a root compound to deterministic gzip (mtime=0)."""
    raw = io.BytesIO()
    raw.write(struct.pack(">b", TAG_COMPOUND))
    _write_payload(raw, TAG_STRING, root_name)
    _write_payload(raw, TAG_COMPOUND, root_payload)
    out = io.BytesIO()
    with gzip.GzipFile(fileobj=out, mode="wb", mtime=0) as gz:
        gz.write(raw.getvalue())
    return out.getvalue()


def write_nbt_file(path: Path, root_name: str, root_payload) -> None:
    Path(path).write_bytes(write_nbt_bytes(root_name, root_payload))


# --- small accessors --------------------------------------------------------
def as_str(tag) -> str:
    if tag[0] != TAG_STRING:
        raise NbtError(f"expected a string, got {TAG_NAMES.get(tag[0])}")
    return tag[1]


def as_compound(tag) -> dict:
    if tag[0] != TAG_COMPOUND:
        raise NbtError(f"expected a compound, got {TAG_NAMES.get(tag[0])}")
    return tag[1]


def as_list_items(tag) -> list:
    if tag[0] != TAG_LIST:
        raise NbtError(f"expected a list, got {TAG_NAMES.get(tag[0])}")
    return tag[1][1]


def find_jigsaws(root_payload: dict) -> list[dict]:
    """Return the ``nbt`` compounds of every jigsaw block in a structure."""
    blocks_tag = root_payload.get("blocks")
    if blocks_tag is None or blocks_tag[0] != TAG_LIST:
        raise NbtError("structure has no 'blocks' list")
    jigsaws = []
    for comp in as_list_items(blocks_tag):
        # list items are raw payloads: a compound payload is already a dict
        if not isinstance(comp, dict):
            continue
        nbt_tag = comp.get("nbt")
        if nbt_tag is None or nbt_tag[0] != TAG_COMPOUND:
            continue
        nbt = as_compound(nbt_tag)
        bid = nbt.get("id")
        if bid is not None and as_str(bid) == "minecraft:jigsaw":
            jigsaws.append(nbt)
    return jigsaws


def _get_str_or_none(comp: dict, key: str):
    tag = comp.get(key)
    return as_str(tag) if tag is not None else None


# --- repo conventions -------------------------------------------------------
MODID = "weather_realm"
REPO = Path(__file__).resolve().parents[1]
STRUCTURE_DIR = REPO / "src" / "main" / "resources" / "data" / MODID / "structure" / "village"
TEMPLATE_NBT = STRUCTURE_DIR / "plains" / "start.nbt"
ALTAR_POOL = "weather_realm:village/altar_pool"
# village type -> the vanilla town-centres pool the anchor must reconnect to
TOWN_CENTERS = {
    "plains": "minecraft:village/plains/town_centers",
    "snowy": "minecraft:village/snowy/town_centers",
    "desert": "minecraft:village/desert/town_centers",
    "savanna": "minecraft:village/savanna/town_centers",
    "taiga": "minecraft:village/taiga/town_centers",
}
GENERATE_TYPES = ["desert", "savanna", "taiga"]


def _describe_jigsaws(jigsaws: list[dict]) -> list[str]:
    lines = []
    for j in jigsaws:
        lines.append(
            "  jigsaw name={name!r} target={target!r} pool={pool!r}".format(
                name=_get_str_or_none(j, "name"),
                target=_get_str_or_none(j, "target"),
                pool=_get_str_or_none(j, "pool"),
            )
        )
    return lines


def derive_anchor(template_path: Path, village_type: str) -> dict:
    """Return a copy of the template root with the reconnect pool rewritten."""
    target_pool = TOWN_CENTERS[village_type]
    _root_name, _root_type, root = read_nbt_file(template_path)
    jigsaws = find_jigsaws(root)
    if len(jigsaws) != 2:
        raise NbtError(
            f"template {template_path} has {len(jigsaws)} jigsaws, expected 2"
        )
    reconnect = [
        j for j in jigsaws if (_get_str_or_none(j, "pool") or "").endswith("/town_centers")
        and (_get_str_or_none(j, "pool") or "").startswith("minecraft:village/")
    ]
    if len(reconnect) != 1:
        raise NbtError(
            f"template {template_path}: expected exactly 1 reconnect jigsaw, "
            f"found {len(reconnect)}"
        )
    altar = [j for j in jigsaws if _get_str_or_none(j, "pool") == ALTAR_POOL]
    if len(altar) != 1:
        raise NbtError(
            f"template {template_path}: expected exactly 1 altar jigsaw, found {len(altar)}"
        )
    pool_tag = reconnect[0].get("pool")
    reconnect[0]["pool"] = (pool_tag[0], target_pool)
    return root


def cmd_generate() -> int:
    if not TEMPLATE_NBT.is_file():
        sys.exit(f"template anchor not found: {TEMPLATE_NBT}")
    for village_type in GENERATE_TYPES:
        root = derive_anchor(TEMPLATE_NBT, village_type)
        out = STRUCTURE_DIR / village_type / "start.nbt"
        out.parent.mkdir(parents=True, exist_ok=True)
        write_nbt_file(out, "", root)
        print(f"[gen] wrote {out.relative_to(REPO)}")
    return 0


def cmd_inspect(paths: list[str]) -> int:
    for raw in paths:
        path = Path(raw)
        root_name, _root_type, root = read_nbt_file(path)
        jigsaws = find_jigsaws(root)
        print(f"{path} (root name={root_name!r}, {len(jigsaws)} jigsaw(s)):")
        for line in _describe_jigsaws(jigsaws):
            print(line)
    return 0


def cmd_diff(a: str, b: str) -> int:
    ra = read_nbt_file(Path(a))
    rb = read_nbt_file(Path(b))
    if ra == rb:
        print(f"{a} == {b} (semantically identical)")
        return 0
    print(f"{a} != {b}")
    ja, jb = find_jigsaws(ra[2]), find_jigsaws(rb[2])
    print("  A jigsaws:")
    print("\n".join(_describe_jigsaws(ja)))
    print("  B jigsaws:")
    print("\n".join(_describe_jigsaws(jb)))
    return 1


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_mutually_exclusive_group()
    sub.add_argument("--inspect", nargs="+", metavar="NBT", help="print jigsaw fields")
    sub.add_argument("--diff", nargs=2, metavar=("A", "B"), help="compare two anchors")
    args = parser.parse_args(argv)
    if args.inspect:
        return cmd_inspect(args.inspect)
    if args.diff:
        return cmd_diff(*args.diff)
    return cmd_generate()


if __name__ == "__main__":
    raise SystemExit(main())
