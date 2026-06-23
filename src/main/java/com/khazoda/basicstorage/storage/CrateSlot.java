package com.khazoda.basicstorage.storage;

import com.khazoda.basicstorage.BasicStorageConfig;
import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

import static com.khazoda.basicstorage.block.CrateBlock.canInsert;
import static com.khazoda.basicstorage.storage.CrateStationHelper.notifyNearbyStations;

public final class CrateSlot extends SnapshotParticipant<CrateSlot.Snapshot>
    implements SingleSlotStorage<ItemVariant>, CrateStorage {
  private final CrateBlockEntity owner;
  private ItemVariant item = ItemVariant.blank();
  private int count;

  public CrateSlot(CrateBlockEntity owner) {
    this.owner = owner;
  }

  @Override
  public CrateBlockEntity getOwner() {
    return owner;
  }

  @Override
  public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    if (maxAmount <= 0 || resource.isBlank()) return 0;

    boolean wasBlank = isBlank();

    if (wasBlank) {
      if (!canInsert(resource.toStack(), this, false)) return 0;
    } else if (!resource.equals(item)) {
      return 0;
    }

    int capacity = BasicStorageConfig.getInstance().crateMaxCapacity();
    int inserted = (int) Math.min((long) capacity - count, maxAmount);
    if (inserted <= 0) return 0;

    updateSnapshots(transaction);
    count += inserted;

    if (wasBlank) {
      item = resource;

      transaction.addOuterCloseCallback(result -> {
        if (result.wasCommitted() && owner.getWorld() != null) notifyNearbyStations(owner.getWorld(), owner.getPos());
      });
    }

    return inserted;
  }

  @Override
  public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    if (maxAmount <= 0 || !resource.equals(item) || count <= 0) return 0;

    int extracted = (int) Math.min(count, maxAmount);
    boolean willBeBlank = count == extracted;

    updateSnapshots(transaction);
    count -= extracted;

    if (willBeBlank) {
      item = ItemVariant.blank();

      transaction.addOuterCloseCallback(result -> {
        if (result.wasCommitted() && owner.getWorld() != null) notifyNearbyStations(owner.getWorld(), owner.getPos());
      });
    }

    return extracted;
  }

  @Override
  public boolean isResourceBlank() {
    return item.isBlank();
  }

  @Override
  public ItemVariant getResource() {
    return item;
  }

  @Override
  public long getAmount() {
    return count;
  }

  @Override
  public long getCapacity() {
    return Math.max(count, BasicStorageConfig.getInstance().crateMaxCapacity());
  }

  @Override
  protected Snapshot createSnapshot() {
    return new Snapshot(new ResourceAmount<>(item, count));
  }

  @Override
  protected void readSnapshot(Snapshot snapshot) {
    item = snapshot.contents.resource();
    count = (int) snapshot.contents.amount();
  }

  @Override
  protected void onFinalCommit() {
    update();
  }

  @Override
  public boolean isBlank() {
    return item.isBlank() || count <= 0;
  }

  public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, registryLookup);
    ItemVariant variant = ItemVariant.CODEC.parse(ops, nbt.getCompound("item")).result().orElse(ItemVariant.blank());

    setStoredContents(variant, nbt.getLong("count"));
  }

  public void setStoredContents(ItemVariant item, long count) {
    if (item.isBlank() || count <= 0) {
      clear();
      return;
    }

    this.item = item;
    this.count = Math.clamp(count, 0, Constants.CRATE_MAX_COUNT);
  }

  public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    nbt.put("item", ItemVariant.CODEC.encodeStart(RegistryOps.of(NbtOps.INSTANCE, registryLookup), item).getOrThrow());
    nbt.putLong("count", count);
  }

  public void clear() {
    if (item.isBlank() && count == 0) return;

    item = ItemVariant.blank();
    count = 0;
  }

  protected record Snapshot(ResourceAmount<ItemVariant> contents) {
  }
}