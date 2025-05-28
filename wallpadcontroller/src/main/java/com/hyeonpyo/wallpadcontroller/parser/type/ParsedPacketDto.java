package com.hyeonpyo.wallpadcontroller.parser.type;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ParsedPacketDto {
    private final DeviceType deviceType;
    private final PacketType packetType;
    private final Map<String, String> fields;
}
