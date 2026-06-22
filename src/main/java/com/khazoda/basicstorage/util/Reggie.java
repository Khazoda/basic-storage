package com.khazoda.basicstorage.util;

import com.khazoda.basicstorage.Constants;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Reggie {
	public static Identifier newID(String name) {
		return Identifier.of(Constants.BS_NAMESPACE, name);
	}

	public static <B extends Block> B register(String name, B block, Item.Settings itemSettings) {
		return register(newID(name), block, itemSettings);
	}

	public static <B extends Block> B register(Identifier name, B block, Item.Settings itemSettings) {
		BlockItem item = new BlockItem(block, (itemSettings));
		item.appendBlocks(Item.BLOCK_ITEMS, item);

		Registry.register(Registries.BLOCK, name, block);
		Registry.register(Registries.ITEM, name, item);
		return block;
	}
}
