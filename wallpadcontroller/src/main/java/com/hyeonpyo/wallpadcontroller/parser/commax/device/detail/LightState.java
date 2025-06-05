package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LightState implements DeviceState {
    private String power;

    @Override
    public String toJson() {
        return String.format("{\"power\": \"%s\"}", power);
    }
}
