package com.khazoda.basicstorage.renderer;

import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Map;

public class CrateBuiltinItemRendererRegistry {
	private static final Map<Item, CrateItemRenderer> renderers = new HashMap<>();

	public static void register(Item item, CrateItemRenderer renderer) {
		renderers.put(item, renderer);
	}

	public CrateItemRenderer rendererOf(Item item) {
		return renderers.get(item);
	}
}
