package com.influxdata.demo.controller;

import com.influxdata.demo.config.ApplicationConfig;
import com.influxdata.demo.config.UIConstants;
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
    private VBox querySection;
    private VBox resultsSection;
    private Label connectionStatusLabel;
    
    public MainApplicationController(Stage mainStage, ApplicationConfig connectionConfig) {
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
        
        // Set connection configuration
        if (connectionConfig != null && connectionConfig.isValid()) {
            this.currentConfig = connectionConfig;
            this.influxDBService = new InfluxDBService(connectionConfig);
            Log.connectionInfo("Database connection established on startup");
        } else {
            // Fallback to loading from settings if no config provided
            loadConfiguration();
        }
        
        // Initialize UI
        initializeUI();
        setupEventHandlers();
        
        // Update connection status
        updateConnectionStatusOnStartup();
        
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
        mainStage.setMaximized(true);
        
        // Set application icon
        setApplicationIcon();
    }
    
    /**
     * Create the main layout
     */
    private VBox createMainLayout() {
        VBox mainLayout = new VBox(UIConstants.DEFAULT_SPACING);
        mainLayout.setStyle(UIConstants.BACKGROUND_STYLE);
        
        // Menu bar
        MenuBar menuBar = createMenuBar();
        
        // Header section
        VBox headerSection = createHeaderSection();
        
        // Query section (initially visible)
        querySection = createQuerySection();
        
        // Results section
        resultsSection = createResultsSection();
        
        // Status section
        HBox statusSection = createStatusSection();
        
        mainLayout.getChildren().addAll(menuBar, headerSection, querySection, resultsSection, statusSection);
        
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
        
        // Header layout
        HBox headerLayout = new HBox(UIConstants.DEFAULT_SPACING);
        headerLayout.setAlignment(Pos.CENTER);
        headerLayout.getChildren().add(titleLabel);
        
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
        
        // Setup drag and drop connection between results panel and query panel
        setupDragAndDropConnection();
        
        resultsSectionBox.getChildren().addAll(resultsTitleLabel, resultsPanel.getLayout());
        return resultsSectionBox;
    }
    
    /**
     * Create menu bar with Database, View, and Help menus
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        
        // Database Menu
        Menu databaseMenu = new Menu("Database");
        databaseMenu.setStyle("-fx-font-weight: bold;");
        
        MenuItem showTablesItem = new MenuItem("Show Measurements");
        showTablesItem.setOnAction(e -> handleShowMeasurements());
        showTablesItem.setStyle("-fx-padding: 5 10 5 10;");
        
        MenuItem refreshConnectionItem = new MenuItem("Refresh Connection");
        refreshConnectionItem.setOnAction(e -> handleRefreshConnection());
        refreshConnectionItem.setStyle("-fx-padding: 5 10 5 10;");
        
        databaseMenu.getItems().addAll(showTablesItem, refreshConnectionItem);
        
        // View Menu
        Menu viewMenu = new Menu("View");
        viewMenu.setStyle("-fx-font-weight: bold;");
        
        MenuItem toggleMaximizeItem = new MenuItem("Toggle Maximize");
        toggleMaximizeItem.setOnAction(e -> toggleMaximize());
        toggleMaximizeItem.setStyle("-fx-padding: 5 10 5 10;");
        
        MenuItem clearResultsItem = new MenuItem("Clear Results");
        clearResultsItem.setOnAction(e -> handleClearResults());
        clearResultsItem.setStyle("-fx-padding: 5 10 5 10;");
        
        viewMenu.getItems().addAll(toggleMaximizeItem, clearResultsItem);
        
        // Tools Menu
        Menu toolsMenu = new Menu("Tools");
        toolsMenu.setStyle("-fx-font-weight: bold;");
        
        MenuItem exportResultsItem = new MenuItem("Export Results to CSV");
        exportResultsItem.setOnAction(e -> handleExportResults());
        exportResultsItem.setStyle("-fx-padding: 5 10 5 10;");
        
        MenuItem memoryInfoItem = new MenuItem("Memory Information");
        memoryInfoItem.setOnAction(e -> handleMemoryInfo());
        memoryInfoItem.setStyle("-fx-padding: 5 10 5 10;");
        
        toolsMenu.getItems().addAll(exportResultsItem, memoryInfoItem);
        
        // Help Menu
        Menu helpMenu = new Menu("Help");
        helpMenu.setStyle("-fx-font-weight: bold;");
        
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        aboutItem.setStyle("-fx-padding: 5 10 5 10;");
        
        MenuItem helpItem = new MenuItem("Help");
        helpItem.setOnAction(e -> showHelpDialog());
        helpItem.setStyle("-fx-padding: 5 10 5 10;");
        
        helpMenu.getItems().addAll(helpItem, aboutItem);
        
        menuBar.getMenus().addAll(databaseMenu, viewMenu, toolsMenu, helpMenu);
        return menuBar;
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
        connectionStatusLabel.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.NORMAL, UIConstants.SMALL_FONT_SIZE));
        connectionStatusLabel.setTextFill(javafx.scene.paint.Color.web(UIConstants.SUCCESS_COLOR));
        
        statusBox.getChildren().add(connectionStatusLabel);
        return statusBox;
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers() {
        // Window close event
        mainStage.setOnCloseRequest(this::handleWindowClose);
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
            showError("No database connection. Please restart the application and configure your connection.");
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
                    
                    // Update connection status to show success
                    updateConnectionStatus("Query completed successfully", true);
                    
                } catch (Exception e) {
                    // Log query error
                    long executionTime = System.currentTimeMillis() - startTime;
                    Log.queryError("Query execution failed after " + executionTime + "ms: " + e.getMessage());
                    Log.logException("query", "Query processing error", e);
                    
                    // Handle error
                    queryPanel.setError(e.getMessage());
                    resultsPanel.displayJsonResults(result); // Show raw JSON even on error
                    updateConnectionStatus("Query failed", false);
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
                updateConnectionStatus("Query failed", false);
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
            updateConnectionStatus("Query stopped", false);
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
                        updateConnectionStatus("Export completed", true);
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
    
    // ==================== MENU HANDLERS ====================
    
    /**
     * Handle show measurements menu item
     */
    private void handleShowMeasurements() {
        Log.appInfo("Show measurements menu item clicked");
        if (queryPanel != null) {
            queryPanel.setQueryText("SHOW MEASUREMENTS");
            handleExecuteQuery();
        }
    }
    
    /**
     * Handle refresh connection menu item
     */
    private void handleRefreshConnection() {
        Log.appInfo("Refresh connection menu item clicked");
        try {
            if (currentConfig != null && currentConfig.isValid()) {
                influxDBService = new InfluxDBService(currentConfig);
                updateConnectionStatus("Connection refreshed", true);
                showInfo("Connection Refreshed", "Database connection has been refreshed successfully.");
            } else {
                showError("No valid connection configuration found. Please reconnect.");
            }
        } catch (Exception e) {
            Log.connectionError("Failed to refresh connection: " + e.getMessage());
            showError("Failed to refresh connection: " + e.getMessage());
        }
    }
    
    /**
     * Toggle maximize window
     */
    private void toggleMaximize() {
        Log.appInfo("Toggle maximize menu item clicked");
        if (mainStage.isMaximized()) {
            mainStage.setMaximized(false);
            mainStage.setWidth(1200);
            mainStage.setHeight(900);
            mainStage.centerOnScreen();
        } else {
            mainStage.setMaximized(true);
        }
    }
    
    
    /**
     * Handle clear results menu item
     */
    private void handleClearResults() {
        Log.appInfo("Clear results menu item clicked");
        if (resultsPanel != null) {
            resultsPanel.clearResults();
            currentResults = null;
            updateRecordCount(0);
        }
    }
    
    /**
     * Handle memory info menu item
     */
    private void handleMemoryInfo() {
        Log.appInfo("Memory info menu item clicked");
        showMemoryInfoDialog();
    }
    
    /**
     * Show about dialog
     */
    private void showAboutDialog() {
        Log.appInfo("About dialog requested");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About InfluxDB IDE");
        alert.setHeaderText("InfluxDB IDE v2.0.0 Beta - Professional Edition");
        alert.setContentText(
            "A modern, user-friendly JavaFX-based IDE for querying InfluxDB databases.\n\n" +
            "🚧 BETA VERSION - This is a pre-release version for testing purposes.\n" +
            "Some features may be experimental or subject to change.\n\n" +
            "Features:\n" +
            "• Modern JavaFX UI with professional styling\n" +
            "• Database connection management\n" +
            "• SQL query execution with syntax highlighting\n" +
            "• Results display with Excel-like filtering and sorting\n" +
            "• Drag & drop functionality\n" +
            "• CSV export capabilities\n" +
            "• Comprehensive logging system\n\n" +
            "Built with JavaFX and InfluxDB Java Client\n" +
            "© 2025 InfluxDB IDE Team"
        );
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500, 400);
        alert.showAndWait();
    }
    
    /**
     * Show help dialog
     */
    private void showHelpDialog() {
        Log.appInfo("Help dialog requested");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help - InfluxDB IDE");
        alert.setHeaderText("How to use InfluxDB IDE");
        alert.setContentText(
            "Getting Started:\n\n" +
            "1. **Connect to Database**: Use the connection dialog to enter your InfluxDB details\n" +
            "2. **Write Queries**: Type your SQL queries in the query editor\n" +
            "3. **Execute Queries**: Click 'Execute Query' or press Ctrl+Enter\n" +
            "4. **View Results**: Results appear in both table and JSON format\n" +
            "5. **Filter & Sort**: Use column headers to sort and filter data\n" +
            "6. **Export Data**: Use the Tools menu to export results to CSV\n\n" +
            "Keyboard Shortcuts:\n" +
            "• Ctrl+Enter: Execute query\n" +
            "• Ctrl+E: Export results\n" +
            "• F11: Toggle maximize\n\n" +
            "For more help, visit the project documentation."
        );
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(600, 500);
        alert.showAndWait();
    }
    
    /**
     * Show memory information dialog
     */
    private void showMemoryInfoDialog() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        String memoryInfo = String.format(
            "Memory Information:\n\n" +
            "Total Memory: %s MB\n" +
            "Used Memory: %s MB\n" +
            "Free Memory: %s MB\n" +
            "Max Memory: %s MB\n" +
            "Memory Usage: %.1f%%\n\n" +
            "Available Processors: %d",
            totalMemory / (1024 * 1024),
            usedMemory / (1024 * 1024),
            freeMemory / (1024 * 1024),
            maxMemory / (1024 * 1024),
            (double) usedMemory / maxMemory * 100,
            runtime.availableProcessors()
        );
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Memory Information");
        alert.setHeaderText("System Memory Status");
        alert.setContentText(memoryInfo);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(400, 300);
        alert.showAndWait();
    }
    
    
    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Log.appInfo("Info dialog shown: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    
    /**
     * Update record count display
     */
    private void updateRecordCount(int count) {
    }
    
    /**
     * Update connection status display
     */
    private void updateConnectionStatus(String message, boolean isSuccess) {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(message);
            if (isSuccess) {
                connectionStatusLabel.setTextFill(javafx.scene.paint.Color.web(UIConstants.SUCCESS_COLOR));
            } else {
                connectionStatusLabel.setTextFill(javafx.scene.paint.Color.web(UIConstants.ERROR_COLOR));
            }
        }
    }
    
    /**
     * Update connection status on startup
     */
    private void updateConnectionStatusOnStartup() {
        if (influxDBService != null && currentConfig != null) {
            updateConnectionStatus("Connected", true);
        } else {
            updateConnectionStatus("Not connected", false);
        }
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
    
    /**
     * Get the query panel for external access
     */
    public QueryPanel getQueryPanel() {
        return queryPanel;
    }
    
    /**
     * Setup drag and drop connection between results panel and query panel
     */
    private void setupDragAndDropConnection() {
        if (resultsPanel != null && queryPanel != null) {
            // The drag and drop is already set up in both panels
            // ResultsPanel handles drag detection and QueryPanel handles drop
            Log.appInfo("Drag and drop connection established between results and query panels");
        }
    }
} 