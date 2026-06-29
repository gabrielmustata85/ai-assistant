package com.ai.assistant.client;

import com.ai.assistant.config.ConversationContext;
import com.ai.assistant.exceptions.PineconeSearchException;
import com.ai.assistant.model.Vector;
import com.ai.assistant.service.AIResponseHistoryService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.ai.assistant.constants.NameSpaces.PDF_DOCUMENTS_NAMESPACE;
import static com.ai.assistant.constants.NameSpaces.SQL_SCHEMA_NAMESPACE;

@Service
@Primary
@Slf4j
public class EnhancedPineconeClient extends PineconeClient {

    private final CopilotClient copilotClient;
    private final ConversationContext conversationContext;
    private final AIResponseHistoryService historyService;

    @Autowired
    public EnhancedPineconeClient(
            @Value("${pinecone.environment}") String environment,
            @Value("${pinecone.index.name}") String indexName,
            @Value("${pinecone.api.key}") String apiKey,
            CopilotClient copilotClient,
            ConversationContext conversationContext,
            AIResponseHistoryService historyService) {
        super(environment, indexName, apiKey);
        this.copilotClient = copilotClient;
        this.conversationContext = conversationContext;
        this.historyService = historyService;
    }

    /**
     * Search for relevant SQL schema information based on user query
     */
    public List<String> searchSqlSchema(String question, String sessionId) throws IOException {
        // Get conversation context
        String context = conversationContext.getFullContext(sessionId);
        String enhancedQuery = context.isEmpty() ? question : context + "\n" + question;

        List<Float> embedding = copilotClient.createEmbedding(enhancedQuery);
        List<String> results = super.searchEmbedding(embedding, SQL_SCHEMA_NAMESPACE);

        // Log to conversation history
        conversationContext.addMessage(sessionId, "User: " + question);
        conversationContext.addMessage(sessionId, "System: Found " + results.size() + " relevant SQL schemas");
        historyService.logInteraction(sessionId, question, "Found " + results.size() + " SQL schema references");

        return results;
    }

    /**
     * Search for PDF document content based on query
     */
    public List<String> searchPdfDocuments(String question, String context) {
        return searchPdfDocuments(question, context, "default-session");
    }

    public List<String> searchPdfDocuments(String question, String context, String sessionId) throws PineconeSearchException {
        try {
            // Combine context and question for better semantic understanding
            String searchQuery = formatSearchQuery(context, question);

            // Generate embedding
            List<Float> embedding = copilotClient.createEmbedding(searchQuery);

            // Execute search
            List<String> results = super.searchEmbedding(embedding, PDF_DOCUMENTS_NAMESPACE);

            // Log interaction
            logSearchInteraction(sessionId, question, results.size(), "PDF documents");

            return results;
        } catch (IOException e) {
            throw new PineconeSearchException("Failed to search PDF documents", e);
        }
    }

    private String formatSearchQuery(String context, String question) {
        if (context == null || context.trim().isEmpty()) {
            return question;
        }
        return String.format("Context: %s\nQuestion: %s", context, question);
    }

    private void logSearchInteraction(String sessionId, String query, int resultCount, String resourceType) {
        try {
            conversationContext.addMessage(sessionId, "User query: " + query);
            conversationContext.addMessage(sessionId, "System: Found " + resultCount + " relevant " + resourceType);
            historyService.logInteraction(sessionId, query, resourceType + " search returned " + resultCount + " results");
        } catch (Exception e) {
            log.error("Failed to log search interaction", e);
        }
    }

    /**
     * Upsert PDF document chunk to Pinecone
     */
    public void upsertPdfDocument(Vector vector, JSONObject metadata) throws IOException {
        super.upsertEmbedding(vector, metadata, PDF_DOCUMENTS_NAMESPACE);
    }

    /**
     * Upsert SQL schema information to Pinecone
     */
    public void upsertSqlSchema(Vector vector, JSONObject metadata) throws IOException {
        String existing = super.fetch(vector.getId(), SQL_SCHEMA_NAMESPACE);
        if (existing != null && !existing.equals("No vectors found for this ID")) {
            log.info("Updating existing SQL schema: " + vector.getId());
        }
        super.upsertEmbedding(vector, metadata, SQL_SCHEMA_NAMESPACE);
    }

    /**
     * Get all Pinecone IDs from a namespace
     */
    public List<String> getAllPineconeIds(String namespace) throws IOException {
        JSONObject json = new JSONObject()
                .put("top_k", 10000)
                .put("include_metadata", false)
                .put("include_values", false);

        if (namespace != null && !namespace.isEmpty()) {
            json.put("namespace", namespace);
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(super.getBaseUrl() + "/query")
                .post(body)
                .addHeader("Api-Key", super.getApiKey())
                .build();

        try (Response response = super.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get IDs: " + response.body().string());
            }

            JSONObject responseJson = new JSONObject(response.body().string());
            JSONArray matches = responseJson.getJSONArray("matches");
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < matches.length(); i++) {
                ids.add(matches.getJSONObject(i).getString("id"));
            }
            return ids;
        }
    }

    /**
     * Delete vector from Pinecone
     */
    public void deleteVector(String vectorId, String namespace) throws IOException {
        HttpUrl url = HttpUrl.parse(super.getBaseUrl() + "/vectors/delete")
                .newBuilder()
                .addQueryParameter("ids", vectorId)
                .addQueryParameter("namespace", namespace)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Api-Key", super.getApiKey())
                .build();

        try (Response response = super.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Delete failed: " + response.body().string());
            }
        }
    }
}