package com.hyeonpyo.wallpadcontroller.domain.device;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceEntityRepository extends JpaRepository<DeviceEntity, String> {
    boolean existsByUniqueId(String uniqueId);

    List<DeviceEntity> findAllByUsedTrue(Sort sort);

    List<DeviceEntity> findAllByUsedFalse(Sort sort);

    List<DeviceEntity> findAllByUsedIsNull(Sort sort);
}
