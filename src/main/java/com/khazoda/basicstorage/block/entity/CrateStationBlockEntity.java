package com.khazoda.basicstorage.block.entity;

import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.storage.CrateSlot;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class CrateStationBlockEntity extends BlockEntity {
	private final Map<ItemVariant, LongList> crateRegistry = new HashMap<>();

	private static final int[] OFFSET_X = { 0, 0, 0, 0, -1, 1 };
	private static final int[] OFFSET_Y = { -1, 1, 0, 0, 0, 0 };
	private static final int[] OFFSET_Z = { 0, 0, -1, 1, 0, 0 };

	public static final int MAX_RADIUS = 16;

	private int connectedCrateCount = 0;
	private boolean needsCacheUpdate = true;

	public CrateStationBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityRegistry.CRATE_STATION_BLOCK_ENTITY, pos, state);
	}

	private void buildCrateCache() {
		if (world == null || world.isClient) return;

		crateRegistry.clear();
		connectedCrateCount = 0;

		int minX = pos.getX() - MAX_RADIUS;
		int maxX = pos.getX() + MAX_RADIUS;
		int minY = pos.getY() - MAX_RADIUS;
		int maxY = pos.getY() + MAX_RADIUS;
		int minZ = pos.getZ() - MAX_RADIUS;
		int maxZ = pos.getZ() + MAX_RADIUS;

		LongArrayFIFOQueue toExplore = new LongArrayFIFOQueue(256);
		LongOpenHashSet queued = new LongOpenHashSet(256);

		long start = pos.asLong();
		toExplore.enqueue(start);
		queued.add(start);

		BlockPos.Mutable mutable = new BlockPos.Mutable();

		while (!toExplore.isEmpty()) {
			long currentLong = toExplore.dequeueLong();

			mutable.set(currentLong);
			BlockEntity be = world.getBlockEntity(mutable);

			if (be instanceof CrateStationBlockEntity) {
				addDirectionsToExplore(toExplore, queued, currentLong, minX, maxX, minY, maxY, minZ, maxZ);
			} else if (be instanceof CrateBlockEntity crate) {
				connectedCrateCount++;

				if (!crate.storage.isBlank()) registerCrate(currentLong, crate.storage);

				addDirectionsToExplore(toExplore, queued, currentLong, minX, maxX, minY, maxY, minZ, maxZ);
			}
		}
	}

	private void addDirectionsToExplore(LongArrayFIFOQueue queue, LongOpenHashSet queued, long current, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		int x = BlockPos.unpackLongX(current);
		int y = BlockPos.unpackLongY(current);
		int z = BlockPos.unpackLongZ(current);

		for (int i = 0; i < 6; i++) {
			int nextX = x + OFFSET_X[i];
			int nextY = y + OFFSET_Y[i];
			int nextZ = z + OFFSET_Z[i];

			if (nextX < minX || nextX > maxX) continue;
			if (nextY < minY || nextY > maxY) continue;
			if (nextZ < minZ || nextZ > maxZ) continue;

			long next = BlockPos.asLong(nextX, nextY, nextZ);

			if (queued.add(next)) queue.enqueue(next);
		}
	}

	private void registerCrate(long cratePos, CrateSlot storage) {
		ItemVariant variant = storage.getResource();
		crateRegistry.computeIfAbsent(variant, k -> new LongArrayList()).add(cratePos);
	}

	@Override
	public void markRemoved() {
		crateRegistry.clear();
		connectedCrateCount = 0;
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

	public int getConnectedCrateCount() {
		ensureCache();
		return connectedCrateCount;
	}

	public Map<ItemVariant, LongList> getCrateRegistry() {
		ensureCache();
		return crateRegistry;
	}
}
