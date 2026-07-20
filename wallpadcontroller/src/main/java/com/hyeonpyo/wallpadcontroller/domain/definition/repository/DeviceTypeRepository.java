package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long>, DeviceTypeRepositoryCustom {
}