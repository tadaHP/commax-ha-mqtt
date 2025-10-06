package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.HashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.Data;

@Data
public class FanState implements DeviceState {
    private final String speed;
    private final String power;

    public FanState(String speed, String power) {
        this.speed = speed;
        this.power = power;
    }


    @Override
    public String toJson() {
        return String.format("{\"speed\": \"%s\", \"power\": \"%s\"}", speed, power);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if (speed != null) {
            map.put("speed", speed);
        }
        if (power != null) {
            map.put("power", power);
        }
        return map;
    }
}
