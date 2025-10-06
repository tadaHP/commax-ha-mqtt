package com.hyeonpyo.wallpadcontroller.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PacketCoverageField {
    private final String name;
    private final List<PacketCoverageValue> definedValues;
    private final List<PacketCoverageValue> newValues;

    public PacketCoverageField(String name, List<PacketCoverageValue> definedValues, List<PacketCoverageValue> newValues) {
        this.name = name;
        this.definedValues = definedValues;
        this.newValues = newValues;
    }
}
