package com.hyeonpyo.wallpadcontroller.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLog;
import com.hyeonpyo.wallpadcontroller.domain.packethistory.PacketLogSearchCriteria;
import com.hyeonpyo.wallpadcontroller.service.PacketLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/packet-logs")
@RequiredArgsConstructor
public class PacketLogRestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PacketLogService packetLogService;

    /**
     * @param page      0부터
     * @param size      페이지 크기 (1~100)
     * @param header    패킷 첫 바이트 헤더(2자리 16진수). 비우면 전체.
     * @param fromDate  수신 시작일 {@code yyyy-MM-dd}
     * @param fromTime  수신 시작 시각(HH:mm 또는 ISO_LOCAL_TIME). 날짜만 있으면 00:00:00.
     * @param toDate    수신 종료일
     * @param toTime    수신 종료 시각. 날짜만 있으면 해당일 23:59:59.999999999.
     *                  날짜·시간은 DB의 {@code receivedAt}(로컬 날짜시각)과 같은 벽시계 기준이며,
     *                  기본은 {@code spring.jpa.properties.hibernate.jdbc.time_zone}(미설정 시 {@code Asia/Seoul})에 맞춥니다.
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String header,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String fromTime,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String toTime) {
        int sz = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pg = Math.max(page, 0);
        // 정렬은 PacketLogSpecification에서 receivedAt·id 내림차순으로 고정(조건 쿼리에서도 일관).
        var pageable = PageRequest.of(pg, sz);
        Page<PacketLog> result;
        try {
            PacketLogSearchCriteria criteria =
                    packetLogService.buildSearchCriteria(header, fromDate, fromTime, toDate, toTime);
            result = packetLogService.search(criteria, pageable);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        List<Map<String, Object>> rows = result.getContent().stream()
                .map(this::toRow)
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", rows);
        body.put("number", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("first", result.isFirst());
        body.put("last", result.isLast());
        return ResponseEntity.ok(body);
    }

    /** 정의된 패킷 타입별 헤더 목록(UI 필터용) */
    @GetMapping("/headers")
    public ResponseEntity<Map<String, List<PacketLogService.PacketFilterTypeDto>>> listHeaderOptions() {
        return ResponseEntity.ok(packetLogService.getGroupedPacketTypes());
    }

    private Map<String, Object> toRow(PacketLog p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("receivedAt", p.getReceivedAt());
        m.put("status", p.getStatus().name());
        m.put("rawData", p.getRawData());
        m.put("notes", p.getNotes());
        return m;
    }
}
