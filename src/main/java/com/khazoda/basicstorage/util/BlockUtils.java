package com.khazoda.basicstorage.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class BlockUtils {

	public static BlockHitResult getHitResult(PlayerEntity player, BlockPos target) {
		final Vec3d castOrigin = player.getEyePos();
		final double castLength = Vec3d.ofCenter(target).subtract(castOrigin).length() + 1;
		final Vec3d playerRotation = player.getRotationVector();
		final Vec3d castTarget = castOrigin.add(playerRotation.multiply(castLength));

		return player.getWorld().raycast(new RaycastContext(castOrigin, castTarget, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
	}

	public static int getComparatorOutputStrength(int itemStackCount) {
		return MathHelper.floor(itemStackCount % 16);
	}
}
