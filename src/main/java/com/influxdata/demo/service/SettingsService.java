package com.influxdata.demo.service;

import com.influxdata.demo.config.ApplicationConfig;
import com.influxdata.demo.config.UIConstants;
import com.influxdata.demo.exception.ApplicationException;
import com.influxdata.demo.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Service for managing application settings
 * Handles loading, saving, and validation of configuration
 */
public class SettingsService {
    
    private static final String SETTINGS_FILE = UIConstants.SETTINGS_FILE;
    private static final String SETTINGS_DIR = UIConstants.SETTINGS_DIR;
    
    /**
     * Load application settings from file
     * @return ApplicationConfig with loaded settings
     */
    public ApplicationConfig loadSettings() {
        try {
            if (!settingsFileExists()) {
                Log.appInfo("No settings file found, using default configuration");
                return new ApplicationConfig();
            }
            
            Log.appInfo("Loading settings from: " + SETTINGS_FILE);
            Properties props = loadPropertiesFromFile();
            ApplicationConfig config = new ApplicationConfig();
            applyPropertiesToConfig(props, config);
            
            Log.appInfo("Settings loaded successfully");
            return config;
        } catch (Exception e) {
            Log.appError("Failed to load settings: " + e.getMessage());
            Log.logException("application", "Settings load error", e);
            throw new ApplicationException("Failed to load settings", e);
        }
    }
    
    /**
     * Save application settings to file
     * @param config Configuration to save
     */
    public void saveSettings(ApplicationConfig config) {
        try {
            Log.appInfo("Saving settings to: " + SETTINGS_FILE);
            createSettingsDirectoryIfNeeded();
            Properties props = convertConfigToProperties(config);
            savePropertiesToFile(props);
            Log.appInfo("Settings saved successfully");
        } catch (Exception e) {
            Log.appError("Failed to save settings: " + e.getMessage());
            Log.logException("application", "Settings save error", e);
            throw new ApplicationException("Failed to save settings", e);
        }
    }
    
    /**
     * Create settings directory if it doesn't exist
     */
    private void createSettingsDirectoryIfNeeded() {
        File settingsDir = new File(SETTINGS_DIR);
        if (!settingsDir.exists()) {
            if (!settingsDir.mkdirs()) {
                throw new ApplicationException("Failed to create settings directory: " + SETTINGS_DIR);
            }
        }
    }
    
    /**
     * Load properties from settings file
     */
    private Properties loadPropertiesFromFile() throws IOException {
        Properties props = new Properties();
        
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try (FileInputStream in = new FileInputStream(settingsFile)) {
                props.load(in);
            }
        }
        
        return props;
    }
    
    /**
     * Apply properties to configuration object
     */
    private void applyPropertiesToConfig(Properties props, ApplicationConfig config) {
        // Connection settings
        config.setProtocol(com.influxdata.demo.model.Protocol.fromValue(
            props.getProperty("protocol", "http")));
        config.setHost(props.getProperty("host", ""));
        config.setDatabase(props.getProperty("database", ""));
        config.setSkipSSLValidation(Boolean.parseBoolean(
            props.getProperty("skipSSLValidation", "false")));
        
        // API settings
        config.setApiType(com.influxdata.demo.model.ApiType.fromDisplayName(
            props.getProperty("apiType", "Flight SQL")));
        config.setQueryTimeout(com.influxdata.demo.model.QueryTimeout.fromDisplayName(
            props.getProperty("queryTimeout", "2 minutes")));
        
        // Timezone settings
        config.setTimezoneConversion(Boolean.parseBoolean(
            props.getProperty("timezoneConversion", "true")));
        config.setSelectedTimezone(props.getProperty("selectedTimezone", "System Default (Local)"));
    }
    
    /**
     * Convert configuration object to properties
     */
    private Properties convertConfigToProperties(ApplicationConfig config) {
        Properties props = new Properties();
        
        // Connection settings
        props.setProperty("protocol", config.getProtocol().getValue());
        props.setProperty("host", config.getHost());
        props.setProperty("database", config.getDatabase());
        props.setProperty("skipSSLValidation", String.valueOf(config.isSkipSSLValidation()));
        
        // API settings
        props.setProperty("apiType", config.getApiType().getDisplayName());
        props.setProperty("queryTimeout", config.getQueryTimeout().getDisplayName());
        
        // Timezone settings
        props.setProperty("timezoneConversion", String.valueOf(config.isTimezoneConversion()));
        props.setProperty("selectedTimezone", config.getSelectedTimezone());
        
        return props;
    }
    
    /**
     * Save properties to settings file
     */
    private void savePropertiesToFile(Properties props) throws IOException {
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "InfluxDB IDE Settings - Generated automatically");
        }
    }
    
    /**
     * Get settings file path
     */
    public String getSettingsFilePath() {
        return SETTINGS_FILE;
    }
    
    /**
     * Check if settings file exists
     */
    public boolean settingsFileExists() {
        return new File(SETTINGS_FILE).exists();
    }
} 