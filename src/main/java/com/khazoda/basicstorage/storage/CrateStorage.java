package com.khazoda.basicstorage.storage;

import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

public interface CrateStorage extends Storage<ItemVariant> {
  CrateBlockEntity getOwner();

  boolean isBlank();

  long getCapacity();

  default void update() {
    getOwner().refresh();
  }
}
