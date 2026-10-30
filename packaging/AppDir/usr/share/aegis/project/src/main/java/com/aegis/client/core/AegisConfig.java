package com.aegis.client.core;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AegisConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aegis-client.properties");
    private static final Properties VALUES = new Properties();

    private AegisConfig() {}

    public static void load(ModuleManager manager) {
        try {
            if (Files.exists(FILE)) {
                try (InputStream input = Files.newInputStream(FILE)) { VALUES.load(input); }
            }
            for (ClientModule module : manager.all()) {
                String value = VALUES.getProperty("module." + module.id());
                if (value != null) manager.setEnabled(module.id(), Boolean.parseBoolean(value));
            }
            AegisClientSettings.hudX = parseInt("hud.x", 8);
            AegisClientSettings.hudY = parseInt("hud.y", 8);
            AegisClientSettings.hudScale = parseInt("hud.scale", 100);
            AegisClientSettings.hudOpacity = parseInt("hud.opacity", 73);
            AegisClientSettings.themeIndex = parseInt("hud.theme", 0);
        } catch (IOException ignored) {
            // Defaults remain active if a configuration file cannot be read.
        }
    }

    public static void save(ModuleManager manager) {
        try {
            Files.createDirectories(FILE.getParent());
            for (ClientModule module : manager.all()) {
                VALUES.setProperty("module." + module.id(), Boolean.toString(module.enabled()));
            }
            VALUES.setProperty("hud.x", Integer.toString(AegisClientSettings.hudX));
            VALUES.setProperty("hud.y", Integer.toString(AegisClientSettings.hudY));
            VALUES.setProperty("hud.scale", Integer.toString(AegisClientSettings.hudScale));
            VALUES.setProperty("hud.opacity", Integer.toString(AegisClientSettings.hudOpacity));
            VALUES.setProperty("hud.theme", Integer.toString(AegisClientSettings.themeIndex));
            try (OutputStream output = Files.newOutputStream(FILE)) {
                VALUES.store(output, "Aegis Client settings");
            }
        } catch (IOException ignored) {
            // A failed save must not interrupt the game.
        }
    }

    private static int parseInt(String key, int fallback) {
        try { return Integer.parseInt(VALUES.getProperty(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
