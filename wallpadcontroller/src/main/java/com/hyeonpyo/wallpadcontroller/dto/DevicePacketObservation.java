package com.hyeonpyo.wallpadcontroller.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DevicePacketObservation {
    private String rawData;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
}
