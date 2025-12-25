package com.hyeonpyo.wallpadcontroller.domain.packethistory;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface PacketLogRepository extends JpaRepository<PacketLog, Long> {

    @Query("SELECT DISTINCT SUBSTRING(REPLACE(p.rawData, ' ', ''), 1, 2) FROM PacketLog p WHERE p.status = 'SUCCESS'")
    Set<String> findDistinctSuccessHeaders();

    List<PacketLog> findByStatus(LogStatus status);
}
