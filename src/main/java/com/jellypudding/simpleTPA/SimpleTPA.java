package com.jellypudding.simpleTPA;

import com.jellypudding.simpleTPA.commands.PlayerCommand;
import com.jellypudding.simpleTPA.commands.TpaCommand;
import com.jellypudding.simpleTPA.commands.TpacancelCommand;
import com.jellypudding.simpleTPA.commands.TpacceptCommand;
import com.jellypudding.simpleTPA.commands.TpdenyCommand;
import com.jellypudding.simpleTPA.request.RequestManager;
import com.jellypudding.simpleTPA.request.RequestType;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SimpleTPA extends JavaPlugin {

    private RequestManager requestManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        requestManager = new RequestManager(this);

        register("tpa", new TpaCommand(requestManager, RequestType.TPA));
        register("tpahere", new TpaCommand(requestManager, RequestType.TPAHERE));
        register("tpaccept", new TpacceptCommand(requestManager));
        register("tpdeny", new TpdenyCommand(requestManager));
        register("tpacancel", new TpacancelCommand(requestManager));

        new Metrics(this, 27552);

        getLogger().info("SimpleTPA has been enabled.");
    }

    @Override
    public void onDisable() {
        if (requestManager != null) {
            requestManager.shutdown();
        }
        getLogger().info("SimpleTPA has been disabled.");
    }

    private void register(String name, PlayerCommand command) {
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand(name), "Command missing from plugin.yml: " + name);
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }
}
