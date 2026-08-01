package com.khazoda.basicstorage.renderer;

import com.khazoda.basicstorage.block.CrateBlock;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.util.NumberFormatter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity, CrateRenderState> {

  private static final Quaternionf ITEM_LIGHT_ROTATION_3D = new Quaternionf().rotateX((float) Math.toRadians(-15)).rotateY((float) Math.toRadians(15));
  private final ItemModelResolver itemModelManager;
  private final Font textRenderer;
  private final BlockModelLighter blockModelLighter = new BlockModelLighter();

  public CrateRenderer(BlockEntityRendererProvider.Context context) {
    this.itemModelManager = context.itemModelResolver();
    this.textRenderer = context.font();
  }

  @Override
  public CrateRenderState createRenderState() {
    return new CrateRenderState();
  }

  @Override
  public void extractRenderState(CrateBlockEntity be, CrateRenderState crateState, float progress, Vec3 camera, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
    BlockEntityRenderer.super.extractRenderState(be, crateState, progress, camera, crumbling);

    BlockState state = be.getBlockState();
    FrontAndTop orientation = state.getValue(CrateBlock.ORIENTATION);
    crateState.orientation = orientation;
    Direction facingDir = orientation.front();
    BlockPos pos = be.getBlockPos();
    var world = be.getLevel();

    if (world instanceof ClientLevel clientLevel) {
      BlockPos neighborPos = pos.relative(facingDir);
      BlockState neighborState = clientLevel.getBlockState(neighborPos);

      if (!Block.shouldRenderFace(state, neighborState, facingDir)) {
        crateState.itemRenderState = null;
        crateState.itemCount = 0;
        return;
      }
      crateState.lightCoords = blockModelLighter.getLightCoords(state, clientLevel, neighborPos);
    }

    if (be.storage.isResourceBlank()) {
      crateState.itemRenderState = null;
      crateState.itemCount = 0;
      crateState.cachedFormattedCount = null;
      crateState.cachedOrderedText = null;
      return;
    }

    ItemStack itemStack = be.storage.getResource().toStack();

    ItemStackRenderState itemState = new ItemStackRenderState();
    this.itemModelManager.updateForTopItem(itemState, itemStack, ItemDisplayContext.GUI, world, be, 0);
    crateState.itemRenderState = itemState;

    Float rolling = ParticleBeamRendering.rollingItemCounts.get(pos);
    Integer known = ParticleBeamRendering.knownItemCounts.get(pos);
    int actual = Math.toIntExact(be.storage.getAmount());

    /* Rolling item count activated by station beam */
    if (rolling != null) {
      crateState.itemCount = rolling.intValue();
    } else {
      if (known != null && known != actual) {
        /* Rolling item count activated via player insertion */
        ParticleBeamRendering.rollingItemCounts.put(pos, (float) known);
        crateState.itemCount = known;
      } else {
        crateState.itemCount = actual;
      }
    }

    ParticleBeamRendering.knownItemCounts.put(pos, actual);

    crateState.cachedFormattedCount = NumberFormatter.format(crateState.itemCount);
    crateState.cachedOrderedText = textRenderer.split(FormattedText.of(crateState.cachedFormattedCount), 128).getFirst();

    crateState.isRegistered = be.isRegistered();

    crateState.highlightTicks = ParticleBeamRendering.highlightedCrates.getOrDefault(pos, 0);
  }

  @Override
  public void submit(CrateRenderState crateState, PoseStack matrices, SubmitNodeCollector queue, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
    ItemStackRenderState itemState = crateState.itemRenderState;

    if (crateState.orientation == null) return;

    matrices.pushPose();
    alignMatricesToOrientation(matrices, crateState.orientation);

    if (itemState != null) {
      this.renderItem(crateState, itemState, matrices, queue);
    }
    this.renderText(crateState, matrices, queue);

    if (crateState.holdingDebugger && !crateState.isRegistered) {
      this.renderNotice(matrices, queue, crateState.lightCoords);
    }

    matrices.popPose();
  }

  /* Red exclamation mark above crate if it's not registered to a network */
  private void renderNotice(PoseStack matrices, SubmitNodeCollector queue, int light) {
    matrices.pushPose();
    matrices.mulPose(Axis.XP.rotationDegrees(180));
    matrices.translate(0.1f, -0.1f, -0.05f);
    matrices.mulPose(Axis.ZP.rotationDegrees(25));
    matrices.scale(0.05f, 0.05f, 0.05f);
    String text = "!";
    queue.submitText(matrices, -textRenderer.width(text) / 2f, -10, textRenderer.split(FormattedText.of(text), 128).getFirst(), false, DisplayMode.POLYGON_OFFSET, light, 0xFFFF1111, 0, 0);
    matrices.popPose();
  }

  private void renderItem(CrateRenderState state, ItemStackRenderState itemState, PoseStack matrices, SubmitNodeCollector queue) {
    matrices.pushPose();

    /* Apply synced animation if highlighted */
    if (state.highlightTicks > 0) {
      float time = getAnimationDuration(state.highlightTicks);
      float ease = getEaseFromTime(time);

      float scale = 1.0f + 0.1f * ease;
      matrices.scale(scale, scale, scale);

      float shakeFreq = state.highlightTicks * 2.5f;
      float shakeAmp = 0.01f * ease;

      float bufX = (float) Math.sin(shakeFreq) * shakeAmp;
      float bufY = (float) Math.cos(shakeFreq * 0.8f) * shakeAmp;
      matrices.translate(bufX, bufY, 0);

      float rotAmp = 2.5f * ease;
      float rotZ = (float) Math.sin(shakeFreq * 0.5f) * rotAmp;
      matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));
    }

    matrices.translate(0f, 0.125f, 0f);
    matrices.scale(0.5f, 0.5f, 0.5f);
    matrices.scale(0.75f, 0.75f, 1);

    matrices.last().pose().mul(new Matrix4f().scale(1, 1, 0.01f));
    matrices.last().normal().identity();
    matrices.last().normal().rotate(ITEM_LIGHT_ROTATION_3D);

    itemState.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    matrices.popPose();
  }

  public void renderText(CrateRenderState state, PoseStack matrices, SubmitNodeCollector queue) {
    if (state.cachedOrderedText == null || state.cachedFormattedCount == null) return;

    matrices.pushPose();
    matrices.mulPose(Axis.XP.rotationDegrees(180));
    matrices.translate(0f, 0.21f, -0.01f);
    matrices.scale(0.02f, 0.02f, 0.02f);

    int baseColor = state.itemCount > 0 ? 0xFFFFDD99 : 0x22FFDD99;
    int color = baseColor;

    // Interpolate color if highlighted
    if (state.highlightTicks > 0) {

      float time = getAnimationDuration(state.highlightTicks);
      float ease = getEaseFromTime(time);

      int r1 = (0xFFFFFFFF >> 16) & 0xFF;
      int g1 = (0xFFFFFFFF >> 8) & 0xFF;
      int b1 = 0xFF;

      int r2 = (baseColor >> 16) & 0xFF;
      int g2 = (baseColor >> 8) & 0xFF;
      int b2 = baseColor & 0xFF;

      int r = (int) (r1 * ease + r2 * (1 - ease));
      int g = (int) (g1 * ease + g2 * (1 - ease));
      int b = (int) (b1 * ease + b2 * (1 - ease));

      color = (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    queue.submitText(matrices, -textRenderer.width(state.cachedFormattedCount) / 2f, 0, state.cachedOrderedText, false, DisplayMode.POLYGON_OFFSET, state.lightCoords, color, 0, 0);
    matrices.popPose();
  }

  /* > Tweak HIGHLIGHT_DURATION_TICKS to make animation longer/shorter */
  private static float getAnimationDuration(int ticks) {
    return ticks / (float) ParticleBeamRendering.HIGHLIGHT_DURATION_TICKS;
  }

  /* > Tweak return value to change animation easing function */
  private static float getEaseFromTime(float time) {
    return 1 - (1 - time) * (1 - time);
  }

  protected void alignMatricesToOrientation(PoseStack matrices, FrontAndTop orientation) {
    matrices.translate(0.5, 0.5, 0.5);
    switch (orientation) {
      case NORTH_UP:
        matrices.mulPose(Axis.YP.rotationDegrees(180));
        break;
      case SOUTH_UP:
        break;
      case EAST_UP:
        matrices.mulPose(Axis.YP.rotationDegrees(90));
        break;
      case WEST_UP:
        matrices.mulPose(Axis.YP.rotationDegrees(270));
        break;
      case UP_NORTH:
        matrices.mulPose(Axis.YP.rotationDegrees(0));
        matrices.mulPose(Axis.XP.rotationDegrees(270));
        break;
      case UP_EAST:
        matrices.mulPose(Axis.YP.rotationDegrees(270));
        matrices.mulPose(Axis.XP.rotationDegrees(270));
        break;
      case UP_SOUTH:
        matrices.mulPose(Axis.YP.rotationDegrees(180));
        matrices.mulPose(Axis.XP.rotationDegrees(270));
        break;
      case UP_WEST:
        matrices.mulPose(Axis.YP.rotationDegrees(90));
        matrices.mulPose(Axis.XP.rotationDegrees(270));
        break;
      case DOWN_NORTH:
        matrices.mulPose(Axis.YP.rotationDegrees(180));
        matrices.mulPose(Axis.XP.rotationDegrees(90));
        break;
      case DOWN_EAST:
        matrices.mulPose(Axis.YP.rotationDegrees(90));
        matrices.mulPose(Axis.XP.rotationDegrees(90));
        break;
      case DOWN_SOUTH:
        matrices.mulPose(Axis.XP.rotationDegrees(90));
        break;
      case DOWN_WEST:
        matrices.mulPose(Axis.YP.rotationDegrees(270));
        matrices.mulPose(Axis.XP.rotationDegrees(90));
        break;
    }
    matrices.translate(0, 0, 0.51);
  }
}
