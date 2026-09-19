package com.jellypudding.simpleTPA.commands;

import com.jellypudding.simpleTPA.request.RequestManager;
import com.jellypudding.simpleTPA.request.RequestType;
import com.jellypudding.simpleTPA.request.TeleportRequest;
import com.jellypudding.simpleTPA.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class TpaCommand extends PlayerCommand {

    private final RequestType type;

    public TpaCommand(RequestManager requestManager, RequestType type) {
        super(requestManager);
        this.type = type;
    }

    @Override
    protected void execute(Player player, String label, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Messages.error("Usage: /" + label + " <player>"));
            return;
        }

        UUID playerId = player.getUniqueId();
        long cooldown = requestManager.getRemainingCooldownSeconds(playerId);
        if (cooldown > 0) {
            player.sendMessage(Messages.error("Please wait " + cooldown + (cooldown == 1 ? " second" : " seconds")
                    + " before sending another request."));
            return;
        }

        Player target = findOnlinePlayer(player, args[0]);
        if (target == null) {
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(Messages.error("You cannot send a teleport request to yourself."));
            return;
        }

        if (!requestManager.isAllowCrossWorld() && !player.getWorld().equals(target.getWorld())) {
            player.sendMessage(Messages.error("You cannot send a teleport request to a player in a different dimension."));
            return;
        }

        if (requestManager.getRequest(playerId, target.getUniqueId()).isPresent()) {
            player.sendMessage(Messages.error("You already have a pending request to this player."));
            return;
        }

        requestManager.add(new TeleportRequest(playerId, target.getUniqueId(), type));
        requestManager.startCooldown(playerId);

        String timeout = requestManager.getTimeoutDisplay();
        player.sendMessage(Messages.success("Teleport request sent to ", target.displayName(), "."));
        player.sendMessage(Messages.info("This request will expire in " + timeout + "."));

        target.sendMessage(type == RequestType.TPA
                ? Messages.success(player.displayName(), " has requested to teleport to you.")
                : Messages.success(player.displayName(), " has requested that you teleport to them."));
        target.sendMessage(Messages.info("Type /tpaccept ", player.displayName(),
                " to accept. This request will expire in " + timeout + "."));
    }

    @Override
    protected List<String> tabComplete(Player player, String partial) {
        return namesStartingWith(Bukkit.getOnlinePlayers(), partial).stream()
                .filter(name -> !name.equals(player.getName()))
                .toList();
    }
}
