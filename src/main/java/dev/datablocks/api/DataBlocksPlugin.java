package dev.datablocks.api;

/**
 * Entrypoint for mods that add their own block component types.
 *
 * <p>Declare it in {@code fabric.mod.json}:
 * <pre>{@code
 * "entrypoints": { "datablocks": ["com.example.MyDataBlocksPlugin"] }
 * }</pre>
 *
 * It runs before any block definitions are read, regardless of mod load order.
 */
@FunctionalInterface
public interface DataBlocksPlugin {
	void registerComponents();
}
