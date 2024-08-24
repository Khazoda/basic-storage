package com.khazoda.basicstorage;

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

    Registry.register(Registries.ITEM_GROUP, new Identifier(Constants.BS_NAMESPACE), BW_ITEMGROUP);
    CrateContentsLootFunction.register();
    BlockRegistry.init();
    BlockEntityRegistry.init();
    SoundRegistry.init();
    EventRegistry.init();

    ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(content -> content.addAfter(Items.BARREL, BlockRegistry.CRATE_BLOCK));
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> content.addAfter(Items.BARREL, BlockRegistry.CRATE_BLOCK));

    BS_LOG.info("[Basic Storage] {}/5 Crates filled!", loadedRegistries);
  }
}