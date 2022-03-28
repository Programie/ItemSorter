package com.selfcoders.itemsorter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ItemSorter extends JavaPlugin {
    private Database database;
    private ItemTransferTask itemTransferTask;

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
        int transferInterval = config.getInt("transfer-interval", 10);

        try {
            database = new Database(this);
        } catch (Exception exception) {
            getLogger().severe("Unable to initialize database: " + exception.getMessage());
            pluginManager.disablePlugin(this);
            return;
        }

        InventoryHelper inventoryHelper = new InventoryHelper(this, allowCrossWorldConnections, maxDistance, allowMultiChests, maxMultiChestsBlocks);
        pluginManager.registerEvents(new EventListener(this, inventoryHelper, allowCrossWorldConnections, maxDistance, maxNamesPerPlayer, maxSignsPerName), this);

        itemTransferTask = new ItemTransferTask(inventoryHelper);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, itemTransferTask, 0, transferInterval);
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

    void addItemTransfer(ItemStack itemStack, Inventory sourceInventory, List<Inventory> targetInventories) {
        itemTransferTask.addTransfer(itemStack, sourceInventory, targetInventories);
    }
}