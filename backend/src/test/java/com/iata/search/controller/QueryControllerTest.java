package com.iata.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iata.search.dto.QueryRequest;
import com.iata.search.dto.QueryResponse;
import com.iata.search.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueryService queryService;

    @Test
    void postQuery_ValidRequest_Returns200() throws Exception {
        QueryResponse mockResponse = new QueryResponse("Answer text", List.of(), "2026-05-15T12:00:00Z");
        when(queryService.processQuery(any())).thenReturn(mockResponse);

        QueryRequest request = new QueryRequest("agent1", "What is checked bag fee?");
        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Answer text"));
    }

    @Test
    void postQuery_EmptyQuery_Returns400() throws Exception {
        QueryRequest request = new QueryRequest("agent1", "");
        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postQuery_NullUserId_Returns400() throws Exception {
        String json = "{\"query\":\"test query\"}";
        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
