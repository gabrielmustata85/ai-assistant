package com.ai.assistant.advisor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvisorPromptBuilderTest {

    private final AdvisorPromptBuilder builder = new AdvisorPromptBuilder();

    @Test
    void promptContainsAllSectionsAndDisclaimer() {
        String prompt = builder.build(
                "Ce taxe am de plătit?",
                "User: salut",
                "FIRMA:\n- CUI: RO123\n",
                List.of("Cota impozit micro este 1%.", "Termen depunere: 25 ale lunii."));

        assertTrue(prompt.contains("Ce taxe am de plătit?"));
        assertTrue(prompt.contains("RO123"));
        assertTrue(prompt.contains("1%"));
        assertTrue(prompt.contains("orientativ"));      // disclaimer prezent
        assertTrue(prompt.toLowerCase().contains("data_gaps")); // cere date lipsă
    }

    @Test
    void handlesEmptyLegislationAndContext() {
        String prompt = builder.build("Întrebare", "", "FIRMA:\n", List.of());
        assertTrue(prompt.contains("Întrebare"));
    }
}
