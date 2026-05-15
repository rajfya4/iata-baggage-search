package com.iata.search.service;

import com.iata.search.dto.AuditEntryDto;
import com.iata.search.entity.AuditLog;
import com.iata.search.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository repository;

    @Test
    void saveAuditLog_ShouldPersistAndReturnEntry() {
        AuditLog saved = auditService.logAudit("testUser", "What is the fee?", "Response text", "Section 3.2");
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("testUser", saved.getUserId());
        assertEquals("What is the fee?", saved.getQueryText());
        assertEquals("Response text", saved.getResponseText());
        assertEquals("Section 3.2", saved.getCitations());
    }

    @Test
    void getAllAudits_ShouldReturnDescendingOrder() throws InterruptedException {
        repository.deleteAll();
        auditService.logAudit("user1", "q1", "r1", "c1");
        Thread.sleep(10);
        auditService.logAudit("user2", "q2", "r2", "c2");
        Thread.sleep(10);
        auditService.logAudit("user3", "q3", "r3", "c3");

        List<AuditEntryDto> audits = auditService.getAllAudits();
        assertEquals(3, audits.size());
        assertTrue(audits.get(0).createdAt().compareTo(audits.get(1).createdAt()) >= 0);
    }

    @Test
    void getAuditById_NotFound_ShouldThrow() {
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> auditService.getAuditById(99999L));
    }
}
