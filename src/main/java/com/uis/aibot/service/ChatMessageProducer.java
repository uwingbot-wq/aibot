package com.uis.aibot.service;

import com.uis.aibot.config.RabbitMQConfig;
import com.uis.aibot.dto.ChatQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service to publish chat messages to RabbitMQ queue.
 */
@Service
public class ChatMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public ChatMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends a chat message to the queue for async processing.
     *
     * @param message The chat message to enqueue
     */
    public void sendMessage(ChatQueueMessage message) {
        logger.info("📤 Enqueueing message: {} from {} via {}",
                    message.getMessageId(),
                    message.getSenderPhone(),
                    message.getSource());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_ROUTING_KEY,
                message
        );

        logger.debug("✅ Message {} enqueued successfully", message.getMessageId());
    }
}
