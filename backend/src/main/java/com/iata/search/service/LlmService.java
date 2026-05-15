package com.iata.search.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final String systemPrompt;
    private final HttpClient httpClient;

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.model:gpt-4o}")
    private String model;

    @Value("${ai.llm.provider:openai}")
    private String provider;

    @Value("${ai.llm.endpoint:https://api.openai.com/v1/chat/completions}")
    private String endpoint;

    public LlmService(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String generateResponse(String userQuery, java.util.List<String> contextChunks) {
        if (contextChunks == null || contextChunks.isEmpty()) {
            log.debug("No context chunks provided, skipping LLM call");
            return "No relevant information found in the IATA guidance document.";
        }

        String context = String.join("\n---\n", contextChunks);
        String fullPrompt = systemPrompt + "\n\nCONTEXT:\n" + context + "\n\nUSER QUERY: " + userQuery;

        String rawResponse;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("LLM API key not configured. Returning mock response.");
            rawResponse = mockResponse(userQuery, contextChunks);
        } else {
            try {
                String requestBody = buildRequestBody(fullPrompt);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    rawResponse = parseResponse(response.body());
                } else {
                    log.error("LLM API error: {} {}", response.statusCode(), response.body());
                    return "I encountered an error processing your request. Please try again.";
                }
            } catch (Exception e) {
                log.error("LLM call failed", e);
                return "The AI service is temporarily unavailable. Please try again.";
            }
        }

        return validateGrounding(rawResponse);
    }

    private String validateGrounding(String response) {
        if (response == null || response.isBlank()) {
            return "I cannot answer this question based on the available IATA guidance document.";
        }
        String trimmed = response.trim();
        if (trimmed.contains("cannot answer this question")
                || trimmed.contains("No relevant information")) {
            return trimmed;
        }
        boolean hasSectionRef = trimmed.matches("(?s).*\\[Section\\s+\\d+(\\.\\d+)*\\].*")
                || trimmed.matches("(?s).*[Ss]ection\\s+\\d+(\\.\\d+)*.*")
                || trimmed.matches("(?s).*\\[\\d+\\].*");
        if (!hasSectionRef && trimmed.length() > 20) {
            log.warn("Response may not be grounded: {}", trimmed.substring(0, Math.min(100, trimmed.length())));
        }
        return trimmed;
    }

    private String buildRequestBody(String prompt) {
        return """
        {
            "model": "%s",
            "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user", "content": "%s"}
            ],
            "temperature": 0.1
        }
        """.formatted(model, escapeJson(systemPrompt), escapeJson(prompt));
    }

    private String parseResponse(String json) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(json);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Failed to parse LLM response", e);
            return "I encountered an error processing your request.";
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String mockResponse(String query, java.util.List<String> chunks) {
        String firstChunk = chunks.get(0);
        String snippet = firstChunk.length() > 200 ? firstChunk.substring(0, 200) + "..." : firstChunk;
        return "Based on the IATA guidance document, regarding your query about \"" + query + "\": "
                + snippet
                + "\n\nPlease consult Section 1 of the document for more details.";
    }
}
