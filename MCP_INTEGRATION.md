# MCP Integration Guide

## Understanding MCP in This Application

The application is configured to use **Model Context Protocol (MCP)** through Spring AI's `spring-ai-starter-mcp-client-webflux` dependency.

## Current MCP Configuration

### Application Properties

Your `application.properties` has MCP configured:

```properties
# MCP STDIO Configuration
spring.ai.mcp.client.stdio.connections.filesystem.command=cmd.exe
spring.ai.mcp.client.stdio.connections.filesystem.args=/c npx -y @modelcontextprotocol/server-filesystem C:\\Apac

# MCP Client Settings
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.type=ASYNC
spring.ai.mcp.client.toolcallback.enabled=true

# MCP Debugging
logging.level.org.springframework.ai.mcp=DEBUG
logging.level.org.springframework.web.reactive.function.client=DEBUG
```

## How MCP Works in Spring AI 2.0

In Spring AI 2.0.0-M2 with Spring Boot 4.x, MCP integration works through **automatic function calling**:

1. **MCP Client Auto-Configuration**: Spring Boot automatically creates MCP client beans based on your configuration
2. **Tool Discovery**: The MCP client connects to MCP servers (like `@modelcontextprotocol/server-filesystem`)
3. **Function Registration**: MCP tools are registered as Spring AI functions
4. **Automatic Invocation**: When the LLM needs to use a tool, Spring AI automatically calls it

## Verifying MCP is Triggered

### 1. Check Application Startup Logs

When you run `.\gradlew.bat bootRun`, look for these log messages:

```
[MCP] Initializing MCP client connections...
[MCP] Connected to server: filesystem
[MCP] Discovered tools: [list_directory, read_file, write_file, ...]
```

### 2. Enable Debug Logging

Your application already has debug logging enabled. Watch for:

```
DEBUG org.springframework.ai.mcp - MCP client initialized
DEBUG org.springframework.ai.mcp - Tools registered: [...]
DEBUG org.springframework.ai.mcp - Calling tool: read_file with params: {...}
```

### 3. Test MCP Tool Usage

Try these prompts in the chat interface to trigger MCP tools:

```
1. "List the files in C:\\Apac directory"
   → Should trigger filesystem MCP tool

2. "Read the contents of build.gradle file"
   → Should trigger read_file MCP tool

3. "What files are in the current project?"
   → Should use MCP to explore filesystem
```

## Current Status

### ✅ What's Configured

- MCP client dependency added (`spring-ai-starter-mcp-client-webflux`)
- MCP filesystem server configured
- Debug logging enabled
- Tool callbacks enabled

### ⚠️ Known Limitations in Spring AI 2.0.0-M2

The milestone version has limited MCP support:

1. **No Direct MCP Client Bean**: The `McpAsyncClient` class doesn't exist in M2
2. **Auto-Configuration Based**: MCP works through auto-configuration, not manual integration
3. **Function Calling Required**: The LLM must support function calling for MCP to work

## How to Confirm MCP is Working

### Method 1: Check Logs on Startup

Run the application and check logs:

```powershell
.\gradlew.bat bootRun
```

Look for MCP-related messages in the console output.

### Method 2: Test with MCP-Aware Queries

Once the app is running, visit `http://localhost:8080` and ask:

```
"Can you list what files are in the C:\Apac folder?"
```

If MCP is working, you should see:
- Debug logs showing MCP tool invocation
- The AI response will include actual file listings

### Method 3: Monitor the MCP Server Process

Check if the MCP server process is running:

```powershell
Get-Process | Where-Object {$_.ProcessName -like "*node*" -or $_.ProcessName -like "*npx*"}
```

You should see a Node.js process running the MCP filesystem server.

## Troubleshooting

### MCP Client Not Starting

**Symptoms**: No MCP logs on startup

**Solutions**:
1. Ensure Node.js/npm is installed: `node --version`
2. Test MCP server manually:
   ```powershell
   npx -y @modelcontextprotocol/server-filesystem C:\Apac
   ```
3. Check MCP client is enabled: `spring.ai.mcp.client.enabled=true`

### MCP Tools Not Being Called

**Symptoms**: App runs but LLM doesn't use tools

**Solutions**:
1. Use an LLM that supports function calling (like llama3.2)
2. Ask explicit questions that require file system access
3. Check `spring.ai.mcp.client.toolcallback.enabled=true`

### MCP Server Crashes

**Symptoms**: Process starts then stops

**Solutions**:
1. Check the path exists: `C:\Apac`
2. Ensure Node.js has permissions to access the folder
3. Try a different folder or use forward slashes: `C:/Apac`

## Next Steps to Enhance MCP Integration

Once the basic MCP is working, you can:

1. **Add More MCP Servers**:
   ```properties
   spring.ai.mcp.client.stdio.connections.github.command=npx
   spring.ai.mcp.client.stdio.connections.github.args=-y @modelcontextprotocol/server-github
   ```

2. **Create Custom MCP Tools**: Build your own MCP server for domain-specific operations

3. **Monitor MCP Usage**: Add metrics to track which tools are being called

## Current Implementation Notes

The `MCPAwareChatService` is "MCP-aware" in the sense that:
- It uses Spring AI's `StreamingChatModel` which supports MCP integration
- It's configured to work with MCP through application properties
- MCP tools are automatically available to the LLM through Spring AI's function calling

However, the service doesn't directly interact with MCP APIs because:
- Spring AI 2.0.0-M2 handles MCP through auto-configuration
- The MCP client isn't exposed as a bean in this version
- Tool calling happens automatically in the background

## Verification Checklist

- [ ] Application starts without errors
- [ ] MCP debug logs appear in console
- [ ] Node.js process running for MCP server
- [ ] Can query about files in `C:\Apac` directory
- [ ] LLM responses include actual file system data

## Support

If MCP is still not triggering:
1. Check Spring AI documentation for 2.0.0-M2 MCP support
2. Verify the LLM model supports function calling
3. Try upgrading to a stable Spring AI version when available
