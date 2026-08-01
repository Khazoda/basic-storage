package com.khazoda.basicstorage.block.entity;

import com.khazoda.basicstorage.block.CrateBlock;
import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.registry.DataComponentRegistry;
import com.khazoda.basicstorage.storage.CrateNetworkManager;
import com.khazoda.basicstorage.storage.CrateSlot;
import com.khazoda.basicstorage.structure.CrateSlotComponent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class CrateBlockEntity extends BlockEntity implements ItemOwner {

  public final CrateSlot storage = new CrateSlot(this);
  private boolean registeredOnServer = false;
  public static final Codec<CrateSlotComponent> SLOT_CODEC = RecordCodecBuilder.create(instance -> instance.group(ItemVariant.CODEC.fieldOf("item").orElse(ItemVariant.blank()).forGetter(CrateSlotComponent::item), Codec.INT.fieldOf("count").orElse(0).forGetter(CrateSlotComponent::count)).apply(instance, CrateSlotComponent::new));

  public CrateBlockEntity(BlockPos pos, BlockState state) {
    super(BlockEntityRegistry.CRATE_BLOCK_ENTITY, pos, state);
  }

  /**
   * modified markDirty() method
   */
  public void refresh() {
    this.setChanged();
    if (this.level instanceof ServerLevel serverLevel) {
      CrateNetworkManager.get(serverLevel).updateStorage(serverLevel, worldPosition, storage.toComponent());
      BlockState state = this.getBlockState();
      serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
    }
  }

  /**
   * NBT Operations
   * WriteView creates the RegistryOps internally,
   * allowing Enchantments and other registry-dependent data to be saved
   * correctly.
   */
  @Override
  protected void saveAdditional(ValueOutput view) {
    super.saveAdditional(view);
    CrateSlotComponent component = storage.toComponent();
    view.store("crateStack", SLOT_CODEC, component);
    view.store("registered", Codec.BOOL, this.registeredOnServer);
  }

  @Override
  protected void loadAdditional(ValueInput view) {
    super.loadAdditional(view);
    view.read("crateStack", SLOT_CODEC).ifPresent(storage::readComponent);
    view.read("registered", Codec.BOOL).ifPresent(v -> this.registeredOnServer = v);
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    CompoundTag nbt = this.saveCustomOnly(registries);
    nbt.putBoolean("registered", this.registeredOnServer);
    return nbt;
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  /**
   * Data to save and read from ItemStack versions of crate
   */
  @Override
  protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
    if (this.storage.isBlank() || this.storage.getAmount() <= 0) return;
    componentMapBuilder.set(DataComponentRegistry.CRATE_CONTENTS, new CrateSlotComponent(this.storage.getResource(), (int) this.storage.getAmount()));
  }

  @Override
  protected void applyImplicitComponents(DataComponentGetter components) {
    CrateSlotComponent contents = components.getOrDefault(DataComponentRegistry.CRATE_CONTENTS, CrateSlotComponent.DEFAULT);
    if (contents == null || contents.count() == 0) return;
    try (Transaction t = Transaction.openOuter()) {
      if (!this.storage.isBlank()) return; // Prevents creative block pick from duping items
      this.storage.insert(contents.item(), contents.count(), t);
      t.commit();
    }
  }

  public boolean isRegistered() {
    return this.registeredOnServer;
  }

  public Level level() {
    return this.level;
  }

  public Vec3 position() {
    return Vec3.atCenterOf(this.getBlockPos());
  }

  public float getVisualRotationYInDegrees() {
    return this.getBlockState().getValue(CrateBlock.FACING).getOpposite().toYRot();
  }

  @Override
  public void setRemoved() {
    if (this.level instanceof ServerLevel serverLevel) {
      if (serverLevel.isLoaded(this.worldPosition) && !this.level.getBlockState(this.worldPosition).is(this.getBlockState().getBlock())) {
        CrateNetworkManager.get(serverLevel).onBlockRemoved(this.level, this.worldPosition);
      }
    }
    super.setRemoved();
  }
}
