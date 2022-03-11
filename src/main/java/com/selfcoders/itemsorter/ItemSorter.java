package com.selfcoders.itemsorter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemSorter extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginManager pluginManager = getServer().getPluginManager();

        FileConfiguration config = getConfig();

        boolean allowCrossWorldConnections = config.getBoolean("allow-cross-world-connections");
        int maxDistance = config.getInt("max-distance");

        InventoryHelper inventoryHelper = new InventoryHelper(allowCrossWorldConnections, maxDistance);
        pluginManager.registerEvents(new EventListener(this, inventoryHelper), this);
    }

    ItemLink getItemLink(String name) {
        return new ItemLink(getConfig(), this, name);
    }
}