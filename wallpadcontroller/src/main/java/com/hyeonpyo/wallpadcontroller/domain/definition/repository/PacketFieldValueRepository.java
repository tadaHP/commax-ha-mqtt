package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketFieldValue;


public interface PacketFieldValueRepository extends JpaRepository<PacketFieldValue, Long> {
}