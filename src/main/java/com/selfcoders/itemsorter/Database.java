package com.selfcoders.itemsorter;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class Database {
    private final JavaPlugin plugin;
    private final Connection connection;
    private final int version;

    Database(JavaPlugin plugin) throws Exception {
        Class.forName("org.sqlite.JDBC");

        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "database.db").getAbsolutePath());
        version = getVersion();
        this.plugin = plugin;

        initTable();
    }

    private void initTable() throws Exception {
        int migration = 0;

        executeMigration(++migration, "Create links table", "CREATE TABLE IF NOT EXISTS `links`\n" +
                "(\n" +
                "    `id`     INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                "    `uuid`   TEXT,\n" +
                "    `player` TEXT COLLATE NOCASE,\n" +
                "    `name`   TEXT COLLATE NOCASE,\n" +
                "    `type`   TEXT COLLATE NOCASE,\n" +
                "    `order`  INTEGER,\n" +
                "    `world`  TEXT,\n" +
                "    `x`      INTEGER,\n" +
                "    `y`      INTEGER,\n" +
                "    `z`      INTEGER\n" +
                ")");

        executeMigration(++migration, "Create location index for links table", "CREATE UNIQUE INDEX `location` ON `links` (`world`, `x`, `y`, `z`)");
        executeMigration(++migration, "Create uuid index for links table", "CREATE INDEX `uuid` ON `links` (`uuid`)");
        executeMigration(++migration, "Create nameType index for links table", "CREATE INDEX `nameType` ON `links` (`name`, `type`)");
        executeMigration(++migration, "Create order index for links table", "CREATE INDEX `order` ON `links` (`order`)");
    }

    private int getVersion() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("PRAGMA `user_version`");

        return resultSet.next() ? resultSet.getInt(1) : 0;
    }

    private void executeMigration(int version, String title, String statementString) throws SQLException {
        if (this.version >= version) {
            return;
        }

        plugin.getLogger().info("Updating database to version " + version + ": " + title);

        Statement statement = connection.createStatement();
        statement.execute(statementString);
        statement.execute("PRAGMA `user_version` = " + version);
    }

    void close() throws SQLException {
        connection.close();
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

    List<String> getNames(Player player) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `name` FROM `links` WHERE `uuid` = ? GROUP BY `name`");

        statement.setString(1, player.getUniqueId().toString());

        List<String> names = new ArrayList<>();

        ResultSet result = statement.executeQuery();

        while (result.next()) {
            names.add(result.getString("name"));
        }

        return names;
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

    List<Location> getLocations(Player player, String name) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `world`, `x`, `y`, `z` FROM `links` WHERE `uuid` = ? AND `name` = ? ORDER BY `order`");

        statement.setString(1, player.getUniqueId().toString());
        statement.setString(2, name);

        return getLocations(statement);
    }

    List<Location> getLocations() throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT `world`, `x`, `y`, `z` FROM `links`");

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