package com.selfcoders.itemsorter;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ItemTransferTask implements Runnable {
    private final InventoryHelper inventoryHelper;
    private final Map<Inventory, Map<Material, TransferItem>> transfers;

    ItemTransferTask(InventoryHelper inventoryHelper) {
        this.inventoryHelper = inventoryHelper;

        transfers = new HashMap<>();
    }

    @Override
    public void run() {
        Iterator<Map.Entry<Inventory, Map<Material, TransferItem>>> transfersIterator = transfers.entrySet().iterator();

        while (transfersIterator.hasNext()) {
            Map.Entry<Inventory, Map<Material, TransferItem>> mapEntry = transfersIterator.next();

            transferItems(mapEntry.getKey(), mapEntry.getValue());

            if (mapEntry.getValue().isEmpty()) {
                transfersIterator.remove();
            }
        }
    }

    void transferItems(Inventory sourceInventory, Map<Material, TransferItem> materialTransferItemMap) {
        Iterator<Map.Entry<Material, TransferItem>> iterator = materialTransferItemMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Material, TransferItem> mapEntry = iterator.next();

            Material material = mapEntry.getKey();
            TransferItem transferItem = mapEntry.getValue();

            if (!sourceInventory.contains(material)) {
                iterator.remove();
                continue;
            }

            if (inventoryHelper.moveItemToInventories(transferItem.stack, sourceInventory, transferItem.targetInventories)) {
                break;
            }
        }
    }

    void addTransfer(ItemStack itemStack, Inventory sourceInventory, List<Inventory> targetInventories) {
        TransferItem transferItem = new TransferItem(itemStack, targetInventories);

        transfers.computeIfAbsent(sourceInventory, key -> new HashMap<>()).put(itemStack.getType(), transferItem);
    }
}
