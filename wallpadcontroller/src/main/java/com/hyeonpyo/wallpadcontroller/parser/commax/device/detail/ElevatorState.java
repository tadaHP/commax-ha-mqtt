package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.HashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ElevatorState implements DeviceState {

    /** EV 호기(패킷 바이트 1, readable/hex). */
    private final String car;
    /** 현재 층·위치(패킷 바이트 2). */
    private final String floor;

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if (car != null && !car.isBlank()) {
            map.put("car", car);
        }
        if (floor != null && !floor.isBlank()) {
            map.put("floor", floor);
        }
        return map;
    }

    @Override
    public String toJson() {
        String c = car == null ? "" : car;
        String f = floor == null ? "" : floor;
        return String.format("{\"car\":\"%s\", \"floor\":\"%s\"}", c, f);
    }
}
