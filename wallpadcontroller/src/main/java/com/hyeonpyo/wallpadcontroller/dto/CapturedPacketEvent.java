package com.hyeonpyo.wallpadcontroller.dto;

import java.time.LocalDateTime;

public record CapturedPacketEvent(
        String rawData,
        String status,
        String notes,
        LocalDateTime receivedAt) {
}
