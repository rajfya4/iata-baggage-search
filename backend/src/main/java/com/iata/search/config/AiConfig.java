package com.iata.search.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class AiConfig {

    @Bean
    public String systemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/system-prompt.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load system prompt", e);
        }
    }

    @Bean
    public List<DocumentChunk> documentStore() {
        return new ArrayList<>();
    }

    public record DocumentChunk(String id, String text, String sectionRef, float[] embedding) {}
}
