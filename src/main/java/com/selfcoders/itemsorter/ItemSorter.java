package com.selfcoders.itemsorter;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemSorter extends JavaPlugin {
    private Database database;
    private ItemTransferTask itemTransferTask;
    private Set<Location> signLocations;

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
        boolean allowPersistentItems = config.getBoolean("allow-persistent-items", true);
        int transferInterval = config.getInt("transfer-interval", 10);

        try {
            database = new Database(this);
        } catch (Exception exception) {
            getLogger().severe("Unable to initialize database: " + exception.getMessage());
            pluginManager.disablePlugin(this);
            return;
        }

        signLocations = new HashSet<>();

        try {
            signLocations.addAll(getDatabase().getLocations());
        } catch (Exception exception) {
            getLogger().severe("Unable to load sign locations from database: " + exception.getMessage());
            pluginManager.disablePlugin(this);
            return;
        }

        InventoryHelper inventoryHelper = new InventoryHelper(this, allowCrossWorldConnections, maxDistance, allowMultiChests, maxMultiChestsBlocks, allowPersistentItems);
        pluginManager.registerEvents(new EventListener(this, inventoryHelper, allowCrossWorldConnections, maxDistance, maxNamesPerPlayer, maxSignsPerName), this);

        itemTransferTask = new ItemTransferTask(inventoryHelper);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, itemTransferTask, 0, transferInterval);

        for (Location location : signLocations) {
            Inventory inventory = SignHelper.getInventoryFromSignLocation(location);
            if (inventory != null) {
                inventoryHelper.updateInventory(inventory);
            }
        }
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

    Set<Location> getSignLocations() {
        return signLocations;
    }

    void addSignLocation(Location location) {
        signLocations.add(location);
    }

    void removeSignLocation(Location location) {
        signLocations.remove(location);
    }

    void addItemTransfer(ItemStack itemStack, Inventory sourceInventory, List<Inventory> targetInventories) {
        itemTransferTask.addTransfer(itemStack, sourceInventory, targetInventories);
    }
}
