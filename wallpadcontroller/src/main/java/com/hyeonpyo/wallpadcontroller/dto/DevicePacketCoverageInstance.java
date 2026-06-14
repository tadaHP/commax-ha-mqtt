package com.hyeonpyo.wallpadcontroller.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DevicePacketCoverageInstance {
    private int index;
    private String displayName;
    private List<DevicePacketCoverageKind> packetKinds;
}
