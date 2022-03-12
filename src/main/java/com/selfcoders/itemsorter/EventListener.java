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
            event.setLine(0, ChatColor.BLUE + SignHelper.SOURCE_TAG);
        } else if (signData.isTarget()) {
            event.setLine(0, ChatColor.BLUE + SignHelper.TARGET_TAG);
        } else {
            player.sendMessage(ChatColor.RED + "First line must be either [ItemSource] or [ItemTarget]!");
            block.breakNaturally();
            return;
        }

        event.setLine(2, player.getName());

        ItemLink itemLink = plugin.getItemLink(signData);

        if (signData.isSource()) {
            itemLink.addSource(block.getLocation(), signData.order);
        } else if (signData.isTarget()) {
            itemLink.addTarget(block.getLocation(), signData.order);
        }

        plugin.saveConfig();

        Container containerBlock = (Container) attachedToBlockState;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> updateInventory(containerBlock.getInventory()), 1L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();
        org.bukkit.block.Sign signBlock;

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

        ItemLink itemLink = plugin.getItemLink(signData);

        if (signData.isSource()) {
            itemLink.removeSource(block.getLocation());
        } else if (signData.isTarget()) {
            itemLink.removeTarget(block.getLocation());
        }

        plugin.saveConfig();
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

    private void updateInventoryForSource(ItemLink itemLink, Inventory inventory) {
        inventoryHelper.moveInventoryContentsToTargets(inventory, itemLink.getTargets());
    }

    private void updateInventoryForTarget(ItemLink itemLink) {
        List<Inventory> inventories = inventoryHelper.getInventories(itemLink.getSources());

        for (Inventory inventory : inventories) {
            inventoryHelper.moveInventoryContentsToTargets(inventory, itemLink.getTargets());
        }
    }

    private void updateInventory(Inventory inventory) {
        SignData signData = SignHelper.getSignDataForInventory(inventory);
        if (signData == null) {
            return;
        }

        ItemLink itemLink = plugin.getItemLink(signData);

        if (signData.isSource()) {
            updateInventoryForSource(itemLink, inventory);
        } else if (signData.isTarget()) {
            updateInventoryForTarget(itemLink);
        }
    }
}