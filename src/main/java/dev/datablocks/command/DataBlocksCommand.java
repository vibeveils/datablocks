package dev.datablocks.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import dev.datablocks.block.DataBlockRegistry;
import dev.datablocks.load.PendingBlocks;
import dev.datablocks.server.ServerDefinitions;

/** {@code /datablocks} — read-only status: what is registered, what the current data packs use, what needs a restart. */
public final class DataBlocksCommand {
	private DataBlocksCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("datablocks")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					int registered = DataBlockRegistry.blocks().size();
					int active = ServerDefinitions.active().size();
					List<String> restart = ServerDefinitions.needsRestart();

					source.sendSuccess(() -> Component.literal("Data Blocks: " + registered + " registered, " + active
							+ " in this world" + (restart.isEmpty() ? "." : ", " + restart.size() + " waiting for a restart.")), false);
					return registered;
				})
				.then(Commands.literal("list").executes(context -> {
					CommandSourceStack source = context.getSource();

					if (DataBlockRegistry.isEmpty()) {
						source.sendSuccess(() -> Component.literal("No data blocks are registered."), false);
						return 0;
					}

					DataBlockRegistry.blocks().forEach((id, block) -> {
						boolean inPacks = ServerDefinitions.active().containsKey(id);
						source.sendSuccess(() -> Component.literal((inPacks ? " + " : " - ") + id + (inPacks ? "" : "  (registered, but not in this world's data packs)")), false);
					});

					return DataBlockRegistry.blocks().size();
				}))
				.then(Commands.literal("pending").executes(context -> {
					CommandSourceStack source = context.getSource();
					List<String> restart = ServerDefinitions.needsRestart();

					List<Identifier> installed = PendingBlocks.lastInstalled();

					if (restart.isEmpty() && installed.isEmpty()) {
						source.sendSuccess(() -> Component.literal("Nothing is waiting for a restart."), false);
					} else {
						restart.forEach(line -> source.sendSuccess(() -> Component.literal(" * " + line), false));
						installed.forEach(id -> source.sendSuccess(() -> Component.literal(" * " + id + " installed, registers on restart"), false));
					}

					return restart.size() + installed.size();
				})));
	}
}
