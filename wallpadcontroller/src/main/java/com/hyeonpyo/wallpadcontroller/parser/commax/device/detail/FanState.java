package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

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

    @Override
    public String toJson() {
        return String.format("{\"speed\": \"%s\", \"mode\": \"%s\"}", speed, mode);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (speed != null) map.put("speed", speed);
        if (mode != null) map.put("mode", mode);
        return map;
    }
}
