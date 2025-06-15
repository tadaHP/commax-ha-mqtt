package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.CommandMappingDetail;

public interface CommandMappingDetailRepository extends JpaRepository<CommandMappingDetail, Long> {
    
}
