package com.khazoda.basicstorage.block.entity;

import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.registry.DataComponentRegistry;
import com.khazoda.basicstorage.storage.CrateSlot;
import com.khazoda.basicstorage.structure.CrateSlotComponent;
import com.khazoda.basicstorage.util.NumberFormatter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class CrateBlockEntity extends BlockEntity {
  private static final String CRATE_STACK_NBT_KEY = "crateStack";
  public final CrateSlot storage = new CrateSlot(this);
  private ItemVariant cachedDisplayVariant = ItemVariant.blank();
  private ItemStack cachedDisplayStack = ItemStack.EMPTY;

  private int cachedDisplayAmount = Integer.MIN_VALUE;
  private String cachedDisplayAmountText = "";

  /**
   * Constructor
   **/
  public CrateBlockEntity(BlockPos pos, BlockState state) {
    super(BlockEntityRegistry.CRATE_BLOCK_ENTITY, pos, state);
  }

  /**
   * modified markDirty() method
   */
  public void refresh() {
    if (world instanceof ServerWorld) {
      world.getWorldChunk(pos).setNeedsSaving(true);

      BlockState state = getCachedState();

      world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
      world.updateComparators(pos, state.getBlock());
    }
  }

  /**
   * NBT Operations
   **/
  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    super.writeNbt(nbt, registryLookup);
    writeCrateStorageNbt(nbt, registryLookup);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    super.readNbt(nbt, registryLookup);

    if (nbt.contains(CRATE_STACK_NBT_KEY, NbtElement.COMPOUND_TYPE)) {
      storage.readNbt(nbt.getCompound(CRATE_STACK_NBT_KEY), registryLookup);
      return;
    }

    storage.clear();
  }

  /**
   * Block Entity Boilerplate
   */
  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
    NbtCompound nbt = new NbtCompound();

    // This is required because somehow the client doesn't react to empty nbt packets
    if (storage.isBlank()) {
      nbt.putBoolean("empty", true);
      return nbt;
    }

    writeCrateStorageNbt(nbt, registryLookup);
    return nbt;
  }

  @Override
  public BlockEntityUpdateS2CPacket toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }

  /**
   * Data to save and read from ItemStack versions of crate
   */
  @Override
  protected void addComponents(ComponentMap.Builder componentMapBuilder) {
    super.addComponents(componentMapBuilder);
    if (this.storage.isBlank()) return;

    componentMapBuilder.add(DataComponentRegistry.CRATE_CONTENTS, new CrateSlotComponent(this.storage.getResource(), (int) this.storage.getAmount()));
    componentMapBuilder.add(DataComponentTypes.MAX_STACK_SIZE, 1);
  }

  @Override
  protected void readComponents(BlockEntity.ComponentsAccess components) {
    super.readComponents(components);
    CrateSlotComponent contents = components.getOrDefault(DataComponentRegistry.CRATE_CONTENTS, CrateSlotComponent.DEFAULT);

    if (!this.storage.isBlank() || contents.count() <= 0 || contents.item().isBlank()) return;

    this.storage.setStoredContents(contents.item(), contents.count());
  }

  /**
   * NBT Helper
   */
  private void writeCrateStorageNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    if (storage.isBlank()) return;

    NbtCompound storageNbt = new NbtCompound();
    storage.writeNbt(storageNbt, registryLookup);
    nbt.put(CRATE_STACK_NBT_KEY, storageNbt);
  }

  /**
   * Gets the ItemStack and caches it for the renderer
   */
  public ItemStack getDisplayStack() {
    if (storage.isBlank()) {
      cachedDisplayVariant = ItemVariant.blank();
      cachedDisplayStack = ItemStack.EMPTY;
      return ItemStack.EMPTY;
    }

    ItemVariant variant = storage.getResource();

    if (!variant.equals(cachedDisplayVariant)) {
      cachedDisplayVariant = variant;
      cachedDisplayStack = variant.toStack();
    }

    return cachedDisplayStack;
  }

  /**
   * Gets the item display amount and caches it for the renderer
   */
  public String getDisplayAmountText() {
    if (storage.isBlank()) {
      cachedDisplayAmount = 0;
      cachedDisplayAmountText = "";
      return "";
    }

    int amount = (int) storage.getAmount();

    if (amount != cachedDisplayAmount) {
      cachedDisplayAmount = amount;
      cachedDisplayAmountText = NumberFormatter.format(amount);
    }

    return cachedDisplayAmountText;
  }
}