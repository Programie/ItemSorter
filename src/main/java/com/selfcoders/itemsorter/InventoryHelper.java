package com.selfcoders.itemsorter;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InventoryHelper {
    private final boolean allowCrossWorldConnections;
    private final int maxDistance;
    private final boolean allowMultiChests;
    private final int maxMultiChestsBlocks;

    InventoryHelper(boolean allowCrossWorldConnections, int maxDistance, boolean allowMultiChests, int maxMultiChestsBlocks) {
        this.allowCrossWorldConnections = allowCrossWorldConnections;
        this.maxDistance = maxDistance;
        this.allowMultiChests = allowMultiChests;
        this.maxMultiChestsBlocks = maxMultiChestsBlocks;
    }

    List<Inventory> getInventories(List<Location> locations) {
        List<Inventory> inventories = new ArrayList<>();

        for (Location location : locations) {
            Inventory inventory = getInventoryForLocation(location);
            if (inventory == null) {
                continue;
            }

            inventories.add(inventory);
        }

        return inventories;
    }

    Map<Material, List<Inventory>> getInventoriesForPerType(List<Location> signLocations) {
        Map<Material, List<Inventory>> inventories = new HashMap<>();

        for (Location signLocation : signLocations) {
            Block signBlock = signLocation.getBlock();

            Sign sign = SignHelper.getSignFromBlock(signBlock);
            if (sign == null) {
                continue;
            }

            Block attachedToBlock = SignHelper.getBlockFromSign(signBlock);
            if (attachedToBlock == null) {
                continue;
            }

            Inventory inventory = getInventoryForBlock(attachedToBlock);
            if (inventory == null) {
                continue;
            }

            List<Inventory> blockInventories = new ArrayList<>();
            blockInventories.add(inventory);

            if (allowMultiChests) {
                SignData signData = new SignData(sign);
                if (signData.multiChests) {
                    List<Inventory> connectedInventories = getConnectedInventories(inventory);
                    if (connectedInventories != null) {
                        blockInventories.addAll(connectedInventories);
                    }
                }
            }

            for (Inventory blockInventory : blockInventories) {
                for (ItemStack stack : blockInventory.getContents()) {
                    if (stack == null) {
                        continue;
                    }

                    inventories.computeIfAbsent(stack.getType(), key -> new ArrayList<>()).add(blockInventory);
                }
            }
        }

        return inventories;
    }

    Inventory getInventoryForBlock(Block block) {
        BlockState blockState = block.getState();
        if (!(blockState instanceof Container)) {
            return null;
        }

        Container container = (Container) blockState;
        return container.getInventory();
    }

    Inventory getInventoryForLocation(Location location) {
        return getInventoryForBlock(location.getBlock());
    }

    void moveInventoryContentsToTargets(Inventory inventory, List<Location> targetSignLocations) {
        Map<Material, List<Inventory>> targetInventories = getInventoriesForPerType(targetSignLocations);

        for (ItemStack stack : inventory.getContents()) {
            if (stack == null) {
                continue;
            }

            List<Inventory> targetInventoriesForType = targetInventories.get(stack.getType());
            if (targetInventoriesForType == null) {
                continue;
            }

            moveStackToInventories(stack.clone(), inventory, targetInventoriesForType);
        }
    }

    void moveStackToInventories(ItemStack itemStack, Inventory sourceInventory, List<Inventory> targetInventories) {
        int movedItems = 0;

        Container sourceContainer = getContainerFromInventory(sourceInventory);
        if (sourceContainer == null) {
            return;
        }
        Location sourceLocation = sourceContainer.getLocation();

        ItemStack removeStack = itemStack.clone();

        for (Inventory targetInventory : targetInventories) {
            Container targetContainer = getContainerFromInventory(targetInventory);
            if (targetContainer == null) {
                continue;
            }
            Location targetLocation = targetContainer.getLocation();
            boolean isSameWorld = targetLocation.getWorld() == sourceLocation.getWorld();

            if (!allowCrossWorldConnections && !isSameWorld) {
                continue;
            }

            if (maxDistance > 0) {
                Integer distance = Util.getDistance(sourceLocation, targetLocation);
                if (distance == null || distance > maxDistance) {
                    continue;
                }
            }

            int amount = itemStack.getAmount();
            int remainingAmount = 0;

            Map<Integer, ItemStack> remainingItems = targetInventory.addItem(itemStack);

            if (!remainingItems.isEmpty()) {
                itemStack = remainingItems.get(0);
                remainingAmount = itemStack.getAmount();
            }

            movedItems += (amount - remainingAmount);

            if (remainingAmount == 0) {
                break;
            }
        }

        if (movedItems > 0) {
            removeStack.setAmount(movedItems);
            sourceInventory.removeItem(removeStack);
        }
    }

    Container getContainerFromInventory(Inventory inventory) {
        InventoryHolder inventoryHolder = inventory.getHolder();

        if (inventoryHolder instanceof Container) {
            return (Container) inventoryHolder;
        }

        // Double chests are a bit special...
        if (inventoryHolder instanceof DoubleChest) {
            InventoryHolder leftSide = ((DoubleChest) inventoryHolder).getLeftSide();
            if (leftSide instanceof Container) {
                return (Container) leftSide;
            }

            InventoryHolder rightSide = ((DoubleChest) inventoryHolder).getRightSide();
            if (rightSide instanceof Container) {
                return (Container) rightSide;
            }
        }

        return null;
    }

    List<Inventory> getConnectedInventories(Inventory inventory) {
        InventoryHolder inventoryHolder = inventory.getHolder();

        if (inventoryHolder instanceof Container) {
            return getConnectedInventories((Container) inventoryHolder);
        }

        // Double chests are a bit special...
        if (inventoryHolder instanceof DoubleChest) {
            InventoryHolder leftSide = ((DoubleChest) inventoryHolder).getLeftSide();
            if (leftSide instanceof Container) {
                return getConnectedInventories((Container) leftSide);
            }

            InventoryHolder rightSide = ((DoubleChest) inventoryHolder).getRightSide();
            if (rightSide instanceof Container) {
                return getConnectedInventories((Container) rightSide);
            }
        }

        return null;
    }

    List<Inventory> getConnectedInventories(Container container) {
        List<Inventory> connectedInventories = new ArrayList<>();
        Set<Block> connectedBlocks = BlockHelper.getConnectedBlocks(container.getBlock(), maxMultiChestsBlocks);

        for (Block block : connectedBlocks) {
            BlockState blockState = block.getState();

            if (blockState instanceof Container) {
                connectedInventories.add(((Container) blockState).getInventory());
            }
        }

        return connectedInventories;
    }
}