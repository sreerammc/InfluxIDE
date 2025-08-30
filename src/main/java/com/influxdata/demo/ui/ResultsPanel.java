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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Dragboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;

import java.util.List;
import java.util.Map;

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
    private Label memoryInfoLabel;
    private Button memoryInfoButton;
    
    // Data
    private ObservableList<Map<String, Object>> tableData;
    private String currentJsonData;
    
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
        
        // Memory info
        memoryInfoLabel = new Label("Memory: --");
        memoryInfoLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.DEFAULT_FONT_SIZE));
        memoryInfoLabel.setTextFill(Color.web(UIConstants.SECONDARY_COLOR));
        
        memoryInfoButton = new Button("Memory Info");
        memoryInfoButton.setStyle(UIConstants.SECONDARY_BUTTON_STYLE);
        memoryInfoButton.setPrefWidth(100);
        memoryInfoButton.setPrefHeight(30);
        
        // Initialize table data
        tableData = FXCollections.observableArrayList();
        resultsTable.setItems(tableData);
    }
    
    /**
     * Setup component layout
     */
    private void setupLayout() {
        // Bottom info bar
        HBox bottomInfoBar = new HBox(UIConstants.DEFAULT_SPACING);
        bottomInfoBar.setAlignment(Pos.CENTER_LEFT);
        bottomInfoBar.setPadding(new Insets(5, 0, 0, 0));
        bottomInfoBar.getChildren().addAll(recordCountLabel, memoryInfoLabel, memoryInfoButton);
        
        // Main layout
        VBox mainLayout = new VBox(UIConstants.DEFAULT_SPACING);
        mainLayout.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        mainLayout.getChildren().addAll(resultsTabPane, bottomInfoBar);
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        memoryInfoButton.setOnAction(e -> showMemoryInfo());
    }
    
    /**
     * Setup drag and drop functionality
     */
    private void setupDragAndDrop() {
        resultsTable.setOnDragDetected(event -> {
            if (resultsTable.getSelectionModel().getSelectedItem() != null) {
                Dragboard db = resultsTable.startDragAndDrop(javafx.scene.input.TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                
                // Get selected cell value
                TablePosition<Map<String, Object>, ?> pos = resultsTable.getFocusModel().getFocusedCell();
                if (pos != null) {
                    Map<String, Object> row = resultsTable.getItems().get(pos.getRow());
                    if (row != null) {
                        String columnName = getColumnName(pos.getColumn());
                        Object value = row.get(columnName);
                        if (value != null) {
                            content.putString(value.toString());
                            db.setContent(content);
                            event.consume();
                        }
                    }
                }
            }
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
        bottomInfoBar.getChildren().addAll(recordCountLabel, memoryInfoLabel, memoryInfoButton);
        
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
            // Clear existing data
            tableData.clear();
            resultsTable.getColumns().clear();
            
            // Create columns based on first result
            Map<String, Object> firstRow = results.get(0);
            if (firstRow != null) {
                for (String columnName : firstRow.keySet()) {
                    TableColumn<Map<String, Object>, Object> column = new TableColumn<>(columnName);
                    column.setCellValueFactory(new PropertyValueFactory<>(columnName));
                    column.setPrefWidth(150);
                    column.setResizable(true);
                    resultsTable.getColumns().add(column);
                }
            }
            
            // Add data rows
            tableData.addAll(results);
            
            // Update record count
            updateRecordCount(results.size());
            
            // Update memory info
            updateMemoryInfo();
            
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
     * Clear all results
     */
    public void clearResults() {
        tableData.clear();
        resultsTable.getColumns().clear();
        jsonTextArea.clear();
        currentJsonData = null;
        updateRecordCount(0);
        updateMemoryInfo();
    }
    
    /**
     * Update record count display
     */
    private void updateRecordCount(int count) {
        recordCountLabel.setText("Records: " + count);
    }
    
    /**
     * Update memory information
     */
    private void updateMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        String memoryInfo = String.format("Memory: %.1f MB used / %.1f MB total / %.1f MB max", 
            usedMemory / (1024.0 * 1024.0),
            totalMemory / (1024.0 * 1024.0),
            maxMemory / (1024.0 * 1024.0));
        
        memoryInfoLabel.setText(memoryInfo);
    }
    
    /**
     * Show memory information dialog
     */
    private void showMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        String memoryInfo = String.format(
            "Memory Information:\n\n" +
            "Used Memory: %.1f MB\n" +
            "Free Memory: %.1f MB\n" +
            "Total Memory: %.1f MB\n" +
            "Max Memory: %.1f MB\n\n" +
            "Memory Usage: %.1f%%",
            usedMemory / (1024.0 * 1024.0),
            freeMemory / (1024.0 * 1024.0),
            totalMemory / (1024.0 * 1024.0),
            maxMemory / (1024.0 * 1024.0),
            (usedMemory * 100.0) / maxMemory
        );
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Memory Information");
        alert.setHeaderText(null);
        alert.setContentText(memoryInfo);
        alert.showAndWait();
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
} 