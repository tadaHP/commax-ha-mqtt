package com.hyeonpyo.wallpadcontroller.domain.coverage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeenPacketRepository extends JpaRepository<SeenPacket, Long> {

    List<SeenPacket> findAllByOrderByLastSeenAtDesc();
}
