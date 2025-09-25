package com.influxdata.demo.ui;

import com.influxdata.demo.config.ApplicationConfig;
import com.influxdata.demo.config.UIConstants;
import com.influxdata.demo.model.ApiType;
import com.influxdata.demo.model.Protocol;
import com.influxdata.demo.model.QueryTimeout;
import com.influxdata.demo.service.SettingsService;
import com.influxdata.demo.service.TimezoneService;
import com.influxdata.demo.service.InfluxDBService;
import com.influxdata.demo.util.Log;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.concurrent.CompletableFuture;

/**
 * Connection dialog component for InfluxDB IDE
 * Handles connection setup and configuration
 */
public class ConnectionDialog {
    
    private final SettingsService settingsService;
    private final TimezoneService timezoneService;
    private final Stage parentStage;
    
    private ApplicationConfig config;
    private boolean connectionSuccessful = false;
    
    // UI Components
    private ComboBox<Protocol> protocolCombo;
    private CheckBox skipSSLValidationCheck;
    private ComboBox<ApiType> apiTypeCombo;
    private ComboBox<QueryTimeout> timeoutCombo;
    private TextField hostField;
    private TextField databaseField;
    private PasswordField tokenField;
    private CheckBox timezoneConversionCheck;
    private ComboBox<String> timezoneCombo;
    private Label statusLabel;
    private Button testButton;
    private Button connectButton;
    
    public ConnectionDialog(Stage parentStage) {
        this.parentStage = parentStage;
        this.settingsService = new SettingsService();
        this.timezoneService = new TimezoneService();
    }
    
    /**
     * Show the connection dialog
     * @return true if connection successful, false if cancelled
     */
    public boolean showDialog() {
        Log.uiInfo("Connection dialog opened");
        
        // Load saved settings
        config = settingsService.loadSettings();
        
        // Create dialog stage
        Stage dialogStage = createDialogStage();
        
        // Create dialog content
        VBox dialogContent = createDialogContent();
        
        // Setup event handlers
        setupEventHandlers(dialogStage);
        
        // Show dialog
        Scene scene = new Scene(dialogContent, UIConstants.CONNECTION_DIALOG_WIDTH, UIConstants.CONNECTION_DIALOG_HEIGHT);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
        
        if (connectionSuccessful) {
            Log.uiInfo("Connection dialog closed successfully");
        } else {
            Log.uiInfo("Connection dialog cancelled by user");
        }
        
        return connectionSuccessful;
    }
    
    /**
     * Create the dialog stage
     */
    private Stage createDialogStage() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("InfluxDB Connection Setup");
        stage.setResizable(false);
        stage.setMinHeight(UIConstants.CONNECTION_DIALOG_MIN_HEIGHT);
        stage.setMinWidth(UIConstants.CONNECTION_DIALOG_MIN_WIDTH);
        
        // Set application icon
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app_icon.png")));
        } catch (Exception e) {
            System.err.println("Failed to set dialog icon: " + e.getMessage());
        }
        
        return stage;
    }
    
    /**
     * Create the dialog content
     */
    private VBox createDialogContent() {
        VBox mainLayout = new VBox(UIConstants.LAYOUT_SPACING);
        mainLayout.setPadding(new Insets(UIConstants.DIALOG_PADDING));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle(UIConstants.BACKGROUND_STYLE);
        
        // Title
        HBox titleLabel = createTitleLabel();
        
        // Connection form
        VBox formBox = createConnectionForm();
        
        // Status label
        statusLabel = createStatusLabel();
        
        // Buttons
        HBox buttonBox = createButtonBox();
        
        mainLayout.getChildren().addAll(titleLabel, formBox, statusLabel, buttonBox);
        return mainLayout;
    }
    
    /**
     * Create title label with icon
     */
    private HBox createTitleLabel() {
        HBox titleBox = new HBox(UIConstants.LAYOUT_SPACING);
        titleBox.setAlignment(Pos.CENTER);
        
        // Add application icon
        ImageView titleIcon = new ImageView();
        try {
            Image appIcon = new Image(getClass().getResourceAsStream("/icons/app_icon.png"));
            titleIcon.setImage(appIcon);
            titleIcon.setFitWidth(32);
            titleIcon.setFitHeight(32);
        } catch (Exception e) {
            // Create a simple programmatic icon as fallback
            titleIcon.setFitWidth(32);
            titleIcon.setFitHeight(32);
            titleIcon.setStyle("-fx-background-color: #2196F3; -fx-background-radius: 4;");
        }
        
        Label label = new Label("InfluxDB Connection Setup");
        label.setFont(Font.font(UIConstants.DEFAULT_FONT_FAMILY, FontWeight.BOLD, UIConstants.TITLE_FONT_SIZE));
        label.setTextFill(Color.web(UIConstants.TITLE_COLOR));
        
        titleBox.getChildren().addAll(titleIcon, label);
        return titleBox;
    }
    
    /**
     * Create connection form
     */
    private VBox createConnectionForm() {
        VBox formBox = new VBox(UIConstants.FORM_SPACING);
        formBox.setPadding(new Insets(UIConstants.FORM_PADDING));
        formBox.setStyle(UIConstants.FORM_STYLE);
        
        // Protocol field
        HBox protocolBox = createProtocolField();
        
        // SSL validation field
        HBox sslBox = createSSLField();
        
        // API type field
        HBox apiTypeBox = createApiTypeField();
        
        // Timeout field
        HBox timeoutBox = createTimeoutField();
        
        // Host field
        HBox hostBox = createHostField();
        
        // Database field
        HBox databaseBox = createDatabaseField();
        
        // Token field
        HBox tokenBox = createTokenField();
        
        // Authentication note
        Label authNote = createAuthNote();
        
        // Timezone settings
        VBox timezoneBox = createTimezoneField();
        
        // Settings persistence note
        Label settingsNote = createSettingsNote();
        
        formBox.getChildren().addAll(protocolBox, sslBox, apiTypeBox, timeoutBox, 
                                   hostBox, databaseBox, tokenBox, authNote, timezoneBox, settingsNote);
        return formBox;
    }
    
    /**
     * Create protocol field
     */
    private HBox createProtocolField() {
        HBox box = new HBox(UIConstants.DEFAULT_SPACING);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("Protocol:");
        label.setMinWidth(UIConstants.LABEL_MIN_WIDTH);
        
        protocolCombo = new ComboBox<>();
        protocolCombo.getItems().addAll(Protocol.values());
        protocolCombo.setValue(config.getProtocol());
        protocolCombo.setPrefWidth(UIConstants.COMBO_BOX_PREF_WIDTH);
        
        box.getChildren().addAll(label, protocolCombo);
        return box;
    }
    
    /**
     * Create SSL validation field
     */
    private HBox createSSLField() {
        HBox box = new HBox(UIConstants.DEFAULT_SPACING);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("SSL Validation:");
        label.setMinWidth(UIConstants.LABEL_MIN_WIDTH);
        
        skipSSLValidationCheck = new CheckBox("Skip SSL validation (for self-signed certificates)");
        skipSSLValidationCheck.setSelected(config.isSkipSSLValidation());
        
        box.getChildren().addAll(label, skipSSLValidationCheck);
        return box;
    }
    
    /**
     * Create API type field
     */
    private HBox createApiTypeField() {
        HBox box = new HBox(UIConstants.DEFAULT_SPACING);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("API Type:");
        label.setMinWidth(UIConstants.LABEL_MIN_WIDTH);
        
        apiTypeCombo = new ComboBox<>();
        apiTypeCombo.getItems().addAll(ApiType.values());
        apiTypeCombo.setValue(config.getApiType());
        apiTypeCombo.setPrefWidth(UIConstants.COMBO_BOX_PREF_WIDTH);
        apiTypeCombo.setTooltip(new Tooltip(apiTypeCombo.getValue().getDescription()));
        
        box.getChildren().addAll(label, apiTypeCombo);
        return box;
    }
    
    /**
     * Create timeout field
     */
    private HBox createTimeoutField() {
        HBox box = new HBox(UIConstants.DEFAULT_SPACING);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("Query Timeout:");
        label.setMinWidth(UIConstants.LABEL_MIN_WIDTH);
        
        timeoutCombo = new ComboBox<>();
        timeoutCombo.getItems().addAll(QueryTimeout.values());
        timeoutCombo.setValue(config.getQueryTimeout());
        timeoutCombo.setPrefWidth(UIConstants.TIMEOUT_COMBO_WIDTH);
        timeoutCombo.setTooltip(new Tooltip("Maximum time to wait for query results"));
        
        box.getChildren().addAll(label, timeoutCombo);
        return box;
    }
    
    /**
     * Create host field
     */
    private HBox createHostField() {
        HBox box = new HBox(UIConstants.DEFAULT_SPACING);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("Host:");
        label.setMinWidth(UIConstants.LABEL_MIN_WIDTH);
        
        hostField = new TextField();
        hostField.setText(config.getHost());
        hostField.setPromptText("Enter host:port (e.g., localhost:8086)");
        hostField.setPrefWidth(UIConstants.TEXT_FIELD_PREF_WIDTH);
        
        box.getChildren().addAll(label, hostField);
        return box;
    }
    
    /**
     * Create database field
     */
    private HBox createDatabaseField() {
        HBox box = new HBox(UIConstants.DEFAULT_SPACING);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("Database:");
        label.setMinWidth(UIConstants.LABEL_MIN_WIDTH);
        
        databaseField = new TextField();
        databaseField.setText(config.getDatabase());
        databaseField.setPromptText("Enter database name");
        databaseField.setPrefWidth(UIConstants.TEXT_FIELD_PREF_WIDTH);
        
        box.getChildren().addAll(label, databaseField);
        return box;
    }
    
    /**
     * Create token field
     */
    private HBox createTokenField() {
        HBox box = new HBox(UIConstants.DEFAULT_SPACING);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("Token:");
        label.setMinWidth(UIConstants.LABEL_MIN_WIDTH);
        
        tokenField = new PasswordField();
        tokenField.setText(config.getToken());
        tokenField.setPromptText("Enter your InfluxDB API token");
        tokenField.setPrefWidth(UIConstants.TEXT_FIELD_PREF_WIDTH);
        
        box.getChildren().addAll(label, tokenField);
        return box;
    }
    
    /**
     * Create authentication note
     */
    private Label createAuthNote() {
        Label label = new Label("⚠️ API Key only - OAuth/SAML not supported");
        label.setStyle(UIConstants.WARNING_NOTE_STYLE);
        return label;
    }
    
    /**
     * Create timezone field
     */
    private VBox createTimezoneField() {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        
        // Timezone conversion checkbox
        timezoneConversionCheck = new CheckBox("Convert UTC timestamps to timezone");
        timezoneConversionCheck.setSelected(config.isTimezoneConversion());
        timezoneConversionCheck.setTooltip(new Tooltip("When enabled, Flight SQL and Java API timestamps will be converted from UTC to the selected timezone"));
        
        // Timezone selection combo box
        HBox timezoneSelectionBox = new HBox(UIConstants.DEFAULT_SPACING);
        timezoneSelectionBox.setAlignment(Pos.CENTER_LEFT);
        
        Label timezoneLabel = new Label("Timezone:");
        timezoneLabel.setMinWidth(UIConstants.LABEL_MIN_WIDTH);
        
        timezoneCombo = new ComboBox<>();
        timezoneCombo.getItems().addAll(timezoneService.getAvailableTimezones());
        timezoneCombo.setValue(config.getSelectedTimezone());
        timezoneCombo.setPrefWidth(UIConstants.TIMEZONE_COMBO_WIDTH);
        timezoneCombo.setTooltip(new Tooltip("Select the timezone for timestamp conversion. 'System Default' uses your computer's local timezone."));
        
        // Enable/disable timezone selection based on checkbox
        timezoneCombo.setDisable(!timezoneConversionCheck.isSelected());
        timezoneConversionCheck.setOnAction(e -> timezoneCombo.setDisable(!timezoneConversionCheck.isSelected()));
        
        timezoneSelectionBox.getChildren().addAll(timezoneLabel, timezoneCombo);
        box.getChildren().addAll(timezoneConversionCheck, timezoneSelectionBox);
        
        return box;
    }
    
    /**
     * Create settings note
     */
    private Label createSettingsNote() {
        Label label = new Label("💾 Connection details (except token) will be remembered for next time");
        label.setStyle(UIConstants.NOTE_STYLE);
        return label;
    }
    
    /**
     * Create status label
     */
    private Label createStatusLabel() {
        Label label = new Label("Enter your InfluxDB connection details");
        label.setStyle(UIConstants.TITLE_STYLE);
        return label;
    }
    
    /**
     * Create button box
     */
    private HBox createButtonBox() {
        HBox box = new HBox(UIConstants.BUTTON_SPACING);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 0, 0, 0));
        
        // Test connection button
        testButton = new Button("Test Connection");
        testButton.setStyle(UIConstants.PRIMARY_BUTTON_STYLE);
        testButton.setPrefWidth(UIConstants.BUTTON_PREF_WIDTH);
        testButton.setPrefHeight(UIConstants.BUTTON_PREF_HEIGHT);
        
        // Connect button
        connectButton = new Button("Connect");
        connectButton.setStyle(UIConstants.SUCCESS_BUTTON_STYLE);
        connectButton.setPrefWidth(UIConstants.BUTTON_PREF_WIDTH);
        connectButton.setPrefHeight(UIConstants.BUTTON_PREF_HEIGHT);
        
        box.getChildren().addAll(testButton, connectButton);
        return box;
    }
    
    /**
     * Setup event handlers
     */
    private void setupEventHandlers(Stage dialogStage) {
        // Test connection button
        testButton.setOnAction(e -> handleTestConnection());
        
        // Connect button
        connectButton.setOnAction(e -> handleConnect(dialogStage));
        
        // API type change handler
        apiTypeCombo.setOnAction(e -> {
            ApiType selected = apiTypeCombo.getValue();
            if (selected != null) {
                apiTypeCombo.setTooltip(new Tooltip(selected.getDescription()));
            }
        });
    }
    
    /**
     * Handle test connection button
     */
    private void handleTestConnection() {
        // Extract current form values
        ApplicationConfig testConfig = extractFormValues();
        
        // Validate configuration
        if (!testConfig.isValid()) {
            Log.connectionWarning("Connection test attempted with invalid configuration");
            updateStatus("Please fill in all fields", false);
            return;
        }
        
        Log.connectionInfo("Testing connection to " + testConfig.getHost() + "/" + testConfig.getDatabase() + " using " + testConfig.getApiType());
        
        // Disable test button and show testing status
        testButton.setDisable(true);
        updateStatus("Testing connection...", true);
        
        long startTime = System.currentTimeMillis();
        
        // Test connection asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                // Test the connection by attempting to connect
                InfluxDBService influxService = new InfluxDBService(testConfig);
                boolean connected = influxService.testConnection();
                
                if (connected) {
                    return "Connection test successful";
                } else {
                    return "Error: Failed to connect to InfluxDB. Please check your connection details.";
                }
            } catch (Exception ex) {
                return "Error: " + ex.getMessage();
            }
        }).thenAcceptAsync(result -> {
            javafx.application.Platform.runLater(() -> {
                long testTime = System.currentTimeMillis() - startTime;
                testButton.setDisable(false);
                
                if (result.startsWith("Error:")) {
                    Log.connectionError("Connection test failed after " + testTime + "ms: " + result);
                    updateStatus("Connection test failed! Please check your connection details.", false);
                    showErrorDialog("Test Connection Failed", result);
                } else {
                    Log.connectionInfo("Connection test successful after " + testTime + "ms");
                    updateStatus("Connection test successful! You can now click Connect to proceed.", true);
                }
            });
        });
    }
    
    /**
     * Handle connect button
     */
    private void handleConnect(Stage dialogStage) {
        // Extract current form values
        config = extractFormValues();
        
        // Validate configuration
        if (!config.isValid()) {
            Log.connectionWarning("Connect attempted with invalid configuration");
            updateStatus("Please fill in all fields", false);
            return;
        }
        
        Log.connectionInfo("Testing connection before saving configuration for " + config.getHost() + "/" + config.getDatabase());
        
        // Disable connect button and show testing status
        connectButton.setDisable(true);
        connectButton.setText("Testing...");
        updateStatus("Testing connection...", true);
        
        // Test connection before allowing to proceed
        CompletableFuture.supplyAsync(() -> {
            try {
                InfluxDBService influxService = new InfluxDBService(config);
                boolean connected = influxService.testConnection();
                
                if (connected) {
                    return "Connection successful";
                } else {
                    return "Error: Failed to connect to InfluxDB. Please check your connection details.";
                }
            } catch (Exception ex) {
                return "Error: " + ex.getMessage();
            }
        }).thenAcceptAsync(result -> {
            javafx.application.Platform.runLater(() -> {
                connectButton.setDisable(false);
                connectButton.setText("Connect");
                
                if (result.startsWith("Error:")) {
                    Log.connectionError("Connection test failed: " + result);
                    updateStatus("Connection failed! Please check your details.", false);
                    showErrorDialog("Connection Failed", result);
                } else {
                    Log.connectionInfo("Connection test successful, saving configuration");
                    updateStatus("Connection successful! Saving settings...", true);
                    
                    // Save settings only after successful connection test
                    try {
                        settingsService.saveSettings(config);
                        Log.connectionInfo("Connection configuration saved successfully");
                        connectionSuccessful = true;
                        dialogStage.close();
                    } catch (Exception e) {
                        Log.connectionError("Failed to save connection settings: " + e.getMessage());
                        Log.logException("connection", "Settings save error", e);
                        updateStatus("Failed to save settings", false);
                        showErrorDialog("Settings Error", "Failed to save settings: " + e.getMessage());
                    }
                }
            });
        });
    }
    
    /**
     * Extract form values to configuration
     */
    private ApplicationConfig extractFormValues() {
        ApplicationConfig newConfig = new ApplicationConfig();
        
        newConfig.setProtocol(protocolCombo.getValue());
        newConfig.setSkipSSLValidation(skipSSLValidationCheck.isSelected());
        newConfig.setApiType(apiTypeCombo.getValue());
        newConfig.setQueryTimeout(timeoutCombo.getValue());
        newConfig.setHost(hostField.getText());
        newConfig.setDatabase(databaseField.getText());
        newConfig.setToken(tokenField.getText());
        newConfig.setTimezoneConversion(timezoneConversionCheck.isSelected());
        newConfig.setSelectedTimezone(timezoneCombo.getValue());
        
        return newConfig;
    }
    
    /**
     * Update status label
     */
    private void updateStatus(String message, boolean isSuccess) {
        statusLabel.setText(message);
        statusLabel.setTextFill(isSuccess ? Color.GREEN : Color.RED);
    }
    
    /**
     * Show error dialog
     */
    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Get the configuration
     */
    public ApplicationConfig getConfig() {
        return config;
    }
} 