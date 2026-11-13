package com.diamondrush.config;

import com.diamondrush.arena.Arena;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles on-disk persistence. All mod state lives in the Fabric config
 * directory so it survives world resets and server restarts:
 *
 * <ul>
 *     <li>{@code diamondrush.json} - arena definitions</li>
 *     <li>{@code diamondrush-stats.json} - player statistics (owned by StatisticsManager)</li>
 * </ul>
 */
public final class ConfigurationManager {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path arenaFile = FabricLoader.getInstance().getConfigDir().resolve("diamondrush.json");
    private ArenaData data = new ArenaData();

    public void load() {
        if (!Files.exists(arenaFile)) {
            data = new ArenaData();
            saveArenas();
            return;
        }
        try (Reader reader = Files.newBufferedReader(arenaFile, StandardCharsets.UTF_8)) {
            ArenaData loaded = GSON.fromJson(reader, ArenaData.class);
            data = loaded == null ? new ArenaData() : loaded;
            if (data.arenas == null) {
                data.arenas = new LinkedHashMap<>();
            }
        } catch (IOException | RuntimeException exception) {
            System.err.println("[DiamondRush] Failed to read " + arenaFile + ": " + exception.getMessage());
            data = new ArenaData();
        }
    }

    public void saveArenas() {
        writeJson(arenaFile, data);
    }

    public Map<String, Arena> arenas() {
        if (data.arenas == null) {
            data.arenas = new LinkedHashMap<>();
        }
        return data.arenas;
    }

    public String lastArena() {
        return data.lastArena;
    }

    public void setLastArena(String lastArena) {
        data.lastArena = lastArena;
        saveArenas();
    }

    public Path arenaFile() {
        return arenaFile;
    }

    /**
     * Writes JSON to a file through a temporary file so a crash mid-write can
     * never leave a truncated configuration behind.
     */
    public static void writeJson(Path target, Object value) {
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            System.err.println("[DiamondRush] Failed to write " + target + ": " + exception.getMessage());
        }
    }

    public static <T> T readJson(Path source, Class<T> type) {
        if (!Files.exists(source)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        } catch (IOException | RuntimeException exception) {
            System.err.println("[DiamondRush] Failed to read " + source + ": " + exception.getMessage());
            return null;
        }
    }

    /**
     * Root object of {@code diamondrush.json}.
     */
    public static final class ArenaData {
        public int version = 1;
        public Map<String, Arena> arenas = new LinkedHashMap<>();
        public String lastArena;
    }
}
