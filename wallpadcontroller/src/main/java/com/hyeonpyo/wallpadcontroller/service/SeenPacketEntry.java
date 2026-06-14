package com.hyeonpyo.wallpadcontroller.service;

import java.time.LocalDateTime;

import com.hyeonpyo.wallpadcontroller.domain.coverage.SeenPacketDirection;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SeenPacketEntry {

    private final String rawData;
    private final String header;
    private final SeenPacketDirection direction;
    private final LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;

    public SeenPacketEntry(String rawData, String header, SeenPacketDirection direction, LocalDateTime firstSeenAt, LocalDateTime lastSeenAt) {
        this.rawData = rawData;
        this.header = header;
        this.direction = direction;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
    }

    public void touchLastSeenAt(LocalDateTime seenAt) {
        if (seenAt.isAfter(lastSeenAt)) {
            lastSeenAt = seenAt;
        }
    }
}
