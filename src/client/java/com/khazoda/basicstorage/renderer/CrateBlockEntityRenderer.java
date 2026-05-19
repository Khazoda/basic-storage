package com.khazoda.basicstorage.renderer;

import com.khazoda.basicstorage.block.CrateBlock;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.mixin.RenderSystemAccessor;
import com.khazoda.basicstorage.util.NumberFormatter;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.Orientation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
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

	private final ItemRenderer itemRenderer;
	private final TextRenderer textRenderer;

	public CrateBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this.itemRenderer = context.getItemRenderer();
		this.textRenderer = context.getTextRenderer();
	}

	@Override
	public void render(CrateBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		if (be.storage.isBlank()) return;

		Orientation orientation = be.getCachedState().get(CrateBlock.ORIENTATION);
		Direction dir = orientation.getFacing();

		World world = be.getWorld();
		if (world == null) return;

		BlockPos pos = be.getPos();
		if (!shouldRenderBE(be, dir)) return;

		matrices.push();
		alignMatricesToOrientation(matrices, orientation);

		renderCrateInfo(be.storage.getResource(), (int)be.storage.getAmount(), matrices, vertexConsumers, WorldRenderer.getLightmapCoordinates(world, pos.offset(dir)), (int)pos.asLong(), pos, world);

		matrices.pop();
	}

	protected void alignMatricesToOrientation(MatrixStack matrices, Orientation orientation) {
		matrices.translate(0.5, 0.5, 0.5);
		switch (orientation) {
			case NORTH_UP -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
			case SOUTH_UP -> {}
			case EAST_UP  -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
			case WEST_UP  -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270));
			case UP_NORTH -> {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(270));
			}
			case UP_EAST -> {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(270));
			}
			case UP_SOUTH -> {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(270));
			}
			case UP_WEST -> {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(270));
			}
			case DOWN_NORTH -> {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
			}
			case DOWN_EAST -> {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
			}
			case DOWN_SOUTH -> {
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
			}
			case DOWN_WEST -> {
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
			}
		}
		matrices.translate(0, 0, 0.51);
	}

	public void renderCrateInfo(ItemVariant item, int amount, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int seed, BlockPos pos, World world) {
		if (amount == 0) return;

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		Vec3d playerPos = player == null ? Vec3d.ofCenter(pos) : player.getPos();
		int distance = 0;
		if (player != null) {
			if (player.isUsingSpyglass()) {
				distance = 100;
			} else {
				distance = 40;
			}
		}
		if (pos.isWithinDistance(playerPos, distance)) {
			renderText(amount, light, matrices, vertexConsumers);
			renderItem(item, light, matrices, vertexConsumers, world, seed);
		}
	}

	public void renderItem(ItemVariant item, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int seed) {
		if (item.isBlank()) return;

		matrices.push();
		matrices.translate(0f, 0.125f, 0f);
		matrices.scale(0.5f, 0.5f, 0.5f);
		matrices.scale(0.75f, 0.75f, 1);
		matrices.scale(1f, 1f, 0.01f);

		ItemStack stack = item.toStack();
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

	public void renderText(int count, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
		matrices.translate(0f, 0.21f, -0.01f);

		String formattedCount = NumberFormatter.format(count);

		matrices.scale(0.02f, 0.02f, 0.02f);
		textRenderer.draw(formattedCount, -textRenderer.getWidth(formattedCount) / 2f, 0, 0xFFDD99, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x000000, light);
		matrices.pop();
	}

	public final boolean shouldRenderBE(BlockEntity be, Direction facing) {
		World world = be.getWorld();
		if (world == null) return false;

		BlockPos pos = be.getPos();
		BlockState state = be.getCachedState();

		return Block.shouldDrawSide(state, world, pos, facing, pos.offset(facing));
	}
}