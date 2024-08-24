package com.khazoda.basicstorage.block;

import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.registry.SoundRegistry;
import com.khazoda.basicstorage.storage.CrateSlot;
import com.khazoda.basicstorage.util.BlockUtils;
import com.khazoda.basicstorage.util.NumberFormatter;
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
import net.minecraft.block.enums.Instrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

import static java.lang.Math.toIntExact;

/**
 * Right Click
 * > holding valid stack - Add one item
 * > holding invalid stack / nothing - Display exact crate contents
 * Shift Right Click - Add all items from inventory that match
 * Left Click - Remove one item
 * Shift Left Click - Remove one stack
 */
public class CrateBlock extends Block implements BlockEntityProvider {
  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
  public static final Settings defaultSettings = Settings.create().sounds(BlockSoundGroup.WOOD).strength(2.5f).pistonBehavior(PistonBehavior.BLOCK).instrument(Instrument.BASS).mapColor(MapColor.OAK_TAN);

  private static Random random;

  public CrateBlock(Settings settings) {
    super(settings);
    random = new Random();
    setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
  }

  public CrateBlock() {
    this(defaultSettings);
  }


  /**
   * Event hook instead of onUse() method in order to capture interactions while sneaking
   */
  public static void initOnUseMethod() {
    /* Method is fired on every block right click, so immediate check for crate block class is needed */
    UseBlockCallback.EVENT.register((PlayerEntity player, World world, Hand hand, BlockHitResult hit) -> {
      if (!world.getBlockState(hit.getBlockPos()).isOf(BlockRegistry.CRATE_BLOCK)) return ActionResult.PASS;
      if (!player.canModifyBlocks() || player.isSpectator()) return ActionResult.PASS;

      BlockPos pos = hit.getBlockPos();
      BlockState state = world.getBlockState(pos);
      BlockEntity be = world.getBlockEntity(pos);
      Direction facing = state.get(Properties.HORIZONTAL_FACING);

      if (be == null) return ActionResult.PASS;
      if (facing != hit.getSide()) return ActionResult.PASS;

      CrateBlockEntity cbe = (CrateBlockEntity) be;
      ItemStack playerStack = player.getMainHandStack();
      CrateSlot slot = cbe.storage;
      if (playerStack.isOf(Items.DEBUG_STICK)) return debugInitOnUseMethod(player, slot);

      try (var t = Transaction.openOuter()) {
        int inserted = 0;
        if (player.isSneaking()) {
          if (!canInsert(playerStack, slot, true)) return listExactContents(player, slot);
          inserted = insertMaximum(player, playerStack, slot, t);
        } else if (!player.isSneaking()) {
          if (!canInsert(playerStack, slot, false)) return listExactContents(player, slot);
          inserted = insertOne(playerStack, slot, t);
        }
        if (inserted == 0) {
          t.abort();
          return ActionResult.CONSUME_PARTIAL;
        }
        t.commit();
        if (inserted == 1)
          world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundRegistry.INSERT_ONE, SoundCategory.BLOCKS, 1f, 1f + ((-1 + random.nextFloat() * (1 + 1)) / 10), false);
        if (inserted > 1)
          world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundRegistry.INSERT_MANY, SoundCategory.BLOCKS, 1f, 1f, false);
        state.updateNeighbors(world, pos, 1);
        cbe.refresh();
        world.updateComparators(pos, state.getBlock());
        player.incrementStat(Stats.USED.getOrCreateStat(playerStack.getItem()));
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
    /* Insert as many items as possible from player's inventory if slot is empty, or matches held stack */
    if (slot.isBlank() && playerStack.isEmpty()) {
      return 0;
    } else if (slot.isBlank() && !playerStack.isEmpty()) {
      /* Insert into empty crate */
      int i = (int) slot.insert(ItemVariant.of(playerStack), playerStack.getCount(), transaction);
      playerStack.decrement(i);
      return i;
    } else {
      /* Insert into crate with items */
      return (int) StorageUtil.move(PlayerInventoryStorage.of(player), slot, itemVariant -> true, Integer.MAX_VALUE, transaction);
    }
  }

  /**
   * UseBlockCallback helper method
   **/
  private static ActionResult listExactContents(PlayerEntity player, CrateSlot slot) {
    /* Show exact contents of crate to play via message */
    Text message;
    if (slot.isBlank()) {
      message = Text.translatable("message.basicstorage.crate.empty").styled(s ->
          s.withColor(0xFFDD99));
    } else {
      message = Text.literal(NumberFormatter.toFormattedNumber(slot.getAmount()) + " " + slot.getResource().getItem().getName().getString()).styled(s ->
          s.withColor(0xFFDD99));
    }
    player.sendMessage(message, true);
    return ActionResult.CONSUME;
  }

  /**
   * UseBlockCallback helper method
   **/
  /* Add blacklisted items to this method */
  /* Stop them being inserted into crates */
  public static boolean canInsert(ItemStack stack, CrateSlot slot, boolean insertingMultiple) {
    if (insertingMultiple) {
      return !slot.isBlank() || canInsert(stack, slot, false); // Prevents stacked undesirables from being insertable when sneaking
// This is ok as another check is done when actually inserting the items in CrateSlot#insert
    } else {
      if (stack.isEmpty()) return false;
      if (stack.isDamaged()) return false;
      if (stack.isOf(BlockRegistry.CRATE_BLOCK.asItem())
          && stack.getOrCreateNbt().getCompound("BlockEntityTag").contains("crate_contents")) return false;
      if (!ItemVariant.of(stack).equals(slot.getResource()) && !slot.isBlank()) return false;
      return slot.isBlank() || stack.isOf(slot.getResource().getItem());
    }
  }

  /**
   * Method for removing either 1 item or a whole stack of items from a crate
   */
  @Override
  public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
    if (!player.canModifyBlocks()) return;

    CrateBlockEntity cbe = (CrateBlockEntity) world.getBlockEntity(pos);
    if (cbe == null) return;
    if (cbe.storage.isBlank()) return;

    var hit = BlockUtils.getHitResult(player, pos);
    if (hit.getType() == HitResult.Type.MISS) return;

    Direction facing = state.get(Properties.HORIZONTAL_FACING);
    if (facing != hit.getSide()) return;

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
        world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundRegistry.INSERT_ONE, SoundCategory.BLOCKS, 0.6f, 1.2f + ((-1 + random.nextFloat() * (1 + 1)) / 10), false);
      if (extracted > 1)
        world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundRegistry.EXTRACT_MANY, SoundCategory.BLOCKS, 0.75f, 1f, false);

      world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.35f, 1f, false);
    }
    cbe.refresh();
    state.updateNeighbors(world, pos, 1);
    world.updateComparators(pos, state.getBlock());
    world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
  }

  /**
   * Handles breaking in creative mode
   */
  @Override
  public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    BlockEntity be = world.getBlockEntity(pos);
    if (!(be == null)) {
      CrateBlockEntity cbe = (CrateBlockEntity) be;
      if (!world.isClient() && player.isCreative() && !cbe.storage.getResource().toStack().isEmpty()) {
        getDroppedStacks(state, (ServerWorld) world, pos, cbe, player, player.getStackInHand(Hand.MAIN_HAND))
            .forEach(stack -> ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack));
      }
    }
  }

  @Override
  public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
    return super.getDroppedStacks(state, builder);
  }

  /**
   * Applies custom tooltip showing crate contents
   **/
  @Override
  public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
    NbtCompound nbt = stack.getOrCreateNbt().getCompound("BlockEntityTag");
    if (!nbt.contains("crate_contents")) return;
    NbtCompound crateContents = nbt.getCompound("crate_contents");
    ItemVariant item = ItemVariant.fromNbt(nbt.getCompound("item"));
    int count = crateContents.getInt("count");

    MutableText contents_line_2 = Text.literal(item.getItem().getName().getString()).styled(s ->
        s.withColor(0xCCAA77));
    MutableText contents_line_1 = Text.literal("x" + NumberFormatter.toFormattedNumber(count)).styled(s ->
        s.withColor(0xFFDD99));

    tooltip.add(contents_line_1);
    tooltip.add(contents_line_2);
  }

  public static Direction getFront(BlockState state) {
    return state.get(FACING);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
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
    return this.getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
  }

  @Override
  public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (state.isOf(newState.getBlock())) {
      return;
    }
    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity instanceof CrateBlockEntity) {
      world.updateComparators(pos, state.getBlock());
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  public BlockState rotate(BlockState state, BlockRotation rotation) {
    return state.with(FACING, rotation.rotate(state.get(FACING)));
  }

  @Override
  public BlockState mirror(BlockState state, BlockMirror mirror) {
    return state.rotate(mirror.getRotation(state.get(FACING)));
  }


  @Override
  public boolean hasComparatorOutput(BlockState state) {
    return true;
  }

  /**
   * Comparator Logic
   * 1-16 items = signal strength, loops to 1 billion
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

  /**
   * Debugging Methods, not for survival gameplay use
   */
  private static ActionResult debugInitOnUseMethod(PlayerEntity player, CrateSlot slot) {
    try (Transaction t = Transaction.openOuter()) {
      if (slot.isBlank()) return ActionResult.PASS;
      if (player.isSneaking()) slot.extract(slot.getResource(), 10000, t);
      if (!player.isSneaking()) slot.insert(slot.getResource(), 100000, t);
      t.commit();
    }
    slot.triggerRefresh();
    return ActionResult.SUCCESS;
  }
}