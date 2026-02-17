package com.uis.aibot.config;

import io.modelcontextprotocol.client.McpAsyncClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class McpVerifier {
    public McpVerifier(List<McpAsyncClient> clients) {
        // This will print the names of all clients detected by Spring
        clients.forEach(client ->
                System.out.println("Active MCP Client found: " + client.getClass().getSimpleName())
        );
    }
}