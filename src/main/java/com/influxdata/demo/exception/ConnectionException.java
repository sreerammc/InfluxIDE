package com.influxdata.demo.exception;

/**
 * Exception thrown when connection to InfluxDB fails
 */
public class ConnectionException extends ApplicationException {
    
    private static final long serialVersionUID = 1L;
    
    public ConnectionException(String message) {
        super(message);
    }
    
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ConnectionException(Throwable cause) {
        super(cause);
    }
} 