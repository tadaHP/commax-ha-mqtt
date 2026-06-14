package com.hyeonpyo.wallpadcontroller.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacketDirection;
import com.hyeonpyo.wallpadcontroller.service.SeenPacketEntry;
import com.hyeonpyo.wallpadcontroller.service.SeenPacketMemoryStore;
import com.hyeonpyo.wallpadcontroller.service.SeenPacketMemoryStore.SeenPacketPage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seen-packets")
@RequiredArgsConstructor
public class SeenPacketRestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SeenPacketMemoryStore seenPacketMemoryStore;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String header,
            @RequestParam(required = false) String direction) {
        int sz = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pg = Math.max(page, 0);
        SeenPacketDirection dir = parseDirection(direction);

        SeenPacketPage result = seenPacketMemoryStore.list(header, dir, pg, sz);
        List<Map<String, Object>> rows = result.content().stream()
                .map(this::toRow)
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", rows);
        body.put("number", result.number());
        body.put("size", result.size());
        body.put("totalElements", result.totalElements());
        body.put("totalPages", result.totalPages());
        body.put("first", result.number() == 0);
        body.put("last", result.totalPages() == 0 || result.number() >= result.totalPages() - 1);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toRow(SeenPacketEntry entry) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rawData", entry.getRawData());
        row.put("header", entry.getHeader());
        row.put("direction", entry.getDirection());
        row.put("firstSeenAt", entry.getFirstSeenAt());
        row.put("lastSeenAt", entry.getLastSeenAt());
        return row;
    }

    private SeenPacketDirection parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return null;
        }
        return SeenPacketDirection.valueOf(direction.trim().toUpperCase());
    }
}
