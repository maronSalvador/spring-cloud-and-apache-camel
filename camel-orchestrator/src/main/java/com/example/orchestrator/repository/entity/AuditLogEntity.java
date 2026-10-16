package com.example.orchestrator.repository.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntity {

    public AuditLogEntity(String correlationId, String interfaceName, String response) {
        this.correlationId = correlationId;
        this.interfaceName = interfaceName;
        this.response = response;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "correlation_id")
    String correlationId;

    @Column(name = "interface_name")
    String interfaceName;

    @Column(name = "response")
    String response;


}
