package com.khazoda.basicstorage.block.entity;

import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.storage.CrateSlot;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

public class CrateStationBlockEntity extends BlockEntity {
  private final Map<ItemVariant, List<BlockPos>> crateRegistry = new HashMap<>();
  private final Set<BlockPos> connectedCrates = new HashSet<>();
  public static final int MAX_RADIUS = 16;
  private boolean needsCacheUpdate = true;

  public CrateStationBlockEntity(BlockPos pos, BlockState state) {
    super(BlockEntityRegistry.CRATE_STATION_BLOCK_ENTITY, pos, state);
  }

  public static void tick(World world, BlockPos pos, BlockState state, CrateStationBlockEntity be) {
    if (be.needsCacheUpdate) {
      be.buildCrateCache();
      be.needsCacheUpdate = false;
    }
  }

  private void buildCrateCache() {
    if (world == null || world.isClient)
      return;

    crateRegistry.clear();
    connectedCrates.clear();

    Queue<BlockPos> toExplore = new LinkedList<>();
    Set<BlockPos> visited = new HashSet<>();
    toExplore.add(pos);

    while (!toExplore.isEmpty()) {
      BlockPos current = toExplore.poll();
      if (visited.contains(current) || !isWithinRange(current))
        continue;

      visited.add(current);
      BlockEntity be = world.getBlockEntity(current);
      if (be instanceof CrateStationBlockEntity)
        addDirectionsToExplore(toExplore, current);
      if (be instanceof CrateBlockEntity crate) {
        registerCrate(current, crate.storage);
        addDirectionsToExplore(toExplore, current);
      }
    }
    //    world.getPlayers().getFirst().sendMessage(Text.literal("Updated cache. New crate number: ".concat(String.valueOf(connectedCrates.size())))); Todo: Uncomment to debug crate connections
    markDirty();
  }

  private void addDirectionsToExplore(Queue<BlockPos> blockPositionExplorationQueue, BlockPos currentBlockPosition) {
    for (Direction dir : Direction.values()) {
      blockPositionExplorationQueue.add(currentBlockPosition.offset(dir));
    }
  }

  private void registerCrate(BlockPos cratePos, CrateSlot storage) {
    if (!storage.isBlank()) {
      ItemVariant variant = storage.getResource();
      crateRegistry.computeIfAbsent(variant, k -> new ArrayList<>()).add(cratePos);
      connectedCrates.add(cratePos);
    }
  }

  private boolean isWithinRange(BlockPos target) {
    return Math.abs(target.getX() - pos.getX()) <= MAX_RADIUS &&
        Math.abs(target.getY() - pos.getY()) <= MAX_RADIUS &&
        Math.abs(target.getZ() - pos.getZ()) <= MAX_RADIUS;
  }

  @Override
  public void markRemoved() {
    crateRegistry.clear();
    connectedCrates.clear();
    super.markRemoved();
  }

  public void markCacheForUpdate() {
    this.needsCacheUpdate = true;
    markDirty();
  }

  public Set<BlockPos> getConnectedCrates() {
    return connectedCrates;
  }

  public Map<ItemVariant, List<BlockPos>> getCrateRegistry() {
    return crateRegistry;
  }
}
