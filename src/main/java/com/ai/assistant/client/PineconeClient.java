package com.ai.assistant.client;

import com.ai.assistant.model.Vector;
import lombok.Getter;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Getter
public class PineconeClient {

    private final String baseUrl;
    private final String apiKey;
    private final OkHttpClient client;

    public PineconeClient(
            @Value("${pinecone.environment}") String environment,
            @Value("${pinecone.index.name}") String indexName,
            @Value("${pinecone.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.baseUrl = String.format("https://%s-%s.pinecone.io", indexName, environment);
        this.client = createSecureClient();
    }

    public void upsertEmbedding(Vector embedding, JSONObject metadata, String namespace) throws IOException {
        Map<String, Object> metadataMap = new HashMap<>();
        for (String key : metadata.keySet()) {
            metadataMap.put(key, metadata.get(key));
        }

        List<Float> values = embedding.getValues();
        JSONObject vector = new JSONObject();
        vector.put("id", embedding.getId());
        vector.put("values", new JSONArray(values));
        vector.put("metadata", new JSONObject(metadataMap));

        JSONArray vectors = new JSONArray();
        vectors.put(vector);

        JSONObject json = new JSONObject();
        json.put("vectors", vectors);
        if (namespace != null && !namespace.isEmpty()) {
            json.put("namespace", namespace);
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(baseUrl + "/vectors/upsert")
                .post(body)
                .addHeader("Api-Key", apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Upsert failed: " + response.body().string());
            }
        }
    }

    public List<String> searchEmbedding(List<Float> queryVector, String namespace) throws IOException {
        if (queryVector == null || queryVector.isEmpty()) {
            throw new IllegalArgumentException("Query vector cannot be null or empty");
        }

        JSONObject json = new JSONObject();
        json.put("vector", new JSONArray(queryVector));
        json.put("top_k", 15);
        json.put("include_metadata", true);
        json.put("include_values", false);

        if (namespace != null && !namespace.isEmpty()) {
            json.put("namespace", namespace);
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(baseUrl + "/query")
                .post(body)
                .addHeader("Api-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "null";
                throw new IOException("Search failed with status " + response.code() + ": " + errorBody);
            }

            String responseBody = response.body() != null ? response.body().string() : "{}";
            JSONObject responseJson = new JSONObject(responseBody);

            if (!responseJson.has("matches")) {
                throw new IOException("Invalid response format: missing 'matches' field");
            }

            JSONArray matches = responseJson.getJSONArray("matches");
            List<String> results = new ArrayList<>();

            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                if (!match.has("id")) {
                    continue;
                }
                results.add(match.getString("id"));
            }

            return results;
        } catch (Exception e) {
            throw new IOException("Failed to process search response", e);
        }
    }

    public String fetch(String vectorId, String namespace) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/vectors/fetch")
                .newBuilder()
                .addQueryParameter("ids", vectorId)
                .build();

        if (namespace != null && !namespace.isEmpty()) {
            url = url.newBuilder().addQueryParameter("namespace", namespace).build();
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Api-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBodyString = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code() + " with body " + responseBodyString);
            }

            if (responseBodyString.trim().isEmpty()) {
                return "No content found";
            }

            try {
                JSONObject responseBody = new JSONObject(responseBodyString);
                JSONObject vectors = responseBody.getJSONObject("vectors");
                if (vectors.has(vectorId)) {
                    JSONObject vector = vectors.getJSONObject(vectorId);
                    return vector.optString("metadata", "No metadata found");
                }
                return "No vectors found for this ID";
            } catch (Exception e) {
                return "Error parsing response";
            }
        }
    }

    private OkHttpClient createSecureClient() {
        try {
            // Create a trust manager that trusts all certificates (not recommended for production)
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true) // Bypass hostname verification
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL client", e);
        }
    }
}