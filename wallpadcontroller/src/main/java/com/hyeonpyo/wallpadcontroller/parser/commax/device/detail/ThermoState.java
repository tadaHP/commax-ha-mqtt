package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ThermoState implements DeviceState {
    private String power;
    private String action;
    private String curTemp;
    private String setTemp;

    @Override
    public String toJson() {
        return String.format("{\"power\": \"%s\", \"action\": \"%s\", \"curTemp\": \"%s\", \"setTemp\": \"%s\"}",
                power, action, curTemp, setTemp);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (power != null) map.put("power", power);
        if (curTemp != null) map.put("curTemp", curTemp);
        if (setTemp != null) map.put("setTemp", setTemp);
        if (action != null) map.put("action", action);
        return map;
    }

}