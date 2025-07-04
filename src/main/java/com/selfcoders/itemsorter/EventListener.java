package com.selfcoders.itemsorter;

import com.selfcoders.bukkitlibrary.LocationUtils;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EventListener implements Listener {
    private final ItemSorter plugin;
    private final InventoryHelper inventoryHelper;
    private final boolean allowCrossWorldConnections;
    private final int maxDistance;
    private final int maxNamesPerPlayer;
    private final int maxSignsPerName;

    public EventListener(ItemSorter plugin, InventoryHelper inventoryHelper, boolean allowCrossWorldConnections, int maxDistance, int maxNamesPerPlayer, int maxSignsPerName) {
        this.plugin = plugin;
        this.inventoryHelper = inventoryHelper;
        this.allowCrossWorldConnections = allowCrossWorldConnections;
        this.maxDistance = maxDistance;
        this.maxNamesPerPlayer = maxNamesPerPlayer;
        this.maxSignsPerName = maxSignsPerName;
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
            event.setCancelled(true);
            return;
        }

        if (!(blockData instanceof WallSign)) {
            player.sendMessage(ChatColor.RED + "An ItemSorter sign must be attached to an inventory block (i.e. chest or hopper)!");
            event.setCancelled(true);
            return;
        }

        WallSign sign = (WallSign) blockData;
        Block attachedToBlock = block.getRelative(sign.getFacing().getOppositeFace());
        BlockState attachedToBlockState = attachedToBlock.getState();
        if (!(attachedToBlockState instanceof Container)) {
            player.sendMessage(ChatColor.RED + "An ItemSorter sign must be attached to an inventory block (i.e. chest or hopper)!");
            event.setCancelled(true);
            return;
        }

        if (!signData.checkName()) {
            player.sendMessage(ChatColor.RED + "No name specified on the second line!");
            event.setCancelled(true);
            return;
        }

        if (signData.isSource()) {
            event.setLine(0, ChatColor.BLUE + SignHelper.TAG_SOURCE);
        } else if (signData.isTarget()) {
            event.setLine(0, ChatColor.BLUE + SignHelper.TAG_TARGET);
        } else {
            player.sendMessage(ChatColor.RED + "First line must be either [ItemSource] or [ItemTarget]!");
            event.setCancelled(true);
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

        Location signLocation = block.getLocation();

        if (maxNamesPerPlayer > 0) {
            try {
                List<String> usedNames = plugin.getDatabase().getNames(player);

                int usedNamesCount = 0;
                for (String usedName : usedNames) {
                    if (usedName.equalsIgnoreCase(signData.name)) {
                        continue;
                    }

                    usedNamesCount++;
                }

                if (usedNamesCount >= maxNamesPerPlayer) {
                    player.sendMessage(ChatColor.RED + "You've reached the maximum number of different sign names (" + maxNamesPerPlayer + ")!");
                    event.setCancelled(true);
                    return;
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Unable to count names in database: " + exception.getMessage());
                player.sendMessage(ChatColor.RED + "An error occurred while adding the sign!");
                event.setCancelled(true);
                return;
            }
        }

        if (maxSignsPerName > 0) {
            try {
                int usedSigns = plugin.getDatabase().getLocations(player, signData.name).size();
                if (usedSigns >= maxSignsPerName) {
                    player.sendMessage(ChatColor.RED + "You've reached the maximum number of signs for '" + signData.name + "' (" + maxSignsPerName + ")!");
                    event.setCancelled(true);
                    return;
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Unable to count signs in database: " + exception.getMessage());
                player.sendMessage(ChatColor.RED + "An error occurred while adding the sign!");
                event.setCancelled(true);
                return;
            }
        }

        try {
            plugin.getDatabase().addLocation(player, signData.name, type, signLocation, signData.order);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to add location to database: " + exception.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while adding the sign!");
            event.setCancelled(true);
            return;
        }

        plugin.addSignLocation(signLocation);

        player.sendMessage(ChatColor.GREEN + "ItemSorter sign placed successfully.");

        try {
            List<Location> otherLocations = plugin.getDatabase().getLocations(player, signData.name, signData.isSource() ? SignHelper.TYPE_TARGET : SignHelper.TYPE_SOURCE);
            int reachableLocations = 0;
            int totalLocations = otherLocations.size();

            for (Location otherLocation : otherLocations) {
                Integer distance = LocationUtils.getDistance(signLocation, otherLocation);

                if (allowCrossWorldConnections && distance == null) {
                    reachableLocations++;
                    continue;
                }

                if (maxDistance == 0 && distance != null) {
                    reachableLocations++;
                    continue;
                }

                if (distance != null && distance <= maxDistance) {
                    reachableLocations++;
                }
            }

            if (totalLocations > 0) {
                if (reachableLocations == 0) {
                    player.sendMessage(ChatColor.RED + "This chest is out of range of any other chest!");
                } else if (reachableLocations < totalLocations) {
                    player.sendMessage(ChatColor.YELLOW + "This chest is now connected to " + totalLocations + " other chests but only " + reachableLocations + " are reachable.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "This chest is now connected to " + totalLocations + " other chests.");
                }
            } else {
                player.sendMessage(ChatColor.GREEN + "This chest is not connected to any other chest.");
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
        }

        Container containerBlock = (Container) attachedToBlockState;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> inventoryHelper.updateInventory(containerBlock.getInventory()), 1L);
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

        plugin.removeSignLocation(signBlock.getLocation());

        player.sendMessage(ChatColor.GREEN + "ItemSorter sign removed successfully.");

        try {
            int totalLocations = plugin.getDatabase().getLocations(player, signData.name).size();
            player.sendMessage(ChatColor.GREEN + "You own " + totalLocations + " other chests with the same name.");
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Material blockType;
        Sign signBlock;

        for (Block block : event.blockList()) {
            blockType = block.getType();

            if (Tag.SIGNS.isTagged(blockType)) {
                // Is a sign
                signBlock = SignHelper.getSignFromBlock(block);
            } else {
                // Is a block which might has a sign attached to it
                signBlock = SignHelper.getSignAttachedToBlock(block);
            }

            if (signBlock == null) {
                continue;
            }

            SignData signData = new SignData(signBlock.getLines());

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
            }

            plugin.removeSignLocation(signBlock.getLocation());
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Material blockType = block.getType();
        if (!Tag.SIGNS.isTagged(blockType)) {
            return;
        }

        Sign signBlock = SignHelper.getSignFromBlock(block);
        if (signBlock == null) {
            return;
        }

        SignData signData = new SignData(signBlock.getLines());
        if (!signData.isItemSorterSign()) {
            return;
        }

        // Sign should not be editable
        // Probably better to set this while creating the sign instead of while interacting with it
        if (!signBlock.isWaxed()) {
            signBlock.setWaxed(true);
            signBlock.update();
        }

        Location signLocation = signBlock.getLocation();
        List<Location> sourceLocations = null;
        List<Location> targetLocations = null;

        try {
            sourceLocations = plugin.getDatabase().getLocations(signData.player, signData.name, SignHelper.TYPE_SOURCE);
            targetLocations = plugin.getDatabase().getLocations(signData.player, signData.name, SignHelper.TYPE_TARGET);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
        }

        List<String> messages = new ArrayList<>();
        messages.add(ChatColor.GREEN + "*** Info for this ItemSorter sign ***");
        messages.add("This sign is owned by " + ChatColor.YELLOW + signData.player);
        if (sourceLocations != null && targetLocations != null) {
            messages.add("There are " + ChatColor.YELLOW + (sourceLocations.size() + targetLocations.size()) + ChatColor.RESET + " signs using the name " + ChatColor.YELLOW + signData.name + ChatColor.RESET + " (" + ChatColor.YELLOW + sourceLocations.size() + ChatColor.RESET + " sources and " + ChatColor.YELLOW + targetLocations.size() + ChatColor.RESET + " targets)");
            if (signData.isSource()) {
                addMessageForLocations(messages, signLocation, targetLocations);
            } else if (signData.isTarget()) {
                addMessageForLocations(messages, signLocation, sourceLocations);
            }
        }

        event.getPlayer().sendMessage(messages.toArray(new String[0]));
    }

    @EventHandler
    public void onChunkLoadEvent(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        int x = chunk.getX();
        int z = chunk.getZ();

        for (Location location : plugin.getSignLocations()) {
            if (location.getBlockX() / 16 == x && location.getBlockZ() / 16 == z) {
                Inventory inventory = SignHelper.getInventoryFromSignLocation(location);
                if (inventory != null) {
                    inventoryHelper.updateInventory(inventory);
                }

                break;
            }
        }
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        inventoryHelper.updateInventory(event.getInventory());
    }

    @EventHandler
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            inventoryHelper.updateInventory(event.getSource());
            inventoryHelper.updateInventory(event.getDestination());
        }, 1L);
    }

    @EventHandler
    public void onInventoryPickupItemEvent(InventoryPickupItemEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> inventoryHelper.updateInventory(event.getInventory()), 1L);
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> inventoryHelper.updateInventory(event.getInventory()), 1L);
    }


    private void addMessageForLocations(List<String> messages, Location signLocation, List<Location> locations) {
        for (Location location : locations) {
            Integer distance = LocationUtils.getDistance(signLocation, location);

            if (allowCrossWorldConnections && distance == null) {
                continue;
            }

            if (distance == null) {
                messages.add(ChatColor.RED + "Note: A connected sign is in a different world '" + location.getWorld().getName() + "' and therefore can't be reached!");
                continue;
            }

            if (maxDistance > 0 && distance > maxDistance) {
                messages.add(ChatColor.RED + "Note: Sign at " + location.getX() + ", " + location.getY() + ", " + location.getZ() + " exceeds the maximum distance (" + distance + " > " + maxDistance + " blocks)!");
            }
        }
    }
}
