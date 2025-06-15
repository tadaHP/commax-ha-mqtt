package com.hyeonpyo.wallpadcontroller.device.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

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
            if (hexValue != null && !"null".equalsIgnoreCase(hexValue)) {
                latestState.put(key, hexValue); // 이미 readable한 값임
            }
        }
    }

    private String makeKey(String deviceName, int deviceIndex, String field) {
        String fullName = deviceName + deviceIndex;
        return "state/" + fullName + "/" + field;
    }

    @PreDestroy
    public void shutdown() {
        log.info("\uD83D\uDEB8 DeviceStateManager executor shutdown");
        executorService.shutdown();
    }
}
