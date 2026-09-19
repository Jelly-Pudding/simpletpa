package com.jellypudding.simpleTPA.commands;

import com.jellypudding.simpleTPA.request.RequestManager;
import com.jellypudding.simpleTPA.request.TeleportRequest;
import com.jellypudding.simpleTPA.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public abstract class PlayerCommand implements CommandExecutor, TabCompleter {

    protected final RequestManager requestManager;

    protected PlayerCommand(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            execute(player, label, args);
        } else {
            sender.sendMessage(Messages.error("Only players can use teleport commands."));
        }
        return true;
    }

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return List.of();
        }
        return tabComplete(player, args[0]);
    }

    protected abstract void execute(Player player, String label, String[] args);

    protected List<String> tabComplete(Player player, String partial) {
        return List.of();
    }

    protected static Player findOnlinePlayer(Player sender, String name) {
        Player found = Bukkit.getPlayer(name);
        if (found == null) {
            sender.sendMessage(Messages.error("Player not found or is offline."));
        }
        return found;
    }

    protected static List<Player> onlinePlayers(List<TeleportRequest> requests, Function<TeleportRequest, UUID> side) {
        return requests.stream()
                .map(side)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    protected static List<String> namesStartingWith(Collection<? extends Player> players, String partial) {
        return players.stream()
                .map(Player::getName)
                .filter(name -> name.regionMatches(true, 0, partial, 0, partial.length()))
                .toList();
    }

    protected static void sendPlayerList(Player to, Collection<? extends Player> players) {
        for (Player listed : players) {
            to.sendMessage(Component.textOfChildren(Component.text(" - ", NamedTextColor.GRAY), listed.displayName()));
        }
    }
}
