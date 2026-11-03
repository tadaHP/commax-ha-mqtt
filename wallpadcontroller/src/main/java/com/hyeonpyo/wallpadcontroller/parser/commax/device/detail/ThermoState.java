package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.math.BigDecimal;
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
    private String away;

    public ThermoState(String rawMode, String curTemp, String setTemp) {
        String safeRawMode = rawMode != null ? rawMode : "off"; // null 방지용 기본값
        
        // Existing DBs leave unmapped state byte 84 as its raw HEX string.
        boolean awayMode = "84".equalsIgnoreCase(safeRawMode) || "away".equalsIgnoreCase(safeRawMode);
        this.away = awayMode ? "ON" : "OFF";
        this.mode = awayMode ? "heat" : switch (safeRawMode) {
            case "idle", "heating", "81", "83" -> "heat";
            default -> "off";
        };
        // On this installation 81 means enabled, including while heating.
        // Display heating demand from the temperatures, not the legacy idle label.
        this.action = awayMode ? "None" : "heat".equals(this.mode)
                ? temperatureAction(curTemp, setTemp) : "off";
        this.curTemp = curTemp;
        this.setTemp = setTemp;
    }

    private static String temperatureAction(String current, String target) {
        if (current == null || target == null) return "None";
        try {
            return new BigDecimal(target.trim()).compareTo(new BigDecimal(current.trim())) > 0
                    ? "heating" : "idle";
        } catch (NumberFormatException e) {
            return "None";
        }
    }

    @Override
    public String toJson() {
        return String.format("{\"mode\": \"%s\", \"action\": \"%s\", \"curTemp\": \"%s\", \"setTemp\": \"%s\", \"away\": \"%s\"}",
                mode, action, curTemp, setTemp, away);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (mode != null) map.put("mode", mode);
        if (curTemp != null) map.put("curTemp", curTemp);
        if (setTemp != null) map.put("setTemp", setTemp);
        if (action != null) map.put("action", action);
        map.put("away", away);
        return map;
    }
}