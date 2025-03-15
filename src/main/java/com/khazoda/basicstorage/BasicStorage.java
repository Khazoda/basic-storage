package com.khazoda.basicstorage;

import com.khazoda.basicstorage.config.ModConfig;
import com.khazoda.basicstorage.datagen.CrateContentsLootFunction;
import com.khazoda.basicstorage.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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

    ModConfig.getInstance().load();
    Registry.register(Registries.ITEM_GROUP, new Identifier(Constants.BS_NAMESPACE), BW_ITEMGROUP);
    CrateContentsLootFunction.register();
    BlockRegistry.init();
    ItemRegistry.init();
    BlockEntityRegistry.init();
    SoundRegistry.init();
    EventRegistry.init();

    ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(content -> content.addAfter(Items.BARREL, BlockRegistry.CRATE_BLOCK));
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> content.addAfter(Items.BARREL, BlockRegistry.CRATE_BLOCK));
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> content.addAfter(Items.BARREL, BlockRegistry.CRATE_BLOCK, BlockRegistry.CRATE_STATION_BLOCK));
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> content.addAfter(Items.BRUSH, ItemRegistry.CRATE_HAMMER_ITEM));

    BS_LOG.info("[Basic Storage] {}/6 Crates filled!", loadedRegistries);
  }
}