package com.hyeonpyo.wallpadcontroller.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PacketParser {

    private final Map<String, Object> deviceStructure = new HashMap<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public PacketParser() {
        loadDeviceStructure();
    }

    private void loadDeviceStructure() {
        try {
            File file = new File("src/main/resources/commax-packet-struct.yml");
            Map<String, Object> structure = yamlMapper.readValue(file, new TypeReference<>() {});
            deviceStructure.clear();
            deviceStructure.putAll(structure);
            log.info("✅ 패킷 구조 로드 완료");
            addFieldPositions();
        } catch (IOException e) {
            log.error("❌ 패킷 구조 로드 실패", e);
        }
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

    public void parse(String hexString) {
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
                    log.info("✅ [{}] [{}] 패킷 분석 시작", deviceName, type);
                    Map<String, Object> structure = cast(packet.get("structure"));
                    for (int i = 1; i < bytes.length && i <= 7; i++) {
                        String key = String.valueOf(i);
                        if (!structure.containsKey(key)) continue;
                        Map<String, Object> field = cast(structure.get(key));
                        String name = (String) field.get("name");
                        String value = String.format("%02X", bytes[i]);
                        log.info("  {}: {}", name, value);
                    }
                    return;
                }
            }
        }

        log.warn("❓ 알 수 없는 헤더: {}", header);
    }

    private byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("\\s", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object obj) {
        return (Map<String, Object>) obj;
    }
}
