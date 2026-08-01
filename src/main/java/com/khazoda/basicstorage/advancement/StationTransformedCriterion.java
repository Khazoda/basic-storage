package com.khazoda.basicstorage.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class StationTransformedCriterion extends SimpleCriterionTrigger<StationTransformedCriterion.Conditions> {

  @Override
  public Codec<Conditions> codec() {
    return Conditions.CODEC;
  }

  public void trigger(ServerPlayer player) {
    this.trigger(player, (conditions) -> true);
  }

  public record Conditions(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create((instance) -> instance.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player)).apply(instance, Conditions::new));

    @Override
    public Optional<ContextAwarePredicate> player() {
      return player;
    }
  }
}
