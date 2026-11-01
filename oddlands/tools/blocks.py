"""The Oddlands block roster: data definitions plus how to build each block's assets."""

NS = "oddlands"


def i(name):
    return f"{NS}:{name}"


LUSH = "#oddlands:lush_soils"
DRY = "#oddlands:dry_soils"

# Shared component sets ----------------------------------------------------------

STONE = {"requires_tool": True, "instrument": "basedrum"}


def plant(extra=None, replaceable=False, support=LUSH, drops=None):
    c = {
        "collision": False,
        "instabreak": True,
        "occludes": False,
        "offset": "xz",
        "sound": "grass",
        "push_reaction": "popped",
        "survives_on": support,
    }
    if replaceable:
        c["replaceable"] = True
    if drops:
        c["drops"] = drops
    c.update(extra or {})
    return c


# name -> (components, asset spec)
# asset spec: ("cube", tex) | ("column", side, end) | ("bottom_top", top, side, bottom) | ("cross", tex)
#             | ("vanilla", template_prefix, texture_map) | ("model", model_json) ...
BLOCKS = {}


def block(name, components, asset, lang):
    BLOCKS[name] = {"components": components, "asset": asset, "lang": lang}


# ---------------------------------------------------------------- stone family
block("knobstone", {"strength": {"hardness": 1.5, "resistance": 6}, "sound": "tuff", "map_color": "terracotta_purple", **STONE},
      ("cube", "knobstone"), "Knobstone")
block("polished_knobstone", {"copy_properties": i("knobstone")}, ("cube", "polished_knobstone"), "Polished Knobstone")
block("knobstone_bricks", {"copy_properties": i("knobstone"), "strength": {"hardness": 2, "resistance": 6}},
      ("cube", "knobstone_bricks"), "Knobstone Bricks")
block("knobstone_brick_slab", {"copy_properties": i("knobstone_bricks"), "kind": "slab"},
      ("vanilla", "stone_brick_slab", {"stone_bricks": "knobstone_bricks"}), "Knobstone Brick Slab")
block("knobstone_brick_stairs", {"copy_properties": i("knobstone_bricks"), "kind": {"type": "stairs", "base": i("knobstone_bricks")}},
      ("vanilla", "stone_brick_stairs", {"stone_bricks": "knobstone_bricks"}), "Knobstone Brick Stairs")
block("knobstone_brick_wall", {"copy_properties": i("knobstone_bricks"), "kind": "wall"},
      ("vanilla", "stone_brick_wall", {"stone_bricks": "knobstone_bricks"}), "Knobstone Brick Wall")
block("chiseled_knobstone", {"copy_properties": i("knobstone_bricks")}, ("cube", "chiseled_knobstone"), "Watching Knobstone")
block("copperknob_ore", {"strength": {"hardness": 3, "resistance": 3}, "sound": "tuff", "map_color": "terracotta_purple", **STONE},
      ("cube", "copperknob_ore"), "Copperknob Ore")
block("dripcrust", {"strength": {"hardness": 2, "resistance": 6}, "sound": "basalt", "map_color": "terracotta_blue", **STONE},
      ("cube", "dripcrust"), "Dripcrust")

# ---------------------------------------------------------------- lush ground
block("squishmoss", {"strength": 0.6, "sound": "moss", "map_color": "color_cyan", "jump_factor": 1.15},
      ("bottom_top", "squishmoss_top", "squishmoss_side", "mulch"), "Squishmoss")
block("mulch", {"strength": 0.5, "sound": "rooted_dirt", "map_color": "terracotta_purple"}, ("cube", "mulch"), "Mulch")
block("squish_cushion", {"strength": 0.8, "sound": "wool", "map_color": "color_cyan", "jump_factor": 1.7,
                         "flammable": {"ignite_odds": 30, "burn_odds": 60}},
      ("cube", "squish_cushion"), "Squish Cushion")

# ---------------------------------------------------------------- dry ground
block("chalkdust", {"strength": 0.5, "sound": "sand", "map_color": "terracotta_white"}, ("cube", "chalkdust"), "Chalkdust")
block("bonestone", {"strength": {"hardness": 1.2, "resistance": 4}, "sound": "bone_block", "map_color": "sand", **STONE},
      ("cube", "bonestone"), "Bonestone")
block("chalk_bricks", {"copy_properties": i("bonestone")}, ("cube", "chalk_bricks"), "Chalk Bricks")
block("chalk_brick_slab", {"copy_properties": i("chalk_bricks"), "kind": "slab"},
      ("vanilla", "stone_brick_slab", {"stone_bricks": "chalk_bricks"}), "Chalk Brick Slab")
block("chalk_brick_stairs", {"copy_properties": i("chalk_bricks"), "kind": {"type": "stairs", "base": i("chalk_bricks")}},
      ("vanilla", "stone_brick_stairs", {"stone_bricks": "chalk_bricks"}), "Chalk Brick Stairs")
block("rustrock", {"strength": {"hardness": 1.25, "resistance": 4.2}, "sound": "nether_bricks", "map_color": "terracotta_orange", **STONE},
      ("cube", "rustrock"), "Rustrock")
block("banded_rustrock", {"copy_properties": i("rustrock"), "map_color": "terracotta_red"}, ("cube", "banded_rustrock"), "Banded Rustrock")
block("rustrock_tiles", {"copy_properties": i("rustrock")}, ("cube", "rustrock_tiles"), "Rustrock Tiles")
block("rustrock_tile_slab", {"copy_properties": i("rustrock_tiles"), "kind": "slab"},
      ("vanilla", "stone_brick_slab", {"stone_bricks": "rustrock_tiles"}), "Rustrock Tile Slab")
block("rustrock_tile_stairs", {"copy_properties": i("rustrock_tiles"), "kind": {"type": "stairs", "base": i("rustrock_tiles")}},
      ("vanilla", "stone_brick_stairs", {"stone_bricks": "rustrock_tiles"}), "Rustrock Tile Stairs")
block("ribstone", {"placement": "axis", "strength": 2, "sound": "bone_block", "map_color": "sand", **STONE},
      ("column", "ribstone", "ribstone_top"), "Ribstone")

# ---------------------------------------------------------------- bulbwood
WOOD_FLAMES = {"flammable": {"ignite_odds": 5, "burn_odds": 20}, "ignited_by_lava": True}
block("bulbwood_log", {"placement": "axis", "strength": 2, "sound": "wood", "map_color": "terracotta_pink",
                       "flammable": {"ignite_odds": 5, "burn_odds": 5}, "ignited_by_lava": True},
      ("column", "bulbwood_log", "bulbwood_log_top"), "Bulbwood Log")
block("bulbwood_planks", {"copy_properties": "minecraft:oak_planks", "map_color": "terracotta_pink", **WOOD_FLAMES},
      ("cube", "bulbwood_planks"), "Bulbwood Planks")
OAK = {"oak_planks": "bulbwood_planks"}
for kind, vanilla, lang in (
        ("slab", "oak_slab", "Bulbwood Slab"), ("stairs", "oak_stairs", "Bulbwood Stairs"),
        ("fence", "oak_fence", "Bulbwood Fence"), ("fence_gate", "oak_fence_gate", "Bulbwood Fence Gate"),
        ("door", "oak_door", "Bulbwood Door"), ("trapdoor", "oak_trapdoor", "Bulbwood Trapdoor"),
        ("button", "oak_button", "Bulbwood Button"), ("pressure_plate", "oak_pressure_plate", "Bulbwood Pressure Plate")):
    k = {"type": kind, "base": i("bulbwood_planks")} if kind == "stairs" else {"type": kind, "block_set": "oak"}
    comps = {"copy_properties": "minecraft:" + vanilla, "kind": k, "map_color": "terracotta_pink"}
    if kind in ("slab", "stairs", "fence", "fence_gate"):
        comps.update(WOOD_FLAMES)
    textures = dict(OAK, oak_door_bottom="bulbwood_door_bottom", oak_door_top="bulbwood_door_top", oak_trapdoor="bulbwood_trapdoor")
    block("bulbwood_" + kind, comps, ("vanilla", vanilla, textures), lang)

block("jellyleaf", {"copy_properties": "minecraft:oak_leaves", "sound": "slime_block", "map_color": "color_light_green",
                    "flammable": {"ignite_odds": 30, "burn_odds": 60},
                    "ambient_particles": {"particle": "minecraft:dripping_honey", "chance": 0.01, "offset": [0, -0.6, 0], "spread": [0.4, 0, 0.4]}},
      ("cube", "jellyleaf"), "Jellyleaf")
block("wobble_sprout", plant({"shape": [4, 0, 4, 12, 13, 12], "offset": "none",
                              "functions": {"random_tick": i("flora/sprout_tick"), "use": i("flora/sprout_use")}}),
      ("cross", "wobble_sprout"), "Wobble Sprout")

# ---------------------------------------------------------------- gawkcaps
block("gawkcap_stem", {"placement": "axis", "strength": 1, "sound": "stem", "map_color": "terracotta_white"},
      ("column", "gawkcap_stem", "gawkcap_stem_top"), "Gawkcap Stem")
block("gawkcap_cap", {"strength": 1, "sound": "wart_block", "light_emission": 10, "map_color": "color_purple"},
      ("cube", "gawkcap_cap"), "Gawkcap Cap")
block("gawkcap", plant({"light_emission": 7, "sound": "fungus", "shape": [4, 0, 4, 12, 10, 12],
                        "functions": {"use": i("flora/gawkcap_use")}},
                       support=[LUSH, i("knobstone"), i("dripcrust")]),
      ("cross", "gawkcap"), "Gawkcap")

# ---------------------------------------------------------------- lush flora
block("peeper_grass", plant({"shape": [2, 0, 2, 14, 10, 14]}, replaceable=True, drops="none"), ("cross", "peeper_grass"), "Peeper Grass")
block("blinkbloom", plant({"light_emission": 3, "shape": [5, 0, 5, 11, 12, 11],
                           "ambient_particles": {"particle": "minecraft:glow", "chance": 0.04, "offset": [0, 0.2, 0], "spread": [0.2, 0.2, 0.2]},
                           "functions": {"use": i("flora/blinkbloom_use")}}),
      ("cross", "blinkbloom"), "Blinkbloom")
block("glowgulp", plant({"light_emission": 13, "sound": "big_dripleaf", "shape": [3, 0, 3, 13, 13, 13], "offset": "none",
                         "ambient_particles": {"particle": "minecraft:glow", "chance": 0.15, "offset": [0, 0.1, 0], "spread": [0.3, 0.3, 0.3],
                                               "velocity": [0, 0.01, 0]},
                         "functions": {"use": i("flora/glowgulp_use")}}),
      ("cross", "glowgulp"), "Glowgulp")
block("dangleroot", {"collision": False, "instabreak": True, "occludes": False, "sound": "hanging_roots", "light_emission": 4,
                     "push_reaction": "popped", "shape": [3, 0, 3, 13, 16, 13],
                     "survives_on": {"blocks": "#oddlands:dangle_anchor", "direction": "up"}},
      ("cross", "dangleroot"), "Dangleroot")
block("sap_glob", {"strength": 0.5, "sound": "honey_block", "collision": False, "occludes": False, "map_color": "color_orange",
                   "slows_movement": {"horizontal": 0.3, "vertical": 0.15},
                   "ambient_particles": {"particle": "minecraft:dripping_honey", "chance": 0.05, "offset": [0, -0.5, 0], "spread": [0.4, 0, 0.4]}},
      ("cube", "sap_glob"), "Sap Glob")
block("peeper_lantern", {"strength": 1.5, "sound": "lantern", "light_emission": 15, "emissive": True, "map_color": "color_cyan"},
      ("cube", "peeper_lantern"), "Peeper Lantern")

# ---------------------------------------------------------------- dry flora
block("ribgrass", plant({"shape": [2, 0, 2, 14, 9, 14], "sound": "grass"}, replaceable=True, support=DRY, drops="none"),
      ("cross", "ribgrass"), "Ribgrass")
block("husk_shrub", plant({"shape": [2, 0, 2, 14, 13, 14], "flammable": {"ignite_odds": 60, "burn_odds": 100}}, support=DRY),
      ("cross", "husk_shrub"), "Husk Shrub")
block("sulkweed", plant({"shape": [2, 0, 2, 14, 14, 14], "slows_movement": {"horizontal": 0.8, "vertical": 0.75}},
                        replaceable=True, support=[DRY, LUSH], drops="none"),
      ("cross", "sulkweed"), "Sulkweed")
block("dustpuff", {"strength": 0.2, "sound": "wool", "occludes": False, "push_reaction": "popped", "offset": "xz",
                   "shape": [3, 0, 3, 13, 8, 13], "survives_on": DRY,
                   "functions": {"step_on": i("flora/dustpuff_step")}},
      ("cross", "dustpuff"), "Dustpuff")
block("tooth_crystal", {"placement": "facing_surface", "strength": 1.5, "sound": "amethyst_cluster", "light_emission": 5,
                        "occludes": False, "requires_tool": True, "push_reaction": "popped", "shape": [4, 4, 6, 12, 12, 16]},
      ("facing_cross", "tooth_crystal"), "Tooth Crystal")

# ---------------------------------------------------------------- the portal
block("wishing_water", {"strength": 1, "sound": "amethyst", "collision": False, "occludes": False, "light_emission": 12,
                        "emissive": True, "push_reaction": "immoveable", "drops": "none", "map_color": "color_cyan",
                        "shape": [0, 0, 0, 16, 13, 16],
                        "functions": {"entity_inside": i("portal/enter")},
                        "ambient_particles": [
                            {"particle": "minecraft:glow", "chance": 0.6, "offset": [0, 0.35, 0], "spread": [0.45, 0.05, 0.45], "velocity": [0, 0.04, 0]},
                            {"particle": "minecraft:wax_off", "chance": 0.3, "count": 2, "offset": [0, 0.5, 0], "spread": [0.5, 0.3, 0.5]},
                        ]},
      ("water", "wishing_water"), "Wishing Water")
