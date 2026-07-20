package com.hyeonpyo.wallpadcontroller.elfin;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.command.CommandQueue;
import com.hyeonpyo.wallpadcontroller.domain.builder.CommandBuilder;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElfinCommandService {

    private final CommandBuilder commandBuilder;
    private final DeviceEntityRepository deviceEntityRepository;
    private final MqttProperties mqttProperties;
    private final CommandQueue commandQueue;

    public void sendCommand(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.info("📨 HA 명령 수신 - topic: {}, payload: {}", topic, payload);

        String[] parts = topic.split("/");
        if (parts.length < 4) {
            log.warn("⚠️ 잘못된 topic 형식: {}", topic);
            return;
        }

        String deviceInfo = parts[2];
        String field = parts[3];

        String deviceType = deviceInfo.replaceAll("\\d+$", "");
        String indexStr = deviceInfo.replace(deviceType, "");
        final int deviceIndex;
        try {
            deviceIndex = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            log.warn("⚠️ device index 파싱 실패: {}", deviceInfo);
            return;
        }

        String uniqueId = mqttProperties.getHaTopic() + "_" + deviceType + "_" + deviceIndex;
        Optional<DeviceEntity> registered = deviceEntityRepository.findById(uniqueId);
        if (registered.isEmpty()) {
            log.warn("⚠️ 등록되지 않은 기기 명령 무시: {}", uniqueId);
            return;
        }
        if (Boolean.FALSE.equals(registered.get().getUsed())) {
            log.warn("⚠️ MQTT 비활성(used=false) 기기 명령 무시: {}", uniqueId);
            return;
        }

        commandBuilder.build(deviceType, deviceIndex, field, payload)
            .ifPresentOrElse(command -> {
                commandQueue.submit(deviceType, deviceIndex, field, payload, command);
            }, () -> {
                log.warn("⚠️ 패킷 생성 실패 - deviceType: {}, index: {}, field: {}, payload: {}", deviceType, deviceIndex, field, payload);
            });
    }

}
