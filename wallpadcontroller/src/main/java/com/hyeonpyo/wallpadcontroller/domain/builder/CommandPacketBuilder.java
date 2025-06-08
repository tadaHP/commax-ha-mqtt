package com.hyeonpyo.wallpadcontroller.domain.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketFieldValue;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandPacketBuilder {

    private final DeviceTypeRepository deviceTypeRepository;

    private final Map<String, Map<String, String>> fieldValueMap = new HashMap<>();
    private final Map<String, PacketType> commandPacketMap = new HashMap<>();
    private final Map<String, Map<Integer, PacketField>> fieldPositionMap = new HashMap<>();

    private final int PACKET_LENGTH = 8;

    @PostConstruct
    public void init() {
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();

        for (DeviceType deviceType : deviceTypes) {
            for (PacketType packet : deviceType.getPacketTypes()) {
                if (!"command".equalsIgnoreCase(packet.getKind())) continue;

                String deviceName = deviceType.getName();
                String key = deviceName.toLowerCase();
                commandPacketMap.put(key, packet);

                Map<Integer, PacketField> fieldsByPos = new HashMap<>();
                for (PacketField field : packet.getFields()) {
                    fieldsByPos.put(field.getPosition(), field);
                    if (field.getValueMappings() != null && field.getName() != null) {
                        fieldValueMap
                            .computeIfAbsent(field.getName(), k -> new HashMap<>())
                            .putAll(field.getValueMappings().stream()
                                .collect(Collectors.toMap(PacketFieldValue::getRawKey, PacketFieldValue::getHex)));
                    }
                }
                fieldPositionMap.put(key, fieldsByPos);
            }
        }

        log.info("✅ 명령 패킷 구조 초기화 완료: {}", commandPacketMap.keySet());
    }

    public Optional<byte[]> build(String deviceType, int deviceIndex, Map<String, String> values) {
        String key = deviceType.toLowerCase();
        PacketType packet = commandPacketMap.get(key);
        if (packet == null) {
            log.warn("⚠️ 해당 deviceType의 command packet 없음: {}", deviceType);
            return Optional.empty();
        }

        byte[] bytes = new byte[PACKET_LENGTH];
        bytes[0] = (byte) Integer.parseInt(packet.getHeader(), 16);

        Map<Integer, PacketField> fields = fieldPositionMap.get(key);
        for (int i = 1; i < PACKET_LENGTH - 1; i++) {
            PacketField field = fields.get(i);
            if (field == null || "empty".equals(field.getName())) {
                bytes[i] = 0x00;
                continue;
            }

            String raw = "deviceId".equals(field.getName())
                    ? String.format("%02X", deviceIndex)
                    : fieldValueMap.getOrDefault(field.getName(), Map.of()).getOrDefault(values.get(field.getName()), "00");

            bytes[i] = (byte) Integer.parseInt(raw, 16);
        }

        // 마지막 바이트는 checksum
        bytes[PACKET_LENGTH - 1] = calculateChecksum(bytes);

        return Optional.of(bytes);
    }

    private byte calculateChecksum(byte[] packet) {
        int sum = 0;
        for (int i = 0; i < PACKET_LENGTH - 1; i++) {
            sum += packet[i] & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }

}
