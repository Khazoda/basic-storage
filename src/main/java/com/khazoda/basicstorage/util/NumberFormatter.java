package com.khazoda.basicstorage.util;

public class NumberFormatter {
  public static String toFormattedNumber(long value) {
    if (value < 0) return "-" + toFormattedNumber(-value);
    if (value < 1_000) return Long.toString(value);

    String raw = Long.toString(value);
    int firstGroup = raw.length() % 3;

    if (firstGroup == 0) firstGroup = 3;

    StringBuilder builder = new StringBuilder(raw.length() + raw.length() / 3);
    builder.append(raw, 0, firstGroup);

    for (int i = firstGroup; i < raw.length(); i += 3) {
      builder.append(',').append(raw, i, i + 3);
    }

    return builder.toString();
  }

  public static String format(int value) {
    if (value == Integer.MIN_VALUE) return "-2.1B";
    if (value < 0) return "-" + format(-value);
    if (value < 100_000) return toFormattedNumber(value);
    if (value >= 1_000_000_000) return formatSuffix(value, 1_000_000_000, "B");
    if (value >= 1_000_000) return formatSuffix(value, 1_000_000, "M");
    return formatSuffix(value, 1_000, "K");
  }

  private static String formatSuffix(int value, int divisor, String suffix) {
    long truncated = value / (divisor / 10L);
    if (truncated < 100 && truncated % 10 != 0) return (truncated / 10.0) + suffix;
    return (truncated / 10) + suffix;
  }
}