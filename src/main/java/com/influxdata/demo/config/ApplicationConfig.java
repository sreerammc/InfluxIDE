package com.influxdata.demo.config;

import com.influxdata.demo.model.ApiType;
import com.influxdata.demo.model.Protocol;
import com.influxdata.demo.model.QueryTimeout;

/**
 * Configuration class for the InfluxDB IDE application
 * Holds all application settings and provides validation
 */
public class ApplicationConfig {
    
    // Connection settings
    private Protocol protocol = Protocol.HTTP;
    private String host = "";
    private String database = "";
    private String token = "";
    private boolean skipSSLValidation = false;
    
    // API settings
    private ApiType apiType = ApiType.FLIGHT_SQL;
    private QueryTimeout queryTimeout = QueryTimeout.getDefault();
    
    // Timezone settings
    private boolean timezoneConversion = true;
    private String selectedTimezone = "System Default (Local)";
    
    // Timestamp format settings
    private String timestampFormat = "ISO_8601";
    
    // Default constructor
    public ApplicationConfig() {}
    
    // Copy constructor
    public ApplicationConfig(ApplicationConfig other) {
        this.protocol = other.protocol;
        this.host = other.host;
        this.database = other.database;
        this.token = other.token;
        this.skipSSLValidation = other.skipSSLValidation;
        this.apiType = other.apiType;
        this.queryTimeout = other.queryTimeout;
        this.timezoneConversion = other.timezoneConversion;
        this.selectedTimezone = other.selectedTimezone;
        this.timestampFormat = other.timestampFormat;
    }
    
    // Getters and Setters
    public Protocol getProtocol() {
        return protocol;
    }
    
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol != null ? protocol : Protocol.HTTP;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host != null ? host.trim() : "";
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database != null ? database.trim() : "";
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token != null ? token.trim() : "";
    }
    
    public boolean isSkipSSLValidation() {
        return skipSSLValidation;
    }
    
    public void setSkipSSLValidation(boolean skipSSLValidation) {
        this.skipSSLValidation = skipSSLValidation;
    }
    
    public ApiType getApiType() {
        return apiType;
    }
    
    public void setApiType(ApiType apiType) {
        this.apiType = apiType != null ? apiType : ApiType.FLIGHT_SQL;
    }
    
    public QueryTimeout getQueryTimeout() {
        return queryTimeout;
    }
    
    public void setQueryTimeout(QueryTimeout queryTimeout) {
        this.queryTimeout = queryTimeout != null ? queryTimeout : QueryTimeout.getDefault();
    }
    
    public boolean isTimezoneConversion() {
        return timezoneConversion;
    }
    
    public void setTimezoneConversion(boolean timezoneConversion) {
        this.timezoneConversion = timezoneConversion;
    }
    
    public String getSelectedTimezone() {
        return selectedTimezone;
    }
    
    public void setSelectedTimezone(String selectedTimezone) {
        this.selectedTimezone = selectedTimezone != null ? selectedTimezone : "System Default (Local)";
    }
    
    public String getTimestampFormat() {
        return timestampFormat;
    }
    
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat != null ? timestampFormat : "ISO_8601";
    }
    
    /**
     * Validate the configuration
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return !host.isEmpty() && !database.isEmpty() && !token.isEmpty();
    }
    
    /**
     * Get validation error message
     * @return error message or null if valid
     */
    public String getValidationError() {
        if (host.isEmpty()) {
            return "Host is required";
        }
        if (database.isEmpty()) {
            return "Database is required";
        }
        if (token.isEmpty()) {
            return "Token is required";
        }
        return null;
    }
    
    /**
     * Check if this configuration supports Flight SQL
     */
    public boolean supportsFlightSQL() {
        return apiType.isFlightSQL();
    }
    
    /**
     * Get connection URL for the current configuration
     */
    public String getConnectionUrl() {
        return protocol.getValue() + "://" + host;
    }
    
    @Override
    public String toString() {
        return String.format("ApplicationConfig{protocol=%s, host='%s', database='%s', apiType=%s, timezoneConversion=%s, selectedTimezone='%s'}",
            protocol, host, database, apiType, timezoneConversion, selectedTimezone);
    }
} 