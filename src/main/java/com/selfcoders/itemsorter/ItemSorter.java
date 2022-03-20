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

        boolean allowCrossWorldConnections = config.getBoolean("allow-cross-world-connections", false);
        int maxDistance = config.getInt("max-distance", 100);
        int maxNamesPerPlayer = config.getInt("max-names-per-player", 0);
        int maxSignsPerName = config.getInt("max-signs-per-name", 0);
        boolean allowMultiChests = config.getBoolean("allow-multi-chests", true);
        int maxMultiChestsBlocks = config.getInt("max-multi-chests-blocks", 0);

        try {
            database = new Database(this);
        } catch (Exception exception) {
            getLogger().severe("Unable to initialize database: " + exception.getMessage());
            pluginManager.disablePlugin(this);
            return;
        }

        InventoryHelper inventoryHelper = new InventoryHelper(allowCrossWorldConnections, maxDistance, allowMultiChests, maxMultiChestsBlocks);
        pluginManager.registerEvents(new EventListener(this, inventoryHelper, allowCrossWorldConnections, maxDistance, maxNamesPerPlayer, maxSignsPerName), this);
    }

    @Override
    public void onDisable() {
        try {
            database.close();
        } catch (Exception exception) {
            getLogger().severe("Error while closing the database: " + exception.getMessage());
        }
    }

    Database getDatabase() {
        return database;
    }
}