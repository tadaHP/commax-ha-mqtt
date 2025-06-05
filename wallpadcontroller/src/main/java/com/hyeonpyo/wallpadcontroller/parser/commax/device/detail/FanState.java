package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

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
}
