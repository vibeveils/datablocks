package dev.datablocks.network;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** Writes {@code id -> definition JSON} maps. Kept to basic buffer calls so it survives buffer API churn. */
final class DefinitionMapCodec {
	static final int MAX_JSON_LENGTH = 1 << 20;

	private DefinitionMapCodec() {
	}

	static void write(FriendlyByteBuf buf, Map<Identifier, String> map) {
		buf.writeVarInt(map.size());

		map.forEach((id, json) -> {
			buf.writeUtf(id.toString());
			buf.writeUtf(json, MAX_JSON_LENGTH);
		});
	}

	static Map<Identifier, String> read(FriendlyByteBuf buf) {
		int size = buf.readVarInt();
		Map<Identifier, String> map = new TreeMap<>();

		for (int i = 0; i < size; i++) {
			Identifier id = Identifier.parse(buf.readUtf());
			map.put(id, buf.readUtf(MAX_JSON_LENGTH));
		}

		return map;
	}
}
