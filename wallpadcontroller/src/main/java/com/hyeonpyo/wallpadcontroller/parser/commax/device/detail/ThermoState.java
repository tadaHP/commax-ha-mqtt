package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

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
}