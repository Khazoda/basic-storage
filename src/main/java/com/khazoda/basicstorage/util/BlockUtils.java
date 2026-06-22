package com.khazoda.basicstorage.util;

public class BlockUtils {
	public static int getComparatorOutputStrength(int itemStackCount) {
		if (itemStackCount <= 0) return 0;
		return ((itemStackCount - 1) % 15) + 1;
	}
}
