package com.selfcoders.itemsorter;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryHelper {
    static List<Inventory> getInventories(List<Location> locations) {
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

    static List<Inventory> getInventoriesForType(List<Location> locations, Material type) {
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

    static Inventory getInventoryForLocation(Location location) {
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

    static void moveInventoryContentsToTargets(Inventory inventory, List<Location> targets) {
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null) {
                continue;
            }

            List<Inventory> targetInventories = InventoryHelper.getInventoriesForType(targets, stack.getType());
            if (targetInventories.isEmpty()) {
                continue;
            }

            InventoryHelper.moveStackToInventories(stack.clone(), inventory, targetInventories);
        }
    }

    static void moveStackToInventories(ItemStack itemStack, Inventory sourceInventory, List<Inventory> targetInventories) {
        int movedItems = 0;

        ItemStack removeStack = itemStack.clone();

        for (Inventory targetInventory : targetInventories) {
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
}