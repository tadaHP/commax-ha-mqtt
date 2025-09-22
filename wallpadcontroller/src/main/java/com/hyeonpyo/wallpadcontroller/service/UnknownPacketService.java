package com.hyeonpyo.wallpadcontroller.service;

import com.hyeonpyo.wallpadcontroller.domain.unknownpacket.UnknownPacket;
import com.hyeonpyo.wallpadcontroller.domain.unknownpacket.UnknownPacketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnknownPacketService {

    private final UnknownPacketRepository unknownPacketRepository;

    public Page<UnknownPacket> findAll(Pageable pageable) {
        return unknownPacketRepository.findAll(pageable);
    }
}
