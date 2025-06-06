package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FanState implements DeviceState {
    private String power;
    private String speed;

    @Override
    public String toJson() {
        return String.format("{\"power\": \"%s\", \"speed\": \"%s\"}", power, speed);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("power", power);
        if (speed != null) map.put("speed", speed);
        return map;
    }

}
