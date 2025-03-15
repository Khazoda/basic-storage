package com.khazoda.basicstorage.storage;

import com.khazoda.basicstorage.block.entity.CrateStationBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrateStationHelper {

  // Search for stations within MAX_RADIUS blocks and force them to re-cache crates
  public static void notifyNearbyStations(World world, BlockPos pos) {
    int scanRadius = CrateStationBlockEntity.MAX_RADIUS;
    BlockPos.iterate(
        pos.add(-scanRadius, -scanRadius, -scanRadius),
        pos.add(scanRadius, scanRadius, scanRadius)).forEach(checkPos -> {
      BlockEntity be = world.getBlockEntity(checkPos);
      if (be instanceof CrateStationBlockEntity station) {
        station.markCacheForUpdate();
      }
    });
  }
}
