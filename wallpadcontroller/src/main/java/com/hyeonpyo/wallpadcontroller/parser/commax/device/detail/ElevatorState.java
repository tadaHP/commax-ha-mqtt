package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.HashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ElevatorState implements DeviceState{
    private final String power;
    private final String floor;


    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("power", power);
        map.put("floor", floor);
        return map;
    }


    @Override
    public String toJson() {
        return String.format("{\"power\":\"%s\", \"floor\":\"%s\"}", power, floor);
    }
}
