package com.khazoda.basicstorage.mixin;

import com.khazoda.basicstorage.block.CrateBlock;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

	@Shadow
	protected ServerWorld world;
	@Shadow
	@Final
	protected ServerPlayerEntity player;
	@Shadow
	private int tickCounter;

	@Unique
	private int basicStorage$startTick = -1;
	@Unique
	private BlockPos basicStorage$targetPos = null;
	@Unique
	private Direction basicStorage$targetSide = null;

	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
	private void onCrateClickStart(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
		if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
			if (this.world.getBlockState(pos).getBlock() instanceof CrateBlock) {
				this.basicStorage$startTick = this.tickCounter;
				this.basicStorage$targetPos = pos;
				this.basicStorage$targetSide = direction;
			} else {
				this.basicStorage$targetPos = null;
				this.basicStorage$targetSide = null;
			}

			return;
		}

		if (action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK) {
			if (this.basicStorage$targetPos != null && this.basicStorage$targetPos.equals(pos)) {
				if (this.tickCounter - this.basicStorage$startTick <= 3 && this.world.getBlockState(pos).getBlock() instanceof CrateBlock && this.basicStorage$targetSide != null)
					CrateBlock.extractFromCrate(this.world, pos, this.player, this.basicStorage$targetSide);

				this.basicStorage$targetPos = null;
				this.basicStorage$targetSide = null;
				this.basicStorage$startTick = -1;
			}

			return;
		}

		if (action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
			if (this.basicStorage$targetPos != null && this.basicStorage$targetPos.equals(pos)) {
				this.basicStorage$targetPos = null;
				this.basicStorage$targetSide = null;
				this.basicStorage$startTick = -1;
			}
		}
	}
}