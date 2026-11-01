package dev.datablocks.api.component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * Which block class to build. {@code block} (the default) is the fully data-driven block. The others reuse
 * vanilla's own classes, so slabs, stairs, fences, walls, doors and the rest behave exactly like vanilla ones:
 * connections, half/shape properties, opening, redstone. Write blockstates and models as you would for the
 * vanilla equivalent.
 *
 * <pre>{@code
 * "kind": "slab"
 * "kind": {"type": "stairs", "base": "mypack:marble"}
 * "kind": {"type": "door", "block_set": "oak"}
 * "kind": {"type": "button", "block_set": "stone", "press_ticks": 20}
 * }</pre>
 *
 * Behaviour components ({@code placement}, {@code shape}, {@code functions}, {@code gravity}...) only apply to
 * {@code block}; property components ({@code strength}, {@code sound}, {@code light_emission}...) apply to all.
 *
 * @param base the block stairs are made of (its state is used for stair particles/sounds); defaults to {@code copy_properties}, then stone
 * @param blockSet vanilla {@code BlockSetType}/{@code WoodType} name for doors, trapdoors, buttons, pressure plates and fence gates
 */
public record Kind(String type, Optional<Identifier> base, String blockSet, int pressTicks) {
	public static final String BLOCK = "block";
	public static final List<String> TYPES = List.of(BLOCK, "slab", "stairs", "fence", "fence_gate", "wall", "pane",
			"carpet", "door", "trapdoor", "button", "pressure_plate");

	public static final Kind DEFAULT = new Kind(BLOCK, Optional.empty(), "oak", 30);

	private static final Codec<String> TYPE = Codec.STRING.comapFlatMap(name -> {
		String key = name.toLowerCase(Locale.ROOT);
		return TYPES.contains(key) ? DataResult.success(key) : DataResult.error(() -> "Unknown kind '" + name + "'. Expected one of " + TYPES);
	}, name -> name);

	private static final Codec<Kind> FULL = RecordCodecBuilder.create(instance -> instance.group(
			TYPE.fieldOf("type").forGetter(Kind::type),
			Identifier.CODEC.optionalFieldOf("base").forGetter(Kind::base),
			Codec.STRING.optionalFieldOf("block_set", "oak").forGetter(Kind::blockSet),
			Codec.intRange(1, 1200).optionalFieldOf("press_ticks", 30).forGetter(Kind::pressTicks)
	).apply(instance, Kind::new));

	public static final Codec<Kind> CODEC = Codec.either(TYPE, FULL).xmap(
			either -> either.map(type -> new Kind(type, Optional.empty(), "oak", 30), kind -> kind),
			Either::right
	);

	public boolean isDataBlock() {
		return BLOCK.equals(type);
	}
}
