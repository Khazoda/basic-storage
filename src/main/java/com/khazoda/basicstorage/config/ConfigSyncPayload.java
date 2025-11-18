package com.khazoda.basicstorage.config;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static com.khazoda.basicstorage.Constants.BS_NAMESPACE;

public record ConfigSyncPayload(boolean breakWithAxeOnly) implements CustomPayload {

  public static final CustomPayload.Id<ConfigSyncPayload> ID = new CustomPayload.Id<>(Identifier.of(BS_NAMESPACE, "config_sync"));
  public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC = PacketCodec.tuple(
      PacketCodecs.BOOL, ConfigSyncPayload::breakWithAxeOnly,
      ConfigSyncPayload::new
  );

  @Override
  public CustomPayload.Id<? extends CustomPayload> getId() {
    return ID;
  }
}