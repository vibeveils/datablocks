package dev.datablocks.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockBehaviour;

import dev.datablocks.DataBlocks;
import dev.datablocks.api.component.Box;
import dev.datablocks.api.component.DataCodecs;
import dev.datablocks.api.component.Drops;
import dev.datablocks.api.component.Flammability;
import dev.datablocks.api.component.FunctionHooks;
import dev.datablocks.api.component.ItemSpec;
import dev.datablocks.api.component.Kind;
import dev.datablocks.api.component.ParticleEmitter;
import dev.datablocks.api.component.Placement;
import dev.datablocks.api.component.Slowdown;
import dev.datablocks.api.component.SoundSpec;
import dev.datablocks.api.component.Strength;
import dev.datablocks.api.component.Support;
import dev.datablocks.api.component.VanillaLookups;
import dev.datablocks.api.component.TintSpec;

/**
 * The registry of block component types and every built-in component.
 *
 * <p>In JSON, component keys without a namespace resolve to {@code datablocks:}, so {@code "strength"} and
 * {@code "datablocks:strength"} are the same component. Other mods add their own through
 * {@link #register(BlockComponentType)} from a {@code "datablocks"} entrypoint (see {@link DataBlocksPlugin}).
 */
public final class BlockComponents {
	private static final Map<Identifier, BlockComponentType<?>> TYPES = new LinkedHashMap<>();

	private BlockComponents() {
	}

	// ---------------------------------------------------------------- properties (static)

	/** Start from a full copy of another block's properties, e.g. {@code "minecraft:oak_planks"}. Applied before everything else. */
	public static final BlockComponentType<Identifier> COPY_PROPERTIES = register(
			BlockComponentType.builder(DataBlocks.id("copy_properties"), Identifier.CODEC).build());

	public static final BlockComponentType<Strength> STRENGTH = register(
			BlockComponentType.builder(DataBlocks.id("strength"), Strength.CODEC)
					.properties((props, value, def) -> props.strength(value.hardness(), value.resistance()))
					.build());

	/** Breaks instantly, like flowers and torches. */
	public static final BlockComponentType<Boolean> INSTABREAK = register(
			BlockComponentType.builder(DataBlocks.id("instabreak"), Codec.BOOL)
					.properties((props, value, def) -> {
						if (value) props.instabreak();
					})
					.build());

	public static final BlockComponentType<SoundSpec> SOUND = register(
			BlockComponentType.builder(DataBlocks.id("sound"), SoundSpec.CODEC)
					.properties((props, value, def) -> props.sound(value.resolve()))
					.build());

	public static final BlockComponentType<String> MAP_COLOR = register(
			BlockComponentType.builder(DataBlocks.id("map_color"), DataCodecs.validatedString(name ->
							VanillaLookups.mapColor(name) != null ? null : "Unknown map color '" + name + "'. Known: " + VanillaLookups.mapColors().keySet()))
					.properties((props, value, def) -> props.mapColor(VanillaLookups.mapColor(value)))
					.build());

	public static final BlockComponentType<Integer> LIGHT_EMISSION = register(
			BlockComponentType.builder(DataBlocks.id("light_emission"), Codec.intRange(0, 15))
					.properties((props, value, def) -> props.lightLevel(state -> value))
					.build());

	/** Only drops when mined with the correct tool. Pair with the {@code minecraft:mineable/*} and {@code needs_*_tool} block tags. */
	public static final BlockComponentType<Boolean> REQUIRES_TOOL = register(
			BlockComponentType.builder(DataBlocks.id("requires_tool"), Codec.BOOL)
					.properties((props, value, def) -> {
						if (value) props.requiresCorrectToolForDrops();
					})
					.build());

	/** {@code false} makes entities walk through the block. */
	public static final BlockComponentType<Boolean> COLLISION = register(
			BlockComponentType.builder(DataBlocks.id("collision"), Codec.BOOL)
					.properties((props, value, def) -> {
						if (!value) props.noCollision();
					})
					.build());

	/** {@code false} for see-through blocks (glass, leaves). Defaults to {@code false} automatically when a non-full {@code shape} is set. */
	public static final BlockComponentType<Boolean> OCCLUDES = register(
			BlockComponentType.builder(DataBlocks.id("occludes"), Codec.BOOL)
					.properties((props, value, def) -> {
						if (!value) props.noOcclusion();
					})
					.build());

	/** Can be replaced by placing another block into it, like grass or snow layers. */
	public static final BlockComponentType<Boolean> REPLACEABLE = register(
			BlockComponentType.builder(DataBlocks.id("replaceable"), Codec.BOOL)
					.properties((props, value, def) -> {
						if (value) props.replaceable();
					})
					.build());

	public static final BlockComponentType<String> INSTRUMENT = register(
			BlockComponentType.builder(DataBlocks.id("instrument"), DataCodecs.validatedString(name ->
							VanillaLookups.instrument(name) != null ? null : "Unknown note block instrument '" + name + "'"))
					.properties((props, value, def) -> props.instrument(VanillaLookups.instrument(value)))
					.build());

	/** How pistons treat the block: {@code push_pull}, {@code push}, {@code popped}, {@code immoveable}, {@code ignore_entity} (pre-26.3 names also work). */
	public static final BlockComponentType<String> PUSH_REACTION = register(
			BlockComponentType.builder(DataBlocks.id("push_reaction"), DataCodecs.validatedString(name ->
							VanillaLookups.pushReaction(name) != null ? null : "Unknown push reaction '" + name + "'"))
					.properties((props, value, def) -> props.pushReaction(VanillaLookups.pushReaction(value)))
					.build());

	/** Lava can set it on fire. Use together with {@code flammable}. */
	public static final BlockComponentType<Boolean> IGNITED_BY_LAVA = register(
			BlockComponentType.builder(DataBlocks.id("ignited_by_lava"), Codec.BOOL)
					.properties((props, value, def) -> {
						if (value) props.ignitedByLava();
					})
					.build());

	/** Renders at full brightness regardless of light, like magma blocks. */
	public static final BlockComponentType<Boolean> EMISSIVE = register(
			BlockComponentType.builder(DataBlocks.id("emissive"), Codec.BOOL)
					.properties((props, value, def) -> {
						if (value) props.emissiveRendering(state -> true);
					})
					.build());

	public static final BlockComponentType<Boolean> REDSTONE_CONDUCTOR = register(
			BlockComponentType.builder(DataBlocks.id("redstone_conductor"), Codec.BOOL)
					.properties((props, value, def) -> props.isRedstoneConductor((state, level, pos) -> value))
					.build());

	public static final BlockComponentType<Boolean> SUFFOCATES = register(
			BlockComponentType.builder(DataBlocks.id("suffocates"), Codec.BOOL)
					.properties((props, value, def) -> props.isSuffocating((state, level, pos) -> value))
					.build());

	public static final BlockComponentType<Boolean> VIEW_BLOCKING = register(
			BlockComponentType.builder(DataBlocks.id("view_blocking"), Codec.BOOL)
					.properties((props, value, def) -> props.isViewBlocking((state, level, pos) -> value))
					.build());

	/** Random horizontal (and optionally vertical) offset per position, like flowers and grass: {@code xz} or {@code xyz}. */
	public static final BlockComponentType<String> OFFSET = register(
			BlockComponentType.builder(DataBlocks.id("offset"), DataCodecs.validatedString(name ->
							name.equals("xz") || name.equals("xyz") || name.equals("none") ? null : "offset must be xz, xyz or none"))
					.properties((props, value, def) -> {
						switch (value) {
							case "xz" -> props.offsetType(BlockBehaviour.OffsetType.XZ);
							case "xyz" -> props.offsetType(BlockBehaviour.OffsetType.XYZ);
							default -> {
							}
						}
					})
					.build());

	// ---------------------------------------------------------------- behaviour (static)

	public static final BlockComponentType<Placement> PLACEMENT = register(
			BlockComponentType.builder(DataBlocks.id("placement"), Placement.CODEC).build());

	/** Adds the {@code waterlogged} property and bucket support. */
	public static final BlockComponentType<Boolean> WATERLOGGABLE = register(
			BlockComponentType.builder(DataBlocks.id("waterloggable"), Codec.BOOL).build());

	/** Outline (and by default collision) shape, authored for the default orientation and rotated automatically. */
	public static final BlockComponentType<List<Box>> SHAPE = register(
			BlockComponentType.builder(DataBlocks.id("shape"), Box.SHAPE_CODEC).build());

	/** Collision shape when it should differ from {@code shape}. */
	public static final BlockComponentType<List<Box>> COLLISION_SHAPE = register(
			BlockComponentType.builder(DataBlocks.id("collision_shape"), Box.SHAPE_CODEC).build());

	/** Falls like sand when unsupported. */
	public static final BlockComponentType<Boolean> GRAVITY = register(
			BlockComponentType.builder(DataBlocks.id("gravity"), Codec.BOOL).build());

	/** Constant redstone signal emitted on all sides, like a redstone block. */
	public static final BlockComponentType<Integer> REDSTONE_POWER = register(
			BlockComponentType.builder(DataBlocks.id("redstone_power"), Codec.intRange(0, 15)).build());

	public static final BlockComponentType<Drops> DROPS = register(
			BlockComponentType.builder(DataBlocks.id("drops"), Drops.CODEC).build());

	public static final BlockComponentType<ItemSpec> ITEM = register(
			BlockComponentType.builder(DataBlocks.id("item"), ItemSpec.CODEC).build());

	/** Which block class to build: the data-driven {@code block} (default), or a vanilla shape such as {@code slab} or {@code stairs}. See {@link Kind}. */
	public static final BlockComponentType<Kind> KIND = register(
			BlockComponentType.builder(DataBlocks.id("kind"), Kind.CODEC).build());

	/** What the block must be attached to. See {@link Support}. */
	public static final BlockComponentType<Support> SURVIVES_ON = register(
			BlockComponentType.builder(DataBlocks.id("survives_on"), Support.CODEC).build());

	// ---------------------------------------------------------------- reloadable

	/** Slipperiness. Vanilla default 0.6, ice 0.98, blue ice 0.989. */
	public static final BlockComponentType<Float> FRICTION = register(
			BlockComponentType.builder(DataBlocks.id("friction"), Codec.floatRange(0f, 1.2f)).reloadable().build());

	/** Walking speed multiplier. Soul sand 0.4, honey 0.4. */
	public static final BlockComponentType<Float> SPEED_FACTOR = register(
			BlockComponentType.builder(DataBlocks.id("speed_factor"), Codec.floatRange(0f, 10f)).reloadable().build());

	/** Jump height multiplier. Honey 0.5. */
	public static final BlockComponentType<Float> JUMP_FACTOR = register(
			BlockComponentType.builder(DataBlocks.id("jump_factor"), Codec.floatRange(0f, 10f)).reloadable().build());

	public static final BlockComponentType<Flammability> FLAMMABLE = register(
			BlockComponentType.builder(DataBlocks.id("flammable"), Flammability.CODEC).reloadable().build());

	/**
	 * Tint colours, indexed by the model's {@code tintindex}: {@code "grass"}, {@code "foliage"}, {@code "water"},
	 * {@code "#RRGGBB"}, an integer, or {@code "none"}. One value or a list. Also allowed in resource packs.
	 */
	public static final BlockComponentType<List<String>> TINT = register(
			BlockComponentType.builder(DataBlocks.id("tint"), DataCodecs.singleOrList(TintSpec.CODEC)).reloadable().clientAllowed().build());

	/** Functions run on use, step, entity contact, placement, breaking and random ticks. See {@link FunctionHooks}. */
	public static final BlockComponentType<FunctionHooks> FUNCTIONS = register(
			BlockComponentType.builder(DataBlocks.id("functions"), FunctionHooks.CODEC).reloadable()
					.properties((props, value, def) -> {
						if (value.randomTick().isPresent()) props.randomTicks();
					})
					.build());

	/** Entities inside move as if stuck in a cobweb. See {@link Slowdown}. */
	public static final BlockComponentType<Slowdown> SLOWS_MOVEMENT = register(
			BlockComponentType.builder(DataBlocks.id("slows_movement"), Slowdown.CODEC).reloadable().build());

	/** Client-side ambient particles. See {@link ParticleEmitter}. Also allowed in resource packs. */
	public static final BlockComponentType<List<ParticleEmitter>> AMBIENT_PARTICLES = register(
			BlockComponentType.builder(DataBlocks.id("ambient_particles"), ParticleEmitter.LIST_CODEC).reloadable().clientAllowed().build());

	// ---------------------------------------------------------------- registry

	/** Registers a component type. Must happen before blocks load, i.e. from a {@code "datablocks"} entrypoint. */
	public static synchronized <T> BlockComponentType<T> register(BlockComponentType<T> type) {
		if (TYPES.containsKey(type.id())) {
			throw new IllegalArgumentException("Duplicate block component type " + type.id());
		}

		TYPES.put(type.id(), type);
		return type;
	}

	public static BlockComponentType<?> get(Identifier id) {
		return TYPES.get(id);
	}

	public static Collection<BlockComponentType<?>> all() {
		return Collections.unmodifiableCollection(TYPES.values());
	}

	/** Resolves a JSON key: {@code "strength"} becomes {@code datablocks:strength}. Returns {@code null} for malformed keys. */
	public static Identifier resolveKey(String key) {
		return key.indexOf(':') >= 0 ? Identifier.tryParse(key) : Identifier.tryBuild(DataBlocks.MOD_ID, key);
	}

	/** Forces class initialisation so the built-ins are registered. */
	public static void bootstrap() {
	}
}
