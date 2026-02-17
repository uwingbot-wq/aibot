package com.uis.aibot.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for chat messages in the RabbitMQ queue.
 * Contains all necessary information to process and respond to a chat message.
 */
public class ChatQueueMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Source {
        WHATSAPP, WEB, API
    }

    public enum MessageType {
        TEXT, IMAGE, DOCUMENT
    }

    private String messageId;
    private Source source;
    private MessageType messageType;
    private String sessionId;
    private String senderPhone;       // For WhatsApp: the phone number to reply to
    private String text;              // The message text or caption
    private String mediaFilePath;     // Local path where media file is stored
    private String mimeType;          // MIME type of the media (if applicable)
    private String originalFilename;  // Original filename (for documents)
    private Instant timestamp;

    public ChatQueueMessage() {
        this.timestamp = Instant.now();
        this.messageId = java.util.UUID.randomUUID().toString();
    }

    // Builder-style setters for convenience
    public static ChatQueueMessage forWhatsAppText(String senderPhone, String text) {
        ChatQueueMessage msg = new ChatQueueMessage();
        msg.source = Source.WHATSAPP;
        msg.messageType = MessageType.TEXT;
        msg.sessionId = senderPhone;
        msg.senderPhone = senderPhone;
        msg.text = text;
        return msg;
    }

    public static ChatQueueMessage forWhatsAppImage(String senderPhone, String caption, String filePath, String mimeType) {
        ChatQueueMessage msg = new ChatQueueMessage();
        msg.source = Source.WHATSAPP;
        msg.messageType = MessageType.IMAGE;
        msg.sessionId = senderPhone;
        msg.senderPhone = senderPhone;
        msg.text = caption;
        msg.mediaFilePath = filePath;
        msg.mimeType = mimeType;
        return msg;
    }

    public static ChatQueueMessage forWhatsAppDocument(String senderPhone, String caption, String filePath, String mimeType, String originalFilename) {
        ChatQueueMessage msg = new ChatQueueMessage();
        msg.source = Source.WHATSAPP;
        msg.messageType = MessageType.DOCUMENT;
        msg.sessionId = senderPhone;
        msg.senderPhone = senderPhone;
        msg.text = caption;
        msg.mediaFilePath = filePath;
        msg.mimeType = mimeType;
        msg.originalFilename = originalFilename;
        return msg;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMediaFilePath() {
        return mediaFilePath;
    }

    public void setMediaFilePath(String mediaFilePath) {
        this.mediaFilePath = mediaFilePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChatQueueMessage{" +
                "messageId='" + messageId + '\'' +
                ", source=" + source +
                ", messageType=" + messageType +
                ", sessionId='" + sessionId + '\'' +
                ", senderPhone='" + senderPhone + '\'' +
                ", text='" + (text != null ? text.substring(0, Math.min(text.length(), 50)) + "..." : "null") + '\'' +
                ", mediaFilePath='" + mediaFilePath + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
