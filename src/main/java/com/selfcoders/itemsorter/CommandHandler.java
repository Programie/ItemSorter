package com.selfcoders.itemsorter;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CommandHandler {
    private final ItemSorter plugin;
    private final CommandSender sender;
    private final String action;
    private final String[] args;

    public CommandHandler(ItemSorter plugin, CommandSender sender, String[] args) {
        this.plugin = plugin;
        this.sender = sender;

        if (args.length == 0) {
            this.action = "help";
            this.args = new String[0];
        } else {
            this.action = args[0];
            this.args = Arrays.copyOfRange(args, 1, args.length);
        }
    }

    public void execute() {
        switch (action) {
            case "help":
                printHelp();
                break;
            case "persistent":
                persistent();
                break;
            case "remove-signs":
                if (!sender.hasPermission("itemsorter.command")) {
                    printError("You do not have the required permissions to use this action!");
                    break;
                }

                if (args.length == 2) {
                    removeSigns(args[0], args[1]);
                } else {
                    printError("Invalid number of arguments for 'remove-signs', expected 'player name' and 'sign name'!");
                }
                break;
            default:
                printError("Invalid action: " + action);
                sender.sendMessage("Use " + ChatColor.GOLD + "/itemsorter" + ChatColor.WHITE + " to see a list of available actions");
                break;
        }
    }

    private void persistent() {
        if (!(sender instanceof Player)) {
            printError("This action can only be used by players!");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("itemsorter.persistent")) {
            printError("You do not have the required permissions to manage persistent items!");
            return;
        }

        String subAction = args.length > 0 ? args[0].toLowerCase() : "";
        if (!subAction.equals("list") && !subAction.equals("add") && !subAction.equals("remove") && !subAction.equals("clear")) {
            printError("Usage: /itemsorter persistent <list|add <material>|remove <material>|clear>");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(6);
        Sign sign = targetBlock == null ? null : SignHelper.getSignFromBlock(targetBlock);
        SignData signData = sign == null ? null : new SignData(sign);

        if (signData == null || !signData.isTarget()) {
            printError("You have to look at an [ItemTarget] sign!");
            return;
        }

        if (!signData.persistent) {
            printError("This sign has no persistent items enabled (add 'p' to the fourth line)!");
            return;
        }

        if (!player.hasPermission("itemsorter.destroyAny") && !player.getName().equalsIgnoreCase(signData.player)) {
            printError("You can only manage persistent items of your own signs!");
            return;
        }

        Location location = sign.getLocation();

        try {
            Database database = plugin.getDatabase();
            Set<Material> materials = database.getPersistentMaterials(location);

            switch (subAction) {
                case "list":
                    List<String> names = new ArrayList<>();
                    for (Material material : materials) {
                        names.add(material.name().toLowerCase());
                    }
                    Collections.sort(names);

                    if (names.isEmpty()) {
                        sender.sendMessage("No persistent items stored");
                    } else {
                        sender.sendMessage("Persistent items (" + names.size() + "): " + String.join(", ", names));
                    }
                    return;
                case "clear":
                    database.removePersistentMaterials(location);
                    printSuccess("Removed " + materials.size() + " persistent items");
                    return;
            }

            if (args.length != 2) {
                printError("Missing material, usage: /itemsorter persistent " + subAction + " <material>");
                return;
            }

            Material material = Material.matchMaterial(args[1]);
            if (material == null || !material.isItem()) {
                printError("Unknown item: " + args[1]);
                return;
            }

            if (subAction.equals("add")) {
                materials.add(material);
                database.setPersistentMaterials(location, materials);
                printSuccess("Added " + material.name().toLowerCase() + " to the persistent items");
            } else if (materials.remove(material)) {
                database.setPersistentMaterials(location, materials);
                printSuccess("Removed " + material.name().toLowerCase() + " from the persistent items");
            } else {
                printError(material.name().toLowerCase() + " is not a persistent item of this sign");
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to update persistent items in database: " + exception.getMessage());
            printError("An error occurred while updating the persistent items!");
        }
    }

    private void removeSigns(String player, String name) {
        try {
            Database database = plugin.getDatabase();
            List<Location> locations = database.getLocations(player, name);

            for (Location location : locations) {
                location.getBlock().breakNaturally();
                plugin.removeSignLocation(location);
            }

            plugin.getDatabase().removeSigns(player, name);

            printSuccess(locations.size() + " signs removed from database");
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to remove signs from database: " + exception.getMessage());
            printError("An error occurred while removing the signs!");
        }
    }

    private void printHelp() {
        sender.sendMessage(ChatColor.YELLOW + "-----" + ChatColor.WHITE + " Actions " + ChatColor.YELLOW + "-----");

        this.printUsage("persistent <list|add <material>|remove <material>|clear>", "Manage the persistent items of the [ItemTarget] sign you are looking at");

        if (sender.hasPermission("itemsorter.command")) {
            this.printUsage("remove-signs <player name> <sign name>", "Remove all signs with the specified name of the specified player");
        }
    }

    private void printUsage(String args, String description) {
        sender.sendMessage(ChatColor.GOLD + "/itemsorter " + args + ChatColor.WHITE + ": " + description);
    }

    private void printSuccess(String string) {
        sender.sendMessage(ChatColor.GREEN + string);
    }

    private void printError(String string) {
        sender.sendMessage(ChatColor.RED + string);
    }
}
