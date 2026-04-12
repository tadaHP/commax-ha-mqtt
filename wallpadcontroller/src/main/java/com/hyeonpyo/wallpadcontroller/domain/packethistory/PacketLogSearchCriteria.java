package com.hyeonpyo.wallpadcontroller.domain.packethistory;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 패킷 로그 목록 필터(헤더 + 수신 시각 범위).
 */
public record PacketLogSearchCriteria(
        Optional<String> header,
        Optional<LocalDateTime> receivedFromInclusive,
        Optional<LocalDateTime> receivedToInclusive) {
}
