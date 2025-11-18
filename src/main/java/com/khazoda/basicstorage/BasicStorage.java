package com.khazoda.basicstorage;

import com.khazoda.basicstorage.config.ConfigSyncPayload;
import com.khazoda.basicstorage.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static com.khazoda.basicstorage.Constants.BS_LOG;

public class BasicStorage implements ModInitializer {
  public static int loadedRegistries = 0;
  public static final ItemGroup BW_ITEMGROUP = ItemGroupRegistry.createItemGroup();

  @Override
  public void onInitialize() {
    BS_LOG.info("[Basic Storage] Filling crates...");

    PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      boolean serverConfigValue = BasicStorageConfig.getInstance().breakWithAxeOnly();
      ServerPlayNetworking.send(handler.getPlayer(), new ConfigSyncPayload(serverConfigValue));
    });
    BasicStorageConfig.getInstance().load();
    Registry.register(Registries.ITEM_GROUP, Identifier.of(Constants.BS_NAMESPACE), BW_ITEMGROUP);
    BlockRegistry.init();
    BlockEntityRegistry.init();
    SoundRegistry.init();
    EventRegistry.init();
    DataComponentRegistry.init();

    ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(content -> content.addAfter(Items.BARREL, BlockRegistry.CRATE_BLOCK, BlockRegistry.CRATE_STATION_BLOCK));
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> content.addAfter(Items.BARREL, BlockRegistry.CRATE_BLOCK, BlockRegistry.CRATE_STATION_BLOCK));

    BS_LOG.info("[Basic Storage] {}/6 registry crates filled!", loadedRegistries);
  }
}