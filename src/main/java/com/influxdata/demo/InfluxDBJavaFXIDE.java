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
import javafx.scene.image.Image;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;

public class InfluxDBJavaFXIDE extends Application {

    private String protocol = "http"; // Default to HTTP
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

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        
        // Set application icon
        setApplicationIcon(primaryStage);
        
        // Show connection dialog first
        if (!showConnectionDialog()) {
            System.exit(0);
        }
        
        // Create main application window
        createMainWindow();
    }
    
    private void setApplicationIcon(Stage stage) {
        try {
            // Try to load icon from resources
            Image appIcon = new Image(getClass().getResourceAsStream("/icons/app_icon.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            // Create a simple programmatic icon as fallback
            createProgrammaticIcon(stage);
        }
    }
    
    private void createProgrammaticIcon(Stage stage) {
        try {
            // Create a simple canvas-based icon
            Canvas canvas = new Canvas(32, 32);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            // Draw a simple database icon
            gc.setFill(Color.rgb(33, 150, 243)); // Blue background
            gc.fillRoundRect(4, 4, 24, 24, 8, 8);
            
            // Draw "IDB" text
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            gc.fillText("IDB", 8, 20);
            
            // Convert canvas to image
            WritableImage snapshot = canvas.snapshot(null, null);
            stage.getIcons().add(snapshot);
            
        } catch (Exception e) {
            // If all else fails, use system default
            System.out.println("Using system default icon");
        }
    }

    private boolean showConnectionDialog() {
        Stage connectionStage = new Stage();
        connectionStage.initModality(Modality.APPLICATION_MODAL);
        connectionStage.setTitle("InfluxDB Connection Setup");
        connectionStage.setResizable(false);
        
        // Set connection dialog icon
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
        protocolCombo.setValue("http");
        protocolCombo.setPrefWidth(100);
        protocolBox.getChildren().addAll(protocolLabel, protocolCombo);

        // Host field
        HBox hostBox = new HBox(10);
        hostBox.setAlignment(Pos.CENTER_LEFT);
        Label hostLabel = new Label("Host:");
        hostLabel.setMinWidth(100);
        TextField hostField = new TextField("172.187.233.15:8181");
        hostField.setPrefWidth(300);
        hostBox.getChildren().addAll(hostLabel, hostField);

        // Database field
        HBox dbBox = new HBox(10);
        dbBox.setAlignment(Pos.CENTER_LEFT);
        Label dbLabel = new Label("Database:");
        dbLabel.setMinWidth(100);
        TextField databaseField = new TextField("test");
        databaseField.setPrefWidth(300);
        dbBox.getChildren().addAll(dbLabel, databaseField);

        // Token field
        HBox tokenBox = new HBox(10);
        tokenBox.setAlignment(Pos.CENTER_LEFT);
        Label tokenLabel = new Label("Token:");
        tokenLabel.setMinWidth(100);
        PasswordField tokenField = new PasswordField();
        tokenField.setText("apiv3_a1PqQhJopy_7fAFCYvbU6Bj7b0tNrYuCdD_ZydtXXoEe_nqTReOB29OMFcZ7o_VBkPVSwK3o-ODnu5Gy4eeFfuQ");
        tokenField.setPrefWidth(300);
        tokenBox.getChildren().addAll(tokenLabel, tokenField);
        
        // Authentication note
        Label authNote = new Label("⚠️ API Key only - OAuth/SAML not supported");
        authNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #FF6B35; -fx-font-style: italic;");

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

        formBox.getChildren().addAll(protocolBox, hostBox, dbBox, tokenBox, authNote, buttonBox);

        // Status label
        Label statusLabel = new Label("Enter your InfluxDB connection details");
        statusLabel.setStyle("-fx-font-weight: bold;");

        connectionLayout.getChildren().addAll(titleLabel, formBox, statusLabel, buttonBox);

        // Event handlers
        testButton.setOnAction(e -> {
            String testProtocol = protocolCombo.getValue();
            String testHost = hostField.getText().trim();
            String testDatabase = databaseField.getText().trim();
            String testToken = tokenField.getText().trim();
            
            if (testHost.isEmpty() || testDatabase.isEmpty() || testToken.isEmpty()) {
                statusLabel.setText("Please fill in all fields");
                statusLabel.setTextFill(Color.RED);
                return;
            }
            
            testButton.setDisable(true);
            statusLabel.setText("Testing connection...");
            statusLabel.setTextFill(Color.BLUE);
            
            // Test connection
            CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("Test connection: Testing " + testProtocol + "://" + testHost + " with database " + testDatabase);
                    String result = executeQueryHTTP(testProtocol, testHost, testToken, testDatabase, "SHOW MEASUREMENTS");
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

        connectButton.setOnAction(e -> {
            this.protocol = protocolCombo.getValue();
            this.host = hostField.getText().trim();
            this.database = databaseField.getText().trim();
            this.token = tokenField.getText().trim();
            
            if (this.host.isEmpty() || this.database.isEmpty() || this.token.isEmpty()) {
                statusLabel.setText("Please fill in all fields");
                statusLabel.setTextFill(Color.RED);
                return;
            }
            
            // Validate connection before proceeding
            connectButton.setDisable(true);
            statusLabel.setText("Validating connection...");
            statusLabel.setTextFill(Color.BLUE);
            
            CompletableFuture.supplyAsync(() -> {
                try {
                    return executeQueryHTTP(this.protocol, this.host, this.token, this.database, "SHOW MEASUREMENTS");
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
                        // Connection successful - proceed to main window
                        statusLabel.setText("Connection validated successfully!");
                        statusLabel.setTextFill(Color.GREEN);
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

        Scene connectionScene = new Scene(connectionLayout, 500, 400);
        connectionStage.setScene(connectionScene);
        connectionStage.showAndWait();

        return this.host != null && this.database != null && this.token != null;
    }

    private void createMainWindow() {
        mainStage.setTitle("InfluxDB Query IDE v1.0 - " + host + "/" + database);
        
        // Ensure main window has the application icon
        setApplicationIcon(mainStage);

        // Create the main layout
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // Menu Bar
        MenuBar menuBar = createMenuBar();

        // Title and connection info
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("InfluxDB Query IDE");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        Label connectionInfo = new Label("Connected to: " + protocol + "://" + host + " | Database: " + database);
        connectionInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        
        // Add connection status indicator
        HBox connectionStatusBox = new HBox(10);
        connectionStatusBox.setAlignment(Pos.CENTER_LEFT);
        
        Label statusIcon = new Label("✅");
        statusIcon.setStyle("-fx-font-size: 16px;");
        
        Label statusText = new Label("Connection Validated");
        statusText.setStyle("-fx-font-size: 12px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        
        connectionStatusBox.getChildren().addAll(statusIcon, statusText);
        
        headerBox.getChildren().addAll(titleLabel, connectionInfo, connectionStatusBox);

        // Query section - compact layout
        VBox queryBox = createCompactQuerySection();
        
        // Results section - takes most of the space
        VBox resultsBox = createResultsSection();
        
        // Control buttons
        HBox buttonBox = createButtonSection();
        
        // Status section
        HBox statusBox = createStatusSection();

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
        mainStage.show();
    }

    private VBox createCompactQuerySection() {
        VBox queryBox = new VBox(10);
        queryBox.setPadding(new Insets(15));
        queryBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label sectionLabel = new Label("Query");
        sectionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // Horizontal layout for query and execute button
        HBox queryRow = new HBox(15);
        queryRow.setAlignment(Pos.CENTER_LEFT);
        
        queryArea = new TextArea();
        queryArea.setPromptText("Enter your InfluxDB query here...\nExamples:\nSELECT * FROM sensor_f\nSHOW TABLES\nSHOW MEASUREMENTS");
        queryArea.setPrefRowCount(4);
        queryArea.setWrapText(true);
        queryArea.setFont(Font.font("Consolas", 12));
        HBox.setHgrow(queryArea, Priority.ALWAYS);
        
        executeButton = new Button("Execute");
        executeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        executeButton.setPrefWidth(100);
        executeButton.setPrefHeight(80);
        executeButton.setAlignment(Pos.CENTER);
        
        queryRow.getChildren().addAll(queryArea, executeButton);

        queryBox.getChildren().addAll(sectionLabel, queryRow);
        return queryBox;
    }

    private VBox createResultsSection() {
        VBox resultsBox = new VBox(15);
        resultsBox.setPadding(new Insets(20));
        VBox.setVgrow(resultsBox, Priority.ALWAYS);
        
        // Header with title and export button
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label sectionLabel = new Label("Results");
        sectionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        Button exportButton = new Button("Export to CSV");
        exportButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        exportButton.setPrefWidth(120);
        exportButton.setPrefHeight(30);
        exportButton.setOnAction(e -> exportToCSV());
        
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
        
        resultsBox.getChildren().addAll(headerBox, controlsBox, resultsTabPane);
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
        
        // Help Menu
        Menu helpMenu = new Menu("Help");
        
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(databaseMenu, helpMenu);
        return menuBar;
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
                                   "• Network connectivity to the server");
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
        alert.setHeaderText("InfluxDB Query IDE");
        alert.setContentText("A JavaFX application for querying InfluxDB databases.\n\n" +
                           "Features:\n" +
                           "• HTTP-based InfluxDB queries\n" +
                           "• Table and JSON result views\n" +
                           "• Database exploration tools\n" +
                           "• User-friendly interface\n" +
                           "• CSV export functionality\n\n" +
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

    private void setupDragAndDrop() {
        // Enable drag and drop on the results table
        resultsTable.setOnDragDetected(event -> {
            // Get the selected row
            ObservableList<String> selectedRow = resultsTable.getSelectionModel().getSelectedItem();
            if (selectedRow != null && !selectedRow.isEmpty()) {
                // Create drag content with the first column value (usually table name)
                String dragContent = selectedRow.get(0);
                
                // Create clipboard content
                ClipboardContent content = new ClipboardContent();
                content.putString(dragContent);
                
                // Start drag and drop
                Dragboard db = resultsTable.startDragAndDrop(TransferMode.COPY);
                db.setContent(content);
                
                // Set drag view (optional visual feedback)
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

    private void executeQuery() {
        String query = queryArea.getText().trim();

        // Validate inputs
        if (query.isEmpty()) {
            showAlert("Input Error", "Please enter a query.");
            return;
        }

        // Update UI state
        executeButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Executing query...");
        rawResultArea.clear();
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();

        // Execute query asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                return executeQueryHTTP(protocol, host, token, database, query);
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
        });
    }

    private String executeQueryHTTP(String protocol, String host, String token, String database, String query) throws Exception {
        String urlString = protocol + "://" + host + "/query";
        
        // Build query parameters - try both methods
        String params = String.format("p=%s&db=%s&q=%s",
            URLEncoder.encode(token, StandardCharsets.UTF_8),
            URLEncoder.encode(database, StandardCharsets.UTF_8),
            URLEncoder.encode(query, StandardCharsets.UTF_8)
        );
        
        URL url = new URL(urlString + "?" + params);
        System.out.println("HTTP Request: " + url);
        System.out.println("Token parameter: p=" + token.substring(0, Math.min(20, token.length())) + "...");
        System.out.println("Database parameter: db=" + database);
        System.out.println("Query parameter: q=" + query);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set request method and headers
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "InfluxDB-IDE/1.0");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        
        // Handle HTTPS connections
        if ("https".equalsIgnoreCase(protocol)) {
            System.out.println("Using HTTPS connection with SSL/TLS");
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

    private void displayResultsInTable(String jsonResult) {
        try {
            // Check if response starts with ERROR
            if (jsonResult.startsWith("ERROR")) {
                System.err.println("HTTP Error response: " + jsonResult);
                // Show error in raw results area
                rawResultArea.setText("HTTP Error: " + jsonResult);
                // Clear table
                resultsTable.getColumns().clear();
                resultsTable.getItems().clear();
                return;
            }
            
            // Debug: log the response
            System.out.println("Raw response: " + jsonResult.substring(0, Math.min(200, jsonResult.length())));
            
            // Parse JSON response
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
    
    private VBox createExcelLikeHeader(String columnName, int columnIndex) {
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER);
        
        // Column name label
        Label nameLabel = new Label(columnName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        nameLabel.setAlignment(Pos.CENTER);
        
        // Controls row
        HBox controlsRow = new HBox(5);
        controlsRow.setAlignment(Pos.CENTER);
        
        // Sort buttons
        Button sortAscButton = new Button("↑");
        sortAscButton.setStyle("-fx-background-color: #E0E0E0; -fx-font-size: 10; -fx-min-width: 20; -fx-min-height: 20;");
        sortAscButton.setTooltip(new Tooltip("Sort ascending"));
        sortAscButton.setOnAction(e -> sortColumn(columnIndex, true));
        
        Button sortDescButton = new Button("↓");
        sortDescButton.setStyle("-fx-background-color: #E0E0E0; -fx-font-size: 10; -fx-min-width: 20; -fx-min-height: 20;");
        sortDescButton.setTooltip(new Tooltip("Sort descending"));
        sortDescButton.setOnAction(e -> sortColumn(columnIndex, false));
        
        // Filter button
        Button filterButton = new Button("🔍");
        filterButton.setStyle("-fx-background-color: #E0E0E0; -fx-font-size: 10; -fx-min-width: 20; -fx-min-height: 20;");
        filterButton.setTooltip(new Tooltip("Filter column"));
        filterButton.setOnAction(e -> showColumnFilter(columnIndex, columnName));
        
        controlsRow.getChildren().addAll(sortAscButton, sortDescButton, filterButton);
        
        headerBox.getChildren().addAll(nameLabel, controlsRow);
        return headerBox;
    }
    
    private void updateRecordCount() {
        int totalRecords = allResultsData != null ? allResultsData.size() : 0;
        int filteredRecords = resultsTable.getItems().size();
        
        if (totalRecords == filteredRecords) {
            recordCountLabel.setText("Records: " + totalRecords);
        } else {
            recordCountLabel.setText("Records: " + filteredRecords + " of " + totalRecords);
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
    
    

    private void clearResults() {
        rawResultArea.clear();
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();
        
        // Reset filter controls
        filterField.clear();
        recordCountLabel.setText("Records: 0");
        allResultsData = null;
        
        statusLabel.setText("Results cleared");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void exportToCSV() {
        try {
            // Get the current table data (respects filtering)
            if (resultsTable.getItems().isEmpty()) {
                showAlert("Export Error", "No data to export. Please run a query first.");
                return;
            }
            
            // Create file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export to CSV");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            
            // Set filename based on current data
            int recordCount = resultsTable.getItems().size();
            String fileName = "influxdb_export_" + recordCount + "_records.csv";
            fileChooser.setInitialFileName(fileName);
            
            File file = fileChooser.showSaveDialog(mainStage);
            if (file != null) {
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
                
            }
        } catch (Exception e) {
            showAlert("Export Error", "Failed to export CSV: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 