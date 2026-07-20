package com.hyeonpyo.wallpadcontroller.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DevicePacketCoverageDeviceType {
    private String name;
    private String type;
    private List<DevicePacketCoverageInstance> instances;
}
