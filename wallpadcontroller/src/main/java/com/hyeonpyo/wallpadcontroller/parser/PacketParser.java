package com.hyeonpyo.wallpadcontroller.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketFieldValue;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
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

    // private final Map<String, Map<String, PacketType>> deviceStructure = new HashMap<>();
    // <deviceType의이름(light, thermo, fan), <packet.getKind(command,state,ack 등), 해당하는 엔티티> >
    // private final Map<String, Map<String, Map<String, String>>> fieldValueMap;

    private final Map<Long, Map<String, String>> packetFieldValueMap = new HashMap<>();
    private final Map<Long, Map<String, String>> hexToRawKeyMap = new HashMap<>();


    private final Map<Long, Map<Integer, PacketField>> fieldLookupMap = new HashMap<>();
    // <PacketType의 PK, <position(패킷의 n번째), 패킷 n번째의 역할 정보>>
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
                // 포지션별 필드 사전 구성
                Map<Integer, PacketField> fieldsByPosition = new HashMap<>();
                for (PacketField field : packet.getFields()) {
                    fieldsByPosition.put(field.getPosition(), field);
                }
                fieldLookupMap.put(packet.getId(), fieldsByPosition);

                // header 기반 단일 조회용 map 구성
                String header = packet.getHeader().toUpperCase();
                headerMap.put(header, packet);
                headerToDeviceName.put(header, deviceType.getName());
            }
            // deviceStructure.put(deviceType.getName(), packetMap);
        }
        // log.info("✅ 패킷 구조 DB에서 로드 완료 ({}종류)", deviceStructure.size());
    }

    // @PostConstruct
    // public void initStructureFromDb() {
    //     List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();

    //     for (DeviceType deviceType : deviceTypes) {
    //         String deviceName = deviceType.getName();
    //         Map<String, Map<String, String>> fieldMap = new HashMap<>();

    //         for (PacketType packetType : deviceType.getPacketTypes()) {
    //             if (!"state".equalsIgnoreCase(packetType.getKind())) continue;

    //             for (PacketField field : packetType.getFields()) {
    //                 if (field.getName() == null || "empty".equals(field.getName())) continue;

    //                 Map<String, String> values = new HashMap<>();
    //                 for (PacketFieldValue fieldValue : field.getValueMappings()) {
    //                     values.put(fieldValue.getRawKey(), fieldValue.getHex().toUpperCase());
    //                 }
    //                 fieldMap.put(field.getName(), values);
    //             }
    //         }

    //         fieldValueMap.put(deviceName, fieldMap);
    //     }
    // }

    @PostConstruct
    public void initStructureFromDb() {
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();
    
        for (DeviceType deviceType : deviceTypes) {
            for (PacketType packetType : deviceType.getPacketTypes()) {
                if (!"state".equalsIgnoreCase(packetType.getKind())) continue;
            
                for (PacketField field : packetType.getFields()) {
                    if (field.getName() == null || "empty".equals(field.getName())) continue;
                
                    Map<String, String> rawToHex = new HashMap<>();
                    Map<String, String> hexToRaw = new HashMap<>();
                
                    for (PacketFieldValue fieldValue : field.getValueMappings()) {
                        rawToHex.put(fieldValue.getRawKey(), fieldValue.getHex().toUpperCase());
                        hexToRaw.put(fieldValue.getHex().toUpperCase(), fieldValue.getRawKey());
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
        if (packet == null) return Optional.empty();
    
        String deviceName = headerToDeviceName.get(header);
        if (deviceName == null) return Optional.empty();
    
        Map<Integer, PacketField> fields = fieldLookupMap.get(packet.getId());
        Map<String, String> parsedFields = new LinkedHashMap<>();
    
        // for (int i = 1; i < PACKET_LENGTH; i++) {
        //     PacketField field = fields.get(i);
        //     if (field == null || "empty".equals(field.getName())) continue;
        //     String hex = String.format("%02X", bytes[i]);
        //     // String readable = convertHexToRawKey(deviceName, field.getName(), hex);
        //     String readable = convertHexToRawKey(field.getId(), hex);
        //     parsedFields.put(field.getName(), readable);
        // }

        for (int i = 1; i < PACKET_LENGTH; i++) {
            PacketField field = fields.get(i);
            if (field == null || "empty".equals(field.getName())) continue;
                
            String hex = String.format("%02X", bytes[i]);
            String readable = convertHexToRawKey(field.getId(), hex); // 수정된 부분
            parsedFields.put(field.getName(), readable);
        }
    
        int deviceIndex = extractDeviceIndex(fields, bytes);
        DeviceState state = toDeviceState(deviceName, parsedFields);
        return Optional.of(new ParsedPacket(deviceName, deviceIndex, PacketKind.fromKey(packet.getKind()), state));
    }

    private int extractDeviceIndex(Map<Integer, PacketField> structure, byte[] bytes) {
        for (Map.Entry<Integer, PacketField> entry : structure.entrySet()) {
            PacketField field = entry.getValue();
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

    // private String convertHexToRawKey(String deviceName, String field, String hex) {
    //     Map<String, Map<String, String>> fieldMap = fieldValueMap.get(deviceName);
    //     if (fieldMap == null) return hex;

    //     Map<String, String> valueMap = fieldMap.get(field);
    //     if (valueMap == null) return hex;

    //     for (Map.Entry<String, String> entry : valueMap.entrySet()) {
    //         if (entry.getValue().equalsIgnoreCase(hex)) {
    //             if(deviceName.equals("Fan")){
    //                 log.info("🔍 FanState: {} -> {}", hex, entry.getKey());
    //             }
    //             return entry.getKey();
    //         }
    //     }

    //     return hex;
    // }

    private String convertHexToRawKey(Long packetFieldId, String hex) {
        Map<String, String> hexMap = hexToRawKeyMap.get(packetFieldId);
        if (hexMap == null) return hex;
        return hexMap.getOrDefault(hex.toUpperCase(), hex);
    }
}
