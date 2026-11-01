package dev.datablocks.load;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import org.slf4j.Logger;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.resources.Identifier;

import dev.datablocks.DataBlocks;
import dev.datablocks.api.BlockDefinition;

/**
 * Finds block definitions before the block registry freezes.
 *
 * <p>Blocks can't be added to a running game, so this reads the same {@code data/<ns>/block_definition/}
 * files that the data pack loader will see later, straight from disk, during mod initialisation. Sources,
 * lowest priority first (a later source overrides an earlier one with the same id):
 *
 * <ol>
 *   <li>mod jars that ship {@code data/<ns>/block_definition/} files</li>
 *   <li>{@code <game dir>/datablocks/packs/} — packs that apply to every world (good for modpacks)</li>
 *   <li>world data packs: {@code <level-name>/datapacks/} on a dedicated server, every {@code saves/<world>/datapacks/} on a client</li>
 *   <li>{@code <game dir>/datablocks/server_cache/} — definitions downloaded from servers (client only)</li>
 * </ol>
 *
 * Each pack may be a folder or a {@code .zip}.
 */
public final class StartupLoader {
	private static final Logger LOGGER = DataBlocks.LOGGER;
	public static final String FOLDER = "block_definition";

	private StartupLoader() {
	}

	public static Path rootDir() {
		return FabricLoader.getInstance().getGameDir().resolve(DataBlocks.MOD_ID);
	}

	public static Path packsDir() {
		return rootDir().resolve("packs");
	}

	public static Path serverCacheDir() {
		return rootDir().resolve("server_cache");
	}

	/**
	 * @param definitions every definition found, by id
	 * @param global ids defined by mods or {@code datablocks/packs/}; these count as present in every world
	 */
	public record Result(Map<Identifier, BlockDefinition> definitions, Set<Identifier> global) {
	}

	public static Result load() {
		Map<Identifier, Found> found = new TreeMap<>();
		FabricLoader loader = FabricLoader.getInstance();
		Path gameDir = loader.getGameDir();

		try {
			Files.createDirectories(packsDir());
		} catch (IOException e) {
			LOGGER.warn("Could not create {}", packsDir(), e);
		}

		for (ModContainer mod : loader.getAllMods()) {
			for (Path root : mod.getRootPaths()) {
				scanRoot(root, "mod " + mod.getMetadata().getId(), found);
			}
		}

		scanPackFolder(packsDir(), "global pack", found);
		Set<Identifier> global = Set.copyOf(found.keySet());

		if (loader.getEnvironmentType() == EnvType.SERVER) {
			String levelName = readLevelName(gameDir);
			scanPackFolder(gameDir.resolve(levelName).resolve("datapacks"), "world '" + levelName + "'", found);
		} else {
			Path saves = gameDir.resolve("saves");

			for (Path world : list(saves)) {
				if (Files.isDirectory(world)) {
					scanPackFolder(world.resolve("datapacks"), "world '" + world.getFileName() + "'", found);
				}
			}

			// Oldest first, so the most recent download from a server wins.
			List<Path> downloads = new ArrayList<>(list(serverCacheDir()));
			downloads.sort(Comparator.comparingLong(StartupLoader::lastModified));

			for (Path download : downloads) {
				if (Files.isDirectory(download) && !download.getFileName().toString().endsWith(".tmp")) {
					scanRoot(download, "server download " + download.getFileName(), found);
				}
			}
		}

		Map<Identifier, BlockDefinition> result = new TreeMap<>();
		found.forEach((id, entry) -> result.put(id, entry.definition()));
		return new Result(result, global);
	}

	private static String readLevelName(Path gameDir) {
		Path file = gameDir.resolve("server.properties");
		Properties properties = new Properties();

		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				properties.load(reader);
			} catch (IOException e) {
				LOGGER.warn("Could not read server.properties, assuming level-name=world", e);
			}
		}

		return properties.getProperty("level-name", "world");
	}

	/** Scans every pack (folder or zip) inside {@code folder}. */
	private static void scanPackFolder(Path folder, String sourceName, Map<Identifier, Found> found) {
		for (Path pack : list(folder)) {
			String name = sourceName + " / " + pack.getFileName();

			if (Files.isDirectory(pack)) {
				scanRoot(pack, name, found);
			} else if (pack.getFileName().toString().toLowerCase().endsWith(".zip")) {
				try (FileSystem zip = FileSystems.newFileSystem(pack)) {
					for (Path root : zip.getRootDirectories()) {
						scanRoot(root, name, found);
					}
				} catch (IOException | RuntimeException e) {
					LOGGER.error("Could not open pack {}", pack, e);
				}
			}
		}
	}

	/** Scans {@code root/data/<ns>/block_definition/**.json}. */
	private static void scanRoot(Path root, String sourceName, Map<Identifier, Found> found) {
		Path data = root.resolve("data");

		if (!Files.isDirectory(data)) {
			return;
		}

		for (Path namespaceDir : list(data)) {
			Path definitions = namespaceDir.resolve(FOLDER);

			if (!Files.isDirectory(definitions)) {
				continue;
			}

			String namespace = stripSlash(namespaceDir.getFileName().toString());
			List<Path> files;

			try (Stream<Path> walk = Files.walk(definitions)) {
				files = walk.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
						.sorted(Comparator.comparing(Path::toString))
						.toList();
			} catch (IOException e) {
				LOGGER.error("Could not list {}", definitions, e);
				continue;
			}

			for (Path file : files) {
				String relative = definitions.relativize(file).toString().replace('\\', '/');
				String path = relative.substring(0, relative.length() - ".json".length());
				Identifier id = Identifier.tryBuild(namespace, path);

				if (id == null) {
					LOGGER.error("Ignoring block definition with invalid id {}:{} in {}", namespace, path, sourceName);
					continue;
				}

				readDefinition(id, file, sourceName, found);
			}
		}
	}

	private static void readDefinition(Identifier id, Path file, String sourceName, Map<Identifier, Found> found) {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			DataResult<BlockDefinition> parsed = DefinitionParser.parse(id, JsonParser.parseReader(reader), false);
			Optional<BlockDefinition> definition = parsed.result();

			if (definition.isEmpty()) {
				LOGGER.error("Invalid block definition {} in {}: {}", id, sourceName, parsed.error().map(error -> error.message()).orElse("?"));
				return;
			}

			Found previous = found.put(id, new Found(definition.get(), sourceName));

			if (previous != null && !previous.definition().staticSignature().equals(definition.get().staticSignature())) {
				LOGGER.warn("Block {} is defined differently by {} and {}; using the one from {}", id, previous.source(), sourceName, sourceName);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Could not read block definition {} in {}", id, sourceName, e);
		}
	}

	private static List<Path> list(Path folder) {
		if (!Files.isDirectory(folder)) {
			return List.of();
		}

		try (Stream<Path> stream = Files.list(folder)) {
			return new ArrayList<>(stream.sorted(Comparator.comparing(Path::toString)).toList());
		} catch (IOException e) {
			LOGGER.error("Could not list {}", folder, e);
			return List.of();
		}
	}

	private static long lastModified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return 0L;
		}
	}

	private static String stripSlash(String name) {
		// Zip filesystem directory names end with '/'
		return name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
	}

	private record Found(BlockDefinition definition, String source) {
	}
}
