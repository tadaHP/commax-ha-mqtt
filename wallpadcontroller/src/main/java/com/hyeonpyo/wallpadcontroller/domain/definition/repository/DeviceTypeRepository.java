package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {
    @Query("SELECT dt FROM DeviceType dt LEFT JOIN FETCH dt.packetTypes pt LEFT JOIN FETCH pt.fields f LEFT JOIN FETCH f.valueMappings")
    List<DeviceType> findAllWithFullStructure();
}