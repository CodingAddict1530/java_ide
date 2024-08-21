/*
 * Copyright 2024 Alexis Mugisha
 * https://github.com/CodingAddict1530
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.project.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * Handles Database related operations.
 */
public class DatabaseUtility {

    /**
     * The logger for the class.
     */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtility.class);

    /**
     * The URL to connect to the database.
     */
    private static final String URL = "jdbc:sqlite:fusion.db";

    private static final ArrayList<Connection> connections = new ArrayList<>();

    /**
     * Creates a connection to the database.
     *
     * @return The Connection.
     */
    public static Connection connect() {

        try {
            logger.info("Connected to database");
            Connection c = DriverManager.getConnection(URL);
            connections.add(c);
            return c;
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return null;
        }

    }

    /**
     * Executes a SELECT query.
     *
     * @param conn The database connection.
     * @param query The query. (In Prepared statement form).
     * @param params The parameters for the prepared statement.
     * @return A ResultSet.
     */
    public static ResultSet executeQuery(Connection conn, String query, Object... params) {

        // Make sure the table exists.
        createCMDTable(conn);
        try {
            PreparedStatement ps = conn.prepareStatement(query);

            // Set all parameters.
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeQuery();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return null;
        }

    }

    /**
     * Executes INSERT, UPDATE, DELETE AND DROP statements.
     *
     * @param conn The database connection.
     * @param query The query. (In Prepared statement form).
     * @param params The parameters for the prepared statement.
     */
    public static void executeUpdate(Connection conn, String query, Object... params) {

        // Make sure the table exists.
        createCMDTable(conn);
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

    }

    /**
     * Creates the ClassMetaData Table.
     */
    private static void createCMDTable(Connection conn) {

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS ClassMetaData (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "packageName TEXT NOT NULL," +
                    "className TEXT NOT NULL," +
                    "qualifiedName TEXT NOT NULL," +
                    "path TEXT NOT NULL" +
                    ")");
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

    }

    /**
     * Closes a given connection.
     *
     * @param conn The Connection to close.
     */
    public static void close(Connection conn) {

        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                logger.info("Closing connection");
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

    }

    /**
     * Closes all connections to the database.
     */
    public static void closeAll() {

        for (Connection conn : connections) {
            close(conn);
        }

    }

}
