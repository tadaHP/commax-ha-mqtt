package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OutletState implements DeviceState {
    private String power;
    private String watt;
    private String ecomode;
    private String cutoff;

    @Override
    public String toJson() {
        return String.format("{\"power\": \"%s\", \"watt\": \"%s\", \"ecomode\": \"%s\", \"cutoff\": \"%s\"}",
                power, watt, ecomode, cutoff);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (power != null) map.put("power", power);
        if (ecomode != null) map.put("ecomode", ecomode);
        if (cutoff != null) map.put("cutoff", cutoff);
        if (watt != null) map.put("watt", watt);
        return map;
    }

}