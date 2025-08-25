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

    /*@Autowired
    public ChatController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(100)
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        (Advisor) tools // 👈 Add this line
                )
                .build();
    }
    */

    // Original endpoint from ChatController.java - kept exactly as is
    @GetMapping("/prompt")
    public String promptForDiscovery(@RequestParam(required = true) String userId,
                                     @RequestParam(required = true, defaultValue = "qui es tu ?") String message) {

        UserMessage userMessage = new UserMessage(message);
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemMessageResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", "L'assistant IA officiel"));

        //Prompt prompt = new Prompt(List.of(userMessage, systemMessage));
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        try {
            return this.chatClient
                    .prompt(prompt)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userId))
                    .toolCallbacks(tools)
                    .call().content();
            /*return this.chatClient
                    .prompt(prompt)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userId))
                    .call()
                    .content();*/
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

    // Added from ChatController2.java - JSON-based chat endpoint
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

            // Use the ChatClient to get response from the AI with MCP tools
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            Map<String, Object> responseBody = Map.of(
                    "message", message,
                    "response", response,
                    "timestamp", timestamp,
                    "responseTimestamp", Instant.now().toString()
            );

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    // Added from ChatController2.java - health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = Map.of(
                "status", "UP",
                "service", "Discovery Intech MCP Chat",
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.ok(health);
    }
}