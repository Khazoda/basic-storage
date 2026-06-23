package com.khazoda.basicstorage.block;

import com.khazoda.basicstorage.BasicStorageConfig;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.registry.DataComponentRegistry;
import com.khazoda.basicstorage.registry.SoundRegistry;
import com.khazoda.basicstorage.storage.CrateSlot;
import com.khazoda.basicstorage.structure.CrateSlotComponent;
import com.khazoda.basicstorage.util.BlockUtils;
import com.khazoda.basicstorage.util.NumberFormatter;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.enums.Orientation;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import static com.khazoda.basicstorage.storage.CrateStationHelper.notifyNearbyStations;
import static java.lang.Math.toIntExact;

/**
 * Right Click
 * > holding valid stack - Add one item
 * > holding invalid stack / nothing - Display exact crate contents
 * Shift Right Click - Add all items from inventory that match
 * Left Click - Remove one item
 * Shift Left Click - Remove one stack
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class CrateBlock extends Block implements BlockEntityProvider {
  public static final EnumProperty<Orientation> ORIENTATION = Properties.ORIENTATION;
  public static final MapCodec<CrateBlock> CODEC = CrateBlock.createCodec(CrateBlock::new);
  public static final Settings defaultSettings = getCrateSettings();
  private static final Random random = new Random();

  public CrateBlock(Settings settings) {
    super(settings);
    setDefaultState(this.stateManager.getDefaultState().with(ORIENTATION, Orientation.NORTH_UP));
  }

  public CrateBlock() {
    this(defaultSettings);
  }

  private static Block.Settings getCrateSettings() {
    return Settings.create().sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.BLOCK).instrument(NoteBlockInstrument.BASS).mapColor(MapColor.OAK_TAN).strength(1f);
  }

  /**
   * Event hook instead of onUse() method in order to capture interactions while sneaking
   */
  public static void initOnUseMethod() {
    //Method is fired on every block right click, so immediate check for crate block class is needed
    UseBlockCallback.EVENT.register((PlayerEntity player, World world, Hand hand, BlockHitResult hit) -> {
      if (!world.getBlockState(hit.getBlockPos()).isOf(BlockRegistry.CRATE_BLOCK)) return ActionResult.PASS;
      if (!player.canModifyBlocks() || player.isSpectator()) return ActionResult.PASS;

      BlockPos pos = hit.getBlockPos();
      BlockState state = world.getBlockState(pos);

      if (state.get(Properties.ORIENTATION).getFacing() != hit.getSide()) return ActionResult.PASS;
      if (world.isClient()) return ActionResult.SUCCESS;
      if (!(world.getBlockEntity(pos) instanceof CrateBlockEntity cbe)) return ActionResult.PASS;

      ItemStack playerStack = player.getStackInHand(hand);
      Item usedItem = playerStack.getItem();
      boolean hadHeldItem = !playerStack.isEmpty();

      CrateSlot slot = cbe.storage;
      boolean insertingMultiple = player.isSneaking();

      if (!canInsert(playerStack, slot, insertingMultiple)) return listExactContents(player, slot);

      try (var transaction = Transaction.openOuter()) {
        int inserted = insertingMultiple ? insertMaximum(player, playerStack, slot, transaction) : insertOne(playerStack, slot, transaction);

        if (inserted == 0) {
          transaction.abort();
          return ActionResult.CONSUME_PARTIAL;
        }

        transaction.commit();

        if (inserted == 1)
          world.playSound(null, pos, SoundRegistry.INSERT_ONE, SoundCategory.BLOCKS, 1f, 1f + ((-0.5f + random.nextFloat() * 1.5f) / 10));
        if (inserted > 1) world.playSound(null, pos, SoundRegistry.INSERT_MANY, SoundCategory.BLOCKS, 1f, 1f);

        state.updateNeighbors(world, pos, Block.NOTIFY_LISTENERS);

        if (hadHeldItem) player.incrementStat(Stats.USED.getOrCreateStat(usedItem));

        world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);

        return ActionResult.SUCCESS;
      }
    });
  }

  /**
   * UseBlockCallback helper method
   **/
  private static int insertOne(ItemStack playerStack, CrateSlot slot, Transaction t) {
    /* Insert one item into crate, if matching player's active held stack */
    if (playerStack.isEmpty()) return 0;
    int inserted = (int) slot.insert(ItemVariant.of(playerStack), 1, t);
    playerStack.decrement(inserted);
    return inserted;
  }

  /**
   * UseBlockCallback helper method
   **/
  private static int insertMaximum(PlayerEntity player, ItemStack playerStack, CrateSlot slot, Transaction transaction) {
    if (slot.isBlank()) {
      if (playerStack.isEmpty()) return 0;

      ItemVariant variant = ItemVariant.of(playerStack);

      int insertedHeld = (int) slot.insert(variant, playerStack.getCount(), transaction);
      playerStack.decrement(insertedHeld);

      int insertedInventory = (int) StorageUtil.move(PlayerInventoryStorage.of(player), slot, itemVariant -> itemVariant.equals(variant), Integer.MAX_VALUE, transaction);

      return insertedHeld + insertedInventory;
    }

    ItemVariant variant = slot.getResource();

    return (int) StorageUtil.move(PlayerInventoryStorage.of(player), slot, itemVariant -> itemVariant.equals(variant), Integer.MAX_VALUE, transaction);
  }

  /**
   * UseBlockCallback helper method
   **/
  private static ActionResult listExactContents(PlayerEntity player, CrateSlot slot) {
    /* Show exact contents of crate to play via message */
    Text message;
    if (slot.isBlank()) {
      message = Text.translatable("message.basicstorage.crate.empty").withColor(0xFFDD99);
    } else {
      message = Text.literal(NumberFormatter.toFormattedNumber(slot.getAmount()) + " " + slot.getResource().getItem().getName().getString()).withColor(0xFFDD99);
    }
    player.sendMessage(message, true);
    return ActionResult.CONSUME;
  }

  /**
   * UseBlockCallback helper method
   **/
  // Add blacklisted items to this method
  // Stop them being inserted into crates
  public static boolean canInsert(ItemStack stack, CrateSlot slot, boolean insertingMultiple) {
    if (insertingMultiple) {
      return !slot.isBlank() || canInsert(stack, slot, false);
      // Prevents stacked undesirables from being insertable when sneaking
      // This is ok as another check is done when actually inserting the items in CrateSlot#insert
    } else {
      if (stack.isEmpty()) return false;
      if (stack.isDamaged()) return false;
      if (stack.isOf(BlockRegistry.CRATE_BLOCK.asItem()) && stack.contains(DataComponentRegistry.CRATE_CONTENTS))
        return false;
      if (!ItemVariant.of(stack).equals(slot.getResource()) && !slot.isBlank()) return false;
      return slot.isBlank() || stack.isOf(slot.getResource().getItem());
    }
  }

  public static void extractFromCrate(World world, BlockPos pos, PlayerEntity player, Direction hitSide) {
    if (!player.canModifyBlocks()) return;
    BlockEntity be = world.getBlockEntity(pos);
    if (!(be instanceof CrateBlockEntity cbe) || cbe.storage.isBlank()) return;

    BlockState state = world.getBlockState(pos);

    Direction facing = state.get(Properties.ORIENTATION).getFacing();
    if (facing != hitSide) return;
    if (AttackBlockCallback.EVENT.invoker().interact(player, world, Hand.MAIN_HAND, pos, hitSide) == ActionResult.FAIL)
      return;

    try (var t = Transaction.openOuter()) {
      var item = cbe.storage.getResource();
      var extracted = (int) cbe.storage.extract(item, player.isSneaking() ? item.getItem().getMaxCount() : 1, t);
      if (extracted == 0) {
        t.abort();
        return;
      }
      player.getInventory().offerOrDrop(item.toStack(extracted));
      t.commit();

      if (extracted == 1)
        world.playSound(null, pos, SoundRegistry.EXTRACT_ONE, SoundCategory.BLOCKS, 0.6f, 1.2f + ((-1 + random.nextFloat() * (1 + 1)) / 10));
      if (extracted > 1) world.playSound(null, pos, SoundRegistry.EXTRACT_MANY, SoundCategory.BLOCKS, 0.75f, 1f);
      world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.35f, 1f);
    }
    state.updateNeighbors(world, pos, Block.NOTIFY_LISTENERS);
    world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
  }

  @Override
  public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
    super.onPlaced(world, pos, state, placer, itemStack);
    notifyNearbyStations(world, pos);
    world.emitGameEvent(placer, GameEvent.BLOCK_PLACE, pos);
  }

  @Override
  protected float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
    if (!player.canModifyBlocks()) return super.calcBlockBreakingDelta(state, player, world, pos);
    if (BasicStorageConfig.getInstance().breakWithAxeOnly()) {
      boolean usingAxe = player.getMainHandStack().isIn(ItemTags.AXES);
      if (usingAxe) {
        return super.calcBlockBreakingDelta(state, player, world, pos);
      } else {
        return 0.0f;
      }
    }
    return super.calcBlockBreakingDelta(state, player, world, pos);
  }

  /**
   * Handles breaking in creative mode
   */
  @Override
  public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    BlockEntity be = world.getBlockEntity(pos);

    if (!world.isClient() && player.isCreative() && be instanceof CrateBlockEntity cbe && !cbe.storage.isBlank()) {
      List<ItemStack> drops = getDroppedStacks(state, (ServerWorld) world, pos, cbe, player, player.getStackInHand(Hand.MAIN_HAND));
      for (int i = 0, size = drops.size(); i < size; i++) {
        ItemStack stack = drops.get(i);
        ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack);
      }
    }

    return super.onBreak(world, pos, state, player);
  }

  /**
   * Applies custom tooltip showing crate contents
   **/
  @Override
  public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
    CrateSlotComponent contentsComponent = stack.get(DataComponentRegistry.CRATE_CONTENTS);
    if (contentsComponent == null) return;
    ItemVariant item = contentsComponent.item();
    int amount = contentsComponent.count();

    MutableText contents_line_2 = Text.literal(item.getItem().getName().getString()).withColor(0xCCAA77);
    MutableText contents_line_1 = Text.literal("x" + NumberFormatter.toFormattedNumber(amount)).withColor(0xFFDD99);

    tooltip.add(contents_line_1);
    tooltip.add(contents_line_2);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(ORIENTATION);
  }

  @Override
  protected boolean canPathfindThrough(BlockState state, NavigationType type) {
    return false;
  }

  @Nullable
  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new CrateBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    Direction facing = ctx.getPlayerLookDirection().getOpposite();
    Direction rotation;

    if (facing.getAxis().isVertical()) {
      rotation = ctx.getHorizontalPlayerFacing();
      if (facing == Direction.DOWN) rotation = rotation.getOpposite();
    } else {
      rotation = Direction.UP;
    }

    return this.getDefaultState().with(Properties.ORIENTATION, Orientation.byDirections(facing, rotation));
  }

  @Override
  protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (state.isOf(newState.getBlock())) return;
    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity instanceof CrateBlockEntity) {
      world.updateComparators(pos, state.getBlock());
      notifyNearbyStations(world, pos);
      world.emitGameEvent(null, GameEvent.BLOCK_DESTROY, pos);
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  protected BlockState rotate(BlockState state, BlockRotation rotation) {
    Orientation current = state.get(ORIENTATION);
    Direction newFacing = rotation.rotate(current.getFacing());
    Direction newRotation = rotation.rotate(current.getRotation());
    return state.with(ORIENTATION, Orientation.byDirections(newFacing, newRotation));
  }

  @Override
  protected BlockState mirror(BlockState state, BlockMirror mirror) {
    Orientation current = state.get(ORIENTATION);
    Direction newFacing = mirror.apply(current.getFacing());
    Direction newRotation = mirror.apply(current.getRotation());
    return state.with(ORIENTATION, Orientation.byDirections(newFacing, newRotation));
  }

  @Override
  public boolean hasComparatorOutput(BlockState state) {
    return true;
  }

  /**
   * Comparator Logic
   * 1-16 items = signal strength, loops
   */
  @Override
  public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
    BlockEntity be = world.getBlockEntity(pos);
    if (be instanceof CrateBlockEntity cbe) {
      return BlockUtils.getComparatorOutputStrength(toIntExact(cbe.storage.getAmount()));
    } else {
      return 0;
    }
  }

  @Override
  public MapCodec<CrateBlock> getCodec() {
    return CODEC;
  }

//  DEBUG METHODS
//	private static ActionResult debugInitOnUseMethod(PlayerEntity player, CrateSlot slot) {
//		try (Transaction t = Transaction.openOuter()) {
//			if (slot.isBlank())
//				return ActionResult.PASS;
//			if (player.isSneaking())
//				slot.extract(slot.getResource(), 10000, t);
//			if (!player.isSneaking())
//				slot.insert(slot.getResource(), 100000, t);
//			t.commit();
//		}
//		slot.update();
//		return ActionResult.SUCCESS;
//	}
}