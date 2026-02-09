package com.uis.aibot.dto;

public record ChatResponse(String message, String sessionId, long timestamp) {

    public ChatResponse(String message, String sessionId) {
        this(message, sessionId, System.currentTimeMillis());
    }
}
