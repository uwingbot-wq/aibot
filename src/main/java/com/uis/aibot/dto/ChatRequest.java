package com.uis.aibot.dto;

public record ChatRequest(
        String message,
        String sessionId,
        String imageBase64,  // Optional: base64 encoded image (deprecated - use filePath instead)
        String mediaType,    // Optional: image media type
        String filePath      // Optional: file path for uploaded images
) {

    public ChatRequest {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        // Use a default session ID if not provided
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }
    }

    // Constructor for backward compatibility (text-only messages)
    public ChatRequest(String message, String sessionId) {
        this(message, sessionId, null, null, null);
    }

    public boolean hasImage() {
        return (imageBase64 != null && !imageBase64.isBlank()) ||
               (filePath != null && !filePath.isBlank());
    }

    public boolean hasFilePath() {
        return filePath != null && !filePath.isBlank();
    }
}
