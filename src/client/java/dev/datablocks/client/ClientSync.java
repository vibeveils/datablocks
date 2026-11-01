package dev.datablocks.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.mojang.serialization.DataResult;
import org.slf4j.Logger;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import dev.datablocks.DataBlocks;
import dev.datablocks.api.BlockDefinition;
import dev.datablocks.block.DataBlockEntry;
import dev.datablocks.block.DataBlockRegistry;
import dev.datablocks.load.DefinitionParser;
import dev.datablocks.network.AckPayload;
import dev.datablocks.network.ActivePayload;
import dev.datablocks.network.SyncPayload;

/**
 * Client half of the join handshake. If the server has blocks this client didn't register at startup (or
 * registered differently), their definitions are saved to {@code datablocks/server_cache/} and the player is
 * asked to restart; the next launch registers them and the join succeeds.
 */
final class ClientSync {
	private static final Logger LOGGER = DataBlocks.LOGGER;
	private static final int MAX_LISTED = 5;

	private ClientSync() {
	}

	static void init() {
		ClientConfigurationNetworking.registerGlobalReceiver(SyncPayload.TYPE, ClientSync::onSync);
		ClientPlayNetworking.registerGlobalReceiver(ActivePayload.TYPE, (payload, context) -> {
			Map<Identifier, BlockDefinition> active = parseAll(payload.active(), null);
			context.client().execute(() -> applyActive(context.client(), active));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(ClientSync::reset));
	}

	private static void onSync(SyncPayload payload, ClientConfigurationNetworking.Context context) {
		List<String> errors = new ArrayList<>();
		Map<Identifier, BlockDefinition> registered = parseAll(payload.registered(), errors);

		if (!errors.isEmpty()) {
			context.responseSender().disconnect(Component.literal("[Data Blocks] This server uses block components your version of Data Blocks doesn't understand."
					+ "\nUpdate the mod to match the server.\n\n" + String.join("\n", errors.subList(0, Math.min(errors.size(), MAX_LISTED)))));
			return;
		}

		// The integrated server shares this game's registry, so there is nothing to compare in singleplayer.
		if (!context.client().isLocalServer()) {
			List<Identifier> missing = new ArrayList<>();
			List<Identifier> changed = new ArrayList<>();

			registered.forEach((id, definition) -> {
				DataBlockEntry local = DataBlockRegistry.get(id);

				if (local == null) {
					missing.add(id);
				} else if (!local.registration().staticSignature().equals(definition.staticSignature())) {
					changed.add(id);
				}
			});

			if (!missing.isEmpty() || !changed.isEmpty()) {
				context.responseSender().disconnect(downloadAndExplain(payload, missing, changed));
				return;
			}
		}

		Map<Identifier, BlockDefinition> active = parseAll(payload.active(), null);
		context.client().execute(() -> applyActive(context.client(), active));
		context.responseSender().sendPacket(AckPayload.INSTANCE);
	}

	private static Component downloadAndExplain(SyncPayload payload, List<Identifier> missing, List<Identifier> changed) {
		StringBuilder message = new StringBuilder("[Data Blocks] This server has custom blocks that aren't loaded in your game yet.\n\n");

		if (!missing.isEmpty()) {
			message.append(missing.size()).append(" new: ").append(summarise(missing)).append('\n');
		}

		if (!changed.isEmpty()) {
			message.append(changed.size()).append(" different from your copy: ").append(summarise(changed)).append('\n');
		}

		try {
			Path folder = ServerCache.save(payload.registered());
			message.append("\nTheir definitions were downloaded to ").append(folder.getFileName())
					.append(".\nRestart Minecraft, then join again.");
		} catch (Exception e) {
			LOGGER.error("Could not save server block definitions", e);
			message.append("\nThey could not be downloaded automatically (").append(e.getMessage()).append(").")
					.append("\nAsk the server for its block pack and put it in .minecraft/datablocks/packs/.");
		}

		return Component.literal(message.toString());
	}

	private static String summarise(List<Identifier> ids) {
		List<String> shown = ids.stream().limit(MAX_LISTED).map(Identifier::toString).toList();
		return String.join(", ", shown) + (ids.size() > MAX_LISTED ? " and " + (ids.size() - MAX_LISTED) + " more" : "");
	}

	private static void applyActive(Minecraft client, Map<Identifier, BlockDefinition> active) {
		if (client.isLocalServer()) {
			// The integrated server already applied these to the shared block instances.
			DataBlockRegistry.setActiveIds(active.keySet());
		} else {
			DataBlockRegistry.applyLive(active);
		}
	}

	private static void reset() {
		DataBlockRegistry.resetLive();
	}

	private static Map<Identifier, BlockDefinition> parseAll(Map<Identifier, String> json, List<String> errors) {
		Map<Identifier, BlockDefinition> result = new TreeMap<>();

		json.forEach((id, text) -> {
			DataResult<BlockDefinition> parsed = DefinitionParser.parse(id, text, false);
			Optional<BlockDefinition> definition = parsed.result();

			if (definition.isPresent()) {
				result.put(id, definition.get());
			} else {
				String error = id + ": " + parsed.error().map(e -> e.message()).orElse("?");

				if (errors != null) {
					errors.add(error);
				} else {
					LOGGER.warn("Ignoring server block definition {}", error);
				}
			}
		});

		return result;
	}
}
