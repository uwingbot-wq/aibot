# Quick Start - Image Chat Feature

## Prerequisites
1. ✅ Ollama installed and running
2. ✅ `llama3.2-vision` model installed: `ollama pull llama3.2-vision`
3. ✅ `llama3.1:latest` model installed: `ollama pull llama3.1:latest`
4. ✅ Java 21+ installed
5. ✅ Gradle installed (or use gradlew)

## Start the Application

### Option 1: Using Gradle Wrapper (Recommended)
```bash
cd C:\Apac\aibot
.\gradlew bootRun
```

### Option 2: Build and Run JAR
```bash
cd C:\Apac\aibot
.\gradlew build -x test
java -jar build/libs/aibot-0.0.1-SNAPSHOT.jar
```

## Access the Chat Interface
Open your browser and navigate to:
```
http://localhost:8080
```

## How to Use

### Text-Only Chat
1. Type your message in the input field
2. Press Enter or click "Send"
3. AI responds using llama3.1 model

### Chat with Images
1. Click **"📷 Upload Image"** button
2. Select an image from your computer
3. Preview appears - you can remove it with the × button
4. Type your message (e.g., "What's in this image?")
5. Click **"Send"**
6. AI analyzes the image using llama3.2-vision model and responds

### Extract Passport Information
1. Click **"📷 Upload Image"**
2. Select a passport image
3. Type: **"Please extract passport information from this image"**
4. Click **"Send"**
5. AI uses PassportExtractorTool to extract:
   - Passport Number
   - Name
   - Birthdate
   - Gender
   - Nationality
   - Issue Date
   - Expiry Date

## Supported Image Formats
- ✅ JPEG (.jpg, .jpeg)
- ✅ PNG (.png)
- ✅ GIF (.gif)
- ✅ WebP (.webp)
- ✅ BMP (.bmp)

## Example Prompts

### General Image Analysis
- "Describe this image in detail"
- "What objects do you see?"
- "What is the mood of this image?"
- "Identify the main subject"

### Document Analysis
- "Extract all text from this document"
- "Summarize this receipt"
- "What information is on this form?"

### Passport/ID Extraction
- "Extract passport information"
- "Get the details from this ID"
- "What's the expiry date on this passport?"

### Chart/Data Analysis
- "What trends do you see in this chart?"
- "Explain this graph"
- "What are the key data points?"

## Configuration

### Check/Update application.properties
Location: `src/main/resources/application.properties`

```properties
# Ollama Base URL
spring.ai.ollama.base-url=http://192.168.88.7:11434

# Regular Chat Model
spring.ai.ollama.chat.options.model=llama3.1:latest

# Vision Model Settings
vision.ollama.base-url=http://192.168.88.7:11434
vision.ollama.model=llama3.2-vision
vision.ollama.stream=true
vision.ollama.temperature=0.2

# MCP Configuration (optional)
spring.ai.mcp.client.enabled=true
```

### Change Ollama Server URL
If your Ollama is running on a different host:
```properties
spring.ai.ollama.base-url=http://localhost:11434
vision.ollama.base-url=http://localhost:11434
```

## Troubleshooting

### Issue: "Error processing your request"
**Cause:** Ollama not running or model not available
**Solution:**
```bash
# Check Ollama is running
ollama list

# Ensure models are installed
ollama pull llama3.1:latest
ollama pull llama3.2-vision

# Start Ollama (if not running)
ollama serve
```

### Issue: Image not uploading
**Cause:** File size too large or wrong format
**Solution:**
- Compress image to under 5MB
- Convert to JPEG or PNG format
- Check browser console for errors

### Issue: Passport extraction not working
**Cause:** llama3.2-vision model not available
**Solution:**
```bash
ollama pull llama3.2-vision
```

### Issue: Build fails
**Cause:** Missing dependencies or wrong Java version
**Solution:**
```bash
# Check Java version (should be 21+)
java -version

# Clean and rebuild
.\gradlew clean build -x test
```

### Issue: Port 8080 already in use
**Cause:** Another application using port 8080
**Solution:** Change port in application.properties
```properties
server.port=8081
```

## Advanced Features

### Streaming Responses
Use the streaming endpoint for real-time responses:
```javascript
// In your custom client
const eventSource = new EventSource('/api/chat/stream');
eventSource.onmessage = (event) => {
    console.log('Chunk:', event.data);
};
```

### Session Management
Each browser session gets a unique ID stored in sessionStorage.
Clear history via:
```bash
DELETE http://localhost:8080/api/chat/history/{sessionId}
```

### Tool Integration
The PassportExtractorTool is automatically available. The AI will invoke it when needed based on user requests.

## API Endpoints

### POST /api/chat
Single response for chat messages
```json
{
  "message": "Hello",
  "sessionId": "session_123",
  "imageBase64": "...",      // Optional
  "mediaType": "image/jpeg"  // Optional
}
```

### POST /api/chat/stream
Streaming response (Server-Sent Events)
Same request format as /api/chat

### DELETE /api/chat/history/{sessionId}
Clear conversation history for a session

## Performance Tips

1. **Image Size**: Resize large images before upload
2. **Format**: Use JPEG for photos, PNG for screenshots
3. **Quality**: Balance quality vs. processing time
4. **Batch**: Process multiple similar images in sequence
5. **Cache**: Ollama caches models in memory for faster responses

## Next Steps

1. **Customize Prompts**: Modify tool descriptions in PassportExtractorTool
2. **Add More Tools**: Create additional @Tool annotated methods
3. **UI Enhancements**: Customize chat.html CSS
4. **Add Features**: Support multiple images, drag-and-drop, etc.
5. **Production Deploy**: Configure for production environment

## Support

For issues or questions:
- Check logs: `build/startup.log`
- Review documentation: `IMAGE_CHAT_IMPLEMENTATION.md`
- See examples: `IMAGE_CHAT_EXAMPLES.md`

## Success Indicators

✅ Application starts without errors
✅ Browser shows chat interface
✅ Text messages work
✅ Image upload shows preview
✅ AI responds to image queries
✅ Passport extraction returns structured data
✅ Conversation history maintained
✅ No blocking errors in reactive chain

---

**Congratulations! Your image chat feature is ready to use! 🎉**
