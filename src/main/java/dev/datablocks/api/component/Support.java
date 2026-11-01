package dev.datablocks.api.component;

import java.util.List;
import java.util.Locale;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What the block must be attached to, like a flower on dirt or a vine under a ceiling. Without valid support the
 * block can't be placed (by players or world generation features) and pops off when its support goes.
 *
 * <pre>{@code
 * "survives_on": "#minecraft:dirt"
 * "survives_on": ["#mypack:soils", "minecraft:stone", "sturdy"]
 * "survives_on": {"blocks": ["#mypack:ceilings"], "direction": "up"}
 * }</pre>
 *
 * Entries are block ids, {@code #tags}, or {@code sturdy} (any block with a full face toward this one).
 * {@code direction} is where the support is: {@code down} (default) or {@code up} for hanging blocks.
 */
public record Support(List<String> blocks, String direction) {
	private static final Codec<String> DIRECTION = Codec.STRING.comapFlatMap(name -> {
		String key = name.toLowerCase(Locale.ROOT);
		return key.equals("down") || key.equals("up") ? DataResult.success(key) : DataResult.error(() -> "direction must be down or up");
	}, name -> name);

	private static final Codec<String> ENTRY = DataCodecs.validatedString(entry ->
			entry.equals("sturdy") || entry.matches("#?[a-z0-9_.-]+(:[a-z0-9_./-]+)?") ? null : "Invalid support entry '" + entry + "'");

	private static final Codec<Support> FULL = RecordCodecBuilder.create(instance -> instance.group(
			DataCodecs.singleOrList(ENTRY).fieldOf("blocks").forGetter(Support::blocks),
			DIRECTION.optionalFieldOf("direction", "down").forGetter(Support::direction)
	).apply(instance, Support::new));

	public static final Codec<Support> CODEC = Codec.either(DataCodecs.singleOrList(ENTRY), FULL).xmap(
			either -> either.map(blocks -> new Support(blocks, "down"), support -> support),
			Either::right
	);

	public boolean hanging() {
		return direction.equals("up");
	}
}
