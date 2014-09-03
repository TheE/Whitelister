/**
 * Copyright (C) 2013 - 2014, Whitelister team and contributors
 *
 * This file is part of Whitelister.
 *
 * Whitelister is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Whitelister is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Whitelister. If not, see <http://www.gnu.org/licenses/>.
 */
package de.minehattan.whitelister.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.google.common.collect.ImmutableMap;
import com.sk89q.commandbook.CommandBook;

public class MySQLWhitelistManager implements WhitelistManager {

    private final String user;
    private final String password;
    private final String tableName;
    private final String dsn;
    
    private Connection conn;

    public MySQLWhitelistManager(String dsn, String tableName, String user, String password) {
        this.dsn = dsn;
        this.tableName = tableName;
        this.user = user;
        this.password = password;
    }

    private Connection getConnection() throws SQLException {
        if (conn != null && !conn.isValid(5)) {
            conn.close();
        }
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(dsn, user, password);
        }
        return conn;
    }

    @Override
    public void addToWhitelist(UUID id, String name) {
        Connection conn = null;
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();
            stmnt = conn.prepareStatement("INSERT INTO `" + tableName
                    + "` (`minecraft-id`, `minecraft-name`) VALUES (?, ?);");
            stmnt.setBytes(1, UUIDBinaryConverter.toBytes(id));
            stmnt.setString(2, name);
            stmnt.execute();
        } catch (SQLException e) {
            CommandBook.logger().log(Level.SEVERE, "Failed to add ' " + id + "' to the whitelist.", e);
        } finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public Map<UUID, String> getImmutableWhitelist() {
        Connection conn = null;
        PreparedStatement stmnt = null;

        ImmutableMap.Builder<UUID, String> builder = new ImmutableMap.Builder<UUID, String>();

        try {
            conn = getConnection();
            stmnt = conn
                    .prepareStatement("SELECT `minecraft-id`, `minecraft-name` FROM `" + tableName + "`;");
            ResultSet results = stmnt.executeQuery();
            while (results.next()) {
                builder.put(UUIDBinaryConverter.fromBytes(results.getBytes(1)), results.getString(2));
            }
        } catch (SQLException e) {
            CommandBook.logger().log(Level.SEVERE, "Failed to get values from the whitelist.", e);
        } finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
        return builder.build();
    }

    @Override
    public boolean isOnWhitelist(UUID id) {
        Connection conn = null;
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();
            stmnt = conn.prepareStatement("SELECT EXISTS (SELECT 1 FROM `" + tableName
                    + "` WHERE `minecraft-id` = ?);");
            stmnt.setBytes(1, UUIDBinaryConverter.toBytes(id));
            return stmnt.execute();
        } catch (SQLException e) {
            CommandBook.logger()
                    .log(Level.SEVERE, "Failed to check if ' " + id + "' is on the whitelist.", e);
        } finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
        return false;
    }

    @Override
    public void removeFromWhitelist(UUID id) {
        Connection conn = null;
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();
            stmnt = conn.prepareStatement("DELETE WHERE `minecraft-id` = ?;");
            stmnt.setBytes(1, UUIDBinaryConverter.toBytes(id));
            stmnt.execute();
        } catch (SQLException e) {
            CommandBook.logger().log(Level.SEVERE, "Failed to remove ' " + id + "' from the whitelist.", e);
        } finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public void updateName(UUID id, String name) {
        Connection conn = null;
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();
            stmnt = conn.prepareStatement("UPDATE SET `minecraft-name`= ? WHERE `minecraft-id` = ?;");
            stmnt.setString(1, name);
            stmnt.setBytes(2, UUIDBinaryConverter.toBytes(id));
            stmnt.execute();
        } catch (SQLException e) {
            CommandBook.logger().log(Level.SEVERE, "Failed to update name for ' " + id + "'.", e);
        } finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

}
