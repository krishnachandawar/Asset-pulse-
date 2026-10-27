package com.assetpulse;

import com.assetpulse.model.Asset;
import com.assetpulse.repository.AssetRepository;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * AssetPulse Phase 1: a database-backed IT asset inventory desktop app.
 */
public class MainApp extends Application {
    private final AssetRepository repository = new AssetRepository();
    private final ObservableList<Asset> rows = FXCollections.observableArrayList();

    private final TableView<Asset> table = new TableView<>();
    private final TextField tagField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField categoryField = new TextField();
    private final TextField assignedField = new TextField();
    private final TextField costField = new TextField();
    private final ComboBox<String> statusBox = new ComboBox<>();
    private final DatePicker purchaseDatePicker = new DatePicker();
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label("Ready");
    private final Label totalLabel = new Label("Total assets: —");
    private final Button addButton = new Button("Add asset");
    private final Button updateButton = new Button("Update selected");
    private final Button deleteButton = new Button("Delete selected");
    private int selectedId = -1;

    @Override
    public void start(Stage stage) {
        stage.setTitle("AssetPulse | IT Asset Inventory");

        Label heading = new Label("AssetPulse");
        heading.getStyleClass().add("heading");
        Label subtitle = new Label("IT asset inventory · MySQL-backed");
        subtitle.getStyleClass().add("subtitle");

        VBox titleBox = new VBox(4, heading, subtitle);
        titleBox.setPadding(new Insets(18, 20, 12, 20));

        configureTable();
        configureForm();

        searchField.setPromptText("Search tag, name, category, employee...");
        Button searchButton = new Button("Search");
        Button refreshButton = new Button("Refresh");
        searchButton.setOnAction(event -> runSearch());
        refreshButton.setOnAction(event -> refreshAssets());

        HBox searchBar = new HBox(8, searchField, searchButton, refreshButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS);
        searchBar.setPadding(new Insets(0, 20, 10, 20));

        VBox center = new VBox(10, searchBar, table);
        center.setPadding(new Insets(0, 20, 12, 20));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        HBox footer = new HBox(16, totalLabel, statusLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 20, 16, 20));
        statusLabel.getStyleClass().add("status-text");

        BorderPane root = new BorderPane();
        root.setTop(titleBox);
        root.setCenter(center);
        root.setRight(buildFormPanel());
        root.setBottom(footer);

        Scene scene = new Scene(root, 1180, 700);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();

        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> populateForm(selected)
        );

        refreshAssets();
    }

    private void configureTable() {
        table.setItems(rows);
        table.setPlaceholder(new Label("No assets found. Add your first asset."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addColumn("ID", "assetId", 55);
        addColumn("Asset tag", "assetTag", 100);
        addColumn("Asset name", "assetName", 170);
        addColumn("Category", "category", 95);
        addColumn("Assigned to", "assignedTo", 120);
        addColumn("Status", "status", 110);
        addColumn("Cost (₹)", "purchaseCost", 100);
        addColumn("Purchase date", "purchaseDate", 110);
    }

    private void addColumn(String title, String property, double width) {
        TableColumn<Asset, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        table.getColumns().add(column);
    }

    private void configureForm() {
        tagField.setPromptText("e.g. LT-004");
        nameField.setPromptText("e.g. ThinkPad E14");
        categoryField.setPromptText("e.g. Laptop");
        assignedField.setPromptText("Optional employee name");
        costField.setPromptText("e.g. 62000.00");
        statusBox.setItems(FXCollections.observableArrayList(
                "AVAILABLE", "ASSIGNED", "MAINTENANCE", "RETIRED"
        ));
        statusBox.setValue("AVAILABLE");
        purchaseDatePicker.setPromptText("Optional purchase date");
    }

    private VBox buildFormPanel() {
        Label formTitle = new Label("Asset details");
        formTitle.getStyleClass().add("section-heading");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Asset tag *"), 0, 0);
        grid.add(tagField, 0, 1);
        grid.add(new Label("Asset name *"), 0, 2);
        grid.add(nameField, 0, 3);
        grid.add(new Label("Category *"), 0, 4);
        grid.add(categoryField, 0, 5);
        grid.add(new Label("Assigned to"), 0, 6);
        grid.add(assignedField, 0, 7);
        grid.add(new Label("Status"), 0, 8);
        grid.add(statusBox, 0, 9);
        grid.add(new Label("Purchase cost (₹) *"), 0, 10);
        grid.add(costField, 0, 11);
        grid.add(new Label("Purchase date"), 0, 12);
        grid.add(purchaseDatePicker, 0, 13);

        for (javafx.scene.Node node : grid.getChildren()) {
            if (node instanceof TextField field) {
                field.setMaxWidth(Double.MAX_VALUE);
            }
            if (node instanceof ComboBox<?> combo) {
                combo.setMaxWidth(Double.MAX_VALUE);
            }
            if (node instanceof DatePicker picker) {
                picker.setMaxWidth(Double.MAX_VALUE);
            }
        }

        addButton.setMaxWidth(Double.MAX_VALUE);
        updateButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setMaxWidth(Double.MAX_VALUE);

        addButton.setOnAction(event -> addAsset());
        updateButton.setOnAction(event -> updateAsset());
        deleteButton.setOnAction(event -> deleteAsset());
        Button clearButton = new Button("Clear form");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setOnAction(event -> clearForm());

        VBox buttons = new VBox(8, addButton, updateButton, deleteButton, clearButton);
        VBox panel = new VBox(12, formTitle, grid, buttons);
        panel.setPadding(new Insets(8, 20, 16, 0));
        panel.setPrefWidth(250);
        return panel;
    }

    private void addAsset() {
        try {
            validateForm();
            int id = repository.insert(
                    tagField.getText().trim(),
                    nameField.getText().trim(),
                    categoryField.getText().trim(),
                    assignedField.getText(),
                    statusBox.getValue(),
                    parseCost(),
                    purchaseDatePicker.getValue()
            );
            refreshAssets();
            clearForm();
            showInfo("Asset created successfully. ID: " + id);
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showDatabaseError(exception);
        }
    }

    private void updateAsset() {
        if (selectedId < 0) {
            showError("Select an asset from the table before updating.");
            return;
        }
        try {
            validateForm();
            boolean changed = repository.update(
                    selectedId,
                    tagField.getText().trim(),
                    nameField.getText().trim(),
                    categoryField.getText().trim(),
                    assignedField.getText(),
                    statusBox.getValue(),
                    parseCost(),
                    purchaseDatePicker.getValue()
            );
            if (!changed) {
                showError("The selected asset no longer exists. Refresh and try again.");
                refreshAssets();
                return;
            }
            refreshAssets();
            clearForm();
            showInfo("Asset updated successfully.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showDatabaseError(exception);
        }
    }

    private void deleteAsset() {
        if (selectedId < 0) {
            showError("Select an asset from the table before deleting.");
            return;
        }
        Asset selected = table.getSelectionModel().getSelectedItem();
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm deletion");
        confirmation.setHeaderText("Delete selected asset?");
        confirmation.setContentText(
                selected == null ? "This action cannot be undone."
                        : "Delete " + selected.getAssetTag() + " — " +
                          selected.getAssetName() + "?"
        );

        if (confirmation.showAndWait().orElse(
                javafx.scene.control.ButtonType.CANCEL
        ) != javafx.scene.control.ButtonType.OK) {
            return;
        }

        try {
            boolean deleted = repository.delete(selectedId);
            if (!deleted) {
                showError("The selected asset no longer exists.");
            } else {
                showInfo("Asset deleted successfully.");
            }
            refreshAssets();
            clearForm();
        } catch (SQLException exception) {
            showDatabaseError(exception);
        }
    }

    private void runSearch() {
        try {
            List<Asset> results = searchField.getText().isBlank()
                    ? repository.findAll()
                    : repository.search(searchField.getText());
            rows.setAll(results);
            totalLabel.setText("Matching assets: " + results.size());
            statusLabel.setText("Search complete");
        } catch (SQLException exception) {
            showDatabaseError(exception);
        }
    }

    private void refreshAssets() {
        try {
            rows.setAll(repository.findAll());
            totalLabel.setText("Total assets: " + repository.countAll());
            statusLabel.setText("Connected · data loaded from MySQL");
        } catch (SQLException exception) {
            rows.clear();
            totalLabel.setText("Total assets: —");
            statusLabel.setText("Database connection failed");
            showDatabaseError(exception);
        }
    }

    private void validateForm() {
        if (tagField.getText().isBlank()) {
            throw new IllegalArgumentException("Asset tag is required.");
        }
        if (tagField.getText().trim().length() > 30) {
            throw new IllegalArgumentException("Asset tag must be 30 characters or fewer.");
        }
        if (nameField.getText().isBlank()) {
            throw new IllegalArgumentException("Asset name is required.");
        }
        if (nameField.getText().trim().length() > 100) {
            throw new IllegalArgumentException("Asset name must be 100 characters or fewer.");
        }
        if (categoryField.getText().isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (categoryField.getText().trim().length() > 50) {
            throw new IllegalArgumentException("Category must be 50 characters or fewer.");
        }
        if (statusBox.getValue() == null) {
            throw new IllegalArgumentException("Choose an asset status.");
        }
        if (assignedField.getText().trim().length() > 100) {
            throw new IllegalArgumentException("Assigned-to field must be 100 characters or fewer.");
        }
        parseCost();
    }

    private BigDecimal parseCost() {
        String value = costField.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Purchase cost is required. Use 0 if unknown.");
        }
        try {
            BigDecimal cost = new BigDecimal(value);
            if (cost.signum() < 0) {
                throw new IllegalArgumentException("Purchase cost cannot be negative.");
            }
            if (cost.scale() > 2) {
                throw new IllegalArgumentException("Purchase cost can have at most two decimal places.");
            }
            if (cost.precision() > 12) {
                throw new IllegalArgumentException("Purchase cost is too large.");
            }
            return cost;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid numeric purchase cost, e.g. 62000.00.");
        }
    }

    private void populateForm(Asset asset) {
        if (asset == null) {
            selectedId = -1;
            return;
        }
        selectedId = asset.getAssetId();
        tagField.setText(asset.getAssetTag());
        nameField.setText(asset.getAssetName());
        categoryField.setText(asset.getCategory());
        assignedField.setText(asset.getAssignedTo() == null ? "" : asset.getAssignedTo());
        statusBox.setValue(asset.getStatus());
        costField.setText(asset.getPurchaseCost().toPlainString());
        purchaseDatePicker.setValue(asset.getPurchaseDate());
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        selectedId = -1;
        tagField.clear();
        nameField.clear();
        categoryField.clear();
        assignedField.clear();
        statusBox.setValue("AVAILABLE");
        costField.clear();
        purchaseDatePicker.setValue(null);
    }

    private void showInfo(String message) {
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AssetPulse");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        statusLabel.setText("Action needs attention");
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("AssetPulse — Check input");
        alert.setHeaderText("Unable to complete action");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showDatabaseError(SQLException exception) {
        String detail = exception.getMessage() == null
                ? "No additional database error details."
                : exception.getMessage();

        String message;
        String lower = detail.toLowerCase();
        if (lower.contains("communications link failure")
                || lower.contains("connection refused")
                || lower.contains("connect timed out")) {
            message = "Cannot reach the configured MySQL server. Make sure MySQL Server " +
                    "is installed and running, and check ASSETPULSE_DB_URL.\n\nDetails: " + detail;
        } else if (lower.contains("access denied")) {
            message = "MySQL rejected the configured username/password. Check " +
                    "ASSETPULSE_DB_USER and ASSETPULSE_DB_PASSWORD.\n\nDetails: " + detail;
        } else if (lower.contains("unknown database")) {
            message = "Database assetpulse_db was not found. Run database/schema.sql " +
                    "in MySQL Workbench.\n\nDetails: " + detail;
        } else if (lower.contains("duplicate entry")) {
            message = "That asset tag already exists. Use a unique tag.\n\nDetails: " + detail;
        } else {
            message = "A database operation failed. Check that MySQL is running, " +
                    "the schema exists, and the app user has SELECT/INSERT/UPDATE/DELETE permissions." +
                    "\n\nDetails: " + detail;
        }

        statusLabel.setText("Database operation failed");
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("AssetPulse — Database error");
        alert.setHeaderText("Could not complete the database operation");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
