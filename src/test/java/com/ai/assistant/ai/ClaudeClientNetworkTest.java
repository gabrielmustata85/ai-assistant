package com.ai.assistant.ai;

import com.ai.assistant.usage.UsageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeClientNetworkTest {

    // UsageService fără repo: check()/record() ies devreme când nu există user autentificat (context de test).
    private static final UsageService NOOP_USAGE =
            new UsageService(null, 50000, 14400, 5000000, 20000000, 2592000);

    @Test
    void generatesTextResponse() {
        ClaudeClient client = new ClaudeClient(NOOP_USAGE, "claude-opus-4-8", "claude-haiku-4-5");
        String reply = client.generateText("Spune exact cuvântul: salut");
        assertNotNull(reply);
        assertTrue(reply.toLowerCase().contains("salut"));
    }
}
