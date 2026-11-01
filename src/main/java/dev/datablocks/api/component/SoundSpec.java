package dev.datablocks.api.component;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/**
 * Block sounds. Either a vanilla sound type name ({@code "sound": "amethyst"}) or an object that starts from a
 * {@code base} type and overrides individual events:
 *
 * <pre>{@code
 * "sound": {
 *   "base": "stone",
 *   "volume": 1.0, "pitch": 1.2,
 *   "break": "minecraft:block.glass.break",
 *   "place": "mypack:block.marble.place"
 * }
 * }</pre>
 *
 * Custom sound event ids just need a matching {@code sounds.json} entry in a resource pack.
 */
public record SoundSpec(String base, Optional<Float> volume, Optional<Float> pitch,
		Optional<Identifier> breakSound, Optional<Identifier> stepSound, Optional<Identifier> placeSound,
		Optional<Identifier> hitSound, Optional<Identifier> fallSound) {
	private static final Codec<String> PRESET = DataCodecs.validatedString(name ->
			VanillaLookups.soundType(name) != null ? null : "Unknown sound type '" + name + "'. Known: " + VanillaLookups.soundTypes().keySet());

	private static final Codec<SoundSpec> FULL = RecordCodecBuilder.create(instance -> instance.group(
			PRESET.optionalFieldOf("base", "stone").forGetter(SoundSpec::base),
			Codec.FLOAT.optionalFieldOf("volume").forGetter(SoundSpec::volume),
			Codec.FLOAT.optionalFieldOf("pitch").forGetter(SoundSpec::pitch),
			Identifier.CODEC.optionalFieldOf("break").forGetter(SoundSpec::breakSound),
			Identifier.CODEC.optionalFieldOf("step").forGetter(SoundSpec::stepSound),
			Identifier.CODEC.optionalFieldOf("place").forGetter(SoundSpec::placeSound),
			Identifier.CODEC.optionalFieldOf("hit").forGetter(SoundSpec::hitSound),
			Identifier.CODEC.optionalFieldOf("fall").forGetter(SoundSpec::fallSound)
	).apply(instance, SoundSpec::new));

	public static final Codec<SoundSpec> CODEC = Codec.either(PRESET, FULL).xmap(
			either -> either.map(SoundSpec::preset, spec -> spec),
			spec -> spec.isPreset() ? Either.<String, SoundSpec>left(spec.base()) : Either.<String, SoundSpec>right(spec)
	);

	public static SoundSpec preset(String name) {
		return new SoundSpec(name, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty(), Optional.empty());
	}

	public boolean isPreset() {
		return volume.isEmpty() && pitch.isEmpty() && breakSound.isEmpty() && stepSound.isEmpty()
				&& placeSound.isEmpty() && hitSound.isEmpty() && fallSound.isEmpty();
	}

	public SoundType resolve() {
		SoundType base = VanillaLookups.soundType(this.base);

		if (isPreset()) {
			return base;
		}

		return new SoundType(
				volume.orElse(base.getVolume()),
				pitch.orElse(base.getPitch()),
				event(breakSound, base.getBreakSound()),
				event(stepSound, base.getStepSound()),
				event(placeSound, base.getPlaceSound()),
				event(hitSound, base.getHitSound()),
				event(fallSound, base.getFallSound())
		);
	}

	private static SoundEvent event(Optional<Identifier> id, SoundEvent fallback) {
		return id.map(SoundEvent::createVariableRangeEvent).orElse(fallback);
	}
}
