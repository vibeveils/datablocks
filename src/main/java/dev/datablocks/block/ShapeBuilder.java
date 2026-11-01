package dev.datablocks.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.datablocks.api.component.Box;
import dev.datablocks.api.component.Placement;

/**
 * Builds one {@link VoxelShape} per block state from boxes authored in the default orientation:
 * facing north for {@code horizontal}/{@code facing}/{@code facing_surface}, and the Y axis for {@code axis}.
 */
final class ShapeBuilder {
	private ShapeBuilder() {
	}

	static Map<BlockState, VoxelShape> build(Block block, Placement placement, List<Box> boxes) {
		if (boxes == null) {
			return null;
		}

		Map<String, VoxelShape> byOrientation = new HashMap<>();
		Map<BlockState, VoxelShape> result = new HashMap<>();

		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			Orientation orientation = orientation(state, placement);
			result.put(state, byOrientation.computeIfAbsent(orientation.name(), key -> shape(boxes, orientation)));
		}

		return Map.copyOf(result);
	}

	private static Orientation orientation(BlockState state, Placement placement) {
		return switch (placement) {
			case NONE -> Orientation.IDENTITY;
			case AXIS -> switch (state.getValue(BlockStateProperties.AXIS)) {
				case X -> Orientation.AXIS_X;
				case Y -> Orientation.IDENTITY;
				case Z -> Orientation.AXIS_Z;
			};
			case HORIZONTAL -> fromDirection(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
			case FACING, FACING_SURFACE -> fromDirection(state.getValue(BlockStateProperties.FACING));
		};
	}

	private static Orientation fromDirection(Direction direction) {
		return switch (direction) {
			case NORTH -> Orientation.IDENTITY;
			case EAST -> Orientation.EAST;
			case SOUTH -> Orientation.SOUTH;
			case WEST -> Orientation.WEST;
			case UP -> Orientation.UP;
			case DOWN -> Orientation.DOWN;
		};
	}

	private static VoxelShape shape(List<Box> boxes, Orientation orientation) {
		VoxelShape shape = Shapes.empty();

		for (Box box : boxes) {
			float[] a = orientation.apply(box.x1(), box.y1(), box.z1());
			float[] b = orientation.apply(box.x2(), box.y2(), box.z2());
			shape = Shapes.or(shape, Block.box(
					Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
					Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])
			));
		}

		return shape.optimize();
	}

	/** Point transforms in pixel space (0..16), rotating about the block centre. */
	private enum Orientation {
		IDENTITY {
			@Override
			float[] apply(float x, float y, float z) {
				return new float[] {x, y, z};
			}
		},
		// Horizontal rotations about Y, from north.
		EAST {
			@Override
			float[] apply(float x, float y, float z) {
				return new float[] {16 - z, y, x};
			}
		},
		SOUTH {
			@Override
			float[] apply(float x, float y, float z) {
				return new float[] {16 - x, y, 16 - z};
			}
		},
		WEST {
			@Override
			float[] apply(float x, float y, float z) {
				return new float[] {z, y, 16 - x};
			}
		},
		// Rotations about X, from north: the north face ends up on top / bottom.
		UP {
			@Override
			float[] apply(float x, float y, float z) {
				return new float[] {x, 16 - z, y};
			}
		},
		DOWN {
			@Override
			float[] apply(float x, float y, float z) {
				return new float[] {x, z, 16 - y};
			}
		},
		// Pillar axes, from Y.
		AXIS_X {
			@Override
			float[] apply(float x, float y, float z) {
				return new float[] {y, 16 - x, z};
			}
		},
		AXIS_Z {
			@Override
			float[] apply(float x, float y, float z) {
				return new float[] {x, 16 - z, y};
			}
		};

		abstract float[] apply(float x, float y, float z);
	}
}
