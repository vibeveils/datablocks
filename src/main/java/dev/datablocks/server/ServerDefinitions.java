package dev.datablocks.server;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import org.slf4j.Logger;

import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import dev.datablocks.DataBlocks;
import dev.datablocks.api.BlockDefinition;
import dev.datablocks.block.DataBlockEntry;
import dev.datablocks.block.DataBlockRegistry;
import dev.datablocks.load.DefinitionParser;
import dev.datablocks.load.StartupLoader;

/**
 * Reads {@code data/<ns>/block_definition/} from the server's active data packs on every load and
 * {@code /reload}. Reloadable components are applied live; anything that would need a new block or a changed
 * static component is reported as needing a restart.
 */
public final class ServerDefinitions extends SimpleReloadListener<Map<Identifier, BlockDefinition>> {
	public static final Identifier ID = DataBlocks.id("block_definitions");
	private static final Logger LOGGER = DataBlocks.LOGGER;

	private static volatile Map<Identifier, BlockDefinition> active = Map.of();
	private static volatile List<String> needsRestart = List.of();

	public static Map<Identifier, BlockDefinition> active() {
		return active;
	}

	/** Human-readable reasons a restart is needed for the current data packs, empty if none. */
	public static List<String> needsRestart() {
		return needsRestart;
	}

	public static Map<Identifier, String> activeJson() {
		Map<Identifier, String> json = new TreeMap<>();
		active.forEach((id, definition) -> json.put(id, definition.toJson()));
		return json;
	}

	public static Map<Identifier, String> registeredJson() {
		Map<Identifier, String> json = new TreeMap<>();
		DataBlockRegistry.blocks().forEach((id, block) -> json.put(id, block.registration().toJson()));
		return json;
	}

	public static void reset() {
		active = Map.of();
		needsRestart = List.of();
		DataBlockRegistry.resetLive();
	}

	@Override
	protected Map<Identifier, BlockDefinition> prepare(SharedState state) {
		FileToIdConverter lister = FileToIdConverter.json(StartupLoader.FOLDER);
		Map<Identifier, BlockDefinition> definitions = new TreeMap<>();

		for (Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(state.resourceManager()).entrySet()) {
			Identifier id = lister.fileToId(entry.getKey());

			try (Reader reader = entry.getValue().openAsReader()) {
				DataResult<BlockDefinition> parsed = DefinitionParser.parse(id, JsonParser.parseReader(reader), false);
				Optional<BlockDefinition> definition = parsed.result();

				if (definition.isPresent()) {
					definitions.put(id, definition.get());
				} else {
					LOGGER.error("Invalid block definition {} ({}): {}", id, entry.getKey(),
							parsed.error().map(error -> error.message()).orElse("?"));
				}
			} catch (Exception e) {
				LOGGER.error("Could not read block definition {}", id, e);
			}
		}

		return definitions;
	}

	@Override
	protected void apply(Map<Identifier, BlockDefinition> definitions, SharedState state) {
		List<String> restart = new ArrayList<>();

		definitions.forEach((id, definition) -> {
			DataBlockEntry block = DataBlockRegistry.get(id);

			if (block == null) {
				restart.add(id + " is new");
			} else if (!block.registration().staticSignature().equals(definition.staticSignature())) {
				restart.add(id + " changed a component that only applies at startup");
			}
		});

		// Globally defined blocks (mods, datablocks/packs) are present in every world even when no data pack
		// lists them; they keep their registration values.
		Map<Identifier, BlockDefinition> effective = new TreeMap<>(definitions);

		for (Identifier id : DataBlockRegistry.globalIds()) {
			DataBlockEntry block = DataBlockRegistry.get(id);

			if (block != null) {
				effective.putIfAbsent(id, block.registration());
			}
		}

		active = Map.copyOf(effective);
		needsRestart = List.copyOf(restart);
		DataBlockRegistry.applyLive(effective);

		LOGGER.info("Loaded {} block definition(s) from data packs ({} registered data blocks)", definitions.size(), DataBlockRegistry.blocks().size());

		if (!restart.isEmpty()) {
			LOGGER.warn("Restart the game/server to finish applying block definitions: {}", String.join(", ", restart));
		}
	}
}
