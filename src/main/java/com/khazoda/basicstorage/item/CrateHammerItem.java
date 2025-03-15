package com.khazoda.basicstorage.item;

import com.khazoda.basicstorage.registry.BlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrateHammerItem extends Item {
  public CrateHammerItem(Settings settings) {
    super(settings);
  }

  @Override
  public ActionResult useOnBlock(ItemUsageContext context) {
    World world = context.getWorld();
    BlockPos pos = context.getBlockPos();
    BlockState blockState = world.getBlockState(pos);

    if (blockState.isOf(BlockRegistry.CRATE_BLOCK)) {
      world.breakBlock(pos, true, context.getPlayer());

      blockState.updateNeighbors(world, pos, Block.NOTIFY_ALL);
      world.updateComparators(pos, blockState.getBlock());
      return ActionResult.SUCCESS;
    }
    return super.useOnBlock(context);
  }
}
