package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;

public interface PacketTypeRepository extends JpaRepository<PacketType, Long> {
    Optional<PacketType> findByDeviceTypeAndKind(DeviceType deviceType, String kind);
}