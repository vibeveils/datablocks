package dev.datablocks.block;

import net.minecraft.world.level.block.SimpleWaterloggedBlock;

import dev.datablocks.api.BlockDefinition;

/**
 * A {@link DataBlock} with the {@code waterlogged} property. Kept separate because
 * {@link SimpleWaterloggedBlock}'s bucket logic assumes the property exists.
 */
public class WaterloggedDataBlock extends DataBlock implements SimpleWaterloggedBlock {
	protected WaterloggedDataBlock(Properties properties, BlockDefinition definition) {
		super(properties, definition);
	}
}
