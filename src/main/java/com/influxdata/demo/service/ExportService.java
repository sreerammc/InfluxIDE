package com.influxdata.demo.service;

import com.influxdata.demo.exception.ApplicationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting data to various formats
 * Currently supports CSV export with future extensibility
 */
public class ExportService {
    
    private static final String CSV_EXTENSION = ".csv";
    private static final String DEFAULT_FILENAME_PREFIX = "influxdb_export";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Export data to CSV file
     * @param data The data to export
     * @param filename Optional filename (without extension)
     * @return Path to the exported file
     * @throws ApplicationException if export fails
     */
    public String exportToCSV(List<Map<String, Object>> data, String filename) throws ApplicationException {
        if (data == null || data.isEmpty()) {
            throw new ApplicationException("No data to export");
        }
        
        try {
            // Generate filename if not provided
            if (filename == null || filename.trim().isEmpty()) {
                filename = generateDefaultFilename();
            }
            
            // Ensure filename has .csv extension
            if (!filename.toLowerCase().endsWith(CSV_EXTENSION)) {
                filename += CSV_EXTENSION;
            }
            
            // Create file
            File exportFile = new File(filename);
            
            // Export data
            exportDataToCSV(data, exportFile);
            
            return exportFile.getAbsolutePath();
            
        } catch (Exception e) {
            throw new ApplicationException("Failed to export to CSV: " + e.getMessage(), e);
        }
    }
    
    /**
     * Export data to CSV file with default filename
     * @param data The data to export
     * @return Path to the exported file
     * @throws ApplicationException if export fails
     */
    public String exportToCSV(List<Map<String, Object>> data) throws ApplicationException {
        return exportToCSV(data, null);
    }
    
    /**
     * Export data to CSV file asynchronously
     * @param data The data to export
     * @param filename Optional filename (without extension)
     * @return CompletableFuture with the export result
     */
    public java.util.concurrent.CompletableFuture<String> exportToCSVAsync(List<Map<String, Object>> data, String filename) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return exportToCSV(data, filename);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Export data to CSV file asynchronously with default filename
     * @param data The data to export
     * @return CompletableFuture with the export result
     */
    public java.util.concurrent.CompletableFuture<String> exportToCSVAsync(List<Map<String, Object>> data) {
        return exportToCSVAsync(data, null);
    }
    
    /**
     * Generate default filename with timestamp
     * @return Default filename
     */
    private String generateDefaultFilename() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        return DEFAULT_FILENAME_PREFIX + "_" + timestamp + CSV_EXTENSION;
    }
    
    /**
     * Export data to CSV file
     * @param data The data to export
     * @param file The file to write to
     * @throws IOException if writing fails
     */
    private void exportDataToCSV(List<Map<String, Object>> data, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            // Write CSV header
            writeCSVHeader(data, writer);
            
            // Write CSV data
            writeCSVData(data, writer);
        }
    }
    
    /**
     * Write CSV header row
     * @param data The data to extract column names from
     * @param writer The file writer
     * @throws IOException if writing fails
     */
    private void writeCSVHeader(List<Map<String, Object>> data, FileWriter writer) throws IOException {
        if (data.isEmpty()) return;
        
        Map<String, Object> firstRow = data.get(0);
        String[] columns = firstRow.keySet().toArray(new String[0]);
        
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) writer.write(",");
            writer.write(escapeCSVField(columns[i]));
        }
        writer.write("\n");
    }
    
    /**
     * Write CSV data rows
     * @param data The data to write
     * @param writer The file writer
     * @throws IOException if writing fails
     */
    private void writeCSVData(List<Map<String, Object>> data, FileWriter writer) throws IOException {
        if (data.isEmpty()) return;
        
        Map<String, Object> firstRow = data.get(0);
        String[] columns = firstRow.keySet().toArray(new String[0]);
        
        for (Map<String, Object> row : data) {
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) writer.write(",");
                
                Object value = row.get(columns[i]);
                String stringValue = value != null ? value.toString() : "";
                
                writer.write(escapeCSVField(stringValue));
            }
            writer.write("\n");
        }
    }
    
    /**
     * Escape CSV field value
     * @param field The field value to escape
     * @return Escaped field value
     */
    private String escapeCSVField(String field) {
        if (field == null) return "";
        
        // Check if field needs escaping
        boolean needsEscaping = field.contains(",") || field.contains("\"") || 
                               field.contains("\n") || field.contains("\r");
        
        if (needsEscaping) {
            // Escape quotes by doubling them
            String escaped = field.replace("\"", "\"\"");
            // Wrap in quotes
            return "\"" + escaped + "\"";
        } else {
            return field;
        }
    }
    
    /**
     * Get export statistics
     * @param data The data that was exported
     * @return Export statistics string
     */
    public String getExportStatistics(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "No data exported";
        }
        
        int rowCount = data.size();
        int columnCount = data.isEmpty() ? 0 : data.get(0).size();
        
        return String.format("Exported %d rows with %d columns", rowCount, columnCount);
    }
    
    /**
     * Validate export data
     * @param data The data to validate
     * @throws ApplicationException if data is invalid
     */
    public void validateExportData(List<Map<String, Object>> data) throws ApplicationException {
        if (data == null) {
            throw new ApplicationException("Export data cannot be null");
        }
        
        if (data.isEmpty()) {
            throw new ApplicationException("Export data cannot be empty");
        }
        
        // Check if all rows have the same columns
        Map<String, Object> firstRow = data.get(0);
        int expectedColumns = firstRow.size();
        
        for (int i = 1; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            if (row.size() != expectedColumns) {
                throw new ApplicationException(
                    String.format("Row %d has %d columns, expected %d", 
                        i + 1, row.size(), expectedColumns));
            }
        }
    }
    
    /**
     * Get supported export formats
     * @return Array of supported format names
     */
    public String[] getSupportedFormats() {
        return new String[]{"CSV"};
    }
    
    /**
     * Check if format is supported
     * @param format The format to check
     * @return true if supported, false otherwise
     */
    public boolean isFormatSupported(String format) {
        if (format == null) return false;
        
        String[] supported = getSupportedFormats();
        for (String supportedFormat : supported) {
            if (supportedFormat.equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }
} 