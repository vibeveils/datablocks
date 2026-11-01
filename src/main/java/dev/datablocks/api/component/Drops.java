package dev.datablocks.api.component;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** What the block drops when broken. */
public enum Drops {
	/** Drops its own item (the default, so a pack works without writing a loot table). */
	SELF,
	/** Drops nothing. */
	NONE,
	/** Uses the vanilla loot table at {@code data/<ns>/loot_table/blocks/<path>.json}. */
	LOOT_TABLE;

	public static final Codec<Drops> CODEC = Codec.STRING.comapFlatMap(name -> {
		for (Drops value : values()) {
			if (value.name().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) {
				return DataResult.success(value);
			}
		}

		return DataResult.error(() -> "Unknown drops mode '" + name + "'. Expected self, none or loot_table");
	}, value -> value.name().toLowerCase(Locale.ROOT));
}
