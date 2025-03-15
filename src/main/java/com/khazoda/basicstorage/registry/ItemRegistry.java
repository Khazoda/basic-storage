package com.khazoda.basicstorage.registry;

import com.khazoda.basicstorage.BasicStorage;
import com.khazoda.basicstorage.config.ModConfig;
import com.khazoda.basicstorage.item.CrateHammerItem;
import com.khazoda.basicstorage.util.RegistryHelper;
import net.minecraft.item.Item;

public class ItemRegistry {
  private static Item.Settings createHammerSettings() {
    int durability = ModConfig.getInstance().getCrateHammerDurability();
    Item.Settings settings = new Item.Settings().maxCount(1);
    if (durability > 0) {
      settings.maxDamage(durability);
    }
    return settings;
  }

  public static final Item CRATE_HAMMER_ITEM = RegistryHelper.registerItem("crate_hammer", new CrateHammerItem(createHammerSettings()));

  public static void init() {
    BasicStorage.loadedRegistries += 1;
  }

}
