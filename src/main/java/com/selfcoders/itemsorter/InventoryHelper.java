package com.selfcoders.itemsorter;

import com.selfcoders.bukkitlibrary.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.*;

public class InventoryHelper {
    private final ItemSorter plugin;
    private final boolean allowCrossWorldConnections;
    private final int maxDistance;
    private final boolean allowMultiChests;
    private final int maxMultiChestsBlocks;
    private final boolean allowPersistentItems;

    InventoryHelper(ItemSorter plugin, boolean allowCrossWorldConnections, int maxDistance, boolean allowMultiChests, int maxMultiChestsBlocks, boolean allowPersistentItems) {
        this.plugin = plugin;
        this.allowCrossWorldConnections = allowCrossWorldConnections;
        this.maxDistance = maxDistance;
        this.allowMultiChests = allowMultiChests;
        this.maxMultiChestsBlocks = maxMultiChestsBlocks;
        this.allowPersistentItems = allowPersistentItems;
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

            SignData signData = new SignData(sign);

            List<Inventory> blockInventories = new ArrayList<>();
            blockInventories.add(inventory);

            if (allowMultiChests && signData.multiChests) {
                List<Inventory> connectedInventories = getConnectedInventories(inventory);
                if (connectedInventories != null) {
                    blockInventories.addAll(connectedInventories);
                }
            }

            for (Inventory blockInventory : blockInventories) {
                ItemStack[] itemStacks = null;

                if (allowPersistentItems && signData.persistent) {
                    try {
                        itemStacks = plugin.getDatabase().getPersistentItems(blockInventory.getLocation()).toArray(new ItemStack[0]);
                    } catch (SQLException exception) {
                        // ignore
                    }
                } else {
                    itemStacks = blockInventory.getContents();
                }

                if (itemStacks == null) {
                    continue;
                }

                for (ItemStack stack : itemStacks) {
                    if (stack == null) {
                        continue;
                    }

                    inventories.computeIfAbsent(stack.getType(), key -> new ArrayList<>()).add(blockInventory);
                }
            }
        }

        return inventories;
    }

    static Inventory getInventoryForBlock(Block block) {
        BlockState blockState = block.getState();
        if (!(blockState instanceof Container)) {
            return null;
        }

        Container container = (Container) blockState;
        return container.getInventory();
    }

    static Inventory getInventoryForLocation(Location location) {
        return getInventoryForBlock(location.getBlock());
    }

    void moveInventoryContentsToTargets(Inventory inventory, List<Location> targetSignLocations) {
        Container sourceContainer = getContainerFromInventory(inventory);
        if (sourceContainer == null) {
            return;
        }

        Location sourceLocation = sourceContainer.getLocation();

        Map<Material, List<Inventory>> targetInventories = getInventoriesForPerType(targetSignLocations);

        for (ItemStack stack : inventory.getContents()) {
            if (stack == null) {
                continue;
            }

            List<Inventory> targetInventoriesForType = targetInventories.get(stack.getType());
            if (targetInventoriesForType == null) {
                continue;
            }

            targetInventoriesForType = filterInventoriesByDistance(sourceLocation, targetInventoriesForType);

            plugin.addItemTransfer(stack, inventory, targetInventoriesForType);
        }
    }

    boolean moveItemToInventories(ItemStack itemStack, Inventory sourceInventory, List<Inventory> targetInventories) {
        ItemStack addStack = itemStack.clone();

        // Only move a single item at once
        addStack.setAmount(1);

        // Clone item stack as it might be modified by Inventory.addItem()
        ItemStack removeStack = addStack.clone();

        for (Inventory targetInventory : targetInventories) {
            if (targetInventory.getHolder() == null) {
                continue;
            }

            Map<Integer, ItemStack> remainingItems = targetInventory.addItem(addStack);

            // Item moved successfully?
            if (remainingItems.isEmpty() || remainingItems.get(0).getAmount() == 0) {
                sourceInventory.removeItem(removeStack);
                return true;
            }
        }

        return false;
    }

    List<Inventory> filterInventoriesByDistance(Location sourceLocation, List<Inventory> targetInventories) {
        List<Inventory> inventories = new ArrayList<>();

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
                Integer distance = LocationUtils.getDistance(sourceLocation, targetLocation);
                if (distance == null || distance > maxDistance) {
                    continue;
                }
            }

            inventories.add(targetInventory);
        }

        return inventories;
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

    SignData getSignDataFromConnectedInventories(Inventory inventory) {
        List<Inventory> connectedInventories = getConnectedInventories(inventory);
        if (connectedInventories == null) {
            return null;
        }

        for (Inventory connectedInventory : connectedInventories) {
            SignData signData = SignHelper.getSignDataForInventory(connectedInventory);
            if (signData == null) {
                continue;
            }

            return signData;
        }

        return null;
    }

    SignData getSignDataFromAllInventories(Inventory inventory) {
        SignData signData;

        // Get sign data from directly attached sign
        signData = SignHelper.getSignDataForInventory(inventory);
        if (signData != null) {
            return signData;
        }

        // Get sign data from connected inventories
        if (allowMultiChests) {
            signData = getSignDataFromConnectedInventories(inventory);
            if (signData != null && signData.multiChests) {
                return signData;
            }
        }

        return null;
    }

    void updateInventoryForSource(SignData signData, Inventory inventory) {
        List<Location> targetLocations;

        try {
            targetLocations = plugin.getDatabase().getLocations(signData.player, signData.name, SignHelper.TYPE_TARGET);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
            return;
        }

        moveInventoryContentsToTargets(inventory, targetLocations);
    }

    void updateInventoryForTarget(SignData signData) {
        List<Location> sourceLocations;
        List<Location> targetLocations;

        try {
            sourceLocations = SignHelper.getBlockLocationsFromSignLocations(plugin.getDatabase().getLocations(signData.player, signData.name, SignHelper.TYPE_SOURCE));
            targetLocations = plugin.getDatabase().getLocations(signData.player, signData.name, SignHelper.TYPE_TARGET);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to get locations from database: " + exception.getMessage());
            return;
        }

        List<Inventory> inventories = getInventories(sourceLocations);

        for (Inventory inventory : inventories) {
            moveInventoryContentsToTargets(inventory, targetLocations);
        }
    }

    void updateInventory(Inventory inventory) {
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
