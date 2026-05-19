package com.khazoda.basicstorage.storage;

import com.khazoda.basicstorage.block.entity.CrateStationBlockEntity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrateStationHelper {

	// Search for stations within MAX_RADIUS blocks and force them to re-cache crates
	public static void notifyNearbyStations(World world, BlockPos pos) {
		if (world == null || world.isClient()) return;

		int scanRadius = CrateStationBlockEntity.MAX_RADIUS;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int x = pos.getX() - scanRadius; x <= pos.getX() + scanRadius; x++) {
			for (int y = pos.getY() - scanRadius; y <= pos.getY() + scanRadius; y++) {
				for (int z = pos.getZ() - scanRadius; z <= pos.getZ() + scanRadius; z++) {
					mutable.set(x, y, z);

					BlockEntity be = world.getBlockEntity(mutable);
					if (be instanceof CrateStationBlockEntity station) station.markCacheForUpdate();
				}
			}
		}
	}
}
