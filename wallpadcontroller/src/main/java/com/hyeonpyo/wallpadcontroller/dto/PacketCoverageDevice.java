package com.hyeonpyo.wallpadcontroller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PacketCoverageDevice {
    private String name;
    private List<PacketCoverageKind> kinds;
}
