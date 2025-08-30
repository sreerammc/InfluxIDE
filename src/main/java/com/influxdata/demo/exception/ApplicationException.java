package com.influxdata.demo.exception;

/**
 * Base exception class for the InfluxDB IDE application
 * Provides consistent exception handling across the application
 */
public class ApplicationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new application exception with the specified detail message
     */
    public ApplicationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new application exception with the specified detail message and cause
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new application exception with the specified cause
     */
    public ApplicationException(Throwable cause) {
        super(cause);
    }
} 