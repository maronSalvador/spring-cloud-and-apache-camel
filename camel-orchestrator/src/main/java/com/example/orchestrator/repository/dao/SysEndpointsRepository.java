package com.example.orchestrator.repository.dao;

import com.example.orchestrator.repository.entity.SysEndpointsEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SysEndpointsRepository extends JpaRepository<SysEndpointsEntity, Long> {
    SysEndpointsEntity findByName(String name);
}
