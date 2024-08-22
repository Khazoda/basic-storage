package com.khazoda.basicstorage;

import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.renderer.CrateBlockEntityRenderer;
import com.khazoda.basicstorage.renderer.CrateItemRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class BasicStorageClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    BlockEntityRendererFactories.register(BlockEntityRegistry.CRATE_BLOCK_ENTITY, CrateBlockEntityRenderer::new);
    BuiltinItemRendererRegistry.INSTANCE.register(BlockRegistry.CRATE_BLOCK, new CrateItemRenderer());
    ModelLoadingPlugin.register(new CrateItemRenderer());

    /* Version Get & Wiki commands */
    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess)
            -> dispatcher.register(ClientCommandManager.literal("basicstorage")
            .executes(context -> {
              context.getSource().sendFeedback(Text.translatable("command.basicstorage.root").append(Text.literal(Constants.BS_VERSION).styled(s ->
                  s.withColor(0x00FFFF))));
                  return 1;
                }
            )
            .then(ClientCommandManager.literal("wiki")
                .executes(context -> {
                  context.getSource().sendFeedback(Text.translatable("command.basicstorage.wiki").setStyle(Style.EMPTY.withColor(Formatting.BLUE).withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modded.wiki/w/Mod:Basic_Storage"))));
                  return 1;
                })
            )
        )
    );
  }
}