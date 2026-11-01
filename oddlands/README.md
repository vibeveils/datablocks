# The Oddlands

A dimension of floating island continents, built from nothing but a data pack, a resource pack, and the
**Data Blocks** mod. Lush teal oases drip with jelly leaves and stare back at you. Bone-white flats bristle with
ribs and teeth. Rust-banded mesas stand over thousand-block drops into the void. Everything is a little bit
wrong, and fairly friendly about it.

You get there by tossing a **copper nugget into a wishing well**.

## Install

1. Install Fabric for Minecraft 26.3, Fabric API, and Data Blocks.
2. Put `oddlands_datapack` (folder or zip) in your world's `datapacks/` folder. On a server, that's
   `world/datapacks/`. For a new singleplayer world, you can add it on the **Data Packs** screen.
3. Turn on `oddlands_resourcepack` as a resource pack. On a server, serve it as the server resource pack.
4. **Restart Minecraft (or the server).** Data Blocks registers the 50 blocks while the game starts, so the
   first time you open a world with the pack, chat reminds you to restart. That only happens once.

## Getting there: the wishing well

Build this anywhere in the Overworld (or any dimension):

```
 y+3  roof     S S S         S = any slab
               S S S
               S S S

 y+1, y+2      F . F         F = any fence or wall, on the 4 corners only
 (posts)       . . .
               F . F

 y+0  rim      R R R         R = cobblestone, mossy cobblestone, stone bricks,
               R W R             mossy stone bricks, cobbled deepslate, deepslate bricks,
               R R R             or knobstone / knobstone bricks / polished knobstone
                             W = a water SOURCE block
```

Toss a **copper nugget** into the water. It chimes, the water turns into swirling copper-teal **wishing
water**, and the nugget is used up.

Jump in. You fall through the well and come out in the Oddlands **at the same X/Z**, beside a well that gets
built for you. That might be on a continent, on a cliff edge, or, if you were above open void, on a little
islet of its own. Jumping into that well takes you back to the well you came from. Wells are paired by
position, so building one somewhere new gives you a new route.

If the Overworld well has been destroyed by the time you come back, a new one is built on the surface at the
same spot.

## The land

| Biome | Feel | Look for |
|---|---|---|
| **Jellywood Oasis** | lush, wet, warm | bendy bulbwood trees with jellyleaf canopies and dangleroots, sap globs, glowgulps, ponds, frogs and axolotls |
| **Peeper Meadow** | lush, cool | peeper grass that watches you, blinkblooms, giant glowing gawkcaps, knobstone boulders |
| **Bone Flats** | dry, desolate | chalkdust and bonestone, ribstone pillars, tooth crystals, dustpuffs that go *poof* |
| **Rust Mesas** | dry, hot, vertical | layer-cake cliffs of rustrock and banded rustrock, lookout towers with views |
| **The Drift** | the void between continents | scattered floating islets, dangleroots, glowgulps |

The terrain is a custom noise router. Continent noise decides where land floats. Saturated noise flattens hill
tops into stepped mesas with sheer sides. A ridged noise cuts **canyons** straight down through the islands,
some of them bottomless. Islands are thickest in the middle, taper at the coasts, and hang dripcrust and
roots from jagged undersides. Springs spill **waterfalls off the cliff edges** into the void.

### Structures

- **Peeper Shrine** (oasis, meadow): a little knobstone temple with a watching altar and a chest.
- **Rib Arch** (bone flats): a ruin of giant ribs and tooth crystals, with a chest.
- **Lookout Tower** (rust mesas): climb the ladder for the view, and the chest at the top.
- **Stranded Well** (anywhere): an old Oddlands well that's *one post and a slab short* of working. Fix it, then
  toss in a nugget from the chest beside it.

## Blocks (50)

**Building:** knobstone, polished knobstone, knobstone bricks (+ slab, stairs, wall), watching knobstone,
chalk bricks (+ slab, stairs), rustrock tiles (+ slab, stairs), banded rustrock, bulbwood planks (+ slab,
stairs, fence, fence gate, door, trapdoor, button, pressure plate), squish cushion, peeper lantern, sap glob.

**Terrain:** squishmoss, mulch, chalkdust, bonestone, rustrock, banded rustrock, dripcrust, ribstone,
copperknob ore, bulbwood log, jellyleaf, gawkcap stem and cap.

**Flora:** peeper grass, blinkbloom, gawkcap, glowgulp, dangleroot (climbable), wobble sprout, ribgrass,
husk shrub, sulkweed, dustpuff, tooth crystal.

### Things to try

- **Bone meal a wobble sprout** to grow a bulbwood tree, or just wait. Sprouts grow on random ticks. Craft one
  from jellyleaf and bone meal.
- **Bone meal a gawkcap** to grow a giant one.
- **Poke a glowgulp.** It burps and gives you night vision.
- **Walk on a dustpuff.**
- Bounce on **squishmoss**, and bounce much higher on a **squish cushion**.
- Wade through **sap globs** and **sulkweed**. They're sticky.
- Smelt **copperknob ore** into copper ingots.

### Recipes

- 2×2 base stone → bricks/tiles. Bricks/tiles go to slabs, stairs and walls the usual way, and via the
  stonecutter.
- Bulbwood log → 4 planks. Planks work for every vanilla wooden recipe (sticks, crafting table and so on) and
  the usual bulbwood slab, stairs, fence, gate, door, trapdoor, button and pressure plate shapes.
- Knobstone smelts to polished knobstone. Chalkdust smelts to bonestone.
- Rustrock + bonestone → 2 banded rustrock.
- 2 knobstone brick slabs stacked → watching knobstone.
- 4 squishmoss → squish cushion.
- Gawkcap surrounded by 4 knobstone (plus shape) → peeper lantern.
- 9 honey bottles → sap glob.

## How it's built (what each part shows off)

| Data Blocks feature | Where it's used |
|---|---|
| Plain property components (`strength`, `sound`, `map_color`, `light_emission`, `requires_tool`, `emissive`) | stones, lantern, caps |
| `copy_properties` from vanilla and from other data blocks | bulbwood set copies oak; knobstone family copies knobstone |
| `kind` (vanilla block classes) | 16 slabs, stairs, walls, fences, gates, doors, trapdoors, buttons, plates |
| `placement` (`axis`, `facing_surface`) + rotating `shape` | logs, stems, ribstone, tooth crystals |
| `survives_on` | every plant, including the ceiling-hanging dangleroot (`direction: up`); worldgen uses it via `would_survive` |
| `offset`, `collision`, `replaceable`, `instabreak` | grasses and flowers |
| `functions.entity_inside` | **the portal**: wishing water runs `oddlands:portal/enter` |
| `functions.use` / `random_tick` / `step_on` | sprout growth, gawkcap growth, glowgulp, blinkbloom, dustpuff |
| `slows_movement` | sap glob, sulkweed |
| `jump_factor`, `flammable` (reloadable) | squishmoss, cushion; all wood |
| `ambient_particles` (data and resource pack) | wishing water, glowgulp, blinkbloom, jellyleaf, sap |
| Resource-pack `block_definition` | `assets/oddlands/block_definition/glowgulp.json` adds extra sparkle client-side |
| `drops: none` | grasses, the portal |

The vanilla data pack side: `dimension`, `dimension_type`, `noise_settings` with a custom noise router,
`density_function`, `noise`, `material_rule`, 5 biomes, 30+ configured and placed features (trees, patches,
columns, ores, lakes, springs), canyon and cave carvers, 4 jigsaw structures with `.nbt` templates,
`structure_set`s, loot tables, recipes, tags, advancements, and mcfunctions with macros for the travel logic.

## Regenerating

Everything in both packs is generated by `tools/gen.py`, including the procedural 16×16 textures and the
structure templates:

```
pip install pillow nbtlib
python3 tools/gen.py <path to 26.3 assets/minecraft with JSON> <output dir>
```

The vanilla assets are only read to copy blockstates and models for the vanilla-shaped blocks. The
`26.3-assets-json` tag of [misode/mcmeta](https://github.com/misode/mcmeta) works. `tools/preview.py` renders a
rough side and top view of the terrain density, which is handy when tuning it.

## Notes

- 26.3 data pack format 121, resource pack format 97.
- None of this has been run in-game yet. The block definitions are validated against the mod's real parser,
  and every model and texture reference resolves. The worldgen JSON follows the vanilla 26.3 data formats.
  Expect a little tuning (feature counts, noise strengths) after the first look.
- The travel functions keep you hovering, blind, for a moment while the far side loads. If it never loads
  within 30 seconds, you're dropped onto a fresh islet (in the Oddlands) or given slow falling (in the
  Overworld).
