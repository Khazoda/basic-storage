package com.khazoda.basicstorage.renderer;

import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.mixin.RenderSystemAccessor;
import com.khazoda.basicstorage.registry.DataComponentRegistry;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

public class CrateItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer, ModelLoadingPlugin {
	public static final Identifier CRATE_ID = Identifier.of(Constants.BS_NAMESPACE, "block/crate");
	private static final Quaternionf ITEM_LIGHT_ROTATION_3D = RotationAxis.POSITIVE_X.rotationDegrees(-15).mul(RotationAxis.POSITIVE_Y.rotationDegrees(15));
	private static final Quaternionf ITEM_LIGHT_ROTATION_FLAT = RotationAxis.POSITIVE_X.rotationDegrees(-45);

	@Override
	public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light, int overlay) {
		var client = MinecraftClient.getInstance();
		ItemRenderer itemRenderer = client.getItemRenderer();

		BakedModelManager modelManager = client.getBakedModelManager();
		BakedModel crateModel = modelManager.getModel(CRATE_ID);

		if (!mode.equals(ModelTransformationMode.GUI) || !stack.contains(DataComponentRegistry.CRATE_CONTENTS)) {
			// Render crate crateModel normally
			renderCrate(stack, mode, matrices, vertexConsumerProvider, light, overlay, itemRenderer, crateModel, false);
		} else {
			// Render create crateModel in GUI with extra information
			if (stack.contains(DataComponentRegistry.CRATE_CONTENTS)) {
				renderCrate(stack, mode, matrices, vertexConsumerProvider, light, overlay, itemRenderer, crateModel, true);
				ItemVariant item = Objects.requireNonNull(stack.get(DataComponentRegistry.CRATE_CONTENTS)).item();
				renderCrateContents(itemRenderer, item, light, matrices, vertexConsumerProvider);
			}
		}
	}

	private void renderCrate(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, ItemRenderer itemRenderer, BakedModel crateModel, boolean hasContents) {
		matrices.push();
		matrices.translate(.5, .5, .5);
		if (hasContents) {
			itemRenderer.renderItem(stack, mode, false, matrices, vertexConsumerProvider, Math.round(light / 1.5f), overlay, crateModel);
		} else {
			itemRenderer.renderItem(stack, mode, false, matrices, vertexConsumerProvider, light, overlay, crateModel);
		}
		matrices.pop();
	}

	@SuppressWarnings("UnreachableCode")
	private void renderCrateContents(ItemRenderer itemRenderer, ItemVariant item, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		if (item.isBlank()) return;

		matrices.push();
		matrices.translate(0.5f, 0.5f, 1f);
		matrices.scale(0.7f, 0.7f, 1f);
		var stack = item.toStack();
		var model = itemRenderer.getModel(stack, null, null, 0);

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

	@Override
	public void onInitializeModelLoader(Context pluginContext) {
		pluginContext.addModels(CRATE_ID);
	}
}
