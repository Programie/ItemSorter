package com.selfcoders.itemsorter;

import org.bukkit.Location;

class Util {
    static Integer getDistance(Location location1, Location location2) {
        if (location1.getWorld() != location2.getWorld()) {
            return null;
        }

        int distanceX = Math.abs((int) location1.getX() - (int) location2.getX());
        int distanceY = Math.abs((int) location1.getY() - (int) location2.getY());
        int distanceZ = Math.abs((int) location1.getZ() - (int) location2.getZ());

        return Math.max(distanceX, Math.max(distanceY, distanceZ));
    }
}