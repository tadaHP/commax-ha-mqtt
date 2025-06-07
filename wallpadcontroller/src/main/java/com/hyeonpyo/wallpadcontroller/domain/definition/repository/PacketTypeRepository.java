package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;

public interface PacketTypeRepository extends JpaRepository<PacketType, Long> {
}