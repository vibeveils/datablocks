package dev.datablocks.load;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import dev.datablocks.DataBlocks;
import dev.datablocks.api.BlockDefinition;
import dev.datablocks.block.DataBlockEntry;
import dev.datablocks.block.DataBlockRegistry;

/**
 * Catches block definitions in a data pack that this game session didn't register, at the moment a data pack set
 * starts loading and before Minecraft validates it.
 *
 * <p>Blocks can only be registered while the game starts. A data pack whose worldgen, recipes or tags mention its
 * own blocks therefore fails validation the first time it is used, typically when it's added on the Create World
 * screen, because the world (and its {@code datapacks/} folder) never gets created for the next launch to find.
 * This check saves those definitions to {@code datablocks/auto_installed/}, which {@link StartupLoader} reads at
 * startup, so after one restart the same pack loads without any manual copying.
 */
public final class PendingBlocks {
	private static final Logger LOGGER = DataBlocks.LOGGER;
	private static volatile List<Identifier> lastInstalled = List.of();

	private PendingBlocks() {
	}

	public static Path dir() {
		return StartupLoader.rootDir().resolve("auto_installed");
	}

	/** Ids installed by the most recent check, i.e. what the next restart will add. */
	public static List<Identifier> lastInstalled() {
		return lastInstalled;
	}

	/** Called (from a mixin) with the resource manager of a data pack set that is about to load its registries. */
	public static synchronized void check(ResourceManager resourceManager) {
		Map<Identifier, BlockDefinition> pending = new TreeMap<>();

		try {
			FileToIdConverter lister = FileToIdConverter.json(StartupLoader.FOLDER);

			for (Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(resourceManager).entrySet()) {
				Identifier id = lister.fileToId(entry.getKey());

				try (Reader reader = entry.getValue().openAsReader()) {
					Optional<BlockDefinition> parsed = DefinitionParser.parse(id, JsonParser.parseReader(reader), false).result();

					if (parsed.isEmpty()) {
						continue; // reported properly by ServerDefinitions once the pack loads
					}

					DataBlockEntry registered = DataBlockRegistry.get(id);

					if (registered == null || !registered.registration().staticSignature().equals(parsed.get().staticSignature())) {
						pending.put(id, parsed.get());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Could not check data packs for new block definitions", e);
			return;
		}

		if (pending.isEmpty()) {
			return;
		}

		try {
			install(pending);
		} catch (IOException e) {
			LOGGER.error("Could not save new block definitions to {}", dir(), e);
			return;
		}

		lastInstalled = List.copyOf(pending.keySet());
		List<String> ids = new ArrayList<>();
		pending.keySet().forEach(id -> ids.add(id.toString()));
		LOGGER.warn("""

				============================================================
				 Data Blocks: these data packs add {} block(s) that aren't
				 registered yet: {}
				 They have been installed to {}.
				 RESTART Minecraft (or the server), then create/open the
				 world again. If this world was being created, it will fail
				 validation now; that's expected until the restart.
				============================================================""",
				pending.size(), summarise(ids), dir());
	}

	private static void install(Map<Identifier, BlockDefinition> definitions) throws IOException {
		Path root = dir();
		var gson = new GsonBuilder().setPrettyPrinting().create();

		for (BlockDefinition definition : definitions.values()) {
			Identifier id = definition.id();
			Path file = root.resolve("data").resolve(id.getNamespace()).resolve(StartupLoader.FOLDER).resolve(id.getPath() + ".json");
			Files.createDirectories(file.getParent());
			Files.writeString(file, gson.toJson(JsonParser.parseString(definition.toJson())), StandardCharsets.UTF_8);
		}

		Path meta = root.resolve("pack.mcmeta");

		if (!Files.exists(meta)) {
			Files.writeString(meta, "{\"pack\":{\"description\":\"Block definitions Data Blocks picked up from data packs\",\"min_format\":94,\"max_format\":999}}",
					StandardCharsets.UTF_8);
		}
	}

	private static String summarise(List<String> ids) {
		return ids.size() <= 6 ? String.join(", ", ids) : String.join(", ", ids.subList(0, 6)) + " and " + (ids.size() - 6) + " more";
	}
}
