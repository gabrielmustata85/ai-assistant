package com.ai.assistant.client;

import com.ai.assistant.config.RetryInterceptor;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.IntArrayList;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CopilotClient {

    private static final String COPILOT_API_URL = "https://api.githubcopilot.com/chat/completions";
    private static final String COPILOT_EMBEDDING_URL = "https://api.githubcopilot.com/embeddings";
    private static final int MAX_TOKENS_PER_CHUNK = 8000;

    private final String apiKey;
    private final OkHttpClient client;
    private final Encoding encoding;

    public CopilotClient(@Value("${copilot.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .addInterceptor(new RetryInterceptor(3))
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();

        this.encoding = Encodings.newDefaultEncodingRegistry()
                .getEncoding("cl100k_base")
                .orElseThrow(() -> new IllegalStateException("Failed to load cl100k_base encoding"));
    }

    public String generateResponse(String prompt) throws IOException {
        JSONObject json = new JSONObject()
                .put("model", "gpt-4")
                .put("messages", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("content", prompt)))
                .put("max_tokens", 2000)
                .put("temperature", 0.3); // Lower temperature for more precise SQL queries

        Request request = new Request.Builder()
                .url(COPILOT_API_URL)
                .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try (ResponseBody errorBody = response.body()) {
                    throw new IOException("Request failed with code " + response.code() +
                            ": " + (errorBody != null ? errorBody.string() : "No error body"));
                }
            }

            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new IOException("Empty response body");
                }
                JSONObject jsonResponse = new JSONObject(responseBody.string());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.isEmpty()) {
                    throw new IOException("No choices in response");
                }
                return choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }
        }
    }

    public List<Float> createEmbedding(String text) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        List<String> chunks = splitTextIntoChunks(text, MAX_TOKENS_PER_CHUNK);
        if (chunks.isEmpty()) {
            throw new IOException("No valid text chunks could be extracted");
        }

        List<List<Float>> allEmbeddings = new ArrayList<>();
        for (String chunk : chunks) {
            try {
                List<Float> embedding = createSingleEmbedding(chunk);
                if (embedding == null || embedding.isEmpty()) {
                    log.warn("Empty embedding for chunk: {}", chunk.substring(0, Math.min(50, chunk.length())));
                    continue;
                }
                if (embedding.size() != 1536) {
                    log.warn("Unexpected embedding size: {}", embedding.size());
                    continue;
                }
                allEmbeddings.add(embedding);
            } catch (IOException e) {
                log.warn("Failed to create embedding for chunk: {}", e.getMessage());
            }
        }

        if (allEmbeddings.isEmpty()) {
            throw new IOException("All embedding attempts failed");
        }
        return averageEmbeddings(allEmbeddings);
    }

    private List<Float> createSingleEmbedding(String text) throws IOException {
        JSONObject json = new JSONObject()
                .put("model", "text-embedding-ada-002")
                .put("input", text);

        Request request = new Request.Builder()
                .url(COPILOT_EMBEDDING_URL)
                .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try (ResponseBody errorBody = response.body()) {
                    throw new IOException("Embedding request failed: " +
                            (errorBody != null ? errorBody.string() : "No error details"));
                }
            }

            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new IOException("Empty embedding response");
                }

                JSONObject jsonResponse = new JSONObject(responseBody.string());
                JSONArray data = jsonResponse.getJSONArray("data");
                if (data.isEmpty()) {
                    throw new IOException("No embedding data in response");
                }

                JSONArray embeddingArray = data.getJSONObject(0).getJSONArray("embedding");
                List<Float> embeddings = new ArrayList<>(embeddingArray.length());
                for (int i = 0; i < embeddingArray.length(); i++) {
                    embeddings.add((float) embeddingArray.getDouble(i));
                }
                return embeddings;
            }
        }
    }

    private List<String> splitTextIntoChunks(String text, int maxTokensPerChunk) {
        // encoding.encode() returns IntArrayList
        IntArrayList tokens = encoding.encode(text);
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < tokens.size()) {
            int end = Math.min(start + maxTokensPerChunk, tokens.size());

            // Create IntArrayList for the sublist
            IntArrayList tokenSubList = new IntArrayList();
            for (int i = start; i < end; i++) {
                tokenSubList.add(tokens.get(i));
            }

            String chunkText = encoding.decode(tokenSubList);

            int lastBreak = Math.max(
                    chunkText.lastIndexOf(". "),
                    Math.max(
                            chunkText.lastIndexOf("\n"),
                            chunkText.lastIndexOf("! ")
                    )
            );

            if (lastBreak > 0 && (end - start) > maxTokensPerChunk/2) {
                String betterChunk = chunkText.substring(0, lastBreak + 1);
                IntArrayList betterTokens = encoding.encode(betterChunk);
                end = start + betterTokens.size();

                // Recalculate tokenSubList with new end
                tokenSubList = new IntArrayList();
                for (int i = start; i < end; i++) {
                    tokenSubList.add(tokens.get(i));
                }
            }

            chunks.add(encoding.decode(tokenSubList));
            start = end;
        }
        return chunks;
    }

    private List<Float> averageEmbeddings(List<List<Float>> embeddingsList) {
        if (embeddingsList == null || embeddingsList.isEmpty()) {
            throw new IllegalArgumentException("Embeddings list cannot be empty");
        }

        int dimensions = embeddingsList.get(0).size();
        List<Float> averaged = new ArrayList<>(dimensions);

        for (int i = 0; i < dimensions; i++) {
            float sum = 0;
            int count = 0;
            for (List<Float> embedding : embeddingsList) {
                if (embedding != null && i < embedding.size()) {
                    sum += embedding.get(i);
                    count++;
                }
            }
            averaged.add(count > 0 ? sum / count : 0f);
        }
        return averaged;
    }
}