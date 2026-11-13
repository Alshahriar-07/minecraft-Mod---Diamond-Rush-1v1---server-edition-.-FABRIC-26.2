package com.diamondrush.arena;

import com.diamondrush.config.ConfigurationManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Owns the configured arenas: creation, spawn assignment, validation,
 * persistence and reservation so two simultaneous matches never share an arena.
 */
public final class ArenaManager {

    private final ConfigurationManager configuration;
    private final Map<String, Arena> arenas;
    private final Set<String> occupied = new HashSet<>();

    public ArenaManager(ConfigurationManager configuration) {
        this.configuration = configuration;
        this.arenas = configuration.arenas();
    }

    public Collection<Arena> arenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public int size() {
        return arenas.size();
    }

    public Arena get(String name) {
        if (name == null) {
            return null;
        }
        return arenas.get(normalize(name));
    }

    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public boolean exists(String name) {
        return get(name) != null;
    }

    /**
     * Creates an empty arena. Spawns are assigned afterwards with
     * {@code /diamondrush arena setspawn}.
     */
    public Arena create(String rawName) {
        String name = normalize(rawName);
        if (arenas.containsKey(name)) {
            throw new IllegalArgumentException("An arena named '" + name + "' already exists.");
        }
        if (arenas.size() >= 64) {
            throw new IllegalArgumentException("Arena limit reached (64). Remove an arena first.");
        }
        Arena arena = new Arena(name);
        arenas.put(name, arena);
        configuration.saveArenas();
        return arena;
    }

    public void setSpawn(String rawName, int slot, SpawnPoint spawnPoint) {
        Arena arena = get(rawName);
        if (arena == null) {
            throw new IllegalArgumentException("Unknown arena '" + rawName + "'.");
        }
        arena.setSpawn(slot, spawnPoint);
        configuration.saveArenas();
    }

    public boolean remove(String rawName) {
        String name = normalize(rawName);
        if (!arenas.containsKey(name)) {
            return false;
        }
        if (occupied.contains(name)) {
            throw new IllegalStateException("Arena '" + name + "' is in use by an active match.");
        }
        arenas.remove(name);
        configuration.saveArenas();
        return true;
    }

    public boolean isOccupied(String name) {
        return occupied.contains(normalize(name));
    }

    public void reserve(String name) {
        occupied.add(normalize(name));
    }

    public void release(String name) {
        if (name != null) {
            occupied.remove(normalize(name));
        }
    }

    public Set<String> occupiedArenas() {
        return Collections.unmodifiableSet(occupied);
    }

    /**
     * Picks a random usable arena. When {@code exclude} is supplied the previous
     * arena is avoided whenever another complete arena is available.
     */
    public Arena pickArena(MinecraftServer server, String exclude) {
        List<Arena> preferred = new ArrayList<>();
        List<Arena> fallback = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (!arena.isComplete() || isOccupied(arena.name) || !isUsable(server, arena)) {
                continue;
            }
            if (exclude != null && exclude.equalsIgnoreCase(arena.name)) {
                fallback.add(arena);
            } else {
                preferred.add(arena);
            }
        }
        List<Arena> pool = preferred.isEmpty() ? fallback : preferred;
        if (pool.isEmpty()) {
            return null;
        }
        Collections.shuffle(pool);
        Arena chosen = pool.getFirst();
        configuration.setLastArena(chosen.name);
        return chosen;
    }

    /**
     * Validates that both spawn dimensions are loaded on this server. This
     * prevents matches starting inside a dimension that no longer exists.
     */
    public boolean isUsable(MinecraftServer server, Arena arena) {
        if (arena == null || !arena.isComplete()) {
            return false;
        }
        return dimensionExists(server, arena.spawn1) && dimensionExists(server, arena.spawn2);
    }

    private static boolean dimensionExists(MinecraftServer server, SpawnPoint point) {
        if (server == null || point == null) {
            return false;
        }
        Identifier identifier;
        try {
            identifier = Identifier.parse(point.dimension);
        } catch (RuntimeException exception) {
            return false;
        }
        return server.levelKeys().stream().anyMatch(key -> key.identifier().equals(identifier));
    }

    public String describeAll() {
        if (arenas.isEmpty()) {
            return "No arenas configured.";
        }
        StringBuilder builder = new StringBuilder();
        for (Arena arena : arenas.values()) {
            builder.append("\n - ").append(arena.name)
                    .append(arena.isComplete() ? " [ready]" : " [incomplete]")
                    .append(isOccupied(arena.name) ? " [in use]" : "");
        }
        return builder.toString();
    }
}
