package com.khazoda.basicstorage.registry;

import com.khazoda.basicstorage.BasicStorage;
import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.structure.CrateSlotComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class DataComponentRegistry {
  public static final ComponentType<CrateSlotComponent> CRATE_CONTENTS = ComponentType.<CrateSlotComponent>builder()
      .codec(CrateSlotComponent.CODEC)
      .packetCodec(CrateSlotComponent.PACKET_CODEC)
      .cache()
      .build();

  public static void init() {
    Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Constants.BS_NAMESPACE, "crate_contents"), CRATE_CONTENTS);
    BasicStorage.loadedRegistries += 1;
  }
}
