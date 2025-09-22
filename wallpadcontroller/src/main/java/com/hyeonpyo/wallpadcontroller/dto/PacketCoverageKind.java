package com.hyeonpyo.wallpadcontroller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PacketCoverageKind {
    private String name;
    private String header;
    private boolean received;
    private String description;
}
