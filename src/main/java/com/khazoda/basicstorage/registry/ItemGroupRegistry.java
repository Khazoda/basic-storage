package com.khazoda.basicstorage.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;


public class ItemGroupRegistry {
  public static ItemGroup createItemGroup() {
    return FabricItemGroup.builder()
        .icon(() -> new ItemStack(BlockRegistry.CRATE_BLOCK))
        .displayName(Text.translatable("basicstorage.itemGroup"))
        .entries((displayContext, entries) -> {
          entries.add(new ItemStack(BlockRegistry.CRATE_BLOCK));
          entries.add(new ItemStack(BlockRegistry.CRATE_STATION_BLOCK));
          entries.add(new ItemStack(BlockRegistry.CRATE_CONNECTOR_BLOCK));
        }).build();
  }
}
