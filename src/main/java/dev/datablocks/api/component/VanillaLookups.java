package dev.datablocks.api.component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Name-based lookups for vanilla constants that have no codec of their own.
 *
 * <p>These read the public static constants reflectively, so every vanilla (and mixin-added) constant is
 * available by its lower-case field name, e.g. {@code "amethyst"} for {@code SoundType.AMETHYST} or
 * {@code "color_light_blue"} for {@code MapColor.COLOR_LIGHT_BLUE}. It also keeps this mod from breaking
 * when Mojang renames individual constants.
 */
public final class VanillaLookups {
	private VanillaLookups() {
	}

	private static Map<String, SoundType> soundTypes;
	private static Map<String, MapColor> mapColors;

	public static synchronized Map<String, SoundType> soundTypes() {
		if (soundTypes == null) {
			soundTypes = constantsOf(SoundType.class, SoundType.class);
		}

		return soundTypes;
	}

	public static synchronized Map<String, MapColor> mapColors() {
		if (mapColors == null) {
			mapColors = constantsOf(MapColor.class, MapColor.class);
		}

		return mapColors;
	}

	private static Map<String, BlockSetType> blockSetTypes;
	private static Map<String, WoodType> woodTypes;

	/** {@code BlockSetType} constants by name ({@code oak}, {@code iron}, {@code stone}, {@code copper}...), for doors, buttons and plates. */
	public static synchronized BlockSetType blockSetType(String name) {
		if (blockSetTypes == null) {
			blockSetTypes = constantsOf(BlockSetType.class, BlockSetType.class);
		}

		return blockSetTypes.get(normalize(name));
	}

	/** {@code WoodType} constants by name, for fence gates. */
	public static synchronized WoodType woodType(String name) {
		if (woodTypes == null) {
			woodTypes = constantsOf(WoodType.class, WoodType.class);
		}

		return woodTypes.get(normalize(name));
	}

	public static SoundType soundType(String name) {
		return soundTypes().get(normalize(name));
	}

	/** Accepts vanilla field names ({@code "stone"}, {@code "color_red"}) and bare dye names ({@code "red"}). */
	public static MapColor mapColor(String name) {
		String key = normalize(name);
		MapColor color = mapColors().get(key);
		return color != null ? color : mapColors().get("color_" + key);
	}

	public static NoteBlockInstrument instrument(String name) {
		String key = normalize(name);

		for (NoteBlockInstrument instrument : NoteBlockInstrument.values()) {
			if (instrument.name().toLowerCase(Locale.ROOT).equals(key) || instrument.getSerializedName().equals(key)) {
				return instrument;
			}
		}

		return null;
	}

	/**
	 * Push reactions were renamed in 26.3 ({@code NORMAL -> PUSH_PULL}, {@code DESTROY -> POPPED}, ...).
	 * Both the new names and the pre-26.3 names are accepted so older packs keep working.
	 */
	public static PushReaction pushReaction(String name) {
		String key = normalize(name);
		String alias = switch (key) {
			case "normal" -> "push_pull";
			case "destroy" -> "popped";
			case "block" -> "immoveable";
			case "immovable" -> "immoveable";
			case "ignore" -> "ignore_entity";
			case "push_only" -> "push";
			default -> key;
		};

		for (PushReaction reaction : PushReaction.values()) {
			String constant = reaction.name().toLowerCase(Locale.ROOT);

			if (constant.equals(alias) || constant.equals(key)) {
				return reaction;
			}
		}

		return null;
	}

	public static String normalize(String name) {
		String key = name.toLowerCase(Locale.ROOT).trim();
		return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
	}

	private static <T> Map<String, T> constantsOf(Class<?> owner, Class<T> type) {
		Map<String, T> result = new TreeMap<>();

		for (Field field : owner.getFields()) {
			int mods = field.getModifiers();

			if (Modifier.isStatic(mods) && Modifier.isFinal(mods) && type.isAssignableFrom(field.getType())) {
				try {
					result.put(field.getName().toLowerCase(Locale.ROOT), type.cast(field.get(null)));
				} catch (IllegalAccessException ignored) {
					// public field, should not happen
				}
			}
		}

		return Map.copyOf(result);
	}
}
