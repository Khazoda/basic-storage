package com.khazoda.basicstorage.storage;

import com.khazoda.basicstorage.Constants;
import com.khazoda.basicstorage.block.entity.CrateBlockEntity;

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

		int inserted = (int) Math.min((long) Constants.CRATE_MAX_COUNT - count, maxAmount);
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
		return Constants.CRATE_MAX_COUNT;
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

	public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		item = ItemVariant.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, registryLookup), nbt.getCompound("item")).result().orElse(ItemVariant.blank());

		long savedCount = nbt.getLong("count");
		count = Math.clamp(savedCount, 0, Constants.CRATE_MAX_COUNT);

		if (item.isBlank() || count == 0) {
			item = ItemVariant.blank();
			count = 0;
		}
	}

	public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		nbt.put("item", ItemVariant.CODEC.encodeStart(RegistryOps.of(NbtOps.INSTANCE, registryLookup), item).getOrThrow());
		nbt.putLong("count", count);
	}

	@Override
	public boolean isBlank() {
		return isResourceBlank();
	}

	protected record Snapshot(ResourceAmount<ItemVariant> contents) { }
}