package com.uis.aibot.config;

import com.uis.aibot.tool.PassportExtractorTool;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.customizer.McpAsyncClientCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;


import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import java.time.Duration;


/**
 * MCP (Model Context Protocol) Configuration
 * This configuration can be extended to include MCP client setup when needed
 */
@Configuration
public class McpConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpConfig.class);

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model}")
    private String ollamaModel;

    public McpConfig() {
        logger.info("MCP Configuration initialized");
        logger.info("To enable MCP features, configure the MCP server in application.properties");
    }

    @Bean
    public McpAsyncClientCustomizer filesystemTimeoutCustomizer() {
        return (serverName, spec) -> {
            if ("filesystem".equals(serverName)) {
                spec.requestTimeout(Duration.ofMinutes(2)); // Longer timeout for npx download
            }
        };
    }



    @Bean
    public WebClient webClient() {

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(1))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(30));
                    conn.addHandlerLast(new WriteTimeoutHandler(30));
                })
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
                .compress(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public ToolCallbackProvider toolProvider(PassportExtractorTool service) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(service)
                .build();
    }


    @Bean("visionChatClient")
    public ChatClient visionChatClient(VisionOllamaProperties props) {
        // 1. Configure the underlying Netty HttpClient for timeouts/wiretapping
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
                .compress(true);

        // 2. Set options (Ensure only one temperature call)
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(props.getModel()) // e.g., "llava" or "llama3.2-vision"
                .temperature(props.getTemperature())
                .build();

        // 3. Build OllamaApi with BOTH builders to ensure consistent timeouts
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(props.getBaseUrl())
                .restClientBuilder(RestClient.builder()
                        // Map your Netty client/timeout settings here if not using default
                )
                .webClientBuilder(WebClient.builder()
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                )
                .build();

        // 4. Instantiate Model and Client
        OllamaChatModel visionModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(visionModel).build();
    }
}
