package com.khazoda.basicstorage.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static com.khazoda.basicstorage.Constants.BS_LOG;

public class ModConfig {
  private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("basicstorage.properties");
  private static ModConfig INSTANCE;
  private final Properties properties;

  private ModConfig() {
    this.properties = new Properties();
  }

  public static ModConfig getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ModConfig();
    }
    return INSTANCE;
  }

  public void load() {
    try {
      if (!Files.exists(CONFIG_PATH)) {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
          writer.write("# Basic Storage Configuration\n\n");

          writer.write("# If true, crates can only be broken using the Crate Hammer.\n");
          writer.write("# If false, crates can also be broken with an axe or by hand.\n");
          writer.write("breakOnlyWithHammer=false\n\n");

          writer.write("# The durability of the Crate Hammer.\n");
          writer.write("# Default: 0 (Does not lose durability; Any value above 0 will give the hammer durability)\n");
          writer.write("crateHammerDurability=0\n");
        }
      }

      try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
        properties.load(reader);
      }
      BS_LOG.info("[Basic Storage] Config loaded successfully");
    } catch (IOException e) {
      BS_LOG.error("[Basic Storage] Failed to load config: " + e.getMessage());
    }
  }

  public void save() {
    try {
      try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
        properties.store(writer, "Basic Storage Configuration");
      }
    } catch (IOException e) {
      BS_LOG.error("[Basic Storage] Failed to save config: " + e.getMessage());
    }
  }

  // Getters
  public boolean isbreakOnlyWithHammer() {
    return Boolean.parseBoolean(properties.getProperty("breakOnlyWithHammer", "false"));
  }

  public int getCrateHammerDurability() {
    try {
      return Math.max(0, Integer.parseInt(properties.getProperty("crateHammerDurability", "0")));
    } catch (NumberFormatException e) {
      BS_LOG.warn("[Basic Storage] Invalid crateHammerDurability value in config, using default");
      return 0;
    }
  }
} 