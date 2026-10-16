package com.example.orchestrator.repository.dao;

import com.example.orchestrator.repository.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}
