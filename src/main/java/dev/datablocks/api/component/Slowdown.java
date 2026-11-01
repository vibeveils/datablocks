package dev.datablocks.api.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Entities inside the block move as if stuck, like cobwebs ({@code 0.25 / 0.05}), sweet berry bushes
 * ({@code 0.8 / 0.75}) or powder snow ({@code 0.9 / 1.5}). Usually paired with {@code "collision": false}.
 */
public record Slowdown(float horizontal, float vertical) {
	public static final Codec<Slowdown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0f, 4f).fieldOf("horizontal").forGetter(Slowdown::horizontal),
			Codec.floatRange(0f, 4f).fieldOf("vertical").forGetter(Slowdown::vertical)
	).apply(instance, Slowdown::new));
}
