package com.hyeonpyo.wallpadcontroller.parser.commax.type;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParsedPacket {
    private String deviceName;
    private PacketKind kind;
    // private Map<String, String> parsedFields; //추후 삭제
    private DeviceState parsedState; 
}
