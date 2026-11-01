package dev.datablocks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.datablocks.DataBlocks;

/** Client to server: "my block registry matches yours", which lets configuration continue. */
public final class AckPayload implements CustomPacketPayload {
	public static final AckPayload INSTANCE = new AckPayload();
	public static final Type<AckPayload> TYPE = new Type<>(DataBlocks.id("ack"));
	public static final StreamCodec<FriendlyByteBuf, AckPayload> CODEC = StreamCodec.unit(INSTANCE);

	private AckPayload() {
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
