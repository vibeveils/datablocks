package dev.datablocks.api.component;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * Data pack functions to run when something happens to the block. Each runs with full permissions, positioned at
 * the block's centre in the block's dimension, and {@code as} the entity involved (where there is one), so
 * {@code @s} and {@code ~ ~ ~} work as you'd expect.
 *
 * <pre>{@code
 * "functions": {
 *   "use": "mypack:block/ring_bell",          // right-clicked (as the player); consumes the click
 *   "step_on": "mypack:block/squish",         // walked on (as the entity)
 *   "entity_inside": "mypack:block/portal",   // entity overlaps the block, every tick (see entity_inside_interval)
 *   "entity_inside_interval": 10,             // only every N game ticks, default 1
 *   "placed": "mypack:block/placed",          // placed by an entity (as the placer)
 *   "broken": "mypack:block/broken",          // about to be broken by a player (as the player)
 *   "random_tick": "mypack:block/grow"        // random ticks (no entity), e.g. for growing
 * }
 * }</pre>
 *
 * The function ids themselves are reloadable, but whether the block receives random ticks at all is decided at
 * startup, so add {@code random_tick} before the first launch.
 */
public record FunctionHooks(Optional<Identifier> use, Optional<Identifier> stepOn, Optional<Identifier> entityInside,
		int entityInsideInterval, Optional<Identifier> placed, Optional<Identifier> broken, Optional<Identifier> randomTick) {
	public static final Codec<FunctionHooks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.optionalFieldOf("use").forGetter(FunctionHooks::use),
			Identifier.CODEC.optionalFieldOf("step_on").forGetter(FunctionHooks::stepOn),
			Identifier.CODEC.optionalFieldOf("entity_inside").forGetter(FunctionHooks::entityInside),
			Codec.intRange(1, 72000).optionalFieldOf("entity_inside_interval", 1).forGetter(FunctionHooks::entityInsideInterval),
			Identifier.CODEC.optionalFieldOf("placed").forGetter(FunctionHooks::placed),
			Identifier.CODEC.optionalFieldOf("broken").forGetter(FunctionHooks::broken),
			Identifier.CODEC.optionalFieldOf("random_tick").forGetter(FunctionHooks::randomTick)
	).apply(instance, FunctionHooks::new));
}
