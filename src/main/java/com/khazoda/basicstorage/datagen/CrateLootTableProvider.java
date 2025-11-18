package com.khazoda.basicstorage.datagen;

import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.registry.DataComponentRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.CopyComponentsLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class CrateLootTableProvider extends FabricBlockLootTableProvider {

  protected CrateLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
    super(dataOutput, registryLookup);
  }

  @Override
  public void generate() {
    addDrop(BlockRegistry.CRATE_BLOCK, this::drawerDrops);
    addDrop(BlockRegistry.CRATE_STATION_BLOCK);
  }

  private LootTable.Builder drawerDrops(Block drop) {
    return LootTable.builder().pool(LootPool.builder()
        .rolls(ConstantLootNumberProvider.create(1.0f)).with(ItemEntry.builder(drop)
            .apply(CopyComponentsLootFunction.builder(CopyComponentsLootFunction.Source.BLOCK_ENTITY)
                .include(DataComponentRegistry.CRATE_CONTENTS))));
  }
}
