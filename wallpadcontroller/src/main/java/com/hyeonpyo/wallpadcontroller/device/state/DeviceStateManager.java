package com.hyeonpyo.wallpadcontroller.device.state;

import java.nio.charset.StandardCharsets;
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

    private final Map<String, TargetEntry> targetState = new ConcurrentHashMap<>(); // commandService에서 내린 명령대로 변경했는지 확인하기 위한 map

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final int RETRY_INTERVAL_SEC = 2;
    private final int MAX_RETRY = 5;

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
    public void startRetryLoop() {
        executorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, TargetEntry> entry : targetState.entrySet()) {
                String key = entry.getKey();
                TargetEntry target = entry.getValue();

                String currentValue = latestState.get(key);
                if (target.getTargetValue().equals(currentValue)) {
                    log.info("✅ 목표 상태 도달: {}", key);
                    targetState.remove(key);
                    continue;
                }

                if (target.getRetryCount() >= MAX_RETRY) {
                    log.warn("❌ 최대 재시도 초과: {}", key);
                    targetState.remove(key);
                    continue;
                }

                // 재전송 수행
                resendCommand(key, target.getTargetValue());
                target.incrementRetry();
            }
        }, 0, RETRY_INTERVAL_SEC, TimeUnit.SECONDS);
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

    public void setTargetState(String deviceName, int deviceIndex, String field, String targetValue) {
        String key = makeKey(deviceName, deviceIndex, field);
        targetState.computeIfAbsent(key, k -> new TargetEntry(targetValue));
    }

    private void resendCommand(String key, String targetValue) {
        try {
            String[] parts = key.split("/");
            if (parts.length != 3) return;

            String deviceNameWithIndex = parts[1];  // Fan1
            String field = parts[2];

            String deviceType = deviceNameWithIndex.replaceAll("\\d+$", "");
            int deviceIndex = Integer.parseInt(deviceNameWithIndex.substring(deviceType.length()));

            log.info("🔁 재전송 수행: {} {}={} ({}회 시도)", deviceNameWithIndex, field, targetValue, targetState.get(key).getRetryCount());

            String topic = mqttProperties.getHaTopic() + "/command/" + deviceNameWithIndex + "/" + field;
            mqttSendService.publish(topic, targetValue, 0);
        } catch (Exception e) {
            log.error("❌ 재전송 실패: {}", key, e);
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
