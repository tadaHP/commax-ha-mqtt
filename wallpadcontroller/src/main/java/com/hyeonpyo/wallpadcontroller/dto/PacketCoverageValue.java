package com.hyeonpyo.wallpadcontroller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PacketCoverageValue {
    private String rawKey;
    private String hexValue;
    private boolean received;
}
