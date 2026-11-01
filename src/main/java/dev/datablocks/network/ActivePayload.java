package dev.datablocks.network;

import java.util.Map;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import dev.datablocks.DataBlocks;

/** Sent in play after every {@code /reload} with the definitions now in the server's data packs. */
public record ActivePayload(Map<Identifier, String> active) implements CustomPacketPayload {
	public static final Type<ActivePayload> TYPE = new Type<>(DataBlocks.id("active"));
	public static final StreamCodec<FriendlyByteBuf, ActivePayload> CODEC = StreamCodec.ofMember(ActivePayload::write, ActivePayload::read);

	private static ActivePayload read(FriendlyByteBuf buf) {
		return new ActivePayload(DefinitionMapCodec.read(buf));
	}

	private void write(FriendlyByteBuf buf) {
		DefinitionMapCodec.write(buf, active);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
