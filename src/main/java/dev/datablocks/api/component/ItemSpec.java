package dev.datablocks.api.component;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * The block's item. {@code "item": false} registers no item. Otherwise:
 *
 * <pre>{@code
 * "item": {
 *   "creative_tabs": ["minecraft:building_blocks"],
 *   "components": {
 *     "max_stack_size": 16,
 *     "rarity": "rare",
 *     "lore": [{"text": "Polished"}]
 *   }
 * }
 * }</pre>
 *
 * {@code components} are ordinary vanilla item data components. They are resolved with full registry access on
 * every data pack load (through {@code Item.Properties#delayedComponent}), so components that reference data-pack
 * registries such as enchantments or damage types work too.
 */
public record ItemSpec(boolean enabled, List<Identifier> creativeTabs, Map<String, JsonElement> components) {
	public static final ItemSpec DEFAULT = new ItemSpec(true, List.of(), Map.of());

	private static final Codec<ItemSpec> FULL = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("enabled", true).forGetter(ItemSpec::enabled),
			DataCodecs.singleOrList(Identifier.CODEC).optionalFieldOf("creative_tabs", List.of()).forGetter(ItemSpec::creativeTabs),
			Codec.unboundedMap(Codec.STRING, DataCodecs.JSON)
					.<Map<String, JsonElement>>xmap(map -> new TreeMap<String, JsonElement>(map), map -> map)
					.optionalFieldOf("components", Map.of()).forGetter(ItemSpec::components)
	).apply(instance, ItemSpec::new));

	public static final Codec<ItemSpec> CODEC = Codec.either(Codec.BOOL, FULL).xmap(
			either -> either.map(enabled -> enabled ? DEFAULT : new ItemSpec(false, List.of(), Map.of()), spec -> spec),
			Either::right
	);
}
