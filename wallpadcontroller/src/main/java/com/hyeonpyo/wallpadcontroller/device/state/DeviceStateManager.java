package com.hyeonpyo.wallpadcontroller.device.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.mqtt.sender.MqttSendService;
import com.hyeonpyo.wallpadcontroller.parser.DeviceStructureLoader;
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
    private final DeviceStructureLoader structureLoader;


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

    private String convertToReadableState(String deviceName, String field, String value) {
        Object deviceObj = structureLoader.getDeviceStructure().get(deviceName);
        if (!(deviceObj instanceof Map<?, ?> device)) return value;

        Object stateObj = device.get("state");
        if (!(stateObj instanceof Map<?, ?> statePacket)) return value;

        Object structureObj = statePacket.get("structure");
        if (!(structureObj instanceof Map<?, ?> structure)) return value;

        for (Map.Entry<?, ?> entry : structure.entrySet()) {
            Object fieldMapObj = entry.getValue();
            if (!(fieldMapObj instanceof Map<?, ?> fieldMap)) continue;

            Object fieldNameObj = fieldMap.get("name");
            if (!(fieldNameObj instanceof String fieldName)) continue;

            if (!fieldName.equals(field)) continue;

            Object valuesObj = fieldMap.get("values");
            if (!(valuesObj instanceof Map<?, ?> values)) continue;

            for (Map.Entry<?, ?> valueEntry : values.entrySet()) {
                if (!(valueEntry.getKey() instanceof String rawKey)) continue;
                if (!(valueEntry.getValue() instanceof String hexValue)) continue;

                if (hexValue.equalsIgnoreCase(value)) {
                    return normalizeValue(deviceName, field, rawKey);
                }
            }
        }

        return value;
    }

    private String normalizeValue(String deviceName, String field, String rawKey) {
        return switch (deviceName) {
            case "Thermo" -> {
                if ("power".equals(field)) {
                    yield switch (rawKey) {
                        case "idle", "heating" -> "heat";
                        case "off" -> "off";
                        default -> rawKey;
                    };
                } else if ("action".equals(field)) {
                    yield switch (rawKey) {
                        case "idle" -> "idle";
                        case "heating" -> "heating";
                        case "off" -> "off";
                        default -> rawKey;
                    };
                } else yield rawKey;
            }
            case "Fan" -> rawKey;
            case "Light", "LightBreaker", "Outlet" -> {
                if ("power".equals(field)) {
                    yield switch (rawKey) {
                        case "on" -> "ON";
                        case "off" -> "OFF";
                        default -> rawKey;
                    };
                } else yield rawKey;
            }
            default -> rawKey;
        };
    }


    @PreDestroy
    public void shutdown() {
        log.info("\uD83D\uDEB8 DeviceStateManager executor shutdown");
        executorService.shutdown();
    }
}
