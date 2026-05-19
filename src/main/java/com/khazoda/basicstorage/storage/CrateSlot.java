package com.khazoda.basicstorage.storage;

import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.structure.CrateSlotComponent;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

import static com.khazoda.basicstorage.block.CrateBlock.canInsert;
import static com.khazoda.basicstorage.storage.CrateStationHelper.notifyNearbyStations;

public final class CrateSlot extends SnapshotParticipant<CrateSlot.Snapshot>
		implements SingleSlotStorage<ItemVariant>, CrateStorage {
	private ItemVariant item = ItemVariant.blank();
	private int count;

	private final CrateBlockEntity owner;
	private boolean markedDirty;

	public CrateSlot(CrateBlockEntity owner) {
		this.owner = owner;
		this.markedDirty = false;
	}

	@Override
	public CrateBlockEntity getOwner() {
		return owner;
	}

	public void readComponent(CrateSlotComponent component) {
		item = component.item();
		count = component.count();

		if (item.isBlank()) count = 0;
	}

	public CrateSlotComponent toComponent() {
		return new CrateSlotComponent(item, count);
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		boolean wasBlank = isBlank();
		if (!canInsert(resource.toStack(), this, maxAmount > 1)) return 0;
		if (maxAmount > 1 && !resource.equals(this.getResource()) && !this.isBlank()) return 0;
		int inserted = (int) Math.min(getCapacity() - count, maxAmount);
		if (inserted > 0) {
			updateSnapshots(transaction);
			count += inserted;
			if (item.isBlank()) {
				item = resource;
				this.markedDirty = true;
			}
			if (wasBlank) {
				transaction.addOuterCloseCallback((result) -> {
					if (owner.getWorld() != null) notifyNearbyStations(owner.getWorld(), owner.getPos());
				});
			}
		} else if (inserted < 0) {
			return 0;
		}
		return inserted;
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		long amountBefore = count;
		if (!resource.equals(item)) return 0;
		int extracted = (int) Math.min(count, maxAmount);
		if (extracted > 0) {
			updateSnapshots(transaction);
			count -= extracted;
			if (count == 0) {
				item = ItemVariant.blank();
				this.markedDirty = true;
			}
			if (amountBefore == extracted) {
				transaction.addOuterCloseCallback((result) -> {
					if (owner.getWorld() != null) notifyNearbyStations(owner.getWorld(), owner.getPos());
				});
			}
		} else if (extracted < 0) {
			return 0;
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
		return Constants.CRATE_MAX_COUNT;
	}

	@Override
	protected Snapshot createSnapshot() {
		return new Snapshot(new ResourceAmount<>(item, count), this.markedDirty);
	}

	@Override
	protected void readSnapshot(Snapshot snapshot) {
		item = snapshot.contents.resource();
		count = (int) snapshot.contents.amount();
		this.markedDirty = snapshot.itemChanged;
	}

	@Override
	protected void onFinalCommit() {
		update();
	}

	public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		item = ItemVariant.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, registryLookup), nbt.getCompound("item")).getOrThrow();
		count = (int) nbt.getLong("count");
		if (item.isBlank()) count = 0;
	}

	public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		nbt.put("item", ItemVariant.CODEC.encodeStart(RegistryOps.of(NbtOps.INSTANCE, registryLookup), item).getOrThrow());
		nbt.putLong("count", count);
	}

	@Override
	public boolean isBlank() {
		return isResourceBlank();
	}

	protected record Snapshot(ResourceAmount<ItemVariant> contents, boolean itemChanged) {
	}
}