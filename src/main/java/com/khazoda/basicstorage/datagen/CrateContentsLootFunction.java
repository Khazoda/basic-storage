package com.khazoda.basicstorage.datagen;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class CrateContentsLootFunction extends ConditionalLootFunction {

  private static final LootFunctionType TYPE = new LootFunctionType(new Serializer());

  protected CrateContentsLootFunction(LootCondition[] conditions) {
    super(conditions);
  }

  public static void register() {
    Registry.register(Registries.LOOT_FUNCTION_TYPE, new Identifier(Constants.BS_NAMESPACE, "drawer_contents"), TYPE);
  }

  public static ConditionalLootFunction.Builder<?> builder() {
    return builder(CrateContentsLootFunction::new);
  }

  @Override
  protected ItemStack process(ItemStack stack, LootContext context) {
    if (stack.isEmpty()) return stack;
    var blockEntity = context.get(LootContextParameters.BLOCK_ENTITY);
    if (blockEntity instanceof CrateBlockEntity crate && !crate.storage.isBlank()) {
      var nbt = new NbtCompound();
      crate.writeNbt(nbt);
      var itemNbt = BlockItem.getBlockEntityNbt(stack);
      if (itemNbt == null) {
        itemNbt = nbt;
      } else {
        itemNbt.copyFrom(nbt);
      }
      BlockItem.setBlockEntityNbt(stack, BlockEntityRegistry.CRATE_BLOCK_ENTITY, itemNbt);
    }
    return stack;
  }

  @Override
  public LootFunctionType getType() {
    return TYPE;
  }

  public static class Serializer extends ConditionalLootFunction.Serializer<CrateContentsLootFunction> {
    @Override
    public CrateContentsLootFunction fromJson(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
      return new CrateContentsLootFunction(conditions);
    }
  }
}