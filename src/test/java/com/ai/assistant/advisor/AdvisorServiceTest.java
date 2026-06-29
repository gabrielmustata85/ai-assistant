package com.ai.assistant.advisor;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.ai.ClaudeResponse;
import com.ai.assistant.client.EnhancedPineconeClient;
import com.ai.assistant.config.ConversationContext;
import com.ai.assistant.service.AIResponseHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvisorServiceTest {

    @Mock CompanyContextBuilder contextBuilder;
    @Mock AdvisorPromptBuilder promptBuilder;
    @Mock EnhancedPineconeClient pineconeClient;
    @Mock ConversationContext conversationContext;
    @Mock ClaudeClient claudeClient;
    @Mock AIResponseHistoryService historyService;
    @InjectMocks AdvisorService service;

    @Test
    void askOrchestratesPipelineAndReturnsClaudeResponse() throws Exception {
        when(conversationContext.getFullContext("s1")).thenReturn("");
        when(contextBuilder.build(1L)).thenReturn("FIRMA:\n");
        when(pineconeClient.searchLegislation(anyString())).thenReturn(List.of("legis-1"));
        when(pineconeClient.fetchLegislation("legis-1")).thenReturn("Cota micro 1%");
        when(promptBuilder.build(anyString(), anyString(), anyString(), anyList()))
                .thenReturn("PROMPT");
        ClaudeResponse expected = new ClaudeResponse(List.of(), List.of(), List.of(), List.of(), "orientativ");
        when(claudeClient.ask("PROMPT")).thenReturn(expected);

        ClaudeResponse result = service.ask("s1", 1L, "Ce taxe am?");

        assertSame(expected, result);
        verify(pineconeClient).searchLegislation(anyString());
        verify(claudeClient).ask("PROMPT");
        verify(historyService).logInteraction(eq("s1"), eq(1L), eq("Ce taxe am?"), anyString(), any());
        verify(conversationContext).addMessage(eq("s1"), contains("Ce taxe am?"));
    }
}
