package com.khazoda.basicstorage.block.entity;

import com.khazoda.basicstorage.registry.BlockEntityRegistry;
import com.khazoda.basicstorage.storage.CrateSlot;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("UnstableApiUsage")
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
      var state = getCachedState();
      world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
      world.updateComparators(pos, state.getBlock());
    }
  }

  /**
   * NBT Operations
   **/
  @Override
  public void writeNbt(NbtCompound nbt) {
    if (this.storage.isBlank()) return;
    var storageNbt = new NbtCompound();
    storage.writeNbt(storageNbt);
    nbt.put("item", this.storage.getResource().toNbt());
    NbtCompound crateContents = new NbtCompound();
    crateContents.putLong("count", this.storage.getAmount());
    nbt.put("crate_contents", crateContents);
    nbt.put("crateStack", storageNbt);
  }


  @Override
  public void readNbt(NbtCompound nbt) {
    if (nbt.contains("crateStack", 10)) {
      storage.readNbt(nbt.getCompound("crateStack"));
      NbtCompound contentsTag = nbt.getCompound("crate_contents");
      if (contentsTag.isEmpty() || contentsTag.getInt("count") == 0) return;
      try (Transaction t = Transaction.openOuter()) {
        if (!this.storage.isBlank()) return; // Prevents creative block pick from duping items
        this.storage.insert(ItemVariant.fromNbt(contentsTag.getCompound("item")), contentsTag.getInt("count"), t);
        t.commit();
      }
    }
    this.refresh();
  }

  /**
   * Block Entity Boilerplate
   */
  @Override
  public NbtCompound toInitialChunkDataNbt() {
    var nbt = new NbtCompound();
    writeNbt(nbt);
    return nbt;
  }

  @Override
  public BlockEntityUpdateS2CPacket toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }
}