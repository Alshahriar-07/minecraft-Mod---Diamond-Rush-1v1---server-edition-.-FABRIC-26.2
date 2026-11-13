package com.diamondrush.command;

import com.diamondrush.arena.Arena;
import com.diamondrush.arena.ArenaManager;
import com.diamondrush.arena.SpawnPoint;
import com.diamondrush.match.MatchManager;
import com.diamondrush.match.MatchmakingManager;
import com.diamondrush.net.GuiAction;
import com.diamondrush.stats.PlayerStats;
import com.diamondrush.stats.StatisticsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * Command surface:
 *
 * <pre>
 * /diamondrush join | leave | menu | stats &lt;player&gt;
 * /diamondrush arena create|setspawn|list|remove
 * /diamondrush start [player1 player2]
 * /diamondrush stop
 * </pre>
 *
 * Arena mutations and match control require game-master permissions (op level 2).
 */
public final class DiamondRushCommand {

    private static final Predicate<CommandSourceStack> ADMIN =
            Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

    private DiamondRushCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                MatchManager matches,
                                ArenaManager arenas,
                                StatisticsManager statistics,
                                MatchmakingManager matchmaking) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("diamondrush");

        root.then(Commands.literal("join").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            matchmaking.join(player);
            return 1;
        }));

        root.then(Commands.literal("leave").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            if (matchmaking.isQueued(player.getUUID())) {
                matchmaking.leave(player);
            } else if (matches.isParticipant(player)) {
                matches.handleAction(player, GuiAction.LEAVE);
            } else {
                reply(context, ChatFormatting.GRAY + "You are not in a Diamond Rush match.");
            }
            return 1;
        }));

        root.then(Commands.literal("menu").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            matchmaking.refresh(player);
            return 1;
        }));

        root.then(Commands.literal("stats")
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    PlayerStats stats = statistics.get(target.getUUID());
                    reply(context, stats == null
                            ? ChatFormatting.GRAY + target.getName().getString() + " has no recorded statistics yet."
                            : ChatFormatting.AQUA + stats.summary());
                    return 1;
                })));

        root.then(Commands.literal("arena")
                .then(Commands.literal("create").requires(ADMIN)
                        .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(context -> {
                                    String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                                    try {
                                        Arena arena = arenas.create(name);
                                        reply(context, ChatFormatting.GREEN + "Created arena '" + arena.name
                                                + "'. Now set both spawns with /diamondrush arena setspawn "
                                                + arena.name + " <player1|player2> (stand at the spawn).");
                                    } catch (IllegalArgumentException exception) {
                                        failure(context, exception.getMessage());
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("setspawn").requires(ADMIN)
                        .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .then(Commands.literal("player1").executes(context -> setSpawn(context, arenas, 1)))
                                .then(Commands.literal("player2").executes(context -> setSpawn(context, arenas, 2)))))
                .then(Commands.literal("list").executes(context -> {
                    reply(context, ChatFormatting.AQUA + "Diamond Rush arenas:" + ChatFormatting.RESET
                            + arenas.describeAll());
                    return 1;
                }))
                .then(Commands.literal("remove").requires(ADMIN)
                        .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(context -> {
                                    String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                                    try {
                                        if (arenas.remove(name)) {
                                            reply(context, ChatFormatting.GREEN + "Removed arena '" + name + "'.");
                                        } else {
                                            failure(context, "Unknown arena '" + name + "'.");
                                        }
                                    } catch (IllegalStateException exception) {
                                        failure(context, exception.getMessage());
                                    }
                                    return 1;
                                }))));

        root.then(Commands.literal("start").requires(ADMIN).executes(context -> {
            String error = matchmaking.forceStart();
            if (error != null) {
                failure(context, error);
            } else {
                reply(context, ChatFormatting.GREEN + "Started the next queued match.");
            }
            return 1;
        }).then(Commands.argument("player1", EntityArgument.player())
                .then(Commands.argument("player2", EntityArgument.player()).executes(context -> {
                    ServerPlayer first = EntityArgument.getPlayer(context, "player1");
                    ServerPlayer second = EntityArgument.getPlayer(context, "player2");
                    String error = matches.beginTournament(first, second);
                    if (error != null) {
                        failure(context, error);
                    } else {
                        reply(context, ChatFormatting.GREEN + "Started " + first.getName().getString()
                                + " vs " + second.getName().getString() + ".");
                    }
                    return 1;
                }))));

        root.then(Commands.literal("stop").requires(ADMIN).executes(context -> {
            int stopped = matches.stopAll();
            matchmaking.clear();
            reply(context, ChatFormatting.GREEN + "Stopped " + stopped + " active tournament(s).");
            return 1;
        }));

        root.executes(context -> {
            reply(context, ChatFormatting.AQUA + "Diamond Rush 1v1"
                    + ChatFormatting.RESET + " - first to mine a diamond wins."
                    + "\n/diamondrush join - join the 1v1 queue"
                    + "\n/diamondrush leave - leave the queue or forfeit"
                    + "\n/diamondrush menu - reopen the match screen"
                    + "\n/diamondrush stats <player> - show statistics"
                    + "\n/diamondrush arena create|setspawn|list|remove [admin]"
                    + "\n/diamondrush start [player1 player2] [admin]"
                    + "\n/diamondrush stop [admin]");
            return 1;
        });

        dispatcher.register(root);
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context, ArenaManager arenas, int slot)
            throws CommandSyntaxException {
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!arenas.exists(name)) {
            failure(context, "Unknown arena '" + name + "'. Create it first.");
            return 0;
        }
        SpawnPoint point = SpawnPoint.of(player);
        try {
            arenas.setSpawn(name, slot, point);
            Arena arena = arenas.get(name);
            reply(context, ChatFormatting.GREEN + "Set arena " + ChatFormatting.WHITE + name
                    + ChatFormatting.GREEN + " spawn player" + slot + ChatFormatting.WHITE + " -> "
                    + ChatFormatting.GRAY + point.describe()
                    + ChatFormatting.GREEN + (arena != null && arena.isComplete()
                    ? " (arena ready)" : " (still needs the other spawn)"));
        } catch (IllegalArgumentException exception) {
            failure(context, exception.getMessage());
        }
        return 1;
    }

    private static void reply(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }

    private static void failure(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendFailure(Component.literal(message));
    }
}
