package com.khazoda.basicstorage.util;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class NumberFormatter {

	private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("###,###,###,###");

	public static String toFormattedNumber(double value) {
		return COMMA_FORMAT.format(value);
	}

	private static final NavigableMap<Integer, String> suffixes = new TreeMap<>();
	static {
		suffixes.put(1_000, "K");
		suffixes.put(1_000_000, "M");
		suffixes.put(1_000_000_000, "B");
	}

	public static String format(int value) {
		if (value == Integer.MIN_VALUE) return format(Integer.MIN_VALUE + 1);
		if (value < 0) return "-" + format(-value);
		if (value < 100000) return toFormattedNumber(value);

		Map.Entry<Integer, String> e = suffixes.floorEntry(value);
		Integer divideBy = e.getKey();
		String suffix = e.getValue();

		long truncated = value / (divideBy / 10);
		boolean hasDecimal = truncated < 100 && (truncated / 10d) != ((double) truncated / 10);
		return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
	}
}
