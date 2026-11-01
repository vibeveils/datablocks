# Data Blocks

A Fabric mod for Minecraft **26.3** that lets data packs and resource packs add real blocks. Each block is a
JSON file made of **components**, the same way items are built from data components:

```json
// data/mypack/block_definition/marble.json
{
  "components": {
    "strength": { "hardness": 1.5, "resistance": 6 },
    "sound": "calcite",
    "map_color": "quartz",
    "requires_tool": true,
    "item": {
      "creative_tabs": ["minecraft:building_blocks"],
      "components": { "rarity": "uncommon" }
    }
  }
}
```

That registers `mypack:marble` as a block and an item. Models, textures and names come from an ordinary
resource pack, the same as for any modded block.

## Requirements

| | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.20.1+ |
| Fabric API | 0.165.0+26.3 |
| Java | 25 |

Install the mod on the **server and every client**. Blocks have to exist on both sides.

## How it works

Minecraft locks its block list at startup, before any world or data pack has loaded. So Data Blocks reads
block definitions **from disk while the game starts**, from every place a data pack could come from:

1. Mod jars that contain `data/<ns>/block_definition/` files
2. `.minecraft/datablocks/packs/` for packs that apply to every world, which is the right place for modpacks.
   These blocks count as present in every world. This folder only handles **registration**, though. Tags,
   loot tables and assets still have to load as a normal data pack and resource pack (see below).
3. World data packs:
   - Dedicated server: `<level-name>/datapacks/` (read from `server.properties`)
   - Client: `saves/*/datapacks/` for every world
4. `.minecraft/datablocks/server_cache/`, where the client keeps definitions it downloaded from servers

When two sources define the same id, the later one in this list wins, and the log says so.

When the world loads, the normal data pack loader reads the same files again. This pass does two jobs. It
applies **reloadable** components live. It also spots blocks that are new or changed since startup and tells
you to restart (in the log, in chat for the singleplayer owner, and through `/datablocks pending`).

### Adding blocks to a world

- **Existing world:** put the pack in its `datapacks/` folder (on a server, `world/datapacks/`) and restart.
- **New world:** add the pack on the Create World screen as usual. If the pack's worldgen, recipes or tags
  mention its own blocks, which any custom dimension does, Minecraft refuses it the first time with "data pack
  validation failed", because those blocks don't exist yet. Data Blocks catches this *before* validation runs.
  It saves the pack's block definitions to `datablocks/auto_installed/` and logs which blocks it found. Restart,
  create the world again with the same pack, and it loads.
- `/reload` with a new pack, or turning a pack on with `/datapack enable`, goes through the same check, so the
  restart it needs registers the new blocks automatically.
- **Shipping big content:** you can also ship a pack as a mod jar with `data/` and `assets/` at the root and a
  minimal `fabric.mod.json`. Fabric loads the jar's data and assets as always-on packs, and Data Blocks
  registers its definitions at startup, so nothing needs a restart. `./gradlew oddlandsJar` builds the Oddlands
  this way.

Why a restart at all? Minecraft fixes its block list, numeric block-state IDs (used in every chunk and packet),
item registry and baked block models during startup. Changing them in a running game would invalidate all of
those. Big content frameworks (KubeJS, Forge) need a restart for new blocks for the same reason.

### Joining a server

The server checks every client's block list during configuration, before Fabric's registry sync runs.

- **Client without the mod:** it is disconnected with a message saying to install Data Blocks.
- **Client with the mod but missing blocks, or with different versions of them:** the server's definitions are
  saved to `datablocks/server_cache/<hash>/`. The client is disconnected with a list of what's missing, and
  after a restart it can join.
- **Client that matches:** it joins normally. Live values (friction, tint and so on) and the list of blocks in
  the server's packs come along with the join.

### What `/reload` can and can't change

Components are **static** or **reloadable**:

- **Static** components define the block itself: its block states, shape, light level, hardness and sounds.
  Minecraft caches these per block state, so changing one needs a restart.
- **Reloadable** components are read again on `/reload` and sent to everyone who is connected.

## Component reference

You can leave the namespace off a key: `"strength"` means `"datablocks:strength"`. Unknown keys are errors,
just as they are for item components.

### Properties (static)

| Component | Value | Notes |
|---|---|---|
| `copy_properties` | block id | Starts from a full copy of another block's properties, e.g. `"minecraft:oak_planks"`. Your other components then override it. You can copy a data block if its id sorts earlier. |
| `strength` | number or `{hardness, resistance?}` | A hardness of `-1` makes the block unbreakable. A bare number sets both values. |
| `instabreak` | bool | Breaks instantly, like flowers do. |
| `sound` | name or object | A vanilla `SoundType` field name (`stone`, `wood`, `amethyst`, `copper`...). An object form is also accepted: `{"base": "stone", "volume", "pitch", "break", "step", "place", "hit", "fall"}`, with sound event ids. |
| `map_color` | name | A `MapColor` field (`stone`, `color_red`) or a dye name (`red`). |
| `light_emission` | 0-15 | |
| `emissive` | bool | Renders at full brightness, like magma blocks. |
| `requires_tool` | bool | The block only drops with the correct tool. Pair it with the `minecraft:mineable/*` and `needs_*_tool` block tags. |
| `collision` | bool | `false` lets entities walk through the block. |
| `occludes` | bool | `false` for see-through blocks. A non-full `shape` turns occlusion off automatically. |
| `replaceable` | bool | Placing a block into it replaces it, the way placing onto grass does. |
| `instrument` | name | The note block instrument played above it: `harp`, `basedrum`, `snare`, `hat`, `bass`, `bell`... |
| `push_reaction` | name | `push_pull`, `push`, `popped`, `immoveable`, `ignore_entity`. The pre-26.3 names (`normal`, `destroy`, `block`) also work. |
| `ignited_by_lava` | bool | |
| `redstone_conductor`, `suffocates`, `view_blocking` | bool | |
| `offset` | `xz` / `xyz` | Random per-position nudge, like flowers and grass. |

### Behaviour (static)

| Component | Value | Notes |
|---|---|---|
| `kind` | name or object | Which block class to build. `block` (the default) is the data-driven block that every component here works on. `slab`, `stairs`, `fence`, `fence_gate`, `wall`, `pane`, `carpet`, `door`, `trapdoor`, `button` and `pressure_plate` reuse vanilla's classes, so connections, halves and opening behave exactly like vanilla. The object form takes `{"type": "stairs", "base": "ns:block"}` or `{"type": "door", "block_set": "oak"}` (vanilla `BlockSetType`/`WoodType` names; buttons also take `press_ticks`). Write the blockstates like vanilla's. Pair it with `copy_properties` from the vanilla equivalent (e.g. `minecraft:oak_door`) to inherit no-occlusion and push behaviour. Property components work on every kind; behaviour components only work on `block`. |
| `survives_on` | blocks, or `{"blocks", "direction"}` | Block ids, `#tags` or `sturdy`. Without valid support the block can't be placed, by players or by worldgen features, and it pops off when its support is removed. `"direction": "up"` is for hanging blocks. |
| `placement` | `none` / `axis` / `horizontal` / `facing` / `facing_surface` | Adds `axis` or `facing` and orients the block on placement. `axis` works like a log. `horizontal` works like a furnace (4 directions). `facing` works like a dispenser (6 directions, pointing toward you). `facing_surface` works like an end rod (pointing away from the clicked face). |
| `waterloggable` | bool | Adds `waterlogged` and bucket support. |
| `shape` | box or list of boxes | Pixel boxes `[x1,y1,z1,x2,y2,z2]`, 0-16. Write them for the **default orientation**: facing north for the facing modes, the Y axis for `axis`. They rotate with the block. |
| `collision_shape` | box or list | Use this when collision should differ from `shape`. |
| `gravity` | bool | The block falls like sand. |
| `redstone_power` | 0-15 | The block emits a constant signal, like a redstone block. |
| `drops` | `self` / `none` / `loot_table` | Defaults to `self`. `loot_table` uses `data/<ns>/loot_table/blocks/<path>.json`. |
| `item` | `false` or object | `false` means no item. Otherwise it takes `{"creative_tabs": [...], "components": {...}}`. The `components` are vanilla item components, decoded with full registry access on every data pack load, so registry-backed ones like enchantments work too. Every data block also shows up in the **Data Blocks** creative tab. |

### Reloadable

| Component | Value | Notes |
|---|---|---|
| `friction` | 0-1.2 | Vanilla uses 0.6, ice 0.98. |
| `speed_factor` | float | Soul sand uses 0.4. |
| `jump_factor` | float | Honey uses 0.5. |
| `flammable` | `{"ignite_odds", "burn_odds"}` | Planks use 5 / 20. |
| `functions` | object | Runs data pack functions on events: `use`, `step_on`, `entity_inside` (+ `entity_inside_interval` ticks), `placed`, `broken`, `random_tick`. Each runs with full permissions, `as` the entity involved, positioned at the block's centre in its dimension. A `use` hook consumes the click. Whether `random_tick` fires at all is fixed at startup. |
| `slows_movement` | `{"horizontal", "vertical"}` | Cobweb-style slowdown while inside the block. Cobweb is `0.25 / 0.05`, berry bush `0.8 / 0.75`. |
| `ambient_particles` | one emitter or a list | Client particles: `{"particle": id or particle object, "chance", "count", "offset", "spread", "velocity"}`. Resource packs can set this too. |
| `tint` | one value or a list | Indexed by the model's `tintindex`: `grass`, `foliage`, `water`, `#RRGGBB`, an integer, or `none`. Resource packs can set this too (see below). |

## Resource packs

Assets go in the usual places: `assets/<ns>/blockstates/`, `models/`, `items/` (item model definitions), and
`lang/` (`block.<ns>.<path>`). Render layers are automatic in 26.x, because Minecraft picks solid, cutout or
translucent from the texture's pixels.

A resource pack can also hold the **client half** of a definition at
`assets/<ns>/block_definition/<path>.json`. Only client components (`tint`, `ambient_particles`) are allowed there, and they
override the data pack's value on that client:

```json
// assets/mypack/block_definition/hedge.json
{ "components": { "tint": "foliage" } }
```

So a resource pack can restyle a server's blocks, and a player can override a server's colours on their own
client.

## Showcase: the Oddlands

[`oddlands/`](oddlands/) is a full dimension built only from data packs, resource packs and this mod. It has 50 data blocks, floating-island worldgen, structures, recipes, and wishing-well portals made with the `functions` component. See its README.

## Example pack

`example_pack/` is a combined data and resource pack with 10 blocks. It uses only vanilla textures and covers
pillars, a horizontal crate, a waterloggable slab-shaped block, a facing rod, a tinted hedge, falling
quicksand, a slippery ice tile and a redstone power block.

To try it:

1. Run `./gradlew examplePack` to get `build/libs/datablocks-example-pack.zip`.
2. Create a world and add the zip under **Data Packs**. Also add it to `resourcepacks/` and turn it on.
3. Open the world. Chat tells you to restart. Restart and open the world again.
4. Look in the **Data Blocks** creative tab.

Tag entries in the pack use `{"id": "example:marble", "required": false}`. Without that, the first world load
(before the restart) would fail the whole `mineable/pickaxe` tag, because the block doesn't exist yet. Do the
same in your own packs.

The mineable tags in the pack only apply while it is loaded as a data pack. That is why it goes in the world
rather than only in `datablocks/packs/`.

## Commands

- `/datablocks` shows a summary.
- `/datablocks list` shows every registered data block and whether this world's packs define it.
- `/datablocks pending` shows what is waiting for a restart, including blocks just auto-installed.

## For mod developers: custom components

Register component types from a `datablocks` entrypoint. It runs before any definitions are read, whatever
order mods load in:

```json
"entrypoints": { "datablocks": ["com.example.MyComponents"] }
```

```java
public final class MyComponents implements DataBlocksPlugin {
    public static BlockComponentType<Integer> GLOW_RADIUS;

    @Override
    public void registerComponents() {
        GLOW_RADIUS = BlockComponents.register(
            BlockComponentType.builder(Identifier.fromNamespaceAndPath("mymod", "glow_radius"), Codec.intRange(1, 32))
                .reloadable()                              // optional: re-read on /reload
                .properties((props, value, def) -> { })    // optional: tweak Block.Properties at registration
                .build());
    }
}
```

Every data block is a `dev.datablocks.block.DataBlock`. Read `block.live().get(MyComponents.GLOW_RADIUS)` for
the current value, or use `registration()` for the startup value. A component without a `properties` applier is
pure data, so your own events or mixins decide what it does.

## Where to put a pack

| Goal | Put the pack in | Also |
|---|---|---|
| One singleplayer world | the world's `datapacks/` folder | add the same zip as a resource pack |
| A server | `world/datapacks/` | serve the assets as the server resource pack |
| Every world (modpack) | `.minecraft/datablocks/packs/` | load it as a global data pack and resource pack too, with your launcher or a global-packs mod, if it has tags or loot tables |
| A mod | `data/<ns>/block_definition/` inside the jar | the jar's `data/` and `assets/` already load as built-in packs |

## Limitations

- New blocks, and changes to static components, need a restart. This is a Minecraft limitation. Block
  registries are frozen, and block state IDs are fixed for the session.
- On a client, blocks from **every** singleplayer world's data packs get registered. Blocks that aren't in the
  current world's packs are hidden from the creative inventory, but they still exist, so `/setblock` works.
- If two worlds, or a world and a server, define the same id differently, only one version can be registered
  at a time. The join check or the restart notice tells you when this happens.
- Data blocks have no block entities or GUIs. For behaviour, use the `functions` component with data pack
  functions, plus vanilla systems (tags, loot tables, `block_transformer`), or add components from a companion
  mod.

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`.

### GitHub Actions

`.github/workflows/build.yml` builds the mod, the example pack and the Oddlands packs on every push and pull request. You can
download the results from the run's **Artifacts** section.

To publish a release, push a version tag:

```
git tag v0.1.0
git push origin v0.1.0
```

The workflow sets the mod version from the tag (`v0.1.0` becomes `0.1.0`). It then creates a GitHub release
with the jar and all the pack zips attached.

## Status

This is a first version written against the Fabric API 0.165.0+26.3 sources and the 26.x porting notes.
Only the definition parsing and pack scanning have been tested, against stubbed Minecraft classes. The first
`./gradlew build` may turn up a few Minecraft method signatures that need adjusting.
