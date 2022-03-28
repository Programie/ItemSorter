package com.selfcoders.itemsorter;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TransferItem {
    ItemStack stack;
    List<Inventory> targetInventories;

    TransferItem(ItemStack stack, List<Inventory> targetInventories) {
        this.stack = stack;
        this.targetInventories = targetInventories;
    }
}
