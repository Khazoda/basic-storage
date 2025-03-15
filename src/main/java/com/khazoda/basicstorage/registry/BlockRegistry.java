package com.khazoda.basicstorage.registry;

import com.khazoda.basicstorage.BasicStorage;
import com.khazoda.basicstorage.block.CrateBlock;
import com.khazoda.basicstorage.block.CrateStationBlock;
import com.khazoda.basicstorage.config.ModConfig;
import com.khazoda.basicstorage.util.RegistryHelper;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.Instrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;

public class BlockRegistry {
  public static final Item.Settings crateItemSettings = new Item.Settings().maxCount(64).fireproof();

  private static Block.Settings getCrateSettings() {
    Block.Settings settings = Block.Settings.create()
        .sounds(BlockSoundGroup.WOOD)
        .pistonBehavior(PistonBehavior.BLOCK)
        .instrument(Instrument.BASS)
        .mapColor(MapColor.OAK_TAN);

    if (ModConfig.getInstance().isbreakOnlyWithHammer()) {
      settings.strength(-1.0F, 3600000.0F); // Bedrock-like settings
    } else {
      settings.strength(2.5f); // Normal settings
    }

    return settings;
  }

  public static final Block CRATE_BLOCK = register("crate", new CrateBlock(getCrateSettings()), crateItemSettings);
  public static final Block CRATE_STATION_BLOCK = register("crate_station", new CrateStationBlock(), crateItemSettings);

  public static void init() {
    BasicStorage.loadedRegistries += 1;
  }

  /* Register block and item with default item settings */
  private static <B extends Block> B register(String name, B block, Item.Settings itemSettings) {
    return RegistryHelper.registerBlock(name, block, itemSettings);
  }

  /* Register block *with* corresponding item*/
  private static <I extends BlockItem> BlockItem register(String name, I blockItem) {
    return RegistryHelper.registerBlockItem(name, blockItem);
  }

  /* Register block *without* corresponding item */
  private static <B extends Block> B register(String name, B block) {
    return RegistryHelper.registerBlockOnly(name, block);
  }

  /* Register item */
  private static Item register(String name) {
    return RegistryHelper.registerItem(name, new Item(new Item.Settings().maxCount(64)));
  }
}
