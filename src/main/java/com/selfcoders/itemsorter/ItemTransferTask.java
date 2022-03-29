package com.selfcoders.itemsorter;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ItemTransferTask implements Runnable {
    private final InventoryHelper inventoryHelper;
    private final Map<Location, Map<Material, TransferItem>> transfers;

    ItemTransferTask(InventoryHelper inventoryHelper) {
        this.inventoryHelper = inventoryHelper;

        transfers = new HashMap<>();
    }

    @Override
    public void run() {
        Iterator<Map.Entry<Location, Map<Material, TransferItem>>> transfersIterator = transfers.entrySet().iterator();

        while (transfersIterator.hasNext()) {
            Map.Entry<Location, Map<Material, TransferItem>> mapEntry = transfersIterator.next();

            transferItems(mapEntry.getValue());

            if (mapEntry.getValue().isEmpty()) {
                transfersIterator.remove();
            }
        }
    }

    void transferItems(Map<Material, TransferItem> materialTransferItemMap) {
        Iterator<Map.Entry<Material, TransferItem>> iterator = materialTransferItemMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Material, TransferItem> mapEntry = iterator.next();

            Material material = mapEntry.getKey();
            TransferItem transferItem = mapEntry.getValue();

            if (!transferItem.source.contains(material)) {
                iterator.remove();
                continue;
            }

            if (inventoryHelper.moveItemToInventories(transferItem.stack, transferItem.source, transferItem.targets)) {
                break;
            }
        }
    }

    void addTransfer(ItemStack itemStack, Inventory sourceInventory, List<Inventory> targetInventories) {
        TransferItem transferItem = new TransferItem(itemStack, sourceInventory, targetInventories);

        transfers.computeIfAbsent(sourceInventory.getLocation(), key -> new HashMap<>()).put(itemStack.getType(), transferItem);
    }
}
