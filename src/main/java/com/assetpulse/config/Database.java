package com.assetpulse.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized JDBC connection factory for AssetPulse.
 *
 * Configure ASSETPULSE_DB_USER and ASSETPULSE_DB_PASSWORD in the environment.
 */
public final class Database {
    private static final String URL =
            System.getenv().getOrDefault(
                    "ASSETPULSE_DB_URL",
                    "jdbc:mysql://localhost:3306/assetpulse_db?serverTimezone=UTC"
            );
    private static final String USER = System.getenv("ASSETPULSE_DB_USER");
    private static final String PASSWORD = System.getenv("ASSETPULSE_DB_PASSWORD");

    private Database() {
        // Utility class.
    }

    /**
     * Opens a new MySQL connection.
     *
     * @return an open JDBC connection
     * @throws SQLException when credentials are missing or MySQL cannot be reached
     */
    public static Connection getConnection() throws SQLException {
        if (USER == null || USER.isBlank()) {
            throw new SQLException(
                    "Missing ASSETPULSE_DB_USER environment variable. " +
                    "Set it in the same PowerShell terminal before launching the app."
            );
        }
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException(
                    "Missing ASSETPULSE_DB_PASSWORD environment variable. " +
                    "Set it in the same PowerShell terminal before launching the app."
            );
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
