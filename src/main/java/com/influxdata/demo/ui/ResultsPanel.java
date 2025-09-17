package com.influxdata.demo.ui;

import com.influxdata.demo.config.UIConstants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Results panel component for InfluxDB IDE
 * Handles results display in table format and JSON view
 */
public class ResultsPanel {
    
    // UI Components
    private TabPane resultsTabPane;
    private TableView<Map<String, Object>> resultsTable;
    private TextArea jsonTextArea;
    private Label recordCountLabel;
    private TextField globalFilterField;
    private Button clearFiltersButton;
    
    // Data
    private ObservableList<Map<String, Object>> tableData;
    private ObservableList<Map<String, Object>> allData; // Store original unfiltered data
    private String currentJsonData;
    
    // Filter data
    private Map<String, Set<Object>> columnFilters = new HashMap<>();
    private Map<String, Set<Object>> selectedFilters = new HashMap<>();
    private Map<String, String> textFilters = new HashMap<>();
    private Map<String, String> filterTypes = new HashMap<>();
    
    public ResultsPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupDragAndDrop();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        // Results table
        resultsTable = new TableView<>();
        resultsTable.setPlaceholder(new Label("No results to display"));
        resultsTable.setEditable(false);
        
        // JSON text area
        jsonTextArea = new TextArea();
        jsonTextArea.setEditable(false);
        jsonTextArea.setWrapText(true);
        jsonTextArea.setPromptText("JSON results will appear here...");
        jsonTextArea.setPrefRowCount(10);
        
        // Tab pane for table and JSON views
        resultsTabPane = new TabPane();
        
        Tab tableTab = new Tab("Table View", resultsTable);
        tableTab.setClosable(false);
        
        Tab jsonTab = new Tab("JSON View", jsonTextArea);
        jsonTab.setClosable(false);
        
        resultsTabPane.getTabs().addAll(tableTab, jsonTab);
        
        // Record count label
        recordCountLabel = new Label("Records: 0");
        recordCountLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.DEFAULT_FONT_SIZE));
        recordCountLabel.setTextFill(Color.web(UIConstants.PRIMARY_COLOR));
        
        
        // Initialize table data
        tableData = FXCollections.observableArrayList();
        allData = FXCollections.observableArrayList();
        resultsTable.setItems(tableData);
        
        // Global filter field
        globalFilterField = new TextField();
        globalFilterField.setPromptText("Type to filter across all columns...");
        globalFilterField.setPrefWidth(300);
        globalFilterField.setStyle("-fx-font-size: 12px; -fx-padding: 5px;");
        
        // Clear filters button
        clearFiltersButton = new Button("Clear All Filters");
        clearFiltersButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        clearFiltersButton.setPrefWidth(120);
        clearFiltersButton.setPrefHeight(30);
    }
    
    /**
     * Setup component layout
     */
    private void setupLayout() {
        // Filter controls bar
        HBox filterControlsBar = new HBox(UIConstants.DEFAULT_SPACING);
        filterControlsBar.setAlignment(Pos.CENTER_LEFT);
        filterControlsBar.setPadding(new Insets(5, 0, 5, 0));
        filterControlsBar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        
        Label filterLabel = new Label("Global Filter:");
        filterLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.DEFAULT_FONT_SIZE));
        filterLabel.setTextFill(Color.web(UIConstants.PRIMARY_COLOR));
        
        filterControlsBar.getChildren().addAll(filterLabel, globalFilterField, clearFiltersButton);
        
        // Bottom info bar
        HBox bottomInfoBar = new HBox(UIConstants.DEFAULT_SPACING);
        bottomInfoBar.setAlignment(Pos.CENTER_LEFT);
        bottomInfoBar.setPadding(new Insets(5, 0, 0, 0));
        bottomInfoBar.getChildren().addAll(recordCountLabel);
        
        // Main layout
        VBox mainLayout = new VBox(UIConstants.DEFAULT_SPACING);
        mainLayout.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        mainLayout.getChildren().addAll(filterControlsBar, resultsTabPane, bottomInfoBar);
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        
        // Global filter functionality
        globalFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyGlobalFilter();
        });
        
        clearFiltersButton.setOnAction(e -> clearAllFilters());
    }
    
    /**
     * Apply global filter across all columns
     */
    private void applyGlobalFilter() {
        if (allData == null || allData.isEmpty()) {
            return;
        }
        
        String filterText = globalFilterField.getText().toLowerCase().trim();
        
        if (filterText.isEmpty()) {
            // Show all data
            tableData.clear();
            tableData.addAll(allData);
        } else {
            // Filter data across all columns
            tableData.clear();
            for (Map<String, Object> row : allData) {
                boolean matches = false;
                for (Object value : row.values()) {
                    if (value != null && value.toString().toLowerCase().contains(filterText)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    tableData.add(row);
                }
            }
        }
        
        // Update record count
        updateRecordCount(tableData.size());
    }
    
    /**
     * Clear all filters and show all data
     */
    private void clearAllFilters() {
        globalFilterField.clear();
        tableData.clear();
        if (allData != null) {
            tableData.addAll(allData);
        }
        updateRecordCount(tableData.size());
    }
    
    /**
     * Setup drag and drop functionality
     */
    private void setupDragAndDrop() {
        resultsTable.setOnDragDetected(event -> {
            // Get the cell under the mouse cursor
            TablePosition<Map<String, Object>, ?> pos = resultsTable.getFocusModel().getFocusedCell();
            if (pos != null && pos.getRow() >= 0 && pos.getRow() < resultsTable.getItems().size()) {
                Dragboard db = resultsTable.startDragAndDrop(javafx.scene.input.TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                
                Map<String, Object> row = resultsTable.getItems().get(pos.getRow());
                if (row != null) {
                    String columnName = getColumnName(pos.getColumn());
                    Object value = row.get(columnName);
                    if (value != null) {
                        String valueStr = value.toString();
                        content.putString(valueStr);
                        db.setContent(content);
                        
                        // Add visual feedback
                        resultsTable.setStyle("-fx-border-color: #2196F3; -fx-border-width: 2px;");
                        
                        event.consume();
                    }
                }
            }
        });
        
        // Reset visual feedback when drag ends
        resultsTable.setOnDragDone(event -> {
            resultsTable.setStyle("");
        });
    }
    
    /**
     * Get column name by index
     */
    private String getColumnName(int columnIndex) {
        if (columnIndex >= 0 && columnIndex < resultsTable.getColumns().size()) {
            TableColumn<Map<String, Object>, ?> column = resultsTable.getColumns().get(columnIndex);
            return column.getText();
        }
        return "";
    }
    
    /**
     * Get the main layout container
     */
    public VBox getLayout() {
        VBox mainLayout = new VBox(UIConstants.DEFAULT_SPACING);
        mainLayout.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        
        // Bottom info bar
        HBox bottomInfoBar = new HBox(UIConstants.DEFAULT_SPACING);
        bottomInfoBar.setAlignment(Pos.CENTER_LEFT);
        bottomInfoBar.setPadding(new Insets(5, 0, 0, 0));
        bottomInfoBar.getChildren().addAll(recordCountLabel);
        
        mainLayout.getChildren().addAll(resultsTabPane, bottomInfoBar);
        return mainLayout;
    }
    
    /**
     * Display results in table format
     */
    public void displayResults(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            clearResults();
            return;
        }
        
        try {
            System.out.println("ResultsPanel: Displaying " + results.size() + " results");
            System.out.println("ResultsPanel: First row keys: " + results.get(0).keySet());
            
            // Clear existing data
            tableData.clear();
            allData.clear();
            resultsTable.getColumns().clear();
            
            // Create columns based on first result
            Map<String, Object> firstRow = results.get(0);
            if (firstRow != null) {
                for (String columnName : firstRow.keySet()) {
                    TableColumn<Map<String, Object>, Object> column = new TableColumn<>(columnName);
                    column.setCellValueFactory(cellData -> {
                        Map<String, Object> row = cellData.getValue();
                        Object value = row.get(columnName);
                        return new javafx.beans.property.SimpleObjectProperty<>(value);
                    });
                    column.setPrefWidth(150);
                    column.setResizable(true);
                    
                    // Create filter button for column header
                    createColumnFilterButton(column, columnName);
                    
                    resultsTable.getColumns().add(column);
                }
            }
            
            // Store original data
            allData.addAll(results);
            
            // Build column filters after data is loaded
            buildColumnFilters();
            
            // Apply current filter or show all data
            applyGlobalFilter();
            
            
        } catch (Exception e) {
            showError("Failed to display results: " + e.getMessage());
        }
    }
    
    /**
     * Display JSON results
     */
    public void displayJsonResults(String jsonData) {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            jsonTextArea.setText("No JSON data to display");
            return;
        }
        
        try {
            currentJsonData = jsonData;
            jsonTextArea.setText(jsonData);
            
            // Switch to JSON tab if there's an error
            if (jsonData.contains("error") || jsonData.contains("Error")) {
                resultsTabPane.getSelectionModel().select(1); // JSON tab
            }
            
        } catch (Exception e) {
            jsonTextArea.setText("Error displaying JSON: " + e.getMessage());
        }
    }
    
    
    /**
     * Update record count display
     */
    private void updateRecordCount(int count) {
        recordCountLabel.setText("Records: " + count);
    }
    
    
    /**
     * Show error message
     */
    private void showError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }
    
    /**
     * Clear all results
     */
    public void clearResults() {
        tableData.clear();
        allData.clear();
        currentJsonData = null;
        jsonTextArea.clear();
        updateRecordCount(0);
        globalFilterField.clear();
        columnFilters.clear();
        selectedFilters.clear();
        textFilters.clear();
        filterTypes.clear();
    }
    
    /**
     * Get the results table
     */
    public TableView<Map<String, Object>> getResultsTable() {
        return resultsTable;
    }
    
    /**
     * Get the JSON text area
     */
    public TextArea getJsonTextArea() {
        return jsonTextArea;
    }
    
    /**
     * Get current JSON data
     */
    public String getCurrentJsonData() {
        return currentJsonData;
    }
    
    /**
     * Get current record count
     */
    public int getCurrentRecordCount() {
        return tableData.size();
    }
    
    /**
     * Check if results are empty
     */
    public boolean hasResults() {
        return !tableData.isEmpty();
    }
    
    /**
     * Create filter button for column header
     */
    private void createColumnFilterButton(TableColumn<Map<String, Object>, Object> column, String columnName) {
        // Create filter menu
        MenuButton filterMenu = new MenuButton("🔽");
        filterMenu.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 12px;");
        filterMenu.setTooltip(new Tooltip("Filter " + columnName));
        
        // Set the filter menu as the column header
        column.setGraphic(filterMenu);
    }
    
    /**
     * Build column filters after data is loaded
     */
    private void buildColumnFilters() {
        if (allData == null || allData.isEmpty()) {
            return;
        }
        
        // Clear existing filters
        columnFilters.clear();
        selectedFilters.clear();
        
        // Get all column names from the first row
        if (!allData.isEmpty()) {
            Map<String, Object> firstRow = allData.get(0);
            for (String columnName : firstRow.keySet()) {
                // Build unique values for this column
                Set<Object> uniqueValues = allData.stream()
                    .map(row -> row.get(columnName))
                    .collect(Collectors.toSet());
                
                columnFilters.put(columnName, uniqueValues);
                selectedFilters.put(columnName, new HashSet<>(uniqueValues));
                
                // Update the filter menu for this column
                TableColumn<Map<String, Object>, Object> column = findColumnByName(columnName);
                if (column != null && column.getGraphic() instanceof MenuButton) {
                    updateFilterMenu((MenuButton) column.getGraphic(), columnName);
                }
            }
        }
    }
    
    /**
     * Find column by name
     */
    private TableColumn<Map<String, Object>, Object> findColumnByName(String columnName) {
        for (TableColumn<Map<String, Object>, ?> column : resultsTable.getColumns()) {
            if (columnName.equals(column.getText())) {
                return (TableColumn<Map<String, Object>, Object>) column;
            }
        }
        return null;
    }
    
    /**
     * Update filter menu for a column
     */
    private void updateFilterMenu(MenuButton filterMenu, String columnName) {
        filterMenu.getItems().clear();
        
        // Add text-based filter options
        MenuItem textFilterItem = new MenuItem("Text Filters");
        textFilterItem.setOnAction(e -> showTextFilterDialog(columnName));
        
        // Add "Select All" option
        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setOnAction(e -> selectAllValues(columnName));
        
        // Add "Clear All" option
        MenuItem clearAllItem = new MenuItem("Clear All");
        clearAllItem.setOnAction(e -> clearAllValues(columnName));
        
        filterMenu.getItems().addAll(textFilterItem, new SeparatorMenuItem(), selectAllItem, clearAllItem, new SeparatorMenuItem());
        
        // Add individual value checkboxes (limit to first 20 for performance)
        Set<Object> uniqueValues = columnFilters.get(columnName);
        Set<Object> selectedValues = selectedFilters.get(columnName);
        
        int count = 0;
        for (Object value : uniqueValues) {
            if (count >= 20) {
                MenuItem moreItem = new MenuItem("... and " + (uniqueValues.size() - 20) + " more");
                moreItem.setDisable(true);
                filterMenu.getItems().add(moreItem);
                break;
            }
            
            String valueStr = value != null ? value.toString() : "null";
            CheckBox checkBox = new CheckBox(valueStr);
            checkBox.setSelected(selectedValues.contains(value));
            checkBox.setOnAction(e -> {
                if (checkBox.isSelected()) {
                    selectedValues.add(value);
                } else {
                    selectedValues.remove(value);
                }
                applyColumnFilters();
            });
            
            MenuItem menuItem = new MenuItem();
            menuItem.setGraphic(checkBox);
            filterMenu.getItems().add(menuItem);
            count++;
        }
        
        // Add apply button
        filterMenu.getItems().add(new SeparatorMenuItem());
        MenuItem applyItem = new MenuItem("Apply Filter");
        applyItem.setOnAction(e -> applyColumnFilters());
        filterMenu.getItems().add(applyItem);
    }
    
    /**
     * Select all values for a column
     */
    private void selectAllValues(String columnName) {
        selectedFilters.get(columnName).clear();
        selectedFilters.get(columnName).addAll(columnFilters.get(columnName));
        applyColumnFilters();
    }
    
    /**
     * Clear all values for a column
     */
    private void clearAllValues(String columnName) {
        selectedFilters.get(columnName).clear();
        applyColumnFilters();
    }
    
    /**
     * Show text filter dialog
     */
    private void showTextFilterDialog(String columnName) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Text Filter - " + columnName);
        dialogStage.setResizable(false);
        
        VBox dialogContent = new VBox(10);
        dialogContent.setPadding(new Insets(20));
        
        // Filter type selection
        Label typeLabel = new Label("Filter Type:");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Contains", "Starts With", "Ends With", "Equals", "Not Equals", "Is Empty", "Is Not Empty");
        typeCombo.setValue(filterTypes.getOrDefault(columnName, "Contains"));
        
        // Text input
        Label textLabel = new Label("Filter Text:");
        TextField textField = new TextField(textFilters.getOrDefault(columnName, ""));
        textField.setPromptText("Enter filter text...");
        
        // Buttons
        HBox buttonBox = new HBox(10);
        Button applyButton = new Button("Apply");
        Button clearButton = new Button("Clear");
        Button cancelButton = new Button("Cancel");
        
        applyButton.setOnAction(e -> {
            String filterType = typeCombo.getValue();
            String filterText = textField.getText().trim();
            
            if (!filterText.isEmpty() || filterType.equals("Is Empty") || filterType.equals("Is Not Empty")) {
                textFilters.put(columnName, filterText);
                filterTypes.put(columnName, filterType);
                applyColumnFilters();
            }
            dialogStage.close();
        });
        
        clearButton.setOnAction(e -> {
            textFilters.remove(columnName);
            filterTypes.remove(columnName);
            applyColumnFilters();
            dialogStage.close();
        });
        
        cancelButton.setOnAction(e -> dialogStage.close());
        
        buttonBox.getChildren().addAll(applyButton, clearButton, cancelButton);
        
        dialogContent.getChildren().addAll(typeLabel, typeCombo, textLabel, textField, buttonBox);
        
        Scene scene = new Scene(dialogContent, 300, 200);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }
    
    /**
     * Apply column filters
     */
    private void applyColumnFilters() {
        if (allData == null || allData.isEmpty()) {
            return;
        }
        
        tableData.clear();
        
        for (Map<String, Object> row : allData) {
            boolean matchesAllFilters = true;
            
            // Check value-based filters
            for (String columnName : selectedFilters.keySet()) {
                Object value = row.get(columnName);
                Set<Object> selectedValues = selectedFilters.get(columnName);
                
                if (!selectedValues.contains(value)) {
                    matchesAllFilters = false;
                    break;
                }
            }
            
            // Check text-based filters
            if (matchesAllFilters) {
                for (String columnName : textFilters.keySet()) {
                    Object value = row.get(columnName);
                    String valueStr = value != null ? value.toString() : "";
                    String filterText = textFilters.get(columnName);
                    String filterType = filterTypes.get(columnName);
                    
                    boolean textMatch = false;
                    switch (filterType) {
                        case "Contains":
                            textMatch = valueStr.toLowerCase().contains(filterText.toLowerCase());
                            break;
                        case "Starts With":
                            textMatch = valueStr.toLowerCase().startsWith(filterText.toLowerCase());
                            break;
                        case "Ends With":
                            textMatch = valueStr.toLowerCase().endsWith(filterText.toLowerCase());
                            break;
                        case "Equals":
                            textMatch = valueStr.equals(filterText);
                            break;
                        case "Not Equals":
                            textMatch = !valueStr.equals(filterText);
                            break;
                        case "Is Empty":
                            textMatch = valueStr.isEmpty();
                            break;
                        case "Is Not Empty":
                            textMatch = !valueStr.isEmpty();
                            break;
                    }
                    
                    if (!textMatch) {
                        matchesAllFilters = false;
                        break;
                    }
                }
            }
            
            if (matchesAllFilters) {
                tableData.add(row);
            }
        }
        
        updateRecordCount(tableData.size());
    }
} 