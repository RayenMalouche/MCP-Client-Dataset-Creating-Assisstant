package com.mcp.dataset.creating.RayenMalouche.Assisstant.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/discovery-ai")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;
    private final MessageWindowChatMemory chatMemory;

    @Value("classpath:/prompts/system-prompt-70.st")
    private Resource systemMessageResource;

    @Autowired
    private ToolCallbackProvider tools;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(100)
                .build();
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
    }

    // Original endpoint from ChatController.java - kept exactly as is
    @GetMapping("/prompt")
    public String promptForDiscovery(@RequestParam(required = true) String userId,
                                     @RequestParam(required = true, defaultValue = "qui es tu ?") String message) {

        // Add imperative email instruction to the user message
        String enhancedMessage = message +
                "\n\nIMPERATIVE EMAIL INSTRUCTION: After successfully creating any dataset, you MUST send an email notification using the send-email tool with these EXACT parameters:" +
                "\n- to: \"rayenmalouche@gmail.com\"" +
                "\n- subject: \"Dataset for [dataset_topic] created successfully\"" +
                "\n- body: \"Dataset has been generated with [number_of_elements] JSON elements about [dataset_topic]. Generation completed at [current_timestamp]. The dataset is ready for training purposes.\"" +
                "\n\nThis email notification is MANDATORY and must be sent after every successful dataset creation.";

        UserMessage userMessage = new UserMessage(enhancedMessage);
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemMessageResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", "L'assistant IA officiel"));

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        try {
            return this.chatClient
                    .prompt(prompt)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userId))
                    .toolCallbacks(tools)
                    .call().content();
        } catch (Exception e) {
            return "Désolé, je n'ai pas pu accéder aux informations demandées. Veuillez réessayer ou contacter le support.";
        }
    }

    // Original endpoint from ChatController.java - kept exactly as is
    @GetMapping("/{userId}/history")
    public List<Message> getHistory(@PathVariable String userId) {
        return chatMemory.get(userId);
    }

    // Original endpoint from ChatController.java - kept exactly as is
    @DeleteMapping("/{userId}/history")
    public String clearHistory(@PathVariable String userId) {
        chatMemory.clear(userId);
        return "Conversation history cleared for user: " + userId;
    }

    // Enhanced JSON-based chat endpoint with mandatory email instructions
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            String timestamp = (String) request.getOrDefault("timestamp", Instant.now().toString());

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Message is required",
                        "timestamp", timestamp
                ));
            }

            // Add imperative email instruction to every message requesting dataset creation
            String enhancedMessage = message;
            if (message.toLowerCase().contains("dataset") ||
                    message.toLowerCase().contains("créez") ||
                    message.toLowerCase().contains("générez") ||
                    message.toLowerCase().contains("produisez")) {

                enhancedMessage = message +
                        "\n\n🚨 IMPERATIVE EMAIL NOTIFICATION REQUIREMENT 🚨" +
                        "\nAfter successfully creating the dataset, you MUST use the send-email tool with these EXACT parameters:" +
                        "\n{" +
                        "\n  \"to\": \"rayenmalouche@gmail.com\"," +
                        "\n  \"subject\": \"Dataset for [dataset_topic] created successfully\"," +
                        "\n  \"body\": \"Dataset has been generated with [number_of_elements] JSON elements about [dataset_topic]. Generation completed at " + timestamp + ". The dataset is ready for training purposes.\"" +
                        "\n}" +
                        "\n\nThis email notification is MANDATORY and must be sent after every successful dataset creation. Do not proceed without sending this email.";
            }

            // Use the ChatClient to get response from the AI with MCP tools
            String response = chatClient.prompt()
                    .user(enhancedMessage)
                    .toolCallbacks(tools)
                    .call()
                    .content();

            // Check if email notification was likely sent
            boolean emailMentioned = response.toLowerCase().contains("email") ||
                    response.toLowerCase().contains("notification") ||
                    response.toLowerCase().contains("envoyé") ||
                    response.toLowerCase().contains("sent");

            Map<String, Object> responseBody = Map.of(
                    "message", message,
                    "response", response,
                    "timestamp", timestamp,
                    "responseTimestamp", Instant.now().toString(),
                    "emailInstructionProvided", enhancedMessage.contains("IMPERATIVE EMAIL"),
                    "emailNotificationDetected", emailMentioned
            );

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    // Enhanced health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "Discovery Intech MCP Chat",
                "timestamp", Instant.now().toString(),
                "features", List.of("dataset_creation", "email_notifications", "web_fetching"),
                "mcpToolsConnected", tools != null
        );
        return ResponseEntity.ok(health);
    }

    // New endpoint specifically for dataset creation with email enforcement
    @PostMapping("/create-dataset")
    public ResponseEntity<Map<String, Object>> createDataset(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.get("topic");
            String customQuery = (String) request.get("customQuery");
            String timestamp = (String) request.getOrDefault("timestamp", Instant.now().toString());

            if (topic == null && customQuery == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Either 'topic' or 'customQuery' is required",
                        "timestamp", timestamp
                ));
            }

            String query = customQuery != null ? customQuery :
                    String.format("Créez un dataset complet sur %s de Discovery Intech", topic);

            // Always add mandatory email instruction for dataset creation
            String enhancedQuery = query +
                    "\n\n🚨 MANDATORY EMAIL NOTIFICATION 🚨" +
                    "\nYou MUST use the send-email tool after creating the dataset with these EXACT parameters:" +
                    "\n{" +
                    "\n  \"to\": \"rayenmalouche@gmail.com\"," +
                    "\n  \"subject\": \"Dataset for " + (topic != null ? topic : "custom topic") + " created successfully\"," +
                    "\n  \"body\": \"Dataset has been generated with [number_of_elements] JSON elements. Generation completed at " + timestamp + ". Topic: " + (topic != null ? topic : "custom") + ". The dataset is ready for training purposes.\"" +
                    "\n}" +
                    "\n\nFAILURE TO SEND EMAIL IS NOT ACCEPTABLE. This is a critical requirement.";

            String response = chatClient.prompt()
                    .user(enhancedQuery)
                    .toolCallbacks(tools)
                    .call()
                    .content();

            // Analyze response for email confirmation
            boolean emailConfirmed = response.toLowerCase().contains("email") &&
                    (response.toLowerCase().contains("sent") ||
                            response.toLowerCase().contains("envoyé") ||
                            response.toLowerCase().contains("notification"));

            Map<String, Object> responseBody = Map.of(
                    "originalQuery", query,
                    "response", response,
                    "topic", topic != null ? topic : "custom",
                    "timestamp", timestamp,
                    "responseTimestamp", Instant.now().toString(),
                    "emailNotificationConfirmed", emailConfirmed,
                    "status", emailConfirmed ? "SUCCESS" : "WARNING_EMAIL_UNCERTAIN"
            );

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", Instant.now().toString(),
                    "status", "ERROR"
            ));
        }
    }
}