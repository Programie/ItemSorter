package com.selfcoders.itemsorter;

import com.selfcoders.bukkitlibrary.BlockUtils;
import com.selfcoders.bukkitlibrary.SignUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public class SignHelper {
    final static String TAG_SOURCE = "[ItemSource]";
    final static String TAG_TARGET = "[ItemTarget]";
    static final String TYPE_SOURCE = "source";
    static final String TYPE_TARGET = "target";

    /**
     * Get the ItemSorter sign attached to the specified block or null if not found.
     *
     * @param block the block to use (i.e. a chest)
     * @return Sign or null
     */
    static Sign getSignAttachedToBlock(Block block) {
        if (!BlockUtils.isChunkLoaded(block)) {
            return null;
        }

        List<Sign> signs = SignUtils.getSignsAttachedToBlock(block, SignHelper::checkSign);

        if (signs.isEmpty()) {
            return null;
        }

        return signs.get(0);
    }

    /**
     * Get the sign from the specified block or null if it is not an ItemSorter sign.
     *
     * @param block the block of the sign (the sign itself, not the block it is attached to)
     * @return Sign or null
     */
    static Sign getSignFromBlock(Block block) {
        if (!BlockUtils.isChunkLoaded(block)) {
            return null;
        }

        Sign signBlock = SignUtils.getSignFromBlock(block);
        if (!checkSign(signBlock)) {
            return null;
        }

        return signBlock;
    }

    /**
     * Get the block the sign is attached to.
     *
     * @param signBlock The block of the sign
     * @return The block the sign is attached to or null if the sign block is not a wall sign
     */
    static Block getBlockFromSign(Block signBlock) {
        if (!BlockUtils.isChunkLoaded(signBlock)) {
            return null;
        }

        return SignUtils.getSignFromAttachedBlock(signBlock);
    }

    /**
     * Get the inventory of the chest the sign at the given location is attached to.
     *
     * @param location The location of the sign
     * @return The inventory of the chest or null if the sign is not attached to a chest
     */
    static Inventory getInventoryFromSignLocation(Location location) {
        Block attachedToBlock = getBlockFromSign(location.getBlock());
        if (attachedToBlock == null) {
            return null;
        }

        return InventoryHelper.getInventoryForBlock(attachedToBlock);
    }

    /**
     * Translate a list of locations of signs to their attached block locations.
     *
     * @param signLocations A list of locations of the signs
     * @return A list of locations of the blocks the signs are attached to
     */
    static List<Location> getBlockLocationsFromSignLocations(List<Location> signLocations) {
        List<Location> blockLocations = new ArrayList<>();

        for (Location signLocation : signLocations) {
            Block block = getBlockFromSign(signLocation.getBlock());
            if (block == null) {
                continue;
            }

            blockLocations.add(block.getLocation());
        }

        return blockLocations;
    }

    static SignData getSignDataForContainer(Container container) {
        Sign sign = SignHelper.getSignAttachedToBlock(container.getBlock());
        if (sign == null) {
            return null;
        }

        return new SignData(sign);
    }

    static SignData getSignDataForInventory(Inventory inventory) {
        InventoryHolder inventoryHolder = inventory.getHolder();

        if (inventoryHolder instanceof Container) {
            return getSignDataForContainer((Container) inventoryHolder);
        }

        // Double chests are a bit special...
        if (inventoryHolder instanceof DoubleChest) {
            InventoryHolder leftSide = ((DoubleChest) inventoryHolder).getLeftSide();
            if (leftSide instanceof Container) {
                SignData signData = getSignDataForContainer((Container) leftSide);
                if (signData != null) {
                    return signData;
                }
            }

            InventoryHolder rightSide = ((DoubleChest) inventoryHolder).getRightSide();
            if (rightSide instanceof Container) {
                return getSignDataForContainer((Container) rightSide);
            }
        }

        return null;
    }

    static boolean checkSign(Sign sign) {
        if (sign == null) {
            return false;
        }

        String tagLine = ChatColor.stripColor(sign.getLine(0));

        return tagLine.equalsIgnoreCase(TAG_SOURCE) || tagLine.equalsIgnoreCase(TAG_TARGET);
    }
}
