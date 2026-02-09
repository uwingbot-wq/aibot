# Image Chat - Example Usage

## Example 1: Simple Image Description

**User uploads:** `photo.jpg` (image of a sunset)
**User types:** "Describe this image"

**Request sent to backend:**
```json
{
  "message": "Describe this image",
  "sessionId": "session_1234567890",
  "imageBase64": "/9j/4AAQSkZJRgABAQEA...",
  "mediaType": "image/jpeg"
}
```

**AI Response:**
```
I can see a beautiful sunset over the ocean. The sky is painted with 
vibrant orange and pink hues, reflecting off the calm water. There 
are silhouettes of clouds scattered across the horizon, and the sun 
is just touching the water's edge.
```

---

## Example 2: Passport Information Extraction

**User uploads:** `passport.jpg` (image of a passport)
**User types:** "Please extract passport information from this image"

**How it works:**
1. Frontend sends image + message to `/api/chat`
2. ChatService receives request with image
3. VisionService builds UserMessage with image
4. AI recognizes request for passport extraction
5. AI automatically calls `PassportExtractorTool`
6. Tool uses llama3.2-vision to analyze passport
7. Returns structured Passport object

**AI Response:**
```json
{
  "passport_no": "P1234567",
  "name": "JOHN DOE",
  "birthdate": "1990-05-15",
  "gender": "M",
  "nationality": "USA",
  "issue_date": "2020-01-10",
  "expiry_date": "2030-01-10"
}
```

**Displayed to user:**
```
I've extracted the following information from the passport:

Passport Number: P1234567
Name: JOHN DOE
Date of Birth: 1990-05-15
Gender: M
Nationality: USA
Issue Date: 2020-01-10
Expiry Date: 2030-01-10
```

---

## Example 3: Text-Only Chat (Backward Compatible)

**User types:** "What is the capital of France?"
**No image uploaded**

**Request sent to backend:**
```json
{
  "message": "What is the capital of France?",
  "sessionId": "session_1234567890"
}
```

**AI Response:**
```
The capital of France is Paris. It's one of the most visited cities 
in the world, known for landmarks like the Eiffel Tower, Louvre Museum, 
and Notre-Dame Cathedral.
```

---

## Example 4: Multiple Interactions with Images

### Interaction 1:
**User uploads:** `chart.png`
**User types:** "What trends do you see in this chart?"

**AI Response:**
```
Based on the chart, I can see an upward trend from 2020 to 2023, 
with a sharp increase in Q3 2022. There's a slight dip in Q4 2022, 
followed by recovery in 2023.
```

### Interaction 2 (same session):
**User types:** "What might cause that dip in Q4?"

**AI Response:**
```
The Q4 2022 dip could be attributed to seasonal factors, economic 
conditions at the time, or specific business challenges. Without 
more context about what the chart represents, it's difficult to 
pinpoint the exact cause.
```

---

## Example 5: Error Handling

### Case: Invalid Image Format
**User uploads:** `document.pdf`
**User types:** "What's in this document?"

**Browser blocks:** File input has `accept="image/*"` attribute, 
preventing non-image files from being selected.

### Case: Vision Model Unavailable
**User uploads:** `photo.jpg`
**User types:** "Describe this"

**If llama3.2-vision not running:**
```
I apologize, but I encountered an error processing your request. 
Please make sure Ollama is running with the model configured in 
application.properties.
```

### Case: Malformed Base64
**Corrupt image data sent**

**PassportExtractorTool response:**
```
{} 
// Empty Passport object returned, logged error on backend
```

---

## Example 6: Using Vision with Regular Chat

**User uploads:** `receipt.jpg`
**User types:** "How much did I spend on groceries?"

**AI Response:**
```
According to the receipt, you spent $87.42 on groceries. The main 
items include:
- Fresh produce: $23.50
- Dairy products: $18.75
- Meat: $32.17
- Pantry items: $13.00
```

**User types (no image):** "Is that more than last week?"

**AI Response:**
```
I don't have information about your previous week's grocery spending 
in our current conversation. If you'd like to compare, you could 
upload last week's receipt and I can help you analyze the difference.
```

---

## Frontend Code Example

### HTML
```html
<!-- Image Upload Button -->
<label for="imageUpload" class="image-upload-label">
    📷 Upload Image
</label>
<input type="file" id="imageUpload" accept="image/*" class="image-upload-input"/>

<!-- Preview -->
<div id="imagePreviewContainer" class="image-preview-container" style="display: none;">
    <img id="imagePreview" class="image-preview" alt="Preview"/>
    <button type="button" class="remove-image-btn" id="removeImageBtn">×</button>
</div>
```

### JavaScript
```javascript
// Handle image upload
imageUpload.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file && file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (event) => {
            const base64String = event.target.result;
            selectedImageBase64 = base64String.split(',')[1];
            selectedImageType = file.type;
            
            imagePreview.src = base64String;
            imagePreviewContainer.style.display = 'inline-block';
        };
        reader.readAsDataURL(file);
    }
});

// Send with image
const requestBody = {
    message: message,
    sessionId: sessionId
};

if (selectedImageBase64) {
    requestBody.imageBase64 = selectedImageBase64;
    requestBody.mediaType = selectedImageType;
}

await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(requestBody)
});
```

---

## Backend Code Example

### MCPAwareChatService.java
```java
public Mono<String> chat(String sessionId, String userMessage, 
                         String imageBase64, String mediaTypeStr) {
    UserMessage msg;
    
    if (imageBase64 != null && !imageBase64.isBlank()) {
        // Decode and build vision message
        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
        Resource imageResource = new ByteArrayResource(imageBytes);
        MediaType mediaType = mediaTypeStr != null 
            ? MediaType.parseMediaType(mediaTypeStr) 
            : MediaType.IMAGE_JPEG;
        
        msg = visionService.buildVisionUserMessage(
            userMessage,
            Collections.singletonList(imageResource),
            Collections.singletonList(mediaType)
        );
    } else {
        msg = new UserMessage(userMessage);
    }
    
    return chatClient.prompt()
        .messages(msg)
        .stream()
        .content()
        // ... rest of processing
}
```

### PassportExtractorTool.java
```java
@Tool(description = "Extracts passport information from a base64 encoded image")
public Mono<Passport> extractPassportInfo(String base64Image, MediaType mediaType) {
    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
    Resource imageResource = new ByteArrayResource(imageBytes);
    
    String prompt = "Extract passport information and return JSON...";
    
    return visionService.describeImage(prompt, base64Image, mediaType)
        .map(response -> objectMapper.readValue(cleanJson(response), Passport.class))
        .onErrorReturn(new Passport());
}
```

---

## Testing Commands

### Start Application
```bash
cd C:\Apac\aibot
.\gradlew bootRun
```

### Access Chat Interface
```
http://localhost:8080
```

### Test with cURL
```bash
# Text-only chat
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","sessionId":"test123"}'

# With image (base64 truncated for readability)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message":"Describe this image",
    "sessionId":"test123",
    "imageBase64":"iVBORw0KGgo...",
    "mediaType":"image/png"
  }'
```

---

## Tips

1. **Image Size**: Keep images under 5MB for best performance
2. **Format**: JPEG and PNG work best with vision models
3. **Quality**: Higher resolution = better analysis but slower processing
4. **Prompts**: Be specific - "Extract text" vs "What's in this image?"
5. **Sessions**: Same sessionId maintains conversation context
6. **Tools**: AI automatically calls PassportExtractorTool when appropriate
7. **Streaming**: Use `/api/chat/stream` for real-time responses
