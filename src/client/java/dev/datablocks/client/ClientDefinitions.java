package dev.datablocks.client;

import java.io.Reader;
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
import dev.datablocks.api.BlockComponents;
import dev.datablocks.api.BlockDefinition;
import dev.datablocks.api.component.ParticleEmitter;
import dev.datablocks.block.DataBlockRegistry;
import dev.datablocks.load.DefinitionParser;
import dev.datablocks.load.StartupLoader;

/**
 * Resource-pack half of a block definition: {@code assets/<ns>/block_definition/<path>.json}, holding only
 * client-side components (currently {@code tint}). Lets a resource pack restyle data blocks without touching
 * the data pack, and lets players override a server's choice locally.
 */
final class ClientDefinitions extends SimpleReloadListener<Map<Identifier, BlockDefinition>> {
	static final Identifier ID = DataBlocks.id("client_block_definitions");
	private static final Logger LOGGER = DataBlocks.LOGGER;

	private static volatile Map<Identifier, BlockDefinition> definitions = Map.of();

	static List<String> tintOverride(Identifier id) {
		BlockDefinition definition = definitions.get(id);
		return definition == null ? null : definition.get(BlockComponents.TINT);
	}

	static List<ParticleEmitter> particleOverride(Identifier id) {
		BlockDefinition definition = definitions.get(id);
		return definition == null ? null : definition.get(BlockComponents.AMBIENT_PARTICLES);
	}

	@Override
	protected Map<Identifier, BlockDefinition> prepare(SharedState state) {
		FileToIdConverter lister = FileToIdConverter.json(StartupLoader.FOLDER);
		Map<Identifier, BlockDefinition> result = new TreeMap<>();

		for (Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(state.resourceManager()).entrySet()) {
			Identifier id = lister.fileToId(entry.getKey());

			try (Reader reader = entry.getValue().openAsReader()) {
				DataResult<BlockDefinition> parsed = DefinitionParser.parse(id, JsonParser.parseReader(reader), true);
				Optional<BlockDefinition> definition = parsed.result();

				if (definition.isPresent()) {
					result.put(id, definition.get());
				} else {
					LOGGER.error("Invalid resource pack block definition {} ({}): {}", id, entry.getKey(),
							parsed.error().map(error -> error.message()).orElse("?"));
				}
			} catch (Exception e) {
				LOGGER.error("Could not read resource pack block definition {}", id, e);
			}
		}

		return result;
	}

	@Override
	protected void apply(Map<Identifier, BlockDefinition> prepared, SharedState state) {
		prepared.keySet().stream()
				.filter(id -> DataBlockRegistry.get(id) == null)
				.forEach(id -> LOGGER.warn("Resource pack defines client components for {}, which is not a registered data block", id));

		definitions = Map.copyOf(prepared);
	}
}
