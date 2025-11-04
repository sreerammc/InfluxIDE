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
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.Alert;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.image.Image;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Priority;
import javafx.scene.control.Separator;
import javafx.geometry.Orientation;

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
    private LineChart<String, Number> timeSeriesChart;
    private VBox chartContainer;
    private ComboBox<String> filterColumnCombo;
    private ComboBox<String> filterOperatorCombo;
    private TextField filterValueField;
    private Button addFilterButton;
    private ComboBox<String> filterConditionCombo;
    private VBox activeFiltersBox;
    private List<ChartFilter> chartFilters;
    private VBox chartConfigurationBox;
    private ComboBox<String> yAxisColumnCombo;
    private ComboBox<String> seriesColumnCombo;
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
    
    // Timestamp formatting
    private String timestampFormat = "ISO_8601";
    
    public ResultsPanel() {
        chartFilters = new ArrayList<>();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupDragAndDrop();
    }
    
    /**
     * Set timestamp format for display
     */
    public void setTimestampFormat(String format) {
        this.timestampFormat = format != null ? format : "ISO_8601";
    }
    
    /**
     * Create chart configuration panel
     */
    private HBox createChartConfigPanel() {
        HBox configPanel = new HBox(15);
        configPanel.setAlignment(Pos.CENTER_LEFT);
        configPanel.setPadding(new Insets(10, 15, 10, 15));
        configPanel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");
        
        // Chart Configuration Section
        Label chartLabel = new Label("Chart Configuration:");
        chartLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        chartLabel.setTextFill(Color.DARKBLUE);
        
        // Y-Axis column selection
        Label yAxisLabel = new Label("Y-Axis:");
        yAxisLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        yAxisColumnCombo = new ComboBox<>();
        yAxisColumnCombo.setPromptText("Select Y Column");
        yAxisColumnCombo.setPrefWidth(120);
        yAxisColumnCombo.setOnAction(e -> updateChartWithCurrentSettings());
        
        // Series column selection (for grouping)
        Label seriesLabel = new Label("Group By:");
        seriesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        seriesColumnCombo = new ComboBox<>();
        seriesColumnCombo.setPromptText("Select Series Column");
        seriesColumnCombo.setPrefWidth(140);
        seriesColumnCombo.setOnAction(e -> updateChartWithCurrentSettings());
        
        // Chart filters section
        Label filterLabel = new Label("Filters:");
        filterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        filterLabel.setTextFill(Color.DARKGREEN);
        
        // Filter column selection
        filterColumnCombo = new ComboBox<>();
        filterColumnCombo.setPromptText("Column");
        filterColumnCombo.setPrefWidth(100);
        
        // Filter operator selection
        filterOperatorCombo = new ComboBox<>();
        filterOperatorCombo.getItems().addAll("=", "!=", ">", ">=", "<", "<=", "contains", "starts with", "ends with", "is null", "is not null");
        filterOperatorCombo.setValue("=");
        filterOperatorCombo.setPrefWidth(80);
        
        // Filter value input
        filterValueField = new TextField();
        filterValueField.setPromptText("Value");
        filterValueField.setPrefWidth(100);
        filterValueField.setOnAction(e -> addChartFilter()); // Allow Enter key to add filter
        
        // Condition selection (AND/OR)
        filterConditionCombo = new ComboBox<>();
        filterConditionCombo.getItems().addAll("AND", "OR");
        filterConditionCombo.setValue("AND");
        filterConditionCombo.setPrefWidth(50);
        filterConditionCombo.setVisible(false); // Initially hidden
        
        // Add filter button
        addFilterButton = new Button("+");
        addFilterButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        addFilterButton.setPrefWidth(30);
        addFilterButton.setOnAction(e -> addChartFilter());
        
        configPanel.getChildren().addAll(
            chartLabel,
            new Separator(Orientation.VERTICAL),
            yAxisLabel, yAxisColumnCombo, seriesLabel, seriesColumnCombo,
            new Separator(Orientation.VERTICAL),
            filterLabel, filterColumnCombo, filterOperatorCombo, filterValueField, filterConditionCombo, addFilterButton
        );
        
        return configPanel;
    }
    
    /**
     * Chart filter class with AND/OR condition support
     */
    private static class ChartFilter {
        private final String column;
        private final String operator;
        private final String value;
        private final String condition; // AND or OR
        
        public ChartFilter(String column, String operator, String value, String condition) {
            this.column = column;
            this.operator = operator;
            this.value = value;
            this.condition = condition;
        }
        
        public String getColumn() { return column; }
        public String getOperator() { return operator; }
        public String getValue() { return value; }
        public String getCondition() { return condition; }
        
        public boolean matches(Object cellValue) {
            // Handle null checks first
            if (operator.equals("is null")) {
                return cellValue == null;
            }
            if (operator.equals("is not null")) {
                return cellValue != null;
            }
            
            if (cellValue == null) return false;
            
            String cellStr = cellValue.toString().toLowerCase();
            String filterValue = value != null ? value.toLowerCase() : "";
            
            switch (operator) {
                case "=": return cellStr.equals(filterValue);
                case "!=": return !cellStr.equals(filterValue);
                case ">": return compareNumeric(cellValue, value) > 0;
                case ">=": return compareNumeric(cellValue, value) >= 0;
                case "<": return compareNumeric(cellValue, value) < 0;
                case "<=": return compareNumeric(cellValue, value) <= 0;
                case "contains": return cellStr.contains(filterValue);
                case "starts with": return cellStr.startsWith(filterValue);
                case "ends with": return cellStr.endsWith(filterValue);
                default: return true;
            }
        }
        
        private double compareNumeric(Object cellValue, String filterValue) {
            try {
                double cellNum = Double.parseDouble(cellValue.toString());
                double filterNum = Double.parseDouble(filterValue);
                return Double.compare(cellNum, filterNum);
            } catch (NumberFormatException e) {
                return cellValue.toString().compareTo(filterValue);
            }
        }
        
        @Override
        public String toString() {
            String filterStr = column + " " + operator;
            if (!operator.equals("is null") && !operator.equals("is not null")) {
                filterStr += " " + value;
            }
            return filterStr;
        }
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
        
        // Time series chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");
        
        timeSeriesChart = new LineChart<>(xAxis, yAxis);
        timeSeriesChart.setTitle("Time Series Data");
        timeSeriesChart.setCreateSymbols(true);
        timeSeriesChart.setLegendVisible(true);
        
        // Chart configuration panel
        HBox chartConfigPanel = createChartConfigPanel();
        
        // Active filters display
        activeFiltersBox = new VBox(5);
        activeFiltersBox.setPadding(new Insets(5));
        activeFiltersBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffeaa7; -fx-border-width: 1; -fx-border-radius: 3;");
        activeFiltersBox.setVisible(false);
        
        // Chart configuration display
        chartConfigurationBox = new VBox(5);
        chartConfigurationBox.setPadding(new Insets(5));
        chartConfigurationBox.setStyle("-fx-background-color: #e8f5e8; -fx-border-color: #4caf50; -fx-border-width: 1; -fx-border-radius: 3;");
        chartConfigurationBox.setVisible(false);
        
        // Chart container with config panel, filters, configuration info, and chart
        chartContainer = new VBox(10);
        chartContainer.setPadding(new Insets(10));
        chartContainer.getChildren().addAll(chartConfigPanel, activeFiltersBox, chartConfigurationBox, timeSeriesChart);
        VBox.setVgrow(timeSeriesChart, Priority.ALWAYS);
        
        // Tab pane for table, JSON, and chart views
        resultsTabPane = new TabPane();
        
        Tab tableTab = new Tab("Table View", resultsTable);
        tableTab.setClosable(false);
        
        Tab jsonTab = new Tab("JSON View", jsonTextArea);
        jsonTab.setClosable(false);
        
        Tab chartTab = new Tab("Chart View", chartContainer);
        chartTab.setClosable(false);
        
        resultsTabPane.getTabs().addAll(tableTab, jsonTab, chartTab);
        
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
                    
                    // Add tooltips to cells and timestamp formatting
                    column.setCellFactory(col -> new TableCell<Map<String, Object>, Object>() {
                        @Override
                        protected void updateItem(Object item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setTooltip(null);
                            } else {
                                String cellText;
                                
                                // Apply timestamp formatting if this is a timestamp column
                                if (isTimestampColumn(columnName)) {
                                    cellText = formatTimestamp(item, timestampFormat);
                                } else {
                                    cellText = item.toString();
                                }
                                
                                setText(cellText);
                                
                                // Create tooltip with full content
                                Tooltip tooltip = new Tooltip(cellText);
                                tooltip.setMaxWidth(300);
                                tooltip.setWrapText(true);
                                setTooltip(tooltip);
                            }
                        }
                    });
                    
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
            
            // Add double-click functionality after data is loaded
            addDoubleClickFunctionality();
            
            // Update chart with new data
            displayChart(results);
            
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
        clearChart();
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
    
    /**
     * Display chart data
     */
    public void displayChart(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            clearChart();
            return;
        }
        
        try {
            System.out.println("ResultsPanel: Displaying chart for " + results.size() + " results");
            
            // Clear existing chart data
            timeSeriesChart.getData().clear();
            
            // Populate column combos with all available columns
            Set<String> allColumns = results.get(0).keySet();
            updateColumnCombos(allColumns);
            
            // Auto-detect time column for X-axis
            String timeColumn = detectTimeColumn(results.get(0));
            if (timeColumn == null) {
                timeSeriesChart.setTitle("No time column detected for X-axis");
                return;
            }
            
            // Get user-selected Y-axis and series columns
            String yAxisColumn = yAxisColumnCombo.getValue();
            String seriesColumn = seriesColumnCombo.getValue();
            
            if (yAxisColumn == null) {
                timeSeriesChart.setTitle("Please select a Y-axis column");
                updateChartConfiguration(timeColumn, null, null);
                return;
            }
            
            System.out.println("Chart: Time column (X-axis) = " + timeColumn);
            System.out.println("Chart: Y-axis column = " + yAxisColumn);
            System.out.println("Chart: Series column (Group By) = " + seriesColumn);
            
            // Apply chart filters first
            List<Map<String, Object>> filteredData = applyChartFilters(results);
            System.out.println("Chart: Filtered from " + results.size() + " to " + filteredData.size() + " rows");
            
            // Group data by series column (if selected)
            if (seriesColumn != null && !seriesColumn.isEmpty()) {
                createGroupedSeries(filteredData, timeColumn, yAxisColumn, seriesColumn);
            } else {
                createSingleSeries(filteredData, timeColumn, yAxisColumn);
            }
            
            // Update chart title and configuration display
            updateChartTitle(yAxisColumn, seriesColumn);
            updateChartConfiguration(timeColumn, yAxisColumn, seriesColumn);
            
        } catch (Exception e) {
            System.err.println("Failed to display chart: " + e.getMessage());
            e.printStackTrace();
            timeSeriesChart.setTitle("Chart Error: " + e.getMessage());
        }
    }
    
    /**
     * Clear chart data
     */
    public void clearChart() {
        if (timeSeriesChart != null) {
            timeSeriesChart.getData().clear();
            timeSeriesChart.setTitle("No Data");
        }
        // Clear chart filters and configuration when clearing chart
        chartFilters.clear();
        updateActiveFiltersDisplay();
        updateChartConfiguration(null, null, null);
        if (filterColumnCombo != null) {
            filterColumnCombo.getItems().clear();
        }
        if (filterConditionCombo != null) {
            filterConditionCombo.setVisible(false);
        }
        if (yAxisColumnCombo != null) {
            yAxisColumnCombo.getItems().clear();
        }
        if (seriesColumnCombo != null) {
            seriesColumnCombo.getItems().clear();
        }
    }
    
    /**
     * Update column combos with available columns
     */
    private void updateColumnCombos(Set<String> allColumns) {
        // Update filter column combo
        if (filterColumnCombo != null) {
            filterColumnCombo.getItems().clear();
            filterColumnCombo.getItems().addAll(allColumns);
        }
        
        // Update Y-axis column combo
        if (yAxisColumnCombo != null) {
            String currentSelection = yAxisColumnCombo.getValue();
            yAxisColumnCombo.getItems().clear();
            yAxisColumnCombo.getItems().addAll(allColumns);
            // Restore selection if still valid
            if (currentSelection != null && allColumns.contains(currentSelection)) {
                yAxisColumnCombo.setValue(currentSelection);
            }
        }
        
        // Update series column combo
        if (seriesColumnCombo != null) {
            String currentSelection = seriesColumnCombo.getValue();
            seriesColumnCombo.getItems().clear();
            seriesColumnCombo.getItems().add(""); // Option for no grouping
            seriesColumnCombo.getItems().addAll(allColumns);
            // Restore selection if still valid
            if (currentSelection != null && (currentSelection.isEmpty() || allColumns.contains(currentSelection))) {
                seriesColumnCombo.setValue(currentSelection);
            }
        }
    }
    
    /**
     * Create grouped series based on series column values
     */
    private void createGroupedSeries(List<Map<String, Object>> data, String timeColumn, String yAxisColumn, String seriesColumn) {
        // Group data by series column values
        Map<String, List<Map<String, Object>>> groupedData = data.stream()
            .collect(Collectors.groupingBy(row -> {
                Object seriesValue = row.get(seriesColumn);
                return seriesValue != null ? seriesValue.toString() : "null";
            }));
        
        System.out.println("Chart: Created " + groupedData.size() + " series groups");
        
        // Create a series for each group
        for (Map.Entry<String, List<Map<String, Object>>> group : groupedData.entrySet()) {
            String seriesName = group.getKey();
            List<Map<String, Object>> seriesData = group.getValue();
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(seriesName);
            
            // Add data points to series
            for (Map<String, Object> row : seriesData) {
                Object timeValue = row.get(timeColumn);
                Object yValue = row.get(yAxisColumn);
                
                if (timeValue != null && yValue != null) {
                    String timeStr = formatTimeForChart(timeValue);
                    Number yNumber = convertToNumber(yValue);
                    
                    if (yNumber != null) {
                        series.getData().add(new XYChart.Data<>(timeStr, yNumber));
                    }
                }
            }
            
            // Sort data points by time
            series.getData().sort((a, b) -> a.getXValue().compareTo(b.getXValue()));
            
            timeSeriesChart.getData().add(series);
            System.out.println("Chart: Added series '" + seriesName + "' with " + series.getData().size() + " points");
        }
    }
    
    /**
     * Create single series (no grouping)
     */
    private void createSingleSeries(List<Map<String, Object>> data, String timeColumn, String yAxisColumn) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(yAxisColumn);
        
        // Add data points to series
        for (Map<String, Object> row : data) {
            Object timeValue = row.get(timeColumn);
            Object yValue = row.get(yAxisColumn);
            
            if (timeValue != null && yValue != null) {
                String timeStr = formatTimeForChart(timeValue);
                Number yNumber = convertToNumber(yValue);
                
                if (yNumber != null) {
                    series.getData().add(new XYChart.Data<>(timeStr, yNumber));
                }
            }
        }
        
        // Sort data points by time
        series.getData().sort((a, b) -> a.getXValue().compareTo(b.getXValue()));
        
        timeSeriesChart.getData().add(series);
        System.out.println("Chart: Added single series with " + series.getData().size() + " points");
    }
    
    /**
     * Convert value to Number for plotting
     */
    private Number convertToNumber(Object value) {
        if (value == null) return null;
        
        if (value instanceof Number) {
            return (Number) value;
        }
        
        try {
            // Try to parse as double
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            // For non-numeric values, we could assign ordinal values or skip
            // For now, let's skip non-numeric values
            return null;
        }
    }
    
    /**
     * Update chart title based on configuration
     */
    private void updateChartTitle(String yAxisColumn, String seriesColumn) {
        StringBuilder title = new StringBuilder();
        
        if (yAxisColumn != null) {
            title.append(yAxisColumn);
            if (seriesColumn != null && !seriesColumn.isEmpty()) {
                title.append(" (Grouped by ").append(seriesColumn).append(")");
            }
        } else {
            title.append("Chart");
        }
        
        if (!chartFilters.isEmpty()) {
            title.append(" [").append(chartFilters.size()).append(" filter").append(chartFilters.size() > 1 ? "s" : "").append("]");
        }
        
        timeSeriesChart.setTitle(title.toString());
    }
    
    /**
     * Update chart configuration display
     */
    private void updateChartConfiguration(String timeColumn, String yAxisColumn, String seriesColumn) {
        chartConfigurationBox.getChildren().clear();
        
        if (timeColumn == null && yAxisColumn == null && seriesColumn == null) {
            chartConfigurationBox.setVisible(false);
            return;
        }
        
        chartConfigurationBox.setVisible(true);
        
        // Configuration title
        Label configTitle = new Label("Current Chart Configuration:");
        configTitle.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        chartConfigurationBox.getChildren().add(configTitle);
        
        // Configuration details
        VBox detailsBox = new VBox(2);
        if (timeColumn != null) {
            Label timeLabel = new Label("X-Axis (Time): " + timeColumn);
            timeLabel.setFont(Font.font("Arial", 10));
            detailsBox.getChildren().add(timeLabel);
        }
        if (yAxisColumn != null) {
            Label yLabel = new Label("Y-Axis: " + yAxisColumn);
            yLabel.setFont(Font.font("Arial", 10));
            detailsBox.getChildren().add(yLabel);
        }
        if (seriesColumn != null && !seriesColumn.isEmpty()) {
            Label seriesLabel = new Label("Group By: " + seriesColumn);
            seriesLabel.setFont(Font.font("Arial", 10));
            detailsBox.getChildren().add(seriesLabel);
        }
        
        chartConfigurationBox.getChildren().add(detailsBox);
    }
    
    /**
     * Detect time column from data
     */
    private String detectTimeColumn(Map<String, Object> sampleRow) {
        if (sampleRow == null) return null;
        
        // Check for common time column names
        String[] timeColumnNames = {"_time", "time", "timestamp", "date", "datetime"};
        
        for (String columnName : timeColumnNames) {
            if (sampleRow.containsKey(columnName)) {
                return columnName;
            }
        }
        
        // Check for columns that contain time-like data
        for (Map.Entry<String, Object> entry : sampleRow.entrySet()) {
            String columnName = entry.getKey().toLowerCase();
            if (columnName.contains("time") || columnName.contains("date")) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Detect numeric columns from data
     */
    private List<String> detectNumericColumns(Map<String, Object> sampleRow) {
        List<String> numericColumns = new ArrayList<>();
        
        if (sampleRow == null) return numericColumns;
        
        for (Map.Entry<String, Object> entry : sampleRow.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();
            
            // Skip time columns and non-numeric data
            if (isTimestampColumn(columnName)) continue;
            
            if (isNumericValue(value)) {
                numericColumns.add(columnName);
            }
        }
        
        return numericColumns;
    }
    
    /**
     * Check if a value is numeric
     */
    private boolean isNumericValue(Object value) {
        if (value == null) return false;
        
        if (value instanceof Number) {
            return true;
        }
        
        if (value instanceof String) {
            try {
                Double.parseDouble((String) value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Parse numeric value from object
     */
    private Number parseNumericValue(Object value) {
        if (value == null) return null;
        
        if (value instanceof Number) {
            return (Number) value;
        }
        
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Format time value for chart display
     */
    private String formatTimeForChart(Object timeValue) {
        if (timeValue == null) return "";
        
        String timeStr = timeValue.toString();
        
        // For ISO 8601 timestamps, show just time part if it's the same date
        if (timeStr.contains("T")) {
            String[] parts = timeStr.split("T");
            if (parts.length > 1) {
                String timePart = parts[1];
                if (timePart.contains(".")) {
                    timePart = timePart.substring(0, timePart.indexOf("."));
                }
                if (timePart.endsWith("Z")) {
                    timePart = timePart.substring(0, timePart.length() - 1);
                }
                return timePart;
            }
        }
        
        return timeStr;
    }
    
    /**
     * Update chart with current settings
     */
    private void updateChartWithCurrentSettings() {
        if (allData != null && !allData.isEmpty()) {
            displayChart(new ArrayList<>(allData));
        }
    }
    
    /**
     * Add chart filter
     */
    private void addChartFilter() {
        String column = filterColumnCombo.getValue();
        String operator = filterOperatorCombo.getValue();
        String value = filterValueField.getText().trim();
        
        // Validation
        if (column == null || column.isEmpty()) {
            showFilterError("Please select a column for the filter.");
            return;
        }
        
        // Value is not required for "is null" and "is not null" operators
        if (!operator.equals("is null") && !operator.equals("is not null") && value.isEmpty()) {
            showFilterError("Please enter a value for the filter.");
            return;
        }
        
        // Get condition (AND/OR) - default to AND for first filter
        String condition = chartFilters.isEmpty() ? "AND" : filterConditionCombo.getValue();
        
        ChartFilter filter = new ChartFilter(column, operator, value, condition);
        chartFilters.add(filter);
        
        // Show condition combo for next filter
        filterConditionCombo.setVisible(true);
        
        // Clear input fields
        filterColumnCombo.setValue(null);
        filterValueField.clear();
        
        // Update active filters display
        updateActiveFiltersDisplay();
        
        // Refresh chart with filters
        updateChartWithCurrentSettings();
    }
    
    /**
     * Show filter error message
     */
    private void showFilterError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Filter");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Remove chart filter
     */
    private void removeChartFilter(ChartFilter filter) {
        chartFilters.remove(filter);
        
        // Hide condition combo if no filters remain
        if (chartFilters.isEmpty()) {
            filterConditionCombo.setVisible(false);
        }
        
        updateActiveFiltersDisplay();
        updateChartWithCurrentSettings();
    }
    
    /**
     * Update active filters display
     */
    private void updateActiveFiltersDisplay() {
        activeFiltersBox.getChildren().clear();
        
        if (chartFilters.isEmpty()) {
            activeFiltersBox.setVisible(false);
            return;
        }
        
        activeFiltersBox.setVisible(true);
        
        // Title
        Label filtersTitle = new Label("Active Filters:");
        filtersTitle.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        activeFiltersBox.getChildren().add(filtersTitle);
        
        // Filter chips with conditions
        HBox filtersRow = new HBox(5);
        for (int i = 0; i < chartFilters.size(); i++) {
            ChartFilter filter = chartFilters.get(i);
            
            // Add condition label (AND/OR) before filter (except for first filter)
            if (i > 0) {
                Label conditionLabel = new Label(filter.getCondition());
                conditionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                conditionLabel.setTextFill(filter.getCondition().equals("OR") ? Color.ORANGE : Color.DARKGREEN);
                conditionLabel.setPadding(new Insets(0, 5, 0, 5));
                filtersRow.getChildren().add(conditionLabel);
            }
            
            HBox filterChip = createFilterChip(filter);
            filtersRow.getChildren().add(filterChip);
        }
        
        // Clear all filters button
        if (!chartFilters.isEmpty()) {
            Button clearAllButton = new Button("Clear All");
            clearAllButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 10px;");
            clearAllButton.setOnAction(e -> clearAllChartFilters());
            filtersRow.getChildren().add(clearAllButton);
        }
        
        activeFiltersBox.getChildren().add(filtersRow);
    }
    
    /**
     * Create filter chip UI element
     */
    private HBox createFilterChip(ChartFilter filter) {
        HBox chip = new HBox(5);
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(2, 8, 2, 8));
        chip.setStyle("-fx-background-color: #007bff; -fx-background-radius: 12; -fx-text-fill: white;");
        
        Label filterLabel = new Label(filter.toString());
        filterLabel.setFont(Font.font("Arial", 10));
        filterLabel.setTextFill(Color.WHITE);
        
        Button removeButton = new Button("×");
        removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 0;");
        removeButton.setOnAction(e -> removeChartFilter(filter));
        
        chip.getChildren().addAll(filterLabel, removeButton);
        return chip;
    }
    
    /**
     * Clear all chart filters
     */
    private void clearAllChartFilters() {
        chartFilters.clear();
        filterConditionCombo.setVisible(false);
        updateActiveFiltersDisplay();
        updateChartWithCurrentSettings();
    }
    
    /**
     * Apply chart filters to data with AND/OR logic
     */
    private List<Map<String, Object>> applyChartFilters(List<Map<String, Object>> data) {
        if (chartFilters.isEmpty()) {
            return data;
        }
        
        return data.stream()
            .filter(row -> evaluateFiltersForRow(row))
            .collect(Collectors.toList());
    }
    
    /**
     * Evaluate all filters for a single row using AND/OR logic
     */
    private boolean evaluateFiltersForRow(Map<String, Object> row) {
        if (chartFilters.isEmpty()) {
            return true;
        }
        
        // Start with first filter result
        ChartFilter firstFilter = chartFilters.get(0);
        boolean result = firstFilter.matches(row.get(firstFilter.getColumn()));
        
        // Apply remaining filters with their conditions
        for (int i = 1; i < chartFilters.size(); i++) {
            ChartFilter filter = chartFilters.get(i);
            boolean filterResult = filter.matches(row.get(filter.getColumn()));
            
            if (filter.getCondition().equals("OR")) {
                result = result || filterResult;
            } else { // AND
                result = result && filterResult;
            }
        }
        
        return result;
    }
    
    
    /**
     * Parse timestamp to milliseconds
     */
    private long parseTimestampToMs(Object timeValue) {
        try {
            if (timeValue instanceof Long) {
                return (Long) timeValue;
            } else if (timeValue instanceof String) {
                String str = (String) timeValue;
                // Try parsing as ISO 8601
                return java.time.Instant.parse(str).toEpochMilli();
            }
        } catch (Exception e) {
            // Fallback to current time
        }
        return System.currentTimeMillis();
    }
    
    /**
     * Aggregate values based on method
     */
    private Number aggregateValues(List<Number> values, String method) {
        if (values.isEmpty()) return 0;
        
        switch (method) {
            case "Average":
                return values.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
            case "Sum":
                return values.stream().mapToDouble(Number::doubleValue).sum();
            case "Min":
                return values.stream().mapToDouble(Number::doubleValue).min().orElse(0.0);
            case "Max":
                return values.stream().mapToDouble(Number::doubleValue).max().orElse(0.0);
            case "Count":
                return values.size();
            case "First":
                return values.get(0);
            case "Last":
                return values.get(values.size() - 1);
            default:
                return values.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
        }
    }
    
    /**
     * Format timestamp based on current settings
     */
    private String formatTimestamp(Object value, String timestampFormat) {
        if (value == null) return "";
        
        try {
            // Try to parse as timestamp (various formats)
            long timestamp = parseTimestamp(value);
            
            switch (timestampFormat) {
                case "ISO_8601":
                    return formatAsISO8601(timestamp);
                case "UNIX":
                    return String.valueOf(timestamp / 1000); // Convert to seconds
                case "UNIX_MS":
                    return String.valueOf(timestamp);
                case "RFC_2822":
                    return formatAsRFC2822(timestamp);
                case "CUSTOM":
                    return formatAsCustom(timestamp, "yyyy-MM-dd HH:mm:ss");
                case "RELATIVE":
                    return formatAsRelative(timestamp);
                default:
                    return value.toString();
            }
        } catch (Exception e) {
            // If parsing fails, return original value
            return value.toString();
        }
    }
    
    /**
     * Parse timestamp from various formats
     */
    private long parseTimestamp(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            String str = (String) value;
            try {
                // Try parsing as Unix timestamp (seconds)
                if (str.matches("\\d{10}")) {
                    return Long.parseLong(str) * 1000;
                }
                // Try parsing as Unix timestamp (milliseconds)
                if (str.matches("\\d{13}")) {
                    return Long.parseLong(str);
                }
                // Try parsing as ISO 8601
                return java.time.Instant.parse(str).toEpochMilli();
            } catch (Exception e) {
                // If all parsing fails, return current time
                return System.currentTimeMillis();
            }
        }
        return System.currentTimeMillis();
    }
    
    /**
     * Format as ISO 8601
     */
    private String formatAsISO8601(long timestamp) {
        return java.time.Instant.ofEpochMilli(timestamp).toString();
    }
    
    /**
     * Format as RFC 2822
     */
    private String formatAsRFC2822(long timestamp) {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
    }
    
    /**
     * Format as custom pattern
     */
    private String formatAsCustom(long timestamp, String pattern) {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Format as relative time
     */
    private String formatAsRelative(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60000) { // Less than 1 minute
            return "Just now";
        } else if (diff < 3600000) { // Less than 1 hour
            long minutes = diff / 60000;
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else if (diff < 86400000) { // Less than 1 day
            long hours = diff / 3600000;
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else {
            long days = diff / 86400000;
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }
    }
    
    /**
     * Check if a column name suggests it's a timestamp
     */
    private boolean isTimestampColumn(String columnName) {
        if (columnName == null) return false;
        String lower = columnName.toLowerCase();
        return lower.contains("time") || lower.contains("date") || 
               lower.equals("_time") || lower.equals("timestamp");
    }
    
    /**
     * Adds double-click functionality to the results table
     */
    private void addDoubleClickFunctionality() {
        resultsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showRowDetailsPopup();
            }
        });
    }
    
    /**
     * Shows a popup with row details in a two-column layout
     * Allows copying cell values and closes on Escape key
     */
    private void showRowDetailsPopup() {
        try {
            Map<String, Object> selectedRow = resultsTable.getSelectionModel().getSelectedItem();
            
            // If no row is selected, try to get the first row or show a message
            if (selectedRow == null) {
                if (resultsTable.getItems().isEmpty()) {
                    showError("No data available to show details.");
                    return;
                } else {
                    // Use the first row as fallback
                    selectedRow = resultsTable.getItems().get(0);
                }
            }
            
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Row Details");
            popupStage.setResizable(true);
            popupStage.setMinWidth(500);
            popupStage.setMinHeight(400);
            
            // Table resize listeners will be added after table creation
            
            // Set application icon
            try {
                popupStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app_icon.png")));
            } catch (Exception e) {
                System.err.println("Failed to set popup icon: " + e.getMessage());
            }
            
            VBox popupLayout = new VBox(15);
            popupLayout.setPadding(new Insets(20));
            popupLayout.setStyle("-fx-background-color: #f5f5f5;");
            
            // Title
            Label titleLabel = new Label("Row Details");
            titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            titleLabel.setTextFill(Color.DARKBLUE);
            
            // Details table
            TableView<Map.Entry<String, Object>> detailsTable = new TableView<>();
            detailsTable.setEditable(false);
            detailsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            
            // Column column
            TableColumn<Map.Entry<String, Object>, String> columnCol = new TableColumn<>("Column");
            columnCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey()));
            columnCol.setPrefWidth(200);
            columnCol.setResizable(true);
            columnCol.setMinWidth(150);
            
            // Value column with copy functionality
            TableColumn<Map.Entry<String, Object>, Object> valueCol = new TableColumn<>("Value");
            valueCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValue()));
            valueCol.setPrefWidth(300);
            valueCol.setResizable(true);
            valueCol.setMinWidth(200);
            
            // Add copy functionality to value cells
            valueCol.setCellFactory(column -> new TableCell<Map.Entry<String, Object>, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item.toString());
                        setTooltip(new Tooltip(item.toString()));
                        
                        // Add double-click to copy functionality
                        setOnMouseClicked(event -> {
                            if (event.getClickCount() == 2) {
                                ClipboardContent content = new ClipboardContent();
                                content.putString(item.toString());
                                Clipboard.getSystemClipboard().setContent(content);
                                
                                Alert copiedAlert = new Alert(Alert.AlertType.INFORMATION);
                                copiedAlert.setTitle("Copied");
                                copiedAlert.setHeaderText(null);
                                copiedAlert.setContentText("Value copied to clipboard!");
                                copiedAlert.showAndWait();
                            }
                        });
                    }
                }
            });
            
            detailsTable.getColumns().addAll(columnCol, valueCol);
            
            // Populate table with row data
            ObservableList<Map.Entry<String, Object>> rowData = FXCollections.observableArrayList();
            for (Map.Entry<String, Object> entry : selectedRow.entrySet()) {
                rowData.add(entry);
            }
            detailsTable.setItems(rowData);
            
            // Add resize listeners after table is created
            popupStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                detailsTable.setPrefWidth(newVal.doubleValue() - 40); // Account for padding
            });
            
            popupStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                detailsTable.setPrefHeight(newVal.doubleValue() - 120); // Account for title and instruction
            });
            
            // Remove close button - users can press Escape to close
            // Add instruction label instead
            Label instructionLabel = new Label("Press Escape to close");
            instructionLabel.setFont(Font.font("Arial", 10));
            instructionLabel.setTextFill(Color.GRAY);
            instructionLabel.setStyle("-fx-font-style: italic;");
            
            popupLayout.getChildren().addAll(titleLabel, detailsTable, instructionLabel);
            
            Scene popupScene = new Scene(popupLayout);
            popupStage.setScene(popupScene);
            
            // Add escape key support
            popupStage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    popupStage.close();
                }
            });
            
            popupStage.showAndWait();
            
        } catch (Exception e) {
            showError("Failed to show row details: " + e.getMessage());
        }
    }
} 