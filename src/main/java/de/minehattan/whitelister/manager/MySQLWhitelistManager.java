/*
 * Copyright (C) 2013 - 2015, Whitelister team and contributors
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

import com.google.common.collect.ImmutableMap;
import com.sk89q.commandbook.CommandBook;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * Manages a whitelist stored in MySQL.
 */
public class MySQLWhitelistManager implements WhitelistManager {

  private final String user;
  private final String password;
  private final String tableName;
  private final String dsn;

  private Connection conn;

  /**
   * Initializes this manager.
   *
   * @param dsn       the dsn of the database
   * @param tableName the name of the table that stores the whitelist
   * @param user      the MySQL user
   * @param password  the user's password
   */
  public MySQLWhitelistManager(String dsn, String tableName, String user, String password) {
    this.dsn = dsn;
    this.tableName = tableName;
    this.user = user;
    this.password = password;
  }

  @Override
  public void add(UUID id, String name) {
    Connection conn = null;
    PreparedStatement stmnt = null;

    try {
      conn = getConnection();
      stmnt =
          conn.prepareStatement("INSERT INTO `" + tableName + "` (`minecraft-uuid`, `minecraft-name`) VALUES (?, ?);");
      stmnt.setBytes(1, UUIDBinaryConverter.toBytes(id));
      stmnt.setString(2, name);
      stmnt.execute();
    } catch (SQLException e) {
      CommandBook.logger().log(Level.SEVERE, "Failed to add '" + id + "' to the whitelist.", e);
    } finally {
      closeQuitly(conn, stmnt);
    }
  }

  @Override
  public Map<UUID, String> getWhitelist() {
    Connection conn = null;
    PreparedStatement stmnt = null;

    ImmutableMap.Builder<UUID, String> builder = new ImmutableMap.Builder<UUID, String>();

    try {
      conn = getConnection();
      stmnt = conn.prepareStatement("SELECT `minecraft-uuid`, `minecraft-name` FROM `" + tableName + "`;");
      ResultSet results = stmnt.executeQuery();
      while (results.next()) {
        builder.put(UUIDBinaryConverter.fromBytes(results.getBytes(1)), results.getString(2));
      }
    } catch (SQLException e) {
      CommandBook.logger().log(Level.SEVERE, "Failed to get values from the whitelist.", e);
    } finally {
      closeQuitly(conn, stmnt);
    }
    return builder.build();
  }

  @Override
  public CheckResult contains(UUID id) {
    Connection conn = null;
    PreparedStatement stmnt = null;
    ResultSet rslt = null;

    try {
      conn = getConnection();
      stmnt =
          conn.prepareStatement("SELECT `minecraft-name` FROM `" + tableName + "` WHERE `minecraft-uuid` = ? LIMIT 1;");
      stmnt.setBytes(1, UUIDBinaryConverter.toBytes(id));
      rslt = stmnt.executeQuery();
      if (rslt.next()) {
        String name = rslt.getString(1);
        if (name != null) {
          return new CheckResult(true, name);
        }
      }
      return new CheckResult(false, null);
    } catch (SQLException e) {
      CommandBook.logger().log(Level.SEVERE, "Failed to check if ' " + id + "' is on the whitelist.", e);
    } finally {
      if (rslt != null) {
        try {
          rslt.close();
        } catch (SQLException e) {
          // ignore since we cannot do anything
        }
      }
      closeQuitly(conn, stmnt);
    }
    return new CheckResult(false, null);
  }

  @Override
  public void remove(UUID id) {
    Connection conn = null;
    PreparedStatement stmnt = null;

    try {
      conn = getConnection();
      stmnt = conn.prepareStatement("DELETE WHERE `minecraft-uuid` = ?;");
      stmnt.setBytes(1, UUIDBinaryConverter.toBytes(id));
      stmnt.execute();
    } catch (SQLException e) {
      CommandBook.logger().log(Level.SEVERE, "Failed to remove ' " + id + "' from the whitelist.", e);
    } finally {
      closeQuitly(conn, stmnt);
    }
  }

  @Override
  public void updateName(UUID id, String name) {
    Connection conn = null;
    PreparedStatement stmnt = null;

    try {
      conn = getConnection();
      stmnt = conn.prepareStatement("UPDATE SET `minecraft-name`= ? WHERE `minecraft-uuid` = ?;");
      stmnt.setString(1, name);
      stmnt.setBytes(2, UUIDBinaryConverter.toBytes(id));
      stmnt.execute();
    } catch (SQLException e) {
      CommandBook.logger().log(Level.SEVERE, "Failed to update name for ' " + id + "'.", e);
    } finally {
      closeQuitly(conn, stmnt);
    }
  }

  @Override
  public UUID getUniqueID(String name) {
    Connection conn = null;
    PreparedStatement stmnt = null;

    UUID ret = null;

    try {
      conn = getConnection();
      stmnt =
          conn.prepareStatement("SELECT `minecraft-uuid` FROM `" + tableName + "` WHERE `minecraft-name`= ? LIMIT 1;");
      stmnt.setString(1, name);
      ResultSet results = stmnt.executeQuery();
      while (results.next()) {
        ret = UUIDBinaryConverter.fromBytes(results.getBytes(1));
      }
    } catch (SQLException e) {
      CommandBook.logger().log(Level.SEVERE, "Failed to get UUID for '" + name + "'.", e);
    } finally {
      closeQuitly(conn, stmnt);
    }
    return ret;
  }

  /**
   * Gets a connection to the database.
   *
   * @return a connection to the database
   * @throws SQLException if a database access error occurs
   */
  private Connection getConnection() throws SQLException {
    if (conn != null && !conn.isValid(5)) {
      conn.close();
    }
    if (conn == null || conn.isClosed()) {
      conn = DriverManager.getConnection(dsn, user, password);
    }
    return conn;
  }

  /**
   * Closes the given resources quietly, ignoring any exceptions.
   *
   * @param conn  a Connection - can be {@code null}
   * @param stmnt a PreparedStatement - can be {@code null}
   */
  private void closeQuitly(@Nullable Connection conn, @Nullable PreparedStatement stmnt) {
    if (stmnt != null) {
      try {
        stmnt.close();
      } catch (SQLException e) {
        // ignore since we cannot do anything
      }
    }
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        // ignore since we cannot do anything
      }
    }
  }

}
