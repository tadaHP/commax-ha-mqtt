package com.hyeonpyo.wallpadcontroller.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SeenPacketEntry {

    private final String rawData;
    private final String header;
    private final LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;

    public SeenPacketEntry(String rawData, String header, LocalDateTime firstSeenAt, LocalDateTime lastSeenAt) {
        this.rawData = rawData;
        this.header = header;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
    }

    public void touchLastSeenAt(LocalDateTime seenAt) {
        if (seenAt.isAfter(lastSeenAt)) {
            lastSeenAt = seenAt;
        }
    }
}
