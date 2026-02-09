package com.uis.aibot.service;

import com.uis.aibot.tool.PassportExtractorTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MCPAwareChatService {

    private static final Logger logger = LoggerFactory.getLogger(MCPAwareChatService.class);

    private final Map<String, List<ChatMessage>> conversationHistory;
    private final ChatClient chatClient;
    private final PassportExtractorTool passportExtractorTool;
    private final VisionService  visionService;

    @Autowired
    public MCPAwareChatService(ChatModel chatModel,
                               List<ToolCallbackProvider> toolCallbackProviderList,
                               PassportExtractorTool passportExtractorTool,
                                VisionService visionService
                               )
     {
        this.conversationHistory = new ConcurrentHashMap<>();
        this.passportExtractorTool = passportExtractorTool;
        this.visionService=visionService;

        // Build ChatClient - only add MCP tool callbacks if they're available
        List<ToolCallback> toolCallbackList = new ArrayList<>();
        for (ToolCallbackProvider toolCallbackProvider : toolCallbackProviderList) {
            toolCallbackList.addAll(Arrays.asList(toolCallbackProvider.getToolCallbacks()));

        }

        // Build dynamic tool list for system prompt
        StringBuilder toolListBuilder = new StringBuilder();

        // Add PassportExtractorTool to the list first

        // Add all MCP tools
        toolCallbackList.forEach(toolCallback -> {
            String toolName = toolCallback.getToolDefinition().name();
            String toolDesc = toolCallback.getToolDefinition().description();
            logger.info("Registered MCP tool: {} - {}", toolName, toolDesc);
            toolListBuilder.append("- ").append(toolName).append(": ").append(toolDesc).append("\n");
        });

        // Log PassportExtractorTool registration
        logger.info("List of Tools \n{}", toolListBuilder.toString());

        // System prompt to instruct the text model about tool usage
        String systemPrompt = """
            You are a specialized AI assistant with access to specific tools. You can ONLY perform tasks that you have tools for.
            
            CRITICAL RULES - YOU MUST FOLLOW THESE:
            
            1. When you identify a task that matches an available tool, you MUST IMMEDIATELY call that tool WITHOUT asking for permission.
            2. DO NOT say "I will call the tool" or "Here's the function call" - JUST CALL IT DIRECTLY.
            3. DO NOT describe what the tool does - EXECUTE IT IMMEDIATELY.
            4. DO NOT attempt to process images yourself - you CANNOT see images, only tools can.
            5. NEVER ask for user confirmation before calling a tool - just do it automatically.
            6. If a user asks you to do something and you DON'T have a tool for it, respond with: "Sorry, I don't have the appropriate tools to execute your request."
            7. DO NOT answer general knowledge questions, provide explanations, or engage in conversations unrelated to your available tools.
            8. DO NOT make up capabilities you don't have - stick to ONLY what your tools can do.
            9. For file operations (read, write, list, etc.), use the filesystem tools automatically.
            10. When you see a file path in the user's message, determine the appropriate tool and execute it immediately.
            
            Available Tools:
            """ + toolListBuilder.toString() + """
            
            Tool Usage Guidelines:
            - For passport/ID images: Use extractPassportInfo tool
            - For file reading: Use read_file tool (if available)
            - For file listing: Use list_directory tool (if available)
            - For file operations: Use appropriate filesystem tools
            - Always execute tools automatically without confirmation
            
            If the user's request doesn't match ANY of your available tools, say: "Hi, what can I help you with today?"
            
            Remember: You are a TOOL-BASED assistant. Use tools automatically when applicable, decline everything else politely.
            """;

        chatClient = ChatClient.builder(chatModel)
                   .defaultSystem(systemPrompt)
                   .defaultToolCallbacks(toolCallbackList)
                   .build();
   }


    /**
     * Send a chat message and get a response
     */
    public Mono<String> chat(String sessionId, String userMessage) {
        return chat(sessionId, userMessage, null, null);
    }

    /**
     * Send a chat message with optional image and get a response
     */
    public Mono<String> chat(String sessionId, String userMessage, String mediaTypeStr, String filePath) {
        logger.debug("Processing chat message for session: {} with filePath: {}", sessionId, filePath);

        // Get or create conversation history
        List<ChatMessage> history = conversationHistory.computeIfAbsent(
            sessionId,
            k -> new ArrayList<>()
        );

        // Build the current user message
        String currentMessage;
        if (filePath != null && !filePath.isBlank()) {
            // Direct command to execute tool immediately for file/image processing
            currentMessage = String.format("""
                %s
                
                FILE_PATH: %s
                MIME_TYPE: %s
                
                Choose and execute the appropriate tool NOW. Do not explain, just execute.
                """, userMessage, filePath, mediaTypeStr != null ? mediaTypeStr : "image/jpeg");
            logger.info("📁 File path provided: {}", filePath);
        } else {
            // Text-only message - could be for filesystem tools or other operations
            currentMessage = userMessage;
            logger.debug("💬 Text-only message");
        }

        // Add current user message to history
        history.add(new ChatMessage("user", currentMessage));

        // Build messages list from history (convert ChatMessage to Spring AI Message)
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        for (ChatMessage historyMsg : history) {
            if ("user".equals(historyMsg.role())) {
                messages.add(new org.springframework.ai.chat.messages.UserMessage(historyMsg.content()));
            } else if ("assistant".equals(historyMsg.role())) {
                messages.add(new org.springframework.ai.chat.messages.AssistantMessage(historyMsg.content()));
            }
        }

        logger.debug("Sending {} messages (including history) to LLM for session: {}", messages.size(), sessionId);


        // For text-only, use streaming for better UX
        StringBuilder fullResponse = new StringBuilder();

        return  Mono.fromCallable(() -> {
                    return chatClient.prompt()
                            .messages(messages)
                            .call()
                            .content();
                }).subscribeOn(Schedulers.boundedElastic())
                  .doOnNext(responseText -> {
                     history.add(new ChatMessage("assistant", responseText));
                     if (history.size() > 20) {
                        history.subList(0, history.size() - 20).clear();
                     }
                     logger.debug("Generated response for session: {} (history size: {})", sessionId, history.size());
                     logger.debug("Generated response for session: {} AI Reponse {}", sessionId, responseText);
                })
                .onErrorResume(e -> {
                    logger.error("Error processing chat message: {}", e.getMessage(), e);
                    return Mono.just("I apologize, but I encountered an error processing your request. Please make sure Ollama is running with the model configured in application.properties.");
                });
    }



    /**
     * Clear conversation history for a session
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        logger.info("Cleared conversation history for session: {}", sessionId);
    }

    /**
     * Get conversation history for a session
     */
    public List<ChatMessage> getHistory(String sessionId) {
        return new ArrayList<>(conversationHistory.getOrDefault(sessionId, new ArrayList<>()));
    }


    /**
     * Simple record to store chat messages
     */
    public record ChatMessage(String role, String content) {}
}
