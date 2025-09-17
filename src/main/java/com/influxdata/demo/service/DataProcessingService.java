package com.influxdata.demo.service;

import com.influxdata.demo.config.UIConstants;
import com.influxdata.demo.exception.QueryExecutionException;

import java.util.*;

/**
 * Service for processing and formatting data
 * Handles JSON parsing, data validation, and result formatting
 */
public class DataProcessingService {
    
    /**
     * Parse InfluxDB response and extract results
     * @param jsonResponse The JSON response from InfluxDB
     * @return List of data rows as maps
     * @throws QueryExecutionException if parsing fails
     */
    public List<Map<String, Object>> parseInfluxDBResponse(String jsonResponse) throws QueryExecutionException {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new QueryExecutionException("Empty response received");
        }
        
        try {
            System.out.println("DataProcessingService: Parsing response of length: " + jsonResponse.length());
            System.out.println("DataProcessingService: Response preview: " + jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
            
            // Try to detect response format
            if (jsonResponse.trim().startsWith("[")) {
                System.out.println("DataProcessingService: Using direct array format");
                return parseDirectArrayFormat(jsonResponse);
            } else if (jsonResponse.contains("\"results\"") && jsonResponse.contains("\"series\"")) {
                System.out.println("DataProcessingService: Using InfluxDB v1 format");
                return parseInfluxDBV1Format(jsonResponse);
            } else {
                System.out.println("DataProcessingService: Unknown response format");
                throw new QueryExecutionException("Unknown response format");
            }
        } catch (Exception e) {
            System.out.println("DataProcessingService: Parsing error: " + e.getMessage());
            throw new QueryExecutionException("Failed to parse response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse direct array format: [{"column1": "value1", "column2": "value2"}]
     */
    private List<Map<String, Object>> parseDirectArrayFormat(String jsonResponse) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Simple JSON parsing for direct array format
        String content = jsonResponse.trim();
        if (!content.startsWith("[") || !content.endsWith("]")) {
            throw new Exception("Invalid array format");
        }
        
        content = content.substring(1, content.length() - 1);
        
        // Split by objects
        String[] objects = splitJsonObjects(content);
        
        for (String obj : objects) {
            if (obj.trim().isEmpty()) continue;
            
            Map<String, Object> row = parseJsonObject(obj);
            if (row != null && !row.isEmpty()) {
                results.add(row);
            }
        }
        
        // Limit results to prevent memory issues
        return limitArraySize(results);
    }
    
    /**
     * Parse InfluxDB v1 format: {"results":[{"series":[{"columns":[],"values":[]}]}]}
     */
    private List<Map<String, Object>> parseInfluxDBV1Format(String jsonResponse) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Simple JSON parsing for InfluxDB v1 format
        String content = jsonResponse;
        
        // Extract columns
        List<String> columns = extractColumns(content);
        if (columns.isEmpty()) {
            throw new Exception("No columns found in response");
        }
        
        // Extract values
        List<List<Object>> values = extractValues(content);
        
        // Convert to map format
        for (List<Object> valueRow : values) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 0; i < columns.size() && i < valueRow.size(); i++) {
                row.put(columns.get(i), valueRow.get(i));
            }
            results.add(row);
        }
        
        // Limit results to prevent memory issues
        return limitArraySize(results);
    }
    
    /**
     * Extract column names from InfluxDB v1 response
     */
    private List<String> extractColumns(String jsonResponse) throws Exception {
        List<String> columns = new ArrayList<>();
        
        int startIndex = jsonResponse.indexOf("\"columns\":[");
        if (startIndex == -1) return columns;
        
        startIndex = jsonResponse.indexOf("[", startIndex);
        int endIndex = jsonResponse.indexOf("]", startIndex);
        
        if (startIndex == -1 || endIndex == -1) return columns;
        
        String columnsSection = jsonResponse.substring(startIndex + 1, endIndex);
        String[] columnArray = columnsSection.split(",");
        
        for (String column : columnArray) {
            column = column.trim();
            if (column.startsWith("\"") && column.endsWith("\"")) {
                column = column.substring(1, column.length() - 1);
                columns.add(column);
            }
        }
        
        return columns;
    }
    
    /**
     * Extract values from InfluxDB v1 response
     */
    private List<List<Object>> extractValues(String jsonResponse) throws Exception {
        List<List<Object>> values = new ArrayList<>();
        
        int startIndex = jsonResponse.indexOf("\"values\":[");
        if (startIndex == -1) return values;
        
        startIndex = jsonResponse.indexOf("[", startIndex);
        int endIndex = findMatchingBracket(jsonResponse, startIndex);
        
        if (startIndex == -1 || endIndex == -1) return values;
        
        String valuesSection = jsonResponse.substring(startIndex + 1, endIndex);
        String[] valueRows = splitValueRows(valuesSection);
        
        for (String valueRow : valueRows) {
            if (valueRow.trim().isEmpty()) continue;
            
            List<Object> row = parseValueRow(valueRow);
            if (row != null && !row.isEmpty()) {
                values.add(row);
            }
        }
        
        return values;
    }
    
    /**
     * Split JSON objects by commas, respecting nested structures
     */
    private String[] splitJsonObjects(String content) {
        List<String> objects = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;
        boolean inQuotes = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                else if (c == ',' && braceCount == 0) {
                    objects.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            objects.add(current.toString().trim());
        }
        
        return objects.toArray(new String[0]);
    }
    
    /**
     * Parse JSON object string into map
     */
    private Map<String, Object> parseJsonObject(String jsonObject) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        if (!jsonObject.startsWith("{") || !jsonObject.endsWith("}")) {
            return result;
        }
        
        String content = jsonObject.substring(1, jsonObject.length() - 1);
        String[] pairs = splitJsonPairs(content);
        
        for (String pair : pairs) {
            if (pair.trim().isEmpty()) continue;
            
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                // Remove quotes from key and value
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }
                
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * Split JSON pairs by commas, respecting nested structures
     */
    private String[] splitJsonPairs(String content) {
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;
        boolean inQuotes = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '{' || c == '[') braceCount++;
                else if (c == '}' || c == ']') braceCount--;
                else if (c == ',' && braceCount == 0) {
                    pairs.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            pairs.add(current.toString().trim());
        }
        
        return pairs.toArray(new String[0]);
    }
    
    /**
     * Parse value row from InfluxDB response
     */
    private List<Object> parseValueRow(String valueRow) throws Exception {
        List<Object> row = new ArrayList<>();
        
        if (!valueRow.startsWith("[") || !valueRow.endsWith("]")) {
            return row;
        }
        
        String content = valueRow.substring(1, valueRow.length() - 1);
        String[] values = splitValueArray(content);
        
        for (String value : values) {
            value = value.trim();
            
            if (value.equals("null")) {
                row.add(null);
            } else if (value.startsWith("\"") && value.endsWith("\"")) {
                row.add(value.substring(1, value.length() - 1));
            } else {
                // Try to parse as number
                try {
                    if (value.contains(".")) {
                        row.add(Double.parseDouble(value));
                    } else {
                        row.add(Long.parseLong(value));
                    }
                } catch (NumberFormatException e) {
                    row.add(value);
                }
            }
        }
        
        return row;
    }
    
    /**
     * Split value array by commas, respecting nested structures
     */
    private String[] splitValueArray(String content) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;
        boolean inQuotes = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '{' || c == '[') braceCount++;
                else if (c == '}' || c == ']') braceCount--;
                else if (c == ',' && braceCount == 0) {
                    values.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            values.add(current.toString().trim());
        }
        
        return values.toArray(new String[0]);
    }
    
    /**
     * Find matching closing bracket
     */
    private int findMatchingBracket(String text, int startIndex) {
        int braceCount = 0;
        boolean inQuotes = false;
        
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '[') braceCount++;
                else if (c == ']') {
                    braceCount--;
                    if (braceCount == 0) return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Split value rows by commas, respecting nested structures
     */
    private String[] splitValueRows(String content) {
        List<String> rows = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;
        boolean inQuotes = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '[') braceCount++;
                else if (c == ']') braceCount--;
                else if (c == ',' && braceCount == 0) {
                    rows.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            rows.add(current.toString().trim());
        }
        
        return rows.toArray(new String[0]);
    }
    
    /**
     * Limit array size to prevent memory issues
     */
    private List<Map<String, Object>> limitArraySize(List<Map<String, Object>> results) {
        if (results.size() > UIConstants.MAX_TABLE_ROWS) {
            return results.subList(0, UIConstants.MAX_TABLE_ROWS);
        }
        return results;
    }
    
    /**
     * Format results for display
     * @param results The results to format
     * @return Formatted results string
     */
    public String formatResultsForDisplay(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return "No results to display";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Results (").append(results.size()).append(" records):\n\n");
        
        // Get column names from first row
        Map<String, Object> firstRow = results.get(0);
        String[] columns = firstRow.keySet().toArray(new String[0]);
        
        // Print column headers
        for (String column : columns) {
            sb.append(String.format("%-20s", column));
        }
        sb.append("\n");
        
        // Print separator line
        for (String column : columns) {
            sb.append(String.format("%-20s", "-".repeat(Math.min(column.length(), 19))));
        }
        sb.append("\n");
        
        // Print data rows
        for (Map<String, Object> row : results) {
            for (String column : columns) {
                Object value = row.get(column);
                String displayValue = value != null ? value.toString() : "null";
                
                // Truncate long values
                if (displayValue.length() > 18) {
                    displayValue = displayValue.substring(0, 15) + "...";
                }
                
                sb.append(String.format("%-20s", displayValue));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
} 