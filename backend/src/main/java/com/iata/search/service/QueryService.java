package com.iata.search.service;

import com.iata.search.dto.QueryRequest;
import com.iata.search.dto.QueryResponse;
import com.iata.search.dto.QueryResponse.CitationDto;
import com.iata.search.exception.RateLimitExceededException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private static final int MAX_QUERY_LENGTH = 1000;

    private final RagService ragService;
    private final LlmService llmService;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;

    public QueryService(RagService ragService, LlmService llmService,
                        AuditService auditService, RateLimiterService rateLimiterService) {
        this.ragService = ragService;
        this.llmService = llmService;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
    }

    @Transactional
    public QueryResponse processQuery(QueryRequest request) {
        String query = request.query().trim();
        String userId = request.userId().trim();

        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Query must not exceed " + MAX_QUERY_LENGTH + " characters");
        }

        if (!rateLimiterService.isAllowed(userId)) {
            throw new RateLimitExceededException("Rate limit exceeded. Max 10 requests per minute.");
        }

        if (containsAbuse(query)) {
            QueryResponse response = buildResponse("I cannot process this query.", List.of());
            auditService.logAudit(userId, query, response.answer(), null);
            return response;
        }

        List<RagService.ScoredChunk> chunks = ragService.retrieveRelevantChunks(query, 5);
        List<String> chunkTexts = chunks.stream().map(RagService.ScoredChunk::text).toList();

        if (chunks.isEmpty()) {
            QueryResponse response = buildResponse("No relevant information found in the IATA guidance document.", List.of());
            auditService.logAudit(userId, query, response.answer(), null);
            return response;
        }

        String answer;
        try {
            answer = llmService.generateResponse(query, chunkTexts);
        } catch (Exception e) {
            log.error("LLM call failed for query: {}", query, e);
            answer = "I encountered an error processing your request. Please try again.";
        }

        List<CitationDto> citations = chunks.stream()
                .map(c -> new CitationDto(c.sectionRef(), truncate(c.text(), 300), c.score()))
                .toList();

        QueryResponse response = buildResponse(answer, citations);
        auditService.logAudit(userId, query, answer, citations.toString());
        log.info("Query processed: userId={}, queryLength={}, chunks={}", userId, query.length(), chunks.size());
        return response;
    }

    private boolean containsAbuse(String query) {
        String lower = query.toLowerCase();
        String[] abuseTerms = { "ignore previous instructions", "ignore all instructions",
                "you are not an iata expert", "tell me something not in the document" };
        for (String term : abuseTerms) {
            if (lower.contains(term)) return true;
        }
        return false;
    }

    private QueryResponse buildResponse(String answer, List<CitationDto> citations) {
        return new QueryResponse(answer, citations, Instant.now().toString());
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
