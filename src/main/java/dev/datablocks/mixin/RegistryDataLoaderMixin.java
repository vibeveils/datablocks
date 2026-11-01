package dev.datablocks.mixin;

import java.util.List;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.ResourceManager;

import dev.datablocks.load.PendingBlocks;

/**
 * Runs before a data pack set's registries (worldgen, recipes...) are loaded and validated. That is the last point
 * where unregistered data blocks can be noticed: once validation fails, the pack never gets anywhere the next
 * launch would look. Same target method Fabric API's own registry sync hooks in 26.3.
 */
@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
	@Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
			at = @At("HEAD"))
	private static void datablocks$catchNewBlocks(ResourceManager resourceManager, List<?> contextRegistries, List<?> registriesToLoad,
			Executor executor, CallbackInfoReturnable<?> cir) {
		PendingBlocks.check(resourceManager);
	}
}
