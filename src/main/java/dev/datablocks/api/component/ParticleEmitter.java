package dev.datablocks.api.component;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Client-side ambient particles, like the smoke above a torch. Each entry rolls {@code chance} every animation
 * tick (roughly 1/3 of displayed blocks get one per frame-tick) and spawns {@code count} particles.
 *
 * <pre>{@code
 * "ambient_particles": [
 *   {"particle": "minecraft:glow", "chance": 0.2},
 *   {"particle": {"type": "minecraft:dust", "color": [0.4, 1.0, 0.8], "scale": 1.0},
 *    "chance": 0.5, "count": 2, "offset": [0, 0.6, 0], "spread": [0.4, 0.2, 0.4], "velocity": [0, 0.02, 0]}
 * ]
 * }</pre>
 *
 * {@code particle} is a particle type id, or a full particle object in the same format as the {@code /particle}
 * command's JSON form. {@code offset} is from the block's centre; {@code spread} is a random +/- range per axis.
 */
public record ParticleEmitter(JsonElement particle, float chance, int count, List<Float> offset, List<Float> spread, List<Float> velocity) {
	private static final Codec<List<Float>> VEC3 = Codec.FLOAT.listOf(3, 3);

	public static final Codec<ParticleEmitter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			DataCodecs.JSON.fieldOf("particle").forGetter(ParticleEmitter::particle),
			Codec.floatRange(0f, 1f).optionalFieldOf("chance", 0.1f).forGetter(ParticleEmitter::chance),
			Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ParticleEmitter::count),
			VEC3.optionalFieldOf("offset", List.of(0f, 0f, 0f)).forGetter(ParticleEmitter::offset),
			VEC3.optionalFieldOf("spread", List.of(0.5f, 0.5f, 0.5f)).forGetter(ParticleEmitter::spread),
			VEC3.optionalFieldOf("velocity", List.of(0f, 0f, 0f)).forGetter(ParticleEmitter::velocity)
	).apply(instance, ParticleEmitter::new));

	public static final Codec<List<ParticleEmitter>> LIST_CODEC = DataCodecs.singleOrList(CODEC);

	/** The particle as an object with a {@code type}, expanding the bare-id shorthand. */
	public JsonElement particleObject() {
		if (particle.isJsonPrimitive()) {
			JsonObject object = new JsonObject();
			object.addProperty("type", particle.getAsString());
			return object;
		}

		return particle;
	}
}
