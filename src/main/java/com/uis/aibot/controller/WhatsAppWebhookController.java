package com.uis.aibot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.uis.aibot.dto.ChatQueueMessage;
import com.uis.aibot.service.ChatMessageProducer;
import com.uis.aibot.service.WhatsAppDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WebClient webClient;
    private final ChatMessageProducer messageProducer;
    private final WhatsAppDeliveryService whatsAppDeliveryService;

    @Value("${whatsapp.verify.token}")
    private String verifyToken;

    @Value("${whatsapp.access.token}")
    private String accessToken;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public WhatsAppWebhookController(WebClient.Builder builder,
                                      ChatMessageProducer messageProducer,
                                      WhatsAppDeliveryService whatsAppDeliveryService) {
        this.webClient = builder.build();
        this.messageProducer = messageProducer;
        this.whatsAppDeliveryService = whatsAppDeliveryService;
    }

    // =============================
    // 1️⃣ Verification Endpoint (GET /webhook)
    // =============================
    @GetMapping
    public String verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        logger.info("🔐 Verification request - mode: {}, token match: {}", mode, verifyToken.equals(token));

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            logger.info("✅ Verification successful");
            return challenge;
        }

        logger.warn("❌ Verification failed - mode: {}, token match: {}", mode, verifyToken.equals(token));
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    // =============================
    // 🧪 Test Endpoint (GET /webhook/test)
    // =============================
    @GetMapping("/test")
    public Mono<String> testWhatsAppConnection(@RequestParam(required = false) String phone) {
        logger.info("🧪 Testing WhatsApp API connection");
        logger.info("📋 Phone Number ID: {}", phoneNumberId);
        logger.info("🔐 Access Token configured: {}", accessToken != null && !accessToken.isEmpty());

        if (phone == null || phone.isEmpty()) {
            return Mono.just("Test endpoint ready. Add ?phone=1234567890 to send a test message");
        }

        return whatsAppDeliveryService.sendMessage(phone, "Test message from aibot 🤖")
                .thenReturn("✅ Test message sent to " + phone)
                .onErrorResume(e -> Mono.just("❌ Failed to send test message: " + e.getMessage()));
    }

    // =============================
    // 2️⃣ Receive Incoming Messages (POST /webhook)
    // =============================
    @PostMapping
    public Mono<String> receive(@RequestBody JsonNode payload) {
        logger.debug("📥 Received webhook payload: {}", payload.toPrettyString());

        JsonNode messageNode = payload
                .path("entry").get(0)
                .path("changes").get(0)
                .path("value")
                .path("messages").get(0);

        if (messageNode == null || messageNode.isMissingNode()) {
            logger.debug("No message found in payload, returning EVENT_RECEIVED");
            return Mono.just("EVENT_RECEIVED");
        }

        String from = messageNode.path("from").asText();
        String messageType = messageNode.path("type").asText();

        logger.info("📨 Received {} message from {}", messageType, from);

        return switch (messageType) {
            case "text" -> handleTextMessage(messageNode, from);
            case "image" -> handleImageMessage(messageNode, from);
            case "document" -> handleDocumentMessage(messageNode, from);
            default -> {
                logger.warn("⚠️ Unsupported message type: {}", messageType);
                yield Mono.just("EVENT_RECEIVED");
            }
        };
    }

    // =============================
    // Handle Text Messages
    // =============================
    private Mono<String> handleTextMessage(JsonNode messageNode, String from) {
        String text = messageNode.path("text").path("body").asText();
        logger.info("💬 Text message: {}", text);

        // Create normalized message and enqueue for async processing
        ChatQueueMessage queueMessage = ChatQueueMessage.forWhatsAppText(from, text);
        messageProducer.sendMessage(queueMessage);

        logger.info("📤 Text message enqueued for processing: {}", queueMessage.getMessageId());
        return Mono.just("EVENT_RECEIVED");
    }

    // =============================
    // Handle Image Messages
    // =============================
    private Mono<String> handleImageMessage(JsonNode messageNode, String from) {
        String mediaId = messageNode.path("image").path("id").asText();
        String mimeType = messageNode.path("image").path("mime_type").asText();
        String caption = messageNode.path("image").path("caption").asText("");

        logger.info("🖼️ Image received - ID: {}, MIME: {}, Caption: '{}'", mediaId, mimeType, caption);

        return downloadMedia(mediaId, mimeType)
                .flatMap(filePath -> {
                    logger.info("📁 Image saved to: {}", filePath);

                    // Create normalized message and enqueue for async processing
                    ChatQueueMessage queueMessage = ChatQueueMessage.forWhatsAppImage(
                            from, caption, filePath, mimeType);
                    messageProducer.sendMessage(queueMessage);

                    logger.info("📤 Image message enqueued for processing: {}", queueMessage.getMessageId());
                    return Mono.just("EVENT_RECEIVED");
                })
                .onErrorResume(e -> {
                    logger.error("❌ Error handling image: {}", e.getMessage(), e);
                    return Mono.just("EVENT_RECEIVED");
                });
    }

    // =============================
    // Handle Document Messages
    // =============================
    private Mono<String> handleDocumentMessage(JsonNode messageNode, String from) {
        String mediaId = messageNode.path("document").path("id").asText();
        String mimeType = messageNode.path("document").path("mime_type").asText();
        String filename = messageNode.path("document").path("filename").asText("");
        String caption = messageNode.path("document").path("caption").asText("");

        logger.info("📄 Document received - ID: {}, MIME: {}, Filename: {}, Caption: '{}'",
                mediaId, mimeType, filename, caption);

        return downloadMedia(mediaId, mimeType)
                .flatMap(filePath -> {
                    logger.info("📁 Document saved to: {}", filePath);

                    // Create normalized message and enqueue for async processing
                    ChatQueueMessage queueMessage = ChatQueueMessage.forWhatsAppDocument(
                            from, caption, filePath, mimeType, filename);
                    messageProducer.sendMessage(queueMessage);

                    logger.info("📤 Document message enqueued for processing: {}", queueMessage.getMessageId());
                    return Mono.just("EVENT_RECEIVED");
                })
                .onErrorResume(e -> {
                    logger.error("❌ Error handling document: {}", e.getMessage(), e);
                    return Mono.just("EVENT_RECEIVED");
                });
    }

    // =============================
    // Download Media from WhatsApp
    // =============================
    private Mono<String> downloadMedia(String mediaId, String mimeType) {
        logger.debug("⬇️ Downloading media ID: {}", mediaId);

        // Step 1: Get media URL from WhatsApp API
        return webClient.get()
                .uri("https://graph.facebook.com/v19.0/" + mediaId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(response -> {
                    String mediaUrl = response.path("url").asText();
                    logger.debug("📍 Media URL: {}", mediaUrl);

                    // Step 2: Download the actual file
                    return webClient.get()
                            .uri(mediaUrl)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToFlux(DataBuffer.class)
                            .collectList()
                            .flatMap(dataBuffers ->
                                    Mono.fromCallable(() -> {
                                        // Create upload directory if it doesn't exist
                                        Path uploadPath = Paths.get(uploadDir);
                                        Files.createDirectories(uploadPath);

                                        // Generate filename with extension from MIME type
                                        String extension = getFileExtension(mimeType);
                                        String filename = mediaId + extension;
                                        Path filePath = uploadPath.resolve(filename);

                                        logger.debug("💾 Saving file to: {}", filePath);
                                        return filePath;
                                    }).flatMap(filePath ->
                                            // Write file using reactive DataBuffer
                                            DataBufferUtils.write(
                                                    Flux.fromIterable(dataBuffers),
                                                    filePath,
                                                    StandardOpenOption.CREATE,
                                                    StandardOpenOption.TRUNCATE_EXISTING
                                            )
                                                    .then(Mono.just(filePath.toString()))
                                                    .doOnSuccess(path -> logger.info("✅ File saved successfully: {}", path))
                                    )
                            );
                });
    }

    // =============================
    // Get File Extension from MIME Type
    // =============================
    private String getFileExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            case "application/vnd.ms-powerpoint" -> ".ppt";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "text/plain" -> ".txt";
            case "audio/ogg" -> ".ogg";
            case "audio/mpeg" -> ".mp3";
            case "video/mp4" -> ".mp4";
            default -> "";
        };
    }
}