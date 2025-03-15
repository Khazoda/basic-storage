package com.khazoda.basicstorage.registry;

import com.khazoda.basicstorage.BasicStorage;
import com.khazoda.basicstorage.item.CrateHammerItem;
import com.khazoda.basicstorage.util.RegistryHelper;
import net.minecraft.item.Item;

public class ItemRegistry {
  public static final Item.Settings unstackableItemSettings = new Item.Settings().maxCount(1);

  public static final Item CRATE_HAMMER_ITEM = RegistryHelper.registerItem("crate_hammer", new CrateHammerItem(unstackableItemSettings));

  public static void init() {
    BasicStorage.loadedRegistries += 1;
  }

}
