package com.khazoda.basicstorage;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static com.khazoda.basicstorage.Constants.BS_LOG;

public class BasicStorageConfig {
  private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("basicstorage.properties");
  private static BasicStorageConfig INSTANCE;
  private final Properties properties;

  private BasicStorageConfig() {
    this.properties = new Properties();
  }

  public static BasicStorageConfig getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new BasicStorageConfig();
    }
    return INSTANCE;
  }

  public void load() {
    try {
      if (!Files.exists(CONFIG_PATH)) {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
          writer.write("# Basic Storage Configuration\n\n");

          writer.write("# If true, crates can only be broken using an axe.\n");
          writer.write("# If false, crates can be broken with anything.\n");
          writer.write("break_with_axe_only=false\n\n");
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

  public boolean breakWithAxeOnly() {
    return Boolean.parseBoolean(properties.getProperty("break_with_axe_only", "false"));
  }

  public void setBreakWithAxeOnly(boolean value) {
    // Explicitly not saving this value, as it's just for runtime while connected to a server
    properties.setProperty("break_with_axe_only", String.valueOf(value));
  }
}