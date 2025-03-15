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

    // Map to store teleport requests: key is target player UUID, value is requesting player UUID
    private final HashMap<UUID, UUID> teleportRequests = new HashMap<>();
    
    // Map to store request expiration tasks
    private final HashMap<UUID, BukkitTask> expirationTasks = new HashMap<>();
    
    // Map to store cooldowns: key is player UUID, value is time when cooldown expires
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    // Timeout and cooldown durations from config
    private long requestTimeoutTicks;
    private long requestCooldownMillis;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Load values from config
        loadConfigValues();
        
        // Register commands with the plugin
        Objects.requireNonNull(getCommand("tpa")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpa")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("tpaccept")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpaccept")).setTabCompleter(this);
        
        getLogger().info("SimpleTPA has been enabled.");
    }

    private void loadConfigValues() {
        // Get request timeout in seconds from config (default 120 seconds / 2 minutes)
        int timeoutSeconds = getConfig().getInt("request-timeout", 120);
        // Convert to ticks (20 ticks = 1 second)
        requestTimeoutTicks = timeoutSeconds * 20L;
        
        // Get cooldown in seconds from config (default 10 seconds)
        int cooldownSeconds = getConfig().getInt("request-cooldown", 10);
        // Convert to milliseconds
        requestCooldownMillis = cooldownSeconds * 1000L;
        
        getLogger().info("Config loaded: timeout=" + timeoutSeconds + "s, cooldown=" + cooldownSeconds + "s");
    }

    @Override
    public void onDisable() {
        // Cancel all pending tasks
        expirationTasks.values().forEach(BukkitTask::cancel);
        teleportRequests.clear();
        expirationTasks.clear();
        cooldowns.clear();
        
        getLogger().info("SimpleTPA has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("tpa")) {
            return handleTpaCommand(player, args);
        } else if (command.getName().equalsIgnoreCase("tpaccept")) {
            return handleTpacceptCommand(player, args);
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
            // Get the names of players who have sent a request to this player
            String partialName = args[0].toLowerCase();
            UUID playerUUID = player.getUniqueId();
            
            return getPendingRequesters(playerUUID).stream()
                    .map(uuid -> Bukkit.getPlayer(uuid))
                    .filter(Objects::nonNull)
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
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

        // Cancel any existing request from this player
        cancelExistingRequests(player.getUniqueId());

        // Store the new request
        teleportRequests.put(target.getUniqueId(), player.getUniqueId());
        
        // Apply cooldown
        cooldowns.put(playerUUID, currentTime + requestCooldownMillis);

        // Schedule task to expire the request after configured time
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (teleportRequests.containsKey(target.getUniqueId()) && 
                teleportRequests.get(target.getUniqueId()).equals(player.getUniqueId())) {
                teleportRequests.remove(target.getUniqueId());
                expirationTasks.remove(target.getUniqueId());
                player.sendMessage(Component.text("Your teleport request to " + target.getName() + " has expired.").color(NamedTextColor.RED));
                target.sendMessage(Component.text("Teleport request from " + player.getName() + " has expired.").color(NamedTextColor.RED));
            }
        }, requestTimeoutTicks);

        // Store the task for cancellation if needed
        expirationTasks.put(target.getUniqueId(), task);

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
        player.sendMessage(Component.text("Teleport request sent to " + target.getName() + ".").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("This request will expire in " + timeoutDisplay + ".").color(NamedTextColor.YELLOW));
        target.sendMessage(Component.text(player.getName() + " has requested to teleport to you.").color(NamedTextColor.GREEN));
        target.sendMessage(Component.text("Type /tpaccept " + player.getName() + " to accept. This request will expire in " + timeoutDisplay + ".").color(NamedTextColor.YELLOW));

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
                    player.sendMessage(Component.text(" - " + requester.getName()).color(NamedTextColor.GOLD));
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
        
        // Check if this player actually sent a request
        if (!pendingRequesters.contains(requesterUUID)) {
            player.sendMessage(Component.text("You don't have a pending request from " + requesterName + ".").color(NamedTextColor.RED));
            return true;
        }

        // Teleport the requester to the target
        requester.teleport(player.getLocation());

        // Send messages
        requester.sendMessage(Component.text("Teleported to " + player.getName() + ".").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text(requester.getName() + " has been teleported to you.").color(NamedTextColor.GREEN));

        // Clean up the request
        teleportRequests.remove(targetUUID);
        cancelExpirationTask(targetUUID);

        return true;
    }
    
    /**
     * Gets a list of UUIDs for players who have sent teleport requests to the specified player.
     *
     * @param playerUUID UUID of the player to check for requests
     * @return List of UUIDs for players who have sent requests
     */
    private List<UUID> getPendingRequesters(UUID playerUUID) {
        if (!teleportRequests.containsKey(playerUUID)) {
            return Collections.emptyList();
        }
        
        return Collections.singletonList(teleportRequests.get(playerUUID));
    }

    private void cancelExistingRequests(UUID playerUUID) {
        // Check if this player has sent any requests
        Optional<Map.Entry<UUID, UUID>> existingRequest = teleportRequests.entrySet().stream()
                .filter(entry -> entry.getValue().equals(playerUUID))
                .findFirst();

        existingRequest.ifPresent(entry -> {
            teleportRequests.remove(entry.getKey());
            cancelExpirationTask(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target != null && target.isOnline()) {
                target.sendMessage(Component.text("Previous teleport request from " + 
                        Bukkit.getPlayer(playerUUID).getName() + " has been cancelled.").color(NamedTextColor.YELLOW));
            }
        });
    }

    private void cancelExpirationTask(UUID targetUUID) {
        BukkitTask task = expirationTasks.remove(targetUUID);
        if (task != null) {
            task.cancel();
        }
    }
}
