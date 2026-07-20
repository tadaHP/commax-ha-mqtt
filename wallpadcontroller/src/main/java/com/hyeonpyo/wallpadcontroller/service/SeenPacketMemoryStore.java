package com.hyeonpyo.wallpadcontroller.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacket;
import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacketDirection;
import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacketRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeenPacketMemoryStore {

    private final SeenPacketRepository seenPacketRepository;
    private final TransactionTemplate transactionTemplate;

    private final ConcurrentHashMap<PacketKey, SeenPacketEntry> byPacket = new ConcurrentHashMap<>();

    @PostConstruct
    void loadFromDatabase() {
        List<SeenPacket> rows = seenPacketRepository.findAllByOrderByLastSeenAtDesc();
        for (SeenPacket row : rows) {
            SeenPacketEntry entry = toEntry(row);
            byPacket.put(PacketKey.of(entry.getRawData(), entry.getDirection()), entry);
        }
        log.info("seen_packet 메모리 로드 완료: {}건", byPacket.size());
    }

    /**
     * SUCCESS 패킷 기록. 이미 본 rawData면 메모리 lastSeenAt만 갱신하고 DB는 건드리지 않음.
     *
     * @return true면 이번에 처음 본 패킷
     */
    public boolean recordSuccessPacket(String rawHex, LocalDateTime seenAt) {
        return recordPacket(rawHex, SeenPacketDirection.INBOUND, seenAt);
    }

    public boolean recordOutboundPacket(String rawHex, LocalDateTime seenAt) {
        return recordPacket(rawHex, SeenPacketDirection.OUTBOUND, seenAt);
    }

    public boolean recordPacket(String rawHex, SeenPacketDirection direction, LocalDateTime seenAt) {
        String normalized = normalizeRawHex(rawHex);
        SeenPacketDirection packetDirection = direction == null ? SeenPacketDirection.INBOUND : direction;
        if (normalized == null || seenAt == null) {
            return false;
        }
        PacketKey key = PacketKey.of(normalized, packetDirection);

        SeenPacketEntry existing = byPacket.get(key);
        if (existing != null) {
            existing.touchLastSeenAt(seenAt);
            return false;
        }

        SeenPacketEntry created = new SeenPacketEntry(
                normalized,
                extractHeader(normalized),
                packetDirection,
                seenAt,
                seenAt);

        synchronized (lockFor(key)) {
            SeenPacketEntry existingAfterLock = byPacket.get(key);
            if (existingAfterLock != null) {
                existingAfterLock.touchLastSeenAt(seenAt);
                return false;
            }
            transactionTemplate.executeWithoutResult(status -> seenPacketRepository.save(SeenPacket.builder()
                    .rawData(created.getRawData())
                    .header(created.getHeader())
                    .direction(created.getDirection())
                    .firstSeenAt(created.getFirstSeenAt())
                    .lastSeenAt(created.getLastSeenAt())
                    .build()));
            byPacket.put(key, created);
            return true;
        }
    }

    private Object lockFor(PacketKey packetKey) {
        return internLocks.computeIfAbsent(packetKey, key -> new Object());
    }

    private final ConcurrentHashMap<PacketKey, Object> internLocks = new ConcurrentHashMap<>();

    public Map<String, Set<String>> buildReceivedData() {
        Map<String, Set<String>> receivedData = new HashMap<>();
        for (SeenPacketEntry entry : byPacket.values()) {
            if (entry.getDirection() != SeenPacketDirection.INBOUND) {
                continue;
            }
            accumulateRawHex(receivedData, entry.getRawData());
        }
        return receivedData;
    }

    public SeenPacketPage list(String headerFilter, int page, int size) {
        return list(headerFilter, null, page, size);
    }

    public List<SeenPacketEntry> snapshot() {
        return byPacket.values().stream()
                .sorted(Comparator.comparing(SeenPacketEntry::getLastSeenAt).reversed())
                .toList();
    }

    public SeenPacketPage list(String headerFilter, SeenPacketDirection directionFilter, int page, int size) {
        String header = normalizeHeaderFilter(headerFilter);

        List<SeenPacketEntry> filtered = byPacket.values().stream()
                .filter(entry -> header == null || header.equals(entry.getHeader()))
                .filter(entry -> directionFilter == null || directionFilter == entry.getDirection())
                .sorted(Comparator.comparing(SeenPacketEntry::getLastSeenAt).reversed())
                .toList();

        int totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);
        List<SeenPacketEntry> slice = filtered.subList(from, to);

        return new SeenPacketPage(slice, page, size, totalElements, totalPages);
    }

    public int count() {
        return byPacket.size();
    }

    static String normalizeRawHex(String rawHex) {
        if (rawHex == null || rawHex.isBlank()) {
            return null;
        }
        String[] parts = rawHex.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(parts[i].toUpperCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static String extractHeader(String normalizedRawHex) {
        return normalizedRawHex.substring(0, 2);
    }

    private static String normalizeHeaderFilter(String headerFilter) {
        if (headerFilter == null || headerFilter.isBlank()) {
            return null;
        }
        return headerFilter.trim().toUpperCase(Locale.ROOT);
    }

    private static void accumulateRawHex(Map<String, Set<String>> receivedData, String rawHex) {
        String[] hexValues = rawHex.split(" ");
        String header = hexValues[0];
        for (int i = 1; i < hexValues.length; i++) {
            String key = header + "-" + (i + 1);
            receivedData.computeIfAbsent(key, k -> new HashSet<>()).add(hexValues[i]);
        }
    }

    private static SeenPacketEntry toEntry(SeenPacket row) {
        return new SeenPacketEntry(
                row.getRawData(),
                row.getHeader(),
                row.getDirection() == null ? SeenPacketDirection.INBOUND : row.getDirection(),
                row.getFirstSeenAt(),
                row.getLastSeenAt());
    }

    public record SeenPacketPage(
            List<SeenPacketEntry> content,
            int number,
            int size,
            long totalElements,
            int totalPages) {
    }

    private record PacketKey(String rawData, SeenPacketDirection direction) {
        private static PacketKey of(String rawData, SeenPacketDirection direction) {
            return new PacketKey(rawData, direction == null ? SeenPacketDirection.INBOUND : direction);
        }
    }
}
