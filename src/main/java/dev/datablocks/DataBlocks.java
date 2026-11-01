package dev.datablocks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.datablocks.api.BlockComponents;
import dev.datablocks.api.DataBlocksPlugin;
import dev.datablocks.api.component.ItemSpec;
import dev.datablocks.block.DataBlockEntry;
import dev.datablocks.block.DataBlockRegistry;
import dev.datablocks.command.DataBlocksCommand;
import dev.datablocks.load.StartupLoader;
import dev.datablocks.network.ServerSync;
import dev.datablocks.server.ServerDefinitions;

public final class DataBlocks implements ModInitializer {
	public static final String MOD_ID = "datablocks";
	public static final Logger LOGGER = LoggerFactory.getLogger("Data Blocks");
	public static final ResourceKey<CreativeModeTab> CREATIVE_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("blocks"));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		BlockComponents.bootstrap();

		for (DataBlocksPlugin plugin : FabricLoader.getInstance().getEntrypoints(MOD_ID, DataBlocksPlugin.class)) {
			plugin.registerComponents();
		}

		StartupLoader.Result loaded = StartupLoader.load();
		DataBlockRegistry.registerAll(loaded.definitions(), loaded.global());
		LOGGER.info("Registered {} data block(s)", DataBlockRegistry.blocks().size());

		registerCreativeTabs();

		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ServerDefinitions.ID, new ServerDefinitions());
		ServerSync.init();
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerDefinitions.reset());
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> DataBlocksCommand.register(dispatcher));
	}

	private static void registerCreativeTabs() {
		List<DataBlockEntry> withItems = DataBlockRegistry.blocks().values().stream()
				.filter(entry -> entry.block().asItem() != Items.AIR)
				.toList();

		if (withItems.isEmpty()) {
			return;
		}

		Item icon = withItems.getFirst().block().asItem();

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.datablocks.blocks"))
				.icon(() -> new ItemStack(icon))
				.displayItems((parameters, output) -> {
					for (DataBlockEntry entry : withItems) {
						if (DataBlockRegistry.isActive(entry.id())) {
							output.accept(entry.block());
						}
					}
				})
				.build());

		// Extra tabs requested with "item": {"creative_tabs": [...]}.
		CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
			Identifier tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);

			if (tabId == null) {
				return;
			}

			for (DataBlockEntry entry : withItems) {
				ItemSpec spec = entry.registration().getOrDefault(BlockComponents.ITEM, ItemSpec.DEFAULT);

				if (spec.creativeTabs().contains(tabId) && DataBlockRegistry.isActive(entry.id())) {
					output.accept(entry.block());
				}
			}
		});
	}
}
