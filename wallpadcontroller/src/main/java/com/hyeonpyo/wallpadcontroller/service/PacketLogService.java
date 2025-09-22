package com.hyeonpyo.wallpadcontroller.service;

import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLog;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PacketLogService {

    private final PacketLogRepository packetLogRepository;

    public Page<PacketLog> findAll(Pageable pageable) {
        return packetLogRepository.findAll(pageable);
    }
}
