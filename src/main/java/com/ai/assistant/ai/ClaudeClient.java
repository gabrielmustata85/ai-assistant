package com.ai.assistant.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Client Claude cu două niveluri de model pentru a controla costul:
 *  - HEAVY: consiliere/raționament (estimări fiscale, chat, generare factură). Implicit Opus.
 *  - LIGHT: extragere structurată de mare volum (parsare PDF-uri). Implicit Haiku, mult mai ieftin.
 * Ambele se pot suprascrie din env: ANTHROPIC_MODEL_HEAVY / ANTHROPIC_MODEL_LIGHT.
 * Pentru economie totală, setează ambele pe un model ieftin.
 */
@Slf4j
@Lazy
@Service
public class ClaudeClient {

    private static final long MAX_TOKENS_HEAVY = 16000L;
    private static final long MAX_TOKENS_LIGHT = 8000L;

    private final AnthropicClient client;
    private final Model modelHeavy;
    private final Model modelLight;

    public ClaudeClient(
            @Value("${anthropic.model.heavy:claude-opus-4-8}") String heavy,
            @Value("${anthropic.model.light:claude-haiku-4-5}") String light) {
        this.client = AnthropicOkHttpClient.fromEnv();   // citește ANTHROPIC_API_KEY
        this.modelHeavy = Model.of(heavy);
        this.modelLight = Model.of(light);
        log.info("ClaudeClient: heavy={}, light={}", heavy, light);
    }

    /** Răspuns text liber (consiliere) — model HEAVY, gândire adaptivă. */
    public String generateText(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(modelHeavy)
                .maxTokens(MAX_TOKENS_HEAVY)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .addUserMessage(prompt)
                .build();

        Message response = client.messages().create(params);
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining("\n"));
    }

    /** Răspuns structurat de consiliere (ClaudeResponse) — model HEAVY. */
    public ClaudeResponse ask(String prompt) {
        return structured(prompt, ClaudeResponse.class, modelHeavy, MAX_TOKENS_HEAVY, true);
    }

    /** Extragere structurată de mare volum (parsare PDF) — model LIGHT, ieftin, fără gândire. */
    public <T> T extractStructured(String prompt, Class<T> schemaClass) {
        return structured(prompt, schemaClass, modelLight, MAX_TOKENS_LIGHT, false);
    }

    /** Extragere structurată care necesită raționament (ex: generarea unei facturi) — model HEAVY. */
    public <T> T extractStructuredHeavy(String prompt, Class<T> schemaClass) {
        return structured(prompt, schemaClass, modelHeavy, MAX_TOKENS_HEAVY, true);
    }

    private <T> T structured(String prompt, Class<T> schemaClass, Model model, long maxTokens, boolean thinking) {
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

        return client.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude nu a returnat un răspuns structurat"));
    }
}
