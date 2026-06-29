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
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
public class ClaudeClient {

    private static final long MAX_TOKENS = 16000L;

    // SDK 2.34.0 does not yet have CLAUDE_OPUS_4_8 as a constant; use Model.of() instead.
    private static final Model MODEL = Model.of("claude-opus-4-8");

    private final AnthropicClient client;

    public ClaudeClient() {
        // Citește ANTHROPIC_API_KEY din mediu.
        this.client = AnthropicOkHttpClient.fromEnv();
    }

    /** Răspuns text liber (înlocuiește CopilotClient.generateResponse). */
    public String generateText(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .addUserMessage(prompt)
                .build();

        Message response = client.messages().create(params);
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining("\n"));
    }

    /** Răspuns structurat JSON conform ClaudeResponse. */
    public ClaudeResponse ask(String prompt) {
        // outputConfig(Class<T>, JsonSchemaLocalValidation) transitions the builder to
        // StructuredMessageCreateParams.Builder<T>; JsonSchemaLocalValidation.NO skips local validation.
        StructuredMessageCreateParams<ClaudeResponse> params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(ClaudeResponse.class, JsonSchemaLocalValidation.NO)
                .addUserMessage(prompt)
                .build();

        return client.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude nu a returnat un răspuns structurat"));
    }
}
