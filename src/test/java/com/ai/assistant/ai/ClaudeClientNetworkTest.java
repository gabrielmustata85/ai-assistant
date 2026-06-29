package com.ai.assistant.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeClientNetworkTest {

    @Test
    void generatesTextResponse() {
        ClaudeClient client = new ClaudeClient();
        String reply = client.generateText("Spune exact cuvântul: salut");
        assertNotNull(reply);
        assertTrue(reply.toLowerCase().contains("salut"));
    }
}
