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

    public EventListener(ItemSorter plugin) {
        this.plugin = plugin;
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

        if (!(blockData instanceof WallSign)) {
            player.sendMessage(ChatColor.RED + "An ItemSorter sign must be attached to an inventory block (i.e. chest or hopper)!");
            block.breakNaturally();
            return;
        }

        WallSign sign = (WallSign) blockData;
        Block attachedToBlock = block.getRelative(sign.getFacing().getOppositeFace());
        if (!(attachedToBlock.getState() instanceof Container)) {
            player.sendMessage(ChatColor.RED + "An ItemSorter sign must be attached to an inventory block (i.e. chest or hopper)!");
            block.breakNaturally();
            return;
        }

        if (!signData.checkType()) {
            player.sendMessage(ChatColor.RED + "Text on the second line must be either '" + ItemLink.TYPE_SOURCE + "' or '" + ItemLink.TYPE_TARGET + "'!");
            block.breakNaturally();
            return;
        }

        if (!signData.checkName()) {
            player.sendMessage(ChatColor.RED + "No name specified on the third line!");
            block.breakNaturally();
            return;
        }

        event.setLine(0, ChatColor.BLUE + SignHelper.SIGN_TAG);
        event.setLine(1, signData.type.toUpperCase());

        ItemLink itemLink = plugin.getItemLink(signData.name);

        switch (signData.type.toLowerCase()) {
            case ItemLink.TYPE_SOURCE:
                itemLink.addSource(block.getLocation());
                break;
            case ItemLink.TYPE_TARGET:
                itemLink.addTarget(block.getLocation());
                break;
        }

        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "Sign correctly placed.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
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

        SignData signData = new SignData(signBlock.getLines());

        ItemLink itemLink = plugin.getItemLink(signData.name);

        switch (signData.type.toLowerCase()) {
            case ItemLink.TYPE_SOURCE:
                itemLink.removeSource(signBlock.getLocation());
                break;
            case ItemLink.TYPE_TARGET:
                itemLink.removeTarget(signBlock.getLocation());
                break;
        }

        plugin.saveConfig();
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        updateInventory(event.getInventory());
    }

    @EventHandler
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> updateInventory(event.getDestination()), 1L);
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
        InventoryHelper.moveInventoryContentsToTargets(inventory, itemLink.getTargets());
    }

    private void updateInventoryForTarget(ItemLink itemLink) {
        List<Inventory> inventories = InventoryHelper.getInventories(itemLink.getSources());

        for (Inventory inventory : inventories) {
            InventoryHelper.moveInventoryContentsToTargets(inventory, itemLink.getTargets());
        }
    }

    private void updateInventory(Inventory inventory) {
        SignData signData = SignHelper.getSignDataForInventory(inventory);
        if (signData == null) {
            return;
        }

        ItemLink itemLink = plugin.getItemLink(signData.name);

        switch (signData.type.toLowerCase()) {
            case ItemLink.TYPE_SOURCE:
                updateInventoryForSource(itemLink, inventory);
                break;
            case ItemLink.TYPE_TARGET:
                updateInventoryForTarget(itemLink);
                break;
        }
    }
}