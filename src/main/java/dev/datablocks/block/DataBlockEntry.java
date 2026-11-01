package dev.datablocks.block;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import dev.datablocks.api.BlockDefinition;

/**
 * A registered data block: the game {@link Block} (a {@link DataBlock}, or a vanilla class such as a slab when the
 * {@code kind} component asks for one), the definition it was registered with, and its current live definition.
 */
public final class DataBlockEntry {
	private final Identifier id;
	private final Block block;
	private final BlockDefinition registration;
	private volatile BlockDefinition live;

	DataBlockEntry(Identifier id, Block block, BlockDefinition registration) {
		this.id = id;
		this.block = block;
		this.registration = registration;
		this.live = registration;
	}

	public Identifier id() {
		return id;
	}

	public Block block() {
		return block;
	}

	public BlockDefinition registration() {
		return registration;
	}

	public BlockDefinition live() {
		return live;
	}

	void setLive(BlockDefinition definition) {
		this.live = definition;

		if (block instanceof DataBlock dataBlock) {
			dataBlock.setLive(definition);
		}
	}
}
