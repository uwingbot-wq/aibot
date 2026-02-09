package com.uis.aibot.controller;

import com.uis.aibot.dto.ChatResponse;
import com.uis.aibot.service.MCPAwareChatService;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.apache.tika.mime.MimeType;        // This one has .getExtension()
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;


@Controller

public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final MCPAwareChatService chatService;


    public ChatController(MCPAwareChatService chatService) {
        this.chatService = chatService;
    }

    @Value("${app.upload.dir}")
    private String uploadDir;

    @GetMapping("/")
    public String index() {
        return "chat";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }

    private final MimeTypes tikaRegistry = MimeTypes.getDefaultMimeTypes();

    /**
     * REST endpoint for chat messages - accepts multipart form data with optional file attachment
     */
    @PostMapping(value = "/api/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Mono<ChatResponse> sendMessage(
            @RequestPart("message") String message,
            @RequestPart("sessionId") String sessionId,
            @RequestPart(name = "files", required = false)  Flux<FilePart> fileParts)
    {

       return  storeFiles(fileParts).next().flatMap(
                filePath  -> {
                    Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(filePath);
                    String mimeType = mediaType.map(MediaType::toString)
                                      .orElse("application/octet-stream");
                    return chatService.chat(sessionId, message, mimeType, filePath);
                }).switchIfEmpty(Mono.defer(() -> {
                     logger.info("No files were provided, returning default value");
                     return  chatService.chat(sessionId, message, null, null);
                })).onErrorResume(e -> {
                    logger.error("Error processing chat message: {}", e.getMessage(), e);
                    return Mono.just("I apologize, but I encountered an error processing your request");
                }).flatMap(response ->Mono.just(new ChatResponse(response,sessionId)));

    }

    public Flux<String> storeFiles(@RequestPart("files") Flux<FilePart> fileParts) {
        Path rootPath = Paths.get(uploadDir);
        if(fileParts==null) return Flux.empty();
        return fileParts
                .flatMap(fp -> {

                    String extension = "";
                    try {
                        String contentType = fp.headers().getContentType().toString();
                        MimeType mimeType = tikaRegistry.forName(contentType);
                        extension = mimeType.getExtension(); // e.g., ".jpg"
                    } catch (Exception e) {
                        // Fallback to filename extension if Tika fails
                        String originalName = fp.filename();
                        int dotIndex = originalName.lastIndexOf('.');
                        extension = (dotIndex >= 0) ? originalName.substring(dotIndex) : "";
                    }

                    // 2. Generate UUID Filename
                    String newFilename = UUID.randomUUID().toString() + extension;
                    Path destination = rootPath.resolve(newFilename);

                    // 3. Non-blocking write and return the new name
                    return fp.transferTo(destination)
                            .then(Mono.just(destination.toAbsolutePath().toString()));
                });
    }


    @DeleteMapping("/api/chat/history/{sessionId}")
    @ResponseBody
    public Mono<Void> clearHistory(@PathVariable String sessionId) {
        logger.info("Clearing history for session: {}", sessionId);
        return Mono.fromRunnable(() -> chatService.clearHistory(sessionId));
    }
}
