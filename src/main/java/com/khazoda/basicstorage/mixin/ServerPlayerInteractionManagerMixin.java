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

import java.util.HashMap;
import java.util.Map;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

  @Shadow protected ServerWorld world;
  @Final
  @Shadow
  protected ServerPlayerEntity player;

  @Unique private final Map<BlockPos, Integer> crateClickStartTicks = new HashMap<>();

  @Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
  private void onCrateClickStart(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
    if (this.world.getBlockState(pos).getBlock() instanceof CrateBlock) {
      if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
        crateClickStartTicks.put(pos, this.world.getServer().getTicks());
      }
    }
  }

  @Inject(method = "processBlockBreakingAction", at = @At("TAIL"))
  private void onCrateClickEnd(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
    if ((action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK ||
        action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) &&
        this.world.getBlockState(pos).getBlock() instanceof CrateBlock) {

      Integer startTick = crateClickStartTicks.remove(pos);
      if (startTick != null) {
        int durationTicks = this.world.getServer().getTicks() - startTick;
        // Quick Click (Extract)
        if (durationTicks <= 3) {
          CrateBlock.extractFromCrate(this.world, pos, this.player);
        }
        // Otherwise do nothing (allow breaking without extraction)
      }
    }
  }
}