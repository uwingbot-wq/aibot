# Quick Start Guide

## What I've Built

A fully functional AI chatbot application using:
- **Ollama** as the LLM provider
- **Spring Boot 4.0.2** with WebFlux (reactive)
- **Spring AI** for Ollama integration
- **Modern chat UI** with Thymeleaf

## Files Created/Modified

### Backend
1. **ChatController.java** - REST API endpoints for chat
2. **MCPAwareChatService.java** - Chat service with conversation history
3. **ChatRequest.java** - DTO for chat requests
4. **ChatResponse.java** - DTO for chat responses
5. **McpConfig.java** - MCP configuration (placeholder for future)

### Frontend
6. **chat.html** - Modern, responsive chat interface

### Configuration
7. **application.properties** - Ollama and server configuration
8. **build.gradle** - Added Ollama and SLF4J dependencies

### Documentation
9. **README.md** - Comprehensive setup and usage guide
10. **QUICKSTART.md** - This file

## Before You Start

### 1. Install Ollama

**Windows:**
- Download from https://ollama.com/download/windows
- Run the installer
- Ollama will start automatically

**Mac:**
```bash
brew install ollama
```

**Linux:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### 2. Pull a Model

```bash
ollama pull llama3.2
```

Other recommended models:
- `ollama pull mistral` - Fast and efficient
- `ollama pull phi3` - Smaller, good for quick responses
- `ollama pull llama3.1` - Latest Llama model

### 3. Verify Ollama is Running

```bash
ollama list
```

You should see your downloaded models listed.

## Running the Application

### Start the Application

**Windows PowerShell:**
```powershell
cd C:\Apac\aibot
.\gradlew.bat bootRun
```

**Mac/Linux:**
```bash
cd /path/to/aibot
./gradlew bootRun
```

### Access the Chat

Open your browser:
```
http://localhost:8080
```

## Testing the API

### Send a Chat Message

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, who are you?", "sessionId": "test-123"}'
```

### Stream a Response

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "Tell me a short story", "sessionId": "test-123"}'
```

### Clear History

```bash
curl -X DELETE http://localhost:8080/api/chat/history/test-123
```

## Features Implemented

✅ **Real-time Chat** - Interactive conversation with AI
✅ **Session Management** - Maintains conversation history per session
✅ **Streaming Support** - Real-time response streaming
✅ **Error Handling** - Graceful error messages
✅ **Modern UI** - Beautiful gradient design with animations
✅ **Responsive Design** - Works on desktop and mobile
✅ **RESTful API** - Full API for integration
✅ **Conversation History** - Maintains context (last 20 messages)
✅ **Logging** - SLF4J logging throughout

## Configuration Options

Edit `src/main/resources/application.properties`:

```properties
# Change the model
spring.ai.ollama.chat.options.model=mistral

# Adjust temperature (0.0 = deterministic, 2.0 = creative)
spring.ai.ollama.chat.options.temperature=0.9

# Change server port
server.port=9090

# Change Ollama URL (if running remotely)
spring.ai.ollama.base-url=http://your-server:11434
```

## Troubleshooting

### "Connection refused" Error
- Make sure Ollama is running: `ollama serve`
- Check Ollama status: `ollama list`

### Model Not Found
- Pull the model: `ollama pull llama3.2`
- Verify model name matches in application.properties

### Port Already in Use
- Change port in application.properties
- Or stop the service using port 8080

### Build Errors
- Ensure Java 21 is installed: `java -version`
- Clean build: `.\gradlew.bat clean build`

## Next Steps

### Enhance the Application

1. **Add Authentication**
   - Implement Spring Security
   - Add user login/registration

2. **Persist Conversations**
   - Add database (MongoDB, PostgreSQL)
   - Store conversation history

3. **Enable MCP**
   - Configure MCP server
   - Add tool calling capabilities

4. **Add More Features**
   - File upload support
   - Image generation
   - Voice input/output
   - Multi-language support

5. **Deploy**
   - Dockerize the application
   - Deploy to cloud (AWS, Azure, GCP)
   - Set up CI/CD pipeline

## Architecture Overview

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────────┐
│  ChatController     │
│  (REST API)         │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ MCPAwareChatService │
│ (Business Logic)    │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│   Spring AI         │
│   (Ollama Client)   │
└──────┬──────────────┘
       │ HTTP
       ▼
┌─────────────────────┐
│   Ollama Server     │
│   (LLM Runtime)     │
└─────────────────────┘
```

## Support

- Ollama Docs: https://ollama.com/docs
- Spring AI Docs: https://docs.spring.io/spring-ai/reference/
- Spring Boot Docs: https://docs.spring.io/spring-boot/reference/

Enjoy your AI chatbot! 🚀
