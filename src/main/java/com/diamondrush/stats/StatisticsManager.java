package com.diamondrush.stats;

import com.diamondrush.config.ConfigurationManager;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tournament statistics persisted to {@code config/diamondrush-stats.json}.
 * Statistics are keyed by UUID and remember the last known display name.
 */
public final class StatisticsManager {

    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("diamondrush-stats.json");
    private final Map<String, PlayerStats> players = new LinkedHashMap<>();

    public void load() {
        StatsData data = ConfigurationManager.readJson(file, StatsData.class);
        players.clear();
        if (data != null && data.players != null) {
            players.putAll(data.players);
        }
    }

    public void save() {
        StatsData data = new StatsData();
        data.players = new LinkedHashMap<>(players);
        ConfigurationManager.writeJson(file, data);
    }

    public PlayerStats get(UUID uuid) {
        return players.get(uuid.toString());
    }

    public PlayerStats getOrCreate(UUID uuid, String name) {
        PlayerStats stats = players.computeIfAbsent(uuid.toString(), key -> new PlayerStats(key, name));
        if (name != null && !name.isBlank()) {
            stats.name = name;
        }
        return stats;
    }

    public PlayerStats byName(String name) {
        for (PlayerStats stats : players.values()) {
            if (stats.name != null && stats.name.equalsIgnoreCase(name)) {
                return stats;
            }
        }
        return null;
    }

    public void recordMatchWin(UUID winner, String winnerName, UUID loser, String loserName, long durationMillis) {
        getOrCreate(winner, winnerName).recordMatchWin(durationMillis);
        getOrCreate(loser, loserName).recordMatchLoss();
        save();
    }

    public void recordTournamentWin(UUID winner, String winnerName) {
        getOrCreate(winner, winnerName).tournamentWins++;
        save();
    }

    public String leaderboard(int limit) {
        return players.values().stream()
                .sorted((a, b) -> Integer.compare(b.tournamentWins, a.tournamentWins))
                .limit(Math.max(1, limit))
                .map(stats -> String.format(Locale.ROOT, "%s - %d tournaments, %d match wins",
                        stats.name, stats.tournamentWins, stats.matchWins))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No statistics recorded yet.");
    }

    public static final class StatsData {
        public int version = 1;
        public Map<String, PlayerStats> players = new LinkedHashMap<>();
    }
}
