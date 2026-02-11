package com.uis.aibot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    private final WebClient webClient;

    @Value("${whatsapp.verify.token}")
    private String verifyToken;

    @Value("${whatsapp.access.token}")
    private String accessToken;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    public WhatsAppWebhookController(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    // =============================
    // 1️⃣ Verification Endpoint
    // =============================
    @GetMapping
    public String verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return challenge;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    // =============================
    // 2️⃣ Receive Incoming Messages
    // =============================
    @PostMapping
    public Mono<String> receive(@RequestBody JsonNode payload) {

        JsonNode messageNode = payload
                .path("entry").get(0)
                .path("changes").get(0)
                .path("value")
                .path("messages").get(0);

        if (messageNode == null || messageNode.isMissingNode()) {
            return Mono.just("EVENT_RECEIVED");
        }

        String from = messageNode.path("from").asText();
        String text = messageNode.path("text").path("body").asText();

        return generateAIResponse(text)
                .flatMap(reply -> sendWhatsAppMessage(from, reply))
                .thenReturn("EVENT_RECEIVED");
    }

    // =============================
    // 3️⃣ Call Ollama
    // =============================
    private Mono<String> generateAIResponse(String prompt) {

        return webClient.post()
                .uri("http://localhost:11434/api/generate")
                .bodyValue(Map.of(
                        "model", "llama3.1:latest",
                        "prompt", prompt,
                        "stream", false
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.path("response").asText());
    }

    // =============================
    // 4️⃣ Send Message Back to WhatsApp
    // =============================
    private Mono<Void> sendWhatsAppMessage(String to, String text) {

        return webClient.post()
                .uri("https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of(
                        "messaging_product", "whatsapp",
                        "to", to,
                        "type", "text",
                        "text", Map.of("body", text)
                ))
                .retrieve()
                .bodyToMono(Void.class);
    }
}