package com.ai.assistant.ai;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VoyageEmbeddingClient implements EmbeddingClient {

    private static final String VOYAGE_URL = "https://api.voyageai.com/v1/embeddings";
    private static final String MODEL = "voyage-3.5";
    private static final int DIMENSION = 1024;

    private final String apiKey;
    private final OkHttpClient client;

    public VoyageEmbeddingClient(@Value("${voyage.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public List<Float> embed(String text) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("input", new JSONArray().put(text));

        Request request = new Request.Builder()
                .url(VOYAGE_URL)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody respBody = response.body();
            String payload = respBody != null ? respBody.string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Voyage request failed with code "
                        + response.code() + ": " + payload);
            }
            return parseEmbedding(payload);
        }
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    List<Float> parseEmbedding(String responseJson) {
        JSONObject json = new JSONObject(responseJson);
        JSONArray data = json.getJSONArray("data");
        if (data.length() == 0) {
            throw new IllegalStateException("No embedding data in Voyage response");
        }
        JSONArray arr = data.getJSONObject(0).getJSONArray("embedding");
        List<Float> embedding = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            embedding.add((float) arr.getDouble(i));
        }
        return embedding;
    }
}
