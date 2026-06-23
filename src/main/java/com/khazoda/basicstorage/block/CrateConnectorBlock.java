package com.khazoda.basicstorage.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.khazoda.basicstorage.storage.CrateStationHelper.notifyNearbyStations;

public class CrateConnectorBlock extends Block {
  public static final MapCodec<CrateConnectorBlock> CODEC = CrateConnectorBlock.createCodec(CrateConnectorBlock::new);

  public static final Settings defaultSettings = Settings.create()
      .sounds(BlockSoundGroup.WOOD)
      .pistonBehavior(PistonBehavior.BLOCK)
      .instrument(NoteBlockInstrument.BASS)
      .mapColor(MapColor.OAK_TAN)
      .strength(1f);

  public CrateConnectorBlock(Settings settings) {
    super(settings);
  }

  public CrateConnectorBlock() {
    this(defaultSettings);
  }

  @Override
  public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
    super.onPlaced(world, pos, state, placer, itemStack);
    notifyNearbyStations(world, pos);
  }

  @Override
  protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())) notifyNearbyStations(world, pos);

    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Nullable
  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    return this.getDefaultState();
  }

  @Override
  protected BlockRenderType getRenderType(BlockState state) {
    return BlockRenderType.MODEL;
  }

  @Override
  public MapCodec<CrateConnectorBlock> getCodec() {
    return CODEC;
  }
}