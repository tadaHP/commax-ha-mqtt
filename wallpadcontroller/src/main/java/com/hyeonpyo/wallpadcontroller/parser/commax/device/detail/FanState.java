package com.hyeonpyo.wallpadcontroller.parser.commax.device.detail;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;

import lombok.Data;

@Data
public class FanState implements DeviceState {
    private final String speed;
    private final String power;

    public FanState(String speed, String power) {
        this.speed = speed;
        this.power = power;
    }


    @Override
    public String toJson() {
        return String.format("{\"speed\": \"%s\", \"power\": \"%s\"}", speed, power);
    }

    /**
     * HA MQTT fan discovery와 맞춤: {@code power} 토픽은 ON/OFF, {@code mode}는 NORMAL/BYPASS(끄면 빈 문자열로 프리셋 해제).
     * {@code speed}는 discovery 템플릿이 기대하는 대문자(LOW/MEDIUM/HIGH/OFF).
     */
    @Override
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if (speed != null && !speed.isBlank()) {
            String s = speed.trim();
            map.put("speed", s.equalsIgnoreCase("off") ? "OFF" : s.toUpperCase(Locale.ROOT));
        }
        if (power != null && !power.isBlank()) {
            String raw = power.trim();
            if (raw.equalsIgnoreCase("off")) {
                map.put("power", "OFF");
                map.put("mode", "");
            } else if (raw.equalsIgnoreCase("NORMAL")) {
                map.put("power", "ON");
                map.put("mode", "NORMAL");
            } else if (raw.equalsIgnoreCase("BYPASS")) {
                map.put("power", "ON");
                map.put("mode", "BYPASS");
            } else {
                String upper = raw.toUpperCase(Locale.ROOT);
                map.put("power", "ON".equals(upper) || "OFF".equals(upper) ? upper : raw);
            }
        }
        return map;
    }
}
