package com.ai.assistant.controller;

import com.ai.assistant.client.CopilotClient;
import com.ai.assistant.client.EnhancedPineconeClient;
import com.ai.assistant.config.ConversationContext;
import com.ai.assistant.service.DocumentIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final EnhancedPineconeClient pineconeClient;
    private final CopilotClient copilotClient;
    private final DocumentIngestionService ingestionService;
    private final ConversationContext conversationContext;

    @Autowired
    public ChatController(
            EnhancedPineconeClient pineconeClient,
            CopilotClient copilotClient,
            DocumentIngestionService ingestionService,
            ConversationContext conversationContext) {
        this.pineconeClient = pineconeClient;
        this.copilotClient = copilotClient;
        this.ingestionService = ingestionService;
        this.conversationContext = conversationContext;
    }

    /**
     * Upload PDF documents for ingestion
     */
    @PostMapping("/upload/pdf")
    public ResponseEntity<Map<String, String>> uploadPdf(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            response.put("status", "error");
            response.put("message", "Please upload a valid PDF file.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            ingestionService.ingestPdfDocument(file);
            response.put("status", "success");
            response.put("message", "PDF uploaded and ingested successfully.");
            response.put("filename", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error processing PDF", e);
            response.put("status", "error");
            response.put("message", "Error processing the PDF: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload SQL files for schema ingestion
     */
    @PostMapping("/upload/sql")
    public ResponseEntity<Map<String, String>> uploadSql(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".sql")) {
            response.put("status", "error");
            response.put("message", "Please upload a valid SQL file.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            ingestionService.ingestSqlFile(file);
            response.put("status", "success");
            response.put("message", "SQL file uploaded and schema ingested successfully.");
            response.put("filename", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error processing SQL file", e);
            response.put("status", "error");
            response.put("message", "Error processing the SQL file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Ask a question - can be SQL query generation or PDF document query
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(
            @RequestHeader("X-Session-ID") String sessionId,
            @RequestBody Map<String, String> request) {

        String question = request.get("question");
        String queryType = request.getOrDefault("type", "auto"); // auto, sql, pdf

        Map<String, Object> response = new HashMap<>();

        conversationContext.addMessage(sessionId, "User: " + question);

        try {
            String fullContext = conversationContext.getFullContext(sessionId);

            // Determine query type if auto
            if ("auto".equals(queryType)) {
                queryType = detectQueryType(question);
            }

            String aiResponse;
            List<String> relevantDocs;

            if ("sql".equals(queryType)) {
                // SQL query generation
                relevantDocs = pineconeClient.searchSqlSchema(question, sessionId);
                aiResponse = generateSqlQueryResponse(question, fullContext, relevantDocs);
                response.put("queryType", "sql");
            } else {
                // PDF document search
                relevantDocs = pineconeClient.searchPdfDocuments(question, fullContext, sessionId);
                aiResponse = generatePdfQueryResponse(question, fullContext, relevantDocs);
                response.put("queryType", "pdf");
            }

            conversationContext.addMessage(sessionId, "AI: " + aiResponse);

            response.put("status", "success");
            response.put("answer", aiResponse);
            response.put("relevantDocuments", relevantDocs.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing question", e);
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Reset conversation context
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetConversation(
            @RequestHeader("X-Session-ID") String sessionId) {

        conversationContext.clear(sessionId);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Conversation reset successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get conversation history
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestHeader("X-Session-ID") String sessionId,
            @RequestParam(defaultValue = "10") int limit) {

        List<String> messages = conversationContext.getRecentMessages(sessionId, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("messages", messages);
        response.put("count", messages.size());

        return ResponseEntity.ok(response);
    }

    // Helper methods

    private String detectQueryType(String question) {
        String lowerQuestion = question.toLowerCase();

        // SQL keywords detection
        if (lowerQuestion.contains("select") ||
                lowerQuestion.contains("query") ||
                lowerQuestion.contains("table") ||
                lowerQuestion.contains("join") ||
                lowerQuestion.contains("database") ||
                lowerQuestion.contains("sql")) {
            return "sql";
        }

        return "pdf";
    }

    private String generateSqlQueryResponse(String question, String context, List<String> relevantSchemas)
            throws IOException {

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are a SQL expert assistant. Generate a SQL query based on the user's request.\n\n");

        if (!context.isEmpty()) {
            promptBuilder.append("Previous conversation:\n").append(context).append("\n\n");
        }

        promptBuilder.append("Available database schemas:\n");
        for (String schemaId : relevantSchemas) {
            try {
                String schemaInfo = pineconeClient.fetch(schemaId, "sql_schema");
                promptBuilder.append(schemaInfo).append("\n");
            } catch (Exception e) {
                log.warn("Failed to fetch schema: {}", schemaId);
            }
        }

        promptBuilder.append("\nUser question: ").append(question).append("\n\n");
        promptBuilder.append("Generate a SQL query that answers this question. ");
        promptBuilder.append("Explain what the query does and include the complete SQL statement.");

        return copilotClient.generateResponse(promptBuilder.toString());
    }

    private String generatePdfQueryResponse(String question, String context, List<String> relevantDocs)
            throws IOException {

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are a helpful assistant. Answer the user's question based on the provided documents.\n\n");

        if (!context.isEmpty()) {
            promptBuilder.append("Previous conversation:\n").append(context).append("\n\n");
        }

        promptBuilder.append("Relevant document excerpts:\n");
        for (String docId : relevantDocs) {
            try {
                String docContent = pineconeClient.fetch(docId, "pdf_documents");
                promptBuilder.append("[Document] ").append(docContent).append("\n\n");
            } catch (Exception e) {
                log.warn("Failed to fetch document: {}", docId);
            }
        }

        promptBuilder.append("User question: ").append(question).append("\n\n");
        promptBuilder.append("Provide a clear and accurate answer based on the documents above.");

        return copilotClient.generateResponse(promptBuilder.toString());
    }
}