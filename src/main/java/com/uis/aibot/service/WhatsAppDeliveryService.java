package com.uis.aibot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Service to deliver messages back to WhatsApp users.
 */
@Service
public class WhatsAppDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppDeliveryService.class);

    private final WebClient webClient;

    @Value("${whatsapp.access.token}")
    private String accessToken;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    public WhatsAppDeliveryService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Send a text message to a WhatsApp user.
     *
     * @param to   The recipient phone number
     * @param text The message text
     * @return A Mono that completes when the message is sent
     */
    public Mono<Void> sendMessage(String to, String text) {
        if (to == null || to.isEmpty()) {
            logger.error("❌ Cannot send message: 'to' is null or empty");
            return Mono.error(new IllegalArgumentException("Recipient phone number is required"));
        }

        if (text == null || text.isEmpty()) {
            logger.error("❌ Cannot send message: 'text' is null or empty");
            return Mono.error(new IllegalArgumentException("Message text is required"));
        }

        logger.debug("📤 Sending WhatsApp message to {}: {}...", to,
                     text.substring(0, Math.min(text.length(), 50)));

        Map<String, Object> requestBody = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );

        return webClient.post()
                .uri("https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("❌ WhatsApp API error response: {}", errorBody);
                                    return Mono.error(new RuntimeException("WhatsApp API error: " + errorBody));
                                }))
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.info("✅ WhatsApp message sent successfully to {}", to))
                .doOnError(e -> logger.error("❌ Failed to send WhatsApp message to {}: {}", to, e.getMessage()));
    }

    /**
     * Send an image message to a WhatsApp user.
     *
     * @param to       The recipient phone number
     * @param imageUrl The URL of the image to send
     * @param caption  Optional caption for the image
     * @return A Mono that completes when the message is sent
     */
    public Mono<Void> sendImage(String to, String imageUrl, String caption) {
        logger.debug("📤 Sending WhatsApp image to {}: {}", to, imageUrl);

        Map<String, Object> imageData = caption != null && !caption.isEmpty()
                ? Map.of("link", imageUrl, "caption", caption)
                : Map.of("link", imageUrl);

        Map<String, Object> requestBody = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "image",
                "image", imageData
        );

        return webClient.post()
                .uri("https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("❌ WhatsApp API error response: {}", errorBody);
                                    return Mono.error(new RuntimeException("WhatsApp API error: " + errorBody));
                                }))
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.info("✅ WhatsApp image sent successfully to {}", to))
                .doOnError(e -> logger.error("❌ Failed to send WhatsApp image to {}: {}", to, e.getMessage()));
    }
}
