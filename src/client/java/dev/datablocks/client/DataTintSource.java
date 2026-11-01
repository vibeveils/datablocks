package dev.datablocks.client;

import java.util.List;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import dev.datablocks.api.BlockComponents;
import dev.datablocks.api.component.TintSpec;
import dev.datablocks.block.DataBlockEntry;

/**
 * One tint layer of a data block. The colour comes from, in order: the resource pack definition
 * ({@code assets/<ns>/block_definition/}), then the data pack {@code tint} component.
 */
final class DataTintSource implements BlockTintSource {
	private static final int NO_TINT = -1;
	private static final int DEFAULT_GRASS = 0xFF91BD59;
	private static final int DEFAULT_FOLIAGE = 0xFF48B518;
	private static final int DEFAULT_WATER = 0xFF3F76E4;

	private final DataBlockEntry entry;
	private final int index;

	DataTintSource(DataBlockEntry entry, int index) {
		this.entry = entry;
		this.index = index;
	}

	private String spec() {
		List<String> tints = ClientDefinitions.tintOverride(entry.id());

		if (tints == null) {
			tints = entry.live().get(BlockComponents.TINT);
		}

		return tints != null && index < tints.size() ? tints.get(index) : null;
	}

	@Override
	public int color(BlockState state) {
		String spec = spec();

		if (spec == null) {
			return NO_TINT;
		}

		return switch (spec) {
			case TintSpec.GRASS -> DEFAULT_GRASS;
			case TintSpec.FOLIAGE -> DEFAULT_FOLIAGE;
			case TintSpec.WATER -> DEFAULT_WATER;
			case TintSpec.NONE -> NO_TINT;
			default -> TintSpec.parseHex(spec);
		};
	}

	@Override
	public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
		String spec = spec();

		if (spec == null) {
			return NO_TINT;
		}

		return switch (spec) {
			case TintSpec.GRASS -> 0xFF000000 | level.getBlockTint(pos, BiomeColors.GRASS_COLOR_RESOLVER);
			case TintSpec.FOLIAGE -> 0xFF000000 | level.getBlockTint(pos, BiomeColors.FOLIAGE_COLOR_RESOLVER);
			case TintSpec.WATER -> 0xFF000000 | level.getBlockTint(pos, BiomeColors.WATER_COLOR_RESOLVER);
			case TintSpec.NONE -> NO_TINT;
			default -> TintSpec.parseHex(spec);
		};
	}
}
