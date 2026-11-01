package dev.datablocks.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;

import dev.datablocks.load.StartupLoader;

/**
 * Writes a server's block definitions as a pack under {@code datablocks/server_cache/<hash>/}, which
 * {@link StartupLoader} picks up on the next launch. The folder name is a hash of the contents, so joining the
 * same server again reuses it and different servers don't overwrite each other.
 */
final class ServerCache {
	private ServerCache() {
	}

	static Path save(Map<Identifier, String> definitions) throws IOException {
		Path folder = StartupLoader.serverCacheDir().resolve(hash(definitions));
		Path tmp = folder.resolveSibling(folder.getFileName() + ".tmp");
		deleteRecursively(tmp);
		Files.createDirectories(tmp);

		var gson = new GsonBuilder().setPrettyPrinting().create();

		for (Map.Entry<Identifier, String> entry : definitions.entrySet()) {
			Identifier id = entry.getKey();
			Path file = tmp.resolve("data").resolve(id.getNamespace()).resolve(StartupLoader.FOLDER).resolve(id.getPath() + ".json");
			Files.createDirectories(file.getParent());
			Files.writeString(file, gson.toJson(JsonParser.parseString(entry.getValue())), StandardCharsets.UTF_8);
		}

		Files.writeString(tmp.resolve("pack.mcmeta"),
				"{\"pack\":{\"description\":\"Block definitions downloaded by Data Blocks\",\"min_format\":94,\"max_format\":999}}",
				StandardCharsets.UTF_8);

		deleteRecursively(folder);
		Files.move(tmp, folder);
		return folder;
	}

	private static String hash(Map<Identifier, String> definitions) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			definitions.forEach((id, json) -> {
				digest.update(id.toString().getBytes(StandardCharsets.UTF_8));
				digest.update((byte) 0);
				digest.update(json.getBytes(StandardCharsets.UTF_8));
				digest.update((byte) 0);
			});

			return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}

		try (var walk = Files.walk(path)) {
			for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(p);
			}
		}
	}
}
