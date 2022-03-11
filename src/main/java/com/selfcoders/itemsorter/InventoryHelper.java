package com.selfcoders.itemsorter;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryHelper {
    private final boolean allowCrossWorldConnections;
    private final int maxDistance;

    InventoryHelper(boolean allowCrossWorldConnections, int maxDistance) {
        this.allowCrossWorldConnections = allowCrossWorldConnections;
        this.maxDistance = maxDistance;
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

    List<Inventory> getInventoriesForType(List<Location> locations, Material type) {
        List<Inventory> inventories = new ArrayList<>();

        for (Location location : locations) {
            Inventory inventory = getInventoryForLocation(location);
            if (inventory == null) {
                continue;
            }

            for (ItemStack stack : inventory.getContents()) {
                if (stack == null) {
                    continue;
                }

                if (stack.getType() == type) {
                    inventories.add(inventory);
                    break;
                }
            }
        }

        return inventories;
    }

    Inventory getInventoryForLocation(Location location) {
        Block signBlock = location.getBlock();

        Block attachedToBlock = SignHelper.getBlockFromSign(signBlock);
        if (attachedToBlock == null) {
            return null;
        }

        BlockState blockState = attachedToBlock.getState();
        if (!(blockState instanceof Container)) {
            return null;
        }

        Container container = (Container) blockState;
        return container.getInventory();
    }

    void moveInventoryContentsToTargets(Inventory inventory, List<Location> targets) {
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null) {
                continue;
            }

            List<Inventory> targetInventories = getInventoriesForType(targets, stack.getType());
            if (targetInventories.isEmpty()) {
                continue;
            }

            moveStackToInventories(stack.clone(), inventory, targetInventories);
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
                double distanceX = Math.abs(targetLocation.getX() - sourceLocation.getX());
                double distanceY = Math.abs(targetLocation.getY() - sourceLocation.getY());
                double distanceZ = Math.abs(targetLocation.getZ() - sourceLocation.getZ());

                if (!isSameWorld || distanceX > maxDistance || distanceY > maxDistance || distanceZ > maxDistance) {
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
}