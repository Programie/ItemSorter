package com.selfcoders.itemsorter;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;

public class SignData {
    String tagLine;
    String name;
    String player;
    Integer order = 0;

    SignData(String[] lines) {
        if (lines.length >= 1) {
            tagLine = ChatColor.stripColor(lines[0]);
        }

        if (lines.length >= 2) {
            name = ChatColor.stripColor(lines[1]);
        }

        if (lines.length >= 3) {
            player = ChatColor.stripColor(lines[2]);
        }

        if (lines.length >= 4) {
            String[] options = ChatColor.stripColor(lines[3]).split(",");

            for (String option : options) {
                String[] optionPairs = option.split(":", 2);
                String value = "";
                if (optionPairs.length == 2) {
                    value = optionPairs[1];
                }

                switch (optionPairs[0]) {
                    case "o":// Order
                        try {
                            order = Integer.valueOf(value);
                        } catch (Exception exception) {
                            // ignore
                        }
                        break;
                }
            }
        }
    }

    SignData(Sign sign) {
        this(sign.getLines());
    }

    boolean isItemSorterSign() {
        if (tagLine == null) {
            return false;
        }

        return tagLine.equalsIgnoreCase(ItemLink.TYPE_SOURCE) || tagLine.equalsIgnoreCase(ItemLink.TYPE_TARGET);
    }

    boolean checkName() {
        if (name == null) {
            return false;
        }

        return !name.isEmpty();
    }

    boolean isSource() {
        if (tagLine == null) {
            return false;
        }

        return tagLine.equalsIgnoreCase(ItemLink.TYPE_SOURCE);
    }

    boolean isTarget() {
        if (tagLine == null) {
            return false;
        }

        return tagLine.equalsIgnoreCase(ItemLink.TYPE_TARGET);
    }
}