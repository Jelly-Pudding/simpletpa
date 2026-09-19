package com.jellypudding.simpleTPA.commands;

import com.jellypudding.simpleTPA.request.RequestManager;
import com.jellypudding.simpleTPA.request.TeleportRequest;
import com.jellypudding.simpleTPA.util.Messages;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public abstract class IncomingRequestCommand extends PlayerCommand {

    protected IncomingRequestCommand(RequestManager requestManager) {
        super(requestManager);
    }

    @Override
    protected final void execute(Player player, String label, String[] args) {
        List<TeleportRequest> incoming = requestManager.getIncoming(player.getUniqueId());
        if (incoming.isEmpty()) {
            player.sendMessage(Messages.error("You don't have any pending teleport requests."));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(Messages.error("Usage: /" + label + " <player>"));
            player.sendMessage(Messages.info("Pending requests from:"));
            sendPlayerList(player, onlinePlayers(incoming, TeleportRequest::requester));
            return;
        }

        Player requester = findOnlinePlayer(player, args[0]);
        if (requester == null) {
            return;
        }

        Optional<TeleportRequest> request = requestManager.getRequest(requester.getUniqueId(), player.getUniqueId());
        if (request.isEmpty()) {
            player.sendMessage(Messages.error("You don't have a pending request from ", requester.displayName(), "."));
            return;
        }

        respond(player, requester, request.get());
    }

    protected abstract void respond(Player player, Player requester, TeleportRequest request);

    @Override
    protected List<String> tabComplete(Player player, String partial) {
        List<TeleportRequest> incoming = requestManager.getIncoming(player.getUniqueId());
        return namesStartingWith(onlinePlayers(incoming, TeleportRequest::requester), partial);
    }
}
