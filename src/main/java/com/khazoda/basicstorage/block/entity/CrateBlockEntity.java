package com.khazoda.basicstorage.block.entity;

import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.registry.DataComponentRegistry;
import com.khazoda.basicstorage.storage.CrateSlot;
import com.khazoda.basicstorage.structure.CrateSlotComponent;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class CrateBlockEntity extends BlockEntity {
	public final CrateSlot storage = new CrateSlot(this);

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

		NbtCompound storageNbt = new NbtCompound();
		storage.writeNbt(storageNbt, registryLookup);
		nbt.put("crateStack", storageNbt);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		if (nbt.contains("crateStack", 10)) storage.readNbt(nbt.getCompound("crateStack"), registryLookup);
	}

	/**
	 * Block Entity Boilerplate
	 */
	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
		NbtCompound nbt = new NbtCompound();
		writeNbt(nbt, registryLookup);
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
	}

	@Override
	protected void readComponents(BlockEntity.ComponentsAccess components) {
		super.readComponents(components);
		CrateSlotComponent contents = components.getOrDefault(DataComponentRegistry.CRATE_CONTENTS, CrateSlotComponent.DEFAULT);

		if (contents.count() <= 0 || contents.item().isBlank()) return;

		try (Transaction t = Transaction.openOuter()) {
			if (!this.storage.isBlank()) return;
			this.storage.insert(contents.item(), contents.count(), t);
			t.commit();
		}
	}
}