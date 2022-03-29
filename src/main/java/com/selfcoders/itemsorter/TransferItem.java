package com.selfcoders.itemsorter;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TransferItem {
    ItemStack stack;
    Inventory source;
    List<Inventory> targets;

    TransferItem(ItemStack stack, Inventory source, List<Inventory> targets) {
        this.stack = stack;
        this.source = source;
        this.targets = targets;
    }
}
