package com.ai.assistant.ai;

import com.ai.assistant.usage.UsageService;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.anthropic.models.messages.Usage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Client Claude cu două niveluri de model pentru a controla costul:
 *  - HEAVY: consiliere/raționament (estimări fiscale, chat complex, generare factură). Implicit Opus.
 *  - LIGHT: extragere structurată de mare volum (parsare PDF) și întrebări simple. Implicit Haiku.
 * Suprascriere din env: ANTHROPIC_MODEL_HEAVY / ANTHROPIC_MODEL_LIGHT.
 * Fiecare cerere verifică și înregistrează consumul de tokens prin UsageService.
 */
@Slf4j
@Lazy
@Service
public class ClaudeClient {

    private static final long MAX_TOKENS_HEAVY = 16000L;
    private static final long MAX_TOKENS_LIGHT = 8000L;
    private static final long MAX_TOKENS_CLASSIFY = 200L;

    private final AnthropicClient client;
    private final UsageService usageService;
    private final Model modelHeavy;
    private final Model modelLight;

    public ClaudeClient(
            UsageService usageService,
            @Value("${anthropic.model.heavy:claude-opus-4-8}") String heavy,
            @Value("${anthropic.model.light:claude-haiku-4-5}") String light) {
        this.client = AnthropicOkHttpClient.fromEnv();   // citește ANTHROPIC_API_KEY
        this.usageService = usageService;
        this.modelHeavy = Model.of(heavy);
        this.modelLight = Model.of(light);
        log.info("ClaudeClient: heavy={}, light={}", heavy, light);
    }

    /** Răspuns text liber (consiliere) — model HEAVY, gândire adaptivă. */
    public String generateText(String prompt) {
        usageService.check();
        MessageCreateParams params = MessageCreateParams.builder()
                .model(modelHeavy)
                .maxTokens(MAX_TOKENS_HEAVY)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .addUserMessage(prompt)
                .build();

        Message response = client.messages().create(params);
        record(response.usage());
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining("\n"));
    }

    /** Răspuns structurat de consiliere (ClaudeResponse) — model HEAVY. */
    public ClaudeResponse ask(String prompt) {
        return ask(prompt, true);
    }

    /** Răspuns structurat de consiliere, cu alegerea nivelului (heavy=puternic, altfel ieftin). */
    public ClaudeResponse ask(String prompt, boolean heavy) {
        return structured(prompt, ClaudeResponse.class,
                heavy ? modelHeavy : modelLight,
                heavy ? MAX_TOKENS_HEAVY : MAX_TOKENS_LIGHT, heavy);
    }

    /** Extragere structurată de mare volum (parsare PDF) — model LIGHT, ieftin, fără gândire. */
    public <T> T extractStructured(String prompt, Class<T> schemaClass) {
        return structured(prompt, schemaClass, modelLight, MAX_TOKENS_LIGHT, false);
    }

    /** Extragere structurată care necesită raționament (ex: generarea unei facturi) — model HEAVY. */
    public <T> T extractStructuredHeavy(String prompt, Class<T> schemaClass) {
        return structured(prompt, schemaClass, modelHeavy, MAX_TOKENS_HEAVY, true);
    }

    /**
     * Triaj rapid și ieftin (model LIGHT) care decide dacă o cerere e complexă.
     * Permite răspunsuri bune la întrebări grele (HEAVY) și economie la cele simple (LIGHT).
     */
    public boolean classifyComplex(String question) {
        String prompt = "Ești un router pentru un asistent fiscal. Clasifică cererea utilizatorului.\n"
                + "complex=true dacă necesită calcule/estimări fiscale, raționament pe legislație, "
                + "corelarea mai multor date sau o generare complexă. "
                + "complex=false dacă e simplă/generală (definiție, salut, întrebare scurtă, clarificare).\n\n"
                + "Cererea: " + question;
        try {
            QuestionComplexity c = structured(prompt, QuestionComplexity.class, modelLight, MAX_TOKENS_CLASSIFY, false);
            return c != null && c.complex();
        } catch (Exception e) {
            log.warn("Clasificarea complexității a eșuat, folosesc modelul puternic: {}", e.getMessage());
            return true;   // în caz de eroare, alegem calitatea (HEAVY)
        }
    }

    private <T> T structured(String prompt, Class<T> schemaClass, Model model, long maxTokens, boolean thinking) {
        usageService.check();
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens);
        if (thinking) {
            builder.thinking(ThinkingConfigAdaptive.builder().build());
        }
        StructuredMessageCreateParams<T> params = builder
                .outputConfig(schemaClass, JsonSchemaLocalValidation.NO)
                .addUserMessage(prompt)
                .build();

        StructuredMessage<T> resp = client.messages().create(params);
        record(resp.usage());
        return resp.content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude nu a returnat un răspuns structurat"));
    }

    private void record(Usage usage) {
        if (usage != null) {
            usageService.record(usage.inputTokens() + usage.outputTokens());
        }
    }
}
