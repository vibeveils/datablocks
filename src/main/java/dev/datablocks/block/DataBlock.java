package dev.datablocks.block;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import dev.datablocks.DataBlocks;
import dev.datablocks.api.component.FunctionHooks;
import dev.datablocks.api.component.ParticleEmitter;
import dev.datablocks.api.component.Slowdown;
import dev.datablocks.api.component.Support;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.datablocks.api.BlockComponents;
import dev.datablocks.api.BlockDefinition;
import dev.datablocks.api.component.Drops;
import dev.datablocks.api.component.Placement;

/**
 * The single block class behind every data-defined block. Its behaviour is driven entirely by its
 * {@link BlockDefinition}: static components are captured at construction, reloadable ones are read from
 * {@link #live()} each time they are needed.
 */
public class DataBlock extends Block {
	/**
	 * {@link Block}'s constructor calls {@link #createBlockStateDefinition} before subclass fields exist, so the
	 * definition being constructed is handed over through this thread-local.
	 */
	private static final ThreadLocal<BlockDefinition> CONSTRUCTING = new ThreadLocal<>();

	private final BlockDefinition registration;
	private volatile BlockDefinition live;

	private final Placement placement;
	private final boolean waterloggable;
	private final boolean gravity;
	private final int redstonePower;
	private final Drops drops;
	private final Map<BlockState, VoxelShape> outlineShapes;
	private final Map<BlockState, VoxelShape> collisionShapes;
	private final Support support;
	private volatile ParticleCache particleCache;

	/**
	 * Where {@code animateTick} gets its particles from. The client entrypoint swaps this for one that prefers
	 * resource-pack definitions.
	 */
	public static volatile Function<DataBlock, List<ParticleEmitter>> particleSource = block -> block.live().get(BlockComponents.AMBIENT_PARTICLES);

	static DataBlock create(Properties properties, BlockDefinition definition) {
		CONSTRUCTING.set(definition);

		try {
			return definition.getOrDefault(BlockComponents.WATERLOGGABLE, false)
					? new WaterloggedDataBlock(properties, definition)
					: new DataBlock(properties, definition);
		} finally {
			CONSTRUCTING.remove();
		}
	}

	protected DataBlock(Properties properties, BlockDefinition definition) {
		super(properties);
		this.registration = definition;
		this.live = definition;
		this.placement = definition.getOrDefault(BlockComponents.PLACEMENT, Placement.NONE);
		this.waterloggable = definition.getOrDefault(BlockComponents.WATERLOGGABLE, false);
		this.gravity = definition.getOrDefault(BlockComponents.GRAVITY, false);
		this.redstonePower = definition.getOrDefault(BlockComponents.REDSTONE_POWER, 0);
		this.drops = definition.getOrDefault(BlockComponents.DROPS, Drops.SELF);
		this.support = definition.get(BlockComponents.SURVIVES_ON);

		BlockState defaultState = this.getStateDefinition().any();

		switch (placement) {
			case AXIS -> defaultState = defaultState.setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
			case HORIZONTAL -> defaultState = defaultState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
			case FACING, FACING_SURFACE -> defaultState = defaultState.setValue(BlockStateProperties.FACING, Direction.NORTH);
			case NONE -> {
			}
		}

		if (waterloggable) {
			defaultState = defaultState.setValue(BlockStateProperties.WATERLOGGED, false);
		}

		this.registerDefaultState(defaultState);
		this.outlineShapes = ShapeBuilder.build(this, placement, definition.get(BlockComponents.SHAPE));
		this.collisionShapes = ShapeBuilder.build(this, placement, definition.get(BlockComponents.COLLISION_SHAPE));
	}

	/** The definition this block was registered with. */
	public BlockDefinition registration() {
		return registration;
	}

	/** The current definition: static components from registration, reloadable ones from the active data packs. */
	public BlockDefinition live() {
		return live;
	}

	public void setLive(BlockDefinition definition) {
		this.live = definition;
	}

	// ------------------------------------------------------------------ state

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		BlockDefinition definition = CONSTRUCTING.get();

		if (definition == null) {
			throw new IllegalStateException("DataBlock must be created through BlockFactory");
		}

		switch (definition.getOrDefault(BlockComponents.PLACEMENT, Placement.NONE)) {
			case AXIS -> builder.add(BlockStateProperties.AXIS);
			case HORIZONTAL -> builder.add(BlockStateProperties.HORIZONTAL_FACING);
			case FACING, FACING_SURFACE -> builder.add(BlockStateProperties.FACING);
			case NONE -> {
			}
		}

		if (definition.getOrDefault(BlockComponents.WATERLOGGABLE, false)) {
			builder.add(BlockStateProperties.WATERLOGGED);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState();

		state = switch (placement) {
			case AXIS -> state.setValue(BlockStateProperties.AXIS, context.getClickedFace().getAxis());
			case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
			case FACING -> state.setValue(BlockStateProperties.FACING, context.getNearestLookingDirection().getOpposite());
			case FACING_SURFACE -> state.setValue(BlockStateProperties.FACING, context.getClickedFace());
			case NONE -> state;
		};

		if (waterloggable) {
			boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
			state = state.setValue(BlockStateProperties.WATERLOGGED, water);
		}

		return state;
	}

	// ------------------------------------------------------------------ support

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (support == null) {
			return super.canSurvive(state, level, pos);
		}

		Direction toSupport = support.hanging() ? Direction.UP : Direction.DOWN;
		BlockPos supportPos = pos.relative(toSupport);
		BlockState supportState = level.getBlockState(supportPos);

		for (String entry : support.blocks()) {
			if (entry.equals("sturdy")) {
				if (supportState.isFaceSturdy(level, supportPos, toSupport.getOpposite())) {
					return true;
				}
			} else if (entry.startsWith("#")) {
				Identifier tag = Identifier.tryParse(entry.substring(1));

				if (tag != null && supportState.is(TagKey.create(Registries.BLOCK, tag))) {
					return true;
				}
			} else {
				Identifier id = Identifier.tryParse(entry);

				if (id != null && BuiltInRegistries.BLOCK.containsKey(id) && supportState.is(BuiltInRegistries.BLOCK.getValue(id))) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return switch (placement) {
			case AXIS -> RotatedPillarBlock.rotatePillar(state, rotation);
			case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
			case FACING, FACING_SURFACE -> state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
			case NONE -> state;
		};
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return switch (placement) {
			case HORIZONTAL -> state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
			case FACING, FACING_SURFACE -> state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.FACING)));
			case AXIS, NONE -> state;
		};
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		if (waterloggable && state.getValue(BlockStateProperties.WATERLOGGED)) {
			return Fluids.WATER.getSource(false);
		}

		return super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (waterloggable && state.getValue(BlockStateProperties.WATERLOGGED)) {
			scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		if (gravity) {
			scheduledTickAccess.scheduleTick(pos, this, 2);
		}

		if (support != null && direction == (support.hanging() ? Direction.UP : Direction.DOWN) && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}

		return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
	}

	// ------------------------------------------------------------------ shapes

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (outlineShapes != null) {
			VoxelShape shape = outlineShapes.get(state);

			if (shape != null) {
				return shape;
			}
		}

		return super.getShape(state, level, pos, context);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (collisionShapes != null && this.hasCollision) {
			VoxelShape shape = collisionShapes.get(state);

			if (shape != null) {
				return shape;
			}
		}

		return super.getCollisionShape(state, level, pos, context);
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return outlineShapes != null;
	}

	// ------------------------------------------------------------------ gravity

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);

		if (gravity) {
			level.scheduleTick(pos, this, 2);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (gravity && FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinY()) {
			FallingBlockEntity.fall(level, pos, state);
			return;
		}

		super.tick(state, level, pos, random);
	}

	// ------------------------------------------------------------------ redstone

	@Override
	protected boolean isSignalSource(BlockState state) {
		return redstonePower > 0;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return redstonePower;
	}

	// ------------------------------------------------------------------ function hooks

	private FunctionHooks hooks() {
		return live.get(BlockComponents.FUNCTIONS);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		FunctionHooks hooks = hooks();

		if (hooks == null || hooks.use().isEmpty()) {
			return super.useWithoutItem(state, level, pos, player, hitResult);
		}

		if (level instanceof ServerLevel serverLevel) {
			BlockFunctions.run(serverLevel, pos, player, hooks.use().get(), false);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		FunctionHooks hooks = hooks();

		if (hooks != null && hooks.stepOn().isPresent() && level instanceof ServerLevel serverLevel) {
			BlockFunctions.run(serverLevel, pos, entity, hooks.stepOn().get(), true);
		}

		super.stepOn(level, pos, state, entity);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean intersects) {
		Slowdown slowdown = live.get(BlockComponents.SLOWS_MOVEMENT);

		if (slowdown != null) {
			entity.makeStuckInBlock(state, new Vec3(slowdown.horizontal(), slowdown.vertical(), slowdown.horizontal()));
		}

		FunctionHooks hooks = hooks();

		if (hooks != null && hooks.entityInside().isPresent() && level instanceof ServerLevel serverLevel
				&& serverLevel.getGameTime() % hooks.entityInsideInterval() == 0) {
			BlockFunctions.run(serverLevel, pos, entity, hooks.entityInside().get(), true);
		}

		super.entityInside(state, level, pos, entity, effectApplier, intersects);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		FunctionHooks hooks = hooks();

		if (hooks != null && hooks.placed().isPresent() && level instanceof ServerLevel serverLevel) {
			BlockFunctions.run(serverLevel, pos, placer, hooks.placed().get(), false);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		FunctionHooks hooks = hooks();

		if (hooks != null && hooks.broken().isPresent() && level instanceof ServerLevel serverLevel) {
			BlockFunctions.run(serverLevel, pos, player, hooks.broken().get(), false);
		}

		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		FunctionHooks hooks = hooks();

		if (hooks != null && hooks.randomTick().isPresent()) {
			BlockFunctions.run(level, pos, null, hooks.randomTick().get(), false);
		}
	}

	// ------------------------------------------------------------------ ambient particles (client)

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		List<ParticleEmitter> emitters = particleSource.apply(this);

		if (emitters == null || emitters.isEmpty()) {
			return;
		}

		ParticleCache cache = particleCache;

		if (cache == null || cache.emitters() != emitters) {
			cache = new ParticleCache(emitters, emitters.stream().map(DataBlock::parseParticle).toList());
			particleCache = cache;
		}

		for (int i = 0; i < emitters.size(); i++) {
			ParticleEmitter emitter = emitters.get(i);
			ParticleOptions options = cache.options().get(i);

			if (options == null || random.nextFloat() >= emitter.chance()) {
				continue;
			}

			for (int n = 0; n < emitter.count(); n++) {
				double x = pos.getX() + 0.5 + emitter.offset().get(0) + (random.nextDouble() * 2 - 1) * emitter.spread().get(0);
				double y = pos.getY() + 0.5 + emitter.offset().get(1) + (random.nextDouble() * 2 - 1) * emitter.spread().get(1);
				double z = pos.getZ() + 0.5 + emitter.offset().get(2) + (random.nextDouble() * 2 - 1) * emitter.spread().get(2);
				level.addParticle(options, x, y, z, emitter.velocity().get(0), emitter.velocity().get(1), emitter.velocity().get(2));
			}
		}
	}

	private static ParticleOptions parseParticle(ParticleEmitter emitter) {
		return ParticleTypes.CODEC.parse(JsonOps.INSTANCE, emitter.particleObject())
				.resultOrPartial(error -> DataBlocks.LOGGER.error("Invalid ambient particle {}: {}", emitter.particle(), error))
				.orElse(null);
	}

	private record ParticleCache(List<ParticleEmitter> emitters, List<ParticleOptions> options) {
	}

	// ------------------------------------------------------------------ drops

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		return switch (drops) {
			case SELF -> this.asItem() == Items.AIR ? List.of() : List.of(new ItemStack(this));
			case NONE -> List.of();
			case LOOT_TABLE -> super.getDrops(state, params);
		};
	}

	// ------------------------------------------------------------------ reloadable movement properties

	@Override
	public float getFriction() {
		Float value = live.get(BlockComponents.FRICTION);
		return value != null ? value : super.getFriction();
	}

	@Override
	public float getSpeedFactor() {
		Float value = live.get(BlockComponents.SPEED_FACTOR);
		return value != null ? value : super.getSpeedFactor();
	}

	@Override
	public float getJumpFactor() {
		Float value = live.get(BlockComponents.JUMP_FACTOR);
		return value != null ? value : super.getJumpFactor();
	}
}
