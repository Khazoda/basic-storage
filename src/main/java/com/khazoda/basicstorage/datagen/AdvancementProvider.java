package com.khazoda.basicstorage.datagen;

import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.registry.CriterionRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AdvancementProvider extends FabricAdvancementProvider {

  protected AdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
    super(output, registryLookup);
  }

  @Override
  public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {
    AdvancementHolder packRat = Advancement.Builder.advancement()
        .parent(Identifier.fromNamespaceAndPath("minecraft", "story/mine_stone"))
        .display(
            BlockRegistry.CRATE_BLOCK,
            Component.translatable("advancement.basicstorage.pack_rat.title"),
            Component.translatable("advancement.basicstorage.pack_rat.desc"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
        )
        .addCriterion("has_crate", InventoryChangeTrigger.TriggerInstance.hasItems(BlockRegistry.CRATE_BLOCK))
        .save(consumer, Constants.ID("pack_rat").toString());

    AdvancementHolder foundation = Advancement.Builder.advancement()
        .parent(packRat)
        .display(
            BlockRegistry.CRATE_STATION_FRAME_BLOCK,
            Component.translatable("advancement.basicstorage.empty_frame.title"),
            Component.translatable("advancement.basicstorage.empty_frame.desc"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
        )
        .addCriterion("has_frame", InventoryChangeTrigger.TriggerInstance.hasItems(BlockRegistry.CRATE_STATION_FRAME_BLOCK))
        .save(consumer, Constants.ID("empty_frame").toString());

    AdvancementHolder organization = Advancement.Builder.advancement()
        .parent(foundation)
        .display(
            BlockRegistry.CRATE_STATION_BLOCK,
            Component.translatable("advancement.basicstorage.eye_for_organization.title"),
            Component.translatable("advancement.basicstorage.eye_for_organization.desc"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
        )
        .addCriterion("transformed", CriterionRegistry.STATION_TRANSFORMED.createCriterion(new com.khazoda.basicstorage.advancement.StationTransformedCriterion.Conditions(java.util.Optional.empty())))
        .save(consumer, Constants.ID("eye_for_organization").toString());

    Advancement.Builder.advancement()
        .parent(organization)
        .display(
            BlockRegistry.CRATE_CONNECTOR_BLOCK,
            Component.translatable("advancement.basicstorage.nodal_networking.title"),
            Component.translatable("advancement.basicstorage.nodal_networking.desc"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
        )
        .addCriterion("has_connector", InventoryChangeTrigger.TriggerInstance.hasItems(BlockRegistry.CRATE_CONNECTOR_BLOCK))
        .save(consumer, Constants.ID("nodal_networking").toString());
  }
}
