package dev.datablocks.block;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import dev.datablocks.DataBlocks;

/** Runs data pack functions for the {@code functions} component. */
final class BlockFunctions {
	private BlockFunctions() {
	}

	/**
	 * Runs {@code function} positioned at the block's centre, as {@code entity} when given.
	 *
	 * @param deferred queue for after the current tick work instead of running now; used from movement callbacks,
	 *                 where teleporting or editing blocks mid-move is unsafe
	 */
	static void run(ServerLevel level, BlockPos pos, Entity entity, Identifier function, boolean deferred) {
		MinecraftServer server = level.getServer();
		Runnable task = () -> {
			if (entity != null && entity.isRemoved()) {
				return;
			}

			CommandSourceStack source = server.createCommandSourceStack()
					.withLevel(level)
					.withPosition(Vec3.atCenterOf(pos))
					.withSuppressedOutput();

			if (entity != null) {
				source = source.withEntity(entity);
			}

			try {
				server.getCommands().performPrefixedCommand(source, "function " + function);
			} catch (RuntimeException e) {
				DataBlocks.LOGGER.error("Block function {} failed at {}", function, pos, e);
			}
		};

		if (deferred) {
			server.schedule(server.wrapRunnable(task));
		} else {
			task.run();
		}
	}
}
