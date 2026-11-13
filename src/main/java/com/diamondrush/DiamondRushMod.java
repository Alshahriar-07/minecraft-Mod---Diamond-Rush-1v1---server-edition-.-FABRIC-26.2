package com.diamondrush;

import com.diamondrush.arena.ArenaManager;
import com.diamondrush.arena.SpawnManager;
import com.diamondrush.command.DiamondRushCommand;
import com.diamondrush.config.ConfigurationManager;
import com.diamondrush.match.DiamondDetectionManager;
import com.diamondrush.match.MatchManager;
import com.diamondrush.match.MatchmakingManager;
import com.diamondrush.match.ScoreManager;
import com.diamondrush.match.TournamentManager;
import com.diamondrush.net.NetworkHandler;
import com.diamondrush.player.InventoryManager;
import com.diamondrush.stats.StatisticsManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diamond Rush 1v1 - server entrypoint. Constructs the managers, registers
 * networking, gameplay hooks and commands.
 */
public final class DiamondRushMod implements ModInitializer {

    public static final String MOD_ID = "diamondrush";
    public static final Logger LOGGER = LoggerFactory.getLogger("DiamondRush");

    private static DiamondRushMod instance;
    private static volatile MinecraftServer server;

    private ConfigurationManager configuration;
    private ArenaManager arenas;
    private MatchManager matches;
    private MatchmakingManager matchmaking;
    private StatisticsManager statistics;

    @Override
    public void onInitialize() {
        instance = this;
        long startedAt = System.currentTimeMillis();

        configuration = new ConfigurationManager();
        configuration.load();
        arenas = new ArenaManager(configuration);
        SpawnManager spawns = new SpawnManager();
        InventoryManager inventories = new InventoryManager();
        statistics = new StatisticsManager();
        statistics.load();

        ScoreManager scores = new ScoreManager();
        TournamentManager tournaments = new TournamentManager(scores, statistics);
        matches = new MatchManager(arenas, spawns, inventories, tournaments, statistics);
        matchmaking = new MatchmakingManager(matches);

        NetworkHandler.registerPayloads();
        NetworkHandler.registerServerReceivers(matches);
        new DiamondDetectionManager(matches).register();

        ServerLifecycleEvents.SERVER_STARTING.register(starting -> server = starting);
        ServerLifecycleEvents.SERVER_STOPPING.register(stopping -> {
            if (matchmaking != null) {
                matchmaking.clear();
            }
            matches.shutdown();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> {
            if (matchmaking != null) {
                matchmaking.clear();
            }
            statistics.save();
            configuration.saveArenas();
            server = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(activeServer -> {
            server = activeServer;
            matches.tick(activeServer);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, activeServer) -> {
            if (handler.player != null) {
                matchmaking.onDisconnect(handler.player);
                matches.onPlayerDisconnect(handler.player);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> matches.onPlayerRespawn(newPlayer));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                DiamondRushCommand.register(dispatcher, matches, arenas, statistics, matchmaking));

        LOGGER.info("Diamond Rush 1v1 loaded in {} ms with {} arena(s)",
                System.currentTimeMillis() - startedAt, arenas.size());
    }

    public static DiamondRushMod instance() {
        return instance;
    }

    /** The running server, or null when the integrated server is stopped. */
    public static MinecraftServer server() {
        return server;
    }

    public MatchmakingManager matchmaking() {
        return matchmaking;
    }

    public MatchManager matches() {
        return matches;
    }

    public ArenaManager arenas() {
        return arenas;
    }

    public StatisticsManager statistics() {
        return statistics;
    }
}
