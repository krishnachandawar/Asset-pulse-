package com.assetpulse.repository;

import com.assetpulse.config.Database;
import com.assetpulse.model.Asset;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes parameterized SQL statements for the assets table.
 */
public final class AssetRepository {
    private static final String SELECT_COLUMNS =
            "SELECT asset_id, asset_tag, asset_name, category, assigned_to, " +
            "status, purchase_cost, purchase_date FROM assets ";

    /**
     * Returns all assets, newest IDs first.
     */
    public List<Asset> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + "ORDER BY asset_id DESC";
        List<Asset> assets = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                assets.add(mapRow(resultSet));
            }
        }
        return assets;
    }

    /**
     * Searches asset tag, name, category, or assigned employee.
     */
    public List<Asset> search(String query) throws SQLException {
        String sql = SELECT_COLUMNS +
                "WHERE asset_tag LIKE ? OR asset_name LIKE ? " +
                "OR category LIKE ? OR COALESCE(assigned_to, '') LIKE ? " +
                "ORDER BY asset_id DESC";
        List<Asset> assets = new ArrayList<>();
        String pattern = "%" + query.trim() + "%";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                statement.setString(i, pattern);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assets.add(mapRow(resultSet));
                }
            }
        }
        return assets;
    }

    /**
     * Inserts an asset and returns its generated primary key.
     */
    public int insert(
            String tag, String name, String category, String assignedTo,
            String status, BigDecimal cost, LocalDate purchaseDate
    ) throws SQLException {
        String sql = """
                INSERT INTO assets
                (asset_tag, asset_name, category, assigned_to, status,
                 purchase_cost, purchase_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            bindAsset(statement, tag, name, category, assignedTo, status, cost, purchaseDate);
            int changed = statement.executeUpdate();
            if (changed != 1) {
                throw new SQLException("MySQL did not insert the asset.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Asset inserted, but MySQL returned no generated ID.");
        }
    }

    /**
     * Updates an existing asset. Returns true only if one row changed.
     */
    public boolean update(
            int id, String tag, String name, String category, String assignedTo,
            String status, BigDecimal cost, LocalDate purchaseDate
    ) throws SQLException {
        String sql = """
                UPDATE assets
                SET asset_tag = ?, asset_name = ?, category = ?, assigned_to = ?,
                    status = ?, purchase_cost = ?, purchase_date = ?
                WHERE asset_id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAsset(statement, tag, name, category, assignedTo, status, cost, purchaseDate);
            statement.setInt(8, id);
            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Deletes the asset with the specified primary key.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM assets WHERE asset_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Returns the total number of stored assets.
     */
    public int countAll() throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT COUNT(*) FROM assets");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    private static void bindAsset(
            PreparedStatement statement,
            String tag, String name, String category, String assignedTo,
            String status, BigDecimal cost, LocalDate purchaseDate
    ) throws SQLException {
        statement.setString(1, tag.trim());
        statement.setString(2, name.trim());
        statement.setString(3, category.trim());

        if (assignedTo == null || assignedTo.isBlank()) {
            statement.setNull(4, Types.VARCHAR);
        } else {
            statement.setString(4, assignedTo.trim());
        }

        statement.setString(5, status);
        statement.setBigDecimal(6, cost);

        if (purchaseDate == null) {
            statement.setNull(7, Types.DATE);
        } else {
            statement.setDate(7, Date.valueOf(purchaseDate));
        }
    }

    private static Asset mapRow(ResultSet resultSet) throws SQLException {
        Date sqlDate = resultSet.getDate("purchase_date");
        return new Asset(
                resultSet.getInt("asset_id"),
                resultSet.getString("asset_tag"),
                resultSet.getString("asset_name"),
                resultSet.getString("category"),
                resultSet.getString("assigned_to"),
                resultSet.getString("status"),
                resultSet.getBigDecimal("purchase_cost"),
                sqlDate == null ? null : sqlDate.toLocalDate()
        );
    }
}
