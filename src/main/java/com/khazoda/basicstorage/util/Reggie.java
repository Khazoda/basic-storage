package com.khazoda.basicstorage.util;

import com.khazoda.basicstorage.Constants;
import net.minecraft.block.Block;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class Reggie {
//  /* Register block and item with default item settings */
//  public static <B extends Block> B register(String name, B block, Item.Settings itemSettings) {
//    return RegistryHelper.registerBlock(name, block, itemSettings);
//  }
//
//  /* Register block *with* corresponding item*/
//  public static <I extends BlockItem> BlockItem register(String name, I blockItem) {
//    return RegistryHelper.registerBlockItem(name, blockItem);
//  }
//
//  /* Register block *without* corresponding item */
//  public static <B extends Block> B register(String name, B block) {
//    return RegistryHelper.registerBlockOnly(name, block);
//  }
//
//  /* Register item */
//  public static Item register(String name) {
//    return RegistryHelper.registerItem(name, new Item(new Item.Settings().maxCount(64)));
//  }
//
//  /* Register armour material */
//  public static RegistryEntry<ArmorMaterial> register(String name, ArmorMaterial material) {
//    return RegistryHelper.registerArmorMaterial(name, material);
//  }

	// General use Identifier() maker function
	public static Identifier newID(String name) {
		return Identifier.of(Constants.BS_NAMESPACE, name);
	}

	// Block Registry Helper Functions
	// *******************************
	// 1. Default BlockItem Registration Entrypoint: creates Identifier from ModID & block name
	public static <B extends Block> B register(String name, B block, Item.Settings itemSettings) {
		return register(newID(name), block, itemSettings);
	}

	// 2. Takes identifier and registers block and block items
	public static <B extends Block> B register(Identifier name, B block, Item.Settings itemSettings) {
		BlockItem item = new BlockItem(block, (itemSettings));
		item.appendBlocks(Item.BLOCK_ITEMS, item);

		Registry.register(Registries.BLOCK, name, block);
		Registry.register(Registries.ITEM, name, item);
		return block;
	}

	public static <B extends Block> B register(String name, B block) {
		return register(newID(name), block);
	}

	public static <B extends Block> B register(Identifier name, B block) {
		Registry.register(Registries.BLOCK, name, block);
		return block;
	}

	public static <I extends BlockItem> I register(String name, I blockItem) {
		return register(newID(name), blockItem);
	}

	public static <I extends BlockItem> I register(Identifier name, I blockItem) {
		Registry.register(Registries.ITEM, name, blockItem);
		return blockItem;
	}

	public static <I extends ItemGroup> I register(I itemGroup) {
		Registry.register(Registries.ITEM_GROUP, Identifier.of("basicstorage"), itemGroup);
		return itemGroup;
	}

	// Item Registry Helper Functions
	// ******************************
	public static <I extends Item> I register(String name, I item) {
		return Registry.register(Registries.ITEM, newID(name), item);
	}

	// Register Armor Material
	public static RegistryEntry<ArmorMaterial> register(String name, ArmorMaterial material) {
		return Registry.registerReference(Registries.ARMOR_MATERIAL, newID(name), material);
	}
}
