package dev.datablocks.load;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;

import net.minecraft.resources.Identifier;

import dev.datablocks.api.BlockComponentType;
import dev.datablocks.api.BlockComponents;
import dev.datablocks.api.BlockDefinition;

/**
 * Parses block definition JSON.
 *
 * <pre>{@code
 * {
 *   "components": {
 *     "strength": 1.5,
 *     "sound": "stone",
 *     "mymod:custom_thing": { ... }
 *   }
 * }
 * }</pre>
 */
public final class DefinitionParser {
	private DefinitionParser() {
	}

	public static DataResult<BlockDefinition> parse(Identifier id, String json, boolean resourcePack) {
		try {
			return parse(id, JsonParser.parseString(json), resourcePack);
		} catch (RuntimeException e) {
			return DataResult.error(() -> "Malformed JSON: " + e.getMessage());
		}
	}

	/**
	 * @param resourcePack whether this is an {@code assets/} definition; only {@linkplain BlockComponentType#clientAllowed() client components} are allowed there
	 */
	public static DataResult<BlockDefinition> parse(Identifier id, JsonElement root, boolean resourcePack) {
		if (!root.isJsonObject()) {
			return DataResult.error(() -> "A block definition must be a JSON object");
		}

		JsonObject object = root.getAsJsonObject();
		List<String> errors = new ArrayList<>();
		Map<BlockComponentType<?>, Object> components = new LinkedHashMap<>();

		for (String key : object.keySet()) {
			if (!key.equals("components")) {
				errors.add("Unknown top-level field '" + key + "' (did you mean to put it inside \"components\"?)");
			}
		}

		JsonElement componentsJson = object.get("components");

		if (componentsJson != null) {
			if (!componentsJson.isJsonObject()) {
				errors.add("\"components\" must be an object");
			} else {
				for (Map.Entry<String, JsonElement> entry : componentsJson.getAsJsonObject().entrySet()) {
					Identifier typeId = BlockComponents.resolveKey(entry.getKey());
					BlockComponentType<?> type = typeId == null ? null : BlockComponents.get(typeId);

					if (type == null) {
						errors.add("Unknown component '" + entry.getKey() + "'");
						continue;
					}

					if (resourcePack && !type.clientAllowed()) {
						errors.add("Component '" + type.id() + "' belongs in a data pack, not a resource pack");
						continue;
					}

					DataResult<?> parsed = type.parse(entry.getValue());
					Optional<?> value = parsed.result();

					if (value.isPresent()) {
						components.put(type, value.get());
					} else {
						errors.add(type.id() + ": " + parsed.error().map(error -> error.message()).orElse("invalid value"));
					}
				}
			}
		}

		if (!errors.isEmpty()) {
			return DataResult.error(() -> String.join("; ", errors));
		}

		return DataResult.success(new BlockDefinition(id, components));
	}
}
