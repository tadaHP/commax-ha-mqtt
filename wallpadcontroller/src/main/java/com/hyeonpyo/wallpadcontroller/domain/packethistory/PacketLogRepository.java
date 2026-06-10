package com.hyeonpyo.wallpadcontroller.domain.packethistory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PacketLogRepository extends JpaRepository<PacketLog, Long>, JpaSpecificationExecutor<PacketLog> {

    @Query("SELECT DISTINCT SUBSTRING(REPLACE(p.rawData, ' ', ''), 1, 2) FROM PacketLog p WHERE p.status = 'SUCCESS'")
    Set<String> findDistinctSuccessHeaders();

    List<PacketLog> findByStatus(LogStatus status);

    @Query("SELECT p.id FROM PacketLog p WHERE p.receivedAt < :cutoff ORDER BY p.receivedAt ASC, p.id ASC")
    List<Long> findIdsOlderThan(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM PacketLog p WHERE p.id IN :ids")
    int deleteByIdInBulk(@Param("ids") Collection<Long> ids);
}
