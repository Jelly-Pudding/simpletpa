package com.jellypudding.simpleTPA.commands;

import com.jellypudding.simpleTPA.request.RequestManager;
import com.jellypudding.simpleTPA.request.RequestType;
import com.jellypudding.simpleTPA.request.TeleportRequest;
import com.jellypudding.simpleTPA.util.Messages;
import org.bukkit.entity.Player;

public final class TpacceptCommand extends IncomingRequestCommand {

    public TpacceptCommand(RequestManager requestManager) {
        super(requestManager);
    }

    @Override
    protected void respond(Player player, Player requester, TeleportRequest request) {
        requestManager.remove(request);

        if (!requestManager.isAllowCrossWorld() && !player.getWorld().equals(requester.getWorld())) {
            player.sendMessage(Messages.error("You cannot accept a teleport request from a player in a different dimension."));
            return;
        }

        boolean requesterTravels = request.type() == RequestType.TPA;
        Player traveller = requesterTravels ? requester : player;
        Player destination = requesterTravels ? player : requester;

        if (traveller.isDead()) {
            if (requesterTravels) {
                player.sendMessage(Messages.error("Cannot teleport ", requester.displayName(), " - they are currently dead."));
                requester.sendMessage(Messages.error(player.displayName(),
                        " tried to accept your teleport request but you were dead. Request cancelled."));
            } else {
                player.sendMessage(Messages.error("You cannot teleport while dead. Request cancelled."));
                requester.sendMessage(Messages.error(player.displayName(),
                        " tried to accept your teleport request but they are dead. Request cancelled."));
            }
            return;
        }

        traveller.teleport(destination.getLocation());
        traveller.sendMessage(Messages.success("Teleported to ", destination.displayName(), "."));
        destination.sendMessage(Messages.success(traveller.displayName(), " has been teleported to you."));
    }
}
