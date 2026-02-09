// src/main/java/com/uis/aibot/config/VisionOllamaProperties.java
package com.uis.aibot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vision.ollama")
public class VisionOllamaProperties {

    private String baseUrl;

    /** e.g., llama3.2-vision */
    private String model;

    /** stream tokens or not */
    private boolean stream = true;

    /** temperature */
    private double temperature = 0.2;

    /** optional client timeouts */
    private int readTimeoutMs = 60000;
    private int connectTimeoutMs = 10000;

    // getters/setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
}