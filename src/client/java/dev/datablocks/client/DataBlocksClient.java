package dev.datablocks.client;

import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.server.packs.PackType;

import dev.datablocks.api.BlockComponents;
import dev.datablocks.api.component.ParticleEmitter;
import dev.datablocks.block.DataBlock;
import dev.datablocks.block.DataBlockEntry;
import dev.datablocks.block.DataBlockRegistry;

public final class DataBlocksClient implements ClientModInitializer {
	/** Tint layers registered per block; a model's {@code tintindex} picks one. */
	static final int TINT_LAYERS = 4;

	@Override
	public void onInitializeClient() {
		ClientSync.init();
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(ClientDefinitions.ID, new ClientDefinitions());

		// Tint sources delegate to the block's current tint, so resource pack reloads and /reload
		// can change colours without re-registering anything.
		for (DataBlockEntry entry : DataBlockRegistry.blocks().values()) {
			BlockTintSource[] layers = new BlockTintSource[TINT_LAYERS];

			for (int i = 0; i < TINT_LAYERS; i++) {
				layers[i] = new DataTintSource(entry, i);
			}

			BlockColorRegistry.register(List.of(layers), entry.block());
		}

		// Resource-pack definitions can override ambient particles on this client.
		DataBlock.particleSource = block -> {
			List<ParticleEmitter> local = ClientDefinitions.particleOverride(block.registration().id());
			return local != null ? local : block.live().get(BlockComponents.AMBIENT_PARTICLES);
		};
	}
}
