package com.khazoda.basicstorage.storage;

import com.khazoda.basicstorage.block.entity.CrateStationBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class CrateStationHelper {

  // Search for stations within MAX_RADIUS blocks and force them to re-cache crates
  public static void notifyNearbyStations(World world, BlockPos pos) {
    if (!(world instanceof ServerWorld serverWorld)) return;

    int r = CrateStationBlockEntity.MAX_RADIUS;

    int originX = pos.getX();
    int originY = pos.getY();
    int originZ = pos.getZ();

    int minX = originX - r;
    int maxX = originX + r;
    int minY = originY - r;
    int maxY = originY + r;
    int minZ = originZ - r;
    int maxZ = originZ + r;

    int minChunkX = (originX - r) >> 4;
    int maxChunkX = (originX + r) >> 4;
    int minChunkZ = (originZ - r) >> 4;
    int maxChunkZ = (originZ + r) >> 4;

    for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
      for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
        WorldChunk chunk = serverWorld.getChunkManager().getWorldChunk(chunkX, chunkZ);
        if (chunk == null) continue;

        for (BlockEntity be : chunk.getBlockEntities().values()) {
          if (!(be instanceof CrateStationBlockEntity station)) continue;

          BlockPos stationPos = be.getPos();

          if (stationPos.getX() < minX || stationPos.getX() > maxX) continue;
          if (stationPos.getY() < minY || stationPos.getY() > maxY) continue;
          if (stationPos.getZ() < minZ || stationPos.getZ() > maxZ) continue;

          station.markCacheForUpdate();
        }
      }
    }
  }
}
