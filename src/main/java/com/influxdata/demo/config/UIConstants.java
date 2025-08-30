package com.influxdata.demo.config;

/**
 * UI Constants for the InfluxDB IDE application
 * Contains all hardcoded values for dimensions, colors, fonts, and styling
 */
public final class UIConstants {
    
    // Prevent instantiation
    private UIConstants() {}
    
    // Connection Dialog Dimensions
    public static final int CONNECTION_DIALOG_WIDTH = 600;
    public static final int CONNECTION_DIALOG_HEIGHT = 700;
    public static final int CONNECTION_DIALOG_MIN_WIDTH = 600;
    public static final int CONNECTION_DIALOG_MIN_HEIGHT = 700;
    
    // Main Window Dimensions
    public static final int MAIN_WINDOW_WIDTH = 1200;
    public static final int MAIN_WINDOW_HEIGHT = 800;
    public static final int MAIN_WINDOW_MIN_WIDTH = 800;
    public static final int MAIN_WINDOW_MIN_HEIGHT = 600;
    
    // Spacing and Padding
    public static final int DEFAULT_SPACING = 10;
    public static final int FORM_SPACING = 20;
    public static final int LAYOUT_SPACING = 25;
    public static final int DIALOG_PADDING = 35;
    public static final int FORM_PADDING = 25;
    public static final int BUTTON_SPACING = 25;
    
    // Label Dimensions
    public static final int LABEL_MIN_WIDTH = 100;
    public static final int LABEL_PREF_WIDTH = 120;
    
    // Input Field Dimensions
    public static final int TEXT_FIELD_PREF_WIDTH = 300;
    public static final int COMBO_BOX_PREF_WIDTH = 200;
    public static final int TIMEOUT_COMBO_WIDTH = 150;
    public static final int TIMEZONE_COMBO_WIDTH = 250;
    
    // Button Dimensions
    public static final int BUTTON_PREF_WIDTH = 120;
    public static final int BUTTON_PREF_HEIGHT = 35;
    
    // Font Settings
    public static final String DEFAULT_FONT_FAMILY = "Arial";
    public static final int TITLE_FONT_SIZE = 24;
    public static final int DEFAULT_FONT_SIZE = 12;
    public static final int SMALL_FONT_SIZE = 10;
    
    // Colors
    public static final String PRIMARY_COLOR = "#2196F3";      // Blue
    public static final String SUCCESS_COLOR = "#4CAF50";      // Green
    public static final String WARNING_COLOR = "#FF9800";      // Orange
    public static final String ERROR_COLOR = "#F44336";        // Red
    public static final String SECONDARY_COLOR = "#6c757d";    // Gray
    public static final String BACKGROUND_COLOR = "#f5f5f5";   // Light Gray
    public static final String FORM_BACKGROUND = "white";
    public static final String BORDER_COLOR = "#cccccc";
    public static final String TITLE_COLOR = "#00008B";        // Dark Blue
    public static final String NOTE_COLOR = "#666";            // Dark Gray
    public static final String WARNING_NOTE_COLOR = "#FF6B35"; // Orange
    
    // Border and Style
    public static final int BORDER_RADIUS = 5;
    public static final String BORDER_STYLE = String.format("-fx-border-color: %s; -fx-border-radius: %d;", BORDER_COLOR, BORDER_RADIUS);
    public static final String FORM_STYLE = String.format("-fx-background-color: %s; %s -fx-background-radius: %d;", FORM_BACKGROUND, BORDER_STYLE, BORDER_RADIUS);
    public static final String BACKGROUND_STYLE = String.format("-fx-background-color: %s;", BACKGROUND_COLOR);
    
    // Button Styles
    public static final String PRIMARY_BUTTON_STYLE = String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold;", PRIMARY_COLOR);
    public static final String SUCCESS_BUTTON_STYLE = String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold;", SUCCESS_COLOR);
    public static final String SECONDARY_BUTTON_STYLE = String.format("-fx-background-color: %s; -fx-text-fill: white;", SECONDARY_COLOR);
    
    // Text Styles
    public static final String TITLE_STYLE = String.format("-fx-font-weight: bold; -fx-font-size: %d;", TITLE_FONT_SIZE);
    public static final String TITLE_COLOR_STYLE = String.format("-fx-font-weight: bold; -fx-font-size: %d; -fx-text-fill: %s;", TITLE_FONT_SIZE, TITLE_COLOR);
    public static final String NOTE_STYLE = String.format("-fx-font-size: %d; -fx-text-fill: %s; -fx-font-style: italic;", SMALL_FONT_SIZE, NOTE_COLOR);
    public static final String WARNING_NOTE_STYLE = String.format("-fx-font-size: %d; -fx-text-fill: %s; -fx-font-style: italic;", SMALL_FONT_SIZE, WARNING_NOTE_COLOR);
    
    // Icon Dimensions
    public static final int ICON_SIZE = 32;
    public static final int ICON_PADDING = 4;
    public static final int ICON_INNER_SIZE = 24;
    public static final int ICON_CORNER_RADIUS = 8;
    
    // Memory Management
    public static final int MAX_RESULT_SIZE_MB = 100;
    public static final int MAX_TABLE_ROWS = 10000;
    public static final int MAX_TEXT_LENGTH = 1000000;
    public static final int CHUNK_SIZE = 1000;
    
    // Settings
    public static final String SETTINGS_DIR = System.getProperty("user.home") + System.getProperty("file.separator") + ".influxdb-ide";
    public static final String SETTINGS_FILE = SETTINGS_DIR + System.getProperty("file.separator") + "settings.properties";
} 