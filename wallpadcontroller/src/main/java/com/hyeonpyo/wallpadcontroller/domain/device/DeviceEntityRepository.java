package com.hyeonpyo.wallpadcontroller.domain.device;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceEntityRepository extends JpaRepository<DeviceEntity, String> {
    boolean existsByUniqueId(String uniqueId);
}
