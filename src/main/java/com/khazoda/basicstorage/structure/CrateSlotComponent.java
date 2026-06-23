package com.khazoda.basicstorage.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record CrateSlotComponent(ItemVariant item, int count) {
  public static final CrateSlotComponent DEFAULT = new CrateSlotComponent(ItemVariant.blank(), 0);

  public static final Codec<CrateSlotComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      ItemVariant.CODEC.fieldOf("item").forGetter(CrateSlotComponent::item),
      Codec.INT.fieldOf("count").forGetter(CrateSlotComponent::count)).apply(instance, CrateSlotComponent::new));

  public static final PacketCodec<RegistryByteBuf, CrateSlotComponent> PACKET_CODEC = PacketCodecs.registryCodec(CODEC);


}