package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;

public interface PacketFieldRepository extends JpaRepository<ParsingField, Long> {
}