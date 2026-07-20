package com.hyeonpyo.wallpadcontroller.domain.device;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceEntityRepository extends JpaRepository<DeviceEntity, String> {
    boolean existsByUniqueId(String uniqueId);

    List<DeviceEntity> findAllByUsedFalse(Sort sort);

    /** {@code used}가 true이거나 null(레거시·노출)인 기기 */
    List<DeviceEntity> findAllByUsedIsNullOrUsedTrue(Sort sort);
}
