package com.selfcoders.itemsorter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemSorter extends JavaPlugin {
    private Database database;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginManager pluginManager = getServer().getPluginManager();

        FileConfiguration config = getConfig();

        boolean allowCrossWorldConnections = config.getBoolean("allow-cross-world-connections");
        int maxDistance = config.getInt("max-distance");

        try {
            database = new Database(this);
        } catch (Exception exception) {
            getLogger().severe("Unable to initialize database: " + exception.getMessage());
            pluginManager.disablePlugin(this);
            return;
        }

        InventoryHelper inventoryHelper = new InventoryHelper(allowCrossWorldConnections, maxDistance);
        pluginManager.registerEvents(new EventListener(this, inventoryHelper, allowCrossWorldConnections, maxDistance), this);
    }

    Database getDatabase() {
        return database;
    }
}