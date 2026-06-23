package com.khazoda.basicstorage.registry;

import com.khazoda.basicstorage.block.CrateBlock;
import com.khazoda.basicstorage.block.CrateConnectorBlock;
import com.khazoda.basicstorage.block.CrateStationBlock;
import com.khazoda.basicstorage.util.Reggie;
import net.minecraft.block.Block;
import net.minecraft.item.Item;


public class BlockRegistry {
  public static final Item.Settings crateItemSettings = new Item.Settings().maxCount(64).fireproof();

  public static final Block CRATE_BLOCK = Reggie.register("crate", new CrateBlock(), crateItemSettings);
  public static final Block CRATE_STATION_BLOCK = Reggie.register("crate_station", new CrateStationBlock(), crateItemSettings);
  public static final Block CRATE_CONNECTOR_BLOCK = Reggie.register("crate_connector", new CrateConnectorBlock(), crateItemSettings);

  public static void init() {
  }
}
