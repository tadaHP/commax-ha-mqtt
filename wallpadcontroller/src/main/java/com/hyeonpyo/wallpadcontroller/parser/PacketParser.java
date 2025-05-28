package com.hyeonpyo.wallpadcontroller.parser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.parser.commax.type.PacketKind;
import com.hyeonpyo.wallpadcontroller.parser.commax.type.ParsedPacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PacketParser {

    private final Map<String, Object> deviceStructure;

    public PacketParser(DeviceStructureLoader loader) {
        this.deviceStructure = loader.getDeviceStructure();
        loadDeviceStructure();
    }

    private void loadDeviceStructure() {
        log.info("✅ 패킷 구조 로드 완료");
        addFieldPositions();
    }

    private void addFieldPositions() {
        for (Map.Entry<String, Object> entry : deviceStructure.entrySet()) {
            Map<String, Object> device = cast(entry.getValue());
            for (String packetType : List.of("command", "state", "state_request", "ack")) {
                if (device.containsKey(packetType)) {
                    Map<String, Object> packet = cast(device.get(packetType));
                    Map<String, Object> structure = cast(packet.get("structure"));
                    Map<String, String> fieldPositions = new HashMap<>();

                    for (Map.Entry<String, Object> posEntry : structure.entrySet()) {
                        String pos = posEntry.getKey();
                        Map<String, Object> field = cast(posEntry.getValue());
                        String name = (String) field.get("name");
                        if (!"empty".equals(name)) {
                            if (fieldPositions.containsKey(name)) {
                                log.error("❗ 중복된 필드: {}.{} - {}", entry.getKey(), packetType, name);
                            } else {
                                fieldPositions.put(name, pos);
                            }
                        }
                    }

                    packet.put("fieldPositions", fieldPositions);
                }
            }
        }
    }

    public ParsedPacket parse(String hexString) {
        byte[] bytes = hexStringToByteArray(hexString);
        String header = String.format("%02X", bytes[0]);

        for (Map.Entry<String, Object> entry : deviceStructure.entrySet()) {
            String deviceName = entry.getKey();
            Map<String, Object> device = cast(entry.getValue());

            for (String type : List.of("command", "state", "state_request", "ack")) {
                if (!device.containsKey(type)) continue;
                Map<String, Object> packet = cast(device.get(type));
                String expectedHeader = (String) packet.get("header");
                if (expectedHeader.equalsIgnoreCase(header)) {
                    log.debug("✅ [{}] [{}] 패킷 분석 시작", deviceName, type);
                    Map<String, Object> structure = cast(packet.get("structure"));
                    Map<String, String> parsedFields = new LinkedHashMap<>();

                    for (int i = 1; i < bytes.length && i <= 7; i++) {
                        String key = String.valueOf(i);
                        if (!structure.containsKey(key)) continue;
                        Map<String, Object> field = cast(structure.get(key));
                        String name = (String) field.get("name");
                        String value = String.format("%02X", bytes[i]);
                        if (!"empty".equals(name)) {
                            parsedFields.put(name, value);
                        }
                    }

                    return new ParsedPacket(deviceName, PacketKind.fromKey(type), parsedFields);
                }
            }
        }

        log.warn("❓ 알 수 없는 헤더: {}", header);
        return null;
    }

    private byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("\\s", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object obj) {
        return (Map<String, Object>) obj;
    }
}
