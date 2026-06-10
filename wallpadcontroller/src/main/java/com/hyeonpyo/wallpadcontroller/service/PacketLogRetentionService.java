package com.hyeonpyo.wallpadcontroller.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogRepository;
import com.hyeonpyo.wallpadcontroller.properties.PacketLogRetentionProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PacketLogRetentionService {

    private final PacketLogRepository packetLogRepository;
    private final PacketLogRetentionProperties properties;

    @Scheduled(
            initialDelayString = "${packet-log.retention.initial-delay-ms:86400000}",
            fixedDelayString = "${packet-log.retention.cleanup-interval-ms:86400000}")
    public void cleanupOldPacketLogs() {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getDays() <= 0) {
            log.warn("패킷 로그 보존 기간이 0 이하라 정리를 건너뜁니다: days={}", properties.getDays());
            return;
        }

        int batchSize = Math.max(100, properties.getBatchSize());
        LocalDateTime cutoff = LocalDateTime.now().minusDays(properties.getDays());
        int totalDeleted = 0;

        while (true) {
            List<Long> ids = packetLogRepository.findIdsOlderThan(cutoff, PageRequest.of(0, batchSize));
            if (ids.isEmpty()) {
                break;
            }
            int deleted = packetLogRepository.deleteByIdInBulk(ids);
            totalDeleted += deleted;

            if (deleted < ids.size()) {
                log.warn("패킷 로그 일부만 삭제됨: selected={}, deleted={}, cutoff={}", ids.size(), deleted, cutoff);
                break;
            }
        }

        if (totalDeleted > 0) {
            log.info("패킷 로그 보존 정책 적용 완료: cutoff={}, deleted={} rows", cutoff, totalDeleted);
        }
    }
}
