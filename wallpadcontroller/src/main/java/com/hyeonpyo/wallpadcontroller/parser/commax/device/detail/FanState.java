package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.HashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.Data;

@Data
public class FanState implements DeviceState {
    private final String speed;
    private final String mode;
    private final String power;

    public FanState(String speed, String mode) {
        this.speed = speed;
        this.mode = mode;
        if(mode == null || mode.isEmpty()) {
            this.power = "OFF";
        } else {
            this.power = mode.equals("off") ? "OFF" : "ON";
        }
    }


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
