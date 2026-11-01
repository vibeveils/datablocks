package dev.datablocks.api.component;

import java.util.List;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

/**
 * An axis-aligned box in pixel coordinates (0-16 is one block), written like a model element:
 * {@code [x1, y1, z1, x2, y2, z2]}. Values from -16 to 32 are allowed for shapes that poke out of the block.
 */
public record Box(float x1, float y1, float z1, float x2, float y2, float z2) {
	public static final Codec<Box> CODEC = Codec.FLOAT.listOf().comapFlatMap(Box::fromList, Box::toList);

	/** One box, or a list of boxes. Dispatches on the JSON shape so error messages point at the real problem. */
	public static final Codec<List<Box>> SHAPE_CODEC = DataCodecs.JSON.comapFlatMap(Box::parseShape,
			boxes -> CODEC.listOf().encodeStart(JsonOps.INSTANCE, boxes).getOrThrow());

	private static DataResult<List<Box>> parseShape(JsonElement json) {
		boolean nested = json.isJsonArray() && !json.getAsJsonArray().isEmpty() && json.getAsJsonArray().get(0).isJsonArray();
		DataResult<List<Box>> result = nested
				? CODEC.listOf().parse(JsonOps.INSTANCE, json)
				: CODEC.parse(JsonOps.INSTANCE, json).map(List::of);
		return result.flatMap(boxes -> boxes.isEmpty() ? DataResult.error(() -> "A shape needs at least one box") : DataResult.success(boxes));
	}

	public static final Box FULL = new Box(0, 0, 0, 16, 16, 16);

	private static DataResult<Box> fromList(List<Float> values) {
		if (values.size() != 6) {
			return DataResult.error(() -> "A box needs exactly 6 numbers [x1, y1, z1, x2, y2, z2], got " + values);
		}

		for (float value : values) {
			if (value < -16f || value > 32f) {
				return DataResult.error(() -> "Box coordinates must be between -16 and 32, got " + values);
			}
		}

		return DataResult.success(new Box(
				Math.min(values.get(0), values.get(3)), Math.min(values.get(1), values.get(4)), Math.min(values.get(2), values.get(5)),
				Math.max(values.get(0), values.get(3)), Math.max(values.get(1), values.get(4)), Math.max(values.get(2), values.get(5))
		));
	}

	private List<Float> toList() {
		return List.of(x1, y1, z1, x2, y2, z2);
	}

	public boolean isFullBlock() {
		return this.equals(FULL);
	}
}
