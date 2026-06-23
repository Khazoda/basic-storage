package com.khazoda.basicstorage.renderer;

import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.registry.DataComponentRegistry;
import com.khazoda.basicstorage.structure.CrateSlotComponent;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class CrateItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer, ModelLoadingPlugin {
  public static final Identifier CRATE_ID = Identifier.of(Constants.BS_NAMESPACE, "block/crate");

  private static final float CONTENT_X_OFFSET = 0.5f;
  private static final float CONTENT_Y_OFFSET = 0.5f;
  private static final float CONTENT_Z_OFFSET = 1.0f;

  private static final float CONTENT_SCALE_XY = 0.7f;
  private static final float CONTENT_SCALE_Z = 1.0f;

  @Override
  public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light, int overlay) {
    MinecraftClient client = MinecraftClient.getInstance();
    ItemRenderer itemRenderer = client.getItemRenderer();

    BakedModelManager modelManager = client.getBakedModelManager();
    BakedModel crateModel = modelManager.getModel(CRATE_ID);

    CrateSlotComponent contents = stack.get(DataComponentRegistry.CRATE_CONTENTS);
    boolean renderContents = mode == ModelTransformationMode.GUI && contents != null && !contents.item().isBlank();

    renderCrate(stack, mode, matrices, vertexConsumerProvider, light, overlay, itemRenderer, crateModel);

    if (renderContents) renderCrateContents(itemRenderer, contents.item(), light, matrices, vertexConsumerProvider);
  }

  private void renderCrate(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, ItemRenderer itemRenderer, BakedModel crateModel) {
    matrices.push();
    matrices.translate(0.5, 0.5, 0.5);
    itemRenderer.renderItem(stack, mode, false, matrices, vertexConsumerProvider, light, overlay, crateModel);
    matrices.pop();
  }

  private void renderCrateContents(ItemRenderer itemRenderer, ItemVariant item, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
    if (item.isBlank()) return;

    matrices.push();
    matrices.translate(CONTENT_X_OFFSET, CONTENT_Y_OFFSET, CONTENT_Z_OFFSET);
    matrices.scale(CONTENT_SCALE_XY, CONTENT_SCALE_XY, CONTENT_SCALE_Z);

    ItemStack stack = item.toStack();
    BakedModel model = itemRenderer.getModel(stack, null, null, 0);

    itemRenderer.renderItem(stack, ModelTransformationMode.GUI, false, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV, model);

    matrices.pop();
  }

  @Override
  public void onInitializeModelLoader(Context pluginContext) {
    pluginContext.addModels(CRATE_ID);
  }
}