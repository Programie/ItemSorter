package com.selfcoders.itemsorter;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemSorter extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new EventListener(this), this);
    }

    ItemLink getItemLink(String name) {
        return new ItemLink(getConfig(), this, name);
    }
}