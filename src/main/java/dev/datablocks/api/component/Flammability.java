package dev.datablocks.api.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Fire behaviour, same numbers as vanilla's {@code FireBlock#setFlammable}. Planks are
 * {@code {"ignite_odds": 5, "burn_odds": 20}}, wool is {@code 30 / 60}, leaves {@code 30 / 60}.
 */
public record Flammability(int igniteOdds, int burnOdds) {
	public static final Codec<Flammability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 300).fieldOf("ignite_odds").forGetter(Flammability::igniteOdds),
			Codec.intRange(0, 300).fieldOf("burn_odds").forGetter(Flammability::burnOdds)
	).apply(instance, Flammability::new));
}
