package com.khazoda.basicstorage.renderer;

import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.mixin.RenderSystemAccessor;
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

    if (!mode.equals(ModelTransformationMode.GUI) || !stack.getOrCreateNbt().contains("crate_contents")) {
      // Render crate crateModel normally
      renderCrate(stack, mode, matrices, vertexConsumerProvider, light, overlay, itemRenderer, crateModel, false);
    } else {
      // Render create crateModel in GUI with extra information
      if (stack.getOrCreateNbt().contains("crate_contents")) {
        renderCrate(stack, mode, matrices, vertexConsumerProvider, light, overlay, itemRenderer, crateModel, true);
        ItemVariant item = Objects.requireNonNull(ItemVariant.fromNbt(stack.getOrCreateNbt().getCompound("crate_contents").getCompound("item")));
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
    crateModel.getTransformation().getTransformation(mode).apply(false, matrices);
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

    var lights = new Vector3f[2];
    System.arraycopy(RenderSystemAccessor.getShaderLightDirections(), 0, lights, 0, 2);

    if (model.isSideLit()) {
      matrices.peek().getNormalMatrix().rotate(ITEM_LIGHT_ROTATION_3D);
      DiffuseLighting.enableGuiDepthLighting();
    } else {
      matrices.peek().getNormalMatrix().rotate(ITEM_LIGHT_ROTATION_FLAT);
      DiffuseLighting.disableGuiDepthLighting();
    }

    itemRenderer.renderItem(stack, ModelTransformationMode.GUI, false, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV, model);

    System.arraycopy(lights, 0, RenderSystemAccessor.getShaderLightDirections(), 0, 2);
    matrices.pop();
  }

  private static void flipHorizontally(MatrixStack matrix) {
    /* Global operation applied to second rotation (quaternions are so weird) */
    matrix.multiply(new Quaternionf(0.24, -0.37, -0.1, 0.89)); // rotates face to the front left face
    matrix.multiply(new Quaternionf(0.24, 0.37, 0.1, 0.89).invert()); // rotates back to base orientation
  }

  @Override
  public void onInitializeModelLoader(Context pluginContext) {
    pluginContext.addModels(CRATE_ID);
  }
}
