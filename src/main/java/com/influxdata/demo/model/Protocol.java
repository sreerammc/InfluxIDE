package com.influxdata.demo.model;

/**
 * Enum representing connection protocols
 */
public enum Protocol {
    
    HTTP("http", 80, false),
    HTTPS("https", 443, true);
    
    private final String value;
    private final int defaultPort;
    private final boolean requiresEncryption;
    
    Protocol(String value, int defaultPort, boolean requiresEncryption) {
        this.value = value;
        this.defaultPort = defaultPort;
        this.requiresEncryption = requiresEncryption;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getDefaultPort() {
        return defaultPort;
    }
    
    public boolean requiresEncryption() {
        return requiresEncryption;
    }
    
    /**
     * Get protocol from string value
     */
    public static Protocol fromValue(String value) {
        for (Protocol protocol : values()) {
            if (protocol.value.equalsIgnoreCase(value)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("Unknown protocol: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
} 