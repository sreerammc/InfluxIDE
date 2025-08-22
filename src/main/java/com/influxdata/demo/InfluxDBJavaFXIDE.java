package com.influxdata.demo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javafx.scene.image.Image;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;

public class InfluxDBJavaFXIDE extends Application {

    private String protocol = "http"; // Default to HTTP
    private boolean skipSSLValidation = false; // Default to strict SSL
    private String host;
    private String database;
    private String token;
    
    private TextArea queryArea;
    private TabPane resultsTabPane;
    private TableView<ObservableList<String>> resultsTable;
    private TextArea rawResultArea;
    private Label recordCountLabel;
    private TextField filterField;

    private ObservableList<ObservableList<String>> allResultsData;
    private Button executeButton;
    private Button clearButton;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private Stage mainStage;
    
    // Settings file constants
    private static final String SETTINGS_DIR = System.getProperty("user.home") + File.separator + ".influxdb-ide";
    private static final String SETTINGS_FILE = SETTINGS_DIR + File.separator + "settings.properties";

    /**
     * Main entry point for the JavaFX application
     * Sets up the application icon and shows connection dialog before main window
     */
    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        
        // Set application icon for better visual identity
        setApplicationIcon(primaryStage);
        
        // Show connection dialog first - exit if user cancels
        if (!showConnectionDialog()) {
            System.exit(0);
        }
        
        // Create and display the main application window
        createMainWindow();
    }
    
    /**
     * Sets the application icon for the given stage
     * First tries to load from resources, falls back to programmatic icon
     */
    private void setApplicationIcon(Stage stage) {
        try {
            // Try to load icon from resources directory
            Image appIcon = new Image(getClass().getResourceAsStream("/icons/app_icon.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            // Create a simple programmatic icon as fallback if resource loading fails
            createProgrammaticIcon(stage);
        }
    }
    
    /**
     * Creates a programmatic fallback icon using JavaFX Canvas
     * Draws a blue rounded rectangle with "IDB" text in white
     */
    private void createProgrammaticIcon(Stage stage) {
        try {
            // Create a 32x32 canvas for the icon
            Canvas canvas = new Canvas(32, 32);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            // Draw a simple database icon with blue background and rounded corners
            gc.setFill(Color.rgb(33, 150, 243)); // Material Design Blue
            gc.fillRoundRect(4, 4, 24, 24, 8, 8);
            
            // Draw "IDB" text in white with bold Arial font
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            gc.fillText("IDB", 8, 20);
            
            // Convert canvas to WritableImage and add to stage icons
            WritableImage snapshot = canvas.snapshot(null, null);
            stage.getIcons().add(snapshot);
            
        } catch (Exception e) {
            // If all else fails, use system default icon
            System.out.println("Using system default icon");
        }
    }

    /**
     * Shows the connection setup dialog as a modal window
     * Loads saved settings and creates the connection form
     * Returns true if connection is successful, false if cancelled
     */
    private boolean showConnectionDialog() {
        // Load previously saved connection settings (excluding token for security)
        Properties savedSettings = loadSettings();
        
        // Create modal connection dialog stage
        Stage connectionStage = new Stage();
        connectionStage.initModality(Modality.APPLICATION_MODAL);
        connectionStage.setTitle("InfluxDB Connection Setup");
        connectionStage.setResizable(false);
        
        // Set the same application icon for consistency
        setApplicationIcon(connectionStage);

        VBox connectionLayout = new VBox(20);
        connectionLayout.setPadding(new Insets(30));
        connectionLayout.setAlignment(Pos.CENTER);
        connectionLayout.setStyle("-fx-background-color: #f5f5f5;");

        // Title
        Label titleLabel = new Label("InfluxDB Connection Setup");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.DARKBLUE);

        // Connection form
        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(20));
        formBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Protocol field
        HBox protocolBox = new HBox(10);
        protocolBox.setAlignment(Pos.CENTER_LEFT);
        Label protocolLabel = new Label("Protocol:");
        protocolLabel.setMinWidth(100);
        ComboBox<String> protocolCombo = new ComboBox<>();
        protocolCombo.getItems().addAll("http", "https");
        protocolCombo.setValue(savedSettings.getProperty("protocol", "http"));
        protocolCombo.setPrefWidth(100);
        protocolCombo.setTooltip(new Tooltip("Select HTTP for local/development, HTTPS for production"));
        protocolBox.getChildren().addAll(protocolLabel, protocolCombo);

        // SSL Validation field (only show when HTTPS is selected)
        HBox sslBox = new HBox(10);
        sslBox.setAlignment(Pos.CENTER_LEFT);
        Label sslLabel = new Label("SSL Options:");
        sslLabel.setMinWidth(100);
        CheckBox skipSSLValidationCheck = new CheckBox("Skip SSL Certificate Validation");
        skipSSLValidationCheck.setSelected(Boolean.parseBoolean(savedSettings.getProperty("skipSSLValidation", "false")));
        skipSSLValidationCheck.setTooltip(new Tooltip("Check this to bypass SSL certificate validation (for development/testing)"));
        sslBox.getChildren().addAll(sslLabel, skipSSLValidationCheck);
        
        // Show/hide SSL options based on protocol selection
        protocolCombo.setOnAction(e -> {
            sslBox.setVisible("https".equals(protocolCombo.getValue()));
        });
        // Set initial SSL box visibility based on loaded protocol
        sslBox.setVisible("https".equals(savedSettings.getProperty("protocol", "http")));

        // Host field
        HBox hostBox = new HBox(10);
        hostBox.setAlignment(Pos.CENTER_LEFT);
        Label hostLabel = new Label("Host:");
        hostLabel.setMinWidth(100);
        TextField hostField = new TextField();
        hostField.setText(savedSettings.getProperty("host", ""));
        hostField.setPromptText("Enter host:port (e.g., localhost:8086)");
        hostField.setPrefWidth(300);
        hostBox.getChildren().addAll(hostLabel, hostField);

        // Database field
        HBox dbBox = new HBox(10);
        dbBox.setAlignment(Pos.CENTER_LEFT);
        Label dbLabel = new Label("Database:");
        dbLabel.setMinWidth(100);
        TextField databaseField = new TextField();
        databaseField.setText(savedSettings.getProperty("database", ""));
        databaseField.setPromptText("Enter database name");
        databaseField.setPrefWidth(300);
        dbBox.getChildren().addAll(dbLabel, databaseField);

        // Token field
        HBox tokenBox = new HBox(10);
        tokenBox.setAlignment(Pos.CENTER_LEFT);
        Label tokenLabel = new Label("Token:");
        tokenLabel.setMinWidth(100);
        PasswordField tokenField = new PasswordField();
        tokenField.setPromptText("Enter your InfluxDB API token");
        tokenField.setPrefWidth(300);
        tokenBox.getChildren().addAll(tokenLabel, tokenField);
        
        // Authentication note
        Label authNote = new Label("⚠️ API Key only - OAuth/SAML not supported");
        authNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #FF6B35; -fx-font-style: italic;");
        
        // Settings persistence note
        Label settingsNote = new Label("💾 Connection details (except token) will be remembered for next time");
        settingsNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #666; -fx-font-style: italic;");

        // Test connection button
        Button testButton = new Button("Test Connection");
        testButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        testButton.setPrefWidth(120);
        testButton.setPrefHeight(35);

        // Connect button
        Button connectButton = new Button("Connect");
        connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        connectButton.setPrefWidth(120);
        connectButton.setPrefHeight(35);

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(testButton, connectButton);

        formBox.getChildren().addAll(protocolBox, sslBox, hostBox, dbBox, tokenBox, authNote, settingsNote, buttonBox);

        // Status label
        Label statusLabel = new Label("Enter your InfluxDB connection details");
        statusLabel.setStyle("-fx-font-weight: bold;");

        connectionLayout.getChildren().addAll(titleLabel, formBox, statusLabel, buttonBox);

        // Event handlers for connection testing and validation
        testButton.setOnAction(e -> {
            // Extract current form values for testing
            String testProtocol = protocolCombo.getValue();
            boolean testSkipSSL = skipSSLValidationCheck.isSelected();
            String testHost = hostField.getText().trim();
            String testDatabase = databaseField.getText().trim();
            String testToken = tokenField.getText().trim();
            
            // Validate that all required fields are filled
            if (testHost.isEmpty() || testDatabase.isEmpty() || testToken.isEmpty()) {
                statusLabel.setText("Please fill in all fields");
                statusLabel.setTextFill(Color.RED);
                return;
            }
            
            // Disable test button and show testing status
            testButton.setDisable(true);
            statusLabel.setText("Testing connection...");
            statusLabel.setTextFill(Color.BLUE);
            
            // Test connection asynchronously to keep UI responsive
            CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("Test connection: Testing " + testProtocol + "://" + testHost + " with database " + testDatabase);
                    String result = executeQueryHTTP(testProtocol, testHost, testToken, testDatabase, "SHOW MEASUREMENTS", testSkipSSL);
                    System.out.println("Test connection result: " + result.substring(0, Math.min(100, result.length())));
                    return result;
                } catch (Exception ex) {
                    System.err.println("Test connection exception: " + ex.getMessage());
                    ex.printStackTrace();
                    return "Error: " + ex.getMessage();
                }
            }).thenAcceptAsync(result -> {
                javafx.application.Platform.runLater(() -> {
                    testButton.setDisable(false);
                    System.out.println("Test connection UI update: " + result.substring(0, Math.min(100, result.length())));
                    if (result.startsWith("Error:") || result.startsWith("ERROR")) {
                        statusLabel.setText("Connection test failed!");
                        statusLabel.setTextFill(Color.RED);
                        // Show detailed error dialog for test connection
                        showConnectionErrorDialog("Test Connection Failed:\n\n" + result);
                    } else {
                        statusLabel.setText("Connection test successful! Click Connect to continue.");
                        statusLabel.setTextFill(Color.GREEN);
                    }
                });
            }).exceptionally(throwable -> {
                System.err.println("Test connection CompletableFuture exception: " + throwable.getMessage());
                throwable.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    testButton.setDisable(false);
                    statusLabel.setText("Connection test failed!");
                    statusLabel.setTextFill(Color.RED);
                    showConnectionErrorDialog("Test Connection Exception:\n\n" + throwable.getMessage());
                });
                return null;
            });
        });

        // Event handler for final connection and proceeding to main application
        connectButton.setOnAction(e -> {
            // Store connection details in instance variables
            this.protocol = protocolCombo.getValue();
            this.skipSSLValidation = skipSSLValidationCheck.isSelected();
            this.host = hostField.getText().trim();
            this.database = databaseField.getText().trim();
            this.token = tokenField.getText().trim();
            
            // Validate that all required fields are filled
            if (this.host.isEmpty() || this.database.isEmpty() || this.token.isEmpty()) {
                statusLabel.setText("Please fill in all fields");
                statusLabel.setTextFill(Color.RED);
                return;
            }
            
            // Validate connection before proceeding to main window
            connectButton.setDisable(true);
            statusLabel.setText("Validating connection...");
            statusLabel.setTextFill(Color.BLUE);
            
            // Validate connection asynchronously to keep UI responsive
            CompletableFuture.supplyAsync(() -> {
                try {
                    return executeQueryHTTP(this.protocol, this.host, this.token, this.database, "SHOW MEASUREMENTS", this.skipSSLValidation);
                } catch (Exception ex) {
                    return "Error: " + ex.getMessage();
                }
            }).thenAcceptAsync(result -> {
                javafx.application.Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    if (result.startsWith("Error:") || result.startsWith("ERROR")) {
                        // Connection failed - show detailed error
                        statusLabel.setText("Connection validation failed!");
                        statusLabel.setTextFill(Color.RED);
                        showConnectionErrorDialog(result);
                    } else {
                        // Connection successful - save settings and proceed to main window
                        statusLabel.setText("Connection validated successfully!");
                        statusLabel.setTextFill(Color.GREEN);
                        
                        // Save settings for next time (excluding token for security)
                        saveSettings(this.protocol, this.host, this.database, this.skipSSLValidation);
                        
                        connectionStage.close();
                    }
                });
            }).exceptionally(throwable -> {
                javafx.application.Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    statusLabel.setText("Connection validation failed!");
                    statusLabel.setTextFill(Color.RED);
                    showConnectionErrorDialog("Error: " + throwable.getMessage());
                });
                return null;
            });
        });

        Scene connectionScene = new Scene(connectionLayout, 500, 500);
        connectionStage.setScene(connectionScene);
        connectionStage.showAndWait();

        return this.host != null && this.database != null && this.token != null;
    }

    /**
     * Creates and displays the main application window
     * Sets up the complete UI layout with menu bar, query section, and results area
     */
    private void createMainWindow() {
        // Set window title with connection information and beta version
        mainStage.setTitle("InfluxDB Query IDE v1.0 Beta - " + host + "/" + database);
        
        // Ensure main window has the application icon for consistency
        setApplicationIcon(mainStage);

        // Create the main layout
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // Menu Bar
        MenuBar menuBar = createMenuBar();

        // Title and connection info
        HBox headerBox = createHeaderBox();

        // Query section - compact layout
        VBox queryBox = createCompactQuerySection();
        
        // Add query toggle checkbox to header so it's always accessible
        CheckBox queryToggleCheckBox = new CheckBox("Show Query Section");
        queryToggleCheckBox.setSelected(true); // Initially selected (query section visible)
        queryToggleCheckBox.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
        queryToggleCheckBox.setTooltip(new Tooltip("Check to show query section, uncheck to hide for more results space"));
        queryToggleCheckBox.setOnAction(e -> toggleQuerySectionWithCheckBox(queryBox, queryToggleCheckBox));
        headerBox.getChildren().add(queryToggleCheckBox);
        
        // Results section - takes most of the space
        VBox resultsBox = createResultsSection();
        
        // Control buttons
        HBox buttonBox = createButtonSection();
        
        // Status section with connection indicators at bottom corners
        HBox statusBox = createStatusSectionWithConnectionInfo();

        // Add all sections to main layout
        mainLayout.getChildren().addAll(
            menuBar,
            headerBox,
            queryBox,
            resultsBox,
            buttonBox,
            statusBox
        );

        // Set up event handlers
        setupEventHandlers();

        // Create scene and show stage
        Scene scene = new Scene(mainLayout, 1000, 800);
        mainStage.setScene(scene);
        mainStage.setMinWidth(900);
        mainStage.setMinHeight(700);
        
        // Maximize the main window for better query and results viewing
        mainStage.setMaximized(true);
        
        mainStage.show();
    }

    /**
     * Creates the header section with title, connection info, SSL warning, and status
     */
    private HBox createHeaderBox() {
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("InfluxDB Query IDE");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        Label connectionInfo = new Label("Connected to: " + protocol + "://" + host + " | Database: " + database);
        connectionInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        
        // Add SSL warning if validation is skipped
        HBox sslWarningBox = new HBox(5);
        sslWarningBox.setAlignment(Pos.CENTER_LEFT);
        sslWarningBox.setVisible(protocol.equalsIgnoreCase("https") && skipSSLValidation);
        
        Label warningIcon = new Label("⚠️");
        warningIcon.setStyle("-fx-font-size: 14px;");
        
        Label warningText = new Label("SSL Certificate Validation Disabled");
        warningText.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF9800; -fx-font-style: italic;");
        
        sslWarningBox.getChildren().addAll(warningIcon, warningText);
        
        headerBox.getChildren().addAll(titleLabel, connectionInfo, sslWarningBox);
        return headerBox;
    }

    /**
     * Creates a compact query input section with horizontal layout
     * Places query text area and execute button side by side for space efficiency
     * Includes a toggle button to hide/show the entire section
     */
    private VBox createCompactQuerySection() {
        // Create main container with styling and padding
        VBox queryBox = new VBox(10);
        queryBox.setPadding(new Insets(15));
        queryBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Header row with section label only (toggle button moved to main header)
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        // Section header label
        Label sectionLabel = new Label("Query");
        sectionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // Make section label expand to fill available space
        HBox.setHgrow(sectionLabel, Priority.ALWAYS);
        headerRow.getChildren().add(sectionLabel);

        // Horizontal layout for query text area and execute button
        HBox queryRow = new HBox(15);
        queryRow.setAlignment(Pos.CENTER_LEFT);
        
        // Query input text area with helpful examples and monospace font
        queryArea = new TextArea();
        queryArea.setPromptText("Enter your InfluxDB query here...\nExamples:\nSELECT * FROM sensor_f\nSHOW TABLES\nSHOW MEASUREMENTS");
        queryArea.setPrefRowCount(2); // Start with 2 rows
        queryArea.setWrapText(true);
        queryArea.setFont(Font.font("Consolas", 12));
        HBox.setHgrow(queryArea, Priority.ALWAYS); // Make text area expand to fill available space
        
        // Set initial height and make resizable
        queryArea.setPrefHeight(40); // Initial height for 2 rows
        queryArea.setMinHeight(40); // Minimum height for 2 rows
        queryArea.setMaxHeight(120); // Maximum height for 6 rows (6 * 20px per row)
        
        // Note: TextArea automatically shows scrollbar when content exceeds visible area
        // Users can now manually resize the query area by dragging the bottom edge
        
        // Execute button with green styling and fixed height
        executeButton = new Button("Execute");
        executeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        executeButton.setPrefWidth(100);
        executeButton.setPrefHeight(40); // Fixed height to match initial query area height
        executeButton.setAlignment(Pos.CENTER);
        
        // Note: Button height is now fixed, users can resize query area independently
        
        // Add components to horizontal row
        queryRow.getChildren().addAll(queryArea, executeButton);

        // Add header row and query row to main container
        queryBox.getChildren().addAll(headerRow, queryRow);
        return queryBox;
    }

    /**
     * Creates the results display section with tabbed interface
     * Includes table view, raw JSON view, filtering, and export functionality
     */
    private VBox createResultsSection() {
        // Main results container that grows to fill available space
        VBox resultsBox = new VBox(15);
        resultsBox.setPadding(new Insets(20));
        VBox.setVgrow(resultsBox, Priority.ALWAYS);
        
        // Header section with title and export button
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        // Section title label
        Label sectionLabel = new Label("Results");
        sectionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // Export to CSV button with blue styling
        Button exportButton = new Button("Export to CSV");
        exportButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        exportButton.setPrefWidth(120);
        exportButton.setPrefHeight(30);
        exportButton.setOnAction(e -> exportToCSV());
        
        // Make section label expand to fill available space
        HBox.setHgrow(sectionLabel, Priority.ALWAYS);
        headerBox.getChildren().addAll(sectionLabel, exportButton);
        
        // Record count and global filter
        HBox controlsBox = new HBox(20);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        
        // Record count
        recordCountLabel = new Label("Records: 0");
        recordCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        
        // Global filter field
        Label filterLabel = new Label("Global Filter:");
        filterField = new TextField();
        filterField.setPromptText("Type to filter across all columns...");
        filterField.setPrefWidth(250);
        filterField.textProperty().addListener((observable, oldValue, newValue) -> applyGlobalFilter());
        
        // Clear filters button
        Button clearFiltersButton = new Button("Clear All Filters");
        clearFiltersButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        clearFiltersButton.setOnAction(e -> clearAllFilters());
        
        controlsBox.getChildren().addAll(
            recordCountLabel, 
            filterLabel, filterField,
            clearFiltersButton
        );
        
        // Create tab pane for results
        resultsTabPane = new TabPane();
        
        // Table view tab
        Tab tableTab = new Tab("Table View");
        tableTab.setClosable(false);
        resultsTable = new TableView<>();
        resultsTable.setPlaceholder(new Label("Query results will appear here in table format..."));
        tableTab.setContent(resultsTable);
        
        // Raw JSON tab
        Tab rawTab = new Tab("Raw JSON");
        rawTab.setClosable(false);
        rawResultArea = new TextArea();
        rawResultArea.setEditable(false);
        rawResultArea.setPromptText("Raw JSON response will appear here...");
        rawResultArea.setPrefRowCount(15);
        rawResultArea.setWrapText(true);
        rawResultArea.setFont(Font.font("Consolas", 11));
        rawTab.setContent(rawResultArea);
        
        resultsTabPane.getTabs().addAll(tableTab, rawTab);
        
        // Bottom record count display
        HBox bottomRecordCountBox = new HBox(10);
        bottomRecordCountBox.setAlignment(Pos.CENTER_LEFT);
        bottomRecordCountBox.setPadding(new Insets(10, 0, 0, 0));
        bottomRecordCountBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-border-style: solid;");
        
        Label bottomRecordLabel = new Label("Query Results: ");
        bottomRecordLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        
        Label bottomRecordCount = new Label("0 records");
        bottomRecordCount.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
        bottomRecordCount.setId("bottomRecordCount"); // For updating later
        
        bottomRecordCountBox.getChildren().addAll(bottomRecordLabel, bottomRecordCount);
        
        resultsBox.getChildren().addAll(headerBox, controlsBox, resultsTabPane, bottomRecordCountBox);
        return resultsBox;
    }

    private HBox createButtonSection() {
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        clearButton = new Button("Clear Results");
        clearButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        clearButton.setPrefWidth(120);
        clearButton.setPrefHeight(35);

        buttonBox.getChildren().add(clearButton);
        return buttonBox;
    }

    private HBox createStatusSection() {
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-weight: bold;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);

        statusBox.getChildren().addAll(statusLabel, progressIndicator);
        return statusBox;
    }

    /**
     * Creates the status section with connection indicators at bottom corners
     * Left: Connection validation status, Right: Main status and progress
     */
    private HBox createStatusSectionWithConnectionInfo() {
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        // Left side: Connection validation status
        HBox leftStatusBox = new HBox(5);
        leftStatusBox.setAlignment(Pos.CENTER_LEFT);
        
        Label statusIcon = new Label("✅");
        statusIcon.setStyle("-fx-font-size: 14px;");
        
        Label statusText = new Label("Connection Validated");
        statusText.setStyle("-fx-font-size: 11px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        
        leftStatusBox.getChildren().addAll(statusIcon, statusText);
        
        // Center: Empty space (grows to fill available space)
        HBox centerStatusBox = new HBox(10);
        centerStatusBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerStatusBox, Priority.ALWAYS);
        
        // Right side: Main status and progress (moved to right)
        HBox rightStatusBox = new HBox(10);
        rightStatusBox.setAlignment(Pos.CENTER_RIGHT);
        
        // Add SSL warning if applicable
        if (protocol.equalsIgnoreCase("https") && skipSSLValidation) {
            Label warningIcon = new Label("⚠️");
            warningIcon.setStyle("-fx-font-size: 14px;");
            
            Label warningText = new Label("SSL Validation Disabled");
            warningText.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF9800; -fx-font-style: italic;");
            
            rightStatusBox.getChildren().addAll(warningIcon, warningText);
        }
        
        // Add main status and progress to right side
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-weight: bold;");
        
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        
        rightStatusBox.getChildren().addAll(statusLabel, progressIndicator);
        
        statusBox.getChildren().addAll(leftStatusBox, centerStatusBox, rightStatusBox);
        return statusBox;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // Database Menu
        Menu databaseMenu = new Menu("Database");
        
        MenuItem showTablesItem = new MenuItem("Show Measurements");
        showTablesItem.setOnAction(e -> {
            queryArea.setText("SHOW MEASUREMENTS");
            executeQuery();
        });
        
        databaseMenu.getItems().add(showTablesItem);
        
        // View Menu
        Menu viewMenu = new Menu("View");
        
        MenuItem toggleMaximizeItem = new MenuItem("Toggle Maximize");
        toggleMaximizeItem.setOnAction(e -> toggleMaximize());
        
        viewMenu.getItems().add(toggleMaximizeItem);
        
        // Help Menu
        Menu helpMenu = new Menu("Help");
        
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(databaseMenu, viewMenu, helpMenu);
        return menuBar;
    }
    
    /**
     * Toggles between maximized and normal window mode
     * Provides users with flexibility to choose their preferred window size
     */
    private void toggleMaximize() {
        if (mainStage.isMaximized()) {
            mainStage.setMaximized(false);
            // Set a reasonable default size when un-maximizing
            mainStage.setWidth(1200);
            mainStage.setHeight(900);
            // Center the window on screen
            mainStage.centerOnScreen();
        } else {
            mainStage.setMaximized(true);
        }
    }

    private void showConnectionErrorDialog(String errorDetails) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Validation Failed");
        alert.setHeaderText("Unable to connect to InfluxDB");
        
        // Create detailed error content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label errorLabel = new Label("Connection failed with the following error:");
        errorLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea errorArea = new TextArea(errorDetails);
        errorArea.setEditable(false);
        errorArea.setPrefRowCount(5);
        errorArea.setWrapText(true);
        errorArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 11;");
        
        Label helpLabel = new Label("Please check:\n" +
                                   "• Host address and port are correct\n" +
                                   "• API token is valid and not expired\n" +
                                   "• Database name exists\n" +
                                   "• Network connectivity to the server\n" +
                                   "• For HTTPS: Try enabling 'Skip SSL Certificate Validation' if using self-signed certificates");
        helpLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
        
        content.getChildren().addAll(errorLabel, errorArea, helpLabel);
        alert.getDialogPane().setContent(content);
        
        // Make dialog resizable
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(400);
        
        alert.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About InfluxDB IDE");
        alert.setHeaderText("InfluxDB Query IDE v1.0 Beta");
        alert.setContentText("A JavaFX application for querying InfluxDB databases.\n\n" +
                           "Features:\n" +
                           "• HTTP-based InfluxDB queries\n" +
                           "• Table and JSON result views\n" +
                           "• Database exploration tools\n" +
                           "• User-friendly interface\n" +
                           "• CSV export functionality\n\n" +
                           "Version: 1.0 Beta\n" +
                           "Author: Sreeram C Machavaram");
        alert.showAndWait();
    }

    private void setupEventHandlers() {
        // Set up event handlers for buttons
        executeButton.setOnAction(e -> executeQuery());
        clearButton.setOnAction(e -> clearResults());
        
        // Set up drag and drop for the results table
        setupDragAndDrop();
    }

    /**
     * Sets up drag and drop functionality between results table and query area
     * Allows users to drag table names from results into query text for convenience
     */
    private void setupDragAndDrop() {
        // Enable drag detection on the results table
        resultsTable.setOnDragDetected(event -> {
            // Get the currently selected row from the table
            ObservableList<String> selectedRow = resultsTable.getSelectionModel().getSelectedItem();
            if (selectedRow != null && !selectedRow.isEmpty()) {
                // Extract the first column value (usually table/measurement name) for dragging
                String dragContent = selectedRow.get(0);
                
                // Create clipboard content with the dragged text
                ClipboardContent content = new ClipboardContent();
                content.putString(dragContent);
                
                // Start drag and drop operation with COPY transfer mode
                Dragboard db = resultsTable.startDragAndDrop(TransferMode.COPY);
                db.setContent(content);
                
                // Consume the event to prevent further processing
                event.consume();
            }
        });
        
        // Enable drop on the query text area
        queryArea.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        
        queryArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasString()) {
                String draggedText = db.getString();
                
                // Get current cursor position in the text area
                int caretPosition = queryArea.getCaretPosition();
                String currentText = queryArea.getText();
                
                // Insert the dragged text at cursor position
                String newText = currentText.substring(0, caretPosition) + 
                               draggedText + 
                               currentText.substring(caretPosition);
                
                queryArea.setText(newText);
                
                // Set cursor position after the inserted text
                queryArea.positionCaret(caretPosition + draggedText.length());
                
                success = true;
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
        
        // Add visual feedback for drag over
        queryArea.setOnDragEntered(event -> {
            if (event.getDragboard().hasString()) {
                queryArea.setStyle("-fx-border-color: #2196F3; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
            }
        });
        
        queryArea.setOnDragExited(event -> {
            queryArea.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        });
    }

    /**
     * Executes the InfluxDB query entered by the user
     * Runs query asynchronously to keep UI responsive and updates results display
     */
    private void executeQuery() {
        // Get and trim the query text from the input area
        String query = queryArea.getText().trim();

        // Validate that a query was entered
        if (query.isEmpty()) {
            showAlert("Input Error", "Please enter a query.");
            return;
        }

        // Update UI state to show query is executing
        executeButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Executing query...");
        
        // Clear previous results from both table and raw JSON views
        rawResultArea.clear();
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();

        // Execute query asynchronously to prevent UI freezing
        CompletableFuture.supplyAsync(() -> {
            try {
                // Update status to show query is being processed
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Connecting to InfluxDB...");
                });
                
                return executeQueryHTTP(protocol, host, token, database, query, skipSSLValidation);
            } catch (Exception ex) {
                return "Error: " + ex.getMessage();
            }
        }).thenAcceptAsync(result -> {
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                // Update raw JSON view
                rawResultArea.setText(result);
                
                // Try to parse and display in table format
                try {
                    displayResultsInTable(result);
                } catch (Exception e) {
                    // If table parsing fails, just show raw result
                    System.err.println("Failed to parse results for table: " + e.getMessage());
                }
                
                executeButton.setDisable(false);
                progressIndicator.setVisible(false);
                statusLabel.setText("Query completed");
            });
        }).exceptionally(throwable -> {
            // Handle any exceptions that occur during execution
            javafx.application.Platform.runLater(() -> {
                executeButton.setDisable(false);
                progressIndicator.setVisible(false);
                statusLabel.setText("Query failed");
                
                // Show error in raw results area
                rawResultArea.setText("Query execution failed: " + throwable.getMessage());
                
                // Clear table
                resultsTable.getColumns().clear();
                resultsTable.getItems().clear();
                
                // Show error dialog
                showAlert("Query Error", "Failed to execute query: " + throwable.getMessage());
            });
            return null;
        });
    }

    /**
     * Executes an InfluxDB query using HTTP GET request
     * Handles both HTTP and HTTPS protocols with optional SSL validation bypass
     * Returns the JSON response as a string
     */
    private String executeQueryHTTP(String protocol, String host, String token, String database, String query, boolean skipSSLValidation) throws Exception {
        // Construct the query endpoint URL
        String urlString = protocol + "://" + host + "/query";
        
        // Build query parameters using InfluxDB v1 API format
        // p=token, db=database, q=query
        String params = String.format("p=%s&db=%s&q=%s",
            URLEncoder.encode(token, StandardCharsets.UTF_8),
            URLEncoder.encode(database, StandardCharsets.UTF_8),
            URLEncoder.encode(query, StandardCharsets.UTF_8)
        );
        
        // Create full URL with query parameters
        URL url = new URL(urlString + "?" + params);
        
        // Debug logging for troubleshooting
        System.out.println("HTTP Request: " + url);
        System.out.println("Token parameter: p=" + token.substring(0, Math.min(20, token.length())) + "...");
        System.out.println("Database parameter: db=" + database);
        System.out.println("Query parameter: q=" + query);
        
        // Open HTTP connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Configure HTTP request properties and timeouts
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "InfluxDB-IDE/1.0");
        connection.setConnectTimeout(10000); // 10 second connection timeout
        connection.setReadTimeout(10000);    // 10 second read timeout
        
        // Handle HTTPS connections
        if ("https".equalsIgnoreCase(protocol)) {
            System.out.println("Using HTTPS connection with SSL/TLS");
            if (skipSSLValidation) {
                System.out.println("SSL Certificate validation will be skipped");
                // Create a trust manager that trusts all certificates
                try {
                    // Set system properties for SSL certificate validation bypass
                    System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
                    if (skipSSLValidation) {
                        System.setProperty("javax.net.ssl.trustStore", "");
                        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
                        System.setProperty("javax.net.ssl.trustStorePassword", "");
                        System.setProperty("javax.net.ssl.keyStore", "");
                        System.setProperty("javax.net.ssl.keyStorePassword", "");
                        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
                        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
                        System.setProperty("com.sun.security.ssl.allowUnsafeRenegotiation", "true");
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Could not configure SSL bypass: " + e.getMessage());
                }
            }
        }
        
        // Debug authentication
        System.out.println("Using token: " + token.substring(0, Math.min(20, token.length())) + "...");
        System.out.println("Database: " + database);
        
        try {
            System.out.println("Connecting to: " + url);
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read successful response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                String result = response.toString();
                System.out.println("Success response length: " + result.length());
                return result;
            } else {
                // Read error response
                StringBuilder response = new StringBuilder();
                try {
                    // Check if error stream exists
                    if (connection.getErrorStream() != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                    } else {
                        // No error stream, create generic message
                        response.append("HTTP ").append(responseCode).append(" Error");
                        if (responseCode == 401) {
                            response.append(" - Unauthorized. Check your API token.");
                        } else if (responseCode == 403) {
                            response.append(" - Forbidden. Check your permissions.");
                        } else if (responseCode == 404) {
                            response.append(" - Not Found. Check the endpoint URL.");
                        } else if (responseCode >= 500) {
                            response.append(" - Server Error. Try again later.");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error reading error stream: " + e.getMessage());
                    response.append("HTTP ").append(responseCode).append(" Error - Unable to read error details");
                }
                
                String errorResult = "ERROR " + responseCode + ": " + response.toString();
                System.out.println("Error response: " + errorResult);
                return errorResult;
            }
            
        } catch (Exception e) {
            System.err.println("HTTP connection exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Parses JSON response and displays results in a table format
     * Handles InfluxDB v1 API response structure with series and values
     * Creates dynamic columns and populates data with Excel-like functionality
     */
    private void displayResultsInTable(String jsonResult) {
        try {
            // Check if response indicates an HTTP error
            if (jsonResult.startsWith("ERROR")) {
                System.err.println("HTTP Error response: " + jsonResult);
                // Display error in raw results area for debugging
                rawResultArea.setText("HTTP Error: " + jsonResult);
                // Clear table since there's no valid data to display
                resultsTable.getColumns().clear();
                resultsTable.getItems().clear();
                return;
            }
            
            // Debug logging - show first 200 characters of response
            System.out.println("Raw response: " + jsonResult.substring(0, Math.min(200, jsonResult.length())));
            
            // Parse the JSON response string into JSONObject
            JSONObject json = new JSONObject(jsonResult);
            JSONArray results = json.getJSONArray("results");
            
            if (results.length() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                
                if (firstResult.has("series")) {
                    JSONArray series = firstResult.getJSONArray("series");
                    
                    if (series.length() > 0) {
                        JSONObject firstSeries = series.getJSONObject(0);
                        
                        // Check if series has error
                        if (firstSeries.has("error")) {
                            String errorMsg = firstSeries.getString("error");
                            System.err.println("Series error: " + errorMsg);
                            rawResultArea.setText("Query Error: " + errorMsg);
                            resultsTable.getColumns().clear();
                            resultsTable.getItems().clear();
                            return;
                        }
                        
                        JSONArray columns = firstSeries.getJSONArray("columns");
                        JSONArray values = firstSeries.getJSONArray("values");
                        
                        // Clear existing table
                        resultsTable.getColumns().clear();
                        resultsTable.getItems().clear();
                        
                        // Create columns dynamically with Excel-like headers
                        for (int i = 0; i < columns.length(); i++) {
                            final int colIndex = i;
                            String columnName = columns.getString(i);
                            
                            // Create column header with sorting and filtering
                            VBox headerBox = createExcelLikeHeader(columnName, colIndex);
                            
                            TableColumn<ObservableList<String>, String> column = new TableColumn<>();
                            column.setGraphic(headerBox);
                            column.setCellValueFactory(data -> {
                                ObservableList<String> row = data.getValue();
                                if (row != null && colIndex < row.size()) {
                                    return new SimpleStringProperty(row.get(colIndex));
                                }
                                return new SimpleStringProperty("");
                            });
                            
                            // Make columns resizable
                            column.setPrefWidth(180);
                            column.setResizable(true);
                            
                            resultsTable.getColumns().add(column);
                        }
                        
                        // Populate data
                        allResultsData = FXCollections.observableArrayList();
                        for (int i = 0; i < values.length(); i++) {
                            JSONArray row = values.getJSONArray(i);
                            ObservableList<String> rowData = FXCollections.observableArrayList();
                            
                            for (int j = 0; j < row.length(); j++) {
                                Object value = row.get(j);
                                rowData.add(value != null ? value.toString() : "");
                            }
                            
                            allResultsData.add(rowData);
                        }
                        
                        // Set data to table
                        resultsTable.setItems(allResultsData);
                        
                        // Update record count
                        updateRecordCount();
                        
                        // Switch to table tab
                        resultsTabPane.getSelectionModel().select(0);
                    } else {
                        // No series - this can happen with some queries like SHOW MEASUREMENTS
                        System.out.println("No series in result - query may have returned no data");
                        rawResultArea.setText("Query executed successfully but returned no data.\n\nRaw response:\n" + jsonResult);
                        resultsTable.getColumns().clear();
                        resultsTable.getItems().clear();
                    }
                } else {
                    // No series field - check if there's an error message
                    if (firstResult.has("error")) {
                        String errorMsg = firstResult.getString("error");
                        System.err.println("Result error: " + errorMsg);
                        rawResultArea.setText("Query Error: " + errorMsg);
                        resultsTable.getColumns().clear();
                        resultsTable.getItems().clear();
                    } else {
                        System.out.println("No series field in result");
                        rawResultArea.setText("Query executed but no data structure found.\n\nRaw response:\n" + jsonResult);
                        resultsTable.getColumns().clear();
                        resultsTable.getItems().clear();
                    }
                }
            } else {
                System.out.println("No results in response");
                rawResultArea.setText("No results in response.\n\nRaw response:\n" + jsonResult);
                resultsTable.getColumns().clear();
                resultsTable.getItems().clear();
            }
        } catch (JSONException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            System.err.println("Raw response that caused error: " + jsonResult);
            rawResultArea.setText("JSON Parsing Error: " + e.getMessage() + "\n\nRaw response:\n" + jsonResult);
            resultsTable.getColumns().clear();
            resultsTable.getItems().clear();
        }
    }
    
    /**
     * Creates an Excel-like column header with sorting and filtering controls
     * Each header contains the column name and three control buttons:
     * - Ascending sort (↑), Descending sort (↓), and Filter (🔍)
     */
    private VBox createExcelLikeHeader(String columnName, int columnIndex) {
        // Create vertical container for header content
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER);
        
        // Column name label with bold Arial font
        Label nameLabel = new Label(columnName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLabel.setAlignment(Pos.CENTER);
        
        // Horizontal row for control buttons
        HBox controlsRow = new HBox(5);
        controlsRow.setAlignment(Pos.CENTER);
        
        // Ascending sort button with up arrow and tooltip
        Button sortAscButton = new Button("↑");
        sortAscButton.setStyle("-fx-background-color: #E0E0E0; -fx-font-size: 10; -fx-min-width: 20; -fx-min-height: 20;");
        sortAscButton.setTooltip(new Tooltip("Sort ascending"));
        sortAscButton.setOnAction(e -> sortColumn(columnIndex, true));
        
        // Descending sort button with down arrow and tooltip
        Button sortDescButton = new Button("↓");
        sortDescButton.setStyle("-fx-background-color: #E0E0E0; -fx-font-size: 10; -fx-min-width: 20; -fx-min-height: 20;");
        sortDescButton.setTooltip(new Tooltip("Sort descending"));
        sortDescButton.setOnAction(e -> sortColumn(columnIndex, false));
        
        // Filter button with magnifying glass icon and tooltip
        Button filterButton = new Button("🔍");
        filterButton.setStyle("-fx-background-color: #E0E0E0; -fx-font-size: 10; -fx-min-width: 20; -fx-min-height: 20;");
        filterButton.setTooltip(new Tooltip("Filter column"));
        filterButton.setOnAction(e -> showColumnFilter(columnIndex, columnName));
        
        // Add all control buttons to the horizontal row
        controlsRow.getChildren().addAll(sortAscButton, sortDescButton, filterButton);
        
        // Combine column name and controls in the header
        headerBox.getChildren().addAll(nameLabel, controlsRow);
        return headerBox;
    }
    
    private void updateRecordCount() {
        int totalRecords = allResultsData != null ? allResultsData.size() : 0;
        int filteredRecords = resultsTable.getItems().size();
        
        // Update the top record count label
        if (totalRecords == filteredRecords) {
            recordCountLabel.setText("Records: " + totalRecords);
        } else {
            recordCountLabel.setText("Records: " + filteredRecords + " of " + totalRecords);
        }
        
        // Update the bottom record count display
        Label bottomRecordCount = (Label) mainStage.getScene().lookup("#bottomRecordCount");
        if (bottomRecordCount != null) {
            if (totalRecords == 0) {
                bottomRecordCount.setText("No records");
            } else if (totalRecords == filteredRecords) {
                bottomRecordCount.setText(totalRecords + " records fetched");
            } else {
                bottomRecordCount.setText(filteredRecords + " of " + totalRecords + " records (filtered)");
            }
        }
    }
    
    private void sortColumn(int columnIndex, boolean ascending) {
        if (allResultsData == null) return;
        
        // Create a copy of current data for sorting
        ObservableList<ObservableList<String>> currentData = FXCollections.observableArrayList(resultsTable.getItems());
        
        // Sort the data
        currentData.sort((row1, row2) -> {
            if (row1.size() <= columnIndex || row2.size() <= columnIndex) {
                return 0;
            }
            
            String value1 = row1.get(columnIndex);
            String value2 = row2.get(columnIndex);
            
            int comparison = value1.compareToIgnoreCase(value2);
            return ascending ? comparison : -comparison;
        });
        
        // Update table with sorted data
        resultsTable.setItems(currentData);
        updateRecordCount();
    }
    
    private void showColumnFilter(int columnIndex, String columnName) {
        if (allResultsData == null) return;
        
        // Create filter dialog
        Stage filterStage = new Stage();
        filterStage.initModality(Modality.APPLICATION_MODAL);
        filterStage.setTitle("Filter Column: " + columnName);
        filterStage.setResizable(false);
        
        VBox filterLayout = new VBox(15);
        filterLayout.setPadding(new Insets(20));
        filterLayout.setAlignment(Pos.CENTER);
        
        // Filter options
        Label titleLabel = new Label("Filter Column: " + columnName);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // Filter type selection
        ComboBox<String> filterTypeCombo = new ComboBox<>();
        filterTypeCombo.getItems().addAll("Contains", "Starts with", "Ends with", "Equals", "Not equals");
        filterTypeCombo.setValue("Contains");
        
        // Filter value
        TextField filterValueField = new TextField();
        filterValueField.setPromptText("Enter filter value...");
        filterValueField.setPrefWidth(200);
        
        // Apply filter button
        Button applyButton = new Button("Apply Filter");
        applyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        applyButton.setOnAction(e -> {
            applyColumnFilter(columnIndex, filterTypeCombo.getValue(), filterValueField.getText());
            filterStage.close();
        });
        
        // Clear column filter button
        Button clearButton = new Button("Clear Column Filter");
        clearButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        clearButton.setOnAction(e -> {
            clearColumnFilter(columnIndex);
            filterStage.close();
        });
        
        // Cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> filterStage.close());
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(applyButton, clearButton, cancelButton);
        
        filterLayout.getChildren().addAll(titleLabel, filterTypeCombo, filterValueField, buttonBox);
        
        Scene filterScene = new Scene(filterLayout, 350, 250);
        filterStage.setScene(filterScene);
        filterStage.showAndWait();
    }
    
    private void applyColumnFilter(int columnIndex, String filterType, String filterValue) {
        if (allResultsData == null || filterValue.isEmpty()) return;
        
        // Apply the filter
        ObservableList<ObservableList<String>> filteredData = FXCollections.observableArrayList();
        
        for (ObservableList<String> row : allResultsData) {
            if (row.size() > columnIndex) {
                String cellValue = row.get(columnIndex).toLowerCase();
                String searchValue = filterValue.toLowerCase();
                
                boolean matches = false;
                switch (filterType) {
                    case "Contains":
                        matches = cellValue.contains(searchValue);
                        break;
                    case "Starts with":
                        matches = cellValue.startsWith(searchValue);
                        break;
                    case "Ends with":
                        matches = cellValue.endsWith(searchValue);
                        break;
                    case "Equals":
                        matches = cellValue.equals(searchValue);
                        break;
                    case "Not equals":
                        matches = !cellValue.equals(searchValue);
                        break;
                }
                
                if (matches) {
                    filteredData.add(row);
                }
            }
        }
        
        resultsTable.setItems(filteredData);
        updateRecordCount();
    }
    
    private void clearColumnFilter(int columnIndex) {
        // Reset to show all data (respecting global filter)
        if (filterField.getText().isEmpty()) {
            resultsTable.setItems(allResultsData);
        } else {
            applyGlobalFilter();
        }
        updateRecordCount();
    }
    
    private void applyGlobalFilter() {
        if (allResultsData == null) return;
        
        String filterText = filterField.getText().toLowerCase();
        
        if (filterText.isEmpty()) {
            // Show all data
            resultsTable.setItems(allResultsData);
        } else {
            // Filter data across all columns
            ObservableList<ObservableList<String>> filteredData = FXCollections.observableArrayList();
            
            for (ObservableList<String> row : allResultsData) {
                boolean matches = false;
                for (String cell : row) {
                    if (cell.toLowerCase().contains(filterText)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    filteredData.add(row);
                }
            }
            
            resultsTable.setItems(filteredData);
        }
        
        updateRecordCount();
    }
    
    private void clearAllFilters() {
        filterField.clear();
        resultsTable.setItems(allResultsData);
        updateRecordCount();
    }
    
    /**
     * Toggles the visibility and layout management of the query section
     * When hidden, provides more space for results viewing by removing it from layout
     * Updates button text to reflect current state
     */
    private void toggleQuerySection(VBox queryBox, Button toggleButton) {
        // Toggle both visibility and layout management of the query section
        boolean isVisible = queryBox.isVisible();
        queryBox.setVisible(!isVisible);
        queryBox.setManaged(!isVisible); // Remove from layout when hidden
        
        // Update button text and styling based on current state
        if (isVisible) {
            // Query section is now hidden - button shows "Show"
            toggleButton.setText("Show Query Section");
            toggleButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            toggleButton.setTooltip(new Tooltip("Click to show the query section"));
        } else {
            // Query section is now visible - button shows "Hide"
            toggleButton.setText("Hide Query Section");
            toggleButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
            toggleButton.setTooltip(new Tooltip("Click to hide the query section for more results space"));
        }
    }

    /**
     * Toggles the visibility and layout management of the query section using a checkbox
     * When unchecked, provides more space for results viewing by removing it from layout
     * Checkbox state directly reflects the visibility state
     */
    private void toggleQuerySectionWithCheckBox(VBox queryBox, CheckBox toggleCheckBox) {
        // Set visibility and layout management based on checkbox state
        boolean shouldShow = toggleCheckBox.isSelected();
        queryBox.setVisible(shouldShow);
        queryBox.setManaged(shouldShow); // Include in layout when checked, remove when unchecked
        
        // Update status label to reflect current state
        if (shouldShow) {
            statusLabel.setText("Query section visible");
        } else {
            statusLabel.setText("Query section hidden - more space for results");
        }
    }
    
    

    private void clearResults() {
        rawResultArea.clear();
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();
        
        // Reset filter controls
        filterField.clear();
        recordCountLabel.setText("Records: 0");
        allResultsData = null;
        
        // Reset bottom record count display
        Label bottomRecordCount = (Label) mainStage.getScene().lookup("#bottomRecordCount");
        if (bottomRecordCount != null) {
            bottomRecordCount.setText("No records");
        }
        
        statusLabel.setText("Results cleared");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Exports the current table data to a CSV file
     * Respects current filtering and sorting applied to the results
     * Shows file chooser dialog and handles CSV formatting with proper escaping
     * Uses multi-threading to keep UI responsive during export
     */
    private void exportToCSV() {
        try {
            // Check if there's data to export (respects current filtering)
            if (resultsTable.getItems().isEmpty()) {
                showAlert("Export Error", "No data to export. Please run a query first.");
                return;
            }
            
            // Create file chooser dialog for saving CSV file
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export to CSV");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            
            // Generate default filename based on current record count
            int recordCount = resultsTable.getItems().size();
            String fileName = "influxdb_export_" + recordCount + "_records.csv";
            fileChooser.setInitialFileName(fileName);
            
            // Show save dialog and get selected file
            File file = fileChooser.showSaveDialog(mainStage);
            if (file != null) {
                // Show progress indicator and update status
                progressIndicator.setVisible(true);
                statusLabel.setText("Exporting CSV...");
                
                // Export CSV data in background thread to keep UI responsive
                CompletableFuture.runAsync(() -> {
                    try {
                        // Write CSV data
                        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                            // Write header
                            StringBuilder header = new StringBuilder();
                            for (TableColumn<ObservableList<String>, ?> column : resultsTable.getColumns()) {
                                if (header.length() > 0) header.append(",");
                                header.append("\"").append(column.getText()).append("\"");
                            }
                            writer.println(header.toString());
                            
                            // Write data rows (current filtered/sorted data)
                            for (ObservableList<String> row : resultsTable.getItems()) {
                                StringBuilder csvRow = new StringBuilder();
                                for (String cell : row) {
                                    if (csvRow.length() > 0) csvRow.append(",");
                                    // Escape quotes and wrap in quotes if contains comma or newline
                                    String escapedCell = cell.replace("\"", "\"\"");
                                    if (escapedCell.contains(",") || escapedCell.contains("\n") || escapedCell.contains("\"")) {
                                        csvRow.append("\"").append(escapedCell).append("\"");
                                    } else {
                                        csvRow.append(escapedCell);
                                    }
                                }
                                writer.println(csvRow.toString());
                            }
                        }
                        
                        // Update UI on JavaFX thread after successful export
                        javafx.application.Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            
                            String exportMessage = "Data exported to CSV successfully: " + file.getName();
                            if (allResultsData != null && allResultsData.size() != recordCount) {
                                exportMessage += " (" + recordCount + " of " + allResultsData.size() + " records)";
                            }
                            statusLabel.setText(exportMessage);
                            
                            // Show success dialog
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Export Successful");
                            successAlert.setHeaderText("CSV Export Complete");
                            successAlert.setContentText("Data has been successfully exported to:\n" + file.getAbsolutePath() + 
                                "\n\nRecords exported: " + recordCount);
                            successAlert.showAndWait();
                        });
                        
                    } catch (Exception e) {
                        // Handle export errors on JavaFX thread
                        javafx.application.Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            statusLabel.setText("Export failed");
                            showAlert("Export Error", "Failed to export CSV: " + e.getMessage());
                        });
                    }
                });
                
            }
        } catch (Exception e) {
            showAlert("Export Error", "Failed to export CSV: " + e.getMessage());
        }
    }
    
    /**
     * Saves connection settings to a properties file in user home directory
     * Stores protocol, host, database, and SSL validation preference
     * Note: API token is NOT saved for security reasons
     */
    private void saveSettings(String protocol, String host, String database, boolean skipSSLValidation) {
        try {
            // Create settings directory in user home if it doesn't exist
            File settingsDir = new File(SETTINGS_DIR);
            if (!settingsDir.exists()) {
                settingsDir.mkdirs();
            }
            
            // Create properties object and populate with connection details
            Properties props = new Properties();
            props.setProperty("protocol", protocol);
            props.setProperty("host", host);
            props.setProperty("database", database);
            props.setProperty("skipSSLValidation", String.valueOf(skipSSLValidation));
            
            // Save properties to file with descriptive header comment
            try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
                props.store(out, "InfluxDB IDE Settings - Generated automatically");
                System.out.println("Settings saved to: " + SETTINGS_FILE);
            }
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }
    
    /**
     * Loads connection settings from properties file in user home directory
     * Returns default values if no settings file exists or loading fails
     * Defaults: HTTP protocol, empty host/database, SSL validation enabled
     */
    private Properties loadSettings() {
        // Initialize properties with sensible defaults
        Properties props = new Properties();
        props.setProperty("protocol", "http");
        props.setProperty("host", "");
        props.setProperty("database", "");
        props.setProperty("skipSSLValidation", "false");
        
        try {
            // Check if settings file exists in user home directory
            File settingsFile = new File(SETTINGS_FILE);
            if (settingsFile.exists()) {
                // Load existing settings from file
                try (FileInputStream in = new FileInputStream(settingsFile)) {
                    props.load(in);
                    System.out.println("Settings loaded from: " + SETTINGS_FILE);
                }
            } else {
                // No settings file found, will use defaults
                System.out.println("No settings file found, using defaults");
            }
        } catch (IOException e) {
            // Log error but continue with default values
            System.err.println("Failed to load settings: " + e.getMessage());
        }
        
        return props;
    }

    public static void main(String[] args) {
        launch(args);
    }
} 