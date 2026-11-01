package dev.datablocks.api;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

/**
 * A parsed block definition: an id plus a typed component map.
 *
 * <p>Definitions are immutable. {@link #toJson()} gives a canonical form (components sorted by id) that is
 * used for syncing to clients; {@link #staticSignature()} covers only the components that cannot change
 * without a restart and is what client and server compare when a player joins.
 */
public final class BlockDefinition {
	private final Identifier id;
	private final Map<BlockComponentType<?>, Object> components;
	private String json;
	private String signature;

	public BlockDefinition(Identifier id, Map<BlockComponentType<?>, Object> components) {
		this.id = id;
		this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
	}

	public Identifier id() {
		return id;
	}

	public Map<BlockComponentType<?>, Object> components() {
		return components;
	}

	public boolean has(BlockComponentType<?> type) {
		return components.containsKey(type);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(BlockComponentType<T> type) {
		return (T) components.get(type);
	}

	public <T> T getOrDefault(BlockComponentType<T> type, T fallback) {
		T value = get(type);
		return value != null ? value : fallback;
	}

	/** This definition's static components combined with {@code other}'s reloadable ones. */
	public BlockDefinition withReloadableFrom(BlockDefinition other) {
		Map<BlockComponentType<?>, Object> merged = new LinkedHashMap<>();

		components.forEach((type, value) -> {
			if (!type.reloadable()) merged.put(type, value);
		});
		other.components.forEach((type, value) -> {
			if (type.reloadable()) merged.put(type, value);
		});

		return new BlockDefinition(id, merged);
	}

	/** Canonical JSON: {@code {"components": {...}}} with keys sorted. */
	public String toJson() {
		if (json == null) {
			json = encode(false);
		}

		return json;
	}

	/** Canonical JSON of the static (registration-time) components only. */
	public String staticSignature() {
		if (signature == null) {
			signature = encode(true);
		}

		return signature;
	}

	private String encode(boolean staticOnly) {
		JsonObject encoded = new JsonObject();

		components.entrySet().stream()
				.filter(entry -> !staticOnly || !entry.getKey().reloadable())
				.sorted(Comparator.comparing(entry -> entry.getKey().id().toString()))
				.forEach(entry -> encoded.add(entry.getKey().id().toString(), entry.getKey().encodeUnchecked(entry.getValue())));

		JsonObject root = new JsonObject();
		root.add("components", encoded);
		return root.toString();
	}

	@Override
	public String toString() {
		return "BlockDefinition[" + id + "]";
	}
}
