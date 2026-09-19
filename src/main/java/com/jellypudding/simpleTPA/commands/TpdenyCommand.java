package com.jellypudding.simpleTPA.commands;

import com.jellypudding.simpleTPA.request.RequestManager;
import com.jellypudding.simpleTPA.request.TeleportRequest;
import com.jellypudding.simpleTPA.util.Messages;
import org.bukkit.entity.Player;

public final class TpdenyCommand extends IncomingRequestCommand {

    public TpdenyCommand(RequestManager requestManager) {
        super(requestManager);
    }

    @Override
    protected void respond(Player player, Player requester, TeleportRequest request) {
        requestManager.remove(request);
        requester.sendMessage(Messages.error(player.displayName(), " has denied your teleport request."));
        player.sendMessage(Messages.info("You have denied ", requester.displayName(), "'s teleport request."));
    }
}
