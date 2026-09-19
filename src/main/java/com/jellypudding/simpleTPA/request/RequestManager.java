package com.jellypudding.simpleTPA.request;

import com.jellypudding.simpleTPA.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RequestManager {

    private record Key(UUID requester, UUID target) {
        static Key of(TeleportRequest request) {
            return new Key(request.requester(), request.target());
        }
    }

    private final Plugin plugin;
    private final Map<Key, TeleportRequest> requests = new LinkedHashMap<>();
    private final Map<Key, BukkitTask> expirations = new HashMap<>();
    private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

    private long requestTimeoutTicks;
    private String timeoutDisplay;
    private long requestCooldownMillis;
    private boolean allowCrossWorld;

    public RequestManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        int timeoutSeconds = plugin.getConfig().getInt("request-timeout", 120);
        int cooldownSeconds = plugin.getConfig().getInt("request-cooldown", 10);

        requestTimeoutTicks = timeoutSeconds * 20L;
        timeoutDisplay = formatDuration(timeoutSeconds);
        requestCooldownMillis = cooldownSeconds * 1000L;
        allowCrossWorld = plugin.getConfig().getBoolean("allow-cross-world", false);

        plugin.getLogger().info("Config loaded: timeout=" + timeoutSeconds + "s, cooldown=" + cooldownSeconds
                + "s, cross-world=" + allowCrossWorld);
    }

    public void shutdown() {
        expirations.values().forEach(BukkitTask::cancel);
        expirations.clear();
        requests.clear();
        cooldownExpiry.clear();
    }

    public Optional<TeleportRequest> getRequest(UUID requester, UUID target) {
        return Optional.ofNullable(requests.get(new Key(requester, target)));
    }

    public List<TeleportRequest> getOutgoing(UUID requester) {
        return requests.values().stream().filter(r -> r.requester().equals(requester)).toList();
    }

    public List<TeleportRequest> getIncoming(UUID target) {
        return requests.values().stream().filter(r -> r.target().equals(target)).toList();
    }

    public void add(TeleportRequest request) {
        Key key = Key.of(request);
        requests.put(key, request);
        expirations.put(key, Bukkit.getScheduler().runTaskLater(plugin, () -> expire(request), requestTimeoutTicks));
    }

    public void remove(TeleportRequest request) {
        Key key = Key.of(request);
        requests.remove(key);
        BukkitTask task = expirations.remove(key);
        if (task != null) {
            task.cancel();
        }
    }

    private void expire(TeleportRequest request) {
        Key key = Key.of(request);
        if (!requests.remove(key, request)) {
            return;
        }
        expirations.remove(key);

        Player requester = Bukkit.getPlayer(request.requester());
        Player target = Bukkit.getPlayer(request.target());
        if (requester != null) {
            requester.sendMessage(Messages.error("Your teleport request to ", Messages.nameOf(target), " has expired."));
        }
        if (target != null) {
            target.sendMessage(Messages.error("Teleport request from ", Messages.nameOf(requester), " has expired."));
        }
    }

    public long getRemainingCooldownSeconds(UUID player) {
        Long expiry = cooldownExpiry.get(player);
        if (expiry == null) {
            return 0;
        }
        long remainingMillis = expiry - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            cooldownExpiry.remove(player);
            return 0;
        }
        return (remainingMillis + 999) / 1000;
    }

    public void startCooldown(UUID player) {
        cooldownExpiry.put(player, System.currentTimeMillis() + requestCooldownMillis);
    }

    public String getTimeoutDisplay() {
        return timeoutDisplay;
    }

    public boolean isAllowCrossWorld() {
        return allowCrossWorld;
    }

    private static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        List<String> parts = new ArrayList<>(2);
        if (minutes > 0) {
            parts.add(minutes + (minutes == 1 ? " minute" : " minutes"));
        }
        if (seconds > 0 || minutes == 0) {
            parts.add(seconds + (seconds == 1 ? " second" : " seconds"));
        }
        return String.join(" and ", parts);
    }
}
