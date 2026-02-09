# Image Chat Feature Implementation Summary

## Overview
Successfully implemented image upload and processing functionality for the AI chatbot, allowing users to send images along with text messages for vision-based AI analysis.

## Changes Made

### 1. Backend Changes

#### A. Updated `ChatRequest.java`
- Added `imageBase64` field for base64-encoded image data
- Added `mediaType` field for specifying image type (e.g., "image/jpeg", "image/png")
- Added `hasImage()` helper method to check if request contains an image
- Maintained backward compatibility with text-only constructor

#### B. Updated `MCPAwareChatService.java`
- **Added VisionService integration**: Injected `VisionService` to handle vision model interactions
- **Enhanced chat() method**: Now supports optional image parameters
  - `chat(sessionId, message)` - text-only (backward compatible)
  - `chat(sessionId, message, imageBase64, mediaType)` - with image support
- **Added chatStream() method**: Stream responses in real-time
  - `chatStream(sessionId, message)` - text-only streaming
  - `chatStream(sessionId, message, imageBase64, mediaType)` - streaming with image
- **Image Processing Logic**:
  - Decodes base64 image to ByteArrayResource
  - Uses VisionService to build UserMessage with image
  - Falls back to text-only message if no image provided

#### C. Updated `ChatController.java`
- Updated `/api/chat` endpoint to pass image data to service
- Updated `/api/chat/stream` endpoint to support streaming with images
- Added logging for image presence in requests

#### D. Completed `PassportExtractorTool.java`
- **Non-blocking implementation** using Reactor's `Mono<Passport>`
- **Vision integration**: Uses `VisionService.describeImage()` to call llama3.2-vision model
- **Input**: Accepts base64 image and MediaType
- **Output**: Returns structured `Passport` object with fields:
  - passport_no
  - name
  - birthdate
  - gender
  - nationality
  - issue_date
  - expiry_date
- **Error handling**: Returns empty Passport object on failures
- **JSON parsing**: Cleans markdown code blocks and parses response

#### E. Created `Passport.java` Model
- Uses Lombok annotations (@Data, @NoArgsConstructor, @AllArgsConstructor)
- Fields with proper Jackson JSON mappings (@JsonProperty)
- Auto-generated getters/setters via Lombok

### 2. Frontend Changes

#### Updated `chat.html`

**CSS Additions:**
- `.image-preview` - Preview uploaded images before sending
- `.image-upload-container` - Container for upload UI
- `.image-upload-label` - Styled file upload button
- `.image-preview-container` - Container with remove button
- `.remove-image-btn` - Delete button for removing images
- `.message-image` - Display images in chat messages

**HTML Additions:**
- File input for image upload (`<input type="file" accept="image/*">`)
- Image preview container with remove button
- Upload button styled as "📷 Upload Image"

**JavaScript Enhancements:**
- **Image upload handling**: FileReader API to convert to base64
- **Image preview**: Shows thumbnail before sending
- **Remove image**: Clear selected image before sending
- **Message display**: Shows images in chat bubbles
- **Request formatting**: Includes imageBase64 and mediaType in API calls
- **Auto-clear**: Removes image after successful send

## How It Works

### User Workflow:
1. **Upload Image** (Optional):
   - Click "📷 Upload Image" button
   - Select image from file system
   - Preview appears with remove option

2. **Type Message**:
   - Enter text in input field
   - Can be used with or without image

3. **Send**:
   - Click "Send" button
   - Message and image sent to backend

4. **Backend Processing**:
   - If image present: Uses VisionService with llama3.2-vision model
   - If text-only: Uses regular ChatModel
   - Response streams back to frontend

5. **Display Response**:
   - User message appears with image (if uploaded)
   - AI response appears after processing
   - Conversation history maintained per session

### Tool Integration:
- `PassportExtractorTool` automatically available via Spring AI's `@Tool` annotation
- When user asks to extract passport info, AI can call the tool
- Tool uses vision model to analyze passport image
- Returns structured Passport object with all fields

## Technical Details

### Non-Blocking Architecture:
- All operations use Reactor's Mono/Flux for reactive, non-blocking execution
- VisionService returns `Mono<String>` for async processing
- PassportExtractorTool returns `Mono<Passport>` to avoid blocking
- Properly integrated with Spring WebFlux

### Image Handling:
- Frontend: FileReader API converts images to base64
- Backend: Base64.getDecoder() converts to byte array
- ByteArrayResource wraps bytes for Spring AI
- VisionService builds proper UserMessage with Media

### Vision Model:
- Configured via `application.properties`:
  - `vision.ollama.model=llama3.2-vision`
  - `vision.ollama.base-url=http://192.168.88.7:11434`
- Uses dedicated ChatClient bean (`visionChatClient`)
- Supports multimodal input (text + images)

## API Endpoints

### POST /api/chat
**Request:**
```json
{
  "message": "What's in this image?",
  "sessionId": "session_12345",
  "imageBase64": "iVBORw0KGgoAAAANS...",  // Optional
  "mediaType": "image/jpeg"                // Optional
}
```

**Response:**
```json
{
  "message": "I can see a passport with...",
  "sessionId": "session_12345"
}
```

### POST /api/chat/stream
Same request format, returns Server-Sent Events stream for real-time responses.

## Testing

### To test image chat:
1. Start application: `.\gradlew bootRun`
2. Open browser: `http://localhost:8080`
3. Click "📷 Upload Image"
4. Select an image
5. Type message: "What's in this image?"
6. Click Send
7. AI will analyze and respond

### To test passport extraction:
1. Upload a passport image
2. Type: "Please extract passport information from this image"
3. The PassportExtractorTool will be invoked automatically
4. Returns structured JSON with all passport fields

## Dependencies Added
- Lombok (already in build.gradle): For @Data annotations

## Configuration Required
Ensure `application.properties` has:
```properties
# Vision Model
vision.ollama.model=llama3.2-vision
vision.ollama.base-url=http://192.168.88.7:11434

# Regular Chat Model
spring.ai.ollama.chat.options.model=llama3.1:latest
spring.ai.ollama.base-url=http://192.168.88.7:11434
```

## Benefits
✅ **Non-blocking**: Fully reactive, doesn't block threads
✅ **Backward compatible**: Text-only chat still works
✅ **Tool integration**: PassportExtractorTool auto-discovered
✅ **User-friendly**: Simple drag-and-drop UI
✅ **Flexible**: Supports any image type
✅ **Structured output**: Passport data as typed objects
✅ **Error resilient**: Graceful error handling throughout

## Future Enhancements
- Support multiple images per message
- Add image compression before upload
- Support image URLs (not just uploads)
- Add image annotation/cropping tools
- Store images in conversation history
- Add file type validation
