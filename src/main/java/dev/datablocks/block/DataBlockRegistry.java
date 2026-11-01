package dev.datablocks.block;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;

import net.minecraft.resources.Identifier;

import dev.datablocks.DataBlocks;
import dev.datablocks.api.BlockComponents;
import dev.datablocks.api.BlockDefinition;
import dev.datablocks.api.component.Flammability;
import dev.datablocks.api.component.Kind;

/** Every data block registered this session, plus the live (reloadable) state applied to them. */
public final class DataBlockRegistry {
	private static final Map<Identifier, DataBlockEntry> ENTRIES = new TreeMap<>();
	private static volatile Set<Identifier> activeIds;
	private static Set<Identifier> globalIds = Set.of();

	private DataBlockRegistry() {
	}

	/**
	 * Registers every definition. A definition that refers to another data block ({@code copy_properties}, or the
	 * {@code base} of stairs) is registered after it, whatever the ids' alphabetical order.
	 */
	public static void registerAll(Map<Identifier, BlockDefinition> definitions, Set<Identifier> global) {
		globalIds = Set.copyOf(global);
		Set<Identifier> visiting = new HashSet<>();

		for (Identifier id : definitions.keySet()) {
			registerWithDependencies(id, definitions, visiting);
		}

		resetLive();
	}

	private static void registerWithDependencies(Identifier id, Map<Identifier, BlockDefinition> definitions, Set<Identifier> visiting) {
		if (ENTRIES.containsKey(id) || !definitions.containsKey(id)) {
			return;
		}

		if (!visiting.add(id)) {
			DataBlocks.LOGGER.error("Data block {} depends on itself through copy_properties / kind.base", id);
			return;
		}

		BlockDefinition definition = definitions.get(id);
		Identifier copyFrom = definition.get(BlockComponents.COPY_PROPERTIES);
		Kind kind = definition.get(BlockComponents.KIND);

		if (copyFrom != null) {
			registerWithDependencies(copyFrom, definitions, visiting);
		}

		if (kind != null && kind.base().isPresent()) {
			registerWithDependencies(kind.base().get(), definitions, visiting);
		}

		var block = BlockFactory.register(definition);

		if (block != null) {
			ENTRIES.put(id, new DataBlockEntry(id, block, definition));
		}

		visiting.remove(id);
	}

	public static Map<Identifier, DataBlockEntry> blocks() {
		return Collections.unmodifiableMap(ENTRIES);
	}

	public static DataBlockEntry get(Identifier id) {
		return ENTRIES.get(id);
	}

	/** Ids defined by mods or global packs, which count as present in every world's data packs. */
	public static Set<Identifier> globalIds() {
		return globalIds;
	}

	public static boolean isEmpty() {
		return ENTRIES.isEmpty();
	}

	/**
	 * Applies the definitions currently present in the active data packs. Static components always stay as
	 * registered; reloadable ones come from {@code active}. Blocks missing from {@code active} fall back to
	 * their registration values.
	 */
	public static void applyLive(Map<Identifier, BlockDefinition> active) {
		ENTRIES.forEach((id, entry) -> {
			BlockDefinition current = active.get(id);
			entry.setLive(current == null ? entry.registration() : entry.registration().withReloadableFrom(current));
		});

		applyFlammability();
		activeIds = Set.copyOf(active.keySet());
	}

	/** Back to registration values, e.g. after leaving a world. Every block counts as active again. */
	public static void resetLive() {
		ENTRIES.values().forEach(entry -> entry.setLive(entry.registration()));
		applyFlammability();
		activeIds = null;
	}

	private static void applyFlammability() {
		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();

		ENTRIES.values().forEach(entry -> {
			Flammability flammability = entry.live().get(BlockComponents.FLAMMABLE);

			if (flammability != null) {
				flammable.add(entry.block(), flammability.igniteOdds(), flammability.burnOdds());
			} else {
				flammable.remove(entry.block());
			}
		});
	}

	/** Sets which blocks are in the current world's data packs without touching live values (singleplayer clients). */
	public static void setActiveIds(Set<Identifier> ids) {
		activeIds = ids == null ? null : Set.copyOf(ids);
	}

	/**
	 * Whether the block's definition is in the current world's data packs. Blocks from other worlds are still
	 * registered (they have to be), but are hidden from the creative inventory.
	 */
	public static boolean isActive(Identifier id) {
		Set<Identifier> ids = activeIds;
		return ids == null || ids.contains(id);
	}
}
