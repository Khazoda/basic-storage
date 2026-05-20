package com.khazoda.basicstorage.block;

import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.block.entity.CrateStationBlockEntity;
import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.registry.SoundRegistry;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.longs.LongList;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.khazoda.basicstorage.storage.CrateStationHelper.notifyNearbyStations;

/**
 * Right Click
 * > holding stack - Search for nearest crate containing stack item type and
 * deposit stack into it
 * > no valid crate found? - notify user
 * > holding nothing - Display number of connected crates
 * Shift Right Click - Add all items from inventory to crates that match the
 * items
 * > no valid crate found? - notify user
 * Left Click - Nothing
 * Shift Left Click - Nothing
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class CrateStationBlock extends BlockWithEntity implements BlockEntityProvider {
	public static final MapCodec<CrateStationBlock> CODEC = CrateStationBlock.createCodec(CrateStationBlock::new);

	public static final Settings defaultSettings = Settings.create()
			.sounds(BlockSoundGroup.WOOD).strength(3.5f)
			.pistonBehavior(PistonBehavior.BLOCK)
			.instrument(NoteBlockInstrument.BASS)
			.mapColor(MapColor.OAK_TAN);

	public CrateStationBlock(Settings settings) {
		super(settings);
	}

	public CrateStationBlock() {
		this(defaultSettings);
	}

	/**
	 * Event hook instead of onUse() method in order to capture interactions while
	 * sneaking
	 */
	public static void initOnUseMethod() {
		UseBlockCallback.EVENT.register((PlayerEntity player, World world, Hand hand, BlockHitResult hit) -> {
			if (!world.getBlockState(hit.getBlockPos()).isOf(BlockRegistry.CRATE_STATION_BLOCK)) return ActionResult.PASS;
			if (!player.canModifyBlocks() || player.isSpectator()) return ActionResult.PASS;
			if(player.getStackInHand(hand).isOf(BlockRegistry.CRATE_BLOCK.asItem()) && player.isSneaking()) return ActionResult.PASS;
			if (world.isClient()) return ActionResult.SUCCESS;

			BlockPos pos = hit.getBlockPos();

			if (!(world.getBlockEntity(pos) instanceof CrateStationBlockEntity cdbe)) return ActionResult.PASS;

			ItemStack playerStack = player.getStackInHand(hand);
			Item usedItem = playerStack.getItem();
			boolean hadHeldItem = !playerStack.isEmpty();

			int inserted;

			if (player.isSneaking()) {
				inserted = depositInventory(player, cdbe);
			} else {
				if (playerStack.isEmpty()) {
					int connectedCrateCount = cdbe.getConnectedCrateCount();
					player.sendMessage(Text.translatable("message.basicstorage.station.connected_crate_count", connectedCrateCount).withColor(0xDDFF99), true);
					return ActionResult.SUCCESS;
				}

				inserted = depositStack(playerStack, cdbe);
			}

			if (inserted <= 0) {
				player.sendMessage(Text.translatable("message.basicstorage.station.no_matching_crates").withColor(0xFF9999), true);
				world.playSound(null, pos, SoundRegistry.NO_MATCH, SoundCategory.BLOCKS, 1.1f, 1f);
				return ActionResult.CONSUME;
			}

			if (inserted == 1) {
				world.playSound(null, pos, SoundRegistry.INSERT_ONE, SoundCategory.BLOCKS, 1f, 1.05f);
			} else if (inserted <= 64) {
				world.playSound(null, pos, SoundRegistry.INSERT_MANY, SoundCategory.BLOCKS, 1f, 1.05f);
			} else {
				world.playSound(null, pos, SoundRegistry.INSERT_LOADS, SoundCategory.BLOCKS, 1f, 1.05f);
			}

			if (hadHeldItem) player.incrementStat(Stats.USED.getOrCreateStat(usedItem));
			world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);

			return ActionResult.SUCCESS;
		});
	}

	private static int depositStack(ItemStack stack, CrateStationBlockEntity cdbe) {
		int inserted = 0;
		if (stack.isEmpty()) return 0;

		ItemVariant variant = ItemVariant.of(stack);
		LongList compatibleCrates = cdbe.getCrateRegistry().get(variant);
		if (compatibleCrates == null) return 0;

		World world = cdbe.getWorld();
		if (world == null) return 0;

		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int i = 0, size = compatibleCrates.size(); i < size; i++) {
			mutable.set(compatibleCrates.getLong(i));

			if (!(world.getBlockEntity(mutable) instanceof CrateBlockEntity crate)) {
				cdbe.markCacheForUpdate();
				continue;
			}

			try (Transaction transaction = Transaction.openOuter()) {
				inserted = (int) crate.storage.insert(variant, stack.getCount(), transaction);
				if (inserted > 0) {
					stack.decrement(inserted);
					transaction.commit();
					return inserted;
				}
			}
		}
		return inserted;
	}

	private static int depositInventory(PlayerEntity player, CrateStationBlockEntity station) {
		int totalInserted = 0;

		World world = station.getWorld();
		if (world == null) return 0;

		Map<ItemVariant, LongList> crateRegistry = station.getCrateRegistry();
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		DefaultedList<ItemStack> mainInventory = player.getInventory().main;

		for (int slotIndex = 0, invSize = mainInventory.size(); slotIndex < invSize; slotIndex++) {
			ItemStack stack = mainInventory.get(slotIndex);
			if (stack.isEmpty()) continue;

			ItemVariant variant = ItemVariant.of(stack);
			LongList compatibleCrates = crateRegistry.get(variant);

			if (compatibleCrates == null || compatibleCrates.isEmpty()) continue;

			for (int crateIndex = 0, crateCount = compatibleCrates.size(); crateIndex < crateCount; crateIndex++) {
				mutable.set(compatibleCrates.getLong(crateIndex));

				if (!(world.getBlockEntity(mutable) instanceof CrateBlockEntity crate)) {
					station.markCacheForUpdate();
					continue;
				}

				try (Transaction transaction = Transaction.openOuter()) {
					int moved = (int) crate.storage.insert(variant, stack.getCount(), transaction);

					if (moved > 0) {
						transaction.commit();
						stack.decrement(moved);
						totalInserted += moved;

						if (stack.isEmpty()) break;
					}
				}
			}
		}

		return totalInserted;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		notifyNearbyStations(world, pos);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock())) notifyNearbyStations(world, pos);
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CrateStationBlockEntity(pos, state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState();
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public MapCodec<CrateStationBlock> getCodec() {
		return CODEC;
	}
}
