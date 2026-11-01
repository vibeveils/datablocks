package dev.datablocks.api.component;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Mining time and blast resistance. Accepts either a single number ({@code "strength": 1.5}, used for both)
 * or {@code {"hardness": 1.5, "resistance": 6}}. A hardness of {@code -1} makes the block unbreakable (and blast-proof unless a resistance is given).
 */
public record Strength(float hardness, float resistance) {
	private static final Codec<Strength> FULL = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("hardness").forGetter(Strength::hardness),
			Codec.FLOAT.optionalFieldOf("resistance").forGetter(s -> Optional.of(s.resistance()))
	).apply(instance, (hardness, resistance) -> new Strength(hardness, resistance.orElse(defaultResistance(hardness)))));

	public static final Codec<Strength> CODEC = Codec.either(Codec.FLOAT, FULL).xmap(
			either -> either.map(value -> new Strength(value, defaultResistance(value)), strength -> strength),
			Either::right
	);

	/** Unbreakable blocks (hardness -1) also get bedrock's blast resistance. */
	private static float defaultResistance(float hardness) {
		return hardness < 0 ? 3_600_000f : hardness;
	}
}
