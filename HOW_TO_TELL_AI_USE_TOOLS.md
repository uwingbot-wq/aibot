# How to Tell AI It Cannot Process Images - Must Use Tools

## The Problem

When you have a **text-only model (like llama3.1)** trying to handle image requests, it will:
- ❌ Try to "describe" what it "sees" (but it can't see!)
- ❌ Make up fake descriptions
- ❌ Not call the available tools
- ❌ Confuse users with incorrect responses

## The Solution: Enhanced System Prompt

Use a **strong, explicit system prompt** that tells the AI:
1. What it **CANNOT** do (process images)
2. What it **MUST** do (use tools)
3. **HOW** to use tools (when and with what parameters)

## Implementation

### Step 1: Create a Clear System Prompt

```java
String systemPrompt = """
    You are a helpful AI assistant with access to specialized tools.
    
    ⚠️ CRITICAL LIMITATION - YOU CANNOT PROCESS IMAGES:
    - You are llama3.1, a TEXT-ONLY model
    - You DO NOT have vision capabilities
    - You CANNOT see, analyze, describe, or extract information from images
    - You CANNOT process base64 image data
    - If you try to process images yourself, you will FAIL
    
    ✅ REQUIRED BEHAVIOR WHEN USER UPLOADS AN IMAGE:
    1. NEVER attempt to process the image yourself
    2. NEVER describe what you "see" in an image (you can't see it!)
    3. ALWAYS use the available tools to process images
    4. If the user asks about an image and you don't have the right tool, say:
       "I cannot process images directly. I need to use specialized vision tools."
    
    📋 AVAILABLE TOOLS FOR IMAGE PROCESSING:
    - extractPassportInfo(base64Image, mediaType): 
      * Use when: User uploads a passport/ID image and asks to extract information
      * Trigger words: "extract", "passport", "scan", "read", "get data"
      * This tool uses llama3.2-vision model to analyze passport images
      * ALWAYS call this tool when user wants passport information
    
    🔧 HOW TO USE TOOLS:
    - When you detect a passport extraction request with an image, call extractPassportInfo
    - Pass the image data parameters exactly as provided
    - Let the tool handle all image processing
    - Wait for the tool's response before replying to the user
    
    💬 FOR TEXT-ONLY CONVERSATIONS:
    - Respond normally using your knowledge and capabilities
    - You are excellent at text-based tasks, reasoning, and answering questions
    
    Remember: You are a text model. Images must go to vision tools. Never pretend you can see images.
    """;
```

### Step 2: Apply to ChatClient

```java
chatClient = ChatClient.builder(chatModel)
    .defaultSystem(systemPrompt)  // ← Apply the system prompt
    .defaultToolCallbacks(toolCallbackList)
    .defaultTools(passportExtractorTool)
    .defaultOptions(ToolCallingChatOptions.builder()
        .internalToolExecutionEnabled(true)
        .build())
    .build();
```

### Step 3: Add Safety Net - Direct Detection

Even with the system prompt, add a **detection layer** to call tools directly:

```java
public Mono<String> chat(String sessionId, String userMessage, 
                         String imageBase64, String mediaTypeStr) {
    
    // Detect passport extraction requests
    boolean isPassportExtractionRequest = imageBase64 != null && !imageBase64.isBlank() &&
        (userMessage.toLowerCase().contains("extract") ||
         userMessage.toLowerCase().contains("passport") ||
         userMessage.toLowerCase().contains("scan") ||
         userMessage.toLowerCase().contains("read"));

    if (isPassportExtractionRequest) {
        // DIRECT TOOL INVOCATION - bypass AI, call tool directly
        logger.info("🎯 Detected passport extraction - calling tool directly");
        return passportExtractorTool.extractPassportInfo(imageBase64, mediaTypeStr)
            .map(passport -> formatPassportResponse(passport));
    }
    
    // For other requests, let AI handle with tools
    // ...
}
```

## Why This Approach Works

### 1. **Explicit Limitations**
```
⚠️ CRITICAL LIMITATION - YOU CANNOT PROCESS IMAGES
```
- Uses strong language: "CANNOT", "DO NOT", "WILL FAIL"
- Explicitly names the model: "You are llama3.1, a TEXT-ONLY model"
- Lists specific things it cannot do

### 2. **Clear Instructions**
```
✅ REQUIRED BEHAVIOR WHEN USER UPLOADS AN IMAGE:
1. NEVER attempt to process the image yourself
2. ALWAYS use the available tools
```
- Step-by-step instructions
- Uses imperative language: "NEVER", "ALWAYS", "MUST"
- Numbered list for clarity

### 3. **Tool Documentation**
```
📋 AVAILABLE TOOLS:
- extractPassportInfo(base64Image, mediaType)
  * Use when: [specific scenarios]
  * Trigger words: "extract", "passport", "scan"
```
- Documents each tool
- Explains when to use it
- Provides trigger words for detection
- Shows exact function signature

### 4. **Safety Net - Direct Detection**
Even if the AI ignores the prompt:
- Code detects keywords + image presence
- Calls tool directly with actual data
- Bypasses AI decision-making
- Guarantees correct behavior

## Testing

### Test 1: Passport Extraction
**User:** Uploads passport image + "extract passport information"

**Expected Behavior:**
1. Detection layer catches it → calls tool directly
2. OR AI sees prompt → calls extractPassportInfo tool
3. Vision model processes image
4. Returns structured JSON

✅ **Result:** Passport data extracted correctly

### Test 2: General Image Question
**User:** Uploads photo + "What's in this image?"

**Expected Behavior:**
1. Detection doesn't match passport keywords
2. AI sees system prompt
3. AI says: "I cannot process images directly. I need specialized vision tools."

✅ **Result:** Honest response, no fake descriptions

### Test 3: Text-Only
**User:** "What is the capital of France?"

**Expected Behavior:**
1. No image detected
2. AI responds normally
3. Uses its text knowledge

✅ **Result:** "The capital of France is Paris."

## Key Principles

### ✅ DO:
- **Be explicit** about limitations
- **Use strong language**: "CANNOT", "NEVER", "MUST"
- **Document tools** clearly with examples
- **Provide trigger words** for tool invocation
- **Add safety nets** with code-based detection
- **Test thoroughly** with various inputs

### ❌ DON'T:
- Assume AI will figure it out
- Use vague language: "try to", "should", "prefer"
- Rely solely on the system prompt
- Forget to handle edge cases
- Skip tool documentation in the prompt

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│ System Prompt (First Line of Defense)                  │
│ "You CANNOT process images. You MUST use tools."       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ User Request: "Extract passport info" + image          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ Detection Layer (Second Line of Defense)               │
│ if (keywords match + image present)                    │
│   → Call tool DIRECTLY                                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ PassportExtractorTool                                   │
│ - Receives base64 image                                │
│ - Calls VisionService                                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ VisionService → llama3.2-vision                        │
│ - Decodes base64                                       │
│ - Processes image                                      │
│ - Extracts passport data                               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ Structured JSON Response                               │
│ {passport_no, name, birthdate, ...}                    │
└─────────────────────────────────────────────────────────┘
```

## Common Mistakes to Avoid

### ❌ Mistake 1: Weak Prompt
```java
// BAD - Too vague
String systemPrompt = """
    You should use tools for images.
    Try to call extractPassportInfo when needed.
    """;
```

**Problem:** "should", "try" are weak. AI might ignore.

### ❌ Mistake 2: No Tool Documentation
```java
// BAD - No details
String systemPrompt = """
    Use extractPassportInfo for passports.
    """;
```

**Problem:** AI doesn't know WHEN or HOW to use it.

### ❌ Mistake 3: No Safety Net
```java
// BAD - Relies only on AI
public Mono<String> chat(String sessionId, String userMessage, String imageBase64) {
    UserMessage msg = new UserMessage(userMessage);
    return chatClient.prompt().messages(msg).call().content();
}
```

**Problem:** If AI fails to call tool, user gets wrong answer.

### ✅ Fix: Complete Implementation
```java
// GOOD - Strong prompt + detection + direct invocation
String systemPrompt = """⚠️ YOU CANNOT PROCESS IMAGES...✅ MUST use tools...""";

public Mono<String> chat(...) {
    // Safety net: detect and call directly
    if (isPassportRequest) {
        return passportExtractorTool.extractPassportInfo(imageBase64, mediaType);
    }
    
    // Let AI try with tools (backed by strong prompt)
    return chatClient.prompt().messages(msg).call().content();
}
```

## Summary

### The Complete Strategy:

1. **Strong System Prompt**
   - Explicitly state limitations: "YOU CANNOT PROCESS IMAGES"
   - Use imperative language: "NEVER", "ALWAYS", "MUST"
   - Document available tools with examples
   - Provide trigger words

2. **Code-Based Detection**
   - Detect keywords + image presence
   - Call tools directly when conditions match
   - Don't rely solely on AI judgment

3. **Proper Tool Registration**
   - Register tools with ChatClient
   - Enable internal tool execution
   - Ensure tools are discoverable

4. **Testing & Validation**
   - Test with passport images
   - Test with general images
   - Test text-only conversations
   - Verify AI doesn't hallucinate image descriptions

### Result:
✅ AI understands it cannot process images  
✅ AI delegates to tools when available  
✅ Code ensures correct behavior even if AI fails  
✅ Users get accurate results  
✅ No fake image descriptions  

**Your implementation is now complete and robust!** 🎉
