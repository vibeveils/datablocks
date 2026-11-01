package dev.datablocks.block;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import dev.datablocks.DataBlocks;
import dev.datablocks.api.BlockComponentType;
import dev.datablocks.api.BlockComponents;
import dev.datablocks.api.BlockDefinition;
import dev.datablocks.api.component.Box;
import dev.datablocks.api.component.ItemSpec;
import dev.datablocks.api.component.Kind;
import dev.datablocks.api.component.VanillaLookups;

/** Turns a {@link BlockDefinition} into a registered block (and item). */
public final class BlockFactory {
	private static final Logger LOGGER = DataBlocks.LOGGER;

	private BlockFactory() {
	}

	/** Registers the block and its item. Returns {@code null} if the id is already taken. */
	public static Block register(BlockDefinition definition) {
		Identifier id = definition.id();

		if (BuiltInRegistries.BLOCK.containsKey(id)) {
			LOGGER.error("Cannot register data block {}: a block with that id already exists", id);
			return null;
		}

		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		BlockBehaviour.Properties properties = createProperties(definition).setId(blockKey);
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, create(definition, properties));

		ItemSpec itemSpec = definition.getOrDefault(BlockComponents.ITEM, ItemSpec.DEFAULT);

		if (itemSpec.enabled()) {
			if (BuiltInRegistries.ITEM.containsKey(id)) {
				LOGGER.error("Data block {} has no item: an item with that id already exists", id);
			} else {
				ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
				Item.Properties itemProperties = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
				addItemComponents(id, itemProperties, itemSpec.components());
				Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProperties));
			}
		}

		return block;
	}

	/**
	 * Builds the block for its {@code kind}. Vanilla classes are instantiated as anonymous subclasses so their
	 * constructors are reachable even where they are {@code protected}.
	 */
	private static Block create(BlockDefinition definition, BlockBehaviour.Properties properties) {
		Kind kind = definition.getOrDefault(BlockComponents.KIND, Kind.DEFAULT);

		return switch (kind.type()) {
			case "slab" -> new SlabBlock(properties) { };
			case "stairs" -> new StairBlock(stairBase(definition, kind), properties) { };
			case "fence" -> new FenceBlock(properties) { };
			case "fence_gate" -> new FenceGateBlock(woodType(definition, kind), properties) { };
			case "wall" -> new WallBlock(properties) { };
			case "pane" -> new IronBarsBlock(properties) { };
			case "carpet" -> new CarpetBlock(properties) { };
			case "door" -> new DoorBlock(blockSet(definition, kind), properties) { };
			case "trapdoor" -> new TrapDoorBlock(blockSet(definition, kind), properties) { };
			case "button" -> new ButtonBlock(blockSet(definition, kind), kind.pressTicks(), properties) { };
			case "pressure_plate" -> new PressurePlateBlock(blockSet(definition, kind), properties) { };
			default -> DataBlock.create(properties, definition);
		};
	}

	private static BlockState stairBase(BlockDefinition definition, Kind kind) {
		Identifier base = kind.base().orElse(definition.get(BlockComponents.COPY_PROPERTIES));

		if (base != null && BuiltInRegistries.BLOCK.containsKey(base)) {
			return BuiltInRegistries.BLOCK.getValue(base).defaultBlockState();
		}

		if (base != null) {
			LOGGER.warn("Data block {}: stairs base {} does not exist; using stone", definition.id(), base);
		}

		return Blocks.STONE.defaultBlockState();
	}

	private static BlockSetType blockSet(BlockDefinition definition, Kind kind) {
		BlockSetType type = VanillaLookups.blockSetType(kind.blockSet());

		if (type == null) {
			LOGGER.warn("Data block {}: unknown block_set '{}'; using oak", definition.id(), kind.blockSet());
			return BlockSetType.OAK;
		}

		return type;
	}

	private static WoodType woodType(BlockDefinition definition, Kind kind) {
		WoodType type = VanillaLookups.woodType(kind.blockSet());

		if (type == null) {
			LOGGER.warn("Data block {}: unknown wood type '{}'; using oak", definition.id(), kind.blockSet());
			return WoodType.OAK;
		}

		return type;
	}

	private static BlockBehaviour.Properties createProperties(BlockDefinition definition) {
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
		Identifier copyFrom = definition.get(BlockComponents.COPY_PROPERTIES);

		if (copyFrom != null) {
			if (BuiltInRegistries.BLOCK.containsKey(copyFrom)) {
				properties = BlockBehaviour.Properties.ofFullCopy(BuiltInRegistries.BLOCK.getValue(copyFrom));
			} else {
				LOGGER.warn("Data block {}: copy_properties target {} does not exist (yet); ignoring", definition.id(), copyFrom);
			}
		}

		for (Map.Entry<BlockComponentType<?>, Object> entry : definition.components().entrySet()) {
			entry.getKey().applyUnchecked(properties, entry.getValue(), definition);
		}

		// Partial shapes should not hide the faces of neighbouring blocks unless the pack says otherwise.
		List<Box> shape = definition.get(BlockComponents.SHAPE);
		boolean partial = shape != null && !(shape.size() == 1 && shape.getFirst().isFullBlock());

		if (partial && !definition.has(BlockComponents.OCCLUDES)) {
			properties.noOcclusion();
		}

		return properties;
	}

	/**
	 * Vanilla item components are attached with {@code delayedComponent}, so they are decoded on each data pack
	 * load with full registry access, the same way vanilla items resolve theirs since 26.1.
	 */
	private static void addItemComponents(Identifier blockId, Item.Properties properties, Map<String, JsonElement> components) {
		for (Map.Entry<String, JsonElement> entry : components.entrySet()) {
			Identifier componentId = Identifier.tryParse(entry.getKey());
			DataComponentType<?> type = componentId == null ? null : BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(componentId);

			if (type == null) {
				LOGGER.error("Data block {}: unknown item component '{}'", blockId, entry.getKey());
				continue;
			}

			addDelayed(blockId, properties, type, entry.getValue());
		}
	}

	private static <T> void addDelayed(Identifier blockId, Item.Properties properties, DataComponentType<T> type, JsonElement json) {
		Codec<T> codec = type.codec();

		if (codec == null) {
			LOGGER.error("Data block {}: item component {} cannot be written in JSON", blockId, type);
			return;
		}

		properties.delayedComponent(type, registries -> codec
				.parse(registries.createSerializationContext(JsonOps.INSTANCE), json)
				.resultOrPartial(error -> LOGGER.error("Data block {}: invalid item component {}: {}", blockId, type, error))
				.orElse(null));
	}
}
