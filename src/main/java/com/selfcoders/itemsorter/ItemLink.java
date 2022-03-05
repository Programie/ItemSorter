package com.selfcoders.itemsorter;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemLink {
    static final String TYPE_SOURCE = "source";
    static final String TYPE_TARGET = "target";

    private final FileConfiguration fileConfiguration;
    private final JavaPlugin plugin;
    private final String name;

    ItemLink(FileConfiguration fileConfiguration, ItemSorter plugin, String name) {
        this.fileConfiguration = fileConfiguration;
        this.plugin = plugin;
        this.name = name;
    }

    private String getConfigPath(String type) {
        return "links." + name + "." + type;
    }

    private List<Map<?, ?>> getList(String type) {
        return fileConfiguration.getMapList(getConfigPath(type));
    }

    private void setList(String type, List<Map<?, ?>> list) {
        if (list.isEmpty()) {
            fileConfiguration.set(getConfigPath(type), null);

            // Remove whole link from config if source and target does not exist
            if (fileConfiguration.get(getConfigPath(TYPE_SOURCE)) == null && fileConfiguration.get(getConfigPath(TYPE_TARGET)) == null) {
                fileConfiguration.set("links." + name, null);
            }
        } else {
            fileConfiguration.set(getConfigPath(type), list);
        }
    }

    private void addLocation(String type, Location location) {
        List<Map<?, ?>> list = getList(type);

        Map<String, Object> entry = new HashMap<>();
        entry.put("world", location.getWorld().getName());
        entry.put("x", (int) location.getX());
        entry.put("y", (int) location.getY());
        entry.put("z", (int) location.getZ());
        list.add(entry);

        setList(type, list);
    }

    private void removeLocation(String type, Location location) {
        List<Map<?, ?>> list = getList(type);
        String locationWorld = location.getWorld().getName();
        int locationX = (int) location.getX();
        int locationY = (int) location.getY();
        int locationZ = (int) location.getZ();

        for (Map<?, ?> entry : list) {
            String world = (String) entry.get("world");
            int x = (int) entry.get("x");
            int y = (int) entry.get("y");
            int z = (int) entry.get("z");

            if (world.equalsIgnoreCase(locationWorld) && x == locationX && y == locationY && z == locationZ) {
                list.remove(entry);
                break;
            }
        }

        setList(type, list);
    }

    private List<Location> getLocations(String type) {
        List<Map<?, ?>> list = getList(type);
        List<Location> locations = new ArrayList<>();

        for (Map<?, ?> entry : list) {
            String world = (String) entry.get("world");
            int x = (int) entry.get("x");
            int y = (int) entry.get("y");
            int z = (int) entry.get("z");

            locations.add(new Location(plugin.getServer().getWorld(world), x, y, z));
        }

        return locations;
    }

    public List<Location> getSources() {
        return getLocations(TYPE_SOURCE);
    }

    public List<Location> getTargets() {
        return getLocations(TYPE_TARGET);
    }

    public void addSource(Location location) {
        addLocation(TYPE_SOURCE, location);
    }

    public void removeSource(Location location) {
        removeLocation(TYPE_SOURCE, location);
    }

    public void addTarget(Location location) {
        addLocation(TYPE_TARGET, location);
    }

    public void removeTarget(Location location) {
        removeLocation(TYPE_TARGET, location);
    }
}