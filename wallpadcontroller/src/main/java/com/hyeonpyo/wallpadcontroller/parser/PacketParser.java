package com.hyeonpyo.wallpadcontroller.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.FanState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.GasState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.LightState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.OutletState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.ThermoState;
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

    public List<ParsedPacket> parseMultiple(String hexString) {
        List<ParsedPacket> results = new ArrayList<>();
        byte[] bytes = hexStringToByteArray(hexString);
        int offset = 0;

        while (offset + PACKET_LENGTH <= bytes.length) {
            byte[] packetBytes = Arrays.copyOfRange(bytes, offset, offset + PACKET_LENGTH);
            Optional<ParsedPacket> parsed = parseSinglePacket(packetBytes);
            parsed.ifPresent(p -> results.add(p));
            offset += PACKET_LENGTH;
        }
        return results;
    }

    private Optional<ParsedPacket> parseSinglePacket(byte[] bytes) {
        String header = String.format("%02X", bytes[0]);

        for (Map.Entry<String, Object> entry : deviceStructure.entrySet()) {
            String deviceName = entry.getKey();
            Map<String, Object> device = cast(entry.getValue());

            for (String type : List.of("command", "state", "state_request", "ack")) {
                if (!device.containsKey(type)) continue;
                Map<String, Object> packet = cast(device.get(type));
                String expectedHeader = (String) packet.get("header");
                if (expectedHeader.equalsIgnoreCase(header)) {
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
                    Optional<DeviceState> state = this.toDeviceState(deviceName, parsedFields);
                    if (state.isEmpty()) {
                        log.warn("⚠️ toDeviceState 실패, 알수 없는 타입: deviceName='{}', fields={}", deviceName, parsedFields);
                        return Optional.empty();
                    }
                    int deviceIndex = extractDeviceIndex(structure, bytes);
                    return state.map(d -> new ParsedPacket(deviceName, deviceIndex, PacketKind.fromKey(type), d));
                }
            }
        }
        return Optional.empty();
    }

    private int extractDeviceIndex(Map<String, Object> structure, byte[] bytes) {
        for (Map.Entry<String, Object> entry : structure.entrySet()) {
            String posStr = entry.getKey();  // 예: "2"
            Map<String, Object> field = cast(entry.getValue());
            String name = (String) field.get("name");

            if ("deviceId".equalsIgnoreCase(name)) {
                int pos = Integer.parseInt(posStr); // 패킷 구조는 1부터 시작
                if (bytes.length > pos) {
                    return bytes[pos] & 0xFF; // unsigned byte to int
                }
            }
        }
        return 1; // 기본값
    }

    private Optional<DeviceState> toDeviceState(String deviceName, Map<String, String> fields) {
        switch (deviceName) {
            case "Fan": {
                String powerHex = fields.get("power"); // mode 판단용으로만 사용
                String speedHex = fields.get("speed");
                        
                String mode = switch (powerHex != null ? powerHex.toUpperCase() : "") {
                    case "04" -> "normal";
                    case "07" -> "bypass";
                    case "00" -> "off";
                    default -> "off";
                };
            
                return Optional.of(new FanState(speedHex, mode, mode));
            }
            case "Thermo": {
                String powerHex = fields.get("power");
                String actionHex = fields.get("power"); // 여기에 action 필드가 없어서 power와 동일하게 설정된 것으로 보임
                        
                String power = switch (powerHex != null ? powerHex.toUpperCase() : "") {
                    case "80" -> "off";
                    case "81" -> "idle";
                    case "83" -> "heating";
                    default -> "off";
                };
            
                String action = switch (actionHex != null ? actionHex.toUpperCase() : "") {
                    case "80" -> "off";
                    case "81" -> "idle";
                    case "83" -> "heating";
                    default -> "off";
                };
            
                return Optional.of(new ThermoState(
                    power,
                    action,
                    fields.get("currentTemp"),
                    fields.get("targetTemp")
                ));
            }
            case "Light":
            case "LightBreaker":
                return Optional.of(new LightState(fields.get("power")));
            case "Outlet":
                return Optional.of(new OutletState(fields.get("power"), fields.get("watt"), fields.get("ecomode"), fields.get("cutoff")));
            case "Gas":
                return Optional.of(new GasState(fields.get("power")));
            default:
                log.warn("⚠️ toDeviceState: Unknown deviceName '{}', fields={}", deviceName, fields);
                return null;
        }
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

    private String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
