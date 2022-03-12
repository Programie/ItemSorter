package com.selfcoders.itemsorter;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class Database {
    private final JavaPlugin plugin;
    private final Connection connection;

    Database(JavaPlugin plugin) throws Exception {
        Class.forName("org.sqlite.JDBC");

        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "database.db").getAbsolutePath());
        this.plugin = plugin;

        initTable();
    }

    private void initTable() throws Exception {
        InputStream inputStream = plugin.getResource("database.sql");
        if (inputStream == null) {
            return;
        }

        String statementString = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));

        Statement statement = connection.createStatement();
        statement.execute(statementString);
    }

    void addLocation(Player player, String name, String type, Location location, int order) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO `links` (`uuid`, `player`, `name`, `type`, `order`, `world`, `x`, `y`, `z`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

        statement.setString(1, player.getUniqueId().toString());
        statement.setString(2, player.getName());
        statement.setString(3, name);
        statement.setString(4, type);
        statement.setInt(5, order);
        statement.setString(6, location.getWorld().getName());
        statement.setInt(7, (int) location.getX());
        statement.setInt(8, (int) location.getY());
        statement.setInt(9, (int) location.getZ());

        statement.executeUpdate();
    }

    void removeLocation(String player, String name, String type, Location location) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("DELETE FROM `links` WHERE `player` = ? AND `name` = ? AND `type` = ? AND `world` = ? AND `x` = ? AND `y` = ? AND `z` = ?");

        statement.setString(1, player);
        statement.setString(2, name);
        statement.setString(3, type);
        statement.setString(4, location.getWorld().getName());
        statement.setInt(5, (int) location.getX());
        statement.setInt(6, (int) location.getY());
        statement.setInt(7, (int) location.getZ());

        statement.executeUpdate();
    }

    List<Location> getLocations(Player player, String name, String type) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `world`, `x`, `y`, `z` FROM `links` WHERE `uuid` = ? AND `name` = ? AND `type` = ? ORDER BY `order`");

        statement.setString(1, player.getUniqueId().toString());
        statement.setString(2, name);
        statement.setString(3, type);

        return getLocations(statement);
    }

    List<Location> getLocations(String player, String name, String type) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `world`, `x`, `y`, `z` FROM `links` WHERE `player` = ? AND `name` = ? AND `type` = ? ORDER BY `order`");

        statement.setString(1, player);
        statement.setString(2, name);
        statement.setString(3, type);

        return getLocations(statement);
    }

    List<Location> getLocations(PreparedStatement statement) throws SQLException {
        List<Location> locations = new ArrayList<>();

        ResultSet result = statement.executeQuery();

        while (result.next()) {
            String world = result.getString("world");
            int x = result.getInt("x");
            int y = result.getInt("y");
            int z = result.getInt("z");

            locations.add(new Location(plugin.getServer().getWorld(world), x, y, z));
        }

        return locations;
    }
}