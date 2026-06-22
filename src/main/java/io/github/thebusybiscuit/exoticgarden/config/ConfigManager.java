package io.github.thebusybiscuit.exoticgarden.config;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Nonnull;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.thebusybiscuit.exoticgarden.ExoticGarden;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;

public final class ConfigManager {

    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String CONFIG_VERSION_PATH = "config-version";
    private static final String CONFIG_BACKUP_DIRECTORY = "config-backups";
    private static final String RESEARCHES_PATH = "options.addon-researches";
    private static final String LEGACY_RESEARCHES_PATH = "options.enable-addon-researches";
    private static final int CONFIG_VERSION = 3;
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String HEADER = String.join("\n",
        "ExoticGarden main configuration",
        "Config version: " + CONFIG_VERSION,
        "When this version changes, the plugin will back up the old config.yml",
        "into config-backups before merging in any new default settings."
    );

    private ConfigManager() {
    }

    @Nonnull
    public static Config load(@Nonnull ExoticGarden plugin) {
        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create plugin data folder at " + dataFolder.getAbsolutePath());
        }

        File configFile = new File(dataFolder, CONFIG_FILE_NAME);
        YamlConfiguration configuration = getVersionedMainConfig(plugin, configFile);
        Config config = new Config(configFile, configuration);
        config.setLogger(plugin.getLogger());
        config.setHeader(HEADER);
        config.setDefaultValue(CONFIG_VERSION_PATH, CONFIG_VERSION);
        config.setDefaultValue(RESEARCHES_PATH, false);
        save(config);
        return config;
    }

    public static boolean areAddonResearchesEnabled(@Nonnull Config config) {
        return config.getBoolean(RESEARCHES_PATH);
    }

    public static void save(@Nonnull Config config) {
        config.setHeader(HEADER);
        config.setValue(CONFIG_VERSION_PATH, CONFIG_VERSION);
        config.save();
    }

    @Nonnull
    private static YamlConfiguration getVersionedMainConfig(@Nonnull ExoticGarden plugin, @Nonnull File configFile) {
        YamlConfiguration bundledConfig = loadBundledConfig(plugin);
        int bundledVersion = bundledConfig.getInt(CONFIG_VERSION_PATH, CONFIG_VERSION);

        if (!configFile.exists()) {
            plugin.saveResource(CONFIG_FILE_NAME, false);
            return YamlConfiguration.loadConfiguration(configFile);
        }

        YamlConfiguration existingConfig = YamlConfiguration.loadConfiguration(configFile);
        int currentVersion = existingConfig.getInt(CONFIG_VERSION_PATH, -1);
        boolean needsBackup = currentVersion != bundledVersion || existingConfig.contains(LEGACY_RESEARCHES_PATH);

        if (needsBackup) {
            try {
                File backup = backupExistingConfig(plugin, configFile, currentVersion);
                plugin.getLogger().info("Backed up old config.yml to " + backup.getName());
            } catch (IOException e) {
                throw new IllegalStateException("Could not back up config.yml before updating it.", e);
            }
        }

        mergeDefaults(existingConfig, bundledConfig);

        if (existingConfig.contains(LEGACY_RESEARCHES_PATH) && !existingConfig.contains(RESEARCHES_PATH)) {
            existingConfig.set(RESEARCHES_PATH, existingConfig.getBoolean(LEGACY_RESEARCHES_PATH));
        }

        existingConfig.set(LEGACY_RESEARCHES_PATH, null);
        existingConfig.set(CONFIG_VERSION_PATH, bundledVersion);
        existingConfig.options().copyDefaults(true);
        existingConfig.options().copyHeader(true);
        existingConfig.options().header(HEADER);

        try {
            existingConfig.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save updated config.yml.", e);
        }

        if (needsBackup) {
            plugin.getLogger().info("Updated config.yml to version " + bundledVersion + " while preserving existing values.");
        }

        return existingConfig;
    }

    @Nonnull
    private static YamlConfiguration loadBundledConfig(@Nonnull ExoticGarden plugin) {
        InputStream inputStream = plugin.getResource(CONFIG_FILE_NAME);

        if (inputStream == null) {
            throw new IllegalStateException("Missing bundled resource: " + CONFIG_FILE_NAME);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read bundled config.yml.", e);
        }
    }

    private static void mergeDefaults(@Nonnull FileConfiguration existingConfig, @Nonnull YamlConfiguration bundledConfig) {
        existingConfig.addDefaults(bundledConfig);
    }

    @Nonnull
    private static File backupExistingConfig(@Nonnull ExoticGarden plugin, @Nonnull File configFile, int currentVersion) throws IOException {
        String versionLabel = currentVersion > 0 ? String.valueOf(currentVersion) : "legacy";
        File backupDirectory = new File(plugin.getDataFolder(), CONFIG_BACKUP_DIRECTORY);
        File backupFile = new File(
            backupDirectory,
            "config-v" + versionLabel + "-" + BACKUP_TIMESTAMP.format(LocalDateTime.now()) + ".yml"
        );
        backupDirectory.mkdirs();
        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return backupFile;
    }
}
