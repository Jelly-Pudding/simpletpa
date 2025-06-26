package com.jellypudding.simpleTPA;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.stream.Collectors;

public final class SimpleTPA extends JavaPlugin {

    // Map to store teleport requests: key is "requesterUUID:targetUUID", value is timestamp.
    private final HashMap<String, Long> teleportRequests = new HashMap<>();
    
    // Map to store request expiration tasks: key is "requesterUUID:targetUUID", value is BukkitTask.
    private final HashMap<String, BukkitTask> expirationTasks = new HashMap<>();
    
    // Map to store cooldowns: key is player UUID, value is time when cooldown expires.
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    // configuration values.
    private long requestTimeoutTicks;
    private long requestCooldownMillis;
    private boolean allowCrossWorld;

    @Override
    public void onEnable() {
        // Save default config.
        saveDefaultConfig();
        
        // Load values from config.
        loadConfigValues();
        
        // Register commands with the plugin.
        Objects.requireNonNull(getCommand("tpa")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpa")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("tpaccept")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpaccept")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("tpdeny")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpdeny")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("tpacancel")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpacancel")).setTabCompleter(this);
        
        getLogger().info("SimpleTPA has been enabled.");
    }

    private void loadConfigValues() {
        // Get request timeout in seconds from config (default 120 seconds / 2 minutes).
        int timeoutSeconds = getConfig().getInt("request-timeout", 120);
        // Convert to ticks (20 ticks = 1 second).
        requestTimeoutTicks = timeoutSeconds * 20L;

        // Get cooldown in seconds from config (default 10 seconds).
        int cooldownSeconds = getConfig().getInt("request-cooldown", 10);
        // Convert to milliseconds.
        requestCooldownMillis = cooldownSeconds * 1000L;

        // Whether to allow teleporting to players in different dimensions.
        allowCrossWorld = getConfig().getBoolean("allow-cross-world", false);

        getLogger().info("Config loaded: timeout=" + timeoutSeconds + "s, cooldown=" + cooldownSeconds + "s, cross-world=" + allowCrossWorld);
    }

    @Override
    public void onDisable() {
        // Cancel all pending tasks.
        expirationTasks.values().forEach(BukkitTask::cancel);
        teleportRequests.clear();
        expirationTasks.clear();
        cooldowns.clear();
        
        getLogger().info("SimpleTPA has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use teleportation commands.").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("tpa")) {
            return handleTpaCommand(player, args);
        } else if (command.getName().equalsIgnoreCase("tpaccept")) {
            return handleTpacceptCommand(player, args);
        } else if (command.getName().equalsIgnoreCase("tpdeny")) {
            return handleTpdenyCommand(player, args);
        } else if (command.getName().equalsIgnoreCase("tpacancel")) {
            return handleTpacancelCommand(player, args);
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("tpa") && args.length == 1) {
            String partialName = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .filter(name -> !name.equals(sender.getName()))
                    .collect(Collectors.toList());
        } else if (command.getName().equalsIgnoreCase("tpaccept") && args.length == 1) {
            String partialName = args[0].toLowerCase();
            UUID playerUUID = player.getUniqueId();
            
            return getPendingRequesters(playerUUID).stream()
                    .map(uuid -> Bukkit.getPlayer(uuid))
                    .filter(Objects::nonNull)
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        } else if (command.getName().equalsIgnoreCase("tpdeny") && args.length == 1) {
            String partialName = args[0].toLowerCase();
            UUID playerUUID = player.getUniqueId();
            
            return getPendingRequesters(playerUUID).stream()
                    .map(uuid -> Bukkit.getPlayer(uuid))
                    .filter(Objects::nonNull)
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        } else if (command.getName().equalsIgnoreCase("tpacancel") && args.length == 1) {
            String partialName = args[0].toLowerCase();
            UUID playerUUID = player.getUniqueId();

            List<String> suggestions = new ArrayList<>();

            if ("all".startsWith(partialName)) {
                suggestions.add("all");
            }

            teleportRequests.keySet().stream()
                    .filter(key -> key.startsWith(playerUUID.toString() + ":"))
                    .forEach(requestKey -> {
                        String[] parts = requestKey.split(":");
                        if (parts.length == 2) {
                            try {
                                UUID targetUUID = UUID.fromString(parts[1]);
                                Player target = Bukkit.getPlayer(targetUUID);
                                if (target != null && target.isOnline()) {
                                    String name = target.getName();
                                    if (name.toLowerCase().startsWith(partialName)) {
                                        suggestions.add(name);
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                // Invalid UUID, skip
                            }
                        }
                    });

            return suggestions;
        }

        return Collections.emptyList();
    }

    private boolean handleTpaCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /tpa <player>").color(NamedTextColor.RED));
            return true;
        }
        
        // Check for cooldown
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(playerUUID)) {
            long cooldownExpires = cooldowns.get(playerUUID);
            if (currentTime < cooldownExpires) {
                long remainingSeconds = (cooldownExpires - currentTime) / 1000 + 1;
                player.sendMessage(Component.text("Please wait " + remainingSeconds + " seconds before sending another request.").color(NamedTextColor.RED));
                return true;
            }
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player not found or is offline.").color(NamedTextColor.RED));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You cannot teleport to yourself.").color(NamedTextColor.RED));
            return true;
        }

        if (!allowCrossWorld && !player.getWorld().equals(target.getWorld())) {
            player.sendMessage(Component.text("You cannot teleport to a player in a different dimension.").color(NamedTextColor.RED));
            return true;
        }

        String requestKey = player.getUniqueId() + ":" + target.getUniqueId();
        if (teleportRequests.containsKey(requestKey)) {
            player.sendMessage(Component.text("You already have a pending request to this player.").color(NamedTextColor.RED));
            return true;
        }

        teleportRequests.put(requestKey, currentTime);

        cooldowns.put(playerUUID, currentTime + requestCooldownMillis);

        // Schedule task to expire the request after configured time.
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (teleportRequests.containsKey(requestKey)) {
                teleportRequests.remove(requestKey);
                expirationTasks.remove(requestKey);
                
                Component expiredMessage = Component.text("Your teleport request to ").color(NamedTextColor.RED)
                    .append(target.displayName())
                    .append(Component.text(" has expired.").color(NamedTextColor.RED));
                player.sendMessage(expiredMessage);
                
                Component targetMessage = Component.text("Teleport request from ").color(NamedTextColor.RED)
                    .append(player.displayName())
                    .append(Component.text(" has expired.").color(NamedTextColor.RED));
                target.sendMessage(targetMessage);
            }
        }, requestTimeoutTicks);

        // Store the task for cancellation if needed.
        expirationTasks.put(requestKey, task);

        // Calculate timeout in minutes and seconds for display
        int timeoutSeconds = (int)(requestTimeoutTicks / 20);
        int minutes = timeoutSeconds / 60;
        int seconds = timeoutSeconds % 60;
        String timeoutDisplay = minutes > 0 ? minutes + " minute" + (minutes > 1 ? "s" : "") : "";
        if (seconds > 0) {
            if (!timeoutDisplay.isEmpty()) timeoutDisplay += " and ";
            timeoutDisplay += seconds + " second" + (seconds > 1 ? "s" : "");
        }

        // Send messages
        Component sentMessage = Component.text("Teleport request sent to ").color(NamedTextColor.GREEN)
            .append(target.displayName())
            .append(Component.text(".").color(NamedTextColor.GREEN));
        player.sendMessage(sentMessage);
        player.sendMessage(Component.text("This request will expire in " + timeoutDisplay + ".").color(NamedTextColor.YELLOW));
        
        Component receivedMessage = player.displayName()
            .append(Component.text(" has requested to teleport to you.").color(NamedTextColor.GREEN));
        target.sendMessage(receivedMessage);
        Component acceptMessage = Component.text("Type /tpaccept " + player.getName() + " to accept ").color(NamedTextColor.YELLOW)
            .append(player.displayName())
            .append(Component.text("'s request. This request will expire in " + timeoutDisplay + ".").color(NamedTextColor.YELLOW));
        target.sendMessage(acceptMessage);

        return true;
    }

    private boolean handleTpacceptCommand(Player player, String[] args) {
        UUID targetUUID = player.getUniqueId();
        List<UUID> pendingRequesters = getPendingRequesters(targetUUID);
        
        if (pendingRequesters.isEmpty()) {
            player.sendMessage(Component.text("You don't have any pending teleport requests.").color(NamedTextColor.RED));
            return true;
        }
        
        // If no name specified, show a list of players who have sent requests
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /tpaccept <player>").color(NamedTextColor.RED));
            player.sendMessage(Component.text("Pending requests from:").color(NamedTextColor.YELLOW));
            
            for (UUID requesterUUID : pendingRequesters) {
                Player requester = Bukkit.getPlayer(requesterUUID);
                if (requester != null && requester.isOnline()) {
                    Component listItem = Component.text(" - ").color(NamedTextColor.GOLD)
                        .append(requester.displayName());
                    player.sendMessage(listItem);
                }
            }
            return true;
        }
        
        // Find the player by name
        String requesterName = args[0];
        Player requester = Bukkit.getPlayer(requesterName);
        
        if (requester == null || !requester.isOnline()) {
            player.sendMessage(Component.text("Player not found or is offline.").color(NamedTextColor.RED));
            return true;
        }

        UUID requesterUUID = requester.getUniqueId();

        String requestKey = requesterUUID + ":" + targetUUID;
        if (!teleportRequests.containsKey(requestKey)) {
            Component noRequestMessage = Component.text("You don't have a pending request from ").color(NamedTextColor.RED)
                .append(requester.displayName())
                .append(Component.text(".").color(NamedTextColor.RED));
            player.sendMessage(noRequestMessage);
            return true;
        }

        if (!allowCrossWorld && !player.getWorld().equals(requester.getWorld())) {
            player.sendMessage(Component.text("You cannot accept a teleport request from a player in a different dimension.").color(NamedTextColor.RED));
            teleportRequests.remove(requestKey);
            cancelExpirationTask(requestKey);
            return true;
        }

        // Teleport the requester to the target
        requester.teleport(player.getLocation());

        // Send messages
        Component teleportMessage = Component.text("Teleported to ").color(NamedTextColor.GREEN)
            .append(player.displayName())
            .append(Component.text(".").color(NamedTextColor.GREEN));
        requester.sendMessage(teleportMessage);
        
        Component notificationMessage = requester.displayName()
            .append(Component.text(" has been teleported to you.").color(NamedTextColor.GREEN));
        player.sendMessage(notificationMessage);

        // Clean up the request
        teleportRequests.remove(requestKey);
        cancelExpirationTask(requestKey);

        return true;
    }

    private boolean handleTpdenyCommand(Player player, String[] args) {
        UUID targetUUID = player.getUniqueId();
        List<UUID> pendingRequesters = getPendingRequesters(targetUUID);

        if (pendingRequesters.isEmpty()) {
            player.sendMessage(Component.text("You don't have any pending teleport requests.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /tpdeny <player>").color(NamedTextColor.RED));
            player.sendMessage(Component.text("Pending requests from:").color(NamedTextColor.YELLOW));

            for (UUID requesterUUID : pendingRequesters) {
                Player requester = Bukkit.getPlayer(requesterUUID);
                if (requester != null && requester.isOnline()) {
                    Component listItem = Component.text(" - ").color(NamedTextColor.GOLD)
                        .append(requester.displayName());
                    player.sendMessage(listItem);
                }
            }
            return true;
        }

        String requesterName = args[0];
        Player requester = Bukkit.getPlayer(requesterName);

        if (requester == null || !requester.isOnline()) {
            player.sendMessage(Component.text("Player not found or is offline.").color(NamedTextColor.RED));
            return true;
        }

        UUID requesterUUID = requester.getUniqueId();
        String requestKey = requesterUUID + ":" + targetUUID;

        if (!teleportRequests.containsKey(requestKey)) {
            Component noRequestMessage = Component.text("You don't have a pending request from ").color(NamedTextColor.RED)
                .append(requester.displayName())
                .append(Component.text(".").color(NamedTextColor.RED));
            player.sendMessage(noRequestMessage);
            return true;
        }

        Component deniedMessage = player.displayName()
            .append(Component.text(" has denied your teleport request.").color(NamedTextColor.RED));
        requester.sendMessage(deniedMessage);

        Component confirmMessage = Component.text("You have denied ").color(NamedTextColor.YELLOW)
            .append(requester.displayName())
            .append(Component.text("'s teleport request.").color(NamedTextColor.YELLOW));
        player.sendMessage(confirmMessage);

        teleportRequests.remove(requestKey);
        cancelExpirationTask(requestKey);

        return true;
    }

    private boolean handleTpacancelCommand(Player player, String[] args) {
        UUID playerUUID = player.getUniqueId();

        // Get all requests sent by this player
        List<String> playerRequests = teleportRequests.keySet().stream()
                .filter(key -> key.startsWith(playerUUID.toString() + ":"))
                .collect(Collectors.toList());

        if (playerRequests.isEmpty()) {
            player.sendMessage(Component.text("You don't have any pending teleport requests.").color(NamedTextColor.RED));
            return true;
        }

        // If no target specified, show list of cancelable requests or cancel if only one.
        if (args.length < 1) {
            if (playerRequests.size() == 1) {
                String requestKey = playerRequests.get(0);
                String[] parts = requestKey.split(":");
                Player target = Bukkit.getPlayer(UUID.fromString(parts[1]));
                
                player.sendMessage(Component.text("You have cancelled your teleport request.").color(NamedTextColor.YELLOW));
                if (target != null && target.isOnline()) {
                    Component cancelMessage = player.displayName()
                        .append(Component.text(" has cancelled their teleport request.").color(NamedTextColor.YELLOW));
                    target.sendMessage(cancelMessage);
                }

                teleportRequests.remove(requestKey);
                cancelExpirationTask(requestKey);
                return true;
            } else {
                player.sendMessage(Component.text("Usage: /tpacancel <player> or /tpacancel all").color(NamedTextColor.RED));
                player.sendMessage(Component.text("You can cancel the following pending requests:").color(NamedTextColor.YELLOW));

                for (String requestKey : playerRequests) {
                    String[] parts = requestKey.split(":");
                    Player target = Bukkit.getPlayer(UUID.fromString(parts[1]));
                    if (target != null && target.isOnline()) {
                        Component listItem = Component.text(" - ").color(NamedTextColor.GOLD)
                            .append(target.displayName());
                        player.sendMessage(listItem);
                    }
                }
                return true;
            }
        }

        // Handle "all" argument
        if (args[0].equalsIgnoreCase("all")) {
            for (String requestKey : playerRequests) {
                String[] parts = requestKey.split(":");
                Player target = Bukkit.getPlayer(UUID.fromString(parts[1]));

                if (target != null && target.isOnline()) {
                    Component cancelMessage = player.displayName()
                        .append(Component.text(" has cancelled their teleport request.").color(NamedTextColor.YELLOW));
                    target.sendMessage(cancelMessage);
                }

                teleportRequests.remove(requestKey);
                cancelExpirationTask(requestKey);
            }

            player.sendMessage(Component.text("You have cancelled all your teleport requests.").color(NamedTextColor.YELLOW));
            return true;
        }

        // Handle specific player name
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player not found or is offline.").color(NamedTextColor.RED));
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        String requestKey = playerUUID + ":" + targetUUID;

        if (!teleportRequests.containsKey(requestKey)) {
            Component noRequestMessage = Component.text("You don't have a pending request to ").color(NamedTextColor.RED)
                .append(target.displayName())
                .append(Component.text(".").color(NamedTextColor.RED));
            player.sendMessage(noRequestMessage);
            return true;
        }

        teleportRequests.remove(requestKey);
        cancelExpirationTask(requestKey);

        Component cancelledMessage = Component.text("You have cancelled your teleport request to ").color(NamedTextColor.YELLOW)
            .append(target.displayName())
            .append(Component.text(".").color(NamedTextColor.YELLOW));
        player.sendMessage(cancelledMessage);

        Component targetMessage = player.displayName()
            .append(Component.text(" has cancelled their teleport request.").color(NamedTextColor.YELLOW));
        target.sendMessage(targetMessage);

        return true;
    }

    private List<UUID> getPendingRequesters(UUID playerUUID) {
        List<UUID> requesters = new ArrayList<>();

        // Look through all requests to find ones targeting this player
        for (String requestKey : teleportRequests.keySet()) {
            String[] parts = requestKey.split(":");
            if (parts.length == 2 && parts[1].equals(playerUUID.toString())) {
                // This request is for the target player
                try {
                    UUID requesterUUID = UUID.fromString(parts[0]);
                    requesters.add(requesterUUID);
                } catch (IllegalArgumentException e) {
                    // Invalid UUID, skip
                }
            }
        }

        return requesters;
    }

    private void cancelExpirationTask(String requestKey) {
        BukkitTask task = expirationTasks.remove(requestKey);
        if (task != null) {
            task.cancel();
        }
    }
}
