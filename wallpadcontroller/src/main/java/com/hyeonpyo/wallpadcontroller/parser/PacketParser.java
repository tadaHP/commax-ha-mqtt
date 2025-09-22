package com.hyeonpyo.wallpadcontroller.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingFieldValue;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.domain.unknownpacket.UnknownPacket;
import com.hyeonpyo.wallpadcontroller.domain.unknownpacket.UnknownPacketRepository;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.DeviceState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.ElevatorState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.FanState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.GasState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.LightState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.OutletState;
import com.hyeonpyo.wallpadcontroller.parser.commax.device.detail.ThermoState;
import com.hyeonpyo.wallpadcontroller.parser.commax.type.PacketKind;
import com.hyeonpyo.wallpadcontroller.parser.commax.type.ParsedPacket;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PacketParser {

    private final DeviceTypeRepository deviceTypeRepository;
    private final UnknownPacketRepository unknownPacketRepository;

    private final Map<Long, Map<String, String>> packetFieldValueMap = new HashMap<>();
    private final Map<Long, Map<String, String>> hexToRawKeyMap = new HashMap<>();
    private final Map<String, Set<String>> reverseValueMap = new HashMap<>();

    private final Map<Long, Map<Integer, ParsingField>> fieldLookupMap = new HashMap<>();
    private final Map<String, PacketType> headerMap = new HashMap<>();
    private final Map<String, String> headerToDeviceName = new HashMap<>();
    private final int PACKET_LENGTH = 8;

    @PostConstruct
    public void init() {
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();
        for (DeviceType deviceType : deviceTypes) {
            Map<String, PacketType> packetMap = new HashMap<>();
            for (PacketType packet : deviceType.getPacketTypes()) {
                packetMap.put(packet.getKind(), packet);
                Map<Integer, ParsingField> fieldsByPosition = new HashMap<>();
                for (ParsingField field : packet.getFields()) {
                    fieldsByPosition.put(field.getPosition(), field);
                }
                fieldLookupMap.put(packet.getId(), fieldsByPosition);

                String header = packet.getHeader().toUpperCase();
                headerMap.put(header, packet);
                headerToDeviceName.put(header, deviceType.getName());
            }
        }
    }

    @PostConstruct
    public void initStructureFromDb() {
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();
    
        for (DeviceType deviceType : deviceTypes) {
            for (PacketType packetType : deviceType.getPacketTypes()) {
                if (!"state".equalsIgnoreCase(packetType.getKind())) continue;
            
                for (ParsingField field : packetType.getFields()) {
                    if (field.getName() == null || "empty".equals(field.getName())) continue;
                
                    Map<String, String> rawToHex = new HashMap<>();
                    Map<String, String> hexToRaw = new HashMap<>();
                
                    for (ParsingFieldValue fieldValue : field.getValueMappings()) {
                        String hex = fieldValue.getHex().toUpperCase();
                        String rawKey = fieldValue.getRawKey();
                        rawToHex.put(rawKey, hex);
                        hexToRaw.put(hex, rawKey);

                        String reverseMapValue = String.format("%s:%s:%s", deviceType.getName(), field.getName(), rawKey);
                        reverseValueMap.computeIfAbsent(hex, k -> new HashSet<>()).add(reverseMapValue);
                    }
                
                    packetFieldValueMap.put(field.getId(), rawToHex);
                    hexToRawKeyMap.put(field.getId(), hexToRaw);
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
            parseSinglePacket(packetBytes).ifPresent(results::add);
            offset += PACKET_LENGTH;
        }
        return results;
    }

    private Optional<ParsedPacket> parseSinglePacket(byte[] bytes) {
        String header = String.format("%02X", bytes[0]);
    
        PacketType packet = headerMap.get(header);
        if (packet == null) {
            String rawHex = bytesToHexString(bytes);
            String notes = generateNotesForUnknownPacket(bytes);
            UnknownPacket unknownPacket = UnknownPacket.builder()
                    .rawData(rawHex)
                    .notes(notes)
                    .build();
            unknownPacketRepository.save(unknownPacket);
            log.info("📝 미확인 패킷 저장: {} | 분석: {}", rawHex, notes);
            return Optional.empty();
        }
    
        String deviceName = headerToDeviceName.get(header);
        if (deviceName == null) return Optional.empty();
    
        Map<Integer, ParsingField> fields = fieldLookupMap.get(packet.getId());
        Map<String, String> parsedFields = new LinkedHashMap<>();
    
        for (int i = 1; i < PACKET_LENGTH; i++) {
            ParsingField field = fields.get(i);
            if (field == null || "empty".equals(field.getName())) continue;
                
            String hex = String.format("%02X", bytes[i]);
            String readable = convertHexToRawKey(field.getId(), hex);
            parsedFields.put(field.getName(), readable);
        }
    
        int deviceIndex = extractDeviceIndex(fields, bytes);
        DeviceState state = toDeviceState(deviceName, parsedFields);
        return Optional.of(new ParsedPacket(deviceName, deviceIndex, PacketKind.fromKey(packet.getKind()), state));
    }

    private String generateNotesForUnknownPacket(byte[] bytes) {
        StringJoiner notes = new StringJoiner(" ");
        for (byte b : bytes) {
            String hex = String.format("%02X", b);
            Set<String> meanings = reverseValueMap.get(hex);
            if (meanings != null && !meanings.isEmpty()) {
                notes.add(String.format("(%s)", String.join(" | ", meanings)));
            } else {
                notes.add("?");
            }
        }
        return notes.toString();
    }

    private int extractDeviceIndex(Map<Integer, ParsingField> structure, byte[] bytes) {
        for (Map.Entry<Integer, ParsingField> entry : structure.entrySet()) {
            ParsingField field = entry.getValue();
            if ("deviceId".equalsIgnoreCase(field.getName())) {
                int pos = entry.getKey();
                if (bytes.length > pos) {
                    return bytes[pos] & 0xFF;
                }
            }
        }
        return 1;
    }

    private DeviceState toDeviceState(String deviceName, Map<String, String> fields) {
        switch (deviceName) {
            case "Fan":
                return new FanState(fields.get("speed"), fields.get("power"));
            case "Thermo":
                return new ThermoState(fields.get("power"), fields.get("currentTemp"), fields.get("targetTemp"));
            case "Light":
            case "LightBreaker":
                return new LightState(fields.get("power"));
            case "Outlet":
                return new OutletState(fields.get("power"), fields.get("watt"), fields.get("ecomode"), fields.get("cutoff"));
            case "Gas":
                return new GasState(fields.get("power"));
            case "EV":
                return new ElevatorState(fields.get("power"), fields.get("floor"));
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

    private String bytesToHexString(byte[] bytes) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexBuilder.append(String.format("%02X ", b));
        }
        return hexBuilder.toString().trim();
    }

    private String convertHexToRawKey(Long packetFieldId, String hex) {
        Map<String, String> hexMap = hexToRawKeyMap.get(packetFieldId);
        if (hexMap == null) return hex;
        return hexMap.getOrDefault(hex.toUpperCase(), hex);
    }
}
