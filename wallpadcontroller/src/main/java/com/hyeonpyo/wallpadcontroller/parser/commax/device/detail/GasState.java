package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GasState implements DeviceState {
    private String power;

    @Override
    public String toJson() {
        return String.format("{\"power\": \"%s\"}", power);
    }

    @Override
    public Map<String, String> toMap() {
        return Map.of("power", power);
    }

}
