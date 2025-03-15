package com.khazoda.basicstorage.datagen;

import com.khazoda.basicstorage.registry.BlockRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.CopyNameLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;

public class CrateLootTableProvider extends FabricBlockLootTableProvider {

  protected CrateLootTableProvider(FabricDataOutput dataOutput) {
    super(dataOutput);
  }

  @Override
  public void generate() {
    addDrop(BlockRegistry.CRATE_BLOCK, this::drawerDrops);
    addDrop(BlockRegistry.CRATE_STATION_BLOCK);

  }

  private LootTable.Builder drawerDrops(Block drop) {
    return LootTable.builder().pool(addSurvivesExplosionCondition(drop, LootPool.builder().rolls(ConstantLootNumberProvider.create(1.0f))
        .with(ItemEntry.builder(drop)
            .apply(CopyNameLootFunction.builder(CopyNameLootFunction.Source.BLOCK_ENTITY))
            .apply(CrateContentsLootFunction.builder()))));
  }
}
