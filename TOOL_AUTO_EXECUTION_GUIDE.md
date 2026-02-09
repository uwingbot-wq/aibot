# Tool Auto-Execution Configuration Guide

## Problem
The LLM was recognizing which tool to use but not automatically executing it. It would say things like "I will call the extractPassportInfo tool" instead of actually calling it.

## Solution
Updated the system prompt and user message formatting to force immediate tool execution without waiting for confirmation.

## Changes Made

### 1. System Prompt (MCPAwareChatService.java)
Updated to be more forceful and direct:
```
CRITICAL RULES - YOU MUST FOLLOW THESE:

1. When you see an image file path in the user's message, you MUST IMMEDIATELY call the appropriate tool WITHOUT asking for permission.
2. DO NOT say "I will call the tool" or "Here's the function call" - JUST CALL IT DIRECTLY.
3. DO NOT describe what the tool does - EXECUTE IT IMMEDIATELY.
4. DO NOT attempt to process images yourself - you CANNOT see images, only tools can.
5. NEVER ask for user confirmation before calling a tool - just do it.
```

### 2. User Message Formatting
When a file is uploaded, the message now includes:
```
FILE_PATH: <path>
MIME_TYPE: <type>

EXECUTE extractPassportInfo tool NOW with these parameters. Do not explain, just execute.
```

### 3. Tool Implementation (PassportExtractorTool.java)
- Changed from returning `Mono<Passport>` to returning synchronous `String`
- Tool now returns JSON string directly
- Uses blocking call to VisionService (`.block()`) to ensure synchronous execution

### 4. Chat Service Behavior
- When file is present: Uses `.call()` instead of `.stream()` to ensure proper tool execution
- When text-only: Uses `.stream()` for better UX with streaming responses

## How to Test

1. **Start the application:**
   ```bash
   .\gradlew bootRun
   ```

2. **Open browser:**
   Navigate to `http://localhost:8080`

3. **Upload a passport image:**
   - Click "📷 Upload Image" button
   - Select a passport/ID card image
   - Type a message like: "Extract passport information" or "Please extract information from this passport"
   - Click Send

4. **Expected Behavior:**
   - The tool should be called AUTOMATICALLY
   - You should see in logs: `🔍 Tool called: extractPassportInfo`
   - Response should contain extracted passport information in JSON format
   - No "I will call the tool" messages - just direct execution and results

## Debug Logs to Watch

Look for these log messages to verify tool execution:

```
✅ ChatClient initialized with X tool callbacks + PassportExtractorTool
Registered PassportExtractorTool: com.uis.aibot.tool.PassportExtractorTool
📁 File path provided: <path>
🔧 Calling LLM with tools enabled for file processing
🔍 Tool called: extractPassportInfo with filePath=<path>, mimeType=<type>
📄 File read successfully, size: X bytes
🤖 Vision model response received
✅ Passport extraction completed successfully: <name>
```

## Common Issues

### Tool Still Not Executing
1. Check that Ollama is running with a tool-capable model (llama3.1 or newer)
2. Verify the model supports function calling
3. Check logs for tool registration messages

### Tool Executes But Fails
1. Verify llama3.2-vision model is available for VisionService
2. Check file permissions on uploaded images
3. Verify file path is correct in logs

## Model Requirements

- **Text Model (for chat):** llama3.1 or newer (supports tool calling)
- **Vision Model (for image analysis):** llama3.2-vision
- Both models must be pulled in Ollama before running the application

## Configuration

In `application.properties`:
```properties
spring.ai.ollama.chat.model=llama3.1
spring.ai.ollama.base-url=http://localhost:11434
ollama.vision.model=llama3.2-vision
ollama.vision.base-url=http://192.168.88.7:11434
```

Make sure both Ollama instances are running and have the required models.
