package com.khazoda.basicstorage;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static com.khazoda.basicstorage.Constants.BS_LOG;

public class BasicStorageConfig {
  private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("basicstorage.properties");
  private static final String BREAK_WITH_AXE_ONLY_KEY = "break_with_axe_only";
  private static final String CRATE_MAX_CAPACITY_KEY = "crate_max_capacity";

  private static BasicStorageConfig INSTANCE;
  private final Properties properties = new Properties();
  private int crateMaxCapacity = Constants.CRATE_MAX_COUNT;

  private BasicStorageConfig() {
  }

  public static BasicStorageConfig getInstance() {
    if (INSTANCE == null) INSTANCE = new BasicStorageConfig();
    return INSTANCE;
  }

  public void load() {
    try {
      if (!Files.exists(CONFIG_PATH)) {
        Files.createDirectories(CONFIG_PATH.getParent());
        Files.writeString(CONFIG_PATH, "# Basic Storage Configuration\n\n"
            + "# If true, crates can only be broken using an axe.\n"
            + "# If false, crates can be broken with anything.\n"
            + "break_with_axe_only=false\n\n"
            + "# Maximum number of items a crate can accept.\n"
            + "# Lowering this will not delete items from existing over-capacity crates.\n"
            + "crate_max_capacity=1000000000\n\n");
      }

      try (var reader = Files.newBufferedReader(CONFIG_PATH)) {
        properties.load(reader);
      }

      crateMaxCapacity = parseCrateMaxCapacity();
    } catch (IOException e) {
      BS_LOG.error("[Basic Storage] Failed to load config: {}", e.getMessage());
    }
  }

  public boolean breakWithAxeOnly() {
    return Boolean.parseBoolean(properties.getProperty(BREAK_WITH_AXE_ONLY_KEY, "false"));
  }

  public int crateMaxCapacity() {
    return crateMaxCapacity;
  }

  public void setBreakWithAxeOnly(boolean value) {
    // Explicitly not saving this value, as it's just for runtime while connected to a server
    properties.setProperty(BREAK_WITH_AXE_ONLY_KEY, String.valueOf(value));
  }

  private int parseCrateMaxCapacity() {
    try {
      int value = Integer.parseInt(properties.getProperty(CRATE_MAX_CAPACITY_KEY, String.valueOf(Constants.CRATE_MAX_COUNT)).trim());
      if (value >= 1 && value <= Constants.CRATE_MAX_COUNT) return value;
    } catch (NumberFormatException ignored) {
    }

    BS_LOG.warn("[Basic Storage] Invalid config value '{}'. Using {}.", CRATE_MAX_CAPACITY_KEY, Constants.CRATE_MAX_COUNT);
    return Constants.CRATE_MAX_COUNT;
  }
}