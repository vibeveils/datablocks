package dev.datablocks.network;

import java.util.List;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.players.NameAndId;

import dev.datablocks.DataBlocks;
import dev.datablocks.block.DataBlockRegistry;
import dev.datablocks.server.ServerDefinitions;

/**
 * Server half of the join handshake.
 *
 * <p>Fabric's registry sync disconnects a client that lacks any block the server has, with an error that
 * doesn't say why. This check runs first (in an earlier event phase) and sends the server's definitions, so a
 * client with this mod can download what it's missing and tell the player to restart, and a client without it
 * gets a clear message.
 */
public final class ServerSync {
	private static final int MAX_PAYLOAD = 32 * 1024 * 1024;
	private static final Identifier BEFORE_REGISTRY_SYNC = DataBlocks.id("before_registry_sync");

	private ServerSync() {
	}

	public static void init() {
		PayloadTypeRegistry.clientboundConfiguration().registerLarge(SyncPayload.TYPE, SyncPayload.CODEC, MAX_PAYLOAD);
		PayloadTypeRegistry.serverboundConfiguration().register(AckPayload.TYPE, AckPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().registerLarge(ActivePayload.TYPE, ActivePayload.CODEC, MAX_PAYLOAD);

		ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.addPhaseOrdering(BEFORE_REGISTRY_SYNC, Event.DEFAULT_PHASE);
		ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register(BEFORE_REGISTRY_SYNC, ServerSync::configure);

		ServerConfigurationNetworking.registerGlobalReceiver(AckPayload.TYPE, (payload, context) ->
				context.packetListener().completeTask(SyncTask.TYPE));

		// Fires for every player after a /reload (joined = false) and on join (joined = true).
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(ServerSync::onSyncDataPackContents);
	}

	private static void configure(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
		if (DataBlockRegistry.isEmpty()) {
			return;
		}

		if (!ServerConfigurationNetworking.canSend(handler, SyncPayload.TYPE)) {
			handler.disconnect(Component.literal("This server adds " + DataBlockRegistry.blocks().size()
					+ " custom blocks with the Data Blocks mod.\nInstall Data Blocks (and Fabric API) to join."));
			return;
		}

		handler.addTask(new SyncTask(new SyncPayload(ServerDefinitions.registeredJson(), ServerDefinitions.activeJson())));
	}

	private static void onSyncDataPackContents(ServerPlayer player, boolean joined) {
		if (!joined && ServerPlayNetworking.canSend(player, ActivePayload.TYPE)) {
			ServerPlayNetworking.send(player, new ActivePayload(ServerDefinitions.activeJson()));
		}

		List<String> restart = ServerDefinitions.needsRestart();
		MinecraftServer server = player.level().getServer();

		if (!restart.isEmpty() && server != null && server.isSingleplayerOwner(new NameAndId(player.getGameProfile()))) {
			player.sendSystemMessage(Component.literal("[Data Blocks] Restart the game to finish loading block definitions from this world's data packs: "
					+ String.join(", ", restart)));
		}
	}

	public record SyncTask(SyncPayload payload) implements ConfigurationTask {
		public static final Type TYPE = new Type(DataBlocks.MOD_ID + ":sync");

		@Override
		public void start(Consumer<Packet<?>> sender) {
			sender.accept(ServerConfigurationNetworking.createClientboundPacket(payload));
		}

		@Override
		public Type type() {
			return TYPE;
		}
	}
}
