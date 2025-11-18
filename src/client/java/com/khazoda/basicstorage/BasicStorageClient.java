package com.khazoda.basicstorage;

import com.khazoda.basicstorage.config.ConfigSyncPayload;
import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.registry.DataComponentRegistry;
import com.khazoda.basicstorage.renderer.CrateBlockEntityRenderer;
import com.khazoda.basicstorage.renderer.CrateItemRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class BasicStorageClient implements ClientModInitializer {
  /* These two fields allow for a predicate that changes the crate item's model if it has items in it */
  public static final Identifier HAS_ITEMS_ID = Identifier.of(Constants.BS_NAMESPACE, "has_items");
  public static final ClampedModelPredicateProvider HAS_ITEMS = ((stack, world, entity, seed) -> stack.get(DataComponentRegistry.CRATE_CONTENTS) == null ? 0 : 1);

  @Override
  public void onInitializeClient() {
    /* Load server's config */
    ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
      context.client().execute(() -> {
        BasicStorageConfig.getInstance().setBreakWithAxeOnly(payload.breakWithAxeOnly());
        Constants.BS_LOG.info("Synced config from server: Axe Only = " + payload.breakWithAxeOnly());
      });
    });
    /* Revert to client's config */
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      BasicStorageConfig.getInstance().load();
    });

    BlockEntityRendererFactories.register(BlockEntityRegistry.CRATE_BLOCK_ENTITY, CrateBlockEntityRenderer::new);
//    ModelPredicateProviderRegistry.register(BlockRegistry.CRATE_BLOCK.asItem(), HAS_ITEMS_ID, HAS_ITEMS);
    BuiltinItemRendererRegistry.INSTANCE.register(BlockRegistry.CRATE_BLOCK, new CrateItemRenderer());
    ModelLoadingPlugin.register(new CrateItemRenderer());
  }
}