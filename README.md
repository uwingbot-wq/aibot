# AI Bot - Ollama Chat Application

A Spring Boot application that integrates with Ollama for AI-powered chat functionality.

## Prerequisites

1. **Java 21** - Required for running the application
2. **Ollama** - Must be installed and running locally
3. **Gradle** - Included via wrapper (gradlew)

## Ollama Setup

### Install Ollama

1. Download and install Ollama from [https://ollama.com](https://ollama.com)

2. Pull a model (e.g., llama3.2):
```bash
ollama pull llama3.2
```

3. Start Ollama service (it usually starts automatically after installation):
```bash
ollama serve
```

Ollama will run on `http://localhost:11434` by default.

## Configuration

The application is configured in `src/main/resources/application.properties`:

```properties
# Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.chat.options.temperature=0.7

# Server Configuration
server.port=8080
```

### Available Models

You can change the model by updating the `spring.ai.ollama.chat.options.model` property. Popular models include:
- `llama3.2` (default)
- `llama3.1`
- `mistral`
- `codellama`
- `phi3`

To see all available models:
```bash
ollama list
```

## Running the Application

### Windows (PowerShell)

```powershell
.\gradlew.bat bootRun
```

### Linux/Mac

```bash
./gradlew bootRun
```

## Accessing the Application

Once the application starts, open your browser and navigate to:

```
http://localhost:8080
```

You'll see a modern chat interface where you can interact with the AI.

## API Endpoints

### Chat Endpoints

1. **POST /api/chat** - Send a message and get a response
   ```json
   {
     "message": "Hello!",
     "sessionId": "session_123"
   }
   ```

2. **POST /api/chat/stream** - Stream chat responses in real-time
   - Content-Type: `text/event-stream`

3. **DELETE /api/chat/history/{sessionId}** - Clear conversation history

### Web Pages

- **GET /** - Main chat interface
- **GET /chat** - Chat interface (alias)

## Features

- ✅ Real-time AI chat with Ollama
- ✅ Conversation history per session
- ✅ Modern, responsive UI
- ✅ Typing indicators
- ✅ Error handling
- ✅ Session management
- ✅ RESTful API
- ✅ Streaming responses support

## Architecture

- **Frontend**: Thymeleaf + Vanilla JavaScript
- **Backend**: Spring Boot 4.0.2 with WebFlux
- **AI Integration**: Spring AI with Ollama
- **Build Tool**: Gradle 8.x
- **Java Version**: 21

## Troubleshooting

### Ollama Connection Issues

If you see errors like "Connection refused" or "Ollama is not running":

1. Check if Ollama is running:
   ```bash
   ollama list
   ```

2. Verify Ollama is accessible:
   ```bash
   curl http://localhost:11434/api/tags
   ```

3. Make sure the model is downloaded:
   ```bash
   ollama pull llama3.2
   ```

### Build Issues

If you encounter build issues:

1. Clean and rebuild:
   ```bash
   .\gradlew.bat clean build
   ```

2. Check Java version:
   ```bash
   java -version
   ```
   Should be Java 21 or higher.

## Development

### Project Structure

```
aibot/
├── src/
│   ├── main/
│   │   ├── java/com/uis/aibot/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── service/         # Business logic
│   │   │   └── AibotApplication.java
│   │   └── resources/
│   │       ├── templates/       # Thymeleaf templates
│   │       └── application.properties
│   └── test/
└── build.gradle
```

### Adding MCP Support

The application includes basic MCP (Model Context Protocol) support. To enable advanced MCP features, you can extend the `McpConfig` class.

## License

This project is for educational and development purposes.

## Support

For issues or questions:
1. Check Ollama documentation: https://ollama.com/docs
2. Check Spring AI documentation: https://docs.spring.io/spring-ai/reference/
