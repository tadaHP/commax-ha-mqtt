package com.hyeonpyo.wallpadcontroller.device.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketFieldValue;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;
import com.hyeonpyo.wallpadcontroller.mqtt.sender.MqttSendService;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStateManager {

    private final MqttSendService mqttSendService;
    private final MqttProperties mqttProperties;
    private final DeviceTypeRepository deviceTypeRepository;

    private final Map<String, Map<String, Map<String, String>>> fieldValueMap = new HashMap<>();

    private final Map<String, String> latestState = new ConcurrentHashMap<>();
    private final Map<String, String> lastPublishedState = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void startPublisherLoop() {
        executorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, String> entry : latestState.entrySet()) {
                String key = entry.getKey();
                String newValue = entry.getValue();
                String lastValue = lastPublishedState.get(key);

                if (!newValue.equals(lastValue)) {
                    mqttSendService.publish(mqttProperties.getHaTopic() + "/" + key, newValue, 1);
                    lastPublishedState.put(key, newValue);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @PostConstruct
    public void initStructureFromDb() {
        List<DeviceType> deviceTypes = deviceTypeRepository.findAllWithFullStructure();

        for (DeviceType deviceType : deviceTypes) {
            String deviceName = deviceType.getName();
            Map<String, Map<String, String>> fieldMap = new HashMap<>();

            for (PacketType packetType : deviceType.getPacketTypes()) {
                if (!"state".equalsIgnoreCase(packetType.getKind())) continue;

                for (PacketField field : packetType.getFields()) {
                    if (field.getName() == null || "empty".equals(field.getName())) continue;

                    Map<String, String> values = new HashMap<>();
                    for (PacketFieldValue fieldValue : field.getValueMappings()) {
                        values.put(fieldValue.getRawKey(), fieldValue.getHex().toUpperCase());
                    }
                    fieldMap.put(field.getName(), values);
                }
            }

            fieldValueMap.put(deviceName, fieldMap);
        }

        log.info("✅ 상태값 매핑 정보 DB에서 로드 완료 ({}개 디바이스)", fieldValueMap.size());
    }
    
        public void updateState(String deviceName, int deviceIndex, Map<String, String> stateMap) {
        for (Map.Entry<String, String> entry : stateMap.entrySet()) {
            String field = entry.getKey();
            String hexValue = entry.getValue();
            String key = makeKey(deviceName, deviceIndex, field);
            String readableValue = convertToReadableState(deviceName, field, hexValue);
            if (readableValue != null && !"null".equalsIgnoreCase(readableValue)) {
                latestState.put(key, readableValue);
            }
        }
    }

    private String makeKey(String deviceName, int deviceIndex, String field) {
        String fullName = deviceName + deviceIndex;
        String suffix = switch (field) {
            case "power", "action", "curTemp", "setTemp", "mode", "speed" -> "/state";
            default -> "";
        };
        return fullName + "/" + field + suffix;
    }

    private String convertToReadableState(String deviceName, String field, String hexValue) {
        Map<String, Map<String, String>> fieldMap = fieldValueMap.get(deviceName);
        if (fieldMap == null) return hexValue;

        Map<String, String> valuesMap = fieldMap.get(field);
        if (valuesMap == null) return hexValue;

        for (Map.Entry<String, String> entry : valuesMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(hexValue)) {
                return normalizeValue(deviceName, field, entry.getKey());
            }
        }

        return normalizeValue(deviceName, field, hexValue);
    }
    private String normalizeValue(String deviceName, String field, String rawKey) {
        switch (deviceName) {
            case "Thermo":
                if ("power".equals(field)) {
                    return switch (rawKey) {
                        case "idle", "heating" -> "heat";
                        case "off" -> "off";
                        default -> rawKey;
                    };
                } else if ("action".equals(field)) {
                    return switch (rawKey) {
                        case "idle" -> "idle";
                        case "heating" -> "heating";
                        case "off" -> "off";
                        default -> rawKey;
                    };
                } else {
                    return rawKey;
                }

            case "Fan":
                if ("power".equals(field)) {
                    return switch (rawKey.toLowerCase()) {
                        case "off" -> "OFF";
                        case "normal", "bypass" -> "ON";
                        default -> "OFF";
                    };
                } else {
                    return rawKey;
                }

            case "Light", "LightBreaker", "Outlet":
                if ("power".equals(field)) {
                    return switch (rawKey) {
                        case "on" -> "ON";
                        case "off" -> "OFF";
                        default -> rawKey;
                    };
                } else {
                    return rawKey;
                }

            default:
                return rawKey;
        }
    }


    @PreDestroy
    public void shutdown() {
        log.info("\uD83D\uDEB8 DeviceStateManager executor shutdown");
        executorService.shutdown();
    }
}
