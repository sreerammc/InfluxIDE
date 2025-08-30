package com.influxdata.demo.exception;

/**
 * Exception thrown when query execution fails
 */
public class QueryExecutionException extends ApplicationException {
    
    private static final long serialVersionUID = 1L;
    
    public QueryExecutionException(String message) {
        super(message);
    }
    
    public QueryExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public QueryExecutionException(Throwable cause) {
        super(cause);
    }
} 