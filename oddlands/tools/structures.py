"""Builds structure template (.nbt) files for the Oddlands."""
import math
import os
import random

import nbtlib
from nbtlib.tag import Compound, Int, List, String

DATA_VERSION = 5023  # Minecraft 26.3


class Template:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}

    def set(self, x, y, z, block, props=None, nbt=None):
        if not (0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]):
            return
        self.blocks[(x, y, z)] = (block, tuple(sorted((props or {}).items())), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, block, props=None, only_empty=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if only_empty and (x, y, z) in self.blocks:
                        continue
                    self.set(x, y, z, block, props)

    def chest(self, x, y, z, loot, facing="north"):
        self.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"},
                 {"id": "minecraft:chest", "LootTable": loot})

    def save(self, path):
        palette, index = [], {}
        blocks = []
        for pos in sorted(self.blocks, key=lambda p: (p[1], p[2], p[0])):
            block, props, nbt = self.blocks[pos]
            key = (block, props)
            if key not in index:
                index[key] = len(palette)
                entry = {"id": String(block)}
                if props:
                    entry["properties"] = Compound({k: String(v) for k, v in props})
                palette.append(Compound(entry))
            b = {"pos": List[Int]([Int(c) for c in pos]), "state": Int(index[key])}
            if nbt:
                b["nbt"] = Compound({k: String(v) for k, v in nbt.items()})
            blocks.append(Compound(b))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(c) for c in self.size]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound]([]),
        })
        os.makedirs(os.path.dirname(path), exist_ok=True)
        nbtlib.File(root, gzipped=True).save(path)


SLAB = {"type": "bottom", "waterlogged": "false"}
FENCE = {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"}


def well(rim, foundation, post, roof, water="oddlands:wishing_water", missing_post=None, rubble=None):
    """5x5x5 well. Foundation at y0 replaces the ground; water at (2,1,2)."""
    t = Template(5, 5, 5)
    t.fill(0, 1, 0, 4, 4, 4, "minecraft:air")
    t.fill(0, 0, 0, 4, 0, 4, foundation)
    for x in range(1, 4):
        for z in range(1, 4):
            t.set(x, 1, z, rim)
    t.set(2, 1, 2, water, {"level": "0"} if water == "minecraft:water" else None)
    corners = [(1, 1), (3, 1), (1, 3), (3, 3)]
    for n, (x, z) in enumerate(corners):
        if n == missing_post:
            continue
        for y in (2, 3):
            t.set(x, y, z, post, FENCE)
    for x in range(1, 4):
        for z in range(1, 4):
            t.set(x, 4, z, roof, SLAB)
    if rubble:
        for (x, y, z, b) in rubble:
            t.set(x, y, z, b)
    return t


def island_well():
    """A floating 9x7x9 islet carrying an Oddlands well. Water at (4,3,4)."""
    t = Template(9, 7, 9)
    r = random.Random(7)
    for y, radius in ((0, 1.6), (1, 3.0), (2, 4.4)):
        for x in range(9):
            for z in range(9):
                d = math.hypot(x - 4, z - 4) + r.random() * 0.6
                if d <= radius:
                    block = "oddlands:squishmoss" if y == 2 else ("oddlands:mulch" if y == 1 else "oddlands:dripcrust")
                    t.set(x, y, z, block)
    t.set(4, 0, 4, "oddlands:dripcrust")
    for (x, z) in ((3, 4), (5, 5), (4, 3)):
        pass
    w = well("oddlands:knobstone_bricks", "oddlands:knobstone_bricks", "oddlands:bulbwood_fence", "oddlands:knobstone_brick_slab")
    for (x, y, z), value in w.blocks.items():
        t.blocks[(x + 2, y + 2, z + 2)] = value
    return t


def peeper_shrine():
    t = Template(7, 6, 7)
    t.fill(0, 1, 0, 6, 5, 6, "minecraft:air")
    t.fill(0, 0, 0, 6, 0, 6, "oddlands:polished_knobstone")
    t.fill(1, 0, 1, 5, 0, 5, "oddlands:knobstone_bricks")
    for (x, z) in ((0, 0), (6, 0), (0, 6), (6, 6)):
        for y in (1, 2, 3):
            t.set(x, y, z, "oddlands:knobstone_brick_wall", {"up": "true", "north": "none", "east": "none", "south": "none", "west": "none", "waterlogged": "false"})
    t.fill(0, 4, 0, 6, 4, 6, "oddlands:knobstone_brick_slab", SLAB)
    t.fill(2, 5, 2, 4, 5, 4, "oddlands:knobstone_brick_slab", SLAB)
    t.set(3, 5, 3, "oddlands:peeper_lantern")
    t.set(3, 1, 3, "oddlands:chiseled_knobstone")
    t.set(3, 2, 3, "oddlands:peeper_lantern")
    t.chest(1, 1, 3, "oddlands:chests/peeper_shrine", "east")
    t.set(5, 1, 1, "oddlands:knobstone_brick_stairs", {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    t.set(5, 1, 5, "oddlands:knobstone_brick_stairs", {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    return t


def rib_arch():
    t = Template(11, 9, 5)
    r = random.Random(3)
    t.fill(0, 0, 0, 10, 0, 4, "oddlands:bonestone")
    t.fill(0, 1, 0, 10, 8, 4, "minecraft:air")
    for x in (1, 9):
        for y in range(1, 7):
            t.set(x, y, 2, "oddlands:ribstone", {"axis": "y"})
    for x in range(1, 10):
        y = 8 - int(round(((x - 5) ** 2) / 8))
        t.set(x, y, 2, "oddlands:ribstone", {"axis": "x"})
        t.set(x, y - 1, 2, "oddlands:ribstone", {"axis": "x"}) if x in (2, 8) else None
    for x in (3, 5, 7):
        for z in (1, 3):
            if r.random() < 0.7:
                t.set(x, 1, z, "oddlands:tooth_crystal", {"facing": "up"})
    for (x, z) in ((2, 0), (8, 4), (6, 0), (4, 4)):
        t.set(x, 1, z, "oddlands:chalk_bricks")
    t.chest(5, 1, 2, "oddlands:chests/rib_arch", "south")
    return t


def lookout_tower():
    t = Template(7, 15, 7)
    t.fill(0, 0, 0, 6, 14, 6, "minecraft:air")
    t.fill(1, 0, 1, 5, 0, 5, "oddlands:rustrock_tiles")
    for y in range(1, 11):
        for x in range(1, 6):
            for z in range(1, 6):
                if x in (1, 5) or z in (1, 5):
                    t.set(x, y, z, "oddlands:banded_rustrock" if y % 4 == 0 else "oddlands:rustrock_tiles")
    t.set(3, 1, 5, "minecraft:air")
    t.set(3, 2, 5, "minecraft:air")
    for y in range(1, 12):
        t.set(3, y, 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    t.fill(0, 11, 0, 6, 11, 6, "oddlands:rustrock_tile_slab", SLAB)
    t.set(3, 11, 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    for x in range(7):
        for z in range(7):
            if x in (0, 6) or z in (0, 6):
                t.set(x, 12, z, "oddlands:bulbwood_fence", FENCE)
    for (x, z) in ((0, 0), (6, 0), (0, 6), (6, 6)):
        t.set(x, 13, z, "oddlands:peeper_lantern")
    t.chest(5, 12, 5, "oddlands:chests/lookout_tower", "north")
    for (x, y) in ((2, 6), (4, 8)):
        t.set(x, y, 1, "minecraft:air")
    return t


def stranded_well():
    # Deliberately one post short of a working wishing well: fix it, then toss in a copper nugget.
    t = well("oddlands:knobstone", "oddlands:knobstone_bricks", "oddlands:bulbwood_fence", "oddlands:knobstone_brick_slab",
             water="minecraft:water", missing_post=2)
    t.set(1, 4, 3, "minecraft:air")
    t.chest(4, 1, 2, "oddlands:chests/stranded_well", "west")
    return t


def build(out_dir):
    templates = {
        "well/overworld": well("minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:oak_fence", "minecraft:cobblestone_slab"),
        "well/oddlands": well("oddlands:knobstone_bricks", "oddlands:knobstone_bricks", "oddlands:bulbwood_fence", "oddlands:knobstone_brick_slab"),
        "well/island": island_well(),
        "peeper_shrine": peeper_shrine(),
        "rib_arch": rib_arch(),
        "lookout_tower": lookout_tower(),
        "stranded_well": stranded_well(),
    }
    for name, t in templates.items():
        t.save(os.path.join(out_dir, name + ".nbt"))
    return templates
