package com.selfcoders.itemsorter;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;

import java.sql.SQLException;
import java.util.List;

public class EventListener implements Listener {
    private final ItemSorter plugin;
    private final InventoryHelper inventoryHelper;

    public EventListener(ItemSorter plugin, InventoryHelper inventoryHelper) {
        this.plugin = plugin;
        this.inventoryHelper = inventoryHelper;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        BlockData blockData = block.getBlockData();
        SignData signData = new SignData(event.getLines());

        if (!signData.isItemSorterSign()) {
            return;
        }

        if (!player.hasPermission("itemsorter.create")) {
            player.sendMessage(ChatColor.RED + "You do not have the required permissions to create ItemSorter signs!");
            block.breakNaturally();
            return;
        }

        if (!(blockData instanceof WallSign)) {
            player.sendMessage(ChatColor.RED + "An ItemSorter sign must be attached to an inventory block (i.e. chest or hopper)!");
            block.breakNaturally();
            return;
        }

        WallSign sign = (WallSign) blockData;
        Block attachedToBlock = block.getRelative(sign.getFacing().getOppositeFace());
        BlockState attachedToBlockState = attachedToBlock.getState();
        if (!(attachedToBlockState instanceof Container)) {
            player.sendMessage(ChatColor.RED + "An ItemSorter sign must be attached to an inventory block (i.e. chest or hopper)!");
            block.breakNaturally();
            return;
        }

        if (!signData.checkName()) {
            player.sendMessage(ChatColor.RED + "No name specified on the second line!");
            block.breakNaturally();
            return;
        }

        if (signData.isSource()) {
            event.setLine(0, ChatColor.BLUE + SignHelper.TAG_SOURCE);
        } else if (signData.isTarget()) {
            event.setLine(0, ChatColor.BLUE + SignHelper.TAG_TARGET);
        } else {
            player.sendMessage(ChatColor.RED + "First line must be either [ItemSource] or [ItemTarget]!");
            block.breakNaturally();
            return;
        }

        event.setLine(2, player.getName());

        // Ensure name of player is not modified
        signData.player = player.getName();

        String type;
        if (signData.isSource()) {
            type = SignHelper.TYPE_SOURCE;
        } else if (signData.isTarget()) {
            type = SignHelper.TYPE_TARGET;
        } else {
            return;
        }

        try {
            plugin.getDatabase().addLocation(player, signData.name, type, block.getLocation(), signData.order);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to add location to database: " + exception.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while adding the sign!");
            block.breakNaturally();
            return;
        }

        player.sendMessage(ChatColor.GREEN + "ItemSorter sign placed successfully.");

        try {
            int totalLocations = plugin.getDatabase().getLocations(player, signData.name, signData.isSource() ? SignHelper.TYPE_TARGET : SignHelper.TYPE_SOURCE).size();
            player.sendMessage(ChatColor.GREEN + "This chest is now connected to " + totalLocations + " other chests.");
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
        }

        Container containerBlock = (Container) attachedToBlockState;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> updateInventory(containerBlock.getInventory()), 1L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();
        Sign signBlock;

        if (Tag.SIGNS.isTagged(blockType)) {
            // Is a sign
            signBlock = SignHelper.getSignFromBlock(block);
        } else {
            // Is a block which might has a sign attached to it
            signBlock = SignHelper.getSignAttachedToBlock(block);
        }

        if (signBlock == null) {
            return;
        }

        if (!player.hasPermission("itemsorter.create") && !player.hasPermission("itemsorter.destroyAny")) {
            player.sendMessage(ChatColor.RED + "You do not have the required permissions to destroy ItemSorter signs!");
            event.setCancelled(true);
            return;
        }

        SignData signData = new SignData(signBlock.getLines());

        if (!player.hasPermission("itemsorter.destroyAny")) {
            Player signOwner = plugin.getServer().getPlayer(signData.player);
            if (signOwner != null && signOwner.getUniqueId() != player.getUniqueId()) {
                player.sendMessage(ChatColor.RED + "You can only destroy your own ItemSorter signs!");
                event.setCancelled(true);
                return;
            }
        }

        String type;
        if (signData.isSource()) {
            type = SignHelper.TYPE_SOURCE;
        } else if (signData.isTarget()) {
            type = SignHelper.TYPE_TARGET;
        } else {
            return;
        }

        try {
            plugin.getDatabase().removeLocation(signData.player, signData.name, type, signBlock.getLocation());
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to remove location from database: " + exception.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while removing the sign!");
            event.setCancelled(true);
            return;
        }

        player.sendMessage(ChatColor.GREEN + "ItemSorter sign removed successfully.");

        try {
            int totalLocations = plugin.getDatabase().getLocations(player, signData.name).size();
            player.sendMessage(ChatColor.GREEN + "You own " + totalLocations + " other chests with the same name.");
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        updateInventory(event.getInventory());
    }

    @EventHandler
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            updateInventory(event.getSource());
            updateInventory(event.getDestination());
        }, 1L);
    }

    @EventHandler
    public void onInventoryPickupItemEvent(InventoryPickupItemEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> updateInventory(event.getInventory()), 1L);
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> updateInventory(event.getInventory()), 1L);
    }

    private void updateInventoryForSource(SignData signData, Inventory inventory) {
        List<Location> targetLocations;

        try {
            targetLocations = plugin.getDatabase().getLocations(signData.player, signData.name, SignHelper.TYPE_TARGET);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
            return;
        }

        inventoryHelper.moveInventoryContentsToTargets(inventory, targetLocations);
    }

    private void updateInventoryForTarget(SignData signData) {
        List<Location> sourceLocations;
        List<Location> targetLocations;

        try {
            sourceLocations = plugin.getDatabase().getLocations(signData.player, signData.name, SignHelper.TYPE_SOURCE);
            targetLocations = plugin.getDatabase().getLocations(signData.player, signData.name, SignHelper.TYPE_TARGET);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
            return;
        }

        List<Inventory> inventories = inventoryHelper.getInventories(sourceLocations);

        for (Inventory inventory : inventories) {
            inventoryHelper.moveInventoryContentsToTargets(inventory, targetLocations);
        }
    }

    private void updateInventory(Inventory inventory) {
        SignData signData = SignHelper.getSignDataForInventory(inventory);
        if (signData == null) {
            return;
        }

        if (signData.isSource()) {
            updateInventoryForSource(signData, inventory);
        } else if (signData.isTarget()) {
            updateInventoryForTarget(signData);
        }
    }
}