package com.khazoda.basicstorage.item;

import com.khazoda.basicstorage.config.ModConfig;
import com.khazoda.basicstorage.registry.BlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    PlayerEntity player = context.getPlayer();
    if (player == null) return ActionResult.PASS;
    ItemStack itemStack = context.getStack();

    if (blockState.isOf(BlockRegistry.CRATE_BLOCK)) {
      world.breakBlock(pos, true, player);
      blockState.updateNeighbors(world, pos, Block.NOTIFY_ALL);
      world.updateComparators(pos, blockState.getBlock());

      // Only damage the item if player is not in creative and config durability > 0
      if (!player.getAbilities().creativeMode && ModConfig.getInstance().getCrateHammerDurability() > 0 && this.isDamageable()) {
        itemStack.damage(1, player, (p) -> p.sendToolBreakStatus(context.getHand()));
      }

      return ActionResult.SUCCESS;
    }
    return super.useOnBlock(context);
  }
}
