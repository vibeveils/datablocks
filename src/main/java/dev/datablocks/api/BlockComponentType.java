package dev.datablocks.api;

import java.util.Objects;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A typed, codec-backed piece of a block definition, modelled on vanilla item data components.
 *
 * <p>A component is either <b>static</b> (read once, when the block is registered at startup; changing it
 * requires a restart) or <b>reloadable</b> (re-read on every {@code /reload} and synced to clients live).
 *
 * <p>Components marked {@link #clientAllowed()} may also appear in resource-pack definitions
 * ({@code assets/<ns>/block_definition/<path>.json}), where they override the data-pack value on that client.
 */
public final class BlockComponentType<T> {
	private final Identifier id;
	private final Codec<T> codec;
	private final boolean reloadable;
	private final boolean clientAllowed;
	private final PropertiesApplier<T> applier;

	private BlockComponentType(Builder<T> builder) {
		this.id = Objects.requireNonNull(builder.id, "id");
		this.codec = Objects.requireNonNull(builder.codec, "codec");
		this.reloadable = builder.reloadable;
		this.clientAllowed = builder.clientAllowed;
		this.applier = builder.applier;
	}

	public static <T> Builder<T> builder(Identifier id, Codec<T> codec) {
		return new Builder<>(id, codec);
	}

	public Identifier id() {
		return id;
	}

	public Codec<T> codec() {
		return codec;
	}

	public boolean reloadable() {
		return reloadable;
	}

	public boolean clientAllowed() {
		return clientAllowed;
	}

	/** Applies this component to block properties at registration time, or {@code null} if it has no properties effect. */
	public PropertiesApplier<T> applier() {
		return applier;
	}

	public DataResult<T> parse(JsonElement json) {
		return codec.parse(JsonOps.INSTANCE, json);
	}

	@SuppressWarnings("unchecked")
	public JsonElement encodeUnchecked(Object value) {
		return codec.encodeStart(JsonOps.INSTANCE, (T) value).getOrThrow();
	}

	@SuppressWarnings("unchecked")
	public void applyUnchecked(BlockBehaviour.Properties properties, Object value, BlockDefinition definition) {
		if (applier != null) {
			applier.apply(properties, (T) value, definition);
		}
	}

	@Override
	public String toString() {
		return "BlockComponentType[" + id + "]";
	}

	@FunctionalInterface
	public interface PropertiesApplier<T> {
		void apply(BlockBehaviour.Properties properties, T value, BlockDefinition definition);
	}

	public static final class Builder<T> {
		private final Identifier id;
		private final Codec<T> codec;
		private boolean reloadable;
		private boolean clientAllowed;
		private PropertiesApplier<T> applier;

		private Builder(Identifier id, Codec<T> codec) {
			this.id = id;
			this.codec = codec;
		}

		/** Marks the component as re-read on {@code /reload} instead of requiring a restart. */
		public Builder<T> reloadable() {
			this.reloadable = true;
			return this;
		}

		/** Allows the component in resource-pack ({@code assets/}) definitions. Implies nothing about reloadability. */
		public Builder<T> clientAllowed() {
			this.clientAllowed = true;
			return this;
		}

		public Builder<T> properties(PropertiesApplier<T> applier) {
			this.applier = applier;
			return this;
		}

		public BlockComponentType<T> build() {
			return new BlockComponentType<>(this);
		}
	}
}
