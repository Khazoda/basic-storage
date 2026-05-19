package com.khazoda.basicstorage.block;

import com.khazoda.basicstorage.block.entity.CrateBlockEntity;
import com.khazoda.basicstorage.block.entity.CrateStationBlockEntity;
import com.khazoda.basicstorage.registry.BlockRegistry;
import com.khazoda.basicstorage.registry.SoundRegistry;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
public class CrateStationBlock extends BlockWithEntity implements BlockEntityProvider {
	public static final MapCodec<CrateStationBlock> CODEC = CrateStationBlock.createCodec(CrateStationBlock::new);
	public static final Settings defaultSettings = Settings.create().sounds(BlockSoundGroup.WOOD).strength(3.5f)
			.pistonBehavior(PistonBehavior.BLOCK).instrument(NoteBlockInstrument.BASS).mapColor(MapColor.OAK_TAN);

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
			BlockState state = world.getBlockState(pos);
			BlockEntity be = world.getBlockEntity(pos);

			if (be == null) return ActionResult.PASS;

			CrateStationBlockEntity cdbe = (CrateStationBlockEntity) be;
			ItemStack playerStack = player.getMainHandStack();
			int connectedCrateCount = cdbe.getConnectedCrates().size();
			int inserted = 0;

			if (player.isSneaking()) {
				inserted = depositInventory(player, cdbe);
			} else if (!player.isSneaking()) {
				if (playerStack.isEmpty()) {
					if (!world.isClient()) player.sendMessage(Text.translatable("message.basicstorage.station.connected_crate_count", connectedCrateCount).withColor(0xDDFF99), true);
					return ActionResult.PASS;
				}
				inserted = depositStack(player.getStackInHand(hand), cdbe);
			}

			if (!world.isClient()) {
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

				state.updateNeighbors(world, pos, 1);
				cdbe.markDirty();
				player.incrementStat(Stats.USED.getOrCreateStat(playerStack.getItem()));
				world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}

			return ActionResult.SUCCESS;
		});
	}

	private static int depositStack(ItemStack stack, CrateStationBlockEntity cdbe) {
		int inserted = 0;
		if (stack.isEmpty()) return 0;

		ItemVariant variant = ItemVariant.of(stack);
		List<BlockPos> compatibleCrates = cdbe.getCrateRegistry().get(variant);
		if (compatibleCrates == null) return 0;
		World world = cdbe.getWorld();

		for (BlockPos cratePos : new ArrayList<>(compatibleCrates)) {
			if (world == null) return 0; // TODO: if something goes wrong, remove this and see if things work lol
			BlockEntity be = world.getBlockEntity(cratePos);
			if (!(be instanceof CrateBlockEntity crate)) {
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

		for (ItemStack stack : player.getInventory().main) {
			if (stack.isEmpty()) continue;

			ItemVariant variant = ItemVariant.of(stack);
			List<BlockPos> compatibleCrates = station.getCrateRegistry().get(variant);

			if (compatibleCrates == null || compatibleCrates.isEmpty()) continue;

			for (BlockPos cratePos : compatibleCrates) {
				BlockEntity be = world.getBlockEntity(cratePos);

				if (!(be instanceof CrateBlockEntity crate)) continue;

				try (Transaction transaction = Transaction.openOuter()) {
					int moved = (int) crate.storage.insert(variant, stack.getCount(), transaction);

					if (moved > 0) {
						stack.decrement(moved);
						totalInserted += moved;
						transaction.commit();

						if (stack.isEmpty()) break;
					}
				}
			}
		}

		return totalInserted;
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
