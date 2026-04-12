package com.hyeonpyo.wallpadcontroller.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLog;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogRepository;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogSearchCriteria;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogSpecification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacketLogService {

    private static final Pattern HEADER_HEX = Pattern.compile("^[0-9A-F]{2}$");

    private final PacketLogRepository packetLogRepository;
    private final DeviceTypeRepository deviceTypeRepository;

    public Page<PacketLog> findAll(Pageable pageable) {
        return packetLogRepository.findAll(pageable);
    }

    public Page<PacketLog> search(PacketLogSearchCriteria criteria, Pageable pageable) {
        return packetLogRepository.findAll(PacketLogSpecification.fromCriteria(criteria), pageable);
    }

    /**
     * 헤더(선택) + 수신 시각 범위(선택)를 조합한 검색 조건.
     *
     * @throws IllegalArgumentException 헤더·날짜·시간 형식이 잘못되었거나 시작이 끝보다 늦을 때
     */
    public PacketLogSearchCriteria buildSearchCriteria(
            String headerQuery,
            String fromDate,
            String fromTime,
            String toDate,
            String toTime) {
        Optional<String> header = Optional.empty();
        if (headerQuery != null && !headerQuery.isBlank()) {
            header = Optional.of(normalizePacketHeader(headerQuery));
        }

        Optional<LocalDateTime> from = parseRangeBound(fromDate, fromTime, true);
        Optional<LocalDateTime> to = parseRangeBound(toDate, toTime, false);

        if (from.isPresent() && to.isPresent() && from.get().isAfter(to.get())) {
            throw new IllegalArgumentException("시작 시각이 끝 시각보다 늦을 수 없습니다.");
        }

        return new PacketLogSearchCriteria(header, from, to);
    }

    private Optional<LocalDateTime> parseRangeBound(String dateStr, String timeStr, boolean startOfRange) {
        String d = dateStr == null ? "" : dateStr.trim();
        String t = timeStr == null ? "" : timeStr.trim();
        if (d.isEmpty()) {
            if (!t.isEmpty()) {
                throw new IllegalArgumentException(
                        (startOfRange ? "시작" : "끝") + " 날짜를 비우면 시간만 입력할 수 없습니다.");
            }
            return Optional.empty();
        }
        LocalDate date;
        try {
            date = LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜는 yyyy-MM-dd 형식이어야 합니다. 예: 2026-04-12", e);
        }
        LocalTime time;
        if (t.isEmpty()) {
            time = startOfRange ? LocalTime.MIN : LocalTime.of(23, 59, 59, 999_999_999);
        } else {
            time = parseTimeFlexible(t);
        }
        return Optional.of(LocalDateTime.of(date, time));
    }

    private static LocalTime parseTimeFlexible(String s) {
        try {
            return LocalTime.parse(s, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm"));
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("시간 형식이 올바르지 않습니다. 예: 09:30 또는 14:05:00", e2);
            }
        }
    }

    /** 공백 제거·대문자화한 2자리 헤더, 또는 형식 오류 시 예외 */
    public String normalizePacketHeader(String input) {
        String compact = input.replaceAll("\\s+", "").toUpperCase();
        if (!HEADER_HEX.matcher(compact).matches()) {
            throw new IllegalArgumentException("헤더는 2자리 16진수만 가능합니다. 예: B0, 30");
        }
        return compact;
    }

    public Map<String, List<PacketFilterTypeDto>> getGroupedPacketTypes() {
        Map<String, List<PacketFilterTypeDto>> groupedPacketTypes = new TreeMap<>();
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();

        for (DeviceType deviceType : deviceTypes) {
            // header를 기준으로 중복을 제거하기 위해 Map을 사용 (PacketParser와 동일한 방식)
            Map<String, PacketType> uniquePacketTypesByHeader = new LinkedHashMap<>();
            for (PacketType packetType : deviceType.getPacketTypes()) {
                uniquePacketTypesByHeader.put(packetType.getHeader(), packetType);
            }

            // 중복이 제거된 PacketType으로 DTO 리스트 생성
            List<PacketFilterTypeDto> dtos = new ArrayList<>();
            for (PacketType packetType : uniquePacketTypesByHeader.values()) {
                dtos.add(new PacketFilterTypeDto(packetType));
            }

            if (!dtos.isEmpty()) {
                groupedPacketTypes.put(deviceType.getName(), dtos);
            }
        }
        return groupedPacketTypes;
    }

    @Getter
    public static class PacketFilterTypeDto {
        private final String header;
        private final String displayName;

        public PacketFilterTypeDto(PacketType packetType) {
            this.header = packetType.getHeader();
            this.displayName = String.format("%s-%s", packetType.getDeviceType().getName(), packetType.getKind());
        }
    }
}
