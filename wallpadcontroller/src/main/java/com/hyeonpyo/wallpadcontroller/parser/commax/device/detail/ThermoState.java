package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.Data;

@Data
public class ThermoState implements DeviceState {
    private String mode;
    private String action;
    private String curTemp;
    private String setTemp;

    public ThermoState(String rawMode, String curTemp, String setTemp) {
        String safeRawMode = rawMode != null ? rawMode : "off"; // null 방지용 기본값
        
        this.mode = switch (safeRawMode) {
            case "idle", "heating" -> "heat";
            default -> "off";
        };
        this.action = safeRawMode;
        this.curTemp = curTemp;
        this.setTemp = setTemp;
    }

    @Override
    public String toJson() {
        return String.format("{\"mode\": \"%s\", \"action\": \"%s\", \"curTemp\": \"%s\", \"setTemp\": \"%s\"}",
                mode, action, curTemp, setTemp);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (mode != null) map.put("mode", mode);
        if (curTemp != null) map.put("curTemp", curTemp);
        if (setTemp != null) map.put("setTemp", setTemp);
        if (action != null) map.put("action", action);
        return map;
    }
}