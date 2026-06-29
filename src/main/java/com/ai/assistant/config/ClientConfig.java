package com.ai.assistant.config;


import com.ai.assistant.client.CopilotClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    @Bean
    public CopilotClient copilotClient(@Value("${copilot.api.key}") String apiKey) {
        return new CopilotClient(apiKey);
    }
}
