package com.khazoda.basicstorage.renderer;

import com.khazoda.basicstorage.block.CrateBlock;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.mixin.RenderSystemAccessor;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.Orientation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CrateBlockEntityRenderer implements BlockEntityRenderer<CrateBlockEntity> {
	private static final Quaternionf ITEM_LIGHT_ROTATION_3D = RotationAxis.POSITIVE_X.rotationDegrees(-15).mul(RotationAxis.POSITIVE_Y.rotationDegrees(15));
	private static final Quaternionf ITEM_LIGHT_ROTATION_FLAT = RotationAxis.POSITIVE_X.rotationDegrees(-45);

	private static final Quaternionf ROT_X_90 = RotationAxis.POSITIVE_X.rotationDegrees(90);
	private static final Quaternionf ROT_X_180 = RotationAxis.POSITIVE_X.rotationDegrees(180);
	private static final Quaternionf ROT_X_270 = RotationAxis.POSITIVE_X.rotationDegrees(270);
	private static final Quaternionf ROT_Y_90 = RotationAxis.POSITIVE_Y.rotationDegrees(90);
	private static final Quaternionf ROT_Y_180 = RotationAxis.POSITIVE_Y.rotationDegrees(180);
	private static final Quaternionf ROT_Y_270 = RotationAxis.POSITIVE_Y.rotationDegrees(270);

	private static final int LABEL_RENDER_DISTANCE = 24;
	private static final double RENDER_DISTANCE_SQUARED = LABEL_RENDER_DISTANCE * LABEL_RENDER_DISTANCE;

	private final ItemRenderer itemRenderer;
	private final TextRenderer textRenderer;

	public CrateBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this.itemRenderer = context.getItemRenderer();
		this.textRenderer = context.getTextRenderer();
	}

	@Override
	public void render(CrateBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		if (be.storage.isBlank()) return;

		World world = be.getWorld();
		if (world == null) return;

		BlockPos pos = be.getPos();
		BlockState state = be.getCachedState();

		Orientation orientation = state.get(CrateBlock.ORIENTATION);
		Direction dir = orientation.getFacing();

		if (!isVisibleToCamera(pos, dir)) return;

		BlockPos facePos = pos.offset(dir);
		if (!Block.shouldDrawSide(state, world, pos, dir, facePos)) return;

		matrices.push();
		alignMatricesToOrientation(matrices, orientation);

		int faceLight = WorldRenderer.getLightmapCoordinates(world, facePos);

		renderText(be.getDisplayAmountText(), faceLight, matrices, vertexConsumers);
		renderItem(be.getDisplayStack(), faceLight, matrices, vertexConsumers, world, (int) pos.asLong());

		matrices.pop();
	}

	@Override
	public int getRenderDistance() {
		return LABEL_RENDER_DISTANCE;
	}

	private void alignMatricesToOrientation(MatrixStack matrices, Orientation orientation) {
		matrices.translate(0.5, 0.5, 0.5);

		switch (orientation) {
			case NORTH_UP -> matrices.multiply(ROT_Y_180);
			case SOUTH_UP -> { }
			case EAST_UP -> matrices.multiply(ROT_Y_90);
			case WEST_UP -> matrices.multiply(ROT_Y_270);
			case UP_NORTH -> matrices.multiply(ROT_X_270);
			case UP_EAST -> {
				matrices.multiply(ROT_Y_270);
				matrices.multiply(ROT_X_270);
			}
			case UP_SOUTH -> {
				matrices.multiply(ROT_Y_180);
				matrices.multiply(ROT_X_270);
			}
			case UP_WEST -> {
				matrices.multiply(ROT_Y_90);
				matrices.multiply(ROT_X_270);
			}
			case DOWN_NORTH -> {
				matrices.multiply(ROT_Y_180);
				matrices.multiply(ROT_X_90);
			}
			case DOWN_EAST -> {
				matrices.multiply(ROT_Y_90);
				matrices.multiply(ROT_X_90);
			}
			case DOWN_SOUTH -> matrices.multiply(ROT_X_90);
			case DOWN_WEST -> {
				matrices.multiply(ROT_Y_270);
				matrices.multiply(ROT_X_90);
			}
		}

		matrices.translate(0, 0, 0.51);
	}

	private void renderItem(ItemStack stack, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int seed) {
		if (stack.isEmpty()) return;

		matrices.push();
		matrices.translate(0f, 0.125f, 0f);
		matrices.scale(0.375f, 0.375f, 0.005f);

		BakedModel model = itemRenderer.getModel(stack, world, null, seed);

		Vector3f[] shaderLights = RenderSystemAccessor.getShaderLightDirections();
		Vector3f oldLight0 = shaderLights[0];
		Vector3f oldLight1 = shaderLights[1];

		try {
			if (model.isSideLit()) {
				matrices.peek().getNormalMatrix().rotate(ITEM_LIGHT_ROTATION_3D);
				DiffuseLighting.enableGuiDepthLighting();
			} else {
				matrices.peek().getNormalMatrix().rotate(ITEM_LIGHT_ROTATION_FLAT);
				DiffuseLighting.disableGuiDepthLighting();
			}

			itemRenderer.renderItem(stack, ModelTransformationMode.GUI, false, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV, model);
		} finally {
			shaderLights[0] = oldLight0;
			shaderLights[1] = oldLight1;
			matrices.pop();
		}
	}

	private void renderText(String formattedCount, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		matrices.push();
		matrices.multiply(ROT_X_180);
		matrices.translate(0f, 0.21f, -0.01f);
		matrices.scale(0.02f, 0.02f, 0.02f);

		textRenderer.draw(formattedCount, -textRenderer.getWidth(formattedCount) / 2f, 0, 0xFFDD99, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x000000, light);

		matrices.pop();
	}

	private boolean isVisibleToCamera(BlockPos pos, Direction facing) {
		Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

		double centerX = pos.getX() + 0.5;
		double centerY = pos.getY() + 0.5;
		double centerZ = pos.getZ() + 0.5;

		double dx = cameraPos.x - centerX;
		double dy = cameraPos.y - centerY;
		double dz = cameraPos.z - centerZ;

		double dot = dx * facing.getOffsetX() + dy * facing.getOffsetY() + dz * facing.getOffsetZ();

		if (dot <= 0.0) return false;

		return dx * dx + dy * dy + dz * dz <= RENDER_DISTANCE_SQUARED;
	}
}