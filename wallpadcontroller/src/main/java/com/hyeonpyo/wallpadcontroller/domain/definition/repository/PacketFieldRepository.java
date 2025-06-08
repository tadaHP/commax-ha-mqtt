package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketField;

public interface PacketFieldRepository extends JpaRepository<PacketField, Long> {
}