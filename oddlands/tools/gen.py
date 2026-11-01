"""Generates the Oddlands data pack and resource pack.

Usage: python3 gen.py <vanilla-assets-dir> <out-dir>

<vanilla-assets-dir> is a 26.3 `assets/minecraft` folder with JSON (blockstates, models, items), e.g. from
https://github.com/misode/mcmeta (tag 26.3-assets-json). It is only used to copy vanilla blockstates/models
for slabs, stairs, doors and other vanilla-shaped blocks. Needs: pip install pillow nbtlib
"""
import copy
import json
import os
import shutil
import sys

import blocks as B
import structures
import textures

NS = B.NS
VANILLA = sys.argv[1]  # .../assets/minecraft
OUT = sys.argv[2]
DP = os.path.join(OUT, "oddlands_datapack")
RP = os.path.join(OUT, "oddlands_resourcepack")


def i(name):
    return f"{NS}:{name}"


def write(root, path, obj):
    p = os.path.join(root, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        if isinstance(obj, str):
            f.write(obj)
        else:
            json.dump(obj, f, indent=2)
            f.write("\n")


def dp(path, obj):
    write(DP, path, obj)


def rp(path, obj):
    write(RP, path, obj)


for d in (DP, RP):
    shutil.rmtree(d, ignore_errors=True)

dp("pack.mcmeta", {"pack": {"description": "The Oddlands: a floating-island dimension reached through wishing wells (needs Data Blocks)",
                            "min_format": [121, 0], "max_format": [999, 0]}})
rp("pack.mcmeta", {"pack": {"description": "The Oddlands: textures and models", "min_format": [97, 0], "max_format": [999, 0]}})

# ============================================================== block definitions + assets

textures.build(os.path.join(RP, "assets", NS, "textures", "block"))
os.makedirs(os.path.join(RP, "assets", NS, "textures", "item"), exist_ok=True)
shutil.move(os.path.join(RP, "assets", NS, "textures", "block", "item", "bulbwood_door.png"),
            os.path.join(RP, "assets", NS, "textures", "item", "bulbwood_door.png"))
os.rmdir(os.path.join(RP, "assets", NS, "textures", "block", "item"))
rp(f"assets/{NS}/textures/block/wishing_water.png.mcmeta", {"animation": {"frametime": 3, "interpolate": True}})

lang = {
    "itemGroup.datablocks.blocks": "Data Blocks",
    "advancements.oddlands.wishing_well.title": "Toss a Coin",
    "advancements.oddlands.wishing_well.description": "Throw a copper nugget into a finished wishing well",
    "advancements.oddlands.through_the_well.title": "Down the Well",
    "advancements.oddlands.through_the_well.description": "Fall through a wishing well into the Oddlands",
    "advancements.oddlands.peeper.title": "It Blinked First",
    "advancements.oddlands.peeper.description": "Pick a blinkbloom",
    "biome.oddlands.jelly_oasis": "Jellywood Oasis",
    "biome.oddlands.peeper_meadow": "Peeper Meadow",
    "biome.oddlands.bone_flats": "Bone Flats",
    "biome.oddlands.rust_mesas": "Rust Mesas",
    "biome.oddlands.the_drift": "The Drift",
}


def vanilla_json(kind, name):
    with open(os.path.join(VANILLA, kind, name + ".json")) as f:
        return json.load(f)


def retexture(obj, tex_map):
    """Deep-replace minecraft:block/<old> texture references with oddlands:block/<new>."""
    s = json.dumps(obj)
    for old, new in sorted(tex_map.items(), key=lambda kv: -len(kv[0])):
        s = s.replace(f'"minecraft:block/{old}"', f'"{NS}:block/{new}"')
    return json.loads(s)


def clone_vanilla(name, template, tex_map):
    """Copy a vanilla block's blockstate, block models and item definition, renaming and retexturing."""
    bs = vanilla_json("blockstates", template)
    models = set()

    def collect(o):
        if isinstance(o, dict):
            if isinstance(o.get("model"), str):
                models.add(o["model"])
            for v in o.values():
                collect(v)
        elif isinstance(o, list):
            for v in o:
                collect(v)

    collect(bs)
    item = vanilla_json("items", template)
    collect(item)
    rename = {}
    for m in models:
        _, path = m.split(":")
        folder, base = path.split("/", 1)
        if template not in base and base in tex_map:
            # e.g. a double slab uses the full block's own model
            rename[m] = f"{NS}:{folder}/{tex_map[base]}"
            continue
        new_base = base.replace(template, name)
        rename[m] = f"{NS}:{folder}/{new_base}"
        src = vanilla_json("models", path)
        rp(f"assets/{NS}/models/{folder}/{new_base}.json", retexture(src, tex_map))
    s = json.dumps(bs)
    si = json.dumps(item)
    for old, new in rename.items():
        s = s.replace(f'"{old}"', f'"{new}"')
        si = si.replace(f'"{old}"', f'"{new}"')
    s = s.replace('"minecraft:item/', f'"{NS}:item/')
    si = si.replace('"minecraft:item/', f'"{NS}:item/')
    rp(f"assets/{NS}/blockstates/{name}.json", json.loads(s))
    rp(f"assets/{NS}/items/{name}.json", json.loads(si))
    # flat item models (doors) reference oddlands:item/<name>
    for m in models:
        if m.startswith("minecraft:item/"):
            rp(f"assets/{NS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})


def simple_assets(name, asset):
    kind = asset[0]
    model = f"{NS}:block/{name}"
    item_model = {"type": "minecraft:model", "model": model}
    if kind == "cube":
        rp(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{asset[1]}"}})
        rp(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": model}}})
    elif kind == "bottom_top":
        rp(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"{NS}:block/{asset[1]}", "side": f"{NS}:block/{asset[2]}", "bottom": f"{NS}:block/{asset[3]}"}})
        rp(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": [{"model": model}, {"model": model, "y": 90},
                                                                      {"model": model, "y": 180}, {"model": model, "y": 270}]}})
    elif kind == "column":
        tex = {"side": f"{NS}:block/{asset[1]}", "end": f"{NS}:block/{asset[2]}"}
        rp(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_column", "textures": tex})
        rp(f"assets/{NS}/models/block/{name}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": tex})
        rp(f"assets/{NS}/blockstates/{name}.json", {"variants": {
            "axis=y": {"model": model},
            "axis=z": {"model": model + "_horizontal", "x": 90},
            "axis=x": {"model": model + "_horizontal", "x": 90, "y": 90}}})
    elif kind in ("cross", "facing_cross"):
        rp(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"{NS}:block/{asset[1]}"}})
        rp(f"assets/{NS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/{asset[1]}"}})
        item_model = {"type": "minecraft:model", "model": f"{NS}:item/{name}"}
        if kind == "cross":
            rp(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": model}}})
        else:
            # Same rotations as vanilla amethyst clusters.
            rp(f"assets/{NS}/blockstates/{name}.json", {"variants": {
                "facing=up": {"model": model}, "facing=down": {"model": model, "x": 180},
                "facing=north": {"model": model, "x": 90}, "facing=south": {"model": model, "x": 90, "y": 180},
                "facing=east": {"model": model, "x": 90, "y": 90}, "facing=west": {"model": model, "x": 90, "y": 270}}})
    elif kind == "water":
        tex = f"{NS}:block/{asset[1]}"
        rp(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {"particle": tex, "all": tex},
                                                      "elements": [{"from": [0, 0, 0], "to": [16, 13, 16], "faces": {
                                                          f: {"texture": "#all", **({"cullface": f} if f != "up" else {})}
                                                          for f in ("down", "up", "north", "south", "east", "west")}}]})
        rp(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": model}}})
    rp(f"assets/{NS}/items/{name}.json", {"model": item_model})


for name, spec in B.BLOCKS.items():
    dp(f"data/{NS}/block_definition/{name}.json", {"components": spec["components"]})
    lang[f"block.{NS}.{name}"] = spec["lang"]
    asset = spec["asset"]
    if asset[0] == "vanilla":
        clone_vanilla(name, asset[1], asset[2])
    else:
        simple_assets(name, asset)

rp(f"assets/{NS}/lang/en_us.json", lang)

# Resource-pack half of a definition: client-only components.
rp(f"assets/{NS}/block_definition/glowgulp.json", {"components": {"ambient_particles": [
    {"particle": "minecraft:glow", "chance": 0.2, "offset": [0, 0.1, 0], "spread": [0.3, 0.3, 0.3], "velocity": [0, 0.01, 0]},
    {"particle": "minecraft:end_rod", "chance": 0.02, "offset": [0, 0.5, 0], "spread": [0.2, 0.1, 0.2], "velocity": [0, 0.02, 0]}]}})

# ============================================================== tags


def tag(path, values, namespace="minecraft"):
    vals = [v if v.startswith("#") else {"id": v, "required": False} for v in values]
    dp(f"data/{namespace}/tags/{path}.json", {"values": vals})


def own(*names):
    return [i(n) for n in names]


tag("block/lush_soils", own("squishmoss", "mulch") + ["#minecraft:dirt", "minecraft:moss_block"], NS)
tag("block/dry_soils", own("chalkdust", "bonestone", "rustrock", "banded_rustrock") + ["#minecraft:sand", "#minecraft:terracotta"], NS)
tag("block/dangle_anchor", own("dripcrust", "knobstone", "mulch", "bonestone", "rustrock", "banded_rustrock", "squishmoss",
                               "dangleroot", "bulbwood_log", "jellyleaf") + ["#minecraft:base_stone_overworld", "#minecraft:dirt"], NS)
tag("block/stone_ore_replaceable", own("knobstone", "dripcrust"), NS)
tag("block/well_rim", ["minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                       "minecraft:cobbled_deepslate", "minecraft:deepslate_bricks"] + own("knobstone", "knobstone_bricks", "polished_knobstone"), NS)
tag("block/well_posts", ["#minecraft:fences", "#minecraft:walls"], NS)
tag("block/well_roof", ["#minecraft:slabs"], NS)
# What the arrival scan treats as "not ground" while looking down a column.
tag("block/scan_passable", ["minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:light", "#minecraft:leaves",
                            "#minecraft:flowers", "#minecraft:saplings", "minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern",
                            "minecraft:large_fern", "minecraft:snow", "minecraft:vine", "minecraft:dead_bush", "minecraft:bush",
                            "#minecraft:logs"] + own("jellyleaf", "bulbwood_log", "peeper_grass", "ribgrass", "sulkweed", "husk_shrub",
                                                     "blinkbloom", "gawkcap", "glowgulp", "dangleroot", "dustpuff",
                                                     "gawkcap_cap", "gawkcap_stem", "wobble_sprout"), NS)
tag("block/mineable/pickaxe", own("knobstone", "polished_knobstone", "knobstone_bricks", "knobstone_brick_slab", "knobstone_brick_stairs",
                                  "knobstone_brick_wall", "chiseled_knobstone", "copperknob_ore", "dripcrust", "bonestone", "chalk_bricks",
                                  "chalk_brick_slab", "chalk_brick_stairs", "rustrock", "banded_rustrock", "rustrock_tiles",
                                  "rustrock_tile_slab", "rustrock_tile_stairs", "ribstone", "tooth_crystal", "peeper_lantern", "wishing_water"))
tag("block/mineable/shovel", own("squishmoss", "mulch", "chalkdust"))
tag("block/mineable/axe", own("bulbwood_log", "bulbwood_planks", "bulbwood_slab", "bulbwood_stairs", "bulbwood_fence", "bulbwood_fence_gate",
                              "bulbwood_door", "bulbwood_trapdoor", "bulbwood_button", "bulbwood_pressure_plate", "gawkcap_stem", "gawkcap_cap"))
tag("block/mineable/hoe", own("jellyleaf", "squish_cushion", "sap_glob", "squishmoss"))
tag("block/needs_stone_tool", own("copperknob_ore", "tooth_crystal"))
tag("block/dirt", own("squishmoss", "mulch"))
tag("block/supports_vegetation", own("squishmoss", "mulch"))
tag("block/animals_spawnable_on", own("squishmoss", "chalkdust"))
tag("block/frogs_spawnable_on", own("squishmoss", "mulch"))
tag("block/rabbits_spawnable_on", own("chalkdust", "rustrock"))
tag("block/climbable", own("dangleroot"))
tag("block/leaves", own("jellyleaf"))
tag("block/replaceable_by_trees", own("peeper_grass", "ribgrass", "sulkweed", "jellyleaf"))
tag("block/base_stone_overworld", own("knobstone", "rustrock", "bonestone"))
for t, names in (("logs", ["bulbwood_log"]), ("logs_that_burn", ["bulbwood_log"]), ("planks", ["bulbwood_planks"]),
                 ("wooden_slabs", ["bulbwood_slab"]), ("wooden_stairs", ["bulbwood_stairs"]), ("wooden_fences", ["bulbwood_fence"]),
                 ("fence_gates", ["bulbwood_fence_gate"]), ("wooden_doors", ["bulbwood_door"]), ("wooden_trapdoors", ["bulbwood_trapdoor"]),
                 ("wooden_buttons", ["bulbwood_button"]), ("wooden_pressure_plates", ["bulbwood_pressure_plate"]),
                 ("slabs", ["knobstone_brick_slab", "chalk_brick_slab", "rustrock_tile_slab"]),
                 ("stairs", ["knobstone_brick_stairs", "chalk_brick_stairs", "rustrock_tile_stairs"]),
                 ("walls", ["knobstone_brick_wall"]), ("leaves", ["jellyleaf"])):
    tag("block/" + t, own(*names))
    tag("item/" + t, own(*names))
tag("item/stone_crafting_materials", own("knobstone"))
tag("item/stone_tool_materials", own("knobstone"))

# ============================================================== recipes


def shaped(name, pattern, key, result, count=1, category="building"):
    dp(f"data/{NS}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "key": key,
                                          "pattern": pattern, "result": {"count": count, "id": result}})


def shapeless(name, ingredients, result, count=1, category="building"):
    dp(f"data/{NS}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
                                          "ingredients": ingredients, "result": {"count": count, "id": result}})


def cook(name, ingredient, result, kind="smelting", xp=0.1, time=200):
    dp(f"data/{NS}/recipe/{name}.json", {"type": f"minecraft:{kind}", "category": "blocks", "cookingtime": time,
                                          "experience": xp, "ingredient": ingredient, "result": {"id": result}})


def cut(name, ingredient, result, count=1):
    dp(f"data/{NS}/recipe/{name}.json", {"type": "minecraft:stonecutting", "ingredient": ingredient, "result": {"count": count, "id": result}})


def stone_family(base, bricks, slab, stairs, wall=None):
    shaped(bricks, ["##", "##"], {"#": i(base)}, i(bricks), 4)
    shaped(slab, ["###"], {"#": i(bricks)}, i(slab), 6)
    shaped(stairs, ["#  ", "## ", "###"], {"#": i(bricks)}, i(stairs), 4)
    cut(f"{bricks}_from_{base}_stonecutting", i(base), i(bricks))
    cut(f"{slab}_from_{bricks}_stonecutting", i(bricks), i(slab), 2)
    cut(f"{stairs}_from_{bricks}_stonecutting", i(bricks), i(stairs))
    if wall:
        shaped(wall, ["###", "###"], {"#": i(bricks)}, i(wall), 6)
        cut(f"{wall}_from_{bricks}_stonecutting", i(bricks), i(wall))


stone_family("knobstone", "knobstone_bricks", "knobstone_brick_slab", "knobstone_brick_stairs", "knobstone_brick_wall")
stone_family("bonestone", "chalk_bricks", "chalk_brick_slab", "chalk_brick_stairs")
stone_family("rustrock", "rustrock_tiles", "rustrock_tile_slab", "rustrock_tile_stairs")
cook("polished_knobstone", i("knobstone"), i("polished_knobstone"))
shaped("chiseled_knobstone", ["#", "#"], {"#": i("knobstone_brick_slab")}, i("chiseled_knobstone"))
cook("copper_ingot_from_copperknob_ore", i("copperknob_ore"), "minecraft:copper_ingot", xp=0.7)
cook("copper_ingot_from_blasting_copperknob_ore", i("copperknob_ore"), "minecraft:copper_ingot", "blasting", 0.7, 100)
cook("bonestone_from_chalkdust", i("chalkdust"), i("bonestone"))
shapeless("banded_rustrock", [i("rustrock"), i("bonestone")], i("banded_rustrock"), 2)
shapeless("bulbwood_planks", [i("bulbwood_log")], i("bulbwood_planks"), 4)
P = i("bulbwood_planks")
shaped("bulbwood_slab", ["###"], {"#": P}, i("bulbwood_slab"), 6)
shaped("bulbwood_stairs", ["#  ", "## ", "###"], {"#": P}, i("bulbwood_stairs"), 4)
shaped("bulbwood_fence", ["W#W", "W#W"], {"#": "minecraft:stick", "W": P}, i("bulbwood_fence"), 3, "misc")
shaped("bulbwood_fence_gate", ["#W#", "#W#"], {"#": "minecraft:stick", "W": P}, i("bulbwood_fence_gate"), 1, "redstone")
shaped("bulbwood_door", ["##", "##", "##"], {"#": P}, i("bulbwood_door"), 3, "redstone")
shaped("bulbwood_trapdoor", ["###", "###"], {"#": P}, i("bulbwood_trapdoor"), 2, "redstone")
shapeless("bulbwood_button", [P], i("bulbwood_button"), 1, "redstone")
shaped("bulbwood_pressure_plate", ["##"], {"#": P}, i("bulbwood_pressure_plate"), 1, "redstone")
shaped("squish_cushion", ["##", "##"], {"#": i("squishmoss")}, i("squish_cushion"))
shaped("peeper_lantern", [" K ", "KGK", " K "], {"K": i("knobstone"), "G": i("gawkcap")}, i("peeper_lantern"))
shapeless("wobble_sprout", [i("jellyleaf"), "minecraft:bone_meal"], i("wobble_sprout"), 1, "misc")
shapeless("mulch_from_squishmoss", [i("squishmoss")], i("mulch"))
shaped("sap_glob", ["###", "###", "###"], {"#": "minecraft:honey_bottle"}, i("sap_glob"))

# ============================================================== loot tables


def entry(item, weight=1, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": item, "weight": weight}
    if hi > 1:
        e["modifier"] = {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}
    return e


def chest(name, pools):
    dp(f"data/{NS}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries} for rolls, entries in pools]})


chest("peeper_shrine", [((2, 4), [entry("minecraft:copper_nugget", 10, 2, 6), entry(i("blinkbloom"), 6, 1, 3), entry(i("gawkcap"), 6, 1, 4),
                                  entry(i("glowgulp"), 4, 1, 2), entry("minecraft:emerald", 3, 1, 2), entry("minecraft:bread", 5, 1, 3)]),
                        ((1, 1), [entry(i("peeper_lantern"), 2), entry("minecraft:amethyst_shard", 3, 1, 4), entry("minecraft:spyglass", 1)])])
chest("rib_arch", [((3, 5), [entry("minecraft:bone", 10, 2, 8), entry(i("tooth_crystal"), 6, 1, 3), entry("minecraft:iron_ingot", 4, 1, 3),
                             entry("minecraft:gold_nugget", 5, 2, 7), entry(i("dustpuff"), 4, 1, 3), entry("minecraft:copper_nugget", 6, 1, 5)])])
chest("lookout_tower", [((3, 6), [entry("minecraft:arrow", 8, 4, 12), entry("minecraft:copper_ingot", 6, 1, 4), entry(i("sap_glob"), 3, 1, 2),
                                  entry("minecraft:compass", 2), entry("minecraft:spyglass", 2), entry(i("peeper_lantern"), 3, 1, 2),
                                  entry("minecraft:cooked_rabbit", 5, 1, 3)])])
chest("stranded_well", [((3, 5), [entry("minecraft:copper_nugget", 10, 3, 9), entry(i("wobble_sprout"), 4, 1, 2),
                                  entry("minecraft:bucket", 2), entry(i("knobstone_bricks"), 5, 2, 6), entry(i("bulbwood_fence"), 5, 1, 2)])])

# ============================================================== worldgen: noises & density


def noise_param(name, octave, mods, amp=0.9):
    dp(f"data/{NS}/worldgen/noise/{name}.json", {"amplitude_modifiers": mods, "base_amplitude": amp, "base_octave": octave,
                                                  "octave_count": len(mods)})


noise_param("continents", -9, [1.0, 1.0, 1.0, 0.5], 0.9381732587751005)
noise_param("height", -7, [1.0, 0.5, 0.25], 0.85)
noise_param("mesa", -8, [1.0, 0.5], 0.9)
noise_param("terrace", -6, [1.0, 0.5], 0.9)
noise_param("canyon", -8, [1.0, 0.6], 0.9)
noise_param("underside", -4, [1.0, 1.0], 0.9)
noise_param("rough", -5, [1.0, 1.0, 0.5], 0.85)
noise_param("islets", -6, [1.0, 0.6], 0.9)
noise_param("temperature", -9, [1.0, 1.0], 1.0)
noise_param("vegetation", -9, [1.0, 1.0, 0.5], 1.0)
noise_param("surface", -6, [1.0, 1.0, 1.0], 0.9)


def N(name, y=0.0):
    return {"type": "minecraft:noise", "noise": i(name), "xz_scale": 1.0, "y_scale": y}


def op(t, a, b):
    return {"type": f"minecraft:{t}", "left": a, "right": b}


def add(a, b): return op("add", a, b)
def sub(a, b): return op("sub", a, b)
def mul(a, b): return op("mul", a, b)
def div(a, b): return op("div", a, b)
def mn(a, b): return op("min", a, b)
def mx(a, b): return op("max", a, b)
def ab(a): return {"type": "minecraft:abs", "input": a}
def clamp(a, lo, hi): return {"type": "minecraft:clamp", "input": a, "min": lo, "max": hi}
def cache(a): return {"type": "minecraft:cache", "input": a}


Y = "minecraft:y"

# Shared, named density functions (so they can be reused and inspected with /debug).
dp(f"data/{NS}/worldgen/density_function/continents.json", cache(N("continents")))
CONT = i("continents")
# > 0 over continents, < 0 over the open void between them
dp(f"data/{NS}/worldgen/density_function/land.json", cache(clamp(mul(add(CONT, 0.05), 3.0), -1.0, 1.0)))
LAND = i("land")
# Rolling hills plus saturated "mesa" noise: the clamp flattens peaks into plateaus with steep sides.
hills = add(add(mul(30.0, clamp(mul(N("height"), 2.5), -1.0, 1.0)), mul(20.0, clamp(mul(N("mesa"), 3.0), -1.0, 1.0))),
            mul(9.0, clamp(mul(N("terrace"), 4.0), -1.0, 1.0)))
dp(f"data/{NS}/worldgen/density_function/surface_height.json", cache(add(150.0, hills)))
SURF = i("surface_height")
# Narrow ridged channels that slice straight down through the islands: canyons, some bottomless.
dp(f"data/{NS}/worldgen/density_function/canyon.json", cache(clamp(sub(1.0, div(ab(N("canyon")), 0.07)), 0.0, 1.0)))
CANYON = i("canyon")
top = sub(SURF, mul(58.0, CANYON))
thickness = add(10.0, mul(70.0, LAND))
bottom = sub(sub(SURF, thickness), mul(36.0, ab(N("underside"))))
rough = N("rough", 1.0)
top_term = add(div(sub(top, Y), 8.0), mul(0.35, rough))
bottom_term = add(div(sub(Y, bottom), 5.0), mul(1.2, rough))
islands = mn(top_term, bottom_term)
# Small floating islets scattered through the void, densest around y=150.
islets = sub(sub(N("islets", 1.0), 0.52), div(ab(sub(Y, 150.0)), 80.0))
shape = mx(islands, islets)
# Keep clear of the build limits.
shape = mn(shape, div(sub(Y, 10.0), 4.0))
shape = mn(shape, div(sub(300.0, Y), 6.0))
dp(f"data/{NS}/worldgen/density_function/base_shape.json", shape)
final = add({"type": "minecraft:squeeze", "input": {"type": "minecraft:interpolated", "cell_size_xz": 8, "cell_size_y": 4,
                                                   "input": mul(i("base_shape"), 0.64)}},
            {"type": "minecraft:beardifier"})

# ============================================================== material rules (surface)


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def blk(b):
    return {"type": "minecraft:block", "result_state": b}


def biome_is(b):
    return {"type": "minecraft:biome", "biome_is": i(b)}


def y_above(y):
    return {"type": "minecraft:y_above", "add_stone_depth": False, "anchor": {"absolute": y}, "surface_depth_multiplier": 0}


ON_FLOOR = {"type": "minecraft:stone_depth", "add_surface_depth": False, "offset": 0, "secondary_depth_range": 0, "surface_type": "floor"}
UNDER_FLOOR = {"type": "minecraft:stone_depth", "add_surface_depth": True, "offset": 0, "secondary_depth_range": 0, "surface_type": "floor"}
CEILING = {"type": "minecraft:stone_depth", "add_surface_depth": True, "offset": 0, "secondary_depth_range": 2, "surface_type": "ceiling"}

# Horizontal strata for the rust mesas: every cliff face reads like a layer cake.
bands = []
layer_blocks = [i("rustrock"), i("banded_rustrock"), i("rustrock"), i("bonestone"), i("rustrock"), i("banded_rustrock")]
heights = [5, 3, 4, 2, 6, 3]
y = 40
n = 0
while y < 300:
    h = heights[n % len(heights)]
    bands.append(cond(y_above(y), cond({"type": "minecraft:not", "invert": y_above(y + h)}, blk(layer_blocks[n % len(layer_blocks)]))))
    y += h
    n += 1

lush = {"type": "minecraft:sequence", "sequence": [cond(ON_FLOOR, blk(i("squishmoss"))), cond(UNDER_FLOOR, blk(i("mulch")))]}
material_rule = {"type": "minecraft:sequence", "sequence": [
    cond(CEILING, blk(i("dripcrust"))),
    cond(biome_is("rust_mesas"), {"type": "minecraft:sequence", "sequence": [cond(ON_FLOOR, blk(i("rustrock")))] + bands}),
    cond(biome_is("bone_flats"), {"type": "minecraft:sequence", "sequence": [
        cond(ON_FLOOR, {"type": "minecraft:sequence", "sequence": [
            cond({"type": "minecraft:noise_threshold", "noise": i("surface"), "min_threshold": 0.45, "max_threshold": 1e9}, blk(i("bonestone"))),
            blk(i("chalkdust"))]}),
        cond(UNDER_FLOOR, blk(i("bonestone")))]}),
    cond(biome_is("jelly_oasis"), lush),
    cond(biome_is("peeper_meadow"), lush),
    cond(biome_is("the_drift"), lush),
]}
dp(f"data/{NS}/worldgen/material_rule/oddlands.json", material_rule)

ZERO = 0.0
dp(f"data/{NS}/worldgen/noise_settings/oddlands.json", {
    "default_block": i("knobstone"),
    "default_fluid": "minecraft:water",
    "disable_mob_generation": False,
    "legacy_random_source": False,
    "material_rule": i("oddlands"),
    "noise": {"height": 320, "min_y": 0},
    "noise_router": {
        "chunk_surface_level": ZERO,
        "continents": CONT,
        "depth": ZERO,
        "erosion": ZERO,
        "final_density": final,
        "ridges": ZERO,
        "temperature": N("temperature"),
        "vegetation": N("vegetation"),
    },
    "sea_level": -64,
    "spawn_target": [],
})

# ============================================================== dimension

dp(f"data/{NS}/dimension_type/oddlands.json", {
    "ambient_light": 0.06,
    "attributes": {
        "minecraft:audio/ambient_sounds": {"mood": {"block_search_extent": 8, "offset": 2.0, "sound": "minecraft:ambient.cave", "tick_delay": 6000}},
        "minecraft:audio/background_music": {"default": {"max_delay": 24000, "min_delay": 9000, "sound": "minecraft:music.overworld.lush_caves"}},
        "minecraft:gameplay/bed_rule": {"can_set_spawn": "always", "can_sleep": "when_dark",
                                        "error_message": {"translate": "block.minecraft.bed.no_sleep"}},
        "minecraft:gameplay/respawn_anchor_works": False,
        "minecraft:visual/ambient_light_color": "#1a0f22",
        "minecraft:visual/cloud_color": "#ccffd6ec",
        "minecraft:visual/cloud_height": 250.0,
        "minecraft:visual/fog_color": "#e6c3dd",
        "minecraft:visual/sky_color": "#b897e0",
    },
    "coordinate_scale": 1.0,
    "default_clock": "minecraft:overworld",
    "has_ceiling": False,
    "has_ender_dragon_fight": False,
    "has_skylight": True,
    "height": 320,
    "infiniburn": "#minecraft:infiniburn_overworld",
    "logical_height": 320,
    "min_y": 0,
    "monster_spawn_block_light_limit": 0,
    "monster_spawn_light_level": {"type": "minecraft:uniform", "max_inclusive": 7, "min_inclusive": 0},
    "timelines": "#minecraft:in_overworld",
})


def params(temp, hum, cont):
    return {"temperature": temp, "humidity": hum, "continentalness": cont, "erosion": [-1.0, 1.0], "weirdness": [-1.0, 1.0],
            "depth": 0.0, "offset": 0.0}


LANDC = [-0.06, 1.0]
dp(f"data/{NS}/dimension/oddlands.json", {
    "type": i("oddlands"),
    "generator": {"type": "minecraft:noise", "settings": i("oddlands"), "biome_source": {"type": "minecraft:multi_noise", "biomes": [
        {"biome": i("the_drift"), "parameters": params([-1.0, 1.0], [-1.0, 1.0], [-1.0, -0.06])},
        {"biome": i("jelly_oasis"), "parameters": params([0.0, 1.0], [0.1, 1.0], LANDC)},
        {"biome": i("peeper_meadow"), "parameters": params([-1.0, 0.0], [0.1, 1.0], LANDC)},
        {"biome": i("bone_flats"), "parameters": params([-1.0, 0.05], [-1.0, 0.1], LANDC)},
        {"biome": i("rust_mesas"), "parameters": params([0.05, 1.0], [-1.0, 0.1], LANDC)},
    ]}},
})

# ============================================================== features


def feature(name, obj):
    dp(f"data/{NS}/worldgen/feature/{name}.json", obj)


def placed(name, feat, placement):
    dp(f"data/{NS}/worldgen/placed_feature/{name}.json", {"feature": feat, "placement": placement})


def state(b, **props):
    s = {"id": b}
    if props:
        s["properties"] = {k: str(v) for k, v in props.items()}
    return s


BELOW_TRUNK = {"type": "minecraft:rule_based", "rules": [{"if_true": {"type": "minecraft:not", "predicate": {
    "type": "minecraft:matching_block_tag", "tag": "minecraft:cannot_replace_below_tree_trunk"}}, "then": state(i("mulch"))}]}


def tree(trunk, foliage, trunk_placer, foliage_placer, decorators=None):
    return {"type": "minecraft:tree", "below_trunk_provider": BELOW_TRUNK, "decorators": decorators or [],
            "foliage_placer": foliage_placer, "foliage_provider": state(foliage),
            "ignore_vines": True, "minimum_size": {"type": "minecraft:two_layers_feature_size"},
            "trunk_placer": trunk_placer, "trunk_provider": state(trunk, axis="y")}


DANGLE = {"type": "minecraft:attached_to_leaves", "block_provider": state(i("dangleroot")), "directions": ["down"],
          "exclusion_radius_xz": 1, "exclusion_radius_y": 0, "probability": 0.12, "required_empty_blocks": 1}
SAP = {"type": "minecraft:attached_to_logs", "block_provider": state(i("sap_glob")), "directions": ["north", "south", "east", "west"],
       "probability": 0.04}
feature("bulbwood_tree", tree(i("bulbwood_log"), i("jellyleaf"),
                              {"type": "minecraft:bending_trunk_placer", "base_height": 4, "bend_length": {"type": "minecraft:uniform", "max_inclusive": 2, "min_inclusive": 1},
                               "height_rand_a": 3, "height_rand_b": 0, "min_height_for_leaves": 3},
                              {"type": "minecraft:blob_foliage_placer", "height": 3, "offset": 0, "radius": 3}, [DANGLE, SAP]))
feature("tall_bulbwood_tree", tree(i("bulbwood_log"), i("jellyleaf"),
                                   {"type": "minecraft:fancy_trunk_placer", "base_height": 5, "height_rand_a": 9, "height_rand_b": 0},
                                   {"type": "minecraft:fancy_foliage_placer", "height": 4, "offset": 4, "radius": 2}, [DANGLE]))
feature("giant_gawkcap", tree(i("gawkcap_stem"), i("gawkcap_cap"),
                              {"type": "minecraft:straight_trunk_placer", "base_height": 4, "height_rand_a": 3, "height_rand_b": 1},
                              {"type": "minecraft:blob_foliage_placer", "height": 2, "offset": 0, "radius": 3}))
feature("oasis_trees", {"type": "minecraft:random_selector", "default": "oddlands:bulbwood_tree", "features": [
    {"chance": 0.3, "feature": "oddlands:tall_bulbwood_tree"}, {"chance": 0.1, "feature": "oddlands:giant_gawkcap"}]})

for name in ("peeper_grass", "blinkbloom", "gawkcap", "glowgulp", "ribgrass", "husk_shrub", "dustpuff", "sulkweed"):
    feature(name, {"type": "minecraft:simple_block", "to_place": state(i(name))})
feature("tooth_crystal", {"type": "minecraft:simple_block", "to_place": state(i("tooth_crystal"), facing="up")})
feature("dangleroots", {"type": "minecraft:block_column", "allowed_placement": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                        "direction": "down", "prioritize_tip": True,
                        "layers": [{"height": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 5}, "provider": state(i("dangleroot"))}]})
feature("ribstone_pillar", {"type": "minecraft:block_column", "allowed_placement": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                            "direction": "up", "prioritize_tip": False,
                            "layers": [{"height": {"type": "minecraft:biased_to_bottom", "min_inclusive": 2, "max_inclusive": 8}, "provider": state(i("ribstone"), axis="y")}]})
feature("knob_boulder", {"type": "minecraft:block_blob", "can_place_on": {"type": "minecraft:matching_block_tag", "tag": "oddlands:lush_soils"},
                         "state": state(i("knobstone"))})
feature("ore_copperknob", {"type": "minecraft:ore", "discard_chance_on_air_exposure": 0.0, "size": 9,
                           "targets": [{"state": state(i("copperknob_ore")), "target": {"predicate_type": "minecraft:tag_match", "tag": "oddlands:stone_ore_replaceable"}}]})
feature("ore_sap", {"type": "minecraft:ore", "discard_chance_on_air_exposure": 0.0, "size": 6,
                    "targets": [{"state": state(i("sap_glob")), "target": {"predicate_type": "minecraft:block_match", "block": i("mulch")}}]})
feature("spring_water", {"type": "minecraft:spring_feature", "state": {"id": "minecraft:water", "properties": {"falling": "true"}},
                         "valid_blocks": [i(b) for b in ("knobstone", "mulch", "dripcrust", "rustrock", "banded_rustrock", "bonestone")]})
feature("oasis_pond", {"type": "minecraft:lake", "barrier": state(i("mulch")), "can_place_feature": {"type": "minecraft:true"},
                       "can_replace_with_air_or_fluid": {"type": "minecraft:not", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:features_cannot_replace"}},
                       "can_replace_with_barrier": {"type": "minecraft:not", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:lava_pool_stone_cannot_replace"}},
                       "fluid": {"id": "minecraft:water", "properties": {"level": "0"}}})


def tri(lo, hi):
    return {"type": "minecraft:trapezoid", "min": lo, "max": hi, "plateau": 0}


def patch(name, feat, tries, per_patch, survive_state, rarity=None):
    p = []
    if rarity:
        p.append({"type": "minecraft:rarity_filter", "chance": rarity})
    p += [{"type": "minecraft:count", "count": tries}, {"type": "minecraft:in_square"},
          {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"},
          {"type": "minecraft:count", "count": per_patch},
          {"type": "minecraft:offset", "x": tri(-6, 6), "y": tri(-3, 3), "z": tri(-6, 6)},
          {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
              {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
              {"type": "minecraft:would_survive", "state": survive_state}]}}]
    placed(name, feat, p)


patch("patch_peeper_grass", i("peeper_grass"), 6, 24, state(i("peeper_grass")))
patch("patch_peeper_grass_sparse", i("peeper_grass"), 2, 12, state(i("peeper_grass")))
patch("patch_blinkbloom", i("blinkbloom"), 3, 10, state(i("blinkbloom")))
patch("patch_gawkcap", i("gawkcap"), 2, 8, state(i("gawkcap")))
patch("patch_glowgulp", i("glowgulp"), 1, 6, state(i("glowgulp")), rarity=2)
patch("patch_ribgrass", i("ribgrass"), 4, 18, state(i("ribgrass")))
patch("patch_husk_shrub", i("husk_shrub"), 2, 6, state(i("husk_shrub")))
patch("patch_dustpuff", i("dustpuff"), 1, 8, state(i("dustpuff")), rarity=2)
patch("patch_sulkweed", i("sulkweed"), 2, 12, state(i("sulkweed")))
patch("patch_tooth_crystal", i("tooth_crystal"), 1, 5, state(i("ribgrass")), rarity=3)


def surface_once(name, feat, count, survive=None, extra=None):
    p = [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
         {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}]
    if survive:
        p.append({"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": survive}})
    p += extra or []
    p.append({"type": "minecraft:biome"})
    placed(name, feat, p)


SPROUT = state(i("wobble_sprout"))
surface_once("oasis_trees", i("oasis_trees"), {"type": "minecraft:weighted_list", "distribution": [
    {"data": 6, "weight": 3}, {"data": 9, "weight": 1}]}, SPROUT)
surface_once("meadow_gawkcaps", i("giant_gawkcap"), {"type": "minecraft:weighted_list", "distribution": [
    {"data": 0, "weight": 3}, {"data": 1, "weight": 2}, {"data": 2, "weight": 1}]}, SPROUT)
surface_once("lone_bulbwood", i("bulbwood_tree"), {"type": "minecraft:weighted_list", "distribution": [
    {"data": 0, "weight": 5}, {"data": 1, "weight": 1}]}, SPROUT)
surface_once("knob_boulders", i("knob_boulder"), {"type": "minecraft:weighted_list", "distribution": [
    {"data": 0, "weight": 3}, {"data": 1, "weight": 1}]})
surface_once("ribstone_pillars", i("ribstone_pillar"), 3, None, [
    {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_block_tag", "tag": "oddlands:dry_soils", "offset": [0, -1, 0]}}])
placed("dangleroots", i("dangleroots"), [
    {"type": "minecraft:count", "count": 90}, {"type": "minecraft:in_square"},
    {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 20}, "max_inclusive": {"absolute": 280}}},
    {"type": "minecraft:environment_scan", "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
     "direction_of_search": "up", "max_steps": 16, "target_condition": {"type": "minecraft:has_sturdy_face", "direction": "down"}},
    {"type": "minecraft:offset", "x": 0, "y": -1, "z": 0}, {"type": "minecraft:biome"}])


def underground(name, feat, count, lo, hi):
    placed(name, feat, [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
                        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
                        {"type": "minecraft:biome"}])


underground("ore_copperknob", i("ore_copperknob"), 14, 20, 260)
underground("ore_sap", i("ore_sap"), 6, 90, 230)
underground("spring_water", i("spring_water"), 16, 60, 230)
placed("oasis_pond", i("oasis_pond"), [{"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"},
                                       {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}])

# ============================================================== carvers

dp(f"data/{NS}/worldgen/carver/island_canyon.json", {
    "type": "minecraft:canyon", "probability": 0.035,
    "shape": {"distance_factor": {"type": "minecraft:uniform", "min_inclusive": 0.75, "max_exclusive": 1.0},
              "horizontal_radius_factor": {"type": "minecraft:uniform", "min_inclusive": 0.9, "max_exclusive": 1.4},
              "thickness": {"type": "minecraft:trapezoid", "min": 0.0, "max": 6.0, "plateau": 2.0},
              "vertical_radius_center_factor": 0.0, "vertical_radius_default_factor": 1.0, "width_smoothness": 3, "y_scale": 4.0},
    "vertical_rotation": {"type": "minecraft:uniform", "min_inclusive": -0.125, "max_exclusive": 0.125},
    "y": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 110}, "max_inclusive": {"absolute": 175}}})
cave = {"type": "minecraft:cave", "count": {"type": "minecraft:very_biased_to_bottom", "max_inclusive": 14, "min_inclusive": 0}, "floor_level": {"type": "minecraft:uniform", "max_exclusive": -0.4, "min_inclusive": -1.0}, "horizontal_radius_multiplier": {"type": "minecraft:uniform", "max_exclusive": 1.4, "min_inclusive": 0.7}, "probability": 0.15, "room_vertical_radius_multiplier": {"type": "minecraft:uniform", "max_exclusive": 0.9, "min_inclusive": 0.1}, "thickness": {"type": "minecraft:trapezoid", "max": 3.0, "min": 0.0, "plateau": 1.0}, "vertical_radius_multiplier": {"type": "minecraft:uniform", "max_exclusive": 1.3, "min_inclusive": 0.8}, "weird_thickness_bias": True, "y": {"type": "minecraft:uniform", "max_inclusive": {"absolute": 180}, "min_inclusive": {"above_bottom": 8}}}
cave["y"] = {"type": "minecraft:uniform", "min_inclusive": {"absolute": 80}, "max_inclusive": {"absolute": 190}}
cave["probability"] = 0.12
dp(f"data/{NS}/worldgen/carver/island_cave.json", cave)

# ============================================================== biomes


def spawns(creature=(), monster=(), ambient=(), water=()):
    cat = {}
    for key, lst in (("creature", creature), ("monster", monster), ("ambient", ambient), ("water_creature", water)):
        if lst:
            cat[key] = [{"type": t, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi} if lo != hi else lo, "weight": w}
                        for (t, w, lo, hi) in lst]
    return {"argument": {"spawn_costs": {}, "spawns_by_category": cat}, "modifier": "overlay"}


MONSTERS = [("minecraft:spider", 100, 2, 4), ("minecraft:slime", 60, 1, 3), ("minecraft:enderman", 10, 1, 2)]


def biome(name, temp, downfall, sky, fog, water, grass, foliage, music, particles, mobs, features):
    steps = [[] for _ in range(11)]
    for step, fid in features:
        steps[step].append(i(fid) if ":" not in fid else fid)
    attrs = {
        "minecraft:audio/background_music": {"default": {"max_delay": 24000, "min_delay": 9000, "sound": music}},
        "minecraft:gameplay/natural_mob_spawns": mobs,
        "minecraft:visual/sky_color": sky,
        "minecraft:visual/fog_color": fog,
        "minecraft:visual/water_fog_color": water,
    }
    if particles:
        attrs["minecraft:visual/ambient_particles"] = {"argument": [{"particle": {"type": particles[0]}, "probability": particles[1]}],
                                                       "modifier": "append"}
    dp(f"data/{NS}/worldgen/biome/{name}.json", {
        "attributes": attrs, "carvers": [i("island_canyon"), i("island_cave")], "downfall": downfall,
        "effects": {"water_color": water, "grass_color": grass, "foliage_color": foliage},
        "features": steps, "has_precipitation": downfall > 0.1, "temperature": temp})


COMMON = [(6, "ore_copperknob"), (8, "spring_water"), (9, "dangleroots")]
biome("jelly_oasis", 0.8, 0.9, "#c7a4f0", "#f0c6e4", "#4fd6c8", "#5fd0b0", "#9ee08a", "minecraft:music.overworld.lush_caves",
      ("minecraft:spore_blossom_air", 0.004),
      spawns([("minecraft:frog", 10, 2, 4)], MONSTERS, water=[("minecraft:axolotl", 4, 1, 2)]),
      COMMON + [(1, "oasis_pond"), (6, "ore_sap"), (9, "oasis_trees"), (9, "patch_peeper_grass"), (9, "patch_glowgulp"),
                (9, "patch_blinkbloom"), (9, "patch_gawkcap")])
biome("peeper_meadow", 0.5, 0.6, "#b9a2e8", "#e8cfe6", "#5ab8d6", "#6ac4a8", "#8fd07a", "minecraft:music.overworld.meadow",
      ("minecraft:firefly", 0.002), spawns([("minecraft:rabbit", 6, 2, 3), ("minecraft:frog", 4, 1, 2)], MONSTERS),
      COMMON + [(9, "knob_boulders"), (9, "meadow_gawkcaps"), (9, "lone_bulbwood"), (9, "patch_peeper_grass"), (9, "patch_blinkbloom"),
                (9, "patch_gawkcap"), (9, "patch_sulkweed")])
biome("bone_flats", 0.6, 0.0, "#d8c7e8", "#efe2d2", "#7fb6c0", "#c0b48a", "#b0a070", "minecraft:music.overworld.desert",
      ("minecraft:white_ash", 0.006), spawns([("minecraft:rabbit", 8, 2, 3)], MONSTERS + [("minecraft:husk", 40, 1, 3)]),
      COMMON + [(9, "ribstone_pillars"), (9, "patch_ribgrass"), (9, "patch_tooth_crystal"), (9, "patch_dustpuff"), (9, "patch_husk_shrub")])
biome("rust_mesas", 1.6, 0.0, "#e0b0c8", "#f0c8a8", "#c08060", "#c08a50", "#b07a40", "minecraft:music.overworld.badlands",
      ("minecraft:white_ash", 0.002), spawns([("minecraft:armadillo", 4, 1, 2)], MONSTERS + [("minecraft:husk", 30, 1, 3)]),
      COMMON + [(9, "patch_husk_shrub"), (9, "patch_sulkweed"), (9, "patch_dustpuff")])
biome("the_drift", 0.7, 0.5, "#a88fe0", "#d8b8ee", "#4fb0d6", "#5fd0b0", "#9ee08a", "minecraft:music.overworld.deep_dark",
      ("minecraft:end_rod", 0.0006), spawns(monster=[("minecraft:phantom", 2, 1, 1)]),
      [(8, "spring_water"), (9, "dangleroots"), (9, "patch_peeper_grass_sparse"), (9, "patch_glowgulp")])

# ============================================================== structures

STRUCT = {
    "peeper_shrine": (["jelly_oasis", "peeper_meadow"], 30, 10, 31415901, "beard_thin"),
    "rib_arch": (["bone_flats"], 26, 9, 31415902, "beard_thin"),
    "lookout_tower": (["rust_mesas"], 28, 10, 31415903, "beard_box"),
    "stranded_well": (["jelly_oasis", "peeper_meadow", "bone_flats", "rust_mesas"], 40, 14, 31415904, "beard_thin"),
}
for name, (biomes, spacing, separation, salt, adapt) in STRUCT.items():
    dp(f"data/{NS}/tags/worldgen/biome/has_structure/{name}.json", {"values": [i(b) for b in biomes]})
    dp(f"data/{NS}/worldgen/template_pool/{name}/start.json", {"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
        "element_type": "minecraft:single_pool_element", "location": i(name), "processors": {"processors": []}, "projection": "rigid"}}]})
    dp(f"data/{NS}/worldgen/structure/{name}.json", {
        "type": "minecraft:jigsaw", "biomes": f"#{NS}:has_structure/{name}", "max_distance_from_center": 80,
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": 1, "spawn_overrides": {}, "start_height": {"absolute": 0},
        "start_pool": i(f"{name}/start"), "step": "surface_structures", "terrain_adaptation": adapt, "use_expansion_hack": False})
    dp(f"data/{NS}/worldgen/structure_set/{name}.json", {
        "placement": {"type": "minecraft:random_spread", "salt": salt, "separation": separation, "spacing": spacing},
        "structures": [{"structure": i(name), "weight": 1}]})

structures.build(os.path.join(DP, "data", NS, "structure"))

# ============================================================== advancements

dp(f"data/{NS}/advancement/wishing_well.json", {
    "criteria": {"activated": {"trigger": "minecraft:impossible"}},
    "display": {"announce_to_chat": True, "background": "minecraft:gui/advancements/backgrounds/stone",
                "description": {"translate": "advancements.oddlands.wishing_well.description"}, "icon": {"id": "minecraft:copper_nugget"},
                "show_toast": True, "title": {"translate": "advancements.oddlands.wishing_well.title"}},
    "requirements": [["activated"]]})
dp(f"data/{NS}/advancement/through_the_well.json", {
    "parent": i("wishing_well"),
    "criteria": {"entered": {"conditions": {"to": i("oddlands")}, "trigger": "minecraft:changed_dimension"}},
    "display": {"description": {"translate": "advancements.oddlands.through_the_well.description"}, "icon": {"id": i("squishmoss")},
                "title": {"translate": "advancements.oddlands.through_the_well.title"}},
    "requirements": [["entered"]]})
dp(f"data/{NS}/advancement/peeper.json", {
    "parent": i("through_the_well"),
    "criteria": {"picked": {"conditions": {"items": [{"items": i("blinkbloom")}]}, "trigger": "minecraft:inventory_changed"}},
    "display": {"description": {"translate": "advancements.oddlands.peeper.description"}, "icon": {"id": i("blinkbloom")},
                "title": {"translate": "advancements.oddlands.peeper.title"}},
    "requirements": [["picked"]]})

# ============================================================== functions

FUNCS = {}


def fn(name, body):
    FUNCS[name] = body.strip() + "\n"


dp("data/minecraft/tags/function/load.json", {"values": [i("load")]})
dp("data/minecraft/tags/function/tick.json", {"values": [i("tick")]})

fn("load", """
scoreboard objectives add od.cd dummy
scoreboard objectives add od.tx dummy
scoreboard objectives add od.tz dummy
scoreboard objectives add od.dest dummy
scoreboard objectives add od.wait dummy
scoreboard objectives add od.tmp dummy
scoreboard objectives add od.puff dummy
""")

fn("tick", """
scoreboard players remove @a[scores={od.cd=1..}] od.cd 1
scoreboard players remove @a[scores={od.puff=1..}] od.puff 1
# A copper nugget landing in still water: is it a finished well?
execute as @e[type=item,tag=!od.checked] if items entity @s contents minecraft:copper_nugget at @s if block ~ ~ ~ minecraft:water run function oddlands:portal/try_activate
# Players falling through a well wait here until the far side has loaded.
execute as @a[tag=od.travelling] at @s run function oddlands:portal/travel_step
""")

# ---- activation
fn("portal/try_activate", """
# as: the copper nugget item, at: its position (inside a water block)
tag @s add od.checked
execute align xyz positioned ~0.5 ~ ~0.5 unless function oddlands:portal/is_well run return run playsound minecraft:entity.generic.splash neutral @a ~ ~ ~ 0.3 1.8
function oddlands:portal/activate
""")

rim = [(1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)]
corners = [(1, 1), (1, -1), (-1, 1), (-1, -1)]
lines = ["# positioned at the centre of the water block. Returns 1 for a finished well:",
         "#   y+0: water source ringed by 8 #oddlands:well_rim blocks",
         "#   y+1, y+2: #oddlands:well_posts on the 4 corners",
         "#   y+3: 3x3 roof of #oddlands:well_roof (slabs)",
         "execute unless block ~ ~ ~ minecraft:water[level=0] run return 0"]
for (x, z) in rim:
    lines.append(f"execute unless block ~{x} ~ ~{z} #oddlands:well_rim run return 0")
for dy in (1, 2):
    for (x, z) in corners:
        lines.append(f"execute unless block ~{x} ~{dy} ~{z} #oddlands:well_posts run return 0")
for x in (-1, 0, 1):
    for z in (-1, 0, 1):
        lines.append(f"execute unless block ~{x} ~3 ~{z} #oddlands:well_roof run return 0")
lines.append("return 1")
fn("portal/is_well", "\n".join(lines).replace("~0", "~"))

fn("portal/activate", """
# as: the nugget item. Light the well, then use up one nugget.
execute align xyz positioned ~0.5 ~ ~0.5 run function oddlands:portal/light_up
execute store result score #count od.tmp run data get entity @s Item.count
execute if score #count od.tmp matches ..1 run return run kill @s
execute store result entity @s Item.count int 1 run scoreboard players remove #count od.tmp 1
tag @s remove od.checked
""")

fn("portal/light_up", """
setblock ~ ~ ~ oddlands:wishing_water
particle minecraft:wax_off ~ ~0.6 ~ 0.6 0.4 0.6 0 40
particle minecraft:glow ~ ~0.6 ~ 0.5 0.6 0.5 0 30
playsound minecraft:block.amethyst_block.chime block @a ~ ~ ~ 1.5 0.6
playsound minecraft:block.bubble_column.upwards_inside block @a ~ ~ ~ 1 0.8
advancement grant @a[distance=..16] only oddlands:wishing_well
""")

# ---- entering
fn("portal/enter", """
# Run by the wishing_water block's "entity_inside" function hook:
# as the entity inside the portal, positioned at the portal block's centre.
execute unless entity @s[type=minecraft:player] run return 0
execute if entity @s[tag=od.travelling] run return 0
execute if score @s od.cd matches 1.. run return 0
scoreboard players set @s od.cd 100
execute store result score @s od.tx run data get entity @s Pos[0]
execute store result score @s od.tz run data get entity @s Pos[2]
execute if dimension oddlands:oddlands run scoreboard players set @s od.dest 2
execute unless dimension oddlands:oddlands run scoreboard players set @s od.dest 1
scoreboard players set @s od.wait 0
tag @s add od.travelling
effect give @s minecraft:blindness 4 0 true
effect give @s minecraft:resistance 15 4 true
playsound minecraft:entity.player.splash.high_speed player @a ~ ~ ~ 1 0.6
playsound minecraft:block.portal.travel player @s ~ ~ ~ 0.25 1.8
function oddlands:portal/store_target
execute if score @s od.dest matches 1 run function oddlands:portal/go_oddlands with storage oddlands:tmp
execute if score @s od.dest matches 2 run function oddlands:portal/go_overworld with storage oddlands:tmp
""")

fn("portal/store_target", """
execute store result storage oddlands:tmp x int 1 run scoreboard players get @s od.tx
execute store result storage oddlands:tmp z int 1 run scoreboard players get @s od.tz
""")
fn("portal/go_oddlands", "$execute in oddlands:oddlands run tp @s $(x) 330 $(z)")
fn("portal/go_overworld", "$execute in minecraft:overworld run tp @s $(x) 330 $(z)")

fn("portal/travel_step", """
# as/at a travelling player, already in the destination dimension above the target column.
scoreboard players add @s od.wait 1
function oddlands:portal/store_target
function oddlands:portal/hold with storage oddlands:tmp
execute if score @s od.wait matches 600.. run return run function oddlands:portal/give_up
function oddlands:portal/try_arrive with storage oddlands:tmp
""")
fn("portal/hold", "$tp @s $(x) 330 $(z)")
fn("portal/try_arrive", """
$execute positioned $(x) 0 $(z) unless loaded ~ ~ ~ run return 0
$execute positioned $(x) 319 $(z) run function oddlands:portal/arrive
""")

fn("portal/arrive", """
# positioned at the top of the target column. 1) look for a well portal already in this column
scoreboard players set #steps od.tmp 400
execute store result score #found od.tmp run function oddlands:portal/find_portal
execute if score #found od.tmp matches 1.. run return run function oddlands:portal/finish
# 2) otherwise build a well on the first solid ground, 3) or on a fresh islet if the column is open void
scoreboard players set #steps od.tmp 400
execute store result score #found od.tmp run function oddlands:portal/find_ground
execute if score #found od.tmp matches 0 run function oddlands:portal/build_island
function oddlands:portal/finish
""")

fn("portal/find_portal", """
scoreboard players remove #steps od.tmp 1
execute if score #steps od.tmp matches ..0 run return 0
execute if block ~ ~ ~ oddlands:wishing_water run return run function oddlands:portal/mark_here
execute positioned ~ ~-1 ~ run return run function oddlands:portal/find_portal
""")
fn("portal/mark_here", """
summon minecraft:marker ~0.5 ~ ~0.5 {Tags:["od.arrival"]}
return 1
""")
fn("portal/find_ground", """
scoreboard players remove #steps od.tmp 1
execute if score #steps od.tmp matches ..0 run return 0
execute unless block ~ ~ ~ #oddlands:scan_passable run return run function oddlands:portal/build_here
execute positioned ~ ~-1 ~ run return run function oddlands:portal/find_ground
""")
fn("portal/build_here", """
# positioned at the ground block under the target column: the well's foundation replaces it
execute if dimension oddlands:oddlands positioned ~-2 ~ ~-2 run place template oddlands:well/oddlands ~ ~ ~
execute unless dimension oddlands:oddlands positioned ~-2 ~ ~-2 run place template oddlands:well/overworld ~ ~ ~
summon minecraft:marker ~0.5 ~1 ~0.5 {Tags:["od.arrival"]}
return 1
""")
fn("portal/build_island", """
execute positioned ~-4 140 ~-4 run place template oddlands:well/island ~ ~ ~
execute positioned ~ 143 ~ run summon minecraft:marker ~0.5 ~ ~0.5 {Tags:["od.arrival"]}
""")
fn("portal/finish", """
# as the player: step out beside the well, facing it
execute at @e[type=minecraft:marker,tag=od.arrival,distance=..600,limit=1,sort=nearest] run tp @s ~2 ~ ~ facing ~ ~0.6 ~
kill @e[type=minecraft:marker,tag=od.arrival,distance=..600]
tag @s remove od.travelling
scoreboard players set @s od.cd 60
effect clear @s minecraft:blindness
effect give @s minecraft:slow_falling 3 0 true
execute at @s run playsound minecraft:block.amethyst_block.resonate player @a ~ ~ ~ 1 0.7
execute at @s run particle minecraft:glow ~ ~1 ~ 0.6 1 0.6 0 30
execute if dimension oddlands:oddlands run title @s actionbar {"text":"The Oddlands","color":"light_purple","italic":true}
""")
fn("portal/give_up", """
# The far side never loaded (30 s). Land them anyway.
execute if dimension oddlands:oddlands positioned ~ 319 ~ run function oddlands:portal/build_island
execute if dimension oddlands:oddlands run return run function oddlands:portal/finish
tag @s remove od.travelling
effect give @s minecraft:slow_falling 30 0 true
""")

# ---- flora behaviours (all wired up through the "functions" component)
fn("flora/sprout_tick", """
# random_tick on a wobble sprout: about 1 in 3 ticks tries to grow
execute store result score #roll od.tmp run random value 1..3
execute if score #roll od.tmp matches 1 run function oddlands:flora/sprout_grow
""")
fn("flora/sprout_use", """
# "use" as the player: bone meal speeds it up
execute unless items entity @s weapon.mainhand minecraft:bone_meal run return 0
execute unless entity @s[gamemode=creative] run clear @s minecraft:bone_meal 1
particle minecraft:happy_villager ~ ~0.2 ~ 0.3 0.3 0.3 0 8
playsound minecraft:item.bone_meal.use block @a ~ ~ ~
execute store result score #roll od.tmp run random value 1..2
execute if score #roll od.tmp matches 1 run function oddlands:flora/sprout_grow
""")
fn("flora/sprout_grow", """
setblock ~ ~ ~ minecraft:air
execute store success score #ok od.tmp run place feature oddlands:bulbwood_tree ~ ~ ~
execute if score #ok od.tmp matches 0 run setblock ~ ~ ~ oddlands:wobble_sprout
execute if score #ok od.tmp matches 1 run playsound minecraft:block.slime_block.place block @a ~ ~ ~ 1 0.6
""")
fn("flora/gawkcap_use", """
execute unless items entity @s weapon.mainhand minecraft:bone_meal run return run playsound minecraft:entity.frog.ambient block @a ~ ~ ~ 0.6 1.8
execute unless entity @s[gamemode=creative] run clear @s minecraft:bone_meal 1
particle minecraft:happy_villager ~ ~0.2 ~ 0.3 0.3 0.3 0 8
execute store result score #roll od.tmp run random value 1..3
execute unless score #roll od.tmp matches 1 run return 0
setblock ~ ~ ~ minecraft:air
execute store success score #ok od.tmp run place feature oddlands:giant_gawkcap ~ ~ ~
execute if score #ok od.tmp matches 0 run setblock ~ ~ ~ oddlands:gawkcap
""")
fn("flora/glowgulp_use", """
# Give it a poke: it gulps, glows, and shares a little night vision.
playsound minecraft:entity.player.burp block @a ~ ~ ~ 0.8 0.5
particle minecraft:glow ~ ~0.4 ~ 0.25 0.25 0.25 0.02 14
effect give @s minecraft:night_vision 30 0 true
""")
fn("flora/blinkbloom_use", """
playsound minecraft:entity.frog.ambient block @a ~ ~ ~ 0.5 2
particle minecraft:glow ~ ~0.3 ~ 0.1 0.1 0.1 0 4
""")
fn("flora/dustpuff_step", """
# step_on fires every tick; only poof once a second per entity
execute if score @s od.puff matches 1.. run return 0
scoreboard players set @s od.puff 20
particle minecraft:white_ash ~ ~0.3 ~ 0.4 0.2 0.4 0.02 24
particle minecraft:poof ~ ~0.2 ~ 0.2 0.1 0.2 0.01 6
playsound minecraft:block.wool.break block @a ~ ~ ~ 0.6 1.7
""")

for name, body in FUNCS.items():
    dp(f"data/{NS}/function/{name}.mcfunction", body)

print("datapack files:", sum(len(f) for _, _, f in os.walk(DP)))
print("resourcepack files:", sum(len(f) for _, _, f in os.walk(RP)))
