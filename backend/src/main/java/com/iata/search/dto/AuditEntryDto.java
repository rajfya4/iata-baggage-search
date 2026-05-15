package com.iata.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Audit log entry")
public record AuditEntryDto(
    @Schema(description = "Audit entry ID")
    Long id,
    @Schema(description = "User who submitted the query")
    String userId,
    @Schema(description = "Original query text")
    String queryText,
    @Schema(description = "AI response text")
    String response,
    @Schema(description = "Source citations")
    String citations,
    @Schema(description = "Creation timestamp", example = "2026-05-15T12:00:00")
    String createdAt
) {}
