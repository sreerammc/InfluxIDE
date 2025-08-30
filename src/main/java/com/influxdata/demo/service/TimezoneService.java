package com.influxdata.demo.service;

import com.influxdata.demo.exception.ApplicationException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Service for handling timezone operations
 * Manages timezone conversion and JVM timezone settings
 */
public class TimezoneService {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Get the selected timezone for timestamp conversion
     * @param selectedTimezone The timezone string from settings
     * @return ZoneId for the selected timezone
     */
    public ZoneId getSelectedTimezone(String selectedTimezone) {
        if ("System Default (Local)".equals(selectedTimezone)) {
            return ZoneId.systemDefault();
        } else if ("UTC".equals(selectedTimezone)) {
            return ZoneOffset.UTC;
        } else {
            try {
                return ZoneId.of(selectedTimezone);
            } catch (Exception e) {
                throw new ApplicationException("Invalid timezone: " + selectedTimezone, e);
            }
        }
    }
    
    /**
     * Convert UTC timestamp to selected timezone
     * @param utcTime UTC timestamp
     * @param targetZone Target timezone
     * @return Formatted timestamp string in target timezone
     */
    public String convertToTimezone(Instant utcTime, ZoneId targetZone) {
        try {
            ZonedDateTime targetTime = utcTime.atZone(targetZone);
            return targetTime.format(TIMESTAMP_FORMATTER);
        } catch (Exception e) {
            throw new ApplicationException("Failed to convert timestamp to timezone: " + targetZone, e);
        }
    }
    
    /**
     * Convert UTC timestamp to selected timezone
     * @param utcTime UTC timestamp as string
     * @param targetZone Target timezone
     * @return Formatted timestamp string in target timezone
     */
    public String convertToTimezone(String utcTime, ZoneId targetZone) {
        try {
            Instant instant = Instant.parse(utcTime);
            return convertToTimezone(instant, targetZone);
        } catch (Exception e) {
            throw new ApplicationException("Failed to parse timestamp: " + utcTime, e);
        }
    }
    
    /**
     * Convert SQL Timestamp to selected timezone
     * @param sqlTimestamp SQL Timestamp object
     * @param targetZone Target timezone
     * @return Formatted timestamp string in target timezone
     */
    public String convertToTimezone(java.sql.Timestamp sqlTimestamp, ZoneId targetZone) {
        try {
            Instant instant = sqlTimestamp.toInstant();
            return convertToTimezone(instant, targetZone);
        } catch (Exception e) {
            throw new ApplicationException("Failed to convert SQL timestamp to timezone: " + targetZone, e);
        }
    }
    
    /**
     * Set JVM default timezone
     * @param selectedTimezone The timezone string from settings
     */
    public void setJVMTimezone(String selectedTimezone) {
        try {
            if (!"System Default (Local)".equals(selectedTimezone)) {
                ZoneId zoneId = getSelectedTimezone(selectedTimezone);
                TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
                System.out.println("JVM timezone set to: " + zoneId + 
                    " (" + zoneId.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) + ")");
            } else {
                System.out.println("Using system default timezone: " + ZoneId.systemDefault());
            }
        } catch (Exception e) {
            throw new ApplicationException("Failed to set JVM timezone", e);
        }
    }
    
    /**
     * Get current JVM timezone
     * @return Current JVM timezone
     */
    public ZoneId getCurrentJVMTimezone() {
        return ZoneId.systemDefault();
    }
    
    /**
     * Check if timezone conversion is needed
     * @param columnName Column name to check
     * @return true if column contains timestamp data
     */
    public boolean isTimestampColumn(String columnName) {
        if (columnName == null) {
            return false;
        }
        
        String lowerColumnName = columnName.toLowerCase();
        return lowerColumnName.equals("time") || 
               lowerColumnName.contains("timestamp") || 
               lowerColumnName.contains("date");
    }
    
    /**
     * Get available timezone options
     * @return Array of common timezone strings
     */
    public String[] getAvailableTimezones() {
        return new String[] {
            "System Default (Local)",
            "UTC",
            "Europe/London",
            "Europe/Paris", 
            "Europe/Berlin",
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "Asia/Tokyo",
            "Asia/Shanghai",
            "Asia/Kolkata",
            "Australia/Sydney",
            "Pacific/Auckland"
        };
    }
} 