package com.khazoda.basicstorage.registry;

import com.khazoda.basicstorage.BasicStorage;
import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BlockEntityRegistry {
  public static final BlockEntityType<CrateBlockEntity> CRATE_BLOCK_ENTITY = Registry.register(
      Registries.BLOCK_ENTITY_TYPE, Identifier.of(Constants.BS_NAMESPACE, "crate_block_entity"),
      FabricBlockEntityTypeBuilder.create(CrateBlockEntity::new,
          BlockRegistry.CRATE_BLOCK).build());


  public static void init() {
    /* Lets crates work with hoppers and other item transfer */
    ItemStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.storage, CRATE_BLOCK_ENTITY);

    BasicStorage.loadedRegistries += 1;
  }
}
