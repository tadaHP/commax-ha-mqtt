package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.CommandMappingRule;

public interface CommandMappingRuleRepository extends JpaRepository<CommandMappingRule, Long> {
    
}
