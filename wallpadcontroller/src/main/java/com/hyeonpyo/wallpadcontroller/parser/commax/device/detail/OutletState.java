package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

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
}