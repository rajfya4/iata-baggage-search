package com.iata.search.service;

import com.iata.search.dto.AuditEntryDto;
import com.iata.search.entity.AuditLog;
import com.iata.search.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public AuditLog logAudit(String userId, String queryText, String responseText, String citations) {
        AuditLog entry = new AuditLog(userId, queryText, responseText, citations);
        AuditLog saved = repository.save(entry);
        log.debug("Audit entry saved: id={}", saved.getId());
        return saved;
    }

    public List<AuditEntryDto> getAllAudits() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public AuditEntryDto getAuditById(Long id) {
        AuditLog entry = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Audit entry not found: " + id));
        return toDto(entry);
    }

    private AuditEntryDto toDto(AuditLog entry) {
        return new AuditEntryDto(
                entry.getId(),
                entry.getUserId(),
                entry.getQueryText(),
                entry.getResponseText(),
                entry.getCitations(),
                entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : null
        );
    }
}
