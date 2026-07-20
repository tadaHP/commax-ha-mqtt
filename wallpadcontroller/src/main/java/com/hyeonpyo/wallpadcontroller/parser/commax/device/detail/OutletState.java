package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.Data;

@Data
public class OutletState implements DeviceState {
    private String power;
    private String watt;
    private String ecomode;
    private String cutoff;

    public OutletState(String power, String watt, String ecomode, String cutoff) {
        this.power = power;
        this.watt = watt;
        this.ecomode = ecomode;
        this.cutoff = cutoff;
    }

    public static OutletState fromPacketFields(Map<String, String> fields) {
        String power = fields.get("power");
        String stateType = fields.get("stateType");
        String data1 = fields.get("data1");
        String data2 = fields.get("data2");
        String data3 = fields.get("data3");
        String ecomode = power != null && power.contains("with_eco") ? "ON" : "OFF";

        if ("wattage".equals(stateType)) {
            return new OutletState(normalizePower(power), formatWatt(decodeBcd(data1, data2, data3)), ecomode, null);
        }
        if ("ecomode".equals(stateType)) {
            return new OutletState(normalizePower(power), null, ecomode, formatCutoff(decodeBcd(data1, data2, data3)));
        }
        return new OutletState(normalizePower(power), "unknown", ecomode, "unknown");
    }

    private static String normalizePower(String power) {
        return power != null && power.startsWith("on") ? "ON" : "OFF";
    }

    private static Long decodeBcd(String... values) {
        long result = 0;
        for (String value : values) {
            if (value == null || !value.matches("[0-9A-Fa-f]{2}")) return null;
            int raw = Integer.parseInt(value, 16);
            if ((raw >> 4) > 9 || (raw & 0x0F) > 9) return null;
            int decimal = ((raw >> 4) * 10) + (raw & 0x0F);
            result = result * 100 + decimal;
        }
        return result;
    }

    private static String formatWatt(Long value) {
        return value == null ? "unknown" : String.valueOf(value / 10.0);
    }

    private static String formatCutoff(Long value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

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
