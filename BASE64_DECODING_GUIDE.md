# Base64 Decoding - Who Does What?

## Quick Answer

**NO** - You do NOT need to decode Base64 in `PassportExtractorTool` or `MCPAwareChatService`.

## Architecture Overview

```
┌───────────────────────────────────────────────────────┐
│                    FRONTEND                           │
│  FileReader → base64 string                           │
│  Sends: {imageBase64: "iVBORw0KGgo..."}              │
└─────────────────────┬─────────────────────────────────┘
                      │ base64 STRING
                      ▼
┌───────────────────────────────────────────────────────┐
│              MCPAwareChatService                      │
│  ✅ Receives: base64 STRING                           │
│  ✅ Passes: base64 STRING to tool                     │
│  ❌ Does NOT decode                                   │
└─────────────────────┬─────────────────────────────────┘
                      │ base64 STRING
                      ▼
┌───────────────────────────────────────────────────────┐
│             PassportExtractorTool                     │
│  ✅ Receives: base64 STRING                           │
│  ✅ Passes: base64 STRING to VisionService            │
│  ❌ Does NOT decode                                   │
└─────────────────────┬─────────────────────────────────┘
                      │ base64 STRING
                      ▼
┌───────────────────────────────────────────────────────┐
│                  VisionService                        │
│  ✅✅✅ THIS IS WHERE DECODING HAPPENS ✅✅✅         │
│                                                       │
│  1. Base64.getDecoder().decode(imageBase64)          │
│     → byte[] imageBytes                              │
│                                                       │
│  2. new ByteArrayResource(imageBytes)                │
│     → Resource imageResource                         │
│                                                       │
│  3. Media.builder().data(imageResource).build()      │
│     → Media object                                   │
│                                                       │
│  4. UserMessage.builder().media(media).build()       │
│     → UserMessage with image                         │
│                                                       │
│  5. visionChatClient.prompt().messages(msg)          │
│     → Sends to llama3.2-vision                       │
└─────────────────────┬─────────────────────────────────┘
                      │ Mono<String> (JSON response)
                      ▼
              Back to PassportExtractorTool
              → Parses JSON to Passport object
```

## Code Examples

### ❌ WRONG - Don't Do This

```java
// ❌ In PassportExtractorTool - WRONG!
@Tool
public Mono<Passport> extractPassportInfo(String base64Image, String mediaType) {
    // DON'T decode here!
    byte[] imageBytes = Base64.getDecoder().decode(base64Image);  // ❌
    Resource imageResource = new ByteArrayResource(imageBytes);    // ❌
    
    // This is VisionService's job!
    UserMessage msg = new UserMessage(...);  // ❌
    
    return ...;
}
```

```java
// ❌ In MCPAwareChatService - WRONG!
public Mono<String> chat(String sessionId, String userMessage, 
                         String imageBase64, String mediaTypeStr) {
    // DON'T decode here!
    byte[] imageBytes = Base64.getDecoder().decode(imageBase64);  // ❌
    Resource imageResource = new ByteArrayResource(imageBytes);    // ❌
    
    return passportExtractorTool.extractPassportInfo(...);
}
```

### ✅ CORRECT - Do This

```java
// ✅ In PassportExtractorTool - CORRECT!
@Tool
public Mono<Passport> extractPassportInfo(String base64Image, String mediaType) {
    String prompt = "Extract passport info...";
    String imageMediaType = (mediaType != null && !mediaType.isEmpty()) 
        ? mediaType : "image/jpeg";
    
    // Just pass the base64 string - VisionService handles the rest
    return visionService.describeImage(prompt, base64Image, imageMediaType)
        .map(response -> parseJson(response));
}
```

```java
// ✅ In MCPAwareChatService - CORRECT!
public Mono<String> chat(String sessionId, String userMessage, 
                         String imageBase64, String mediaTypeStr) {
    boolean isPassportRequest = imageBase64 != null && 
        userMessage.toLowerCase().contains("passport");
    
    if (isPassportRequest) {
        // Just pass the base64 string through
        return passportExtractorTool.extractPassportInfo(imageBase64, mediaTypeStr)
            .map(passport -> formatResponse(passport));
    }
    // ...
}
```

```java
// ✅ In VisionService - CORRECT! (Already implemented)
public Mono<String> describeImage(String prompt, String imageBase64, String mimeTypes) {
    // THIS is where decoding happens
    UserMessage user = buildVisionUserMessage(prompt, imageBase64, mimeTypes);
    return visionChatClient.prompt()
        .messages(user)
        .stream()
        .content()
        .collectList()
        .map(list -> String.join("", list));
}

public UserMessage buildVisionUserMessage(String prompt, 
                                          String imageBase64, 
                                          String mimeTypes) {
    var mediaList = new ArrayList<Media>();
    
    if (imageBase64 != null && !imageBase64.isEmpty()) {
        // HERE is where we decode base64
        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
        Resource imageResource = new ByteArrayResource(imageBytes);
        MimeType type = (mimeTypes != null) 
            ? MimeType.valueOf(mimeTypes) 
            : MediaType.IMAGE_JPEG;
        
        mediaList.add(Media.builder()
            .data(imageResource)
            .mimeType(type)
            .build());
    }
    
    UserMessage.Builder b = UserMessage.builder().text(prompt);
    if (!mediaList.isEmpty()) {
        b.media(mediaList);
    }
    return b.build();
}
```

## Why This Design?

### Principle: Single Responsibility

| Layer | Responsibility | Base64 Handling |
|-------|----------------|-----------------|
| **Frontend** | Convert file to base64 | ✅ Encodes |
| **ChatController** | Route HTTP requests | ⚪ Pass-through |
| **MCPAwareChatService** | Detect intent & orchestrate | ⚪ Pass-through |
| **PassportExtractorTool** | Define what to extract | ⚪ Pass-through |
| **VisionService** | Handle ALL image processing | ✅ Decodes |
| **Vision Model** | Analyze images | ⏺️ Receives binary |

### Benefits

1. **DRY** - Decoding logic in ONE place only
2. **Testability** - Easy to mock VisionService
3. **Maintainability** - Change image handling? Update VisionService only
4. **Separation of Concerns** - Each layer has clear responsibility
5. **Performance** - No unnecessary conversions

### Data Flow

```
File → base64 → base64 → base64 → base64 → DECODE → Resource → Media → Vision Model
  ↑       ↑       ↑       ↑       ↑          ↑
User   Frontend  Ctrl  Service  Tool    VisionService
```

## Common Questions

### Q: Why not decode in the tool?
**A:** The tool defines WHAT to extract, not HOW to process images. Image processing is VisionService's job.

### Q: Should I create a Resource in PassportExtractorTool?
**A:** NO. Just pass the base64 string. VisionService creates the Resource internally.

### Q: What if I need to validate the image first?
**A:** VisionService can be enhanced to validate. Keep validation with processing logic.

### Q: Can I decode base64 in MCPAwareChatService for logging?
**A:** You can log the base64 string length or first few characters, but don't decode the whole thing. Let VisionService handle it.

## Summary

✅ **DO:** Pass base64 strings all the way to VisionService  
✅ **DO:** Let VisionService handle decoding and Resource creation  
✅ **DO:** Keep your tool and service layers clean and simple  

❌ **DON'T:** Decode base64 in PassportExtractorTool  
❌ **DON'T:** Decode base64 in MCPAwareChatService  
❌ **DON'T:** Create Resources outside of VisionService  

**Remember:** VisionService is the ONLY place where base64 → bytes → Resource → Media conversion happens!
