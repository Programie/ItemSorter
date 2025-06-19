package com.selfcoders.itemsorter;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

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
            case "remove-signs":
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

    private void removeSigns(String player, String name) {
        try {
            Database database = plugin.getDatabase();
            List<Location> locations = database.getLocations(player, name);

            for (Location location : locations) {
                location.getBlock().breakNaturally();
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

        this.printUsage("remove-signs <player name> <sign name>", "Remove all signs with the specified name of the specified player");
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
