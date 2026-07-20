package com.hyeonpyo.wallpadcontroller.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DevicePacketCoverageKind {
    private String name;
    private String header;
    private boolean inboundSeen;
    private boolean outboundSeen;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private List<DevicePacketObservation> inboundPackets;
    private List<DevicePacketObservation> outboundPackets;
}
