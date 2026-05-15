package com.iata.search;

import com.iata.search.dto.QueryRequest;
import com.iata.search.dto.QueryResponse;
import com.iata.search.dto.AuditEntryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IataSearchApplicationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullFlow_QueryThenAudit() {
        QueryRequest request = new QueryRequest("integration-tester", "checked baggage allowance");
        ResponseEntity<QueryResponse> queryResponse = restTemplate.postForEntity(
                "/api/query", request, QueryResponse.class);

        assertEquals(HttpStatus.OK, queryResponse.getStatusCode());
        QueryResponse body = queryResponse.getBody();
        assertNotNull(body);
        assertNotNull(body.answer());
        assertFalse(body.answer().isBlank());

        ResponseEntity<AuditEntryDto[]> auditResponse = restTemplate.getForEntity(
                "/api/audit", AuditEntryDto[].class);

        assertEquals(HttpStatus.OK, auditResponse.getStatusCode());
        AuditEntryDto[] entries = auditResponse.getBody();
        assertNotNull(entries);
        assertTrue(entries.length > 0);

        boolean found = Arrays.stream(entries)
                .anyMatch(e -> e.queryText().contains("checked baggage allowance"));
        assertTrue(found, "Audit log should contain the submitted query");
    }

    @Test
    void emptyQuery_Returns400() {
        QueryRequest request = new QueryRequest("tester", "");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/query", request, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
