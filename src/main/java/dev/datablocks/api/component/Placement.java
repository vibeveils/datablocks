package dev.datablocks.api.component;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** How the block orients itself when placed, and which block state property that adds. */
public enum Placement {
	/** No orientation property. */
	NONE,
	/** {@code axis} property, like logs and pillars: follows the clicked face's axis. */
	AXIS,
	/** {@code facing} (4 directions), like furnaces: front faces the player. */
	HORIZONTAL,
	/** {@code facing} (6 directions), like dispensers and observers: front faces the player. */
	FACING,
	/** {@code facing} (6 directions), like end rods and amethyst buds: points away from the clicked surface. */
	FACING_SURFACE;

	public static final Codec<Placement> CODEC = Codec.STRING.comapFlatMap(Placement::byName, Placement::serializedName);

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	private static DataResult<Placement> byName(String name) {
		String key = name.toLowerCase(Locale.ROOT);

		if (key.equals("horizontal_facing")) {
			return DataResult.success(HORIZONTAL);
		}

		for (Placement value : values()) {
			if (value.serializedName().equals(key)) {
				return DataResult.success(value);
			}
		}

		return DataResult.error(() -> "Unknown placement '" + name + "'. Expected one of " + Arrays.stream(values()).map(Placement::serializedName).toList());
	}
}
