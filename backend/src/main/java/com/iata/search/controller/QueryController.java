package com.iata.search.controller;

import com.iata.search.dto.QueryRequest;
import com.iata.search.dto.QueryResponse;
import com.iata.search.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
@Tag(name = "Query", description = "Baggage policy query endpoints")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    @Operation(summary = "Submit a baggage policy query")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Query processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QueryResponse> submitQuery(@Valid @RequestBody QueryRequest request) {
        long start = System.currentTimeMillis();
        QueryResponse response = queryService.processQuery(request);
        long elapsed = System.currentTimeMillis() - start;
        log.info("Query processed in {}ms", elapsed);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Health check for query endpoint")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Query endpoint is active");
    }
}
