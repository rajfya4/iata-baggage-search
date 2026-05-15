package com.iata.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response containing the AI-generated answer and source citations")
public record QueryResponse(
    @Schema(description = "AI-generated answer text")
    String answer,

    @Schema(description = "Source citations from the IATA guidance document")
    List<CitationDto> citations,

    @Schema(description = "Response timestamp (ISO-8601)", example = "2026-05-15T12:00:00Z")
    String timestamp
) {

    @Schema(description = "A single citation referencing a section in the guidance document")
    public record CitationDto(
        @Schema(description = "Section reference", example = "Section 3.2")
        String section,
        @Schema(description = "Excerpt text from the document")
        String text,
        @Schema(description = "Relevance score (0.0 to 1.0)")
        double relevanceScore
    ) {}
}
