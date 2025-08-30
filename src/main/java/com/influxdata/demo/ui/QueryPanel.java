package com.influxdata.demo.ui;

import com.influxdata.demo.config.UIConstants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Query panel component for InfluxDB IDE
 * Handles query input, execution, and control buttons
 */
public class QueryPanel {
    
    // UI Components
    private TextArea queryArea;
    private Button executeButton;
    private Button stopButton;
    private Button clearButton;
    private Button exportButton;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    
    // Event handlers
    private Runnable onExecuteQuery;
    private Runnable onStopQuery;
    private Runnable onClearQuery;
    private Runnable onExportResults;
    
    public QueryPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        // Query input area
        queryArea = new TextArea();
        queryArea.setPromptText("Enter your InfluxDB query here...\nExample: SELECT * FROM measurement_name LIMIT 100");
        queryArea.setWrapText(true);
        queryArea.setPrefRowCount(2);
        queryArea.setMinHeight(40);
        queryArea.setMaxHeight(120);
        queryArea.setPrefWidth(800);
        
        // Control buttons
        executeButton = new Button("Execute Query");
        executeButton.setStyle(UIConstants.PRIMARY_BUTTON_STYLE);
        executeButton.setPrefWidth(UIConstants.BUTTON_PREF_WIDTH);
        executeButton.setPrefHeight(UIConstants.BUTTON_PREF_HEIGHT);
        
        stopButton = new Button("Stop Query");
        stopButton.setStyle(UIConstants.SECONDARY_BUTTON_STYLE);
        stopButton.setPrefWidth(UIConstants.BUTTON_PREF_WIDTH);
        stopButton.setPrefHeight(UIConstants.BUTTON_PREF_HEIGHT);
        stopButton.setDisable(true);
        
        clearButton = new Button("Clear");
        clearButton.setStyle(UIConstants.SECONDARY_BUTTON_STYLE);
        clearButton.setPrefWidth(80);
        clearButton.setPrefHeight(UIConstants.BUTTON_PREF_HEIGHT);
        
        exportButton = new Button("Export CSV");
        exportButton.setStyle(UIConstants.SUCCESS_BUTTON_STYLE);
        exportButton.setPrefWidth(100);
        exportButton.setPrefHeight(UIConstants.BUTTON_PREF_HEIGHT);
        exportButton.setDisable(true);
        
        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);
        
        // Status label
        statusLabel = new Label("Ready");
        statusLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.DEFAULT_FONT_SIZE));
    }
    
    /**
     * Setup component layout
     */
    private void setupLayout() {
        // Button row
        HBox buttonRow = new HBox(UIConstants.DEFAULT_SPACING);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.getChildren().addAll(executeButton, stopButton, clearButton, exportButton, progressIndicator);
        
        // Status row
        HBox statusRow = new HBox(UIConstants.DEFAULT_SPACING);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getChildren().add(statusLabel);
        
        // Main layout
        VBox mainLayout = new VBox(UIConstants.DEFAULT_SPACING);
        mainLayout.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        mainLayout.getChildren().addAll(queryArea, buttonRow, statusRow);
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        executeButton.setOnAction(e -> {
            if (onExecuteQuery != null) {
                onExecuteQuery.run();
            }
        });
        
        stopButton.setOnAction(e -> {
            if (onStopQuery != null) {
                onStopQuery.run();
            }
        });
        
        clearButton.setOnAction(e -> {
            if (onClearQuery != null) {
                onClearQuery.run();
            }
        });
        
        exportButton.setOnAction(e -> {
            if (onExportResults != null) {
                onExportResults.run();
            }
        });
    }
    
    /**
     * Get the main layout container
     */
    public VBox getLayout() {
        VBox mainLayout = new VBox(UIConstants.DEFAULT_SPACING);
        mainLayout.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        
        // Button row
        HBox buttonRow = new HBox(UIConstants.DEFAULT_SPACING);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.getChildren().addAll(executeButton, stopButton, clearButton, exportButton, progressIndicator);
        
        // Status row
        HBox statusRow = new HBox(UIConstants.DEFAULT_SPACING);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getChildren().add(statusLabel);
        
        mainLayout.getChildren().addAll(queryArea, buttonRow, statusRow);
        return mainLayout;
    }
    
    /**
     * Set query execution handler
     */
    public void setOnExecuteQuery(Runnable handler) {
        this.onExecuteQuery = handler;
    }
    
    /**
     * Set query stop handler
     */
    public void setOnStopQuery(Runnable handler) {
        this.onStopQuery = handler;
    }
    
    /**
     * Set query clear handler
     */
    public void setOnClearQuery(Runnable handler) {
        this.onClearQuery = handler;
    }
    
    /**
     * Set export results handler
     */
    public void setOnExportResults(Runnable handler) {
        this.onExportResults = handler;
    }
    
    /**
     * Get the current query text
     */
    public String getQueryText() {
        return queryArea.getText();
    }
    
    /**
     * Set the query text
     */
    public void setQueryText(String text) {
        queryArea.setText(text);
    }
    
    /**
     * Clear the query text
     */
    public void clearQuery() {
        queryArea.clear();
    }
    
    /**
     * Set query execution state
     */
    public void setExecuting(boolean executing) {
        executeButton.setDisable(executing);
        stopButton.setDisable(!executing);
        progressIndicator.setVisible(executing);
        
        if (executing) {
            statusLabel.setText("Executing query...");
            statusLabel.setTextFill(javafx.scene.paint.Color.BLUE);
        } else {
            statusLabel.setText("Ready");
            statusLabel.setTextFill(javafx.scene.paint.Color.BLACK);
        }
    }
    
    /**
     * Set query completion state
     */
    public void setQueryCompleted(boolean hasResults) {
        executeButton.setDisable(false);
        stopButton.setDisable(true);
        progressIndicator.setVisible(false);
        exportButton.setDisable(!hasResults);
        
        if (hasResults) {
            statusLabel.setText("Query completed successfully");
            statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
        } else {
            statusLabel.setText("Query completed (no results)");
            statusLabel.setTextFill(javafx.scene.paint.Color.ORANGE);
        }
    }
    
    /**
     * Set error state
     */
    public void setError(String errorMessage) {
        executeButton.setDisable(false);
        stopButton.setDisable(true);
        progressIndicator.setVisible(false);
        exportButton.setDisable(true);
        
        statusLabel.setText("Error: " + errorMessage);
        statusLabel.setTextFill(javafx.scene.paint.Color.RED);
    }
    
    /**
     * Enable/disable export button
     */
    public void setExportEnabled(boolean enabled) {
        exportButton.setDisable(!enabled);
    }
    
    /**
     * Get the query area for external access
     */
    public TextArea getQueryArea() {
        return queryArea;
    }
    
    /**
     * Get the execute button for external access
     */
    public Button getExecuteButton() {
        return executeButton;
    }
    
    /**
     * Get the stop button for external access
     */
    public Button getStopButton() {
        return stopButton;
    }
} 