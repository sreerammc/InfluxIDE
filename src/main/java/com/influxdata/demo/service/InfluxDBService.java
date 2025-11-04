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

// Note: Using JDBC approach for Flight SQL (not direct Flight SQL client API)
// The JDBC driver internally handles Flight SQL protocol communication
// See: https://docs.influxdata.com/influxdb3/core/reference/client-libraries/flight/java-flightsql/

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
        // According to Apache Arrow documentation: https://arrow.apache.org/docs/java/flight_sql_jdbc_driver.html
        // The driver class is automatically loaded via ServiceLoader, but we can also load it explicitly
        String[] driverClassNames = {
            "org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver",  // Arrow Flight SQL JDBC Driver
            "org.apache.arrow.flight.sql.jdbc.FlightSqlDriver",     // Alternative class name
            "org.apache.arrow.flight.jdbc.FlightJdbcDriver"         // Legacy class name
        };
        
        boolean driverLoaded = false;
        for (String driverClassName : driverClassNames) {
            try {
                Class<?> driverClass = Class.forName(driverClassName);
                // Explicitly register the driver
                Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(driver);
                Log.connectionInfo("Flight SQL JDBC driver loaded and registered successfully: " + driverClassName);
                driverLoaded = true;
                break;
        } catch (ClassNotFoundException e) {
                // Try next driver class name
                continue;
            } catch (Exception e) {
                Log.connectionWarning("Failed to load driver " + driverClassName + ": " + e.getMessage());
            }
        }
        
        if (!driverLoaded) {
            Log.connectionWarning("Flight SQL JDBC driver not found. Tried: " + String.join(", ", driverClassNames));
            Log.connectionWarning("Please ensure flight-sql-jdbc-driver is in the classpath and JVM arguments are set.");
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
        
        // Configure SSL validation skip if needed
        if (config.isSkipSSLValidation() && config.getProtocol() == Protocol.HTTPS) {
            configureSSLValidationSkip();
        }
        
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
            Log.connectionError("Flight SQL connection test failed: " + e.getMessage());
            throw new ConnectionException("Flight SQL connection test failed", e);
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
            Log.connectionError("InfluxDB 3 Java API connection test failed: " + e.getMessage());
            throw new ConnectionException("InfluxDB 3 Java API connection test failed", e);
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
     * According to Apache Arrow docs: https://arrow.apache.org/docs/java/flight_sql_jdbc_driver.html
     * JDBC URL format is: jdbc:arrow-flight-sql://HOSTNAME:PORT (no endpoint paths)
     */
    private String executeFlightSQLQuery(String query) throws Exception {
        // Translate query if needed
        String translatedQuery = translateQueryForFlightSQL(query);
        
        // Build JDBC URL (format: jdbc:arrow-flight-sql://HOSTNAME:PORT)
        String jdbcUrl = buildFlightSQLJDBCUrl();
        Log.connectionInfo("Attempting Flight SQL connection with URL: " + jdbcUrl);
        
        // Check if a driver is available for this URL
        try {
            Driver driver = DriverManager.getDriver(jdbcUrl);
            Log.connectionInfo("Driver found for URL: " + driver.getClass().getName());
        } catch (SQLException e) {
            Log.connectionError("No driver found for URL: " + jdbcUrl);
            Log.connectionError("Available drivers: " + getAvailableDrivers());
            throw new QueryExecutionException("No suitable driver found for Flight SQL. " +
                "Please ensure the Arrow Flight SQL JDBC driver is loaded and JVM arguments are set. " +
                "Error: " + e.getMessage());
        }
        
        // According to Apache Arrow docs, token can be passed via Properties or URL parameter
        // Using Properties is cleaner and avoids URL encoding issues
        java.util.Properties props = new java.util.Properties();
        props.setProperty("token", config.getToken());
        
        try (Connection connection = DriverManager.getConnection(jdbcUrl, props)) {
            Log.connectionInfo("Flight SQL connection established successfully");
            
            connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), config.getQueryTimeout().getMilliseconds());
            
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout((int) TimeUnit.MILLISECONDS.toSeconds(config.getQueryTimeout().getMilliseconds()));
                
                try (ResultSet resultSet = statement.executeQuery(translatedQuery)) {
                    String result = convertResultSetToJSON(resultSet);
                    Log.queryInfo("Flight SQL query executed successfully");
                    return result;
                }
            }
        } catch (SQLException e) {
            Log.connectionError("Flight SQL connection failed: " + e.getMessage());
            Log.connectionError("SQL State: " + e.getSQLState());
            Log.connectionError("Error Code: " + e.getErrorCode());
            throw e;
        }
    }
    
    /**
     * Execute InfluxDB 3 Java API query
     */
    private String executeInfluxDB3Query(String query) throws Exception {
        // Configure SSL validation skip if needed
        if (config.isSkipSSLValidation() && config.getProtocol() == Protocol.HTTPS) {
            configureSSLValidationSkip();
        }
        
        // Build the InfluxDB 3 API URL with query parameters (matching curl command format)
        String urlString = buildInfluxDB3ApiUrl(query);
        URL url = new URL(urlString);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        // Use Bearer token authentication (matching curl command)
        connection.setRequestProperty("Authorization", "Bearer " + config.getToken());
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(config.getQueryTimeout().getMilliseconds());
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            String errorMessage = connection.getResponseMessage();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream() != null ? 
                        connection.getErrorStream() : connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                if (errorResponse.length() > 0) {
                    errorMessage = errorResponse.toString();
                }
            }
            throw new QueryExecutionException("InfluxDB 3 API error: " + responseCode + " - " + errorMessage);
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
     * Based on Apache Arrow Flight SQL JDBC Driver documentation:
     * https://arrow.apache.org/docs/java/flight_sql_jdbc_driver.html
     * 
     * URI format: jdbc:arrow-flight-sql://HOSTNAME:PORT[/?param1=val1&param2=val2&...]
     * 
     * For HTTP (insecure): useEncryption=0
     * For HTTPS (TLS): useEncryption=1
     */
    private String buildFlightSQLJDBCUrl() {
        StringBuilder url = new StringBuilder("jdbc:arrow-flight-sql://");
        
        // Parse host and port from config.getHost()
        String host = config.getHost();
        int port = 443; // Default port for HTTPS (as per InfluxDB 3 docs)
        
        if (host.contains(":")) {
            String[] parts = host.split(":");
            host = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // Use default port if parsing fails
                // InfluxDB 3 typically uses 8181 for HTTP, 443 for HTTPS
                port = config.getProtocol() == Protocol.HTTPS ? 443 : 8181;
            }
        } else {
            // No port specified, use default based on protocol
            // InfluxDB 3 default: 8181 for HTTP, 443 for HTTPS
            port = config.getProtocol() == Protocol.HTTPS ? 443 : 8181;
        }
        
        // Build URL: jdbc:arrow-flight-sql://HOSTNAME:PORT
        // According to Apache Arrow docs, endpoint paths are not used in JDBC URL
        url.append(host).append(":").append(port);
        
        // Add query parameters
        // According to docs: https://arrow.apache.org/docs/java/flight_sql_jdbc_driver.html
        // Parameters can be passed as URL query parameters
        List<String> params = new ArrayList<>();
        
        // Database parameter - passed as gRPC header (any unrecognized param becomes a header)
        params.add("database=" + config.getDatabase());
        
        // Configure SSL/TLS parameters based on protocol
        // For HTTP: useEncryption=false (equivalent to Location.forGrpcInsecure)
        // For HTTPS: useEncryption=true (equivalent to Location.forGrpcTls)
        boolean useEncryption = config.getProtocol() == Protocol.HTTPS;
        params.add("useEncryption=" + (useEncryption ? "1" : "0"));
        
        // If SSL validation should be skipped, add the appropriate parameters
        // This is important for self-signed certificates
        if (config.isSkipSSLValidation() && useEncryption) {
            params.add("disableCertificateVerification=true");
            Log.connectionInfo("Flight SQL SSL certificate verification disabled");
        }
        
        // Token authentication - can be passed as URL parameter or via Properties
        // We'll pass it via Properties in getConnection(), but can also add to URL
        // params.add("token=" + config.getToken());
        
        if (!params.isEmpty()) {
            url.append("?").append(String.join("&", params));
        }
        
        return url.toString();
    }
    
    /**
     * Build InfluxDB 3 API URL (matching curl command format)
     */
    private String buildInfluxDB3ApiUrl(String query) throws Exception {
        StringBuilder url = new StringBuilder();
        // Use HTTP by default for InfluxDB 3 (matching curl command)
        String protocol = config.getProtocol() == Protocol.HTTPS ? "https" : "http";
        url.append(protocol).append("://");
        url.append(config.getHost()).append("/api/v3/query_sql");
        
        // Add query parameters (URL encoded)
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String encodedDatabase = URLEncoder.encode(config.getDatabase(), StandardCharsets.UTF_8.toString());
        
        url.append("?db=").append(encodedDatabase);
        url.append("&q=").append(encodedQuery);
        
        return url.toString();
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        if (str == null) return "null";
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
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
     * Get list of available JDBC drivers for debugging
     */
    private String getAvailableDrivers() {
        List<String> drivers = new ArrayList<>();
        DriverManager.drivers().forEach(driver -> {
            drivers.add(driver.getClass().getName());
        });
        return drivers.isEmpty() ? "No drivers found" : String.join(", ", drivers);
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