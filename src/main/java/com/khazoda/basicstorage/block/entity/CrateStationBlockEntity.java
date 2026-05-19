package com.khazoda.basicstorage.block.entity;

import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.storage.CrateSlot;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class CrateStationBlockEntity extends BlockEntity {
	private final Map<ItemVariant, List<BlockPos>> crateRegistry = new HashMap<>();
	private final Set<BlockPos> connectedCrates = new HashSet<>();
	public static final int MAX_RADIUS = 16;
	private boolean needsCacheUpdate = true;

	public CrateStationBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityRegistry.CRATE_STATION_BLOCK_ENTITY, pos, state);
	}

	private void buildCrateCache() {
		if (world == null || world.isClient) return;

		crateRegistry.clear();
		connectedCrates.clear();

		Queue<BlockPos> toExplore = new ArrayDeque<>();
		Set<BlockPos> queued = new HashSet<>();

		toExplore.add(pos);
		queued.add(pos);

		while (!toExplore.isEmpty()) {
			BlockPos current = toExplore.poll();

			BlockEntity be = world.getBlockEntity(current);

			if (be instanceof CrateStationBlockEntity) addDirectionsToExplore(toExplore, queued, current);

			if (be instanceof CrateBlockEntity crate) {
				connectedCrates.add(current);

				if (!crate.storage.isBlank()) registerCrate(current, crate.storage);

				addDirectionsToExplore(toExplore, queued, current);
			}
		}
		//world.getPlayers().getFirst().sendMessage(Text.literal("Updated cache. New crate number: ".concat(String.valueOf(connectedCrates.size())))); // TODO: Uncomment to debug crate connections
	}

	private void addDirectionsToExplore(Queue<BlockPos> queue, Set<BlockPos> queued, BlockPos current) {
		for (Direction dir : Direction.values()) {
			BlockPos next = current.offset(dir);

			if (isWithinRange(next) && queued.add(next)) {
				queue.add(next);
			}
		}
	}

	private void registerCrate(BlockPos cratePos, CrateSlot storage) {
		ItemVariant variant = storage.getResource();
		crateRegistry.computeIfAbsent(variant, k -> new ArrayList<>()).add(cratePos);
	}

	private boolean isWithinRange(BlockPos target) {
		return Math.abs(target.getX() - pos.getX()) <= MAX_RADIUS && Math.abs(target.getY() - pos.getY()) <= MAX_RADIUS && Math.abs(target.getZ() - pos.getZ()) <= MAX_RADIUS;
	}

	@Override
	public void markRemoved() {
		crateRegistry.clear();
		connectedCrates.clear();
		super.markRemoved();
	}

	private void ensureCache() {
		if (!needsCacheUpdate) return;
		if (world == null || world.isClient) return;

		buildCrateCache();
		needsCacheUpdate = false;
	}

	public void markCacheForUpdate() {
		this.needsCacheUpdate = true;
	}

	public Set<BlockPos> getConnectedCrates() {
		ensureCache();
		return connectedCrates;
	}

	public Map<ItemVariant, List<BlockPos>> getCrateRegistry() {
		ensureCache();
		return crateRegistry;
	}
}
