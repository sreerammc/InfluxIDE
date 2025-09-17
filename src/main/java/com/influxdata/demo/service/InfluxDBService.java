package com.influxdata.demo.service;

import com.influxdata.demo.config.ApplicationConfig;
import com.influxdata.demo.exception.ConnectionException;
import com.influxdata.demo.exception.QueryExecutionException;
import com.influxdata.demo.model.ApiType;
import com.influxdata.demo.model.Protocol;
import com.influxdata.demo.model.QueryTimeout;
import com.influxdata.demo.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// SSL imports
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;

/**
 * Service for InfluxDB operations
 * Handles connection testing, query execution, and data retrieval
 */
public class InfluxDBService {
    
    private final ApplicationConfig config;
    private final TimezoneService timezoneService;
    
    public InfluxDBService(ApplicationConfig config) {
        this.config = config;
        this.timezoneService = new TimezoneService();
        
        // Load Flight SQL JDBC driver
        try {
            Class.forName("org.apache.arrow.flight.sql.jdbc.FlightSqlDriver");
            Log.connectionInfo("Flight SQL JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            Log.connectionWarning("Flight SQL JDBC driver not found: " + e.getMessage());
        }
    }
    
    /**
     * Test connection to InfluxDB
     * @return true if connection successful
     * @throws ConnectionException if connection fails
     */
    public boolean testConnection() throws ConnectionException {
        long startTime = System.currentTimeMillis();
        Log.connectionInfo("Testing connection to " + config.getHost() + "/" + config.getDatabase() + " using " + config.getApiType());
        
        // Configure SSL validation skip if needed
        if (config.isSkipSSLValidation() && config.getProtocol() == Protocol.HTTPS) {
            configureSSLValidationSkip();
        }
        
        try {
            boolean result = false;
            switch (config.getApiType()) {
                case FLIGHT_SQL:
                    result = testFlightSQLConnection();
                    break;
                case INFLUXDB_3_API:
                    result = testInfluxDB3Connection();
                    break;
                case REST_API:
                    result = testRESTConnection();
                    break;
                default:
                    throw new ConnectionException("Unknown API type: " + config.getApiType());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            Log.logConnectionAttempt(config.getHost(), config.getDatabase(), result, duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.connectionError("Connection test failed after " + duration + "ms: " + e.getMessage());
            Log.logException("connection", "Connection test error", e);
            throw new ConnectionException("Connection test failed", e);
        }
    }
    
    /**
     * Execute query against InfluxDB
     * @param query The query to execute
     * @return Query results as JSON string
     * @throws QueryExecutionException if query execution fails
     */
    public String executeQuery(String query) throws QueryExecutionException {
        long startTime = System.currentTimeMillis();
        Log.queryInfo("Executing query using " + config.getApiType() + ": " + query.substring(0, Math.min(query.length(), 100)) + (query.length() > 100 ? "..." : ""));
        
        try {
            String result = null;
            switch (config.getApiType()) {
                case FLIGHT_SQL:
                    result = executeFlightSQLQuery(query);
                    break;
                case INFLUXDB_3_API:
                    result = executeInfluxDB3Query(query);
                    break;
                case REST_API:
                    result = executeRESTQuery(query);
                    break;
                default:
                    throw new QueryExecutionException("Unknown API type: " + config.getApiType());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            Log.queryInfo("Query executed successfully in " + duration + "ms");
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.queryError("Query execution failed after " + duration + "ms: " + e.getMessage());
            Log.logException("query", "Query execution error", e);
            throw new QueryExecutionException("Query execution failed", e);
        }
    }
    
    /**
     * Execute query asynchronously
     * @param query The query to execute
     * @return CompletableFuture with query results
     */
    public CompletableFuture<String> executeQueryAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeQuery(query);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Test Flight SQL connection
     */
    private boolean testFlightSQLConnection() throws Exception {
        String testQuery = "SHOW TABLES";
        
        try {
            String result = executeFlightSQLQuery(testQuery);
            return result != null && !result.contains("error");
        } catch (Exception e) {
            // Try fallback to REST API
            System.out.println("Flight SQL connection failed, trying REST API fallback: " + e.getMessage());
            return testRESTConnection();
        }
    }
    
    /**
     * Test InfluxDB 3 Java API connection
     */
    private boolean testInfluxDB3Connection() throws Exception {
        String testQuery = "SHOW TABLES";
        
        try {
            String result = executeInfluxDB3Query(testQuery);
            return result != null && !result.contains("error");
        } catch (Exception e) {
            // Try fallback to REST API
            System.out.println("InfluxDB 3 API connection failed, trying REST API fallback: " + e.getMessage());
            return testRESTConnection();
        }
    }
    
    /**
     * Test REST API connection
     */
    private boolean testRESTConnection() throws Exception {
        String testQuery = "SHOW MEASUREMENTS";
        String result = executeRESTQuery(testQuery);
        return result != null && !result.contains("error");
    }
    
    /**
     * Execute Flight SQL query
     */
    private String executeFlightSQLQuery(String query) throws Exception {
        // Try multiple Flight SQL endpoints
        String[] endpoints = {"/flight", "/arrow-flight", ""};
        
        for (String endpoint : endpoints) {
            try {
                return executeFlightSQLQueryWithEndpoint(query, endpoint);
            } catch (Exception e) {
                System.out.println("Flight SQL endpoint " + endpoint + " failed: " + e.getMessage());
                if (endpoint.equals("")) {
                    // Last endpoint failed, throw exception
                    throw e;
                }
            }
        }
        
        throw new QueryExecutionException("All Flight SQL endpoints failed");
    }
    
    /**
     * Execute Flight SQL query with specific endpoint
     */
    private String executeFlightSQLQueryWithEndpoint(String query, String endpoint) throws Exception {
        // Translate query if needed
        String translatedQuery = translateQueryForFlightSQL(query);
        
        // Build JDBC URL
        String jdbcUrl = buildFlightSQLJDBCUrl(endpoint);
        
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "token", config.getToken())) {
            connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), config.getQueryTimeout().getMilliseconds());
            
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout((int) TimeUnit.MILLISECONDS.toSeconds(config.getQueryTimeout().getMilliseconds()));
                
                try (ResultSet resultSet = statement.executeQuery(translatedQuery)) {
                    return convertResultSetToJSON(resultSet);
                }
            }
        }
    }
    
    /**
     * Execute InfluxDB 3 Java API query
     */
    private String executeInfluxDB3Query(String query) throws Exception {
        // For now, fallback to REST API
        // TODO: Implement actual InfluxDB 3 Java API client
        return executeRESTQuery(query);
    }
    
    /**
     * Execute REST API query
     */
    private String executeRESTQuery(String query) throws Exception {
        String urlString = buildRESTUrl(query);
        URL url = new URL(urlString);
        
        // Configure SSL if needed
        if (config.isSkipSSLValidation() && config.getProtocol() == Protocol.HTTPS) {
            configureSSLValidationSkip();
        }
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Token " + config.getToken());
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(config.getQueryTimeout().getMilliseconds());
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new QueryExecutionException("HTTP error: " + responseCode + " - " + connection.getResponseMessage());
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    /**
     * Build Flight SQL JDBC URL
     */
    private String buildFlightSQLJDBCUrl(String endpoint) {
        StringBuilder url = new StringBuilder("jdbc:arrow-flight://");
        
        // Parse host and port from config.getHost()
        String host = config.getHost();
        int port = 443; // Default port for HTTPS
        
        if (host.contains(":")) {
            String[] parts = host.split(":");
            host = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // Use default port if parsing fails
                port = config.getProtocol() == Protocol.HTTPS ? 443 : 80;
            }
        } else {
            // No port specified, use default based on protocol
            port = config.getProtocol() == Protocol.HTTPS ? 443 : 80;
        }
        
        // Build the endpoint with host and port
        String fullEndpoint = host + ":" + port;
        if (!endpoint.isEmpty()) {
            fullEndpoint += endpoint;
        }
        
        url.append(fullEndpoint);
        url.append("?database=").append(config.getDatabase());
        url.append("&useEncryption=").append(config.getProtocol() == Protocol.HTTPS);
        
        return url.toString();
    }
    
    /**
     * Build REST API URL
     */
    private String buildRESTUrl(String query) throws Exception {
        StringBuilder url = new StringBuilder();
        url.append(config.getProtocol().getValue()).append("://");
        url.append(config.getHost()).append("/query");
        
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String encodedToken = URLEncoder.encode(config.getToken(), StandardCharsets.UTF_8.toString());
        String encodedDatabase = URLEncoder.encode(config.getDatabase(), StandardCharsets.UTF_8.toString());
        
        url.append("?p=").append(encodedToken);
        url.append("&db=").append(encodedDatabase);
        url.append("&q=").append(encodedQuery);
        
        return url.toString();
    }
    
    /**
     * Translate query for Flight SQL compatibility
     */
    private String translateQueryForFlightSQL(String query) {
        if (query == null) return "";
        
        String upperQuery = query.toUpperCase();
        
        // Translate SHOW MEASUREMENTS to SHOW TABLES
        if (upperQuery.contains("SHOW MEASUREMENTS")) {
            query = query.replaceAll("(?i)SHOW MEASUREMENTS", "SHOW TABLES");
        }
        
        // Translate SHOW DATABASES to SHOW SCHEMAS
        if (upperQuery.contains("SHOW DATABASES")) {
            query = query.replaceAll("(?i)SHOW DATABASES", "SHOW SCHEMAS");
        }
        
        return query;
    }
    
    /**
     * Convert ResultSet to JSON string
     */
    private String convertResultSetToJSON(ResultSet resultSet) throws Exception {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                
                // Handle timestamp conversion
                if (timezoneService.isTimestampColumn(columnName) && config.isTimezoneConversion()) {
                    if (value instanceof Timestamp) {
                        value = timezoneService.convertToTimezone((Timestamp) value, 
                            timezoneService.getSelectedTimezone(config.getSelectedTimezone()));
                    }
                }
                
                row.put(columnName, value);
            }
            
            results.add(row);
        }
        
        // Convert to JSON format
        return convertToJSONFormat(results);
    }
    
    /**
     * Convert results to JSON format
     */
    private String convertToJSONFormat(List<Map<String, Object>> results) {
        StringBuilder json = new StringBuilder();
        json.append("{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"result\",\"columns\":[");
        
        if (!results.isEmpty()) {
            Map<String, Object> firstRow = results.get(0);
            String[] columns = firstRow.keySet().toArray(new String[0]);
            
            // Add column names
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escapeJsonString(columns[i])).append("\"");
            }
            
            json.append("],\"values\":[");
            
            // Add data rows
            for (int i = 0; i < results.size(); i++) {
                if (i > 0) json.append(",");
                json.append("[");
                
                Map<String, Object> row = results.get(i);
                for (int j = 0; j < columns.length; j++) {
                    if (j > 0) json.append(",");
                    Object value = row.get(columns[j]);
                    json.append("\"").append(escapeJsonString(value != null ? value.toString() : "")).append("\"");
                }
                
                json.append("]");
            }
            
            json.append("]}]}]}");
        } else {
            json.append("],\"values\":[]}]}]}");
        }
        
        return json.toString();
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJsonString(String input) {
        if (input == null) return "";
        
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Get current configuration
     */
    public ApplicationConfig getConfig() {
        return config;
    }
    
    /**
     * Configure SSL validation skip for self-signed certificates
     * This method sets up a trust manager that accepts all certificates
     */
    private void configureSSLValidationSkip() {
        try {
            Log.connectionInfo("Configuring SSL validation skip for self-signed certificates");
            
            // Create a trust manager that accepts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Accept all client certificates
                    }
                    
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Accept all server certificates
                    }
                }
            };
            
            // Create SSL context with the trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            // Set the default SSL socket factory
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
            // Create a hostname verifier that accepts all hostnames
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
            // Set system properties for additional SSL bypass
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
            System.setProperty("com.sun.net.ssl.checkRevocation", "false");
            System.setProperty("com.sun.security.ssl.allowUnsafeRenegotiation", "true");
            
            Log.connectionInfo("SSL validation skip configured successfully");
            
        } catch (Exception e) {
            Log.connectionError("Failed to configure SSL validation skip: " + e.getMessage());
            throw new ConnectionException("Failed to configure SSL validation skip: " + e.getMessage(), e);
        }
    }
} 