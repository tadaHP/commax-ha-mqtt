package com.hyeonpyo.wallpadcontroller.parser;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final int PACKET_LENGTH = 8;  // 고정 패킷 길이

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

    public List<ParsedPacket> parseMultiple(String hexString) {
        List<ParsedPacket> results = new ArrayList<>();
        byte[] bytes = hexStringToByteArray(hexString);

        int offset = 0;

        while (offset + PACKET_LENGTH <= bytes.length) {
            byte[] packetBytes = Arrays.copyOfRange(bytes, offset, offset + PACKET_LENGTH);
            ParsedPacket parsed = parseSinglePacket(packetBytes);
            if (parsed != null) {
                results.add(parsed);
            }
            offset += PACKET_LENGTH;
        }

        // 남은 데이터가 8바이트 미만일 경우: 유실된 패킷 가능성
        if (offset < bytes.length) {
            byte[] remaining = Arrays.copyOfRange(bytes, offset, bytes.length);
            log.warn("⚠️ 패킷 유실 가능성: {} 바이트 → {}", remaining.length, byteArrayToHex(remaining));
        }

        return results;
    }

    private ParsedPacket parseSinglePacket(byte[] packet) {
        String header = String.format("%02X", packet[0]);
        log.info("📦 단일 패킷 분석: 헤더={}", header);
        
        for (Map.Entry<String, Object> entry : deviceStructure.entrySet()) {
            String deviceName = entry.getKey();
            Map<String, Object> device = cast(entry.getValue());
        
            for (String type : List.of("command", "state", "state_request", "ack")) {
                if (!device.containsKey(type)) continue;
            
                Map<String, Object> packetDef = cast(device.get(type));
                String expectedHeader = (String) packetDef.get("header");
            
                if (expectedHeader.equalsIgnoreCase(header)) {
                    Map<String, Object> structure = cast(packetDef.get("structure"));
                    Map<String, String> parsedFields = new LinkedHashMap<>();
                
                    for (int i = 1; i < packet.length; i++) {
                        String key = String.valueOf(i);
                        if (!structure.containsKey(key)) continue;
                        Map<String, Object> field = cast(structure.get(key));
                        String name = (String) field.get("name");
                        String value = String.format("%02X", packet[i]);
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

    private String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object obj) {
        return (Map<String, Object>) obj;
    }
}
