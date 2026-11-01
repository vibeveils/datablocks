package dev.datablocks.api.component;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

/** Small codec helpers shared by the built-in components. */
public final class DataCodecs {
	private DataCodecs() {
	}

	/** Raw JSON passthrough, used for nested vanilla item components that are resolved later with registry access. */
	public static final Codec<JsonElement> JSON = Codec.PASSTHROUGH.xmap(
			dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
			json -> new Dynamic<>(JsonOps.INSTANCE, json)
	);

	/** Accepts either a single value or a list of values; always encodes as a list. */
	public static <T> Codec<List<T>> singleOrList(Codec<T> codec) {
		return Codec.either(codec.listOf(), codec).xmap(
				either -> either.map(Function.identity(), List::of),
				Either::left
		);
	}

	/** A string that must satisfy {@code validator}, which returns an error message or {@code null}. */
	public static Codec<String> validatedString(Function<String, String> validator) {
		return Codec.STRING.validate(value -> {
			String error = validator.apply(value);
			return error == null ? DataResult.success(value) : DataResult.error(() -> error);
		});
	}
}
