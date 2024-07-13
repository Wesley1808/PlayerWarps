package me.wesley1808.playerwarps.config;

import me.wesley1808.playerwarps.PlayerWarps;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConfigManager {
    public static final File DIR = FabricLoader.getInstance().getConfigDir().toFile();
    private static final File CONFIG = new File(DIR, "playerwarps.json");

    public static void load() {
        ConfigManager.generateIfNeeded();

        try (var reader = getReader(CONFIG)) {
            Config.instance = Json.CONFIG.fromJson(reader, Config.class);
        } catch (Exception ex) {
            PlayerWarps.LOGGER.error("Failed to load config!", ex);
        }
    }

    public static void save() {
        try (var writer = getWriter(CONFIG)) {
            writer.write(Json.CONFIG.toJson(Config.instance));
        } catch (Exception ex) {
            PlayerWarps.LOGGER.error("Failed to save config!", ex);
        }
    }

    private static void generateIfNeeded() {
        if (!DIR.exists()) DIR.mkdirs();
        if (!CONFIG.exists()) save();
    }

    public static BufferedWriter getWriter(File file) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
    }

    public static BufferedReader getReader(File file) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }
}
