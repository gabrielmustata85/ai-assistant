package com.ai.assistant.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoyageEmbeddingClientTest {

    @Test
    void parsesEmbeddingFromVoyageResponse() {
        VoyageEmbeddingClient client = new VoyageEmbeddingClient("test-key");
        String json = "{\"object\":\"list\",\"data\":[{\"object\":\"embedding\","
                + "\"embedding\":[0.1,0.2,0.3],\"index\":0}],\"model\":\"voyage-3.5\"}";

        List<Float> embedding = client.parseEmbedding(json);

        assertEquals(3, embedding.size());
        assertEquals(0.1f, embedding.get(0), 1e-6);
        assertEquals(0.3f, embedding.get(2), 1e-6);
    }
}
