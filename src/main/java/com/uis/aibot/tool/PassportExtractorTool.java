package com.uis.aibot.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uis.aibot.model.Passport;
import com.uis.aibot.service.VisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;


@Component
public class PassportExtractorTool {

    private static final Logger logger = LoggerFactory.getLogger(PassportExtractorTool.class);
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    private VisionService visionService;

    @Tool(description = "Extracts passport information from an image file. Pass the file path where the passport image is stored and the mime type. Returns passport information in JSON format with fields: passport_no, name, birthdate, gender, nationality, issue_date, expiry_date.")
    public String extractPassportInfo(String filePath, String mimeType) {
        logger.info("🔍 Tool called: extractPassportInfo with filePath={}, mimeType={}", filePath, mimeType);

        try {
            // Read the file and convert to base64
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
            logger.info("📄 File read successfully, size: {} bytes", fileBytes.length);

            // Create prompt for passport extraction
            String prompt = "Extract the following information from this passport image and return ONLY a valid JSON object with these exact fields: " +
                    "passport_no (string), name (string), birthdate (string in YYYY-MM-DD format), " +
                    "gender (string), nationality (string), issue_date (string in YYYY-MM-DD format), " +
                    "expiry_date (string in YYYY-MM-DD format). " +
                    "Return only the JSON object, no additional text or explanation.";

            // Use default media type if not provided
            String imgMimeType = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "image/jpeg";

            // Call vision service to analyze the image (blocking call)
            String response = visionService.describeImage(prompt, base64Image, imgMimeType).block();
            logger.info("🤖 Vision model response received");
            logger.debug("Vision model response: {}", response);

            // Clean up the response to extract JSON
            String cleanResponse = response != null ? response.trim() : "{}";
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();

            // Parse JSON response to Passport object to validate
            Passport passport = objectMapper.readValue(cleanResponse, Passport.class);
            logger.info("✅ Passport extraction completed successfully: {}", passport.getName());

            // Return as formatted JSON string
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(passport);

        } catch (Exception e) {
            logger.error("❌ Error extracting passport information: {}", e.getMessage(), e);
            return "{\"error\": \"Failed to extract passport information: " + e.getMessage() + "\"}";
        }
    }
}



