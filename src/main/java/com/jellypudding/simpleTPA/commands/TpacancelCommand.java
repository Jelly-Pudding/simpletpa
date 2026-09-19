package com.jellypudding.simpleTPA.commands;

import com.jellypudding.simpleTPA.request.RequestManager;
import com.jellypudding.simpleTPA.request.TeleportRequest;
import com.jellypudding.simpleTPA.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TpacancelCommand extends PlayerCommand {

    public TpacancelCommand(RequestManager requestManager) {
        super(requestManager);
    }

    @Override
    protected void execute(Player player, String label, String[] args) {
        List<TeleportRequest> outgoing = requestManager.getOutgoing(player.getUniqueId());
        if (outgoing.isEmpty()) {
            player.sendMessage(Messages.error("You don't have any pending teleport requests."));
            return;
        }

        if (args.length < 1) {
            if (outgoing.size() == 1) {
                cancel(player, outgoing.get(0));
            } else {
                player.sendMessage(Messages.error("Usage: /" + label + " <player> or /" + label + " all"));
                player.sendMessage(Messages.info("You can cancel the following pending requests:"));
                sendPlayerList(player, onlinePlayers(outgoing, TeleportRequest::target));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("all")) {
            outgoing.forEach(request -> notifyTarget(player, request));
            player.sendMessage(Messages.info("You have cancelled all your teleport requests."));
            return;
        }

        Player target = findOnlinePlayer(player, args[0]);
        if (target == null) {
            return;
        }

        Optional<TeleportRequest> request = requestManager.getRequest(player.getUniqueId(), target.getUniqueId());
        if (request.isEmpty()) {
            player.sendMessage(Messages.error("You don't have a pending request to ", target.displayName(), "."));
            return;
        }

        cancel(player, request.get());
    }

    @Override
    protected List<String> tabComplete(Player player, String partial) {
        List<String> suggestions = new ArrayList<>();
        if ("all".regionMatches(true, 0, partial, 0, partial.length())) {
            suggestions.add("all");
        }
        List<TeleportRequest> outgoing = requestManager.getOutgoing(player.getUniqueId());
        suggestions.addAll(namesStartingWith(onlinePlayers(outgoing, TeleportRequest::target), partial));
        return suggestions;
    }

    private void cancel(Player player, TeleportRequest request) {
        Player target = notifyTarget(player, request);
        player.sendMessage(Messages.info("You have cancelled your teleport request to ", Messages.nameOf(target), "."));
    }

    // Removes the request and returns the target if online.
    private Player notifyTarget(Player player, TeleportRequest request) {
        requestManager.remove(request);
        Player target = Bukkit.getPlayer(request.target());
        if (target != null) {
            target.sendMessage(Messages.info(player.displayName(), " has cancelled their teleport request."));
        }
        return target;
    }
}
