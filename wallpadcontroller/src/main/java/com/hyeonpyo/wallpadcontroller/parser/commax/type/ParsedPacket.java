package com.hyeonpyo.wallpadcontroller.parser.commax.type;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParsedPacket {
    private String deviceName;
    private PacketKind kind;
    private Map<String, String> parsedFields;
}
