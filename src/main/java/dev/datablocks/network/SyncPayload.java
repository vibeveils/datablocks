package dev.datablocks.network;

import java.util.Map;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import dev.datablocks.DataBlocks;

/**
 * Sent during configuration, before Fabric's registry sync.
 *
 * @param registered every data block the server registered at startup (canonical JSON); the client must have the same static components
 * @param active the definitions in the server's current data packs (canonical JSON); supplies reloadable values and creative visibility
 */
public record SyncPayload(Map<Identifier, String> registered, Map<Identifier, String> active) implements CustomPacketPayload {
	public static final Type<SyncPayload> TYPE = new Type<>(DataBlocks.id("sync"));
	public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC = StreamCodec.ofMember(SyncPayload::write, SyncPayload::read);

	private static SyncPayload read(FriendlyByteBuf buf) {
		return new SyncPayload(DefinitionMapCodec.read(buf), DefinitionMapCodec.read(buf));
	}

	private void write(FriendlyByteBuf buf) {
		DefinitionMapCodec.write(buf, registered);
		DefinitionMapCodec.write(buf, active);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
