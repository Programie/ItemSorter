package com.selfcoders.itemsorter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Arrays;
import java.util.List;

public class SignHelper {
    final static String TAG_SOURCE = "[ItemSource]";
    final static String TAG_TARGET = "[ItemTarget]";
    static final String TYPE_SOURCE = "source";
    static final String TYPE_TARGET = "target";
    final static List<BlockFace> BLOCK_FACES = Arrays.asList(BlockFace.UP, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH);

    /**
     * Get the ItemSorter sign attached to the specified block or null if not found.
     *
     * @param block the block to use (i.e. a chest)
     * @return Sign or null
     */
    static Sign getSignAttachedToBlock(Block block) {
        for (BlockFace blockFace : BLOCK_FACES) {
            Block faceBlock = block.getRelative(blockFace);
            Material faceBlockType = faceBlock.getType();

            if (!Tag.WALL_SIGNS.isTagged(faceBlockType)) {
                continue;
            }

            Sign signBlock = (Sign) faceBlock.getState();
            BlockFace attachedFace = ((WallSign) signBlock.getBlockData()).getFacing();

            if (!blockFace.equals(attachedFace)) {
                continue;
            }

            if (!checkSign(signBlock)) {
                continue;
            }

            return signBlock;
        }

        return null;
    }

    /**
     * Get the sign from the specified block or null if it is not an ItemSorter sign.
     *
     * @param block the block of the sign (the sign itself, not the block it is attached to)
     * @return Sign or null
     */
    static Sign getSignFromBlock(Block block) {
        BlockData blockData = block.getBlockData();

        if (!(blockData instanceof WallSign) && !(blockData instanceof org.bukkit.block.data.type.Sign)) {
            return null;
        }

        BlockState blockState = block.getState();

        if (!(blockState instanceof org.bukkit.block.Sign)) {
            return null;
        }

        Sign signBlock = (Sign) blockState;

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
        BlockData blockData = signBlock.getBlockData();

        if (!(blockData instanceof WallSign)) {
            return null;
        }

        WallSign sign = (WallSign) blockData;
        return signBlock.getRelative(sign.getFacing().getOppositeFace());
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
        String tagLine = ChatColor.stripColor(sign.getLine(0));

        return tagLine.equalsIgnoreCase(TAG_SOURCE) || tagLine.equalsIgnoreCase(TAG_TARGET);
    }
}