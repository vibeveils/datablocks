package dev.datablocks.api.component;

import java.util.Locale;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * One tint layer. Normalised to a string: {@code grass}, {@code foliage}, {@code water}, {@code none},
 * or {@code #RRGGBB}. Integers are accepted and converted to {@code #RRGGBB}.
 */
public final class TintSpec {
	private TintSpec() {
	}

	public static final String GRASS = "grass";
	public static final String FOLIAGE = "foliage";
	public static final String WATER = "water";
	public static final String NONE = "none";

	private static final Codec<String> STRING = Codec.STRING.comapFlatMap(TintSpec::normalize, s -> s);

	public static final Codec<String> CODEC = Codec.either(Codec.INT, STRING).xmap(
			either -> either.map(TintSpec::fromInt, s -> s),
			Either::right
	);

	private static String fromInt(int rgb) {
		return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
	}

	private static DataResult<String> normalize(String raw) {
		String value = raw.toLowerCase(Locale.ROOT).trim();

		switch (value) {
			case GRASS, FOLIAGE, WATER, NONE -> {
				return DataResult.success(value);
			}
			default -> {
			}
		}

		if (value.matches("#[0-9a-f]{6}")) {
			return DataResult.success(value.toUpperCase(Locale.ROOT));
		}

		return DataResult.error(() -> "Unknown tint '" + raw + "'. Use grass, foliage, water, none or #RRGGBB");
	}

	/** Parses a {@code #RRGGBB} value to opaque ARGB. */
	public static int parseHex(String value) {
		return 0xFF000000 | Integer.parseInt(value.substring(1), 16);
	}
}
