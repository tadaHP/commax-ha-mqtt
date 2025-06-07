package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FanState implements DeviceState {
    private String speed;
    private String mode;
    private final String power;


    @Override
    public String toJson() {
        return String.format("{\"speed\": \"%s\", \"mode\": \"%s\"}", speed, mode);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("speed", speed);
        map.put("mode", mode);
        map.put("power", power);
        return map;
    }
}
