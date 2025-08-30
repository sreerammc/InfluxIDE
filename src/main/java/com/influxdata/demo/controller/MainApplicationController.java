package com.influxdata.demo.controller;

import com.influxdata.demo.config.ApplicationConfig;
import com.influxdata.demo.config.UIConstants;
import com.influxdata.demo.exception.ApplicationException;
import com.influxdata.demo.exception.ConnectionException;
import com.influxdata.demo.exception.QueryExecutionException;
import com.influxdata.demo.service.*;
import com.influxdata.demo.ui.ConnectionDialog;
import com.influxdata.demo.ui.QueryPanel;
import com.influxdata.demo.ui.ResultsPanel;
import com.influxdata.demo.util.Log;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main application controller for InfluxDB IDE
 * Coordinates all UI components and business services
 */
public class MainApplicationController {
    
    // Main stage and scene
    private final Stage mainStage;
    private Scene mainScene;
    
    // Services
    private final SettingsService settingsService;
    private final TimezoneService timezoneService;
    private InfluxDBService influxDBService;
    private final DataProcessingService dataProcessingService;
    private final ExportService exportService;
    
    // UI Components
    private ConnectionDialog connectionDialog;
    private QueryPanel queryPanel;
    private ResultsPanel resultsPanel;
    
    // Application state
    private ApplicationConfig currentConfig;
    private List<Map<String, Object>> currentResults;
    private final AtomicBoolean isQueryRunning = new AtomicBoolean(false);
    
    // UI Elements
    private CheckBox queryToggleCheckBox;
    private VBox querySection;
    private VBox resultsSection;
    private Label connectionStatusLabel;
    private Label querySectionStatusLabel;
    private Label recordCountLabel;
    
    public MainApplicationController(Stage mainStage) {
        this.mainStage = mainStage;
        
        // Initialize logging system
        try {
            Log.appInfo("Starting InfluxDB IDE v2.0.0 - Professional Edition");
            Log.appInfo("Log directory: " + Log.getLogDirectory());
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
        
        // Initialize services
        this.settingsService = new SettingsService();
        this.timezoneService = new TimezoneService();
        this.dataProcessingService = new DataProcessingService();
        this.exportService = new ExportService();
        
        // Load configuration
        loadConfiguration();
        
        // Initialize UI
        initializeUI();
        setupEventHandlers();
        
        // Set JVM timezone
        setJVMTimezone();
    }
    
    /**
     * Load application configuration
     */
    private void loadConfiguration() {
        try {
            currentConfig = settingsService.loadSettings();
            if (currentConfig.isValid()) {
                influxDBService = new InfluxDBService(currentConfig);
            }
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            currentConfig = new ApplicationConfig();
        }
    }
    
    /**
     * Initialize the main UI
     */
    private void initializeUI() {
        // Create main layout
        VBox mainLayout = createMainLayout();
        
        // Create main scene
        mainScene = new Scene(mainLayout, UIConstants.MAIN_WINDOW_WIDTH, UIConstants.MAIN_WINDOW_HEIGHT);
        
        // Configure main stage
        mainStage.setTitle("InfluxDB IDE v2.0.0 - Professional Edition");
        mainStage.setScene(mainScene);
        mainStage.setMinWidth(UIConstants.MAIN_WINDOW_MIN_WIDTH);
        mainStage.setMinHeight(UIConstants.MAIN_WINDOW_MIN_HEIGHT);
        
        // Set application icon
        setApplicationIcon();
    }
    
    /**
     * Create the main layout
     */
    private VBox createMainLayout() {
        VBox mainLayout = new VBox(UIConstants.DEFAULT_SPACING);
        mainLayout.setStyle(UIConstants.BACKGROUND_STYLE);
        
        // Header section
        VBox headerSection = createHeaderSection();
        
        // Query section (initially visible)
        querySection = createQuerySection();
        
        // Results section
        resultsSection = createResultsSection();
        
        // Status section
        HBox statusSection = createStatusSection();
        
        mainLayout.getChildren().addAll(headerSection, querySection, resultsSection, statusSection);
        
        return mainLayout;
    }
    
    /**
     * Create header section
     */
    private VBox createHeaderSection() {
        VBox headerBox = new VBox(UIConstants.DEFAULT_SPACING);
        headerBox.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        headerBox.setStyle(UIConstants.BACKGROUND_STYLE);
        
        // Title
        Label titleLabel = new Label("InfluxDB IDE v2.0.0 - Professional Edition");
        titleLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.TITLE_FONT_SIZE));
        titleLabel.setTextFill(javafx.scene.paint.Color.web(UIConstants.TITLE_COLOR));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Query toggle checkbox
        queryToggleCheckBox = new CheckBox("Show Query Section");
        queryToggleCheckBox.setSelected(true);
        queryToggleCheckBox.setTooltip(new Tooltip("Toggle visibility of the query input section"));
        
        // Header layout
        HBox headerLayout = new HBox(UIConstants.DEFAULT_SPACING);
        headerLayout.setAlignment(Pos.CENTER);
        headerLayout.getChildren().addAll(titleLabel, queryToggleCheckBox);
        
        headerBox.getChildren().add(headerLayout);
        return headerBox;
    }
    
    /**
     * Create query section
     */
    private VBox createQuerySection() {
        VBox querySectionBox = new VBox(UIConstants.DEFAULT_SPACING);
        querySectionBox.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        querySectionBox.setStyle(UIConstants.BACKGROUND_STYLE);
        
        // Create query panel
        queryPanel = new QueryPanel();
        
        // Setup query panel event handlers
        queryPanel.setOnExecuteQuery(this::handleExecuteQuery);
        queryPanel.setOnStopQuery(this::handleStopQuery);
        queryPanel.setOnClearQuery(this::handleClearQuery);
        queryPanel.setOnExportResults(this::handleExportResults);
        
        querySectionBox.getChildren().add(queryPanel.getLayout());
        return querySectionBox;
    }
    
    /**
     * Create results section
     */
    private VBox createResultsSection() {
        VBox resultsSectionBox = new VBox(UIConstants.DEFAULT_SPACING);
        resultsSectionBox.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        resultsSectionBox.setStyle(UIConstants.BACKGROUND_STYLE);
        
        // Section title
        Label resultsTitleLabel = new Label("Query Results");
        resultsTitleLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.DEFAULT_FONT_SIZE));
        resultsTitleLabel.setTextFill(javafx.scene.paint.Color.web(UIConstants.PRIMARY_COLOR));
        
        // Create results panel
        resultsPanel = new ResultsPanel();
        
        // Record count label
        recordCountLabel = new Label("Records: 0");
        recordCountLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.DEFAULT_FONT_SIZE));
        recordCountLabel.setTextFill(javafx.scene.paint.Color.web(UIConstants.SECONDARY_COLOR));
        
        resultsSectionBox.getChildren().addAll(resultsTitleLabel, resultsPanel.getLayout(), recordCountLabel);
        return resultsSectionBox;
    }
    
    /**
     * Create status section
     */
    private HBox createStatusSection() {
        HBox statusBox = new HBox(UIConstants.DEFAULT_SPACING);
        statusBox.setPadding(new Insets(UIConstants.DEFAULT_SPACING));
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setStyle(UIConstants.BACKGROUND_STYLE);
        
        // Connection status (left)
        connectionStatusLabel = new Label("Ready");
        connectionStatusLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.DEFAULT_FONT_SIZE));
        connectionStatusLabel.setTextFill(javafx.scene.paint.Color.web(UIConstants.SUCCESS_COLOR));
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Query section status (right)
        querySectionStatusLabel = new Label("Query section visible");
        querySectionStatusLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.DEFAULT_FONT_SIZE));
        querySectionStatusLabel.setTextFill(javafx.scene.paint.Color.web(UIConstants.SECONDARY_COLOR));
        
        statusBox.getChildren().addAll(connectionStatusLabel, spacer, querySectionStatusLabel);
        return statusBox;
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Query toggle checkbox
        queryToggleCheckBox.setOnAction(e -> toggleQuerySection());
        
        // Window close event
        mainStage.setOnCloseRequest(this::handleWindowClose);
    }
    
    /**
     * Toggle query section visibility
     */
    private void toggleQuerySection() {
        boolean isVisible = queryToggleCheckBox.isSelected();
        querySection.setVisible(isVisible);
        querySection.setManaged(isVisible);
        
        // Update status label
        querySectionStatusLabel.setText(isVisible ? "Query section visible" : "Query section hidden");
        
        // Adjust results section size
        if (isVisible) {
            resultsSection.setPrefHeight(400);
        } else {
            resultsSection.setPrefHeight(600);
        }
    }
    
    /**
     * Handle query execution
     */
    private void handleExecuteQuery() {
        if (isQueryRunning.get()) {
            Log.queryInfo("Query execution skipped - already running");
            return; // Already running
        }
        
        String query = queryPanel.getQueryText().trim();
        if (query.isEmpty()) {
            Log.queryWarning("Query execution attempted with empty query");
            showError("Please enter a query");
            return;
        }
        
        if (influxDBService == null) {
            Log.queryError("Query execution attempted without database connection");
            showError("No database connection. Please configure connection first.");
            return;
        }
        
        Log.queryInfo("Starting query execution: " + query.substring(0, Math.min(query.length(), 100)) + (query.length() > 100 ? "..." : ""));
        
        // Start query execution
        isQueryRunning.set(true);
        queryPanel.setExecuting(true);
        resultsPanel.clearResults();
        
        long startTime = System.currentTimeMillis();
        
        // Execute query asynchronously
        CompletableFuture<String> queryFuture = influxDBService.executeQueryAsync(query);
        
        queryFuture.thenAcceptAsync(result -> {
            Platform.runLater(() -> {
                try {
                    long executionTime = System.currentTimeMillis() - startTime;
                    
                    // Process results
                    currentResults = dataProcessingService.parseInfluxDBResponse(result);
                    
                    // Log successful query execution
                    Log.logQueryExecution(query, executionTime, currentResults.size());
                    
                    // Display results
                    resultsPanel.displayResults(currentResults);
                    resultsPanel.displayJsonResults(result);
                    
                    // Update UI
                    queryPanel.setQueryCompleted(!currentResults.isEmpty());
                    updateRecordCount(currentResults.size());
                    updateConnectionStatus("Query completed successfully", true);
                    
                } catch (Exception e) {
                    // Log query error
                    long executionTime = System.currentTimeMillis() - startTime;
                    Log.queryError("Query execution failed after " + executionTime + "ms: " + e.getMessage());
                    Log.logException("query", "Query processing error", e);
                    
                    // Handle error
                    queryPanel.setError(e.getMessage());
                    resultsPanel.displayJsonResults(result); // Show raw JSON even on error
                    updateConnectionStatus("Query failed: " + e.getMessage(), false);
                } finally {
                    isQueryRunning.set(false);
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                long executionTime = System.currentTimeMillis() - startTime;
                Log.queryError("Query execution failed after " + executionTime + "ms: " + throwable.getMessage());
                Log.logException("query", "Query execution error", throwable);
                
                queryPanel.setError(throwable.getMessage());
                updateConnectionStatus("Query failed: " + throwable.getMessage(), false);
                isQueryRunning.set(false);
            });
            return null;
        });
    }
    
    /**
     * Handle query stop
     */
    private void handleStopQuery() {
        if (isQueryRunning.get()) {
            isQueryRunning.set(false);
            queryPanel.setExecuting(false);
            updateConnectionStatus("Query stopped by user", false);
        }
    }
    
    /**
     * Handle query clear
     */
    private void handleClearQuery() {
        queryPanel.clearQuery();
        resultsPanel.clearResults();
        currentResults = null;
        updateRecordCount(0);
        updateConnectionStatus("Query cleared", true);
    }
    
    /**
     * Handle export results
     */
    private void handleExportResults() {
        if (currentResults == null || currentResults.isEmpty()) {
            Log.exportWarning("Export attempted with no results");
            showError("No results to export");
            return;
        }
        
        Log.exportInfo("Starting export of " + currentResults.size() + " records to CSV");
        long startTime = System.currentTimeMillis();
        
        try {
            // Show file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Results to CSV");
            fileChooser.setInitialFileName("influxdb_export.csv");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            
            File selectedFile = fileChooser.showSaveDialog(mainStage);
            if (selectedFile != null) {
                Log.exportInfo("Export file selected: " + selectedFile.getAbsolutePath());
                
                // Export asynchronously
                CompletableFuture<String> exportFuture = exportService.exportToCSVAsync(currentResults, selectedFile.getAbsolutePath());
                
                exportFuture.thenAcceptAsync(filePath -> {
                    Platform.runLater(() -> {
                        long exportTime = System.currentTimeMillis() - startTime;
                        Log.logExportOperation("CSV", currentResults.size(), exportTime, filePath);
                        showInfo("Export Successful", "Results exported to: " + filePath);
                        updateConnectionStatus("Export completed successfully", true);
                    });
                }).exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        long exportTime = System.currentTimeMillis() - startTime;
                        Log.exportError("Export failed after " + exportTime + "ms: " + throwable.getMessage());
                        Log.logException("export", "Export operation failed", throwable);
                        showError("Export failed: " + throwable.getMessage());
                        updateConnectionStatus("Export failed", false);
                    });
                    return null;
                });
            } else {
                Log.exportInfo("Export cancelled by user");
            }
        } catch (Exception e) {
            long exportTime = System.currentTimeMillis() - startTime;
            Log.exportError("Export failed after " + exportTime + "ms: " + e.getMessage());
            Log.logException("export", "Export operation failed", e);
            showError("Export failed: " + e.getMessage());
        }
    }
    
    /**
     * Update record count display
     */
    private void updateRecordCount(int count) {
        recordCountLabel.setText("Records: " + count);
    }
    
    /**
     * Update connection status
     */
    private void updateConnectionStatus(String message, boolean isSuccess) {
        connectionStatusLabel.setText(message);
        connectionStatusLabel.setTextFill(javafx.scene.paint.Color.web(
            isSuccess ? UIConstants.SUCCESS_COLOR : UIConstants.ERROR_COLOR
        ));
    }
    
    /**
     * Set JVM timezone
     */
    private void setJVMTimezone() {
        try {
            if (currentConfig != null) {
                timezoneService.setJVMTimezone(currentConfig.getSelectedTimezone());
            }
        } catch (Exception e) {
            System.err.println("Failed to set JVM timezone: " + e.getMessage());
        }
    }
    
    /**
     * Set application icon
     */
    private void setApplicationIcon() {
        try {
            // TODO: Set application icon when available
        } catch (Exception e) {
            System.err.println("Failed to set application icon: " + e.getMessage());
        }
    }
    
    /**
     * Handle window close
     */
    private void handleWindowClose(WindowEvent event) {
        Log.appInfo("Application shutdown initiated by user");
        
        // Save any pending changes
        try {
            if (currentConfig != null) {
                settingsService.saveSettings(currentConfig);
                Log.appInfo("Settings saved successfully on exit");
            }
        } catch (Exception e) {
            Log.appError("Failed to save settings on exit: " + e.getMessage());
            Log.logException("application", "Settings save error on exit", e);
        }
        
        // Shutdown logging service
        try {
            Log.shutdown();
        } catch (Exception e) {
            System.err.println("Failed to shutdown logging service: " + e.getMessage());
        }
        
        // Close application
        Platform.exit();
        System.exit(0);
    }
    
    /**
     * Show error dialog
     */
    private void showError(String message) {
        Log.uiError("Error dialog shown: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Log.uiInfo("Info dialog shown: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Get the main stage
     */
    public Stage getMainStage() {
        return mainStage;
    }
    
    /**
     * Get the main scene
     */
    public Scene getMainScene() {
        return mainScene;
    }
    
    /**
     * Get current configuration
     */
    public ApplicationConfig getCurrentConfig() {
        return currentConfig;
    }
    
    /**
     * Get current results
     */
    public List<Map<String, Object>> getCurrentResults() {
        return currentResults;
    }
} 