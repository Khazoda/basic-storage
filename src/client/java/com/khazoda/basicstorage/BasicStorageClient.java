package com.khazoda.basicstorage;

import com.khazoda.basicstorage.config.ConfigSyncPayload;
import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.renderer.CrateBlockEntityRenderer;
import com.khazoda.basicstorage.renderer.CrateItemRenderer;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;

import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class BasicStorageClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Load Server Config
		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> context.client().execute(() -> {
			BasicStorageConfig.getInstance().setBreakWithAxeOnly(payload.breakWithAxeOnly());
			Constants.BS_LOG.info("Synced config from server: Axe Only = {}", payload.breakWithAxeOnly());
		}));

		// Revert Client Config
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BasicStorageConfig.getInstance().load());

		BlockEntityRendererFactories.register(BlockEntityRegistry.CRATE_BLOCK_ENTITY, CrateBlockEntityRenderer::new);
		CrateItemRenderer crateItemRenderer = new CrateItemRenderer();

		BuiltinItemRendererRegistry.INSTANCE.register(BlockRegistry.CRATE_BLOCK, crateItemRenderer);
		ModelLoadingPlugin.register(crateItemRenderer);
	}
}