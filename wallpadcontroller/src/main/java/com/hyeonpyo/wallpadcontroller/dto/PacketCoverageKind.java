package com.hyeonpyo.wallpadcontroller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PacketCoverageKind {
    private String name;
    private String header;
    private CoverageStatus status;
    private List<PacketCoverageField> fields;
}
