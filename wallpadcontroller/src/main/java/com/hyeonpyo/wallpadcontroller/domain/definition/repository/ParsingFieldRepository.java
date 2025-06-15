package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;

public interface ParsingFieldRepository extends JpaRepository<ParsingField, Long> {
    List<ParsingField> findByPacketType(PacketType packetType);
}