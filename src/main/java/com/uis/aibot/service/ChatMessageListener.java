package com.uis.aibot.service;

import com.uis.aibot.config.RabbitMQConfig;
import com.uis.aibot.dto.ChatQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ listener that processes chat messages from the queue.
 * Uses MCPAwareChatService for AI processing and delivers responses
 * back to the appropriate channel (WhatsApp, Web, etc.).
 */
@Service
public class ChatMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageListener.class);

    private final MCPAwareChatService chatService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;

    public ChatMessageListener(MCPAwareChatService chatService,
                               WhatsAppDeliveryService whatsAppDeliveryService) {
        this.chatService = chatService;
        this.whatsAppDeliveryService = whatsAppDeliveryService;
    }

    /**
     * Listens for chat messages from RabbitMQ and processes them.
     *
     * @param message The incoming chat message from the queue
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void processMessage(ChatQueueMessage message) {
        logger.info("📥 Processing message: {} from {} via {}",
                    message.getMessageId(),
                    message.getSenderPhone(),
                    message.getSource());

        try {
            switch (message.getSource()) {
                case WHATSAPP -> processWhatsAppMessage(message);
                case WEB -> processWebMessage(message);
                case API -> processApiMessage(message);
                default -> logger.warn("⚠️ Unknown message source: {}", message.getSource());
            }
        } catch (Exception e) {
            logger.error("❌ Error processing message {}: {}", message.getMessageId(), e.getMessage(), e);
            // Re-throw to let RabbitMQ handle retry/DLQ
            throw new RuntimeException("Failed to process message: " + message.getMessageId(), e);
        }
    }

    /**
     * Process WhatsApp messages - chat with AI and send response back to WhatsApp.
     */
    private void processWhatsAppMessage(ChatQueueMessage message) {
        logger.debug("📱 Processing WhatsApp message from: {}", message.getSenderPhone());

        String response;

        try {
            switch (message.getMessageType()) {
                case TEXT -> {
                    // Text-only message
                    response = chatService.chat(message.getSessionId(), message.getText())
                            .block();
                }
                case IMAGE -> {
                    if (message.getText() == null || message.getText().isEmpty()) {
                        // Image without caption - just add to history
                        String uploadMessage = "upload file to filepath: " + message.getMediaFilePath();
                        chatService.addToHistory(message.getSessionId(),
                                new MCPAwareChatService.ChatMessage("user", uploadMessage));
                        logger.info("📝 Added image to history for session: {}", message.getSessionId());
                        return; // No response needed
                    } else {
                        // Image with caption - process with AI
                        response = chatService.chat(
                                message.getSessionId(),
                                message.getText(),
                                message.getMimeType(),
                                message.getMediaFilePath()
                        ).block();
                    }
                }
                case DOCUMENT -> {
                    if (message.getText() == null || message.getText().isEmpty()) {
                        // Document without caption - just add to history
                        String uploadMessage = "upload file to filepath: " + message.getMediaFilePath();
                        chatService.addToHistory(message.getSessionId(),
                                new MCPAwareChatService.ChatMessage("user", uploadMessage));
                        logger.info("📝 Added document to history for session: {}", message.getSessionId());
                        return; // No response needed
                    } else {
                        // Document with caption - process with AI
                        response = chatService.chat(
                                message.getSessionId(),
                                message.getText(),
                                message.getMimeType(),
                                message.getMediaFilePath()
                        ).block();
                    }
                }
                default -> {
                    logger.warn("⚠️ Unsupported message type: {}", message.getMessageType());
                    return;
                }
            }

            // Send response back to WhatsApp
            if (response != null && !response.isEmpty()) {
                whatsAppDeliveryService.sendMessage(message.getSenderPhone(), response)
                        .block();
                logger.info("✅ Response sent to WhatsApp user: {}", message.getSenderPhone());
            }

        } catch (Exception e) {
            logger.error("❌ Error processing WhatsApp message: {}", e.getMessage(), e);
            // Try to send error message back to user
            try {
                whatsAppDeliveryService.sendMessage(
                        message.getSenderPhone(),
                        "Sorry, I encountered an error processing your message. Please try again."
                ).block();
            } catch (Exception sendError) {
                logger.error("❌ Failed to send error message to user: {}", sendError.getMessage());
            }
            throw e;
        }
    }

    /**
     * Process Web messages - placeholder for web chat processing.
     */
    private void processWebMessage(ChatQueueMessage message) {
        logger.debug("🌐 Processing Web message for session: {}", message.getSessionId());
        // Web messages can be handled through WebSocket or polling
        // For now, just process with chat service
        chatService.chat(message.getSessionId(), message.getText()).block();
    }

    /**
     * Process API messages - placeholder for API-based chat processing.
     */
    private void processApiMessage(ChatQueueMessage message) {
        logger.debug("🔌 Processing API message for session: {}", message.getSessionId());
        // API messages might need callback URLs or other delivery mechanisms
        chatService.chat(message.getSessionId(), message.getText()).block();
    }
}
