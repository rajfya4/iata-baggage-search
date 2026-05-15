package com.iata.search.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for a baggage policy query")
public record QueryRequest(
    @NotBlank(message = "userId must not be blank")
    @Schema(description = "User identifier", example = "agent123")
    String userId,

    @NotBlank(message = "query must not be blank")
    @Schema(description = "Natural language query about baggage policy", example = "What is the checked bag fee for international flights?")
    String query
) {}
